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

/* $Id: PDFRenderer.java 938005 2010-04-26 11:26:08Z jeremias $ */

package org.apache.fop.render.pdf;

// Java
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.ResourceEventProducer;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.area.Area;
import org.apache.fop.area.Block;
import org.apache.fop.area.BookmarkData;
import org.apache.fop.area.CTM;
import org.apache.fop.area.DestinationData;
import org.apache.fop.area.LineArea;
import org.apache.fop.area.OffDocumentExtensionAttachment;
import org.apache.fop.area.OffDocumentItem;
import org.apache.fop.area.PageSequence;
import org.apache.fop.area.PageViewport;
import org.apache.fop.area.Trait;
import org.apache.fop.area.inline.AbstractTextArea;
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
import org.apache.fop.fonts.LazyFont;
import org.apache.fop.fonts.SingleByteFont;
import org.apache.fop.fonts.Typeface;
import org.apache.fop.pdf.PDFAMode;
import org.apache.fop.pdf.PDFAction;
import org.apache.fop.pdf.PDFAnnotList;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFEncryptionParams;
import org.apache.fop.pdf.PDFFactory;
import org.apache.fop.pdf.PDFGoTo;
import org.apache.fop.pdf.PDFInfo;
import org.apache.fop.pdf.PDFLink;
import org.apache.fop.pdf.PDFNumber;
import org.apache.fop.pdf.PDFOutline;
import org.apache.fop.pdf.PDFPage;
import org.apache.fop.pdf.PDFPaintingState;
import org.apache.fop.pdf.PDFResourceContext;
import org.apache.fop.pdf.PDFResources;
import org.apache.fop.pdf.PDFTextUtil;
import org.apache.fop.pdf.PDFXMode;
import org.apache.fop.pdf.PDFXObject;
import org.apache.fop.render.AbstractPathOrientedRenderer;
import org.apache.fop.render.Graphics2DAdapter;
import org.apache.fop.render.RendererContext;
import org.apache.fop.render.RendererContextConstants;
import org.apache.fop.render.pdf.PDFLogicalStructureHandler.MarkedContentInfo;
import org.apache.fop.traits.RuleStyle;
import org.apache.fop.util.AbstractPaintingState;
import org.apache.fop.util.AbstractPaintingState.AbstractData;
import org.apache.fop.util.CharUtilities;
import org.apache.fop.util.XMLUtil;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageManager;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.image.loader.util.ImageUtil;
import org.apache.xmlgraphics.util.QName;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Renderer that renders areas to PDF.
 */
@Slf4j
public class PDFRenderer extends AbstractPathOrientedRenderer<AbstractData>
implements PDFConfigurationConstants {

    /** The MIME type for PDF */
    public static final String MIME_TYPE = MimeConstants.MIME_PDF;

    /** Normal PDF resolution (72dpi) */
    public static final int NORMAL_PDF_RESOLUTION = 72;

    /** Controls whether comments are written to the PDF stream. */
    protected static final boolean WRITE_COMMENTS = true;

    /**
     * the PDF Document being created
     */
    protected PDFDocument pdfDoc;

    /**
     * Utility class which enables all sorts of features that are not directly
     * connected to the normal rendering process.
     */
    protected PDFRenderingUtil pdfUtil;

    /**
     * Map of pages using the PageViewport as the key this is used for prepared
     * pages that cannot be immediately rendered
     */
    private Map<PageViewport, PDFPage> pages;

    /**
     * Maps unique PageViewport key to PDF page reference
     */
    protected Map<String, String> pageReferences = new HashMap<>();

    /**
     * Maps unique PageViewport key back to PageViewport itself
     */
    // protected Map pvReferences = new HashMap();

    /**
     * Maps XSL-FO element IDs to their on-page XY-positions Must be used in
     * conjunction with the page reference to fully specify the PDFGoTo details
     */
    protected Map<String, Point2D.Float> idPositions = new HashMap<>();

    /**
     * Maps XSL-FO element IDs to PDFGoTo objects targeting the corresponding
     * areas These objects may not all be fully filled in yet
     */
    protected Map<String, PDFGoTo> idGoTos = new HashMap<>();

    /**
     * The PDFGoTos in idGoTos that are not complete yet
     */
    protected List<PDFGoTo> unfinishedGoTos = new ArrayList<>();
    // can't use a Set because PDFGoTo.equals returns true if the target is the
    // same,
    // even if the object number differs

    /**
     * The output stream to write the document to
     */
    protected OutputStream ostream;

    /**
     * the /Resources object of the PDF document being created
     */
    protected PDFResources pdfResources;

    /** The current content generator to produce PDF commands with */
    protected PDFContentGenerator generator;
    private PDFBorderPainter borderPainter;

    /**
     * the current annotation list to add annotations to
     */
    protected PDFResourceContext currentContext = null;

    /**
     * the current page to add annotations to
     */
    protected PDFPage currentPage;

    /**
     * the current page's PDF reference string (to avoid numerous function
     * calls)
     */
    protected String currentPageRef;

    /** page height */
    protected int pageHeight;

    /** Image handler registry */
    private final PDFImageHandlerRegistry imageHandlerRegistry = new PDFImageHandlerRegistry();

    private boolean accessEnabled;

    private PDFLogicalStructureHandler logicalStructureHandler;

    private int pageSequenceIndex;

    /** Reference in the structure tree to the image being rendered. */
    private String imageReference;

    /**
     * create the PDF renderer
     */
    public PDFRenderer() {
    }

    /** {@inheritDoc} */
    @Override
    public void setUserAgent(final FOUserAgent agent) {
        super.setUserAgent(agent);
        this.pdfUtil = new PDFRenderingUtil(getUserAgent());
        this.accessEnabled = agent.isAccessibilityEnabled();
    }

    PDFRenderingUtil getPDFUtil() {
        return this.pdfUtil;
    }

    PDFContentGenerator getGenerator() {
        return this.generator;
    }

    PDFPaintingState getState() {
        return getGenerator().getState();
    }

    /** {@inheritDoc} */
    @Override
    public void startRenderer(final OutputStream stream) throws IOException {
        if (this.userAgent == null) {
            throw new IllegalStateException(
                    "UserAgent must be set before starting the renderer");
        }
        this.ostream = stream;
        this.pdfDoc = this.pdfUtil.setupPDFDocument(stream);
        if (this.accessEnabled) {
            this.pdfDoc.getRoot().makeTagged();
            this.logicalStructureHandler = new PDFLogicalStructureHandler(
                    this.pdfDoc, this.userAgent.getEventBroadcaster());
        }
    }

    /**
     * Checks if there are any unfinished PDFGoTos left in the list and resolves
     * them to a default position on the page. Logs a warning, as this should
     * not happen.
     */
    protected void finishOpenGoTos() {
        final int count = this.unfinishedGoTos.size();
        if (count > 0) {
            // TODO : page height may not be the same for all targeted pages
            final Point2D.Float defaultPos = new Point2D.Float(0f,
                    this.pageHeight / 1000f); // top-o-page
            while (!this.unfinishedGoTos.isEmpty()) {
                final PDFGoTo gt = this.unfinishedGoTos.get(0);
                finishIDGoTo(gt, defaultPos);
            }
            final PDFEventProducer eventProducer = PDFEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.nonFullyResolvedLinkTargets(this, count);
            // dysfunctional if pageref is null
        }
    }

    /** {@inheritDoc} */
    @Override
    public void stopRenderer() throws IOException {
        finishOpenGoTos();

        this.pdfDoc.getResources().addFonts(this.pdfDoc, this.fontInfo);
        this.pdfDoc.outputTrailer(this.ostream);

        this.pdfDoc = null;
        this.ostream = null;

        this.pages = null;

        this.pageReferences.clear();
        // pvReferences.clear();
        this.pdfResources = null;
        this.generator = null;
        this.currentContext = null;
        this.currentPage = null;

        this.idPositions.clear();
        this.idGoTos.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsOutOfOrder() {
        return !this.accessEnabled;
    }

    /**
     * {@inheritDoc}
     */
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
                this.pdfUtil.renderXMPMetadata((XMPMetadata) attachment);
            }
        }
    }

    private void renderDestination(final DestinationData dd) {
        final String targetID = dd.getIDRef();
        if (targetID == null || targetID.length() == 0) {
            throw new IllegalArgumentException(
                    "DestinationData must contain a ID reference");
        }
        final PageViewport pv = dd.getPageViewport();
        if (pv != null) {
            final PDFGoTo gt = getPDFGoToForID(targetID, pv.getKey());
            this.pdfDoc.getFactory().makeDestination(dd.getIDRef(),
                    gt.makeReference());
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
        for (int i = 0; i < bookmarks.getCount(); ++i) {
            final BookmarkData ext = bookmarks.getSubData(i);
            renderBookmarkItem(ext, null);
        }
    }

    private void renderBookmarkItem(final BookmarkData bookmarkItem,
            final PDFOutline parentBookmarkItem) {
        PDFOutline pdfOutline = null;

        final String targetID = bookmarkItem.getIDRef();
        if (targetID == null || targetID.length() == 0) {
            throw new IllegalArgumentException(
                    "DestinationData must contain a ID reference");
        }
        final PageViewport pv = bookmarkItem.getPageViewport();
        if (pv != null) {
            final String pvKey = pv.getKey();
            final PDFGoTo gt = getPDFGoToForID(targetID, pvKey);
            // create outline object:
            final PDFOutline parent = parentBookmarkItem != null ? parentBookmarkItem
                    : this.pdfDoc.getOutlineRoot();
            pdfOutline = this.pdfDoc.getFactory().makeOutline(parent,
                    bookmarkItem.getBookmarkTitle(), gt,
                    bookmarkItem.showChildItems());
        } else {
            // Warning already issued by AreaTreeHandler (debug level is
            // sufficient)
            log.debug("Bookmark with IDRef \"" + targetID
                    + "\" has a null PageViewport.");
        }

        for (int i = 0; i < bookmarkItem.getCount(); ++i) {
            renderBookmarkItem(bookmarkItem.getSubData(i), pdfOutline);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Graphics2DAdapter getGraphics2DAdapter() {
        return new PDFGraphics2DAdapter(this);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveGraphicsState() {
        this.generator.saveGraphicsState();
    }

    /** {@inheritDoc} */
    @Override
    protected void restoreGraphicsState() {
        this.generator.restoreGraphicsState();
    }

    /** Indicates the beginning of a text object. */
    @Override
    protected void beginTextObject() {
        this.generator.beginTextObject();
    }

    /** Indicates the end of a text object. */
    @Override
    protected void endTextObject() {
        this.generator.endTextObject();
    }

    /**
     * Start the next page sequence. For the PDF renderer there is no concept of
     * page sequences but it uses the first available page sequence title to set
     * as the title of the PDF document, and the language of the document.
     *
     * @param pageSequence
     *            the page sequence
     */
    @Override
    public void startPageSequence(final PageSequence pageSequence) {
        super.startPageSequence(pageSequence);
        final LineArea seqTitle = pageSequence.getTitle();
        if (seqTitle != null) {
            final String str = convertTitleToString(seqTitle);
            final PDFInfo info = this.pdfDoc.getInfo();
            if (info.getTitle() == null) {
                info.setTitle(str);
            }
        }
        Locale language = null;
        if (pageSequence.getLanguage() != null) {
            final String lang = pageSequence.getLanguage();
            final String country = pageSequence.getCountry();
            if (lang != null) {
                language = country == null ? new Locale(lang) : new Locale(
                        lang, country);
            }
            if (this.pdfDoc.getRoot().getLanguage() == null) {
                // Only set if not set already (first non-null is used)
                // Note: No checking is performed whether the values are valid!
                this.pdfDoc.getRoot().setLanguage(XMLUtil.toRFC3066(language));
            }
        }
        this.pdfUtil.generateDefaultXMPMetadata();
        if (this.accessEnabled) {
            final NodeList nodes = getUserAgent().getStructureTree()
                    .getPageSequence(this.pageSequenceIndex++);
            this.logicalStructureHandler.processStructureTree(nodes, language);
        }
    }

    /**
     * The pdf page is prepared by making the page. The page is made in the pdf
     * document without any contents and then stored to add the contents later.
     * The page objects is stored using the area tree PageViewport as a key.
     *
     * @param page
     *            the page to prepare
     */
    @Override
    public void preparePage(final PageViewport page) {
        setupPage(page);
        if (this.pages == null) {
            this.pages = new HashMap<>();
        }
        this.pages.put(page, this.currentPage);
    }

    private void setupPage(final PageViewport page) {
        this.pdfResources = this.pdfDoc.getResources();

        final Rectangle2D bounds = page.getViewArea();
        final double w = bounds.getWidth();
        final double h = bounds.getHeight();
        this.currentPage = this.pdfDoc.getFactory().makePage(this.pdfResources,
                (int) Math.round(w / 1000), (int) Math.round(h / 1000),
                page.getPageIndex());
        this.pageReferences.put(page.getKey(), this.currentPage.referencePDF());
        // pvReferences.put(page.getKey(), page);

        this.pdfUtil.generatePageLabel(page.getPageIndex(),
                page.getPageNumberString());
    }

    /**
     * This method creates a PDF stream for the current page uses it as the
     * contents of a new page. The page is written immediately to the output
     * stream. {@inheritDoc}
     */
    @Override
    public void renderPage(final PageViewport page) throws IOException {
        if (this.pages != null
                && (this.currentPage = this.pages.get(page)) != null) {
            // Retrieve previously prepared page (out-of-line rendering)
            this.pages.remove(page);
        } else {
            setupPage(page);
        }
        this.currentPageRef = this.currentPage.referencePDF();

        if (this.accessEnabled) {
            this.logicalStructureHandler.startPage(this.currentPage);
        }

        final Rectangle bounds = page.getViewArea();
        this.pageHeight = bounds.height;

        this.generator = new PDFContentGenerator(this.pdfDoc, this.ostream,
                this.currentPage);
        this.borderPainter = new PDFBorderPainter(this.generator);

        // Transform the PDF's default coordinate system (0,0 at lower left) to
        // the PDFRenderer's
        saveGraphicsState();
        final AffineTransform basicPageTransform = new AffineTransform(1, 0, 0,
                -1, 0, this.pageHeight / 1000f);
        this.generator.concatenate(basicPageTransform);

        super.renderPage(page);

        restoreGraphicsState();
        if (this.accessEnabled) {
            this.logicalStructureHandler.endPage();
        }

        this.pdfDoc.registerObject(this.generator.getStream());
        this.currentPage.setContents(this.generator.getStream());
        final PDFAnnotList annots = this.currentPage.getAnnotations();
        if (annots != null) {
            this.pdfDoc.addObject(annots);
        }
        this.pdfDoc.addObject(this.currentPage);
        this.borderPainter = null;
        this.generator.flushPDFDoc();
        this.generator = null;
    }

    /** {@inheritDoc} */
    @Override
    protected void startVParea(final CTM ctm, final Rectangle2D clippingRect) {
        saveGraphicsState();
        // Set the given CTM in the graphics state
        /*
         * currentState.concatenate( new
         * AffineTransform(CTMHelper.toPDFArray(ctm)));
         */

        if (clippingRect != null) {
            clipRect((float) clippingRect.getX() / 1000f,
                    (float) clippingRect.getY() / 1000f,
                    (float) clippingRect.getWidth() / 1000f,
                    (float) clippingRect.getHeight() / 1000f);
        }
        // multiply with current CTM
        this.generator.concatenate(new AffineTransform(CTMHelper
                .toPDFArray(ctm)));
    }

    /** {@inheritDoc} */
    @Override
    protected void endVParea() {
        restoreGraphicsState();
    }

    /** {@inheritDoc} */
    @Override
    protected void concatenateTransformationMatrix(final AffineTransform at) {
        this.generator.concatenate(at);
    }

    /**
     * Formats a float value (normally coordinates) as Strings.
     *
     * @param value
     *            the value
     * @return the formatted value
     */
    protected static String format(final float value) {
        return PDFNumber.doubleOut(value);
    }

    /** {@inheritDoc} */
    @Override
    protected void drawBorderLine(final float x1, final float y1,
            final float x2, final float y2, final boolean horz,
            final boolean startOrBefore, final int style, final Color col) {
        PDFBorderPainter.drawBorderLine(this.generator, x1, y1, x2, y2, horz,
                startOrBefore, style, col);
    }

    /** {@inheritDoc} */
    @Override
    protected void clipRect(final float x, final float y, final float width,
            final float height) {
        this.generator.add(format(x) + " " + format(y) + " " + format(width)
                + " " + format(height) + " re ");
        clip();
    }

    /**
     * Clip an area.
     */
    @Override
    protected void clip() {
        this.generator.add("W\n" + "n\n");
    }

    /**
     * Moves the current point to (x, y), omitting any connecting line segment.
     *
     * @param x
     *            x coordinate
     * @param y
     *            y coordinate
     */
    @Override
    protected void moveTo(final float x, final float y) {
        this.generator.add(format(x) + " " + format(y) + " m ");
    }

    /**
     * Appends a straight line segment from the current point to (x, y). The new
     * current point is (x, y).
     *
     * @param x
     *            x coordinate
     * @param y
     *            y coordinate
     */
    @Override
    protected void lineTo(final float x, final float y) {
        this.generator.add(format(x) + " " + format(y) + " l ");
    }

    /**
     * Closes the current subpath by appending a straight line segment from the
     * current point to the starting point of the subpath.
     */
    @Override
    protected void closePath() {
        this.generator.add("h ");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fillRect(final float x, final float y, final float width,
            final float height) {
        if (width > 0 && height > 0) {
            this.generator.add(format(x) + " " + format(y) + " "
                    + format(width) + " " + format(height) + " re f\n");
        }
    }

    /**
     * Breaks out of the state stack to handle fixed block-containers.
     *
     * @return the saved state stack to recreate later
     */
    @Override
    protected List<AbstractPaintingState.AbstractData> breakOutOfStateStack() {
        final PDFPaintingState paintingState = getState();
        final List<AbstractPaintingState.AbstractData> breakOutList = new ArrayList<>();
        AbstractPaintingState.AbstractData data;
        while (true) {
            data = paintingState.getData();
            if (paintingState.restore() == null) {
                break;
            }
            if (breakOutList.size() == 0) {
                this.generator.comment("------ break out!");
            }
            breakOutList.add(0, data); // Insert because of stack-popping
            this.generator.restoreGraphicsState(false);
        }
        return breakOutList;
    }

    /**
     * Restores the state stack after a break out.
     *
     * @param breakOutList
     *            the state stack to restore.
     */
    @Override
    protected void restoreStateStackAfterBreakOut(
            final List<AbstractData> breakOutList) {
        this.generator.comment("------ restoring context after break-out...");
        for (final AbstractData data : breakOutList) {
            saveGraphicsState();
            final AffineTransform at = data.getTransform();
            concatenateTransformationMatrix(at);
            // TODO Break-out: Also restore items such as line width and color
            // Left out for now because all this painting stuff is very
            // inconsistent. Some values go over PDFState, some don't.
        }
        this.generator.comment("------ done.");
    }

    /**
     * Returns area's id if it is the first area in the document with that id
     * (i.e. if the area qualifies as a link target). Otherwise, or if the area
     * has no id, null is returned.
     *
     * <i>NOTE</i>: area must be on currentPageViewport, otherwise result may be
     * wrong!
     *
     * @param area
     *            the area for which to return the id
     * @return the area's id (null if the area has no id or other preceding
     *         areas have the same id)
     */
    protected String getTargetableID(final Area area) {
        final String id = (String) area.getTrait(Trait.PROD_ID);
        if (id == null || id.length() == 0
                || !this.currentPageViewport.isFirstWithID(id)
                || this.idPositions.containsKey(id)) {
            return null;
        } else {
            return id;
        }
    }

    /**
     * Set XY position in the PDFGoTo and add it to the PDF trailer.
     *
     * @param gt
     *            the PDFGoTo object
     * @param position
     *            the X,Y position to set
     */
    protected void finishIDGoTo(final PDFGoTo gt, final Point2D.Float position) {
        gt.setPosition(position);
        this.pdfDoc.addTrailerObject(gt);
        this.unfinishedGoTos.remove(gt);
    }

    /**
     * Set page reference and XY position in the PDFGoTo and add it to the PDF
     * trailer.
     *
     * @param gt
     *            the PDFGoTo object
     * @param pdfPageRef
     *            the PDF reference string of the target page object
     * @param position
     *            the X,Y position to set
     */
    protected void finishIDGoTo(final PDFGoTo gt, final String pdfPageRef,
            final Point2D.Float position) {
        gt.setPageReference(pdfPageRef);
        finishIDGoTo(gt, position);
    }

    /**
     * Get a PDFGoTo pointing to the given id. Create one if necessary. It is
     * possible that the PDFGoTo is not fully resolved yet. In that case it must
     * be completed (and added to the PDF trailer) later.
     *
     * @param targetID
     *            the target id of the PDFGoTo
     * @param pvKey
     *            the unique key of the target PageViewport
     *
     * @return the PDFGoTo that was found or created
     */
    protected PDFGoTo getPDFGoToForID(final String targetID, final String pvKey) {
        // Already a PDFGoTo present for this target? If not, create.
        PDFGoTo gt = this.idGoTos.get(targetID);
        if (gt == null) {
            final String pdfPageRef = this.pageReferences.get(pvKey);
            final Point2D.Float position = this.idPositions.get(targetID);
            // can the GoTo already be fully filled in?
            if (pdfPageRef != null && position != null) {
                // getPDFGoTo shares PDFGoTo objects as much as possible.
                // It also takes care of assignObjectNumber and
                // addTrailerObject.
                gt = this.pdfDoc.getFactory().getPDFGoTo(pdfPageRef, position);
            } else {
                // Not complete yet, can't use getPDFGoTo:
                gt = new PDFGoTo(pdfPageRef);
                this.pdfDoc.assignObjectNumber(gt);
                // pdfDoc.addTrailerObject() will be called later, from
                // finishIDGoTo()
                this.unfinishedGoTos.add(gt);
            }
            this.idGoTos.put(targetID, gt);
        }
        return gt;
    }

    /**
     * Saves id's absolute position on page for later retrieval by PDFGoTos
     *
     * @param id
     *            the id of the area whose position must be saved
     * @param pdfPageRef
     *            the PDF page reference string
     * @param relativeIPP
     *            the *relative* IP position in millipoints
     * @param relativeBPP
     *            the *relative* BP position in millipoints
     * @param tf
     *            the transformation to apply once the relative positions have
     *            been converted to points
     */
    protected void saveAbsolutePosition(final String id,
            final String pdfPageRef, final int relativeIPP,
            final int relativeBPP, final AffineTransform tf) {
        final Point2D.Float position = new Point2D.Float(relativeIPP / 1000f,
                relativeBPP / 1000f);
        tf.transform(position, position);
        this.idPositions.put(id, position);
        // is there already a PDFGoTo waiting to be completed?
        final PDFGoTo gt = this.idGoTos.get(id);
        if (gt != null) {
            finishIDGoTo(gt, pdfPageRef, position);
        }
        /*
         * // The code below auto-creates a named destination for every id in
         * the document. // This should probably be controlled by a
         * user-configurable setting, as it may // make the PDF file grow
         * noticeably. // *** NOT YET WELL-TESTED ! *** if (true) { PDFFactory
         * factory = pdfDoc.getFactory(); if (gt == null) { gt =
         * factory.getPDFGoTo(pdfPageRef, position); idGoTos.put(id, gt); // so
         * others can pick it up too } factory.makeDestination(id,
         * gt.referencePDF(), currentPageViewport); // Note: using
         * currentPageViewport is only correct if the id is indeed on // the
         * current PageViewport. But even if incorrect, it won't interfere with
         * // what gets created in the PDF. // For speedup, we should also
         * create a lookup map id -> PDFDestination }
         */
    }

    /**
     * Saves id's absolute position on page for later retrieval by PDFGoTos,
     * using the currently valid transformation and the currently valid PDF page
     * reference
     *
     * @param id
     *            the id of the area whose position must be saved
     * @param relativeIPP
     *            the *relative* IP position in millipoints
     * @param relativeBPP
     *            the *relative* BP position in millipoints
     */
    protected void saveAbsolutePosition(final String id, final int relativeIPP,
            final int relativeBPP) {
        saveAbsolutePosition(id, this.currentPageRef, relativeIPP, relativeBPP,
                getState().getTransform());
    }

    /**
     * If the given block area is a possible link target, its id + absolute
     * position will be saved. The saved position is only correct if this
     * function is called at the very start of renderBlock!
     *
     * @param block
     *            the block area in question
     */
    protected void saveBlockPosIfTargetable(final Block block) {
        final String id = getTargetableID(block);
        if (id != null) {
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
            final AffineTransform tf = positioning == Block.FIXED ? getState()
                    .getBaseTransform() : getState().getTransform();
                    saveAbsolutePosition(id, this.currentPageRef, ipp, bpp, tf);
        }
    }

    /**
     * If the given inline area is a possible link target, its id + absolute
     * position will be saved. The saved position is only correct if this
     * function is called at the very start of renderInlineArea!
     *
     * @param inlineArea
     *            the inline area in question
     */
    protected void saveInlinePosIfTargetable(final InlineArea inlineArea) {
        final String id = getTargetableID(inlineArea);
        if (id != null) {
            final int extraMarginBefore = 5000; // millipoints
            final int ipp = this.currentIPPosition;
            final int bpp = this.currentBPPosition + inlineArea.getOffset()
                    - extraMarginBefore;
            saveAbsolutePosition(id, ipp, bpp);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderBlock(final Block block) {
        saveBlockPosIfTargetable(block);
        super.renderBlock(block);
    }

    /** {@inheritDoc} */
    @Override
    protected void renderLineArea(final LineArea line) {
        super.renderLineArea(line);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderInlineArea(final InlineArea inlineArea) {
        saveInlinePosIfTargetable(inlineArea);
        super.renderInlineArea(inlineArea);
    }

    /**
     * Render inline parent area. For pdf this handles the inline parent area
     * traits such as links, border, background.
     *
     * @param ip
     *            the inline parent area
     */
    @Override
    public void renderInlineParent(final InlineParent ip) {

        final boolean annotsAllowed = this.pdfDoc.getProfile()
                .isAnnotationAllowed();

        // stuff we only need if a link must be created:
        Rectangle2D ipRect = null;
        PDFFactory factory = null;
        PDFAction action = null;
        if (annotsAllowed) {
            // make sure the rect is determined *before* calling super!
            final int ipp = this.currentIPPosition;
            final int bpp = this.currentBPPosition + ip.getOffset();
            ipRect = new Rectangle2D.Float(ipp / 1000f, bpp / 1000f,
                    ip.getIPD() / 1000f, ip.getBPD() / 1000f);
            final AffineTransform transform = getState().getTransform();
            ipRect = transform.createTransformedShape(ipRect).getBounds2D();

            factory = this.pdfDoc.getFactory();
        }

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
                if (annotsAllowed) {
                    action = getPDFGoToForID(idRef, pvKey);
                }
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
                    if (annotsAllowed) {
                        action = factory.getExternalAction(extDest,
                                extLink.newWindow());
                    }
                }
            }
        }

        // warn if link trait found but not allowed, else create link
        if (linkTraitFound) {
            if (!annotsAllowed) {
                log.warn("Skipping annotation for a link due to PDF profile: "
                        + this.pdfDoc.getProfile());
            } else if (action != null) {
                final PDFLink pdfLink = factory.makeLink(ipRect, action);
                if (this.accessEnabled) {
                    final String ptr = (String) ip.getTrait(Trait.PTR);
                    this.logicalStructureHandler.addLinkContentItem(pdfLink,
                            ptr);
                }
                this.currentPage.addAnnotation(pdfLink);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void renderViewport(final Viewport viewport) {
        this.imageReference = (String) viewport.getTrait(Trait.PTR);
        super.renderViewport(viewport);
        this.imageReference = null;
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
    public void renderText(final TextArea text) {
        renderInlineAreaBackAndBorders(text);
        final Color ct = (Color) text.getTrait(Trait.COLOR);
        updateColor(ct, true);

        if (this.accessEnabled) {
            final String ptr = (String) text.getTrait(Trait.PTR);
            final MarkedContentInfo mci = this.logicalStructureHandler
                    .addTextContentItem(ptr);
            if (this.generator.getTextUtil().isInTextObject()) {
                this.generator.separateTextElements(mci.tag, mci.mcid);
            }
            this.generator.beginTextObject(mci.tag, mci.mcid);
        } else {
            beginTextObject();
        }

        final String fontName = getInternalFontNameForArea(text);
        final int size = ((Integer) text.getTrait(Trait.FONT_SIZE)).intValue();

        // This assumes that *all* CIDFonts use a /ToUnicode mapping
        final Typeface tf = getTypeface(fontName);

        final PDFTextUtil textutil = this.generator.getTextUtil();
        textutil.updateTf(fontName, size / 1000f, tf.isMultiByte());

        // word.getOffset() = only height of text itself
        // currentBlockIPPosition: 0 for beginning of line; nonzero
        // where previous line area failed to take up entire allocated space
        final int rx = this.currentIPPosition
                + text.getBorderAndPaddingWidthStart();
        final int bl = this.currentBPPosition + text.getOffset()
                + text.getBaselineOffset();

        textutil.writeTextMatrix(new AffineTransform(1, 0, 0, -1, rx / 1000f,
                bl / 1000f));

        super.renderText(text);

        textutil.writeTJ();

        renderTextDecoration(tf, size, text, bl, rx);
    }

    /** {@inheritDoc} */
    @Override
    public void renderWord(final WordArea word) {
        final Font font = getFontFromArea(word.getParentArea());
        final String s = word.getWord();

        escapeText(s, word.getLetterAdjustArray(), font,
                (AbstractTextArea) word.getParentArea());

        super.renderWord(word);
    }

    /** {@inheritDoc} */
    @Override
    public void renderSpace(final SpaceArea space) {
        final Font font = getFontFromArea(space.getParentArea());
        final String s = space.getSpace();

        final AbstractTextArea textArea = (AbstractTextArea) space
                .getParentArea();
        escapeText(s, null, font, textArea);

        if (space.isAdjustable()) {
            final int tws = -((TextArea) space.getParentArea())
                    .getTextWordSpaceAdjust()
                    - 2
                    * textArea.getTextLetterSpaceAdjust();

            if (tws != 0) {
                final float adjust = tws / (font.getFontSize() / 1000f);
                this.generator.getTextUtil().adjustGlyphTJ(adjust);
            }
        }

        super.renderSpace(space);
    }

    /**
     * Escapes text according to PDF rules.
     *
     * @param s
     *            Text to escape
     * @param letterAdjust
     *            an array of widths for letter adjustment (may be null)
     * @param font
     *            to font in use
     * @param parentArea
     *            the parent text area to retrieve certain traits from
     */
    protected void escapeText(final String s, final int[] letterAdjust,
            final Font font, final AbstractTextArea parentArea) {
        escapeText(s, 0, s.length(), letterAdjust, font, parentArea);
    }

    /**
     * Escapes text according to PDF rules.
     *
     * @param s
     *            Text to escape
     * @param start
     *            the start position in the text
     * @param end
     *            the end position in the text
     * @param letterAdjust
     *            an array of widths for letter adjustment (may be null)
     * @param font
     *            to font in use
     * @param parentArea
     *            the parent text area to retrieve certain traits from
     */
    protected void escapeText(final String s, final int start, final int end,
            final int[] letterAdjust, final Font font,
            final AbstractTextArea parentArea) {
        final String fontName = font.getFontName();
        final float fontSize = font.getFontSize() / 1000f;
        final Typeface tf = getTypeface(fontName);
        SingleByteFont singleByteFont = null;
        if (tf instanceof SingleByteFont) {
            singleByteFont = (SingleByteFont) tf;
        }
        final PDFTextUtil textutil = this.generator.getTextUtil();

        final int l = s.length();

        for (int i = start; i < end; ++i) {
            final char orgChar = s.charAt(i);
            char ch;
            float glyphAdjust = 0;
            if (font.hasChar(orgChar)) {
                ch = font.mapChar(orgChar);
                if (singleByteFont != null
                        && singleByteFont.hasAdditionalEncodings()) {
                    final int encoding = ch / 256;
                    if (encoding == 0) {
                        textutil.updateTf(fontName, fontSize, tf.isMultiByte());
                    } else {
                        textutil.updateTf(
                                fontName + "_" + Integer.toString(encoding),
                                fontSize, tf.isMultiByte());
                        ch = (char) (ch % 256);
                    }
                }
                final int tls = i < l - 1 ? parentArea
                        .getTextLetterSpaceAdjust() : 0;
                        glyphAdjust -= tls;
            } else {
                if (CharUtilities.isFixedWidthSpace(orgChar)) {
                    // Fixed width space are rendered as spaces so copy/paste
                    // works in a reader
                    ch = font.mapChar(CharUtilities.SPACE);
                    glyphAdjust = font.getCharWidth(ch)
                            - font.getCharWidth(orgChar);
                } else {
                    ch = font.mapChar(orgChar);
                }
            }
            if (letterAdjust != null && i < l - 1) {
                glyphAdjust -= letterAdjust[i + 1];
            }

            textutil.writeTJMappedChar(ch);

            final float adjust = glyphAdjust / fontSize;

            if (adjust != 0) {
                textutil.adjustGlyphTJ(adjust);
            }

        }
    }

    /** {@inheritDoc} */
    @Override
    protected void updateColor(final Color col, final boolean fill) {
        this.generator.updateColor(col, fill, null);
    }

    /** {@inheritDoc} */
    @Override
    public void renderImage(final Image image, final Rectangle2D pos) {
        endTextObject();
        final String url = image.getURL();
        putImage(url, pos, image.getForeignAttributes());
    }

    /** {@inheritDoc} */
    @Override
    protected void drawImage(final String url, final Rectangle2D pos,
            final Map<QName, String> foreignAttributes) {
        endTextObject();
        putImage(url, pos, foreignAttributes);
    }

    /**
     * Adds a PDF XObject (a bitmap or form) to the PDF that will later be
     * referenced.
     *
     * @param uri
     *            URL of the bitmap
     * @param pos
     *            Position of the bitmap
     * @param foreignAttributes
     *            foreign attributes associated with the image
     */
    protected void putImage(String uri, final Rectangle2D pos,
            final Map<QName, String> foreignAttributes) {
        final Rectangle posInt = new Rectangle((int) pos.getX(),
                (int) pos.getY(), (int) pos.getWidth(), (int) pos.getHeight());

        uri = URISpecification.getURL(uri);
        final PDFXObject xobject = this.pdfDoc.getXObject(uri);
        if (xobject != null) {
            final float w = (float) pos.getWidth() / 1000f;
            final float h = (float) pos.getHeight() / 1000f;
            placeImage((float) pos.getX() / 1000f, (float) pos.getY() / 1000f,
                    w, h, xobject);
            return;
        }
        final Point origin = new Point(this.currentIPPosition,
                this.currentBPPosition);
        final int x = origin.x + posInt.x;
        final int y = origin.y + posInt.y;

        final ImageManager manager = getUserAgent().getFactory()
                .getImageManager();
        ImageInfo info = null;
        try {
            final ImageSessionContext sessionContext = getUserAgent()
                    .getImageSessionContext();
            info = manager.getImageInfo(uri, sessionContext);

            final Map<Object, Object> hints = ImageUtil
                    .getDefaultHints(sessionContext);
            final ImageFlavor[] supportedFlavors = this.imageHandlerRegistry
                    .getSupportedFlavors();
            final org.apache.xmlgraphics.image.loader.Image img = manager
                    .getImage(info, supportedFlavors, hints, sessionContext);

            // First check for a dynamically registered handler
            final PDFImageHandler handler = (PDFImageHandler) this.imageHandlerRegistry
                    .getHandler(img.getClass());
            if (handler != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Using PDFImageHandler: "
                            + handler.getClass().getName());
                }
                try {
                    final RendererContext context = createRendererContext(x, y,
                            posInt.width, posInt.height, foreignAttributes);
                    handler.generateImage(context, img, origin, posInt);
                } catch (final IOException ioe) {
                    final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                            .get(getUserAgent().getEventBroadcaster());
                    eventProducer.imageWritingError(this, ioe);
                    return;
                }
            } else {
                throw new UnsupportedOperationException(
                        "No PDFImageHandler available for image: " + info
                                + " (" + img.getClass().getName() + ")");
            }
        } catch (final ImageException ie) {
            final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.imageError(this,
                    info != null ? info.toString() : uri, ie, null);
        } catch (final FileNotFoundException fe) {
            final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.imageNotFound(this, info != null ? info.toString()
                    : uri, fe, null);
        } catch (final IOException ioe) {
            final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.imageIOError(this, info != null ? info.toString()
                    : uri, ioe, null);
        }

        // output new data
        try {
            this.generator.flushPDFDoc();
        } catch (final IOException ioe) {
            // ioexception will be caught later
            log.error(ioe.getMessage());
        }
    }

    /**
     * Places a previously registered image at a certain place on the page.
     *
     * @param x
     *            X coordinate
     * @param y
     *            Y coordinate
     * @param w
     *            width for image
     * @param h
     *            height for image
     * @param xobj
     *            the image XObject
     */
    public void placeImage(final float x, final float y, final float w,
            final float h, final PDFXObject xobj) {
        if (this.accessEnabled) {
            final MarkedContentInfo mci = this.logicalStructureHandler
                    .addImageContentItem(this.imageReference);
            this.generator.saveGraphicsState(mci.tag, mci.mcid);
        } else {
            saveGraphicsState();
        }
        this.generator.add(format(w) + " 0 0 " + format(-h) + " "
                + format(this.currentIPPosition / 1000f + x) + " "
                + format(this.currentBPPosition / 1000f + h + y) + " cm\n"
                + xobj.getName() + " Do\n");
        if (this.accessEnabled) {
            this.generator.restoreGraphicsStateAccess();
        } else {
            restoreGraphicsState();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected RendererContext createRendererContext(final int x, final int y,
            final int width, final int height,
            final Map<QName, String> foreignAttributes) {
        final RendererContext context = super.createRendererContext(x, y,
                width, height, foreignAttributes);
        context.setProperty(PDFRendererContextConstants.PDF_DOCUMENT,
                this.pdfDoc);
        context.setProperty(RendererContextConstants.OUTPUT_STREAM,
                this.ostream);
        context.setProperty(PDFRendererContextConstants.PDF_PAGE,
                this.currentPage);
        context.setProperty(PDFRendererContextConstants.PDF_CONTEXT,
                this.currentContext);
        context.setProperty(PDFRendererContextConstants.PDF_STREAM,
                this.generator.getStream());
        context.setProperty(PDFRendererContextConstants.PDF_FONT_INFO,
                this.fontInfo);
        context.setProperty(PDFRendererContextConstants.PDF_FONT_NAME, "");
        context.setProperty(PDFRendererContextConstants.PDF_FONT_SIZE, 0);
        return context;
    }

    /** {@inheritDoc} */
    @Override
    public void renderDocument(final Document doc, final String ns,
            final Rectangle2D pos, final Map<QName, String> foreignAttributes) {
        if (this.accessEnabled) {
            final MarkedContentInfo mci = this.logicalStructureHandler
                    .addImageContentItem(this.imageReference);
            this.generator.beginMarkedContentSequence(mci.tag, mci.mcid);
        }
        super.renderDocument(doc, ns, pos, foreignAttributes);
        if (this.accessEnabled) {
            this.generator.endMarkedContentSequence();
        }
    }

    /**
     * Render leader area. This renders a leader area which is an area with a
     * rule.
     *
     * @param area
     *            the leader area to render
     */
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

        endTextObject();
        this.borderPainter.drawLine(new Point(startx, starty), new Point(endx,
                starty), ruleThickness, col, RuleStyle.valueOf(style));
        super.renderLeader(area);
    }

    /** {@inheritDoc} */
    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    /**
     * Sets the PDF/A mode for the PDF renderer.
     *
     * @param mode
     *            the PDF/A mode
     */
    public void setAMode(final PDFAMode mode) {
        this.pdfUtil.setAMode(mode);
    }

    /**
     * Sets the PDF/X mode for the PDF renderer.
     *
     * @param mode
     *            the PDF/X mode
     */
    public void setXMode(final PDFXMode mode) {
        this.pdfUtil.setXMode(mode);
    }

    /**
     * Sets the output color profile for the PDF renderer.
     *
     * @param outputProfileURI
     *            the URI to the output color profile
     */
    public void setOutputProfileURI(final String outputProfileURI) {
        this.pdfUtil.setOutputProfileURI(outputProfileURI);
    }

    /**
     * Sets the filter map to be used by the PDF renderer.
     *
     * @param filterMap
     *            the filter map
     */
    public void setFilterMap(final Map filterMap) {
        this.pdfUtil.setFilterMap(filterMap);
    }

    /**
     * Sets the encryption parameters used by the PDF renderer.
     *
     * @param encryptionParams
     *            the encryption parameters
     */
    public void setEncryptionParams(final PDFEncryptionParams encryptionParams) {
        this.pdfUtil.setEncryptionParams(encryptionParams);
    }

    MarkedContentInfo addCurrentImageToStructureTree() {
        return this.logicalStructureHandler
                .addImageContentItem(this.imageReference);
    }
}
