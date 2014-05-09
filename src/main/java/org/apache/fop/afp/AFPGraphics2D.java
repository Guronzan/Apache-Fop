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

/* $Id: AFPGraphics2D.java 953952 2010-06-12 08:19:48Z jeremias $ */

package org.apache.fop.afp;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.afp.goca.GraphicsSetLineType;
import org.apache.fop.afp.modca.GraphicsObject;
import org.apache.fop.afp.svg.AFPGraphicsConfiguration;
import org.apache.fop.afp.util.CubicBezierApproximator;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.render.afp.AFPImageHandlerRenderedImage;
import org.apache.fop.render.afp.AFPRenderingContext;
import org.apache.fop.svg.NativeImageHandler;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageSize;
import org.apache.xmlgraphics.image.loader.impl.ImageRendered;
import org.apache.xmlgraphics.java2d.AbstractGraphics2D;
import org.apache.xmlgraphics.java2d.GraphicContext;
import org.apache.xmlgraphics.java2d.StrokingTextHandler;
import org.apache.xmlgraphics.java2d.TextHandler;
import org.apache.xmlgraphics.util.UnitConv;

/**
 * This is a concrete implementation of {@link AbstractGraphics2D} (and
 * therefore of {@link Graphics2D}) which is able to generate GOCA byte codes.
 *
 * @see org.apache.xmlgraphics.java2d.AbstractGraphics2D
 */
@Slf4j
public class AFPGraphics2D extends AbstractGraphics2D implements
        NativeImageHandler {

    private static final int X = 0;

    private static final int Y = 1;

    private static final int X1 = 0;

    private static final int Y1 = 1;

    private static final int X2 = 2;

    private static final int Y2 = 3;

    private static final int X3 = 4;

    private static final int Y3 = 5;

    /** graphics object */
    private GraphicsObject graphicsObj = null;

    /** Fallback text handler */
    protected TextHandler fallbackTextHandler = new StrokingTextHandler();

    /** Custom text handler */
    protected TextHandler customTextHandler = null;

    /** AFP resource manager */
    private AFPResourceManager resourceManager = null;

    /** AFP resource info */
    private AFPResourceInfo resourceInfo = null;

    /** Current AFP state */
    private AFPPaintingState paintingState = null;

    /** AFP graphics configuration */
    private final AFPGraphicsConfiguration graphicsConfig = new AFPGraphicsConfiguration();

    /** The AFP FontInfo */
    private FontInfo fontInfo;

    /**
     * Main constructor
     *
     * @param textAsShapes
     *            if true, all text is turned into shapes in the convertion. No
     *            text is output.
     * @param paintingState
     *            painting state
     * @param resourceManager
     *            resource manager
     * @param resourceInfo
     *            resource info
     * @param fontInfo
     *            font info
     */
    public AFPGraphics2D(final boolean textAsShapes,
            final AFPPaintingState paintingState,
            final AFPResourceManager resourceManager,
            final AFPResourceInfo resourceInfo, final FontInfo fontInfo) {
        super(textAsShapes);
        setPaintingState(paintingState);
        setResourceManager(resourceManager);
        setResourceInfo(resourceInfo);
        setFontInfo(fontInfo);
    }

    /**
     * Copy Constructor
     *
     * @param g2d
     *            a AFPGraphics2D whose properties should be copied
     */
    public AFPGraphics2D(final AFPGraphics2D g2d) {
        super(g2d);
        this.paintingState = g2d.paintingState;
        this.resourceManager = g2d.resourceManager;
        this.resourceInfo = g2d.resourceInfo;
        this.fontInfo = g2d.fontInfo;

        this.graphicsObj = g2d.graphicsObj;
        this.fallbackTextHandler = g2d.fallbackTextHandler;
        this.customTextHandler = g2d.customTextHandler;
    }

    /**
     * Sets the AFP resource manager
     *
     * @param resourceManager
     *            the AFP resource manager
     */
    private void setResourceManager(final AFPResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    /**
     * Sets the AFP resource info
     *
     * @param resourceInfo
     *            the AFP resource info
     */
    private void setResourceInfo(final AFPResourceInfo resourceInfo) {
        this.resourceInfo = resourceInfo;
    }

    /**
     * Returns the GOCA graphics object
     *
     * @return the GOCA graphics object
     */
    public GraphicsObject getGraphicsObject() {
        return this.graphicsObj;
    }

    /**
     * Sets the GOCA graphics object
     *
     * @param obj
     *            the GOCA graphics object
     */
    public void setGraphicsObject(final GraphicsObject obj) {
        this.graphicsObj = obj;
    }

    /**
     * Sets the AFP painting state
     *
     * @param paintingState
     *            the AFP painting state
     */
    private void setPaintingState(final AFPPaintingState paintingState) {
        this.paintingState = paintingState;
    }

    /**
     * Returns the AFP painting state
     *
     * @return the AFP painting state
     */
    public AFPPaintingState getPaintingState() {
        return this.paintingState;
    }

    /**
     * Sets the FontInfo
     *
     * @param fontInfo
     *            the FontInfo
     */
    private void setFontInfo(final FontInfo fontInfo) {
        this.fontInfo = fontInfo;
    }

    /**
     * Returns the FontInfo
     *
     * @return the FontInfo
     */
    public FontInfo getFontInfo() {
        return this.fontInfo;
    }

    /**
     * Sets the GraphicContext
     *
     * @param gc
     *            GraphicContext to use
     */
    public void setGraphicContext(final GraphicContext gc) {
        this.gc = gc;
    }

    private int getResolution() {
        return this.paintingState.getResolution();
    }

    /**
     * Converts a length value to an absolute value. Please note that this only
     * uses the "ScaleY" factor, so this will result in a bad value should
     * "ScaleX" and "ScaleY" be different.
     *
     * @param length
     *            the length
     * @return the absolute length
     */
    public double convertToAbsoluteLength(final double length) {
        final AffineTransform current = getTransform();
        final double mult = getResolution() / (double) UnitConv.IN2PT;
        final double factor = -current.getScaleY() / mult;
        return length * factor;
    }

    /**
     * IBM's AFP Workbench paints lines that are wider than expected. We correct
     * manually.
     */
    private static final double GUESSED_WIDTH_CORRECTION = 1.7;

    private static final double SPEC_NORMAL_LINE_WIDTH = UnitConv.in2pt(0.01); // "approx"
    // 0.01
    // inch
    private static final double NORMAL_LINE_WIDTH = SPEC_NORMAL_LINE_WIDTH
            * GUESSED_WIDTH_CORRECTION;

    /**
     * Apply the stroke to the AFP graphics object. This takes the java stroke
     * and outputs the appropriate settings to the AFP graphics object so that
     * the stroke attributes are handled.
     *
     * @param stroke
     *            the java stroke
     */
    protected void applyStroke(final Stroke stroke) {
        if (stroke instanceof BasicStroke) {
            final BasicStroke basicStroke = (BasicStroke) stroke;

            // set line width
            final float lineWidth = basicStroke.getLineWidth();
            if (false) {
                // Old approach. Retained until verified problems with 1440
                // resolution
                this.graphicsObj.setLineWidth(Math.round(lineWidth / 2));
            } else {
                final double absoluteLineWidth = lineWidth
                        * Math.abs(getTransform().getScaleY());
                final double multiplier = absoluteLineWidth / NORMAL_LINE_WIDTH;
                this.graphicsObj.setLineWidth((int) Math.round(multiplier));
                // TODO Use GSFLW instead of GSLW for higher accuracy?
            }

            // No line join, miter limit and end cap support in GOCA. :-(

            // set line type/style (note: this is an approximation at best!)
            final float[] dashArray = basicStroke.getDashArray();
            if (this.paintingState.setDashArray(dashArray)) {
                byte type = GraphicsSetLineType.DEFAULT; // normally SOLID
                if (dashArray != null) {
                    type = GraphicsSetLineType.DOTTED; // default to plain
                    // DOTTED if dashed line
                    // float offset = basicStroke.getDashPhase();
                    if (dashArray.length == 2) {
                        if (dashArray[0] < dashArray[1]) {
                            type = GraphicsSetLineType.SHORT_DASHED;
                        } else if (dashArray[0] > dashArray[1]) {
                            type = GraphicsSetLineType.LONG_DASHED;
                        }
                    } else if (dashArray.length == 4) {
                        if (dashArray[0] > dashArray[1]
                                && dashArray[2] < dashArray[3]) {
                            type = GraphicsSetLineType.DASH_DOT;
                        } else if (dashArray[0] < dashArray[1]
                                && dashArray[2] < dashArray[3]) {
                            type = GraphicsSetLineType.DOUBLE_DOTTED;
                        }
                    } else if (dashArray.length == 6) {
                        if (dashArray[0] > dashArray[1]
                                && dashArray[2] < dashArray[3]
                                && dashArray[4] < dashArray[5]) {
                            type = GraphicsSetLineType.DASH_DOUBLE_DOTTED;
                        }
                    }
                }
                this.graphicsObj.setLineType(type);
            }
        } else {
            log.warn("Unsupported Stroke: " + stroke.getClass().getName());
        }
    }

    /**
     * Apply the java paint to the AFP. This takes the java paint sets up the
     * appropriate AFP commands for the drawing with that paint. Currently this
     * supports the gradients and patterns from batik.
     *
     * @param paint
     *            the paint to convert to AFP
     * @param fill
     *            true if the paint should be set for filling
     * @return true if the paint is handled natively, false if the paint should
     *         be rasterized
     */
    private boolean applyPaint(final Paint paint, final boolean fill) {
        if (paint instanceof Color) {
            return true;
        }
        log.debug("NYI: applyPaint() " + paint + " fill=" + fill);
        if (paint instanceof TexturePaint) {
            // TexturePaint texturePaint = (TexturePaint)paint;
            // BufferedImage bufferedImage = texturePaint.getImage();
            // AffineTransform at = paintingState.getTransform();
            // int x = (int)Math.round(at.getTranslateX());
            // int y = (int)Math.round(at.getTranslateY());
            // drawImage(bufferedImage, x, y, null);
        }
        return false;
    }

    /**
     * Handle the Batik drawing event
     *
     * @param shape
     *            the shape to draw
     * @param fill
     *            true if the shape is to be drawn filled
     */
    private void doDrawing(final Shape shape, final boolean fill) {
        if (!fill) {
            this.graphicsObj.newSegment();
        }

        this.graphicsObj.setColor(this.gc.getColor());

        applyPaint(this.gc.getPaint(), fill);

        if (fill) {
            this.graphicsObj.beginArea();
        } else {
            applyStroke(this.gc.getStroke());
        }

        final AffineTransform trans = this.gc.getTransform();
        final PathIterator iter = shape.getPathIterator(trans);
        if (shape instanceof Line2D) {
            final double[] dstPts = new double[6];
            iter.currentSegment(dstPts);
            final int[] coords = new int[4];
            coords[X1] = (int) Math.round(dstPts[X]);
            coords[Y1] = (int) Math.round(dstPts[Y]);
            iter.next();
            iter.currentSegment(dstPts);
            coords[X2] = (int) Math.round(dstPts[X]);
            coords[Y2] = (int) Math.round(dstPts[Y]);
            this.graphicsObj.addLine(coords);
        } else if (shape instanceof Rectangle2D) {
            final double[] dstPts = new double[6];
            iter.currentSegment(dstPts);
            final int[] coords = new int[4];
            coords[X2] = (int) Math.round(dstPts[X]);
            coords[Y2] = (int) Math.round(dstPts[Y]);
            iter.next();
            iter.next();
            iter.currentSegment(dstPts);
            coords[X1] = (int) Math.round(dstPts[X]);
            coords[Y1] = (int) Math.round(dstPts[Y]);
            this.graphicsObj.addBox(coords);
        } else if (shape instanceof Ellipse2D) {
            final double[] dstPts = new double[6];
            final Ellipse2D elip = (Ellipse2D) shape;
            final double scale = trans.getScaleX();
            final double radiusWidth = elip.getWidth() / 2;
            final double radiusHeight = elip.getHeight() / 2;
            this.graphicsObj.setArcParams(
                    (int) Math.round(radiusWidth * scale),
                    (int) Math.round(radiusHeight * scale), 0, 0);
            final double[] srcPts = new double[] { elip.getCenterX(),
                    elip.getCenterY() };
            trans.transform(srcPts, 0, dstPts, 0, 1);
            final int mh = 1;
            final int mhr = 0;
            this.graphicsObj.addFullArc((int) Math.round(dstPts[X]),
                    (int) Math.round(dstPts[Y]), mh, mhr);
        } else {
            processPathIterator(iter);
        }

        if (fill) {
            this.graphicsObj.endArea();
        }
    }

    /**
     * Processes a path iterator generating the necessary painting operations.
     *
     * @param iter
     *            PathIterator to process
     */
    private void processPathIterator(final PathIterator iter) {
        final double[] dstPts = new double[6];
        double[] currentPosition = new double[2];
        for (int[] openingCoords = new int[2]; !iter.isDone(); iter.next()) {
            switch (iter.currentSegment(dstPts)) {
                case PathIterator.SEG_LINETO:
                    this.graphicsObj.addLine(
                            new int[] { (int) Math.round(dstPts[X]),
                                    (int) Math.round(dstPts[Y]) }, true);
                    currentPosition = new double[] { dstPts[X], dstPts[Y] };
                    break;
                case PathIterator.SEG_QUADTO:
                    this.graphicsObj.addFillet(
                            new int[] { (int) Math.round(dstPts[X1]),
                                    (int) Math.round(dstPts[Y1]),
                                    (int) Math.round(dstPts[X2]),
                                    (int) Math.round(dstPts[Y2]) }, true);
                    currentPosition = new double[] { dstPts[X2], dstPts[Y2] };
                    break;
                case PathIterator.SEG_CUBICTO:
                    final double[] cubicCoords = new double[] {
                            currentPosition[0], currentPosition[1], dstPts[X1],
                            dstPts[Y1], dstPts[X2], dstPts[Y2], dstPts[X3],
                            dstPts[Y3] };
                    final double[][] quadParts = CubicBezierApproximator
                            .fixedMidPointApproximation(cubicCoords);
                    if (quadParts.length >= 4) {
                        for (final double[] quadPts : quadParts) {
                            if (quadPts != null && quadPts.length == 4) {
                                this.graphicsObj.addFillet(new int[] {
                                        (int) Math.round(quadPts[X1]),
                                        (int) Math.round(quadPts[Y1]),
                                        (int) Math.round(quadPts[X2]),
                                        (int) Math.round(quadPts[Y2]) }, true);
                                currentPosition = new double[] { quadPts[X2],
                                        quadPts[Y2] };
                            }
                        }
                    }
                    break;
                case PathIterator.SEG_MOVETO:
                    openingCoords = new int[] { (int) Math.round(dstPts[X]),
                            (int) Math.round(dstPts[Y]) };
                    currentPosition = new double[] { dstPts[X], dstPts[Y] };
                    this.graphicsObj.setCurrentPosition(openingCoords);
                    break;
                case PathIterator.SEG_CLOSE:
                    this.graphicsObj.addLine(openingCoords, true);
                    currentPosition = new double[] { openingCoords[0],
                            openingCoords[1] };
                    break;
                default:
                    log.debug("Unrecognised path iterator type");
                    break;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void draw(final Shape shape) {
        log.debug("draw() shape=" + shape);
        doDrawing(shape, false);
    }

    /** {@inheritDoc} */
    @Override
    public void fill(final Shape shape) {
        log.debug("fill() shape=" + shape);
        doDrawing(shape, true);
    }

    /**
     * Central handler for IOExceptions for this class.
     *
     * @param ioe
     *            IOException to handle
     */
    public void handleIOException(final IOException ioe) {
        // TODO Surely, there's a better way to do this.
        log.error(ioe.getMessage(), ioe);
    }

    /** {@inheritDoc} */
    @Override
    public void drawString(final String str, final float x, final float y) {
        try {
            if (this.customTextHandler != null && !this.textAsShapes) {
                this.customTextHandler.drawString(this, str, x, y);
            } else {
                this.fallbackTextHandler.drawString(this, str, x, y);
            }
        } catch (final IOException ioe) {
            handleIOException(ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return this.graphicsConfig;
    }

    /** {@inheritDoc} */
    @Override
    public Graphics create() {
        return new AFPGraphics2D(this);
    }

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        this.graphicsObj = null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean drawImage(final Image img, final int x, final int y,
            final ImageObserver observer) {
        return drawImage(img, x, y, img.getWidth(observer),
                img.getHeight(observer), observer);
    }

    private BufferedImage buildBufferedImage(final Dimension size) {
        return new BufferedImage(size.width, size.height,
                BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Draws an AWT image into a BufferedImage using an AWT Graphics2D
     * implementation
     *
     * @param img
     *            the AWT image
     * @param bufferedImage
     *            the AWT buffered image
     * @param width
     *            the image width
     * @param height
     *            the image height
     * @param observer
     *            the image observer
     * @return true if the image was drawn
     */
    private boolean drawBufferedImage(final Image img,
            final BufferedImage bufferedImage, final int width,
            final int height, final ImageObserver observer) {

        final java.awt.Graphics2D g2d = bufferedImage.createGraphics();
        try {
            g2d.setComposite(AlphaComposite.SrcOver);

            final Color color = new Color(1, 1, 1, 0);
            g2d.setBackground(color);
            g2d.setPaint(color);

            g2d.fillRect(0, 0, width, height);

            final int imageWidth = bufferedImage.getWidth();
            final int imageHeight = bufferedImage.getHeight();
            final Rectangle clipRect = new Rectangle(0, 0, imageWidth,
                    imageHeight);
            g2d.clip(clipRect);

            g2d.setComposite(this.gc.getComposite());

            return g2d.drawImage(img, 0, 0, imageWidth, imageHeight, observer);
        } finally {
            g2d.dispose(); // drawn so dispose immediately to free system
            // resource
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean drawImage(final Image img, final int x, final int y,
            final int width, final int height, final ImageObserver observer) {
        // draw with AWT Graphics2D
        final Dimension imageSize = new Dimension(width, height);
        final BufferedImage bufferedImage = buildBufferedImage(imageSize);

        final boolean drawn = drawBufferedImage(img, bufferedImage, width,
                height, observer);
        if (drawn) {
            drawRenderedImage(bufferedImage, new AffineTransform());
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void drawRenderedImage(final RenderedImage img,
            final AffineTransform xform) {
        final int imgWidth = img.getWidth();
        final int imgHeight = img.getHeight();

        final AffineTransform gat = this.gc.getTransform();
        final int graphicsObjectHeight = this.graphicsObj
                .getObjectEnvironmentGroup().getObjectAreaDescriptor()
                .getHeight();

        final double toMillipointFactor = UnitConv.IN2PT * 1000
                / (double) this.paintingState.getResolution();
        double x = gat.getTranslateX();
        double y = -(gat.getTranslateY() - graphicsObjectHeight);
        x = toMillipointFactor * x;
        y = toMillipointFactor * y;
        final double w = toMillipointFactor * imgWidth * gat.getScaleX();
        final double h = toMillipointFactor * imgHeight * -gat.getScaleY();

        final AFPImageHandlerRenderedImage handler = new AFPImageHandlerRenderedImage();
        final ImageInfo imageInfo = new ImageInfo(null, null);
        imageInfo.setSize(new ImageSize(img.getWidth(), img.getHeight(),
                this.paintingState.getResolution()));
        imageInfo.getSize().calcSizeFromPixels();
        final ImageRendered red = new ImageRendered(imageInfo, img, null);
        final Rectangle targetPos = new Rectangle((int) Math.round(x),
                (int) Math.round(y), (int) Math.round(w), (int) Math.round(h));
        final AFPRenderingContext context = new AFPRenderingContext(null,
                this.resourceManager, this.paintingState, this.fontInfo, null);
        try {
            handler.handleImage(context, red, targetPos);
        } catch (final IOException ioe) {
            handleIOException(ioe);
        }
    }

    /**
     * Sets a custom TextHandler implementation that is responsible for painting
     * text. The default TextHandler paints all text as shapes. A custom
     * implementation can implement text painting using text painting operators.
     *
     * @param handler
     *            the custom TextHandler implementation
     */
    public void setCustomTextHandler(final TextHandler handler) {
        this.customTextHandler = handler;
    }

    /** {@inheritDoc} */
    @Override
    public void drawRenderableImage(final RenderableImage img,
            final AffineTransform xform) {
        log.debug("drawRenderableImage() NYI: img=" + img + ", xform=" + xform);
    }

    /** {@inheritDoc} */
    @Override
    public FontMetrics getFontMetrics(final Font f) {
        log.debug("getFontMetrics() NYI: f=" + f);
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void setXORMode(final Color col) {
        log.debug("setXORMode() NYI: col=" + col);
    }

    /** {@inheritDoc} */
    @Override
    public void addNativeImage(
            final org.apache.xmlgraphics.image.loader.Image image,
            final float x, final float y, final float width, final float height) {
        log.debug("NYI: addNativeImage() " + "image=" + image + ",x=" + x
                + ",y=" + y + ",width=" + width + ",height=" + height);
    }

    /** {@inheritDoc} */
    @Override
    public void copyArea(final int x, final int y, final int width,
            final int height, final int dx, final int dy) {
        log.debug("copyArea() NYI: ");
    }
}
