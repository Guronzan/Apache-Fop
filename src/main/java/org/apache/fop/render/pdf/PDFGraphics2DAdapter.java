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

/* $Id: PDFGraphics2DAdapter.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.render.pdf;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.apache.fop.render.AbstractGraphics2DAdapter;
import org.apache.fop.render.RendererContext;
import org.apache.fop.render.RendererContext.RendererContextWrapper;
import org.apache.fop.svg.PDFGraphics2D;
import org.apache.xmlgraphics.java2d.Graphics2DImagePainter;

/**
 * Graphics2DAdapter implementation for PDF.
 */
public class PDFGraphics2DAdapter extends AbstractGraphics2DAdapter {

    private final PDFRenderer renderer;

    /**
     * Main constructor
     * 
     * @param renderer
     *            the Renderer instance to which this instance belongs
     */
    public PDFGraphics2DAdapter(final PDFRenderer renderer) {
        this.renderer = renderer;
    }

    /** {@inheritDoc} */
    @Override
    public void paintImage(final Graphics2DImagePainter painter,
            final RendererContext context, final int x, final int y,
            final int width, final int height) throws IOException {

        final PDFContentGenerator generator = this.renderer.getGenerator();
        final PDFSVGHandler.PDFInfo pdfInfo = PDFSVGHandler.getPDFInfo(context);
        final float fwidth = width / 1000f;
        final float fheight = height / 1000f;
        final float fx = x / 1000f;
        final float fy = y / 1000f;

        // get the 'width' and 'height' attributes of the SVG document
        final Dimension dim = painter.getImageSize();
        final float imw = (float) dim.getWidth() / 1000f;
        final float imh = (float) dim.getHeight() / 1000f;

        final float sx = pdfInfo.paintAsBitmap ? 1.0f : fwidth / imw;
        final float sy = pdfInfo.paintAsBitmap ? 1.0f : fheight / imh;

        generator.comment("G2D start");
        generator.saveGraphicsState();
        generator.updateColor(Color.black, false, null);
        generator.updateColor(Color.black, true, null);

        // TODO Clip to the image area.

        // transform so that the coordinates (0,0) is from the top left
        // and positive is down and to the right. (0,0) is where the
        // viewBox puts it.
        generator.add(sx + " 0 0 " + sy + " " + fx + " " + fy + " cm\n");

        final boolean textAsShapes = false;
        if (pdfInfo.pdfContext == null) {
            pdfInfo.pdfContext = pdfInfo.pdfPage;
        }
        final PDFGraphics2D graphics = new PDFGraphics2D(textAsShapes,
                pdfInfo.fi, pdfInfo.pdfDoc, pdfInfo.pdfContext,
                pdfInfo.pdfPage.referencePDF(), pdfInfo.currentFontName,
                pdfInfo.currentFontSize);
        graphics.setGraphicContext(new org.apache.xmlgraphics.java2d.GraphicContext());

        final AffineTransform transform = new AffineTransform();
        transform.translate(fx, fy);
        generator.getState().concatenate(transform);
        graphics.setPaintingState(generator.getState());
        graphics.setOutputStream(pdfInfo.outputStream);

        if (pdfInfo.paintAsBitmap) {
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

        generator.add(graphics.getString());
        generator.restoreGraphicsState();
        generator.comment("G2D end");
    }

    /** {@inheritDoc} */
    @Override
    protected void setRenderingHintsForBufferedImage(final Graphics2D g2d) {
        super.setRenderingHintsForBufferedImage(g2d);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

}
