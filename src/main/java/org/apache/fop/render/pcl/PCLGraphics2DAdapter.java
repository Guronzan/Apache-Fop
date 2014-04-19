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

/* $Id: PCLGraphics2DAdapter.java 687369 2008-08-20 15:13:56Z acumiskey $ */

package org.apache.fop.render.pcl;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.fop.render.AbstractGraphics2DAdapter;
import org.apache.fop.render.RendererContext;
import org.apache.xmlgraphics.java2d.GraphicContext;
import org.apache.xmlgraphics.java2d.Graphics2DImagePainter;
import org.apache.xmlgraphics.util.UnitConv;

/**
 * Graphics2DAdapter implementation for PCL and HP GL/2.
 */
@Slf4j
public class PCLGraphics2DAdapter extends AbstractGraphics2DAdapter {

    /**
     * Main constructor
     */
    public PCLGraphics2DAdapter() {
    }

    /** {@inheritDoc} */
    @Override
    public void paintImage(final Graphics2DImagePainter painter,
            final RendererContext context, final int x, final int y,
            final int width, final int height) throws IOException {
        final PCLRendererContext pclContext = PCLRendererContext
                .wrapRendererContext(context);
        final PCLRenderer pcl = (PCLRenderer) context.getRenderer();
        final PCLGenerator gen = pcl.gen;

        // get the 'width' and 'height' attributes of the image/document
        final Dimension dim = painter.getImageSize();
        final float imw = (float) dim.getWidth();
        final float imh = (float) dim.getHeight();

        boolean painted = false;
        final boolean paintAsBitmap = pclContext.paintAsBitmap();
        if (!paintAsBitmap) {
            final ByteArrayOutputStream baout = new ByteArrayOutputStream();
            final PCLGenerator tempGen = new PCLGenerator(baout,
                    gen.getMaximumBitmapResolution());
            try {
                final GraphicContext ctx = (GraphicContext) pcl
                        .getGraphicContext().clone();

                final AffineTransform prepareHPGL2 = new AffineTransform();
                prepareHPGL2.scale(0.001, 0.001);
                ctx.setTransform(prepareHPGL2);

                final PCLGraphics2D graphics = new PCLGraphics2D(tempGen);
                graphics.setGraphicContext(ctx);
                graphics.setClippingDisabled(pclContext.isClippingDisabled());
                final Rectangle2D area = new Rectangle2D.Double(0.0, 0.0, imw,
                        imh);
                painter.paint(graphics, area);

                // If we arrive here, the graphic is natively paintable, so
                // write the graphic
                pcl.saveGraphicsState();
                pcl.setCursorPos(x, y);
                gen.writeCommand("*c" + gen.formatDouble4(width / 100f) + "x"
                        + gen.formatDouble4(height / 100f) + "Y");
                gen.writeCommand("*c0T");
                gen.enterHPGL2Mode(false);
                gen.writeText("\nIN;");
                gen.writeText("SP1;");
                // One Plotter unit is 0.025mm!
                final double scale = imw / UnitConv.mm2pt(imw * 0.025);
                gen.writeText("SC0," + gen.formatDouble4(scale) + ",0,-"
                        + gen.formatDouble4(scale) + ",2;");
                gen.writeText("IR0,100,0,100;");
                gen.writeText("PU;PA0,0;\n");
                baout.writeTo(gen.getOutputStream()); // Buffer is written to
                // output stream
                gen.writeText("\n");

                gen.enterPCLMode(false);
                pcl.restoreGraphicsState();
                painted = true;
            } catch (final UnsupportedOperationException uoe) {
                log.debug("Cannot paint graphic natively. Falling back to bitmap painting. Reason: "
                        + uoe.getMessage());
            }
        }

        if (!painted) {
            // Fallback solution: Paint to a BufferedImage
            final int resolution = Math.round(context.getUserAgent()
                    .getTargetResolution());
            final BufferedImage bi = paintToBufferedImage(painter, pclContext,
                    resolution, !pclContext.isColorCanvas(), false);

            pcl.setCursorPos(x, y);
            gen.paintBitmap(bi, new Dimension(width, height),
                    pclContext.isSourceTransparency());
        }
    }

}
