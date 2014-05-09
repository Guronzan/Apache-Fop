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

/* $Id: IFRenderer.java 830293 2009-10-27 19:07:52Z vhennebert $ */

package org.apache.fop.render.intermediate;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import javax.xml.transform.stream.StreamResult;

import lombok.extern.slf4j.Slf4j;

import org.apache.batik.parser.AWTTransformProducer;
import org.apache.fop.Version;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.area.Area;
import org.apache.fop.area.AreaTreeObject;
import org.apache.fop.area.Block;
import org.apache.fop.area.BlockViewport;
import org.apache.fop.area.BookmarkData;
import org.apache.fop.area.CTM;
import org.apache.fop.area.DestinationData;
import org.apache.fop.area.OffDocumentExtensionAttachment;
import org.apache.fop.area.OffDocumentItem;
import org.apache.fop.area.PageSequence;
import org.apache.fop.area.PageViewport;
import org.apache.fop.area.RegionViewport;
import org.apache.fop.area.Trait;
import org.apache.fop.area.inline.AbstractTextArea;
import org.apache.fop.area.inline.ForeignObject;
import org.apache.fop.area.inline.Image;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.InlineParent;
import org.apache.fop.area.inline.Leader;
import org.apache.fop.area.inline.SpaceArea;
import org.apache.fop.area.inline.TextArea;
import org.apache.fop.area.inline.Viewport;
import org.apache.fop.area.inline.WordArea;
import org.apache.fop.datatypes.URISpecification;
import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.apache.fop.fo.extensions.xmp.XMPMetadata;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontTriplet;
import org.apache.fop.fonts.LazyFont;
import org.apache.fop.fonts.Typeface;
import org.apache.fop.render.AbstractPathOrientedRenderer;
import org.apache.fop.render.Renderer;
import org.apache.fop.render.intermediate.IFGraphicContext.Group;
import org.apache.fop.render.intermediate.extensions.AbstractAction;
import org.apache.fop.render.intermediate.extensions.ActionSet;
import org.apache.fop.render.intermediate.extensions.Bookmark;
import org.apache.fop.render.intermediate.extensions.BookmarkTree;
import org.apache.fop.render.intermediate.extensions.GoToXYAction;
import org.apache.fop.render.intermediate.extensions.Link;
import org.apache.fop.render.intermediate.extensions.NamedDestination;
import org.apache.fop.render.intermediate.extensions.URIAction;
import org.apache.fop.render.pdf.PDFEventProducer;
import org.apache.fop.traits.BorderProps;
import org.apache.fop.traits.RuleStyle;
import org.apache.xmlgraphics.util.QName;
import org.apache.xmlgraphics.xmp.Metadata;
import org.apache.xmlgraphics.xmp.schemas.DublinCoreAdapter;
import org.apache.xmlgraphics.xmp.schemas.DublinCoreSchema;
import org.apache.xmlgraphics.xmp.schemas.XMPBasicAdapter;
import org.apache.xmlgraphics.xmp.schemas.XMPBasicSchema;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * This renderer implementation is an adapter to the {@link IFPainter}
 * interface. It is used to generate content using FOP's intermediate format.
 */
@Slf4j
public class IFRenderer extends AbstractPathOrientedRenderer {

    // TODO Many parts of the Renderer infrastructure are using floats
    // (coordinates in points)
    // instead of ints (in millipoints). A lot of conversion to and from is
    // performed.
    // When the new IF is established, the Renderer infrastructure should be
    // revisited so check
    // if optimizations can be done to avoid int->float->int conversions.

    /** XML MIME type */
    public static final String IF_MIME_TYPE = MimeConstants.MIME_FOP_IF;

    private IFDocumentHandler documentHandler;
    private IFPainter painter;

    /**
     * If not null, the XMLRenderer will mimic another renderer by using its
     * font setup.
     */
    protected Renderer mimic;

    private boolean inPageSequence = false;

    private final Stack<IFGraphicContext> graphicContextStack = new Stack<>();
    private final Stack<Dimension> viewportDimensionStack = new Stack<>();
    private IFGraphicContext graphicContext = new IFGraphicContext();
    // private Stack groupStack = new Stack();

    private Metadata documentMetadata;

    /**
     * Maps XSL-FO element IDs to their on-page XY-positions Must be used in
     * conjunction with the page reference to fully specify the details of a
     * "go-to" action.
     */
    private final Map<String, Point> idPositions = new HashMap<>();

    /**
     * The "go-to" actions in idGoTos that are not complete yet
     */
    private final List<GoToXYAction> unfinishedGoTos = new ArrayList<>();
    // can't use a Set because PDFGoTo.equals returns true if the target is the
    // same,
    // even if the object number differs

    /** Maps unique PageViewport key to page indices (for link target handling) */
    protected Map<String, Integer> pageIndices = new HashMap<>();

    private BookmarkTree bookmarkTree;
    private final List<NamedDestination> deferredDestinations = new ArrayList<>();
    private final List<Link> deferredLinks = new ArrayList<>();
    private final ActionSet actionSet = new ActionSet();

    private final TextUtil textUtil = new TextUtil();

    /**
     * Main constructor
     */
    public IFRenderer() {
    }

    /** {@inheritDoc} */
    @Override
    public String getMimeType() {
        return IF_MIME_TYPE;
    }

    /**
     * Sets the {@link IFDocumentHandler} to be used by the {@link IFRenderer}.
     *
     * @param documentHandler
     *            the {@link IFDocumentHandler}
     */
    public void setDocumentHandler(final IFDocumentHandler documentHandler) {
        this.documentHandler = documentHandler;
    }

    /** {@inheritDoc} */
    @Override
    public void setupFontInfo(final FontInfo inFontInfo) throws FOPException {
        if (this.documentHandler == null) {
            this.documentHandler = createDefaultDocumentHandler();
        }
        IFUtil.setupFonts(this.documentHandler, inFontInfo);
        this.fontInfo = inFontInfo;
    }

    private void handleIFException(final IFException ife) {
        if (ife.getCause() instanceof SAXException) {
            throw new RuntimeException(ife.getCause());
        } else {
            throw new RuntimeException(ife);
        }
    }

    private void handleIFExceptionWithIOException(final IFException ife)
            throws IOException {
        if (ife.getCause() instanceof IOException) {
            throw (IOException) ife.getCause();
        } else {
            handleIFException(ife);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsOutOfOrder() {
        return this.documentHandler != null ? this.documentHandler
                .supportsPagesOutOfOrder() : false;
    }

    /**
     * Returns the document navigation handler if available/supported.
     *
     * @return the document navigation handler or null if not supported
     */
    protected IFDocumentNavigationHandler getDocumentNavigationHandler() {
        return this.documentHandler.getDocumentNavigationHandler();
    }

    /**
     * Indicates whether document navigation features are supported by the
     * document handler.
     *
     * @return true if document navigation features are available
     */
    protected boolean hasDocumentNavigation() {
        return getDocumentNavigationHandler() != null;
    }

    /**
     * Creates a default {@link IFDocumentHandler} when none has been set.
     *
     * @return the default IFDocumentHandler
     */
    protected IFDocumentHandler createDefaultDocumentHandler() {
        final IFSerializer serializer = new IFSerializer();
        serializer.setContext(new IFContext(getUserAgent()));
        return serializer;
    }

    /** {@inheritDoc} */
    @Override
    public void startRenderer(final OutputStream outputStream)
            throws IOException {
        try {
            if (outputStream != null) {
                final StreamResult result = new StreamResult(outputStream);
                if (getUserAgent().getOutputFile() != null) {
                    result.setSystemId(getUserAgent().getOutputFile().toURI()
                            .toURL().toExternalForm());
                }
                if (this.documentHandler == null) {
                    this.documentHandler = createDefaultDocumentHandler();
                }
                this.documentHandler.setResult(result);
            }
            super.startRenderer(null);
            if (log.isDebugEnabled()) {
                log.debug("Rendering areas via IF document handler ("
                        + this.documentHandler.getClass().getName() + ")...");
            }
            this.documentHandler.startDocument();
            this.documentHandler.startDocumentHeader();
        } catch (final IFException e) {
            handleIFExceptionWithIOException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void stopRenderer() throws IOException {
        try {
            if (this.inPageSequence) {
                this.documentHandler.endPageSequence();
                this.inPageSequence = false;
            }
            this.documentHandler.startDocumentTrailer();

            // Wrap up document navigation
            if (hasDocumentNavigation()) {
                finishOpenGoTos();
                final Iterator<NamedDestination> iter = this.deferredDestinations
                        .iterator();
                while (iter.hasNext()) {
                    final NamedDestination dest = iter.next();
                    iter.remove();
                    getDocumentNavigationHandler().renderNamedDestination(dest);
                }

                if (this.bookmarkTree != null) {
                    getDocumentNavigationHandler().renderBookmarkTree(
                            this.bookmarkTree);
                }
            }

            this.documentHandler.endDocumentTrailer();
            this.documentHandler.endDocument();
        } catch (final IFException e) {
            handleIFExceptionWithIOException(e);
        }
        this.pageIndices.clear();
        this.idPositions.clear();
        this.actionSet.clear();
        super.stopRenderer();
        log.debug("Rendering finished.");
    }

    /** {@inheritDoc} */
    @Override
    public void processOffDocumentItem(final OffDocumentItem odi) {
        if (odi instanceof DestinationData) {
            // render Destinations
            renderDestination((DestinationData) odi);
        } else if (odi instanceof BookmarkData) {
            // render Bookmark-Tree
            renderBookmarkTree((BookmarkData) odi);
        } else if (odi instanceof OffDocumentExtensionAttachment) {
            final ExtensionAttachment attachment = ((OffDocumentExtensionAttachment) odi)
                    .getAttachment();
            if (XMPMetadata.CATEGORY.equals(attachment.getCategory())) {
                renderXMPMetadata((XMPMetadata) attachment);
            } else {
                try {
                    this.documentHandler.handleExtensionObject(attachment);
                } catch (final IFException ife) {
                    handleIFException(ife);
                }
            }
        }
    }

    private void renderDestination(final DestinationData dd) {
        if (!hasDocumentNavigation()) {
            return;
        }
        final String targetID = dd.getIDRef();
        if (targetID == null || targetID.length() == 0) {
            throw new IllegalArgumentException(
                    "DestinationData must contain a ID reference");
        }
        final PageViewport pv = dd.getPageViewport();
        if (pv != null) {
            final GoToXYAction action = getGoToActionForID(targetID,
                    pv.getPageIndex());
            final NamedDestination namedDestination = new NamedDestination(
                    targetID, action);
            this.deferredDestinations.add(namedDestination);
        } else {
            // Warning already issued by AreaTreeHandler (debug level is
            // sufficient)
            log.debug("Unresolved destination item received: " + dd.getIDRef());
        }
    }

    /**
     * Renders a Bookmark-Tree object
     *
     * @param bookmarks
     *            the BookmarkData object containing all the Bookmark-Items
     */
    protected void renderBookmarkTree(final BookmarkData bookmarks) {
        assert this.bookmarkTree == null;
        if (!hasDocumentNavigation()) {
            return;
        }
        this.bookmarkTree = new BookmarkTree();
        for (int i = 0; i < bookmarks.getCount(); ++i) {
            final BookmarkData ext = bookmarks.getSubData(i);
            final Bookmark b = renderBookmarkItem(ext);
            this.bookmarkTree.addBookmark(b);
        }
    }

    private Bookmark renderBookmarkItem(final BookmarkData bookmarkItem) {

        final String targetID = bookmarkItem.getIDRef();
        if (targetID == null || targetID.length() == 0) {
            throw new IllegalArgumentException(
                    "DestinationData must contain a ID reference");
        }
        GoToXYAction action = null;
        final PageViewport pv = bookmarkItem.getPageViewport();

        if (pv != null) {
            action = getGoToActionForID(targetID, pv.getPageIndex());
        } else {
            // Warning already issued by AreaTreeHandler (debug level is
            // sufficient)
            log.debug("Bookmark with IDRef \"" + targetID
                    + "\" has a null PageViewport.");
        }

        final Bookmark b = new Bookmark(bookmarkItem.getBookmarkTitle(),
                bookmarkItem.showChildItems(), action);
        for (int i = 0; i < bookmarkItem.getCount(); ++i) {
            b.addChildBookmark(renderBookmarkItem(bookmarkItem.getSubData(i)));
        }
        return b;
    }

    private void renderXMPMetadata(final XMPMetadata metadata) {
        this.documentMetadata = metadata.getMetadata();
    }

    private GoToXYAction getGoToActionForID(final String targetID,
            final int pageIndex) {
        // Already a GoToXY present for this target? If not, create.
        GoToXYAction action = (GoToXYAction) this.actionSet.get(targetID);
        // GoToXYAction action = (GoToXYAction)idGoTos.get(targetID);
        if (action == null) {
            if (pageIndex < 0) {
                // pageIndex = page
            }
            final Point position = this.idPositions.get(targetID);
            // can the GoTo already be fully filled in?
            if (pageIndex >= 0 && position != null) {
                action = new GoToXYAction(targetID, pageIndex, position);
            } else {
                // Not complete yet, can't use getPDFGoTo:
                action = new GoToXYAction(targetID, pageIndex, null);
                this.unfinishedGoTos.add(action);
            }
            action = (GoToXYAction) this.actionSet.put(action);
            // idGoTos.put(targetID, action);
        }
        return action;
    }

    private void finishOpenGoTos() {
        final int count = this.unfinishedGoTos.size();
        if (count > 0) {
            final Point defaultPos = new Point(0, 0); // top-o-page
            while (!this.unfinishedGoTos.isEmpty()) {
                final GoToXYAction action = this.unfinishedGoTos.get(0);
                noteGoToPosition(action, defaultPos);
            }
            final PDFEventProducer eventProducer = PDFEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.nonFullyResolvedLinkTargets(this, count);
            // dysfunctional if pageref is null
        }
    }

    private void noteGoToPosition(final GoToXYAction action,
            final Point position) {
        action.setTargetLocation(position);
        try {
            getDocumentNavigationHandler().addResolvedAction(action);
        } catch (final IFException ife) {
            handleIFException(ife);
        }
        this.unfinishedGoTos.remove(action);
    }

    private void noteGoToPosition(final GoToXYAction action,
            final PageViewport pv, final Point position) {
        action.setPageIndex(pv.getPageIndex());
        noteGoToPosition(action, position);
    }

    private void saveAbsolutePosition(final String id, final PageViewport pv,
            final int relativeIPP, final int relativeBPP,
            final AffineTransform tf) {
        final Point position = new Point(relativeIPP, relativeBPP);
        tf.transform(position, position);
        this.idPositions.put(id, position);
        // is there already a GoTo action waiting to be completed?
        final GoToXYAction action = (GoToXYAction) this.actionSet.get(id);
        if (action != null) {
            noteGoToPosition(action, pv, position);
        }
    }

    private void saveAbsolutePosition(final String id, final int relativeIPP,
            final int relativeBPP) {
        saveAbsolutePosition(id, this.currentPageViewport, relativeIPP,
                relativeBPP, this.graphicContext.getTransform());
    }

    private void saveBlockPosIfTargetable(final Block block) {
        final String id = getTargetableID(block);
        if (hasDocumentNavigation() && id != null) {
            // FIXME: Like elsewhere in the renderer code, absolute and relative
            // directions are happily mixed here. This makes sure that the
            // links point to the right location, but it is not correct.
            int ipp = block.getXOffset();
            int bpp = block.getYOffset() + block.getSpaceBefore();
            final int positioning = block.getPositioning();
            if (!(positioning == Block.FIXED || positioning == Block.ABSOLUTE)) {
                ipp += this.currentIPPosition;
                bpp += this.currentBPPosition;
            }
            saveAbsolutePosition(id, this.currentPageViewport, ipp, bpp,
                    this.graphicContext.getTransform());
        }
    }

    private void saveInlinePosIfTargetable(final InlineArea inlineArea) {
        final String id = getTargetableID(inlineArea);
        if (hasDocumentNavigation() && id != null) {
            final int extraMarginBefore = 5000; // millipoints
            final int ipp = this.currentIPPosition;
            final int bpp = this.currentBPPosition + inlineArea.getOffset()
                    - extraMarginBefore;
            saveAbsolutePosition(id, ipp, bpp);
        }
    }

    private String getTargetableID(final Area area) {
        final String id = (String) area.getTrait(Trait.PROD_ID);
        if (id == null || id.length() == 0
                || !this.currentPageViewport.isFirstWithID(id)
                || this.idPositions.containsKey(id)) {
            return null;
        } else {
            return id;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startPageSequence(final PageSequence pageSequence) {
        try {
            if (this.inPageSequence) {
                this.documentHandler.endPageSequence();
                this.documentHandler.getContext().setLanguage(null);
            } else {
                if (this.documentMetadata == null) {
                    this.documentMetadata = createDefaultDocumentMetadata();
                }
                this.documentHandler
                .handleExtensionObject(this.documentMetadata);
                this.documentHandler.endDocumentHeader();
                this.inPageSequence = true;
            }
            establishForeignAttributes(pageSequence.getForeignAttributes());
            this.documentHandler.getContext().setLanguage(
                    toLocale(pageSequence));
            this.documentHandler.startPageSequence(null);
            resetForeignAttributes();
            processExtensionAttachments(pageSequence);
        } catch (final IFException e) {
            handleIFException(e);
        }
    }

    private Locale toLocale(final PageSequence pageSequence) {
        if (pageSequence.getLanguage() != null) {
            if (pageSequence.getCountry() != null) {
                return new Locale(pageSequence.getLanguage(),
                        pageSequence.getCountry());
            } else {
                return new Locale(pageSequence.getLanguage());
            }
        }
        return null;
    }

    private Metadata createDefaultDocumentMetadata() {
        final Metadata xmp = new Metadata();
        final DublinCoreAdapter dc = DublinCoreSchema.getAdapter(xmp);
        if (getUserAgent().getTitle() != null) {
            dc.setTitle(getUserAgent().getTitle());
        }
        if (getUserAgent().getAuthor() != null) {
            dc.addCreator(getUserAgent().getAuthor());
        }
        if (getUserAgent().getKeywords() != null) {
            dc.addSubject(getUserAgent().getKeywords());
        }
        final XMPBasicAdapter xmpBasic = XMPBasicSchema.getAdapter(xmp);
        if (getUserAgent().getProducer() != null) {
            xmpBasic.setCreatorTool(getUserAgent().getProducer());
        } else {
            xmpBasic.setCreatorTool(Version.getVersion());
        }
        xmpBasic.setMetadataDate(new Date());
        if (getUserAgent().getCreationDate() != null) {
            xmpBasic.setCreateDate(getUserAgent().getCreationDate());
        } else {
            xmpBasic.setCreateDate(xmpBasic.getMetadataDate());
        }
        return xmp;
    }

    /** {@inheritDoc} */
    @Override
    public void preparePage(final PageViewport page) {
        super.preparePage(page);
    }

    /** {@inheritDoc} */
    @Override
    public void renderPage(final PageViewport page) throws IOException {
        log.trace("renderPage() {}", page);
        try {
            this.pageIndices.put(page.getKey(), page.getPageIndex());
            final Rectangle viewArea = page.getViewArea();
            final Dimension dim = new Dimension(viewArea.width, viewArea.height);

            establishForeignAttributes(page.getForeignAttributes());
            this.documentHandler.startPage(page.getPageIndex(),
                    page.getPageNumberString(), page.getSimplePageMasterName(),
                    dim);
            resetForeignAttributes();
            this.documentHandler.startPageHeader();

            // Add page attachments to page header
            processExtensionAttachments(page);

            this.documentHandler.endPageHeader();
            this.painter = this.documentHandler.startPageContent();
            super.renderPage(page);
            this.painter = null;
            this.documentHandler.endPageContent();

            this.documentHandler.startPageTrailer();
            if (hasDocumentNavigation()) {
                final Iterator<Link> iter = this.deferredLinks.iterator();
                while (iter.hasNext()) {
                    final Link link = iter.next();
                    iter.remove();
                    getDocumentNavigationHandler().renderLink(link);
                }
            }
            this.documentHandler.endPageTrailer();

            establishForeignAttributes(page.getForeignAttributes());
            this.documentHandler.endPage();
            resetForeignAttributes();
        } catch (final IFException e) {
            handleIFException(e);
        }
    }

    private void processExtensionAttachments(final AreaTreeObject area)
            throws IFException {
        if (area.hasExtensionAttachments()) {
            for (final ExtensionAttachment attachment : area
                    .getExtensionAttachments()) {
                this.documentHandler.handleExtensionObject(attachment);
            }
        }
    }

    private void establishForeignAttributes(
            final Map<QName, String> foreignAttributes) {
        this.documentHandler.getContext().setForeignAttributes(
                foreignAttributes);
    }

    private void resetForeignAttributes() {
        this.documentHandler.getContext().resetForeignAttributes();
    }

    private void establishStructurePointer(final String ptr) {
        this.documentHandler.getContext().setStructurePointer(ptr);
    }

    private void resetStructurePointer() {
        this.documentHandler.getContext().resetStructurePointer();
    }

    /** {@inheritDoc} */
    @Override
    protected void saveGraphicsState() {
        this.graphicContextStack.push(this.graphicContext);
        this.graphicContext = (IFGraphicContext) this.graphicContext.clone();
    }

    /** {@inheritDoc} */
    @Override
    protected void restoreGraphicsState() {
        while (this.graphicContext.getGroupStackSize() > 0) {
            final IFGraphicContext.Group[] groups = this.graphicContext
                    .dropGroups();
            for (int i = groups.length - 1; i >= 0; i--) {
                try {
                    groups[i].end(this.painter);
                } catch (final IFException ife) {
                    handleIFException(ife);
                }
            }
        }
        this.graphicContext = this.graphicContextStack.pop();
    }

    private void pushGroup(final IFGraphicContext.Group group) {
        this.graphicContext.pushGroup(group);
        try {
            group.start(this.painter);
        } catch (final IFException ife) {
            handleIFException(ife);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected List<IFGraphicContext> breakOutOfStateStack() {
        log.debug("Block.FIXED --> break out");
        final List<IFGraphicContext> breakOutList = new ArrayList<>();
        while (!this.graphicContextStack.empty()) {
            // Handle groups
            final IFGraphicContext.Group[] groups = this.graphicContext
                    .getGroups();
            for (int j = groups.length - 1; j >= 0; j--) {
                try {
                    groups[j].end(this.painter);
                } catch (final IFException ife) {
                    handleIFException(ife);
                }
            }

            breakOutList.add(0, this.graphicContext);
            this.graphicContext = this.graphicContextStack.pop();
        }
        return breakOutList;
    }

    /** {@inheritDoc} */
    @Override
    protected void restoreStateStackAfterBreakOut(final List breakOutList) {
        log.debug("Block.FIXED --> restoring context after break-out");
        for (int i = 0, c = breakOutList.size(); i < c; ++i) {
            this.graphicContextStack.push(this.graphicContext);
            this.graphicContext = (IFGraphicContext) breakOutList.get(i);

            // Handle groups
            final IFGraphicContext.Group[] groups = this.graphicContext
                    .getGroups();
            for (final Group group : groups) {
                try {
                    group.start(this.painter);
                } catch (final IFException ife) {
                    handleIFException(ife);
                }
            }
        }
        log.debug("restored.");
    }

    /** {@inheritDoc} */
    @Override
    protected void concatenateTransformationMatrix(final AffineTransform at) {
        if (!at.isIdentity()) {
            concatenateTransformationMatrixMpt(ptToMpt(at), false);
        }
    }

    private void concatenateTransformationMatrixMpt(final AffineTransform at,
            final boolean force) {
        if (force || !at.isIdentity()) {
            if (log.isTraceEnabled()) {
                log.trace("-----concatenateTransformationMatrix: " + at);
            }
            final IFGraphicContext.Group group = new IFGraphicContext.Group(at);
            pushGroup(group);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void beginTextObject() {
        // nop - Ignore, handled by painter internally
    }

    /** {@inheritDoc} */
    @Override
    protected void endTextObject() {
        // nop - Ignore, handled by painter internally
    }

    /** {@inheritDoc} */
    @Override
    protected void renderRegionViewport(final RegionViewport viewport) {
        final Dimension dim = new Dimension(viewport.getIPD(),
                viewport.getBPD());
        this.viewportDimensionStack.push(dim);
        super.renderRegionViewport(viewport);
        this.viewportDimensionStack.pop();
    }

    /** {@inheritDoc} */
    @Override
    protected void renderBlockViewport(final BlockViewport bv,
            final List children) {
        // Essentially the same code as in the super class but optimized for the
        // IF

        // This is the content-rect
        final Dimension dim = new Dimension(bv.getIPD(), bv.getBPD());
        this.viewportDimensionStack.push(dim);

        // save positions
        final int saveIP = this.currentIPPosition;
        final int saveBP = this.currentBPPosition;

        CTM ctm = bv.getCTM();
        final int borderPaddingStart = bv.getBorderAndPaddingWidthStart();
        final int borderPaddingBefore = bv.getBorderAndPaddingWidthBefore();

        if (bv.getPositioning() == Block.ABSOLUTE
                || bv.getPositioning() == Block.FIXED) {

            // For FIXED, we need to break out of the current viewports to the
            // one established by the page. We save the state stack for
            // restoration
            // after the block-container has been painted. See below.
            List<IFGraphicContext> breakOutList = null;
            if (bv.getPositioning() == Block.FIXED) {
                breakOutList = breakOutOfStateStack();
            }

            final AffineTransform positionTransform = new AffineTransform();
            positionTransform.translate(bv.getXOffset(), bv.getYOffset());

            // "left/"top" (bv.getX/YOffset()) specify the position of the
            // content rectangle
            positionTransform.translate(-borderPaddingStart,
                    -borderPaddingBefore);

            // Free transformation for the block-container viewport
            String transf;
            transf = bv.getForeignAttributeValue(FOX_TRANSFORM);
            if (transf != null) {
                final AffineTransform freeTransform = AWTTransformProducer
                        .createAffineTransform(transf);
                positionTransform.concatenate(freeTransform);
            }

            saveGraphicsState();
            // Viewport position
            concatenateTransformationMatrixMpt(positionTransform, false);

            // Background and borders
            final float bpwidth = borderPaddingStart
                    + bv.getBorderAndPaddingWidthEnd();
            final float bpheight = borderPaddingBefore
                    + bv.getBorderAndPaddingWidthAfter();
            drawBackAndBorders(bv, 0, 0, (dim.width + bpwidth) / 1000f,
                    (dim.height + bpheight) / 1000f);

            // Shift to content rectangle after border painting
            final AffineTransform contentRectTransform = new AffineTransform();
            contentRectTransform.translate(borderPaddingStart,
                    borderPaddingBefore);
            concatenateTransformationMatrixMpt(contentRectTransform, false);

            // Clipping
            Rectangle clipRect = null;
            if (bv.getClip()) {
                clipRect = new Rectangle(0, 0, dim.width, dim.height);
                // clipRect(0f, 0f, width, height);
            }

            // saveGraphicsState();
            // Set up coordinate system for content rectangle
            final AffineTransform contentTransform = ctm.toAffineTransform();
            // concatenateTransformationMatrixMpt(contentTransform);
            startViewport(contentTransform, clipRect);

            this.currentIPPosition = 0;
            this.currentBPPosition = 0;
            renderBlocks(bv, children);

            endViewport();
            // restoreGraphicsState();
            restoreGraphicsState();

            if (breakOutList != null) {
                restoreStateStackAfterBreakOut(breakOutList);
            }

            this.currentIPPosition = saveIP;
            this.currentBPPosition = saveBP;
        } else {

            this.currentBPPosition += bv.getSpaceBefore();

            // borders and background in the old coordinate system
            handleBlockTraits(bv);

            // Advance to start of content area
            this.currentIPPosition += bv.getStartIndent();

            final CTM tempctm = new CTM(this.containingIPPosition,
                    this.currentBPPosition);
            ctm = tempctm.multiply(ctm);

            // Now adjust for border/padding
            this.currentBPPosition += borderPaddingBefore;

            Rectangle2D clippingRect = null;
            if (bv.getClip()) {
                clippingRect = new Rectangle(this.currentIPPosition,
                        this.currentBPPosition, bv.getIPD(), bv.getBPD());
            }

            startVParea(ctm, clippingRect);
            this.currentIPPosition = 0;
            this.currentBPPosition = 0;
            renderBlocks(bv, children);
            endVParea();

            this.currentIPPosition = saveIP;
            this.currentBPPosition = saveBP;

            this.currentBPPosition += bv.getAllocBPD();
        }
        this.viewportDimensionStack.pop();
    }

    /** {@inheritDoc} */
    @Override
    public void renderViewport(final Viewport viewport) {
        final String ptr = (String) viewport.getTrait(Trait.PTR);
        establishStructurePointer(ptr);
        final Dimension dim = new Dimension(viewport.getIPD(),
                viewport.getBPD());
        this.viewportDimensionStack.push(dim);
        super.renderViewport(viewport);
        this.viewportDimensionStack.pop();
        resetStructurePointer();
    }

    /** {@inheritDoc} */
    @Override
    protected void startVParea(final CTM ctm, final Rectangle2D clippingRect) {
        if (log.isTraceEnabled()) {
            log.trace("startVParea() ctm=" + ctm + ", clippingRect="
                    + clippingRect);
        }
        final AffineTransform at = new AffineTransform(ctm.toArray());
        Rectangle clipRect = null;
        if (clippingRect != null) {
            clipRect = new Rectangle((int) clippingRect.getMinX()
                    - this.currentIPPosition, (int) clippingRect.getMinY()
                    - this.currentBPPosition, (int) clippingRect.getWidth(),
                    (int) clippingRect.getHeight());
        }
        startViewport(at, clipRect);
        if (log.isTraceEnabled()) {
            log.trace("startVPArea: " + at + " --> "
                    + this.graphicContext.getTransform());
        }
    }

    private void startViewport(final AffineTransform at,
            final Rectangle clipRect) {
        saveGraphicsState();
        try {
            final IFGraphicContext.Viewport viewport = new IFGraphicContext.Viewport(
                    at, this.viewportDimensionStack.peek(), clipRect);
            this.graphicContext.pushGroup(viewport);
            viewport.start(this.painter);
        } catch (final IFException e) {
            handleIFException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void endVParea() {
        log.trace("endVParea()");
        endViewport();
        if (log.isTraceEnabled()) {
            log.trace("endVPArea() --> " + this.graphicContext.getTransform());
        }
    }

    private void endViewport() {
        restoreGraphicsState();
    }

    /** {@inheritDoc} */
    @Override
    protected void renderInlineArea(final InlineArea inlineArea) {
        saveInlinePosIfTargetable(inlineArea);
        super.renderInlineArea(inlineArea);
    }

    /** {@inheritDoc} */
    @Override
    public void renderInlineParent(final InlineParent ip) {
        // stuff we only need if a link must be created:
        Rectangle ipRect = null;
        AbstractAction action = null;
        final String ptr = (String) ip.getTrait(Trait.PTR); // used for
        // accessibility
        // make sure the rect is determined *before* calling super!
        final int ipp = this.currentIPPosition;
        final int bpp = this.currentBPPosition + ip.getOffset();
        ipRect = new Rectangle(ipp, bpp, ip.getIPD(), ip.getBPD());
        final AffineTransform transform = this.graphicContext.getTransform();
        ipRect = transform.createTransformedShape(ipRect).getBounds();

        // render contents
        super.renderInlineParent(ip);

        boolean linkTraitFound = false;

        // try INTERNAL_LINK first
        final Trait.InternalLink intLink = (Trait.InternalLink) ip
                .getTrait(Trait.INTERNAL_LINK);
        if (intLink != null) {
            linkTraitFound = true;
            final String pvKey = intLink.getPVKey();
            final String idRef = intLink.getIDRef();
            final boolean pvKeyOK = pvKey != null && pvKey.length() > 0;
            final boolean idRefOK = idRef != null && idRef.length() > 0;
            if (pvKeyOK && idRefOK) {
                final Integer pageIndex = this.pageIndices.get(pvKey);
                action = getGoToActionForID(idRef,
                        pageIndex != null ? pageIndex.intValue() : -1);
            } else {
                // Warnings already issued by AreaTreeHandler
            }
        }

        // no INTERNAL_LINK, look for EXTERNAL_LINK
        if (!linkTraitFound) {
            final Trait.ExternalLink extLink = (Trait.ExternalLink) ip
                    .getTrait(Trait.EXTERNAL_LINK);
            if (extLink != null) {
                final String extDest = extLink.getDestination();
                if (extDest != null && extDest.length() > 0) {
                    linkTraitFound = true;
                    action = new URIAction(extDest, extLink.newWindow());
                    action = this.actionSet.put(action);
                }
            }
        }

        // warn if link trait found but not allowed, else create link
        if (linkTraitFound) {
            action.setStructurePointer(ptr); // used for accessibility
            final Link link = new Link(action, ipRect);
            this.deferredLinks.add(link);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void renderBlock(final Block block) {
        if (log.isTraceEnabled()) {
            log.trace("renderBlock() " + block);
        }
        saveBlockPosIfTargetable(block);
        super.renderBlock(block);
    }

    private Typeface getTypeface(final String fontName) {
        Typeface tf = (Typeface) this.fontInfo.getFonts().get(fontName);
        if (tf instanceof LazyFont) {
            tf = ((LazyFont) tf).getRealFont();
        }
        return tf;
    }

    /** {@inheritDoc} */
    @Override
    protected void renderText(final TextArea text) {
        if (log.isTraceEnabled()) {
            log.trace("renderText() " + text);
        }
        renderInlineAreaBackAndBorders(text);
        final Color ct = (Color) text.getTrait(Trait.COLOR);

        beginTextObject();

        final String fontName = getInternalFontNameForArea(text);
        final int size = ((Integer) text.getTrait(Trait.FONT_SIZE)).intValue();
        final String ptr = (String) text.getTrait(Trait.PTR); // used for
        // accessibility
        establishStructurePointer(ptr);

        // This assumes that *all* CIDFonts use a /ToUnicode mapping
        final Typeface tf = getTypeface(fontName);

        final FontTriplet triplet = (FontTriplet) text.getTrait(Trait.FONT);
        try {
            this.painter.setFont(triplet.getName(), triplet.getStyle(),
                    triplet.getWeight(), "normal", size, ct);
        } catch (final IFException e) {
            handleIFException(e);
        }

        final int rx = this.currentIPPosition
                + text.getBorderAndPaddingWidthStart();
        final int bl = this.currentBPPosition + text.getOffset()
                + text.getBaselineOffset();
        this.textUtil.flush();
        this.textUtil.setStartPosition(rx, bl);
        this.textUtil.setSpacing(text.getTextLetterSpaceAdjust(),
                text.getTextWordSpaceAdjust());
        super.renderText(text);

        this.textUtil.flush();
        renderTextDecoration(tf, size, text, bl, rx);
        resetStructurePointer();
    }

    /** {@inheritDoc} */
    @Override
    protected void renderWord(final WordArea word) {
        final Font font = getFontFromArea(word.getParentArea());
        final String s = word.getWord();

        renderText(s, word.getLetterAdjustArray(), font,
                (AbstractTextArea) word.getParentArea());

        super.renderWord(word);
    }

    /** {@inheritDoc} */
    @Override
    protected void renderSpace(final SpaceArea space) {
        final Font font = getFontFromArea(space.getParentArea());
        final String s = space.getSpace();

        final AbstractTextArea textArea = (AbstractTextArea) space
                .getParentArea();
        renderText(s, null, font, textArea);

        if (this.textUtil.combined && space.isAdjustable()) {
            // Used for justified text, for example
            final int tws = textArea.getTextWordSpaceAdjust() + 2
                    * textArea.getTextLetterSpaceAdjust();
            if (tws != 0) {
                this.textUtil.adjust(tws);
            }
        }
        super.renderSpace(space);
    }

    /**
     * Does low-level rendering of text.
     *
     * @param s
     *            text to render
     * @param letterAdjust
     *            an array of widths for letter adjustment (may be null)
     * @param font
     *            to font in use
     * @param parentArea
     *            the parent text area to retrieve certain traits from
     */
    protected void renderText(final String s, final int[] letterAdjust,
            final Font font, final AbstractTextArea parentArea) {
        final int l = s.length();
        if (l == 0) {
            return;
        }

        if (letterAdjust != null) {
            this.textUtil.adjust(letterAdjust[0]);
        }
        for (int i = 0; i < l; ++i) {
            final char ch = s.charAt(i);
            this.textUtil.addChar(ch);
            int glyphAdjust = 0;
            if (this.textUtil.combined && font.hasChar(ch)) {
                final int tls = i < l - 1 ? parentArea
                        .getTextLetterSpaceAdjust() : 0;
                glyphAdjust += tls;
            }
            if (letterAdjust != null && i < l - 1) {
                glyphAdjust += letterAdjust[i + 1];
            }

            this.textUtil.adjust(glyphAdjust);
        }
    }

    private class TextUtil {
        private static final int INITIAL_BUFFER_SIZE = 16;
        private int[] dx = new int[INITIAL_BUFFER_SIZE];
        private int lastDXPos = 0;
        private final StringBuilder text = new StringBuilder();
        private int startx, starty;
        private int tls, tws;
        private final boolean combined = false;

        void addChar(final char ch) {
            this.text.append(ch);
        }

        void adjust(final int adjust) {
            if (adjust != 0) {
                final int idx = this.text.length();
                if (idx > this.dx.length - 1) {
                    final int newSize = Math.max(this.dx.length, idx + 1)
                            + INITIAL_BUFFER_SIZE;
                    final int[] newDX = new int[newSize];
                    System.arraycopy(this.dx, 0, newDX, 0, this.dx.length);
                    this.dx = newDX;
                }
                this.dx[idx] += adjust;
                this.lastDXPos = idx;
            }
        }

        void reset() {
            if (this.text.length() > 0) {
                this.text.setLength(0);
                Arrays.fill(this.dx, 0);
                this.lastDXPos = 0;
            }
        }

        void setStartPosition(final int x, final int y) {
            this.startx = x;
            this.starty = y;
        }

        void setSpacing(final int tls, final int tws) {
            this.tls = tls;
            this.tws = tws;
        }

        void flush() {
            if (this.text.length() > 0) {
                try {
                    int[] effDX = null;
                    if (this.lastDXPos > 0) {
                        final int size = this.lastDXPos + 1;
                        effDX = new int[size];
                        System.arraycopy(this.dx, 0, effDX, 0, size);
                    }
                    if (this.combined) {
                        IFRenderer.this.painter.drawText(this.startx,
                                this.starty, 0, 0, effDX, this.text.toString());
                    } else {
                        IFRenderer.this.painter.drawText(this.startx,
                                this.starty, this.tls, this.tws, effDX,
                                this.text.toString());
                    }
                } catch (final IFException e) {
                    handleIFException(e);
                }
                reset();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void renderImage(final Image image, final Rectangle2D pos) {
        drawImage(image.getURL(), pos, image.getForeignAttributes());
    }

    /** {@inheritDoc} */
    @Override
    protected void drawImage(String uri, final Rectangle2D pos,
            final Map foreignAttributes) {
        final Rectangle posInt = new Rectangle(this.currentIPPosition
                + (int) pos.getX(), this.currentBPPosition + (int) pos.getY(),
                (int) pos.getWidth(), (int) pos.getHeight());
        uri = URISpecification.getURL(uri);
        try {
            establishForeignAttributes(foreignAttributes);
            this.painter.drawImage(uri, posInt);
            resetForeignAttributes();
        } catch (final IFException ife) {
            handleIFException(ife);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void renderForeignObject(final ForeignObject fo,
            final Rectangle2D pos) {
        endTextObject();
        final Rectangle posInt = new Rectangle(this.currentIPPosition
                + (int) pos.getX(), this.currentBPPosition + (int) pos.getY(),
                (int) pos.getWidth(), (int) pos.getHeight());
        final Document doc = fo.getDocument();
        try {
            establishForeignAttributes(fo.getForeignAttributes());
            this.painter.drawImage(doc, posInt);
            resetForeignAttributes();
        } catch (final IFException ife) {
            handleIFException(ife);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void renderLeader(final Leader area) {
        renderInlineAreaBackAndBorders(area);

        final int style = area.getRuleStyle();
        final int ruleThickness = area.getRuleThickness();
        final int startx = this.currentIPPosition
                + area.getBorderAndPaddingWidthStart();
        final int starty = this.currentBPPosition + area.getOffset()
                + ruleThickness / 2;
        final int endx = this.currentIPPosition
                + area.getBorderAndPaddingWidthStart() + area.getIPD();
        final Color col = (Color) area.getTrait(Trait.COLOR);

        final Point start = new Point(startx, starty);
        final Point end = new Point(endx, starty);
        try {
            this.painter.drawLine(start, end, ruleThickness, col,
                    RuleStyle.valueOf(style));
        } catch (final IFException ife) {
            handleIFException(ife);
        }

        super.renderLeader(area);
    }

    /** {@inheritDoc} */
    @Override
    protected void clip() {
        throw new IllegalStateException("Not used");
    }

    /** {@inheritDoc} */
    @Override
    protected void clipRect(final float x, final float y, final float width,
            final float height) {
        pushGroup(new IFGraphicContext.Group());
        try {
            this.painter.clipRect(toMillipointRectangle(x, y, width, height));
        } catch (final IFException ife) {
            handleIFException(ife);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void closePath() {
        throw new IllegalStateException("Not used");
    }

    /** {@inheritDoc} */
    @Override
    protected void drawBorders(final float startx, final float starty,
            final float width, final float height, final BorderProps bpsBefore,
            final BorderProps bpsAfter, final BorderProps bpsStart,
            final BorderProps bpsEnd) {
        final Rectangle rect = toMillipointRectangle(startx, starty, width,
                height);
        try {
            this.painter.drawBorderRect(rect, bpsBefore, bpsAfter, bpsStart,
                    bpsEnd);
        } catch (final IFException ife) {
            handleIFException(ife);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void drawBorderLine(final float x1, final float y1,
            final float x2, final float y2, final boolean horz,
            final boolean startOrBefore, final int style, final Color col) {
        // Simplified implementation that is only used by renderTextDecoration()
        // drawBorders() is overridden and uses the Painter's high-level method
        // drawBorderRect()
        updateColor(col, true);
        fillRect(x1, y1, x2 - x1, y2 - y1);
    }

    private int toMillipoints(final float coordinate) {
        return Math.round(coordinate * 1000);
    }

    private Rectangle toMillipointRectangle(final float x, final float y,
            final float width, final float height) {
        return new Rectangle(toMillipoints(x), toMillipoints(y),
                toMillipoints(width), toMillipoints(height));
    }

    /** {@inheritDoc} */
    @Override
    protected void fillRect(final float x, final float y, final float width,
            final float height) {
        try {
            this.painter.fillRect(toMillipointRectangle(x, y, width, height),
                    this.graphicContext.getPaint());
        } catch (final IFException e) {
            handleIFException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void moveTo(final float x, final float y) {
        throw new IllegalStateException("Not used");
    }

    /** {@inheritDoc} */
    @Override
    protected void lineTo(final float x, final float y) {
        throw new IllegalStateException("Not used");
    }

    /** {@inheritDoc} */
    @Override
    protected void updateColor(final Color col, final boolean fill) {
        if (fill) {
            this.graphicContext.setPaint(col);
        } else {
            this.graphicContext.setColor(col);
        }

    }

}
