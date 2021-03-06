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

/* $Id: PCLRendererConfigurator.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.render.pcl;

import java.awt.Graphics2D;
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
import org.apache.fop.render.PrintRendererConfigurator;
import org.apache.fop.render.Renderer;
import org.apache.fop.render.intermediate.IFDocumentHandler;
import org.apache.fop.render.java2d.Base14FontCollection;
import org.apache.fop.render.java2d.ConfiguredFontCollection;
import org.apache.fop.render.java2d.InstalledFontCollection;
import org.apache.fop.render.java2d.Java2DFontMetrics;

/**
 * PCL Renderer configurator
 */
public class PCLRendererConfigurator extends PrintRendererConfigurator {

    /**
     * Default constructor
     *
     * @param userAgent
     *            user agent
     */
    public PCLRendererConfigurator(final FOUserAgent userAgent) {
        super(userAgent);
    }

    /**
     * Configure the PCL renderer.
     *
     * @param renderer
     *            PCL renderer
     * @throws FOPException
     *             fop exception
     */
    @Override
    public void configure(final Renderer renderer) throws FOPException {
        final Configuration cfg = super.getRendererConfig(renderer);
        if (cfg != null) {
            final PCLRenderer pclRenderer = (PCLRenderer) renderer;

            final PCLRenderingUtil pclUtil = pclRenderer.getPCLUtil();
            configure(cfg, pclUtil);
        }
        super.configure(renderer);
    }

    private void configure(final Configuration cfg,
            final PCLRenderingUtil pclUtil) throws FOPException {
        final String rendering = cfg.getChild("rendering").getValue(null);
        if (rendering != null) {
            try {
                pclUtil.setRenderingMode(PCLRenderingMode.valueOf(rendering));
            } catch (final IllegalArgumentException e) {
                throw new FOPException(
                        "Valid values for 'rendering' are 'quality', 'speed' and 'bitmap'."
                                + " Value found: " + rendering);
            }
        }

        final String textRendering = cfg.getChild("text-rendering").getValue(
                null);
        if ("bitmap".equalsIgnoreCase(textRendering)) {
            pclUtil.setAllTextAsBitmaps(true);
        } else if ("auto".equalsIgnoreCase(textRendering)) {
            pclUtil.setAllTextAsBitmaps(false);
        } else if (textRendering != null) {
            throw new FOPException(
                    "Valid values for 'text-rendering' are 'auto' and 'bitmap'. Value found: "
                            + textRendering);
        }

        pclUtil.setPJLDisabled(cfg.getChild("disable-pjl").getValueAsBoolean(
                false));
    }

    // ---=== IFDocumentHandler configuration ===---

    /** {@inheritDoc} */
    @Override
    public void configure(final IFDocumentHandler documentHandler)
            throws FOPException {
        final Configuration cfg = super.getRendererConfig(documentHandler
                .getMimeType());
        if (cfg != null) {
            final PCLDocumentHandler pclDocumentHandler = (PCLDocumentHandler) documentHandler;
            final PCLRenderingUtil pclUtil = pclDocumentHandler.getPCLUtil();
            configure(cfg, pclUtil);
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
