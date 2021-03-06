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

/* $Id: ImageProxyPanel.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.render.awt.viewer;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import javax.swing.JPanel;

import org.apache.fop.apps.FOPException;
import org.apache.fop.render.awt.AWTRenderer;

/**
 * Panel used to display a single page of a document. This is basically a
 * lazy-load display panel which gets the size of the image for layout purposes
 * but doesn't get the actual image data until needed. The image data is then
 * accessed via a soft reference, so it will be garbage collected when moving
 * through large documents.
 */
public class ImageProxyPanel extends JPanel {

    /**
     *
     */
    private static final long serialVersionUID = -8828404053511562291L;

    /** The reference to the BufferedImage storing the page data */
    private Reference imageRef;

    /** The maximum and preferred size of the panel */
    private Dimension size;

    /** The renderer. Shared with PreviewPanel and PreviewDialog. */
    private final AWTRenderer renderer;

    /** The page to be rendered. */
    private int page;

    /**
     * Panel constructor. Doesn't allocate anything until needed.
     * 
     * @param renderer
     *            the AWTRenderer instance to use for painting
     * @param page
     *            initial page number to show
     */
    public ImageProxyPanel(final AWTRenderer renderer, final int page) {
        this.renderer = renderer;
        this.page = page;
        // Allows single panel to appear behind page display.
        // Important for textured L&Fs.
        setOpaque(false);
    }

    /**
     * @return the size of the page plus the border.
     */
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    /**
     * @return the size of the page plus the border.
     */
    @Override
    public Dimension getPreferredSize() {
        if (this.size == null) {
            try {
                final Insets insets = getInsets();
                this.size = this.renderer.getPageImageSize(this.page);
                this.size = new Dimension(this.size.width + insets.left
                        + insets.right, this.size.height + insets.top
                        + insets.bottom);
            } catch (final FOPException fopEx) {
                // Arbitary size. Doesn't really matter what's returned here.
                return new Dimension(10, 10);
            }
        }
        return this.size;
    }

    /**
     * Sets the number of the page to be displayed and refreshes the display.
     * 
     * @param pg
     *            the page number
     */
    public void setPage(final int pg) {
        if (this.page != pg) {
            this.page = pg;
            this.imageRef = null;
            repaint();
        }
    }

    /**
     * Gets the image data and paints it on screen. Will make calls to
     * getPageImage as required.
     * 
     * @param graphics
     * @see javax.swing.JComponent#paintComponent(Graphics)
     * @see org.apache.fop.render.java2d.Java2DRenderer#getPageImage(int)
     */
    @Override
    public synchronized void paintComponent(final Graphics graphics) {
        try {
            if (isOpaque()) { // paint background
                graphics.setColor(getBackground());
                graphics.fillRect(0, 0, getWidth(), getHeight());
            }

            super.paintComponent(graphics);

            BufferedImage image = null;
            if (this.imageRef == null || this.imageRef.get() == null) {
                image = this.renderer.getPageImage(this.page);
                this.imageRef = new SoftReference(image);
            } else {
                image = (BufferedImage) this.imageRef.get();
            }

            final int x = (getWidth() - image.getWidth()) / 2;
            final int y = (getHeight() - image.getHeight()) / 2;

            graphics.drawImage(image, x, y, image.getWidth(),
                    image.getHeight(), null);
        } catch (final FOPException fopEx) {
            fopEx.printStackTrace();
        }
    }
}
