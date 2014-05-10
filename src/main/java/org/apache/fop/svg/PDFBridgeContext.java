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

/* $Id: PDFBridgeContext.java 1069439 2011-02-10 15:58:57Z jeremias $ */

package org.apache.fop.svg;

import java.awt.geom.AffineTransform;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.SVGTextElementBridge;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.dom.svg.SVGOMDocument;
import org.apache.batik.gvt.TextPainter;
import org.apache.fop.fonts.FontInfo;
import org.apache.xmlgraphics.image.loader.ImageManager;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;

/**
 * BridgeContext which registers the custom bridges for PDF output.
 */
public class PDFBridgeContext extends AbstractFOPBridgeContext {

    /**
     * Constructs a new bridge context.
     * 
     * @param userAgent
     *            the user agent
     * @param documentLoader
     *            the Document Loader to use for referenced documents.
     * @param fontInfo
     *            the font list for the text painter, may be null in which case
     *            text is painted as shapes
     * @param imageManager
     *            an image manager
     * @param imageSessionContext
     *            an image session context
     * @param linkTransform
     *            AffineTransform to properly place links, may be null
     */
    public PDFBridgeContext(final UserAgent userAgent,
            final DocumentLoader documentLoader, final FontInfo fontInfo,
            final ImageManager imageManager,
            final ImageSessionContext imageSessionContext,
            final AffineTransform linkTransform) {
        super(userAgent, documentLoader, fontInfo, imageManager,
                imageSessionContext, linkTransform);
    }

    /**
     * Constructs a new bridge context.
     * 
     * @param userAgent
     *            the user agent
     * @param fontInfo
     *            the font list for the text painter, may be null in which case
     *            text is painted as shapes
     * @param imageManager
     *            an image manager
     * @param imageSessionContext
     *            an image session context
     */
    public PDFBridgeContext(final UserAgent userAgent, final FontInfo fontInfo,
            final ImageManager imageManager,
            final ImageSessionContext imageSessionContext) {
        super(userAgent, fontInfo, imageManager, imageSessionContext);
    }

    /**
     * Constructs a new bridge context.
     * 
     * @param userAgent
     *            the user agent
     * @param fontInfo
     *            the font list for the text painter, may be null in which case
     *            text is painted as shapes
     * @param imageManager
     *            an image manager
     * @param imageSessionContext
     *            an image session context
     * @param linkTransform
     *            AffineTransform to properly place links, may be null
     */
    public PDFBridgeContext(final SVGUserAgent userAgent,
            final FontInfo fontInfo, final ImageManager imageManager,
            final ImageSessionContext imageSessionContext,
            final AffineTransform linkTransform) {
        super(userAgent, fontInfo, imageManager, imageSessionContext,
                linkTransform);
    }

    /** {@inheritDoc} */
    @Override
    public void registerSVGBridges() {
        super.registerSVGBridges();

        if (this.fontInfo != null) {
            final TextPainter textPainter = new PDFTextPainter(this.fontInfo);
            final SVGTextElementBridge textElementBridge = new PDFTextElementBridge(
                    textPainter);
            putBridge(textElementBridge);

            // Batik flow text extension (may not always be available)
            // putBridge(new PDFBatikFlowTextElementBridge(fontInfo);
            putElementBridgeConditional(
                    "org.apache.fop.svg.PDFBatikFlowTextElementBridge",
                    "org.apache.batik.extension.svg.BatikFlowTextElementBridge");

            // SVG 1.2 flow text support
            // putBridge(new PDFSVG12TextElementBridge(fontInfo)); //-->Batik
            // 1.7
            putElementBridgeConditional(
                    "org.apache.fop.svg.PDFSVG12TextElementBridge",
                    "org.apache.batik.bridge.svg12.SVG12TextElementBridge");

            // putBridge(new PDFSVGFlowRootElementBridge(fontInfo));
            putElementBridgeConditional(
                    "org.apache.fop.svg.PDFSVGFlowRootElementBridge",
                    "org.apache.batik.bridge.svg12.SVGFlowRootElementBridge");
        }

        final PDFAElementBridge pdfAElementBridge = new PDFAElementBridge();
        if (this.linkTransform != null) {
            pdfAElementBridge.setCurrentTransform(this.linkTransform);
        } else {
            pdfAElementBridge.setCurrentTransform(new AffineTransform());
        }
        putBridge(pdfAElementBridge);

        putBridge(new PDFImageElementBridge());
    }

    /** {@inheritDoc} */
    @Override
    public BridgeContext createBridgeContext() {
        // Retained for pre-Batik-1.7 compatibility
        return createBridgeContext(null);
    }

    /** {@inheritDoc} */
    public BridgeContext createBridgeContext(final SVGOMDocument doc) {
        // Make sure any 'sub bridge contexts' also have our bridges.
        return new PDFBridgeContext(getUserAgent(), getDocumentLoader(),
                this.fontInfo, getImageManager(), getImageSessionContext(),
                this.linkTransform);
    }

}
