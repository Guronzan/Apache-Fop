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

/* $Id: PCLRenderer.java 932497 2010-04-09 16:34:29Z vhennebert $ */

package org.apache.fop.render.pcl;

//Java
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.ResourceEventProducer;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.area.Area;
import org.apache.fop.area.Block;
import org.apache.fop.area.BlockViewport;
import org.apache.fop.area.CTM;
import org.apache.fop.area.NormalFlow;
import org.apache.fop.area.PageViewport;
import org.apache.fop.area.RegionViewport;
import org.apache.fop.area.Trait;
import org.apache.fop.area.inline.AbstractTextArea;
import org.apache.fop.area.inline.ForeignObject;
import org.apache.fop.area.inline.Image;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.Leader;
import org.apache.fop.area.inline.SpaceArea;
import org.apache.fop.area.inline.TextArea;
import org.apache.fop.area.inline.Viewport;
import org.apache.fop.area.inline.WordArea;
import org.apache.fop.datatypes.URISpecification;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontCollection;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontMetrics;
import org.apache.fop.render.Graphics2DAdapter;
import org.apache.fop.render.ImageHandlerUtil;
import org.apache.fop.render.PrintRenderer;
import org.apache.fop.render.RendererContext;
import org.apache.fop.render.RendererContextConstants;
import org.apache.fop.render.RendererEventProducer;
import org.apache.fop.render.java2d.Base14FontCollection;
import org.apache.fop.render.java2d.ConfiguredFontCollection;
import org.apache.fop.render.java2d.FontMetricsMapper;
import org.apache.fop.render.java2d.InstalledFontCollection;
import org.apache.fop.render.java2d.Java2DFontMetrics;
import org.apache.fop.render.java2d.Java2DRenderer;
import org.apache.fop.render.pcl.extensions.PCLElementMapping;
import org.apache.fop.traits.BorderProps;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageManager;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.image.loader.ImageSize;
import org.apache.xmlgraphics.image.loader.impl.ImageGraphics2D;
import org.apache.xmlgraphics.image.loader.impl.ImageRendered;
import org.apache.xmlgraphics.image.loader.impl.ImageXMLDOM;
import org.apache.xmlgraphics.image.loader.util.ImageUtil;
import org.apache.xmlgraphics.java2d.GraphicContext;
import org.apache.xmlgraphics.java2d.Graphics2DImagePainter;
import org.apache.xmlgraphics.util.UnitConv;
import org.w3c.dom.Document;

/* Note:
 * There are some commonalities with AbstractPathOrientedRenderer but it's not possible
 * to derive from it due to PCL's restrictions. We may need an additional common subclass to
 * avoid methods copied from AbstractPathOrientedRenderer. Or we wait until after the IF redesign.
 */

/**
 * Renderer for the PCL 5 printer language. It also uses HP GL/2 for certain
 * graphic elements.
 */
@Slf4j
public class PCLRenderer extends PrintRenderer implements PCLConstants {

    /** The MIME type for PCL */
    public static final String MIME_TYPE = MimeConstants.MIME_PCL_ALT;

    /** The OutputStream to write the PCL stream to */
    protected OutputStream out;

    /** The PCL generator */
    protected PCLGenerator gen;
    private boolean ioTrouble = false;

    private final Stack graphicContextStack = new Stack();
    private GraphicContext graphicContext = new GraphicContext();

    private PCLPageDefinition currentPageDefinition;
    private int currentPrintDirection = 0;
    private GeneralPath currentPath = null;
    private java.awt.Color currentFillColor = null;

    /**
     * Utility class which enables all sorts of features that are not directly
     * connected to the normal rendering process.
     */
    private PCLRenderingUtil pclUtil;

    /** contains the pageWith of the last printed page */
    private long pageWidth = 0;
    /** contains the pageHeight of the last printed page */
    private long pageHeight = 0;

    /**
     * Create the PCL renderer
     */
    public PCLRenderer() {
    }

    /** {@inheritDoc} */
    @Override
    public void setUserAgent(final FOUserAgent agent) {
        super.setUserAgent(agent);
        this.pclUtil = new PCLRenderingUtil(getUserAgent());
    }

    PCLRenderingUtil getPCLUtil() {
        return this.pclUtil;
    }

    /**
     * Configures the renderer to trade speed for quality if desired. One
     * example here is the way that borders are rendered.
     *
     * @param qualityBeforeSpeed
     *            true if quality is more important than speed
     */
    public void setQualityBeforeSpeed(final boolean qualityBeforeSpeed) {
        this.pclUtil
        .setRenderingMode(qualityBeforeSpeed ? PCLRenderingMode.QUALITY
                : PCLRenderingMode.SPEED);
    }

    /**
     * Controls whether PJL commands shall be generated by the PCL renderer.
     *
     * @param disable
     *            true to disable PJL commands
     */
    public void setPJLDisabled(final boolean disable) {
        this.pclUtil.setPJLDisabled(disable);
    }

    /**
     * Indicates whether PJL generation is disabled.
     *
     * @return true if PJL generation is disabled.
     */
    public boolean isPJLDisabled() {
        return this.pclUtil.isPJLDisabled();
    }

    /**
     * Controls whether all text should be generated as bitmaps or only text for
     * which there's no native font.
     *
     * @param allTextAsBitmaps
     *            true if all text should be painted as bitmaps
     */
    public void setAllTextAsBitmaps(final boolean allTextAsBitmaps) {
        this.pclUtil.setAllTextAsBitmaps(allTextAsBitmaps);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setupFontInfo(final FontInfo inFontInfo) {
        // Don't call super.setupFontInfo() here!
        // The PCLRenderer uses the Java2D FontSetup which needs a special font
        // setup
        // create a temp Image to test font metrics on
        this.fontInfo = inFontInfo;
        final Graphics2D graphics2D = Java2DFontMetrics
                .createFontMetricsGraphics2D();

        final FontCollection[] fontCollections = new FontCollection[] {
                new Base14FontCollection(graphics2D),
                new InstalledFontCollection(graphics2D),
                new ConfiguredFontCollection(getFontResolver(), getFontList()) };
        this.userAgent.getFactory().getFontManager()
        .setup(getFontInfo(), fontCollections);
    }

    /**
     * Central exception handler for I/O exceptions.
     *
     * @param ioe
     *            IOException to handle
     */
    protected void handleIOTrouble(final IOException ioe) {
        if (!this.ioTrouble) {
            final RendererEventProducer eventProducer = RendererEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.ioError(this, ioe);
            this.ioTrouble = true;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Graphics2DAdapter getGraphics2DAdapter() {
        return new PCLGraphics2DAdapter();
    }

    /**
     * @return the GraphicContext used to track coordinate system
     *         transformations
     */
    public GraphicContext getGraphicContext() {
        return this.graphicContext;
    }

    /** @return the target resolution */
    protected int getResolution() {
        final int resolution = Math.round(this.userAgent.getTargetResolution());
        if (resolution <= 300) {
            return 300;
        } else {
            return 600;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startRenderer(final OutputStream outputStream)
            throws IOException {
        log.debug("Rendering areas to PCL...");
        this.out = outputStream;
        this.gen = new PCLGenerator(this.out, getResolution());

        if (!isPJLDisabled()) {
            this.gen.universalEndOfLanguage();
            this.gen.writeText("@PJL COMMENT Produced by "
                    + this.userAgent.getProducer() + "\n");
            if (this.userAgent.getTitle() != null) {
                this.gen.writeText("@PJL JOB NAME = \""
                        + this.userAgent.getTitle() + "\"\n");
            }
            this.gen.writeText("@PJL SET RESOLUTION = " + getResolution()
                    + "\n");
            this.gen.writeText("@PJL ENTER LANGUAGE = PCL\n");
        }
        this.gen.resetPrinter();
        this.gen.setUnitOfMeasure(getResolution());
        this.gen.setRasterGraphicsResolution(getResolution());
    }

    /** {@inheritDoc} */
    @Override
    public void stopRenderer() throws IOException {
        this.gen.separateJobs();
        this.gen.resetPrinter();
        if (!isPJLDisabled()) {
            this.gen.universalEndOfLanguage();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    /** {@inheritDoc} */
    @Override
    public void renderPage(final PageViewport page) throws IOException,
    FOPException {
        saveGraphicsState();

        // Paper source
        final String paperSource = page
                .getForeignAttributeValue(PCLElementMapping.PCL_PAPER_SOURCE);
        if (paperSource != null) {
            this.gen.selectPaperSource(Integer.parseInt(paperSource));
        }

        // Output bin
        final String outputBin = page
                .getForeignAttributeValue(PCLElementMapping.PCL_OUTPUT_BIN);
        if (outputBin != null) {
            this.gen.selectOutputBin(Integer.parseInt(outputBin));
        }

        // Is Page duplex?
        final String pageDuplex = page
                .getForeignAttributeValue(PCLElementMapping.PCL_DUPLEX_MODE);
        if (pageDuplex != null) {
            this.gen.selectDuplexMode(Integer.parseInt(pageDuplex));
        }

        // Page size
        final long pagewidth = Math.round(page.getViewArea().getWidth());
        final long pageheight = Math.round(page.getViewArea().getHeight());
        selectPageFormat(pagewidth, pageheight);

        super.renderPage(page);

        // Eject page
        this.gen.formFeed();
        restoreGraphicsState();
    }

    private void selectPageFormat(final long pagewidth, final long pageheight)
            throws IOException {
        // Only set the page format if it changes (otherwise duplex printing
        // won't work)
        if (pagewidth != this.pageWidth || pageheight != this.pageHeight) {
            this.pageWidth = pagewidth;
            this.pageHeight = pageheight;

            this.currentPageDefinition = PCLPageDefinition.getPageDefinition(
                    pagewidth, pageheight, 1000);

            if (this.currentPageDefinition == null) {
                this.currentPageDefinition = PCLPageDefinition
                        .getDefaultPageDefinition();
                log.warn("Paper type could not be determined. Falling back to: "
                        + this.currentPageDefinition.getName());
            }
            if (log.isDebugEnabled()) {
                log.debug("page size: "
                        + this.currentPageDefinition.getPhysicalPageSize());
                log.debug("logical page: "
                        + this.currentPageDefinition.getLogicalPageRect());
            }

            if (this.currentPageDefinition.isLandscapeFormat()) {
                this.gen.writeCommand("&l1O"); // Landscape Orientation
            } else {
                this.gen.writeCommand("&l0O"); // Portrait Orientation
            }
            this.gen.selectPageSize(this.currentPageDefinition.getSelector());

            this.gen.clearHorizontalMargins();
            this.gen.setTopMargin(0);
        }
    }

    /** Saves the current graphics state on the stack. */
    protected void saveGraphicsState() {
        this.graphicContextStack.push(this.graphicContext);
        this.graphicContext = (GraphicContext) this.graphicContext.clone();
    }

    /** Restores the last graphics state from the stack. */
    protected void restoreGraphicsState() {
        this.graphicContext = (GraphicContext) this.graphicContextStack.pop();
    }

    /**
     * Clip an area. write a clipping operation given coordinates in the current
     * transform. Coordinates are in points.
     *
     * @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     * @param width
     *            the width of the area
     * @param height
     *            the height of the area
     */
    protected void clipRect(final float x, final float y, final float width,
            final float height) {
        // PCL cannot clip (only HP GL/2 can)
    }

    private Point2D transformedPoint(final float x, final float y) {
        return transformedPoint(Math.round(x), Math.round(y));
    }

    private Point2D transformedPoint(final int x, final int y) {
        return PCLRenderingUtil.transformedPoint(x, y,
                this.graphicContext.getTransform(), this.currentPageDefinition,
                this.currentPrintDirection);
    }

    private void changePrintDirection() {
        final AffineTransform at = this.graphicContext.getTransform();
        int newDir;
        try {
            newDir = PCLRenderingUtil.determinePrintDirection(at);
            if (newDir != this.currentPrintDirection) {
                this.currentPrintDirection = newDir;
                this.gen.changePrintDirection(this.currentPrintDirection);
            }
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void startVParea(final CTM ctm, final Rectangle2D clippingRect) {
        saveGraphicsState();
        final AffineTransform at = new AffineTransform(ctm.toArray());
        this.graphicContext.transform(at);
        changePrintDirection();
        if (log.isDebugEnabled()) {
            log.debug("startVPArea: " + at + " --> "
                    + this.graphicContext.getTransform());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void endVParea() {
        restoreGraphicsState();
        changePrintDirection();
        if (log.isDebugEnabled()) {
            log.debug("endVPArea() --> " + this.graphicContext.getTransform());
        }
    }

    /**
     * Handle block traits. The block could be any sort of block with any
     * positioning so this should render the traits such as border and
     * background in its position.
     *
     * @param block
     *            the block to render the traits
     */
    @Override
    protected void handleBlockTraits(final Block block) {
        final int borderPaddingStart = block.getBorderAndPaddingWidthStart();
        final int borderPaddingBefore = block.getBorderAndPaddingWidthBefore();

        float startx = this.currentIPPosition / 1000f;
        final float starty = this.currentBPPosition / 1000f;
        float width = block.getIPD() / 1000f;
        float height = block.getBPD() / 1000f;

        startx += block.getStartIndent() / 1000f;
        startx -= block.getBorderAndPaddingWidthStart() / 1000f;

        width += borderPaddingStart / 1000f;
        width += block.getBorderAndPaddingWidthEnd() / 1000f;
        height += borderPaddingBefore / 1000f;
        height += block.getBorderAndPaddingWidthAfter() / 1000f;

        drawBackAndBorders(block, startx, starty, width, height);
    }

    /**
     * {@inheritDoc}
     *
     * @todo Copied from AbstractPathOrientedRenderer
     */
    @Override
    protected void handleRegionTraits(final RegionViewport region) {
        final Rectangle2D viewArea = region.getViewArea();
        final float startx = (float) (viewArea.getX() / 1000f);
        final float starty = (float) (viewArea.getY() / 1000f);
        final float width = (float) (viewArea.getWidth() / 1000f);
        final float height = (float) (viewArea.getHeight() / 1000f);

        if (region.getRegionReference().getRegionClass() == FO_REGION_BODY) {
            this.currentBPPosition = region.getBorderAndPaddingWidthBefore();
            this.currentIPPosition = region.getBorderAndPaddingWidthStart();
        }
        drawBackAndBorders(region, startx, starty, width, height);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderText(final TextArea text) {
        renderInlineAreaBackAndBorders(text);

        final String fontname = getInternalFontNameForArea(text);
        final int fontsize = text.getTraitAsInteger(Trait.FONT_SIZE);

        // Determine position
        final int saveIP = this.currentIPPosition;
        final int rx = this.currentIPPosition
                + text.getBorderAndPaddingWidthStart();
        final int bl = this.currentBPPosition + text.getOffset()
                + text.getBaselineOffset();

        try {

            final Color col = (Color) text.getTrait(Trait.COLOR);
            final boolean pclFont = this.pclUtil.isAllTextAsBitmaps() ? false
                    : HardcodedFonts.setFont(this.gen, fontname, fontsize,
                            text.getText());
            if (pclFont) {
                // this.currentFill = col;
                if (col != null) {
                    // useColor(ct);
                    this.gen.setTransparencyMode(true, false);
                    this.gen.selectGrayscale(col);
                }

                saveGraphicsState();
                this.graphicContext.translate(rx, bl);
                setCursorPos(0, 0);
                this.gen.setTransparencyMode(true, true);
                if (text.hasUnderline()) {
                    this.gen.writeCommand("&d0D");
                }
                super.renderText(text); // Updates IPD and renders words and
                // spaces
                if (text.hasUnderline()) {
                    this.gen.writeCommand("&d@");
                }
                restoreGraphicsState();
            } else {
                // Use Java2D to paint different fonts via bitmap
                final Font font = getFontFromArea(text);
                final int baseline = text.getBaselineOffset();

                // for cursive fonts, so the text isn't clipped
                final int extraWidth = font.getFontSize() / 3;
                final FontMetricsMapper mapper = (FontMetricsMapper) this.fontInfo
                        .getMetricsFor(font.getFontName());
                final int maxAscent = mapper.getMaxAscent(font.getFontSize()) / 1000;
                final int additionalBPD = maxAscent - baseline;

                final Graphics2DAdapter g2a = getGraphics2DAdapter();
                final Rectangle paintRect = new Rectangle(rx,
                        this.currentBPPosition + text.getOffset()
                        - additionalBPD, text.getIPD() + extraWidth,
                        text.getBPD() + additionalBPD);
                final RendererContext rc = createRendererContext(paintRect.x,
                        paintRect.y, paintRect.width, paintRect.height, null);
                final Map atts = new java.util.HashMap();
                atts.put(ImageHandlerUtil.CONVERSION_MODE,
                        ImageHandlerUtil.CONVERSION_MODE_BITMAP);
                atts.put(SRC_TRANSPARENCY, "true");
                rc.setProperty(RendererContextConstants.FOREIGN_ATTRIBUTES,
                        atts);

                final Graphics2DImagePainter painter = new Graphics2DImagePainter() {

                    @Override
                    public void paint(final Graphics2D g2d,
                            final Rectangle2D area) {
                        g2d.setFont(mapper.getFont(font.getFontSize()));
                        g2d.translate(0, baseline + additionalBPD);
                        g2d.scale(1000, 1000);
                        g2d.setColor(col);
                        Java2DRenderer.renderText(text, g2d, font);
                        renderTextDecoration(g2d, mapper, fontsize, text, 0, 0);
                    }

                    @Override
                    public Dimension getImageSize() {
                        return paintRect.getSize();
                    }

                };
                g2a.paintImage(painter, rc, paintRect.x, paintRect.y,
                        paintRect.width, paintRect.height);
                this.currentIPPosition = saveIP + text.getAllocIPD();
            }

        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }
    }

    /**
     * Paints the text decoration marks.
     *
     * @param g2d
     *            Graphics2D instance to paint to
     * @param fm
     *            Current typeface
     * @param fontsize
     *            Current font size
     * @param inline
     *            inline area to paint the marks for
     * @param baseline
     *            position of the baseline
     * @param startx
     *            start IPD
     */
    private static void renderTextDecoration(final Graphics2D g2d,
            final FontMetrics fm, final int fontsize, final InlineArea inline,
            final int baseline, final int startx) {
        final boolean hasTextDeco = inline.hasUnderline()
                || inline.hasOverline() || inline.hasLineThrough();
        if (hasTextDeco) {
            final float descender = fm.getDescender(fontsize) / 1000f;
            final float capHeight = fm.getCapHeight(fontsize) / 1000f;
            final float lineWidth = descender / -4f / 1000f;
            final float endx = (startx + inline.getIPD()) / 1000f;
            if (inline.hasUnderline()) {
                final Color ct = (Color) inline.getTrait(Trait.UNDERLINE_COLOR);
                g2d.setColor(ct);
                final float y = baseline - descender / 2f;
                g2d.setStroke(new BasicStroke(lineWidth));
                g2d.draw(new Line2D.Float(startx / 1000f, y / 1000f, endx,
                        y / 1000f));
            }
            if (inline.hasOverline()) {
                final Color ct = (Color) inline.getTrait(Trait.OVERLINE_COLOR);
                g2d.setColor(ct);
                final float y = (float) (baseline - 1.1 * capHeight);
                g2d.setStroke(new BasicStroke(lineWidth));
                g2d.draw(new Line2D.Float(startx / 1000f, y / 1000f, endx,
                        y / 1000f));
            }
            if (inline.hasLineThrough()) {
                final Color ct = (Color) inline
                        .getTrait(Trait.LINETHROUGH_COLOR);
                g2d.setColor(ct);
                final float y = (float) (baseline - 0.45 * capHeight);
                g2d.setStroke(new BasicStroke(lineWidth));
                g2d.draw(new Line2D.Float(startx / 1000f, y / 1000f, endx,
                        y / 1000f));
            }
        }
    }

    /**
     * Sets the current cursor position. The coordinates are transformed to the
     * absolute position on the logical PCL page and then passed on to the
     * PCLGenerator.
     *
     * @param x
     *            the x coordinate (in millipoints)
     * @param y
     *            the y coordinate (in millipoints)
     */
    void setCursorPos(final float x, final float y) {
        try {
            final Point2D transPoint = transformedPoint(x, y);
            this.gen.setCursorPos(transPoint.getX(), transPoint.getY());
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }
    }

    /** Clip using the current path. */
    protected void clip() {
        if (this.currentPath == null) {
            throw new IllegalStateException("No current path available!");
        }
        // TODO Find a good way to do clipping. PCL itself cannot clip.
        this.currentPath = null;
    }

    /**
     * Closes the current subpath by appending a straight line segment from the
     * current point to the starting point of the subpath.
     */
    protected void closePath() {
        this.currentPath.closePath();
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
    protected void lineTo(final float x, final float y) {
        if (this.currentPath == null) {
            this.currentPath = new GeneralPath();
        }
        this.currentPath.lineTo(x, y);
    }

    /**
     * Moves the current point to (x, y), omitting any connecting line segment.
     *
     * @param x
     *            x coordinate
     * @param y
     *            y coordinate
     */
    protected void moveTo(final float x, final float y) {
        if (this.currentPath == null) {
            this.currentPath = new GeneralPath();
        }
        this.currentPath.moveTo(x, y);
    }

    /**
     * Fill a rectangular area.
     *
     * @param x
     *            the x coordinate (in pt)
     * @param y
     *            the y coordinate (in pt)
     * @param width
     *            the width of the rectangle
     * @param height
     *            the height of the rectangle
     */
    protected void fillRect(final float x, final float y, final float width,
            final float height) {
        try {
            setCursorPos(x * 1000, y * 1000);
            this.gen.fillRect((int) (width * 1000), (int) (height * 1000),
                    this.currentFillColor);
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }
    }

    /**
     * Sets the new current fill color.
     *
     * @param color
     *            the color
     */
    protected void updateFillColor(final java.awt.Color color) {
        this.currentFillColor = color;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderWord(final WordArea word) {
        // Font font = getFontFromArea(word.getParentArea());

        final String s = word.getWord();

        try {
            this.gen.writeText(s);
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }

        super.renderWord(word);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderSpace(final SpaceArea space) {
        final AbstractTextArea textArea = (AbstractTextArea) space
                .getParentArea();
        final String s = space.getSpace();
        final char sp = s.charAt(0);
        final Font font = getFontFromArea(textArea);

        final int tws = space.isAdjustable() ? textArea
                .getTextWordSpaceAdjust()
                + 2
                * textArea.getTextLetterSpaceAdjust() : 0;

        final double dx = (font.getCharWidth(sp) + tws) / 100f;
        try {
            this.gen.writeCommand("&a+" + this.gen.formatDouble2(dx) + "H");
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }
        super.renderSpace(space);
    }

    /**
     * Render an inline viewport. This renders an inline viewport by clipping if
     * necessary.
     *
     * @param viewport
     *            the viewport to handle
     * @todo Copied from AbstractPathOrientedRenderer
     */
    @Override
    public void renderViewport(final Viewport viewport) {

        final float x = this.currentIPPosition / 1000f;
        final float y = (this.currentBPPosition + viewport.getOffset()) / 1000f;
        final float width = viewport.getIPD() / 1000f;
        final float height = viewport.getBPD() / 1000f;
        // TODO: Calculate the border rect correctly.
        final float borderPaddingStart = viewport
                .getBorderAndPaddingWidthStart() / 1000f;
        final float borderPaddingBefore = viewport
                .getBorderAndPaddingWidthBefore() / 1000f;
        final float bpwidth = borderPaddingStart
                + viewport.getBorderAndPaddingWidthEnd() / 1000f;
        final float bpheight = borderPaddingBefore
                + viewport.getBorderAndPaddingWidthAfter() / 1000f;

        drawBackAndBorders(viewport, x, y, width + bpwidth, height + bpheight);

        if (viewport.getClip()) {
            saveGraphicsState();

            clipRect(x + borderPaddingStart, y + borderPaddingBefore, width,
                    height);
        }
        super.renderViewport(viewport);

        if (viewport.getClip()) {
            restoreGraphicsState();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderBlockViewport(final BlockViewport bv,
            final List children) {
        // clip and position viewport if necessary

        // save positions
        final int saveIP = this.currentIPPosition;
        final int saveBP = this.currentBPPosition;

        CTM ctm = bv.getCTM();
        final int borderPaddingStart = bv.getBorderAndPaddingWidthStart();
        final int borderPaddingBefore = bv.getBorderAndPaddingWidthBefore();
        // This is the content-rect
        final float width = bv.getIPD() / 1000f;
        final float height = bv.getBPD() / 1000f;

        if (bv.getPositioning() == Block.ABSOLUTE
                || bv.getPositioning() == Block.FIXED) {

            // For FIXED, we need to break out of the current viewports to the
            // one established by the page. We save the state stack for
            // restoration
            // after the block-container has been painted. See below.
            List breakOutList = null;
            if (bv.getPositioning() == Block.FIXED) {
                breakOutList = breakOutOfStateStack();
            }

            final AffineTransform positionTransform = new AffineTransform();
            positionTransform.translate(bv.getXOffset(), bv.getYOffset());

            // "left/"top" (bv.getX/YOffset()) specify the position of the
            // content rectangle
            positionTransform.translate(-borderPaddingStart,
                    -borderPaddingBefore);

            saveGraphicsState();
            // Viewport position
            concatenateTransformationMatrix(UnitConv.mptToPt(positionTransform));

            // Background and borders
            final float bpwidth = (borderPaddingStart + bv
                    .getBorderAndPaddingWidthEnd()) / 1000f;
            final float bpheight = (borderPaddingBefore + bv
                    .getBorderAndPaddingWidthAfter()) / 1000f;
            drawBackAndBorders(bv, 0, 0, width + bpwidth, height + bpheight);

            // Shift to content rectangle after border painting
            final AffineTransform contentRectTransform = new AffineTransform();
            contentRectTransform.translate(borderPaddingStart,
                    borderPaddingBefore);
            concatenateTransformationMatrix(UnitConv
                    .mptToPt(contentRectTransform));

            // Clipping
            if (bv.getClip()) {
                clipRect(0f, 0f, width, height);
            }

            saveGraphicsState();
            // Set up coordinate system for content rectangle
            final AffineTransform contentTransform = ctm.toAffineTransform();
            concatenateTransformationMatrix(UnitConv.mptToPt(contentTransform));

            this.currentIPPosition = 0;
            this.currentBPPosition = 0;
            renderBlocks(bv, children);

            restoreGraphicsState();
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
        // currentFontName = saveFontName;
    }

    /** {@inheritDoc} */
    @Override
    protected void renderReferenceArea(final Block block) {
        // TODO This is the same code as in AbstractPathOrientedRenderer
        // So there's some optimization potential but not otherwise PCLRenderer
        // is a little
        // difficult to derive from AbstractPathOrientedRenderer. Maybe an
        // additional layer
        // between PrintRenderer and AbstractPathOrientedRenderer is necessary.

        // save position and offset
        final int saveIP = this.currentIPPosition;
        final int saveBP = this.currentBPPosition;

        // Establish a new coordinate system
        final AffineTransform at = new AffineTransform();
        at.translate(this.currentIPPosition, this.currentBPPosition);
        at.translate(block.getXOffset(), block.getYOffset());
        at.translate(0, block.getSpaceBefore());

        if (!at.isIdentity()) {
            saveGraphicsState();
            concatenateTransformationMatrix(UnitConv.mptToPt(at));
        }

        this.currentIPPosition = 0;
        this.currentBPPosition = 0;
        handleBlockTraits(block);

        final List children = block.getChildAreas();
        if (children != null) {
            renderBlocks(block, children);
        }

        if (!at.isIdentity()) {
            restoreGraphicsState();
        }

        // stacked and relative blocks effect stacking
        this.currentIPPosition = saveIP;
        this.currentBPPosition = saveBP;
    }

    /** {@inheritDoc} */
    @Override
    protected void renderFlow(final NormalFlow flow) {
        // TODO This is the same code as in AbstractPathOrientedRenderer
        // So there's some optimization potential but not otherwise PCLRenderer
        // is a little
        // difficult to derive from AbstractPathOrientedRenderer. Maybe an
        // additional layer
        // between PrintRenderer and AbstractPathOrientedRenderer is necessary.

        // save position and offset
        final int saveIP = this.currentIPPosition;
        final int saveBP = this.currentBPPosition;

        // Establish a new coordinate system
        final AffineTransform at = new AffineTransform();
        at.translate(this.currentIPPosition, this.currentBPPosition);

        if (!at.isIdentity()) {
            saveGraphicsState();
            concatenateTransformationMatrix(UnitConv.mptToPt(at));
        }

        this.currentIPPosition = 0;
        this.currentBPPosition = 0;
        super.renderFlow(flow);

        if (!at.isIdentity()) {
            restoreGraphicsState();
        }

        // stacked and relative blocks effect stacking
        this.currentIPPosition = saveIP;
        this.currentBPPosition = saveBP;
    }

    /**
     * Concatenates the current transformation matrix with the given one,
     * therefore establishing a new coordinate system.
     *
     * @param at
     *            the transformation matrix to process (coordinates in points)
     */
    protected void concatenateTransformationMatrix(final AffineTransform at) {
        if (!at.isIdentity()) {
            this.graphicContext.transform(UnitConv.ptToMpt(at));
            changePrintDirection();
        }
    }

    private List breakOutOfStateStack() {
        log.debug("Block.FIXED --> break out");
        final List breakOutList = new java.util.ArrayList();
        while (!this.graphicContextStack.empty()) {
            breakOutList.add(0, this.graphicContext);
            restoreGraphicsState();
        }
        return breakOutList;
    }

    private void restoreStateStackAfterBreakOut(final List breakOutList) {
        log.debug("Block.FIXED --> restoring context after break-out");
        for (int i = 0, c = breakOutList.size(); i < c; i++) {
            saveGraphicsState();
            this.graphicContext = (GraphicContext) breakOutList.get(i);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected RendererContext createRendererContext(final int x, final int y,
            final int width, final int height, final Map foreignAttributes) {
        final RendererContext context = super.createRendererContext(x, y,
                width, height, foreignAttributes);
        context.setProperty(PCLRendererContextConstants.PCL_COLOR_CANVAS,
                Boolean.valueOf(this.pclUtil.isColorCanvasEnabled()));
        return context;
    }

    /** {@inheritDoc} */
    @Override
    public void renderImage(final Image image, final Rectangle2D pos) {
        drawImage(image.getURL(), pos, image.getForeignAttributes());
    }

    private static final ImageFlavor[] FLAVORS = new ImageFlavor[] {
        ImageFlavor.GRAPHICS2D, ImageFlavor.BUFFERED_IMAGE,
        ImageFlavor.RENDERED_IMAGE, ImageFlavor.XML_DOM };

    /**
     * Draw an image at the indicated location.
     *
     * @param uri
     *            the URI/URL of the image
     * @param pos
     *            the position of the image
     * @param foreignAttributes
     *            an optional Map with foreign attributes, may be null
     */
    protected void drawImage(String uri, final Rectangle2D pos,
            final Map foreignAttributes) {
        uri = URISpecification.getURL(uri);
        final Rectangle posInt = new Rectangle((int) pos.getX(),
                (int) pos.getY(), (int) pos.getWidth(), (int) pos.getHeight());
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

            // Only now fully load/prepare the image
            final Map hints = ImageUtil.getDefaultHints(sessionContext);
            final org.apache.xmlgraphics.image.loader.Image img = manager
                    .getImage(info, FLAVORS, hints, sessionContext);

            // ...and process the image
            if (img instanceof ImageGraphics2D) {
                final ImageGraphics2D imageG2D = (ImageGraphics2D) img;
                final RendererContext context = createRendererContext(posInt.x,
                        posInt.y, posInt.width, posInt.height,
                        foreignAttributes);
                getGraphics2DAdapter().paintImage(
                        imageG2D.getGraphics2DImagePainter(), context, x, y,
                        posInt.width, posInt.height);
            } else if (img instanceof ImageRendered) {
                final ImageRendered imgRend = (ImageRendered) img;
                final RenderedImage ri = imgRend.getRenderedImage();
                setCursorPos(x, y);
                this.gen.paintBitmap(ri, new Dimension(posInt.width,
                        posInt.height), false);
            } else if (img instanceof ImageXMLDOM) {
                final ImageXMLDOM imgXML = (ImageXMLDOM) img;
                renderDocument(imgXML.getDocument(), imgXML.getRootNamespace(),
                        pos, foreignAttributes);
            } else {
                throw new UnsupportedOperationException(
                        "Unsupported image type: " + img);
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
    }

    /** {@inheritDoc} */
    @Override
    public void renderForeignObject(final ForeignObject fo,
            final Rectangle2D pos) {
        final Document doc = fo.getDocument();
        final String ns = fo.getNameSpace();
        renderDocument(doc, ns, pos, fo.getForeignAttributes());
    }

    /**
     * Common method to render the background and borders for any inline area.
     * The all borders and padding are drawn outside the specified area.
     *
     * @param area
     *            the inline area for which the background, border and padding
     *            is to be rendered
     * @todo Copied from AbstractPathOrientedRenderer
     */
    @Override
    protected void renderInlineAreaBackAndBorders(final InlineArea area) {
        final float x = this.currentIPPosition / 1000f;
        final float y = (this.currentBPPosition + area.getOffset()) / 1000f;
        final float width = area.getIPD() / 1000f;
        final float height = area.getBPD() / 1000f;
        final float borderPaddingStart = area.getBorderAndPaddingWidthStart() / 1000f;
        final float borderPaddingBefore = area.getBorderAndPaddingWidthBefore() / 1000f;
        final float bpwidth = borderPaddingStart
                + area.getBorderAndPaddingWidthEnd() / 1000f;
        final float bpheight = borderPaddingBefore
                + area.getBorderAndPaddingWidthAfter() / 1000f;

        if (height != 0.0f || bpheight != 0.0f && bpwidth != 0.0f) {
            drawBackAndBorders(area, x, y - borderPaddingBefore, width
                    + bpwidth, height + bpheight);
        }
    }

    /**
     * Draw the background and borders. This draws the background and border
     * traits for an area given the position.
     *
     * @param area
     *            the area whose traits are used
     * @param startx
     *            the start x position
     * @param starty
     *            the start y position
     * @param width
     *            the width of the area
     * @param height
     *            the height of the area
     */
    protected void drawBackAndBorders(final Area area, final float startx,
            final float starty, final float width, final float height) {
        final BorderProps bpsBefore = (BorderProps) area
                .getTrait(Trait.BORDER_BEFORE);
        final BorderProps bpsAfter = (BorderProps) area
                .getTrait(Trait.BORDER_AFTER);
        final BorderProps bpsStart = (BorderProps) area
                .getTrait(Trait.BORDER_START);
        final BorderProps bpsEnd = (BorderProps) area
                .getTrait(Trait.BORDER_END);

        // draw background
        Trait.Background back;
        back = (Trait.Background) area.getTrait(Trait.BACKGROUND);
        if (back != null) {

            // Calculate padding rectangle
            float sx = startx;
            float sy = starty;
            float paddRectWidth = width;
            float paddRectHeight = height;

            if (bpsStart != null) {
                sx += bpsStart.width / 1000f;
                paddRectWidth -= bpsStart.width / 1000f;
            }
            if (bpsBefore != null) {
                sy += bpsBefore.width / 1000f;
                paddRectHeight -= bpsBefore.width / 1000f;
            }
            if (bpsEnd != null) {
                paddRectWidth -= bpsEnd.width / 1000f;
            }
            if (bpsAfter != null) {
                paddRectHeight -= bpsAfter.width / 1000f;
            }

            if (back.getColor() != null) {
                updateFillColor(back.getColor());
                fillRect(sx, sy, paddRectWidth, paddRectHeight);
            }

            // background image
            if (back.getImageInfo() != null) {
                final ImageSize imageSize = back.getImageInfo().getSize();
                saveGraphicsState();
                clipRect(sx, sy, paddRectWidth, paddRectHeight);
                int horzCount = (int) (paddRectWidth * 1000
                        / imageSize.getWidthMpt() + 1.0f);
                int vertCount = (int) (paddRectHeight * 1000
                        / imageSize.getHeightMpt() + 1.0f);
                if (back.getRepeat() == EN_NOREPEAT) {
                    horzCount = 1;
                    vertCount = 1;
                } else if (back.getRepeat() == EN_REPEATX) {
                    vertCount = 1;
                } else if (back.getRepeat() == EN_REPEATY) {
                    horzCount = 1;
                }
                // change from points to millipoints
                sx *= 1000;
                sy *= 1000;
                if (horzCount == 1) {
                    sx += back.getHoriz();
                }
                if (vertCount == 1) {
                    sy += back.getVertical();
                }
                for (int x = 0; x < horzCount; x++) {
                    for (int y = 0; y < vertCount; y++) {
                        // place once
                        Rectangle2D pos;
                        // Image positions are relative to the currentIP/BP
                        pos = new Rectangle2D.Float(sx - this.currentIPPosition
                                + x * imageSize.getWidthMpt(), sy
                                - this.currentBPPosition + y
                                * imageSize.getHeightMpt(),
                                imageSize.getWidthMpt(),
                                imageSize.getHeightMpt());
                        drawImage(back.getURL(), pos, null);
                    }
                }
                restoreGraphicsState();
            }
        }

        final Rectangle2D.Float borderRect = new Rectangle2D.Float(startx,
                starty, width, height);
        drawBorders(borderRect, bpsBefore, bpsAfter, bpsStart, bpsEnd);
    }

    /**
     * Draws borders.
     *
     * @param borderRect
     *            the border rectangle
     * @param bpsBefore
     *            the border specification on the before side
     * @param bpsAfter
     *            the border specification on the after side
     * @param bpsStart
     *            the border specification on the start side
     * @param bpsEnd
     *            the border specification on the end side
     */
    protected void drawBorders(final Rectangle2D.Float borderRect,
            final BorderProps bpsBefore, final BorderProps bpsAfter,
            final BorderProps bpsStart, final BorderProps bpsEnd) {
        if (bpsBefore == null && bpsAfter == null && bpsStart == null
                && bpsEnd == null) {
            return; // no borders to paint
        }
        if (PCLRenderingMode.SPEED == this.pclUtil.getRenderingMode()) {
            drawFastBorders(borderRect, bpsBefore, bpsAfter, bpsStart, bpsEnd);
        } else {
            drawQualityBorders(borderRect, bpsBefore, bpsAfter, bpsStart,
                    bpsEnd);
        }
    }

    /**
     * Draws borders. Borders are drawn as shaded rectangles with no clipping.
     *
     * @param borderRect
     *            the border rectangle
     * @param bpsBefore
     *            the border specification on the before side
     * @param bpsAfter
     *            the border specification on the after side
     * @param bpsStart
     *            the border specification on the start side
     * @param bpsEnd
     *            the border specification on the end side
     */
    protected void drawFastBorders(final Rectangle2D.Float borderRect,
            final BorderProps bpsBefore, final BorderProps bpsAfter,
            final BorderProps bpsStart, final BorderProps bpsEnd) {
        final float startx = borderRect.x;
        final float starty = borderRect.y;
        final float width = borderRect.width;
        final float height = borderRect.height;
        if (bpsBefore != null) {
            final float borderWidth = bpsBefore.width / 1000f;
            updateFillColor(bpsBefore.color);
            fillRect(startx, starty, width, borderWidth);
        }
        if (bpsAfter != null) {
            final float borderWidth = bpsAfter.width / 1000f;
            updateFillColor(bpsAfter.color);
            fillRect(startx, starty + height - borderWidth, width, borderWidth);
        }
        if (bpsStart != null) {
            final float borderWidth = bpsStart.width / 1000f;
            updateFillColor(bpsStart.color);
            fillRect(startx, starty, borderWidth, height);
        }
        if (bpsEnd != null) {
            final float borderWidth = bpsEnd.width / 1000f;
            updateFillColor(bpsEnd.color);
            fillRect(startx + width - borderWidth, starty, borderWidth, height);
        }
    }

    /**
     * Draws borders. Borders are drawn in-memory and painted as a bitmap.
     *
     * @param borderRect
     *            the border rectangle
     * @param bpsBefore
     *            the border specification on the before side
     * @param bpsAfter
     *            the border specification on the after side
     * @param bpsStart
     *            the border specification on the start side
     * @param bpsEnd
     *            the border specification on the end side
     */
    protected void drawQualityBorders(final Rectangle2D.Float borderRect,
            final BorderProps bpsBefore, final BorderProps bpsAfter,
            final BorderProps bpsStart, final BorderProps bpsEnd) {
        final Graphics2DAdapter g2a = getGraphics2DAdapter();
        final Rectangle.Float effBorderRect = new Rectangle2D.Float(0, 0,
                borderRect.width, borderRect.height);
        final Rectangle paintRect = new Rectangle(
                Math.round(borderRect.x * 1000f),
                Math.round(borderRect.y * 1000f),
                (int) Math.floor(borderRect.width * 1000f) + 1,
                (int) Math.floor(borderRect.height * 1000f) + 1);
        // Add one pixel wide safety margin around the paint area
        final int pixelWidth = (int) Math.round(UnitConv.in2mpt(1)
                / this.userAgent.getTargetResolution());
        final int xoffset = Math.round(-effBorderRect.x * 1000f) + pixelWidth;
        final int yoffset = pixelWidth;
        paintRect.x += xoffset;
        paintRect.y += yoffset;
        paintRect.width += 2 * pixelWidth;
        paintRect.height += 2 * pixelWidth;

        final RendererContext rc = createRendererContext(paintRect.x,
                paintRect.y, paintRect.width, paintRect.height, null);
        final Map atts = new java.util.HashMap();
        atts.put(ImageHandlerUtil.CONVERSION_MODE,
                ImageHandlerUtil.CONVERSION_MODE_BITMAP);
        atts.put(SRC_TRANSPARENCY, "true");
        rc.setProperty(RendererContextConstants.FOREIGN_ATTRIBUTES, atts);

        final Graphics2DImagePainter painter = new Graphics2DImagePainter() {

            @Override
            public void paint(final Graphics2D g2d, final Rectangle2D area) {
                g2d.translate(xoffset, yoffset);
                g2d.scale(1000, 1000);
                float startx = effBorderRect.x;
                float starty = effBorderRect.y;
                float width = effBorderRect.width;
                float height = effBorderRect.height;
                final boolean[] b = new boolean[] { bpsBefore != null,
                        bpsEnd != null, bpsAfter != null, bpsStart != null };
                if (!b[0] && !b[1] && !b[2] && !b[3]) {
                    return;
                }
                final float[] bw = new float[] {
                        b[0] ? bpsBefore.width / 1000f : 0.0f,
                                b[1] ? bpsEnd.width / 1000f : 0.0f,
                                        b[2] ? bpsAfter.width / 1000f : 0.0f,
                                                b[3] ? bpsStart.width / 1000f : 0.0f };
                final float[] clipw = new float[] {
                        BorderProps.getClippedWidth(bpsBefore) / 1000f,
                        BorderProps.getClippedWidth(bpsEnd) / 1000f,
                        BorderProps.getClippedWidth(bpsAfter) / 1000f,
                        BorderProps.getClippedWidth(bpsStart) / 1000f };
                starty += clipw[0];
                height -= clipw[0];
                height -= clipw[2];
                startx += clipw[3];
                width -= clipw[3];
                width -= clipw[1];

                final boolean[] slant = new boolean[] { b[3] && b[0],
                        b[0] && b[1], b[1] && b[2], b[2] && b[3] };
                if (bpsBefore != null) {
                    // endTextObject();

                    final float sx1 = startx;
                    final float sx2 = slant[0] ? sx1 + bw[3] - clipw[3] : sx1;
                    final float ex1 = startx + width;
                    final float ex2 = slant[1] ? ex1 - bw[1] + clipw[1] : ex1;
                    final float outery = starty - clipw[0];
                    final float clipy = outery + clipw[0];
                    final float innery = outery + bw[0];

                    // saveGraphicsState();
                    final Graphics2D g = (Graphics2D) g2d.create();
                    moveTo(sx1, clipy);
                    float sx1a = sx1;
                    float ex1a = ex1;
                    if (bpsBefore.mode == BorderProps.COLLAPSE_OUTER) {
                        if (bpsStart != null
                                && bpsStart.mode == BorderProps.COLLAPSE_OUTER) {
                            sx1a -= clipw[3];
                        }
                        if (bpsEnd != null
                                && bpsEnd.mode == BorderProps.COLLAPSE_OUTER) {
                            ex1a += clipw[1];
                        }
                        lineTo(sx1a, outery);
                        lineTo(ex1a, outery);
                    }
                    lineTo(ex1, clipy);
                    lineTo(ex2, innery);
                    lineTo(sx2, innery);
                    closePath();
                    // clip();
                    g.clip(PCLRenderer.this.currentPath);
                    PCLRenderer.this.currentPath = null;
                    final Rectangle2D.Float lineRect = new Rectangle2D.Float(
                            sx1a, outery, ex1a - sx1a, innery - outery);
                    Java2DRenderer.drawBorderLine(lineRect, true, true,
                            bpsBefore.style, bpsBefore.color, g);
                    // restoreGraphicsState();
                }
                if (bpsEnd != null) {
                    // endTextObject();

                    final float sy1 = starty;
                    final float sy2 = slant[1] ? sy1 + bw[0] - clipw[0] : sy1;
                    final float ey1 = starty + height;
                    final float ey2 = slant[2] ? ey1 - bw[2] + clipw[2] : ey1;
                    final float outerx = startx + width + clipw[1];
                    final float clipx = outerx - clipw[1];
                    final float innerx = outerx - bw[1];

                    // saveGraphicsState();
                    final Graphics2D g = (Graphics2D) g2d.create();
                    moveTo(clipx, sy1);
                    float sy1a = sy1;
                    float ey1a = ey1;
                    if (bpsEnd.mode == BorderProps.COLLAPSE_OUTER) {
                        if (bpsBefore != null
                                && bpsBefore.mode == BorderProps.COLLAPSE_OUTER) {
                            sy1a -= clipw[0];
                        }
                        if (bpsAfter != null
                                && bpsAfter.mode == BorderProps.COLLAPSE_OUTER) {
                            ey1a += clipw[2];
                        }
                        lineTo(outerx, sy1a);
                        lineTo(outerx, ey1a);
                    }
                    lineTo(clipx, ey1);
                    lineTo(innerx, ey2);
                    lineTo(innerx, sy2);
                    closePath();
                    // clip();
                    g.setClip(PCLRenderer.this.currentPath);
                    PCLRenderer.this.currentPath = null;
                    final Rectangle2D.Float lineRect = new Rectangle2D.Float(
                            innerx, sy1a, outerx - innerx, ey1a - sy1a);
                    Java2DRenderer.drawBorderLine(lineRect, false, false,
                            bpsEnd.style, bpsEnd.color, g);
                    // restoreGraphicsState();
                }
                if (bpsAfter != null) {
                    // endTextObject();

                    final float sx1 = startx;
                    final float sx2 = slant[3] ? sx1 + bw[3] - clipw[3] : sx1;
                    final float ex1 = startx + width;
                    final float ex2 = slant[2] ? ex1 - bw[1] + clipw[1] : ex1;
                    final float outery = starty + height + clipw[2];
                    final float clipy = outery - clipw[2];
                    final float innery = outery - bw[2];

                    // saveGraphicsState();
                    final Graphics2D g = (Graphics2D) g2d.create();
                    moveTo(ex1, clipy);
                    float sx1a = sx1;
                    float ex1a = ex1;
                    if (bpsAfter.mode == BorderProps.COLLAPSE_OUTER) {
                        if (bpsStart != null
                                && bpsStart.mode == BorderProps.COLLAPSE_OUTER) {
                            sx1a -= clipw[3];
                        }
                        if (bpsEnd != null
                                && bpsEnd.mode == BorderProps.COLLAPSE_OUTER) {
                            ex1a += clipw[1];
                        }
                        lineTo(ex1a, outery);
                        lineTo(sx1a, outery);
                    }
                    lineTo(sx1, clipy);
                    lineTo(sx2, innery);
                    lineTo(ex2, innery);
                    closePath();
                    // clip();
                    g.setClip(PCLRenderer.this.currentPath);
                    PCLRenderer.this.currentPath = null;
                    final Rectangle2D.Float lineRect = new Rectangle2D.Float(
                            sx1a, innery, ex1a - sx1a, outery - innery);
                    Java2DRenderer.drawBorderLine(lineRect, true, false,
                            bpsAfter.style, bpsAfter.color, g);
                    // restoreGraphicsState();
                }
                if (bpsStart != null) {
                    // endTextObject();

                    final float sy1 = starty;
                    final float sy2 = slant[0] ? sy1 + bw[0] - clipw[0] : sy1;
                    final float ey1 = sy1 + height;
                    final float ey2 = slant[3] ? ey1 - bw[2] + clipw[2] : ey1;
                    final float outerx = startx - clipw[3];
                    final float clipx = outerx + clipw[3];
                    final float innerx = outerx + bw[3];

                    // saveGraphicsState();
                    final Graphics2D g = (Graphics2D) g2d.create();
                    moveTo(clipx, ey1);
                    float sy1a = sy1;
                    float ey1a = ey1;
                    if (bpsStart.mode == BorderProps.COLLAPSE_OUTER) {
                        if (bpsBefore != null
                                && bpsBefore.mode == BorderProps.COLLAPSE_OUTER) {
                            sy1a -= clipw[0];
                        }
                        if (bpsAfter != null
                                && bpsAfter.mode == BorderProps.COLLAPSE_OUTER) {
                            ey1a += clipw[2];
                        }
                        lineTo(outerx, ey1a);
                        lineTo(outerx, sy1a);
                    }
                    lineTo(clipx, sy1);
                    lineTo(innerx, sy2);
                    lineTo(innerx, ey2);
                    closePath();
                    // clip();
                    g.setClip(PCLRenderer.this.currentPath);
                    PCLRenderer.this.currentPath = null;
                    final Rectangle2D.Float lineRect = new Rectangle2D.Float(
                            outerx, sy1a, innerx - outerx, ey1a - sy1a);
                    Java2DRenderer.drawBorderLine(lineRect, false, false,
                            bpsStart.style, bpsStart.color, g);
                    // restoreGraphicsState();
                }
            }

            @Override
            public Dimension getImageSize() {
                return paintRect.getSize();
            }

        };
        try {
            g2a.paintImage(painter, rc, paintRect.x - xoffset, paintRect.y,
                    paintRect.width, paintRect.height);
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void renderLeader(final Leader area) {
        renderInlineAreaBackAndBorders(area);

        saveGraphicsState();
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
        case EN_DASHED: // TODO Improve me and following (this is just a
                        // quick-fix ATM)
        case EN_DOUBLE:
        case EN_DOTTED:
        case EN_GROOVE:
        case EN_RIDGE:
            updateFillColor(col);
            fillRect(startx, starty, endx - startx, ruleThickness);
            break;
        default:
            throw new UnsupportedOperationException("rule style not supported");
        }

        restoreGraphicsState();
        super.renderLeader(area);
    }

}
