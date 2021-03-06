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

/* $Id: IFSerializer.java 834020 2009-11-09 11:21:52Z vhennebert $ */

package org.apache.fop.render.intermediate;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.fop.accessibility.StructureTree;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.render.PrintRendererConfigurator;
import org.apache.fop.render.RenderingContext;
import org.apache.fop.render.intermediate.extensions.AbstractAction;
import org.apache.fop.render.intermediate.extensions.Bookmark;
import org.apache.fop.render.intermediate.extensions.BookmarkTree;
import org.apache.fop.render.intermediate.extensions.DocumentNavigationExtensionConstants;
import org.apache.fop.render.intermediate.extensions.Link;
import org.apache.fop.render.intermediate.extensions.NamedDestination;
import org.apache.fop.traits.BorderProps;
import org.apache.fop.traits.RuleStyle;
import org.apache.fop.util.ColorUtil;
import org.apache.fop.util.DOM2SAX;
import org.apache.fop.util.XMLConstants;
import org.apache.fop.util.XMLUtil;
import org.apache.xmlgraphics.util.QName;
import org.apache.xmlgraphics.util.XMLizable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * IFPainter implementation that serializes the intermediate format to XML.
 */
public class IFSerializer extends AbstractXMLWritingIFDocumentHandler implements
IFConstants, IFPainter, IFDocumentNavigationHandler {

    private IFDocumentHandler mimicHandler;
    private int pageSequenceIndex; // used for accessibility

    /** Holds the intermediate format state */
    private IFState state;

    /**
     * Default constructor.
     */
    public IFSerializer() {
    }

    /** {@inheritDoc} */
    @Override
    protected String getMainNamespace() {
        return NAMESPACE;
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsPagesOutOfOrder() {
        return false;
        // Theoretically supported but disabled to improve performance when
        // rendering the IF to the final format later on
    }

    /** {@inheritDoc} */
    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    /** {@inheritDoc} */
    @Override
    public IFDocumentHandlerConfigurator getConfigurator() {
        if (this.mimicHandler != null) {
            return getMimickedDocumentHandler().getConfigurator();
        } else {
            return new PrintRendererConfigurator(getUserAgent());
        }
    }

    /** {@inheritDoc} */
    @Override
    public IFDocumentNavigationHandler getDocumentNavigationHandler() {
        return this;
    }

    /**
     * Tells this serializer to mimic the given document handler (mostly applies
     * to the font set that is used during layout).
     *
     * @param targetHandler
     *            the document handler to mimic
     */
    public void mimicDocumentHandler(final IFDocumentHandler targetHandler) {
        this.mimicHandler = targetHandler;
    }

    /**
     * Returns the document handler that is being mimicked by this serializer.
     *
     * @return the mimicked document handler or null if no such document handler
     *         has been set
     */
    public IFDocumentHandler getMimickedDocumentHandler() {
        return this.mimicHandler;
    }

    /** {@inheritDoc} */
    @Override
    public FontInfo getFontInfo() {
        if (this.mimicHandler != null) {
            return this.mimicHandler.getFontInfo();
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setFontInfo(final FontInfo fontInfo) {
        if (this.mimicHandler != null) {
            this.mimicHandler.setFontInfo(fontInfo);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultFontInfo(final FontInfo fontInfo) {
        if (this.mimicHandler != null) {
            this.mimicHandler.setDefaultFontInfo(fontInfo);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startDocument() throws IFException {
        super.startDocument();
        try {
            this.handler.startDocument();
            this.handler.startPrefixMapping("", NAMESPACE);
            this.handler.startPrefixMapping(XLINK_PREFIX, XLINK_NAMESPACE);
            this.handler.startPrefixMapping(
                    DocumentNavigationExtensionConstants.PREFIX,
                    DocumentNavigationExtensionConstants.NAMESPACE);
            this.handler.startElement(EL_DOCUMENT);
        } catch (final SAXException e) {
            throw new IFException("SAX error in startDocument()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startDocumentHeader() throws IFException {
        try {
            this.handler.startElement(EL_HEADER);
        } catch (final SAXException e) {
            throw new IFException("SAX error in startDocumentHeader()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endDocumentHeader() throws IFException {
        try {
            this.handler.endElement(EL_HEADER);
        } catch (final SAXException e) {
            throw new IFException("SAX error in startDocumentHeader()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startDocumentTrailer() throws IFException {
        try {
            this.handler.startElement(EL_TRAILER);
        } catch (final SAXException e) {
            throw new IFException("SAX error in startDocumentTrailer()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endDocumentTrailer() throws IFException {
        try {
            this.handler.endElement(EL_TRAILER);
        } catch (final SAXException e) {
            throw new IFException("SAX error in endDocumentTrailer()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endDocument() throws IFException {
        try {
            this.handler.endElement(EL_DOCUMENT);
            this.handler.endDocument();
            finishDocumentNavigation();
        } catch (final SAXException e) {
            throw new IFException("SAX error in endDocument()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startPageSequence(final String id) throws IFException {
        try {
            final AttributesImpl atts = new AttributesImpl();
            if (id != null) {
                atts.addAttribute(XML_NAMESPACE, "id", "xml:id",
                        XMLConstants.CDATA, id);
            }
            final Locale lang = getContext().getLanguage();
            if (lang != null) {
                atts.addAttribute(XML_NAMESPACE, "lang", "xml:lang",
                        XMLConstants.CDATA, XMLUtil.toRFC3066(lang));
            }
            XMLUtil.addAttribute(atts, XMLConstants.XML_SPACE, "preserve");
            addForeignAttributes(atts);
            this.handler.startElement(EL_PAGE_SEQUENCE, atts);
            if (getUserAgent().isAccessibilityEnabled()) {
                final StructureTree structureTree = getUserAgent()
                        .getStructureTree();
                this.handler.startElement(EL_STRUCTURE_TREE); // add structure
                // tree
                final NodeList nodes = structureTree
                        .getPageSequence(this.pageSequenceIndex++);
                for (int i = 0, n = nodes.getLength(); i < n; ++i) {
                    final Node node = nodes.item(i);
                    new DOM2SAX(this.handler).writeFragment(node);
                }
                this.handler.endElement(EL_STRUCTURE_TREE);
            }
        } catch (final SAXException e) {
            throw new IFException("SAX error in startPageSequence()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endPageSequence() throws IFException {
        try {
            this.handler.endElement(EL_PAGE_SEQUENCE);
        } catch (final SAXException e) {
            throw new IFException("SAX error in endPageSequence()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startPage(final int index, final String name,
            final String pageMasterName, final Dimension size)
                    throws IFException {
        try {
            final AttributesImpl atts = new AttributesImpl();
            addAttribute(atts, "index", Integer.toString(index));
            addAttribute(atts, "name", name);
            addAttribute(atts, "page-master-name", pageMasterName);
            addAttribute(atts, "width", Integer.toString(size.width));
            addAttribute(atts, "height", Integer.toString(size.height));
            addForeignAttributes(atts);
            this.handler.startElement(EL_PAGE, atts);
        } catch (final SAXException e) {
            throw new IFException("SAX error in startPage()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startPageHeader() throws IFException {
        try {
            this.handler.startElement(EL_PAGE_HEADER);
        } catch (final SAXException e) {
            throw new IFException("SAX error in startPageHeader()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endPageHeader() throws IFException {
        try {
            this.handler.endElement(EL_PAGE_HEADER);
        } catch (final SAXException e) {
            throw new IFException("SAX error in endPageHeader()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public IFPainter startPageContent() throws IFException {
        try {
            this.handler.startElement(EL_PAGE_CONTENT);
            this.state = IFState.create();
            return this;
        } catch (final SAXException e) {
            throw new IFException("SAX error in startPageContent()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endPageContent() throws IFException {
        try {
            this.state = null;
            this.handler.endElement(EL_PAGE_CONTENT);
        } catch (final SAXException e) {
            throw new IFException("SAX error in endPageContent()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startPageTrailer() throws IFException {
        try {
            this.handler.startElement(EL_PAGE_TRAILER);
        } catch (final SAXException e) {
            throw new IFException("SAX error in startPageTrailer()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endPageTrailer() throws IFException {
        try {
            commitNavigation();
            this.handler.endElement(EL_PAGE_TRAILER);
        } catch (final SAXException e) {
            throw new IFException("SAX error in endPageTrailer()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endPage() throws IFException {
        try {
            this.handler.endElement(EL_PAGE);
        } catch (final SAXException e) {
            throw new IFException("SAX error in endPage()", e);
        }
    }

    // ---=== IFPainter ===---

    /** {@inheritDoc} */
    @Override
    public void startViewport(final AffineTransform transform,
            final Dimension size, final Rectangle clipRect) throws IFException {
        startViewport(IFUtil.toString(transform), size, clipRect);
    }

    /** {@inheritDoc} */
    @Override
    public void startViewport(final AffineTransform[] transforms,
            final Dimension size, final Rectangle clipRect) throws IFException {
        startViewport(IFUtil.toString(transforms), size, clipRect);
    }

    private void startViewport(final String transform, final Dimension size,
            final Rectangle clipRect) throws IFException {
        try {
            final AttributesImpl atts = new AttributesImpl();
            if (transform != null && transform.length() > 0) {
                addAttribute(atts, "transform", transform);
            }
            addAttribute(atts, "width", Integer.toString(size.width));
            addAttribute(atts, "height", Integer.toString(size.height));
            if (clipRect != null) {
                addAttribute(atts, "clip-rect", IFUtil.toString(clipRect));
            }
            this.handler.startElement(EL_VIEWPORT, atts);
        } catch (final SAXException e) {
            throw new IFException("SAX error in startViewport()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endViewport() throws IFException {
        try {
            this.handler.endElement(EL_VIEWPORT);
        } catch (final SAXException e) {
            throw new IFException("SAX error in endViewport()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startGroup(final AffineTransform[] transforms)
            throws IFException {
        startGroup(IFUtil.toString(transforms));
    }

    /** {@inheritDoc} */
    @Override
    public void startGroup(final AffineTransform transform) throws IFException {
        startGroup(IFUtil.toString(transform));
    }

    private void startGroup(final String transform) throws IFException {
        try {
            final AttributesImpl atts = new AttributesImpl();
            if (transform != null && transform.length() > 0) {
                addAttribute(atts, "transform", transform);
            }
            this.handler.startElement(EL_GROUP, atts);
        } catch (final SAXException e) {
            throw new IFException("SAX error in startGroup()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endGroup() throws IFException {
        try {
            this.handler.endElement(EL_GROUP);
        } catch (final SAXException e) {
            throw new IFException("SAX error in endGroup()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawImage(final String uri, final Rectangle rect)
            throws IFException {
        try {
            final AttributesImpl atts = new AttributesImpl();
            addAttribute(atts, XLINK_HREF, uri);
            addAttribute(atts, "x", Integer.toString(rect.x));
            addAttribute(atts, "y", Integer.toString(rect.y));
            addAttribute(atts, "width", Integer.toString(rect.width));
            addAttribute(atts, "height", Integer.toString(rect.height));
            addForeignAttributes(atts);
            addStructurePointerAttribute(atts);
            this.handler.element(EL_IMAGE, atts);
        } catch (final SAXException e) {
            throw new IFException("SAX error in startGroup()", e);
        }
    }

    private void addForeignAttributes(final AttributesImpl atts)
            throws SAXException {
        final Map<QName, String> foreignAttributes = getContext()
                .getForeignAttributes();
        for (final Entry<QName, String> entry : foreignAttributes.entrySet()) {
            addAttribute(atts, entry.getKey(), entry.getValue());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawImage(final Document doc, final Rectangle rect)
            throws IFException {
        try {
            final AttributesImpl atts = new AttributesImpl();
            addAttribute(atts, "x", Integer.toString(rect.x));
            addAttribute(atts, "y", Integer.toString(rect.y));
            addAttribute(atts, "width", Integer.toString(rect.width));
            addAttribute(atts, "height", Integer.toString(rect.height));
            addForeignAttributes(atts);
            addStructurePointerAttribute(atts);
            this.handler.startElement(EL_IMAGE, atts);
            new DOM2SAX(this.handler).writeDocument(doc, true);
            this.handler.endElement(EL_IMAGE);
        } catch (final SAXException e) {
            throw new IFException("SAX error in startGroup()", e);
        }
    }

    private static String toString(final Paint paint) {
        if (paint instanceof Color) {
            return ColorUtil.colorToString((Color) paint);
        } else {
            throw new UnsupportedOperationException("Paint not supported: "
                    + paint);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void clipRect(final Rectangle rect) throws IFException {
        try {
            final AttributesImpl atts = new AttributesImpl();
            addAttribute(atts, "x", Integer.toString(rect.x));
            addAttribute(atts, "y", Integer.toString(rect.y));
            addAttribute(atts, "width", Integer.toString(rect.width));
            addAttribute(atts, "height", Integer.toString(rect.height));
            this.handler.element(EL_CLIP_RECT, atts);
        } catch (final SAXException e) {
            throw new IFException("SAX error in clipRect()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void fillRect(final Rectangle rect, final Paint fill)
            throws IFException {
        if (fill == null) {
            return;
        }
        try {
            final AttributesImpl atts = new AttributesImpl();
            addAttribute(atts, "x", Integer.toString(rect.x));
            addAttribute(atts, "y", Integer.toString(rect.y));
            addAttribute(atts, "width", Integer.toString(rect.width));
            addAttribute(atts, "height", Integer.toString(rect.height));
            addAttribute(atts, "fill", toString(fill));
            this.handler.element(EL_RECT, atts);
        } catch (final SAXException e) {
            throw new IFException("SAX error in fillRect()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawBorderRect(final Rectangle rect, final BorderProps before,
            final BorderProps after, final BorderProps start,
            final BorderProps end) throws IFException {
        if (before == null && after == null && start == null && end == null) {
            return;
        }
        try {
            final AttributesImpl atts = new AttributesImpl();
            addAttribute(atts, "x", Integer.toString(rect.x));
            addAttribute(atts, "y", Integer.toString(rect.y));
            addAttribute(atts, "width", Integer.toString(rect.width));
            addAttribute(atts, "height", Integer.toString(rect.height));
            if (before != null) {
                addAttribute(atts, "before", before.toString());
            }
            if (after != null) {
                addAttribute(atts, "after", after.toString());
            }
            if (start != null) {
                addAttribute(atts, "start", start.toString());
            }
            if (end != null) {
                addAttribute(atts, "end", end.toString());
            }
            this.handler.element(EL_BORDER_RECT, atts);
        } catch (final SAXException e) {
            throw new IFException("SAX error in drawBorderRect()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawLine(final Point start, final Point end, final int width,
            final Color color, final RuleStyle style) throws IFException {
        try {
            final AttributesImpl atts = new AttributesImpl();
            addAttribute(atts, "x1", Integer.toString(start.x));
            addAttribute(atts, "y1", Integer.toString(start.y));
            addAttribute(atts, "x2", Integer.toString(end.x));
            addAttribute(atts, "y2", Integer.toString(end.y));
            addAttribute(atts, "stroke-width", Integer.toString(width));
            addAttribute(atts, "color", ColorUtil.colorToString(color));
            addAttribute(atts, "style", style.getName());
            this.handler.element(EL_LINE, atts);
        } catch (final SAXException e) {
            throw new IFException("SAX error in drawLine()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawText(final int x, final int y, final int letterSpacing,
            final int wordSpacing, final int[] dx, final String text)
                    throws IFException {
        try {
            final AttributesImpl atts = new AttributesImpl();
            addAttribute(atts, "x", Integer.toString(x));
            addAttribute(atts, "y", Integer.toString(y));
            if (letterSpacing != 0) {
                addAttribute(atts, "letter-spacing",
                        Integer.toString(letterSpacing));
            }
            if (wordSpacing != 0) {
                addAttribute(atts, "word-spacing",
                        Integer.toString(wordSpacing));
            }
            if (dx != null) {
                addAttribute(atts, "dx", IFUtil.toString(dx));
            }
            addStructurePointerAttribute(atts);
            this.handler.startElement(EL_TEXT, atts);
            final char[] chars = text.toCharArray();
            this.handler.characters(chars, 0, chars.length);
            this.handler.endElement(EL_TEXT);
        } catch (final SAXException e) {
            throw new IFException("SAX error in setFont()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setFont(final String family, final String style,
            final Integer weight, final String variant, final Integer size,
            final Color color) throws IFException {
        try {
            final AttributesImpl atts = new AttributesImpl();
            boolean changed;
            if (family != null) {
                changed = !family.equals(this.state.getFontFamily());
                if (changed) {
                    this.state.setFontFamily(family);
                    addAttribute(atts, "family", family);
                }
            }
            if (style != null) {
                changed = !style.equals(this.state.getFontStyle());
                if (changed) {
                    this.state.setFontStyle(style);
                    addAttribute(atts, "style", style);
                }
            }
            if (weight != null) {
                changed = weight.intValue() != this.state.getFontWeight();
                if (changed) {
                    this.state.setFontWeight(weight.intValue());
                    addAttribute(atts, "weight", weight.toString());
                }
            }
            if (variant != null) {
                changed = !variant.equals(this.state.getFontVariant());
                if (changed) {
                    this.state.setFontVariant(variant);
                    addAttribute(atts, "variant", variant);
                }
            }
            if (size != null) {
                changed = size.intValue() != this.state.getFontSize();
                if (changed) {
                    this.state.setFontSize(size.intValue());
                    addAttribute(atts, "size", size.toString());
                }
            }
            if (color != null) {
                changed = !color.equals(this.state.getTextColor());
                if (changed) {
                    this.state.setTextColor(color);
                    addAttribute(atts, "color", toString(color));
                }
            }
            if (atts.getLength() > 0) {
                this.handler.element(EL_FONT, atts);
            }
        } catch (final SAXException e) {
            throw new IFException("SAX error in setFont()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleExtensionObject(final Object extension)
            throws IFException {
        if (extension instanceof XMLizable) {
            try {
                ((XMLizable) extension).toSAX(this.handler);
            } catch (final SAXException e) {
                throw new IFException(
                        "SAX error while handling extension object", e);
            }
        } else {
            throw new UnsupportedOperationException(
                    "Extension must implement XMLizable: " + extension + " ("
                            + extension.getClass().getName() + ")");
        }
    }

    /** {@inheritDoc} */
    protected RenderingContext createRenderingContext() {
        throw new IllegalStateException("Should never be called!");
    }

    private void addAttribute(final AttributesImpl atts,
            final org.apache.xmlgraphics.util.QName attribute,
            final String value) throws SAXException {
        this.handler.startPrefixMapping(attribute.getPrefix(),
                attribute.getNamespaceURI());
        XMLUtil.addAttribute(atts, attribute, value);
    }

    private void addAttribute(final AttributesImpl atts,
            final String localName, final String value) {
        XMLUtil.addAttribute(atts, localName, value);
    }

    private void addStructurePointerAttribute(final AttributesImpl atts) {
        final String ptr = getContext().getStructurePointer();
        if (ptr != null) {
            addAttribute(atts, "ptr", ptr);
        }
    }

    // ---=== IFDocumentNavigationHandler ===---

    private final Map<String, AbstractAction> incompleteActions = new HashMap<>();
    private final List<AbstractAction> completeActions = new LinkedList<>();

    private void noteAction(final AbstractAction action) {
        if (action == null) {
            throw new NullPointerException("action must not be null");
        }
        if (!action.isComplete()) {
            assert action.hasID();
            this.incompleteActions.put(action.getID(), action);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void renderNamedDestination(final NamedDestination destination)
            throws IFException {
        noteAction(destination.getAction());

        final AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, "name", "name", XMLConstants.CDATA,
                destination.getName());
        try {
            this.handler.startElement(
                    DocumentNavigationExtensionConstants.NAMED_DESTINATION,
                    atts);
            serializeXMLizable(destination.getAction());
            this.handler
            .endElement(DocumentNavigationExtensionConstants.NAMED_DESTINATION);
        } catch (final SAXException e) {
            throw new IFException("SAX error serializing named destination", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void renderBookmarkTree(final BookmarkTree tree) throws IFException {
        final AttributesImpl atts = new AttributesImpl();
        try {
            this.handler.startElement(
                    DocumentNavigationExtensionConstants.BOOKMARK_TREE, atts);
            for (final Bookmark b : tree.getBookmarks()) {
                serializeBookmark(b);
            }
            this.handler
            .endElement(DocumentNavigationExtensionConstants.BOOKMARK_TREE);
        } catch (final SAXException e) {
            throw new IFException("SAX error serializing bookmark tree", e);
        }
    }

    private void serializeBookmark(final Bookmark bookmark)
            throws SAXException, IFException {
        noteAction(bookmark.getAction());

        final AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, "title", "title", XMLConstants.CDATA,
                bookmark.getTitle());
        atts.addAttribute(null, "starting-state", "starting-state",
                XMLConstants.CDATA, bookmark.isShown() ? "show" : "hide");
        this.handler.startElement(
                DocumentNavigationExtensionConstants.BOOKMARK, atts);
        serializeXMLizable(bookmark.getAction());
        for (final Bookmark b : bookmark.getChildBookmarks()) {
            serializeBookmark(b);
        }
        this.handler.endElement(DocumentNavigationExtensionConstants.BOOKMARK);

    }

    /** {@inheritDoc} */
    @Override
    public void renderLink(final Link link) throws IFException {
        noteAction(link.getAction());

        final AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, "rect", "rect", XMLConstants.CDATA,
                IFUtil.toString(link.getTargetRect()));
        if (getUserAgent().isAccessibilityEnabled()) {
            addAttribute(atts, "ptr", link.getAction().getStructurePointer());
        }
        try {
            this.handler.startElement(
                    DocumentNavigationExtensionConstants.LINK, atts);
            serializeXMLizable(link.getAction());
            this.handler.endElement(DocumentNavigationExtensionConstants.LINK);
        } catch (final SAXException e) {
            throw new IFException("SAX error serializing link", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addResolvedAction(final AbstractAction action) {
        assert action.isComplete();
        assert action.hasID();
        final AbstractAction noted = this.incompleteActions.remove(action
                .getID());
        if (noted != null) {
            this.completeActions.add(action);
        } else {
            // ignore as it was already complete when it was first used.
        }
    }

    private void commitNavigation() throws IFException {
        final Iterator<AbstractAction> iter = this.completeActions.iterator();
        while (iter.hasNext()) {
            final AbstractAction action = iter.next();
            iter.remove();
            serializeXMLizable(action);
        }
        assert this.completeActions.size() == 0;
    }

    private void finishDocumentNavigation() {
        assert this.incompleteActions.size() == 0 : "Still holding incomplete actions!";
    }

    private void serializeXMLizable(final XMLizable object) throws IFException {
        try {
            object.toSAX(this.handler);
        } catch (final SAXException e) {
            throw new IFException("SAX error serializing object", e);
        }
    }

}
