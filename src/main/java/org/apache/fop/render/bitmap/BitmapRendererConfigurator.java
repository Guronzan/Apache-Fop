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

/* $Id: BitmapRendererConfigurator.java 820939 2009-10-02 09:19:12Z jeremias $ */

package org.apache.fop.render.bitmap;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fonts.EmbedFontInfo;
import org.apache.fop.fonts.FontCollection;
import org.apache.fop.fonts.FontEventAdapter;
import org.apache.fop.fonts.FontEventListener;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontManager;
import org.apache.fop.fonts.FontResolver;
import org.apache.fop.render.DefaultFontResolver;
import org.apache.fop.render.intermediate.IFDocumentHandler;
import org.apache.fop.render.java2d.Base14FontCollection;
import org.apache.fop.render.java2d.ConfiguredFontCollection;
import org.apache.fop.render.java2d.InstalledFontCollection;
import org.apache.fop.render.java2d.Java2DFontMetrics;
import org.apache.fop.render.java2d.Java2DRenderer;
import org.apache.fop.render.java2d.Java2DRendererConfigurator;
import org.apache.fop.util.ColorUtil;

/**
 * Configurator for bitmap output.
 */
public class BitmapRendererConfigurator extends Java2DRendererConfigurator {

    /**
     * Default constructor
     *
     * @param userAgent
     *            user agent
     */
    public BitmapRendererConfigurator(final FOUserAgent userAgent) {
        super(userAgent);
    }

    // ---=== IFDocumentHandler configuration ===---

    /** {@inheritDoc} */
    @Override
    public void configure(final IFDocumentHandler documentHandler)
            throws FOPException {
        super.configure(documentHandler);
        final Configuration cfg = super.getRendererConfig(documentHandler
                .getMimeType());
        if (cfg != null) {
            final AbstractBitmapDocumentHandler bitmapHandler = (AbstractBitmapDocumentHandler) documentHandler;
            final BitmapRenderingSettings settings = bitmapHandler
                    .getSettings();

            final boolean transparent = cfg.getChild(
                    Java2DRenderer.JAVA2D_TRANSPARENT_PAGE_BACKGROUND)
                    .getValueAsBoolean(settings.hasTransparentPageBackground());
            if (transparent) {
                settings.setPageBackgroundColor(null);
            } else {
                final String background = cfg.getChild("background-color")
                        .getValue(null);
                if (background != null) {
                    settings.setPageBackgroundColor(ColorUtil.parseColorString(
                            this.userAgent, background));
                }
            }

            final boolean antiAliasing = cfg.getChild("anti-aliasing")
                    .getValueAsBoolean(settings.isAntiAliasingEnabled());
            settings.setAntiAliasing(antiAliasing);

            final String optimization = cfg.getChild("rendering")
                    .getValue(null);
            if ("quality".equalsIgnoreCase(optimization)) {
                settings.setQualityRendering(true);
            } else if ("speed".equalsIgnoreCase(optimization)) {
                settings.setQualityRendering(false);
            }

            final String color = cfg.getChild("color-mode").getValue(null);
            if (color != null) {
                if ("rgba".equalsIgnoreCase(color)) {
                    settings.setBufferedImageType(BufferedImage.TYPE_INT_ARGB);
                } else if ("rgb".equalsIgnoreCase(color)) {
                    settings.setBufferedImageType(BufferedImage.TYPE_INT_RGB);
                } else if ("gray".equalsIgnoreCase(color)) {
                    settings.setBufferedImageType(BufferedImage.TYPE_BYTE_GRAY);
                } else if ("binary".equalsIgnoreCase(color)) {
                    settings.setBufferedImageType(BufferedImage.TYPE_BYTE_BINARY);
                } else if ("bi-level".equalsIgnoreCase(color)) {
                    settings.setBufferedImageType(BufferedImage.TYPE_BYTE_BINARY);
                } else {
                    throw new FOPException("Invalid value for color-mode: "
                            + color);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setupFontInfo(final IFDocumentHandler documentHandler,
            final FontInfo fontInfo) throws FOPException {
        final FontManager fontManager = this.userAgent.getFactory()
                .getFontManager();

        final Graphics2D graphics2D = Java2DFontMetrics
                .createFontMetricsGraphics2D();

        final List<FontCollection> fontCollections = new ArrayList<>();
        fontCollections.add(new Base14FontCollection(graphics2D));
        fontCollections.add(new InstalledFontCollection(graphics2D));

        final Configuration cfg = super.getRendererConfig(documentHandler
                .getMimeType());
        if (cfg != null) {
            final FontResolver fontResolver = new DefaultFontResolver(
                    this.userAgent);
            final FontEventListener listener = new FontEventAdapter(
                    this.userAgent.getEventBroadcaster());
            final List<EmbedFontInfo> fontList = buildFontList(cfg,
                    fontResolver, listener);
            fontCollections.add(new ConfiguredFontCollection(fontResolver,
                    fontList));
        }

        fontManager.setup(fontInfo, fontCollections
                .toArray(new FontCollection[fontCollections.size()]));
        documentHandler.setFontInfo(fontInfo);
    }

}
