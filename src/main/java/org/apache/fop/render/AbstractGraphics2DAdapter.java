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

/* $Id: AbstractGraphics2DAdapter.java 721430 2008-11-28 11:13:12Z acumiskey $ */

package org.apache.fop.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;

import org.apache.fop.render.RendererContext.RendererContextWrapper;
import org.apache.xmlgraphics.java2d.Graphics2DImagePainter;
import org.apache.xmlgraphics.util.UnitConv;

/**
 * Abstract base class for Graphics2DAdapter implementations.
 */
public abstract class AbstractGraphics2DAdapter implements Graphics2DAdapter {

    /**
     * Paints the image to a BufferedImage and returns that.
     *
     * @param painter
     *            the painter which will paint the actual image
     * @param context
     *            the renderer context for the current renderer
     * @param resolution
     *            the requested bitmap resolution
     * @param gray
     *            true if the generated image should be in grayscales
     * @param withAlpha
     *            true if an alpha channel should be created
     * @return the generated BufferedImage
     */
    protected BufferedImage paintToBufferedImage(
            final org.apache.xmlgraphics.java2d.Graphics2DImagePainter painter,
            final RendererContextWrapper context, final int resolution,
            final boolean gray, final boolean withAlpha) {
        final int bmw = mpt2px(context.getWidth(), resolution);
        final int bmh = mpt2px(context.getHeight(), resolution);
        BufferedImage bi;
        if (gray) {
            if (withAlpha) {
                bi = createGrayBufferedImageWithAlpha(bmw, bmh);
            } else {
                bi = new BufferedImage(bmw, bmh, BufferedImage.TYPE_BYTE_GRAY);
            }
        } else {
            if (withAlpha) {
                bi = new BufferedImage(bmw, bmh, BufferedImage.TYPE_INT_ARGB);
            } else {
                bi = new BufferedImage(bmw, bmh, BufferedImage.TYPE_INT_RGB);
            }
        }
        final Graphics2D g2d = bi.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            setRenderingHintsForBufferedImage(g2d);

            g2d.setBackground(Color.white);
            g2d.setColor(Color.black);
            if (!withAlpha) {
                g2d.clearRect(0, 0, bmw, bmh);
            }
            /*
             * debug code int off = 2; g2d.drawLine(off, 0, off, bmh);
             * g2d.drawLine(bmw - off, 0, bmw - off, bmh); g2d.drawLine(0, off,
             * bmw, off); g2d.drawLine(0, bmh - off, bmw, bmh - off);
             */
            final double sx = (double) bmw / context.getWidth();
            final double sy = (double) bmh / context.getHeight();
            g2d.scale(sx, sy);

            // Paint the image on the BufferedImage
            final Rectangle2D area = new Rectangle2D.Double(0.0, 0.0,
                    context.getWidth(), context.getHeight());
            painter.paint(g2d, area);
        } finally {
            g2d.dispose();
        }
        return bi;
    }

    /**
     * Converts millipoints to pixels
     *
     * @param unit
     *            the unit to convert in mpts
     * @param resolution
     *            the target resolution
     * @return the converted unit in pixels
     */
    protected int mpt2px(final int unit, final int resolution) {
        return (int) Math.ceil(UnitConv.mpt2px(unit, resolution));
    }

    private static BufferedImage createGrayBufferedImageWithAlpha(
            final int width, final int height) {
        BufferedImage bi;
        final boolean alphaPremultiplied = true;
        final int bands = 2;
        final int[] bits = new int[bands];
        for (int i = 0; i < bands; ++i) {
            bits[i] = 8;
        }
        final ColorModel cm = new ComponentColorModel(
                ColorSpace.getInstance(ColorSpace.CS_GRAY), bits, true,
                alphaPremultiplied, Transparency.TRANSLUCENT,
                DataBuffer.TYPE_BYTE);
        final WritableRaster wr = Raster.createInterleavedRaster(
                DataBuffer.TYPE_BYTE, width, height, bands, new Point(0, 0));
        bi = new BufferedImage(cm, wr, alphaPremultiplied, null);
        return bi;
    }

    /**
     * Sets rendering hints on the Graphics2D created for painting to a
     * BufferedImage. Subclasses can modify the settings to customize the
     * behaviour.
     *
     * @param g2d
     *            the Graphics2D instance
     */
    protected void setRenderingHintsForBufferedImage(final Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    }

    /** {@inheritDoc} */
    @Override
    public void paintImage(final Graphics2DImagePainter painter,
            final RendererContext context, final int x, final int y,
            final int width, final int height) throws IOException {
        paintImage(painter, context, x, y, width, height);
    }

}
