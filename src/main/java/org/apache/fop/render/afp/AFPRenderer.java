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

/* $Id: AFPRenderer.java 953952 2010-06-12 08:19:48Z jeremias $ */

package org.apache.fop.render.afp;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.ResourceEventProducer;
import org.apache.fop.afp.AFPBorderPainter;
import org.apache.fop.afp.AFPDataObjectInfo;
import org.apache.fop.afp.AFPDitheredRectanglePainter;
import org.apache.fop.afp.AFPEventProducer;
import org.apache.fop.afp.AFPPaintingState;
import org.apache.fop.afp.AFPRectanglePainter;
import org.apache.fop.afp.AFPResourceLevelDefaults;
import org.apache.fop.afp.AFPResourceManager;
import org.apache.fop.afp.AFPTextDataInfo;
import org.apache.fop.afp.AFPUnitConverter;
import org.apache.fop.afp.AbstractAFPPainter;
import org.apache.fop.afp.BorderPaintingInfo;
import org.apache.fop.afp.DataStream;
import org.apache.fop.afp.RectanglePaintingInfo;
import org.apache.fop.afp.fonts.AFPFont;
import org.apache.fop.afp.fonts.AFPFontAttributes;
import org.apache.fop.afp.fonts.AFPFontCollection;
import org.apache.fop.afp.fonts.AFPPageFonts;
import org.apache.fop.afp.fonts.CharacterSet;
import org.apache.fop.afp.modca.PageObject;
import org.apache.fop.afp.modca.ResourceObject;
import org.apache.fop.afp.util.DefaultFOPResourceAccessor;
import org.apache.fop.afp.util.ResourceAccessor;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.area.CTM;
import org.apache.fop.area.OffDocumentExtensionAttachment;
import org.apache.fop.area.OffDocumentItem;
import org.apache.fop.area.PageSequence;
import org.apache.fop.area.PageViewport;
import org.apache.fop.area.Trait;
import org.apache.fop.area.inline.Image;
import org.apache.fop.area.inline.Leader;
import org.apache.fop.area.inline.TextArea;
import org.apache.fop.datatypes.URISpecification;
import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontCollection;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontManager;
import org.apache.fop.render.AbstractPathOrientedRenderer;
import org.apache.fop.render.Graphics2DAdapter;
import org.apache.fop.render.RendererContext;
import org.apache.fop.render.afp.extensions.AFPElementMapping;
import org.apache.fop.render.afp.extensions.AFPExtensionAttachment;
import org.apache.fop.render.afp.extensions.AFPIncludeFormMap;
import org.apache.fop.render.afp.extensions.AFPInvokeMediumMap;
import org.apache.fop.render.afp.extensions.AFPPageOverlay;
import org.apache.fop.render.afp.extensions.AFPPageSetup;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageManager;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.image.loader.util.ImageUtil;
import org.apache.xmlgraphics.ps.ImageEncodingHelper;

/**
 * This is an implementation of a FOP Renderer that renders areas to AFP.
 * <p>
 * A renderer is primarily designed to convert a given area tree into the output
 * document format. It should be able to produce pages and fill the pages with
 * the text and graphical content. Usually the output is sent to an output
 * stream. Some output formats may support extra information that is not
 * available from the area tree or depends on the destination of the document.
 * Each renderer is given an area tree to render to its output format. The area
 * tree is simply a representation of the pages and the placement of text and
 * graphical objects on those pages.
 * </p>
 * <p>
 * The renderer will be given each page as it is ready and an output stream to
 * write the data out. All pages are supplied in the order they appear in the
 * document. In order to save memory it is possible to render the pages out of
 * order. Any page that is not ready to be rendered is setup by the renderer
 * first so that it can reserve a space or reference for when the page is ready
 * to be rendered.The renderer is responsible for managing the output format and
 * associated data and flow.
 * </p>
 * <p>
 * Each renderer is totally responsible for its output format. Because font
 * metrics (and therefore layout) are obtained in two different ways depending
 * on the renderer, the renderer actually sets up the fonts being used. The font
 * metrics are used during the layout process to determine the size of
 * characters.
 * </p>
 * <p>
 * The render context is used by handlers. It contains information about the
 * current state of the renderer, such as the page, the position, and any other
 * miscellaneous objects that are required to draw into the page.
 * </p>
 * <p>
 * A renderer is created by implementing the Renderer interface. However, the
 * AbstractRenderer does most of what is needed, including iterating through the
 * tree parts, so it is this that is extended. This means that this object only
 * need to implement the basic functionality such as text, images, and lines.
 * AbstractRenderer's methods can easily be overridden to handle things in a
 * different way or do some extra processing.
 * </p>
 * <p>
 * The relevant AreaTree structures that will need to be rendered are Page,
 * Viewport, Region, Span, Block, Line, Inline. A renderer implementation
 * renders each individual page, clips and aligns child areas to a viewport,
 * handle all types of inline area, text, image etc and draws various lines and
 * rectangles.
 * </p>
 *
 * Note: There are specific extensions that have been added to the FO. They are
 * specific to their location within the FO and have to be processed accordingly
 * (ie. at the start or end of the page).
 *
 */
@Slf4j
public class AFPRenderer extends AbstractPathOrientedRenderer implements
AFPCustomizable {

    private static final int X = 0;
    private static final int Y = 1;

    /** the resource manager */
    private AFPResourceManager resourceManager;

    /** the painting state */
    private final AFPPaintingState paintingState;

    /** unit converter */
    private final AFPUnitConverter unitConv;

    /** the line painter */
    private AFPBorderPainter borderPainter;

    /** the map of page segments */
    private final Map/* <String,String> */pageSegmentMap = new java.util.HashMap/*
     * <
     * String
     * ,
     * String
     * >
     */();

    /** the map of saved incomplete pages */
    private final Map pages = new java.util.HashMap/* <PageViewport,PageObject> */();

    /** the AFP datastream */
    private DataStream dataStream;

    /** the image handler registry */
    private final AFPImageHandlerRegistry imageHandlerRegistry;

    private AbstractAFPPainter rectanglePainter;

    /** the shading mode for filled rectangles */
    private AFPShadingMode shadingMode = AFPShadingMode.COLOR;

    /** medium map referenced used on previous page **/
    private String lastMediumMap;

    /**
     * Constructor for AFPRenderer.
     */
    public AFPRenderer() {
        super();
        this.imageHandlerRegistry = new AFPImageHandlerRegistry();
        this.resourceManager = new AFPResourceManager();
        this.paintingState = new AFPPaintingState();
        this.unitConv = this.paintingState.getUnitConverter();
    }

    /** {@inheritDoc} */
    @Override
    public void setupFontInfo(final FontInfo inFontInfo) {
        this.fontInfo = inFontInfo;
        final FontManager fontManager = this.userAgent.getFactory()
                .getFontManager();
        final FontCollection[] fontCollections = new FontCollection[] { new AFPFontCollection(
                this.userAgent.getEventBroadcaster(), getFontList()) };
        fontManager.setup(getFontInfo(), fontCollections);
    }

    /** {@inheritDoc} */
    @Override
    public void setUserAgent(final FOUserAgent agent) {
        super.setUserAgent(agent);
    }

    /** {@inheritDoc} */
    @Override
    public void startRenderer(final OutputStream outputStream)
            throws IOException {
        this.paintingState.setColor(Color.WHITE);

        this.dataStream = this.resourceManager.createDataStream(
                this.paintingState, outputStream);
        this.borderPainter = new AFPBorderPainter(this.paintingState,
                this.dataStream);
        this.rectanglePainter = createRectanglePainter();

        this.dataStream.startDocument();
    }

    AbstractAFPPainter createRectanglePainter() {
        if (AFPShadingMode.DITHERED.equals(this.shadingMode)) {
            return new AFPDitheredRectanglePainter(this.paintingState,
                    this.dataStream, this.resourceManager);
        } else {
            return new AFPRectanglePainter(this.paintingState, this.dataStream);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void stopRenderer() throws IOException {
        this.dataStream.endDocument();
        this.resourceManager.writeToStream();
        this.resourceManager = null;
    }

    /** {@inheritDoc} */
    @Override
    public void startPageSequence(final PageSequence pageSequence) {
        super.startPageSequence(pageSequence);
        try {
            this.dataStream.startPageGroup();
        } catch (final IOException e) {
            log.error(e.getMessage());
        }
        if (pageSequence.hasExtensionAttachments()) {
            for (final Iterator iter = pageSequence.getExtensionAttachments()
                    .iterator(); iter.hasNext();) {
                final ExtensionAttachment attachment = (ExtensionAttachment) iter
                        .next();
                if (attachment instanceof AFPInvokeMediumMap) {
                    final AFPInvokeMediumMap imm = (AFPInvokeMediumMap) attachment;
                    final String mediumMap = imm.getName();
                    if (mediumMap != null) {
                        this.dataStream.createInvokeMediumMap(mediumMap);
                    }
                } else if (attachment instanceof AFPPageSetup) {
                    final AFPPageSetup aps = (AFPPageSetup) attachment;
                    final String name = aps.getName();
                    final String value = aps.getValue();
                    this.dataStream.createTagLogicalElement(name, value);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsOutOfOrder() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void preparePage(final PageViewport page) {
        final int pageRotation = this.paintingState.getPageRotation();
        final int pageWidth = this.paintingState.getPageWidth();
        final int pageHeight = this.paintingState.getPageHeight();
        final int resolution = this.paintingState.getResolution();
        this.dataStream.startPage(pageWidth, pageHeight, pageRotation,
                resolution, resolution);

        renderPageObjectExtensions(page);

        final PageObject currentPage = this.dataStream.savePage();
        this.pages.put(page, currentPage);
    }

    /** {@inheritDoc} */
    @Override
    public void processOffDocumentItem(final OffDocumentItem odi) {
        if (odi instanceof OffDocumentExtensionAttachment) {
            final ExtensionAttachment attachment = ((OffDocumentExtensionAttachment) odi)
                    .getAttachment();
            if (attachment != null) {
                if (AFPExtensionAttachment.CATEGORY.equals(attachment
                        .getCategory())) {
                    if (attachment instanceof AFPIncludeFormMap) {
                        handleIncludeFormMap((AFPIncludeFormMap) attachment);
                    }
                }
            }
        }
    }

    private void handleIncludeFormMap(final AFPIncludeFormMap formMap) {
        final ResourceAccessor accessor = new DefaultFOPResourceAccessor(
                getUserAgent(), null, null);
        try {
            this.resourceManager.createIncludedResource(formMap.getName(),
                    formMap.getSrc(), accessor, ResourceObject.TYPE_FORMDEF);
        } catch (final IOException ioe) {
            final AFPEventProducer eventProducer = AFPEventProducer.Provider
                    .get(this.userAgent.getEventBroadcaster());
            eventProducer.resourceEmbeddingError(this, formMap.getName(), ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Graphics2DAdapter getGraphics2DAdapter() {
        return new AFPGraphics2DAdapter(this.paintingState);
    }

    /** {@inheritDoc} */
    @Override
    public void startVParea(final CTM ctm, final Rectangle2D clippingRect) {
        saveGraphicsState();
        if (ctm != null) {
            final AffineTransform at = ctm.toAffineTransform();
            concatenateTransformationMatrix(at);
        }
        if (clippingRect != null) {
            clipRect((float) clippingRect.getX() / 1000f,
                    (float) clippingRect.getY() / 1000f,
                    (float) clippingRect.getWidth() / 1000f,
                    (float) clippingRect.getHeight() / 1000f);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endVParea() {
        restoreGraphicsState();
    }

    /** {@inheritDoc} */
    @Override
    protected void concatenateTransformationMatrix(final AffineTransform at) {
        if (!at.isIdentity()) {
            this.paintingState.concatenate(at);
        }
    }

    /**
     * Returns the base AFP transform
     *
     * @return the base AFP transform
     */
    private AffineTransform getBaseTransform() {
        final AffineTransform baseTransform = new AffineTransform();
        final double scale = this.unitConv.mpt2units(1);
        baseTransform.scale(scale, scale);
        return baseTransform;
    }

    /** {@inheritDoc} */
    @Override
    public void renderPage(final PageViewport pageViewport) throws IOException,
    FOPException {
        this.paintingState.clear();

        final Rectangle2D bounds = pageViewport.getViewArea();

        final AffineTransform baseTransform = getBaseTransform();
        this.paintingState.concatenate(baseTransform);

        if (this.pages.containsKey(pageViewport)) {
            this.dataStream.restorePage((PageObject) this.pages
                    .remove(pageViewport));
        } else {
            final int pageWidth = Math.round(this.unitConv
                    .mpt2units((float) bounds.getWidth()));
            this.paintingState.setPageWidth(pageWidth);

            final int pageHeight = Math.round(this.unitConv
                    .mpt2units((float) bounds.getHeight()));
            this.paintingState.setPageHeight(pageHeight);

            final int pageRotation = this.paintingState.getPageRotation();

            final int resolution = this.paintingState.getResolution();

            // IMM should occur before BPG
            renderInvokeMediumMap(pageViewport);

            this.dataStream.startPage(pageWidth, pageHeight, pageRotation,
                    resolution, resolution);

            renderPageObjectExtensions(pageViewport);
        }

        super.renderPage(pageViewport);

        final AFPPageFonts pageFonts = this.paintingState.getPageFonts();
        if (pageFonts != null && !pageFonts.isEmpty()) {
            this.dataStream.addFontsToCurrentPage(pageFonts);
        }

        this.dataStream.endPage();
    }

    /** {@inheritDoc} */
    @Override
    public void drawBorderLine(final float x1, final float y1, final float x2,
            final float y2, final boolean horz, final boolean startOrBefore,
            final int style, final Color col) {
        final BorderPaintingInfo borderPaintInfo = new BorderPaintingInfo(x1,
                y1, x2, y2, horz, style, col);
        this.borderPainter.paint(borderPaintInfo);
    }

    /** {@inheritDoc} */
    @Override
    public void fillRect(final float x, final float y, final float width,
            final float height) {
        final RectanglePaintingInfo rectanglePaintInfo = new RectanglePaintingInfo(
                x, y, width, height);
        try {
            this.rectanglePainter.paint(rectanglePaintInfo);
        } catch (final IOException ioe) {
            // TODO not ideal, but the AFPRenderer is legacy
            throw new RuntimeException(
                    "I/O error while painting a filled rectangle", ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected RendererContext instantiateRendererContext() {
        return new AFPRendererContext(this, getMimeType());
    }

    /** {@inheritDoc} */
    @Override
    protected RendererContext createRendererContext(final int x, final int y,
            final int width, final int height, final Map foreignAttributes) {
        RendererContext context;
        context = super.createRendererContext(x, y, width, height,
                foreignAttributes);
        context.setProperty(AFPRendererContextConstants.AFP_FONT_INFO,
                this.fontInfo);
        context.setProperty(AFPRendererContextConstants.AFP_RESOURCE_MANAGER,
                this.resourceManager);
        context.setProperty(AFPRendererContextConstants.AFP_PAINTING_STATE,
                this.paintingState);
        return context;
    }

    private static final ImageFlavor[] NATIVE_FLAVORS = new ImageFlavor[] {
        ImageFlavor.XML_DOM,
        /* ImageFlavor.RAW_PNG, */// PNG not natively supported in AFP
        ImageFlavor.RAW_JPEG, ImageFlavor.RAW_CCITTFAX,
        ImageFlavor.RAW_EPS, ImageFlavor.RAW_TIFF, ImageFlavor.GRAPHICS2D,
        ImageFlavor.BUFFERED_IMAGE, ImageFlavor.RENDERED_IMAGE };

    private static final ImageFlavor[] FLAVORS = new ImageFlavor[] {
        ImageFlavor.XML_DOM, ImageFlavor.GRAPHICS2D,
        ImageFlavor.BUFFERED_IMAGE, ImageFlavor.RENDERED_IMAGE };

    /** {@inheritDoc} */
    @Override
    public void drawImage(String uri, final Rectangle2D pos,
            final Map foreignAttributes) {
        uri = URISpecification.getURL(uri);
        this.paintingState.setImageUri(uri);

        final Point origin = new Point(this.currentIPPosition,
                this.currentBPPosition);
        final Rectangle posInt = new Rectangle((int) Math.round(pos.getX()),
                (int) Math.round(pos.getY()), (int) Math.round(pos.getWidth()),
                (int) Math.round(pos.getHeight()));
        final int x = origin.x + posInt.x;
        final int y = origin.y + posInt.y;

        final String name = (String) this.pageSegmentMap.get(uri);
        if (name != null) {
            final float[] srcPts = { x, y, posInt.width, posInt.height };
            final int[] coords = this.unitConv.mpts2units(srcPts);
            final int width = Math.round(this.unitConv.mpt2units(posInt.width));
            final int height = Math.round(this.unitConv
                    .mpt2units(posInt.height));
            this.dataStream.createIncludePageSegment(name, coords[X],
                    coords[Y], width, height);
        } else {
            final ImageManager manager = this.userAgent.getFactory()
                    .getImageManager();
            ImageInfo info = null;
            try {
                final ImageSessionContext sessionContext = this.userAgent
                        .getImageSessionContext();
                info = manager.getImageInfo(uri, sessionContext);

                // Only now fully load/prepare the image
                final Map hints = ImageUtil.getDefaultHints(sessionContext);

                final boolean nativeImagesSupported = this.paintingState
                        .isNativeImagesSupported();
                final ImageFlavor[] flavors = nativeImagesSupported ? NATIVE_FLAVORS
                        : FLAVORS;

                // Load image
                final org.apache.xmlgraphics.image.loader.Image img = manager
                        .getImage(info, flavors, hints, sessionContext);

                // Handle image
                final AFPImageHandler imageHandler = (AFPImageHandler) this.imageHandlerRegistry
                        .getHandler(img);
                if (imageHandler != null) {
                    final RendererContext rendererContext = createRendererContext(
                            x, y, posInt.width, posInt.height,
                            foreignAttributes);
                    final AFPRendererImageInfo rendererImageInfo = new AFPRendererImageInfo(
                            uri, pos, origin, info, img, rendererContext,
                            foreignAttributes);
                    AFPDataObjectInfo dataObjectInfo = null;
                    try {
                        dataObjectInfo = imageHandler
                                .generateDataObjectInfo(rendererImageInfo);
                        // Create image
                        if (dataObjectInfo != null) {
                            this.resourceManager.createObject(dataObjectInfo);
                        }
                    } catch (final IOException ioe) {
                        final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                                .get(this.userAgent.getEventBroadcaster());
                        eventProducer.imageWritingError(this, ioe);
                        throw ioe;
                    }
                } else {
                    throw new UnsupportedOperationException(
                            "No AFPImageHandler available for image: " + info
                            + " (" + img.getClass().getName() + ")");
                }

            } catch (final ImageException ie) {
                final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                        .get(this.userAgent.getEventBroadcaster());
                eventProducer.imageError(this, info != null ? info.toString()
                        : uri, ie, null);
            } catch (final FileNotFoundException fe) {
                final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                        .get(this.userAgent.getEventBroadcaster());
                eventProducer.imageNotFound(this,
                        info != null ? info.toString() : uri, fe, null);
            } catch (final IOException ioe) {
                final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                        .get(this.userAgent.getEventBroadcaster());
                eventProducer.imageIOError(this, info != null ? info.toString()
                        : uri, ioe, null);
            }
        }
    }

    /**
     * Writes a RenderedImage to an OutputStream as raw sRGB bitmaps.
     *
     * @param image
     *            the RenderedImage
     * @param out
     *            the OutputStream
     * @throws IOException
     *             In case of an I/O error.
     * @deprecated use ImageEncodingHelper.encodeRenderedImageAsRGB(image, out)
     *             directly instead
     */
    @Deprecated
    public static void writeImage(final RenderedImage image,
            final OutputStream out) throws IOException {
        ImageEncodingHelper.encodeRenderedImageAsRGB(image, out);
    }

    /** {@inheritDoc} */
    @Override
    public void updateColor(final Color col, final boolean fill) {
        if (fill) {
            this.paintingState.setColor(col);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void restoreStateStackAfterBreakOut(final List breakOutList) {
        log.debug("Block.FIXED --> restoring context after break-out");
        this.paintingState.saveAll(breakOutList);
    }

    /** {@inheritDoc} */
    @Override
    protected List breakOutOfStateStack() {
        log.debug("Block.FIXED --> break out");
        return this.paintingState.restoreAll();
    }

    /** {@inheritDoc} */
    @Override
    public void saveGraphicsState() {
        this.paintingState.save();
    }

    /** {@inheritDoc} */
    @Override
    public void restoreGraphicsState() {
        this.paintingState.restore();
    }

    /** {@inheritDoc} */
    @Override
    public void renderImage(final Image image, final Rectangle2D pos) {
        drawImage(image.getURL(), pos, image.getForeignAttributes());
    }

    /** {@inheritDoc} */
    @Override
    public void renderText(final TextArea text) {
        renderInlineAreaBackAndBorders(text);

        // set font size
        final int fontSize = ((Integer) text.getTrait(Trait.FONT_SIZE))
                .intValue();
        this.paintingState.setFontSize(fontSize);

        // register font as necessary
        final String internalFontName = getInternalFontNameForArea(text);
        final Map/* <String,FontMetrics> */fontMetricMap = this.fontInfo
                .getFonts();
        final AFPFont font = (AFPFont) fontMetricMap.get(internalFontName);
        final AFPPageFonts pageFonts = this.paintingState.getPageFonts();
        final AFPFontAttributes fontAttributes = pageFonts.registerFont(
                internalFontName, font, fontSize);
        final Font fnt = getFontFromArea(text);

        if (font.isEmbeddable()) {
            final CharacterSet charSet = font.getCharacterSet(fontSize);
            try {
                this.resourceManager.embedFont(font, charSet);
            } catch (final IOException ioe) {
                final AFPEventProducer eventProducer = AFPEventProducer.Provider
                        .get(this.userAgent.getEventBroadcaster());
                eventProducer.resourceEmbeddingError(this, charSet.getName(),
                        ioe);
            }
        }

        // create text data info
        final AFPTextDataInfo textDataInfo = new AFPTextDataInfo();

        final int fontReference = fontAttributes.getFontReference();
        textDataInfo.setFontReference(fontReference);

        final int x = this.currentIPPosition
                + text.getBorderAndPaddingWidthStart();
        final int y = this.currentBPPosition + text.getOffset()
                + text.getBaselineOffset();

        final int[] coords = this.unitConv.mpts2units(new float[] { x, y });
        textDataInfo.setX(coords[X]);
        textDataInfo.setY(coords[Y]);

        final Color color = (Color) text.getTrait(Trait.COLOR);
        textDataInfo.setColor(color);

        final int textWordSpaceAdjust = text.getTextWordSpaceAdjust();
        final int textLetterSpaceAdjust = text.getTextLetterSpaceAdjust();
        int textWidth = font.getWidth(' ', fontSize) / 1000;
        textWidth = 0; // JM, the above is strange
        int variableSpaceCharacterIncrement = textWidth + textWordSpaceAdjust
                + textLetterSpaceAdjust;

        variableSpaceCharacterIncrement = Math.round(this.unitConv
                .mpt2units(variableSpaceCharacterIncrement));
        textDataInfo
        .setVariableSpaceCharacterIncrement(variableSpaceCharacterIncrement);

        final int interCharacterAdjustment = Math.round(this.unitConv
                .mpt2units(textLetterSpaceAdjust));
        textDataInfo.setInterCharacterAdjustment(interCharacterAdjustment);

        final CharacterSet charSet = font.getCharacterSet(fontSize);
        final String encoding = charSet.getEncoding();
        textDataInfo.setEncoding(encoding);

        final String textString = text.getText();
        textDataInfo.setString(textString);

        try {
            this.dataStream.createText(textDataInfo, textLetterSpaceAdjust,
                    textWordSpaceAdjust, fnt, charSet);
        } catch (final UnsupportedEncodingException e) {
            final AFPEventProducer eventProducer = AFPEventProducer.Provider
                    .get(this.userAgent.getEventBroadcaster());
            eventProducer.characterSetEncodingError(this, charSet.getName(),
                    encoding);
        }
        // word.getOffset() = only height of text itself
        // currentBlockIPPosition: 0 for beginning of line; nonzero
        // where previous line area failed to take up entire allocated space

        super.renderText(text);

        renderTextDecoration(font, fontSize, text, y, x);
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
        final float startx = (this.currentIPPosition + area
                .getBorderAndPaddingWidthStart()) / 1000f;
        final float starty = (this.currentBPPosition + area.getOffset()) / 1000f;
        final float endx = (this.currentIPPosition
                + area.getBorderAndPaddingWidthStart() + area.getIPD()) / 1000f;
        final float ruleThickness = area.getRuleThickness() / 1000f;
        final Color col = (Color) area.getTrait(Trait.COLOR);

        switch (style) {
        case EN_SOLID:
        case EN_DASHED:
        case EN_DOUBLE:
        case EN_DOTTED:
        case EN_GROOVE:
        case EN_RIDGE:
            drawBorderLine(startx, starty, endx, starty + ruleThickness, true,
                    true, style, col);
            break;
        default:
            throw new UnsupportedOperationException("rule style not supported");
        }
        super.renderLeader(area);
    }

    /**
     * Get the MIME type of the renderer.
     *
     * @return The MIME type of the renderer
     */
    @Override
    public String getMimeType() {
        return org.apache.xmlgraphics.util.MimeConstants.MIME_AFP;
    }

    /**
     * checks for IMM Extension and renders if found and different from previous
     * page
     *
     * @param pageViewport
     *            the page object
     */
    private void renderInvokeMediumMap(final PageViewport pageViewport) {
        if (pageViewport.getExtensionAttachments() != null
                && pageViewport.getExtensionAttachments().size() > 0) {
            final Iterator it = pageViewport.getExtensionAttachments()
                    .iterator();
            while (it.hasNext()) {
                final ExtensionAttachment attachment = (ExtensionAttachment) it
                        .next();
                if (AFPExtensionAttachment.CATEGORY.equals(attachment
                        .getCategory())) {
                    final AFPExtensionAttachment aea = (AFPExtensionAttachment) attachment;
                    if (AFPElementMapping.INVOKE_MEDIUM_MAP.equals(aea
                            .getElementName())) {
                        final AFPInvokeMediumMap imm = (AFPInvokeMediumMap) attachment;
                        final String mediumMap = imm.getName();
                        if (mediumMap != null) {
                            if (!mediumMap.equals(this.lastMediumMap)) {
                                this.dataStream
                                .createInvokeMediumMap(mediumMap);
                                this.lastMediumMap = mediumMap;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Method to render the page extension.
     * <p>
     *
     * @param pageViewport
     *            the page object
     */
    private void renderPageObjectExtensions(final PageViewport pageViewport) {
        this.pageSegmentMap.clear();
        if (pageViewport.getExtensionAttachments() != null
                && pageViewport.getExtensionAttachments().size() > 0) {
            // Extract all AFPPageSetup instances from the attachment list on
            // the s-p-m
            final Iterator it = pageViewport.getExtensionAttachments()
                    .iterator();
            while (it.hasNext()) {
                final ExtensionAttachment attachment = (ExtensionAttachment) it
                        .next();
                if (AFPExtensionAttachment.CATEGORY.equals(attachment
                        .getCategory())) {
                    if (attachment instanceof AFPPageSetup) {
                        final AFPPageSetup aps = (AFPPageSetup) attachment;
                        final String element = aps.getElementName();
                        if (AFPElementMapping.INCLUDE_PAGE_SEGMENT
                                .equals(element)) {
                            final String name = aps.getName();
                            final String source = aps.getValue();
                            this.pageSegmentMap.put(source, name);
                        } else if (AFPElementMapping.TAG_LOGICAL_ELEMENT
                                .equals(element)) {
                            final String name = aps.getName();
                            final String value = aps.getValue();
                            this.dataStream
                            .createTagLogicalElement(name, value);
                        } else if (AFPElementMapping.NO_OPERATION
                                .equals(element)) {
                            final String content = aps.getContent();
                            if (content != null) {
                                this.dataStream.createNoOperation(content);
                            }
                        }
                    } else if (attachment instanceof AFPPageOverlay) {
                        final AFPPageOverlay ipo = (AFPPageOverlay) attachment;
                        final String element = ipo.getElementName();
                        if (AFPElementMapping.INCLUDE_PAGE_OVERLAY
                                .equals(element)) {
                            final String overlay = ipo.getName();
                            if (overlay != null) {
                                this.dataStream.createIncludePageOverlay(
                                        overlay, ipo.getX(), ipo.getY());
                            }
                        }
                    }
                }
            }
        }

    }

    /**
     * Sets the rotation to be used for portrait pages, valid values are 0
     * (default), 90, 180, 270.
     *
     * @param rotation
     *            The rotation in degrees.
     */
    public void setPortraitRotation(final int rotation) {
        this.paintingState.setPortraitRotation(rotation);
    }

    /**
     * Sets the rotation to be used for landscape pages, valid values are 0, 90,
     * 180, 270 (default).
     *
     * @param rotation
     *            The rotation in degrees.
     */
    public void setLandscapeRotation(final int rotation) {
        this.paintingState.setLandscapeRotation(rotation);
    }

    // ---=== AFPCustomizable ===---

    /** {@inheritDoc} */
    @Override
    public void setBitsPerPixel(final int bitsPerPixel) {
        this.paintingState.setBitsPerPixel(bitsPerPixel);
    }

    /** {@inheritDoc} */
    @Override
    public void setColorImages(final boolean colorImages) {
        this.paintingState.setColorImages(colorImages);
    }

    /** {@inheritDoc} */
    @Override
    public void setNativeImagesSupported(final boolean nativeImages) {
        this.paintingState.setNativeImagesSupported(nativeImages);
    }

    /** {@inheritDoc} */
    @Override
    public void setCMYKImagesSupported(final boolean value) {
        this.paintingState.setCMYKImagesSupported(value);
    }

    /** {@inheritDoc} */
    @Override
    public void setDitheringQuality(final float quality) {
        this.paintingState.setDitheringQuality(quality);
    }

    /** {@inheritDoc} */
    @Override
    public void setShadingMode(final AFPShadingMode shadingMode) {
        this.shadingMode = shadingMode;
    }

    /** {@inheritDoc} */
    @Override
    public void setResolution(final int resolution) {
        this.paintingState.setResolution(resolution);
    }

    /** {@inheritDoc} */
    @Override
    public int getResolution() {
        return this.paintingState.getResolution();
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultResourceGroupFilePath(final String filePath) {
        this.resourceManager.setDefaultResourceGroupFilePath(filePath);
    }

    /** {@inheritDoc} */
    @Override
    public void setResourceLevelDefaults(final AFPResourceLevelDefaults defaults) {
        this.resourceManager.setResourceLevelDefaults(defaults);
    }

    /** {@inheritDoc} */
    @Override
    protected void establishTransformationMatrix(final AffineTransform at) {
        saveGraphicsState();
        concatenateTransformationMatrix(at);
    }

    /** {@inheritDoc} */
    @Override
    public void clip() {
        // TODO
        // log.debug("NYI clip()");
    }

    /** {@inheritDoc} */
    @Override
    public void clipRect(final float x, final float y, final float width,
            final float height) {
        // TODO
        // log.debug("NYI clipRect(x=" + x + ",y=" + y
        // + ",width=" + width + ", height=" + height + ")");
    }

    /** {@inheritDoc} */
    @Override
    public void moveTo(final float x, final float y) {
        // TODO
        // log.debug("NYI moveTo(x=" + x + ",y=" + y + ")");
    }

    /** {@inheritDoc} */
    @Override
    public void lineTo(final float x, final float y) {
        // TODO
        // log.debug("NYI lineTo(x=" + x + ",y=" + y + ")");
    }

    /** {@inheritDoc} */
    @Override
    public void closePath() {
        // TODO
        // log.debug("NYI closePath()");
    }

    /** Indicates the beginning of a text object. */
    @Override
    public void beginTextObject() {
        // TODO PDF specific maybe?
        // log.debug("NYI beginTextObject()");
    }

    /** Indicates the end of a text object. */
    @Override
    public void endTextObject() {
        // TODO PDF specific maybe?
        // log.debug("NYI endTextObject()");
    }

}
