/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: PDFDocumentNavigationHandler.java 830293 2009-10-27 19:07:52Z vhennebert $ */

package org.apache.fop.render.pdf;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import org.apache.fop.pdf.PDFAction;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFFactory;
import org.apache.fop.pdf.PDFGoTo;
import org.apache.fop.pdf.PDFLink;
import org.apache.fop.pdf.PDFOutline;
import org.apache.fop.render.intermediate.IFDocumentNavigationHandler;
import org.apache.fop.render.intermediate.extensions.AbstractAction;
import org.apache.fop.render.intermediate.extensions.Bookmark;
import org.apache.fop.render.intermediate.extensions.BookmarkTree;
import org.apache.fop.render.intermediate.extensions.GoToXYAction;
import org.apache.fop.render.intermediate.extensions.Link;
import org.apache.fop.render.intermediate.extensions.NamedDestination;
import org.apache.fop.render.intermediate.extensions.URIAction;
import org.apache.fop.render.pdf.PDFDocumentHandler.PageReference;

/**
 * Implementation of the {@link IFDocumentNavigationHandler} interface for PDF
 * output.
 */
public class PDFDocumentNavigationHandler implements
        IFDocumentNavigationHandler {

    private final PDFDocumentHandler documentHandler;

    private final Map<String, PDFAction> incompleteActions = new HashMap<>();
    private final Map<String, PDFAction> completeActions = new HashMap<>();

    /**
     * Default constructor.
     *
     * @param documentHandler
     *            the parent document handler
     */
    public PDFDocumentNavigationHandler(final PDFDocumentHandler documentHandler) {
        super();
        this.documentHandler = documentHandler;
    }

    PDFDocument getPDFDoc() {
        return this.documentHandler.pdfDoc;
    }

    /** {@inheritDoc} */
    @Override
    public void renderNamedDestination(final NamedDestination destination) {
        final PDFAction action = getAction(destination.getAction());
        getPDFDoc().getFactory().makeDestination(destination.getName(),
                action.makeReference());
    }

    /** {@inheritDoc} */
    @Override
    public void renderBookmarkTree(final BookmarkTree tree) {
        for (final Bookmark b : tree.getBookmarks()) {
            renderBookmark(b, null);
        }
    }

    private void renderBookmark(final Bookmark bookmark, PDFOutline parent) {
        if (parent == null) {
            parent = getPDFDoc().getOutlineRoot();
        }
        final PDFAction action = getAction(bookmark.getAction());
        final String actionRef = action != null ? action.makeReference()
                .toString() : null;
        final PDFOutline pdfOutline = getPDFDoc().getFactory().makeOutline(
                parent, bookmark.getTitle(), actionRef, bookmark.isShown());
        for (final Bookmark b : bookmark.getChildBookmarks()) {
            renderBookmark(b, pdfOutline);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void renderLink(final Link link) {
        final Rectangle targetRect = link.getTargetRect();
        final int pageHeight = this.documentHandler.currentPageRef
                .getPageDimension().height;
        final Rectangle2D targetRect2D = new Rectangle2D.Double(
                targetRect.getMinX() / 1000.0,
                (pageHeight - targetRect.getMinY() - targetRect.getHeight()) / 1000.0,
                targetRect.getWidth() / 1000.0, targetRect.getHeight() / 1000.0);
        final PDFAction pdfAction = getAction(link.getAction());
        // makeLink() currently needs a PDFAction and not a reference
        // TODO Revisit when PDFLink is converted to a PDFDictionary
        final PDFLink pdfLink = getPDFDoc().getFactory().makeLink(targetRect2D,
                pdfAction);
        if (pdfLink != null) {
            final String ptr = link.getAction().getStructurePointer();
            if (this.documentHandler.getUserAgent().isAccessibilityEnabled()
                    && ptr != null && ptr.length() > 0) {
                this.documentHandler.getLogicalStructureHandler()
                        .addLinkContentItem(pdfLink, ptr);
            }
            this.documentHandler.currentPage.addAnnotation(pdfLink);
        }
    }

    /**
     * Commits all pending elements to the PDF document.
     */
    public void commit() {
    }

    /** {@inheritDoc} */
    @Override
    public void addResolvedAction(final AbstractAction action) {
        assert action.isComplete();
        final PDFAction pdfAction = this.incompleteActions.remove(action
                .getID());
        if (pdfAction == null) {
            getAction(action);
        } else if (pdfAction instanceof PDFGoTo) {
            final PDFGoTo pdfGoTo = (PDFGoTo) pdfAction;
            updateTargetLocation(pdfGoTo, (GoToXYAction) action);
        } else {
            throw new UnsupportedOperationException(
                    "Action type not supported: "
                            + pdfAction.getClass().getName());
        }
    }

    private PDFAction getAction(final AbstractAction action) {
        if (action == null) {
            return null;
        }
        PDFAction pdfAction = this.completeActions.get(action.getID());
        if (pdfAction != null) {
            return pdfAction;
        } else if (action instanceof GoToXYAction) {
            pdfAction = this.incompleteActions.get(action.getID());
            if (pdfAction != null) {
                return pdfAction;
            } else {
                final GoToXYAction a = (GoToXYAction) action;
                final PDFGoTo pdfGoTo = new PDFGoTo(null);
                getPDFDoc().assignObjectNumber(pdfGoTo);
                if (action.isComplete()) {
                    updateTargetLocation(pdfGoTo, a);
                } else {
                    this.incompleteActions.put(action.getID(), pdfGoTo);
                }
                return pdfGoTo;
            }
        } else if (action instanceof URIAction) {
            final URIAction u = (URIAction) action;
            assert u.isComplete();
            final PDFFactory factory = getPDFDoc().getFactory();
            pdfAction = factory.getExternalAction(u.getURI(), u.isNewWindow());
            if (!pdfAction.hasObjectNumber()) {
                // Some PDF actions a pooled
                getPDFDoc().registerObject(pdfAction);
            }
            this.completeActions.put(action.getID(), pdfAction);
            return pdfAction;
        } else {
            throw new UnsupportedOperationException("Unsupported action type: "
                    + action + " (" + action.getClass().getName() + ")");
        }
    }

    private void updateTargetLocation(final PDFGoTo pdfGoTo,
            final GoToXYAction action) {
        final PageReference pageRef = this.documentHandler
                .getPageReference(action.getPageIndex());
        // Convert target location from millipoints to points and adjust for
        // different
        // page origin
        Point2D p2d = null;
        p2d = new Point2D.Double(
                action.getTargetLocation().x / 1000.0,
                (pageRef.getPageDimension().height - action.getTargetLocation().y) / 1000.0);
        final String pdfPageRef = pageRef.getPageRef().toString();
        pdfGoTo.setPageReference(pdfPageRef);
        pdfGoTo.setPosition(p2d);

        // Queue this object now that it's complete
        getPDFDoc().addObject(pdfGoTo);
        this.completeActions.put(action.getID(), pdfGoTo);
    }

}
