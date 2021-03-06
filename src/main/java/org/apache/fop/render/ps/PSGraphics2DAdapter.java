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

/* $Id: PSGraphics2DAdapter.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.render.ps;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

import org.apache.fop.render.AbstractGraphics2DAdapter;
import org.apache.fop.render.ImageHandlerUtil;
import org.apache.fop.render.RendererContext;
import org.apache.fop.render.RendererContext.RendererContextWrapper;
import org.apache.fop.render.RendererContextConstants;
import org.apache.fop.render.pdf.PDFRenderer;
import org.apache.xmlgraphics.java2d.Graphics2DImagePainter;
import org.apache.xmlgraphics.java2d.ps.PSGraphics2D;
import org.apache.xmlgraphics.ps.PSGenerator;

/**
 * Graphics2DAdapter implementation for PostScript.
 */
public class PSGraphics2DAdapter extends AbstractGraphics2DAdapter {

    private final PSGenerator gen;
    private boolean clip = true;

    /**
     * Main constructor
     * 
     * @param renderer
     *            the Renderer instance to which this instance belongs
     */
    public PSGraphics2DAdapter(final PSRenderer renderer) {
        this(renderer.gen, true);
    }

    /**
     * Constructor for use without a PSRenderer instance.
     * 
     * @param gen
     *            the PostScript generator
     * @param clip
     *            true if the image should be clipped
     */
    public PSGraphics2DAdapter(final PSGenerator gen, final boolean clip) {
        this.gen = gen;
        this.clip = clip;
    }

    /** {@inheritDoc} */
    @Override
    public void paintImage(final Graphics2DImagePainter painter,
            final RendererContext context, final int x, final int y,
            final int width, final int height) throws IOException {
        final float fwidth = width / 1000f;
        final float fheight = height / 1000f;
        final float fx = x / 1000f;
        final float fy = y / 1000f;

        // get the 'width' and 'height' attributes of the SVG document
        final Dimension dim = painter.getImageSize();
        final float imw = (float) dim.getWidth() / 1000f;
        final float imh = (float) dim.getHeight() / 1000f;

        boolean paintAsBitmap = false;
        if (context != null) {
            final Map foreign = (Map) context
                    .getProperty(RendererContextConstants.FOREIGN_ATTRIBUTES);
            paintAsBitmap = foreign != null
                    && ImageHandlerUtil.isConversionModeBitmap(foreign);
        }

        final float sx = paintAsBitmap ? 1.0f : fwidth / imw;
        final float sy = paintAsBitmap ? 1.0f : fheight / imh;

        this.gen.commentln("%FOPBeginGraphics2D");
        this.gen.saveGraphicsState();
        if (this.clip) {
            // Clip to the image area.
            this.gen.writeln("newpath");
            this.gen.defineRect(fx, fy, fwidth, fheight);
            this.gen.writeln("clip");
        }

        // transform so that the coordinates (0,0) is from the top left
        // and positive is down and to the right. (0,0) is where the
        // viewBox puts it.
        this.gen.concatMatrix(sx, 0, 0, sy, fx, fy);

        final boolean textAsShapes = false;
        final PSGraphics2D graphics = new PSGraphics2D(textAsShapes, this.gen);
        graphics.setGraphicContext(new org.apache.xmlgraphics.java2d.GraphicContext());
        final AffineTransform transform = new AffineTransform();
        // scale to viewbox
        transform.translate(fx, fy);
        this.gen.getCurrentState().concatMatrix(transform);
        if (paintAsBitmap) {
            // Fallback solution: Paint to a BufferedImage
            final int resolution = Math.round(context.getUserAgent()
                    .getTargetResolution());
            final RendererContextWrapper ctx = RendererContext
                    .wrapRendererContext(context);
            final BufferedImage bi = paintToBufferedImage(painter, ctx,
                    resolution, false, false);

            final float scale = PDFRenderer.NORMAL_PDF_RESOLUTION
                    / context.getUserAgent().getTargetResolution();
            graphics.drawImage(bi,
                    new AffineTransform(scale, 0, 0, scale, 0, 0), null);
        } else {
            final Rectangle2D area = new Rectangle2D.Double(0.0, 0.0, imw, imh);
            painter.paint(graphics, area);
        }

        this.gen.restoreGraphicsState();
        this.gen.commentln("%FOPEndGraphics2D");
    }

}
