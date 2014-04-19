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

/* $Id: PDFSVGHandler.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.render.pdf;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.SVGConstants;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.image.loader.batik.BatikUtil;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFPage;
import org.apache.fop.pdf.PDFResourceContext;
import org.apache.fop.render.AbstractGenericSVGHandler;
import org.apache.fop.render.ImageHandlerUtil;
import org.apache.fop.render.Renderer;
import org.apache.fop.render.RendererContext;
import org.apache.fop.render.RendererContextConstants;
import org.apache.fop.svg.PDFAElementBridge;
import org.apache.fop.svg.PDFBridgeContext;
import org.apache.fop.svg.PDFGraphics2D;
import org.apache.fop.svg.SVGEventProducer;
import org.apache.fop.svg.SVGUserAgent;
import org.w3c.dom.Document;

/**
 * PDF XML handler for SVG (uses Apache Batik). This handler handles XML for
 * foreign objects when rendering to PDF. It renders SVG to the PDF document
 * using the PDFGraphics2D. The properties from the PDF renderer are subject to
 * change.
 */
@Slf4j
public class PDFSVGHandler extends AbstractGenericSVGHandler implements
PDFRendererContextConstants {

    /**
     * Get the pdf information from the render context.
     *
     * @param context
     *            the renderer context
     * @return the pdf information retrieved from the context
     */
    public static PDFInfo getPDFInfo(final RendererContext context) {
        final PDFInfo pdfi = new PDFInfo();
        pdfi.pdfDoc = (PDFDocument) context.getProperty(PDF_DOCUMENT);
        pdfi.outputStream = (OutputStream) context.getProperty(OUTPUT_STREAM);
        // pdfi.pdfState = (PDFState)context.getProperty(PDF_STATE);
        pdfi.pdfPage = (PDFPage) context.getProperty(PDF_PAGE);
        pdfi.pdfContext = (PDFResourceContext) context.getProperty(PDF_CONTEXT);
        // pdfi.currentStream = (PDFStream)context.getProperty(PDF_STREAM);
        pdfi.width = ((Integer) context.getProperty(WIDTH)).intValue();
        pdfi.height = ((Integer) context.getProperty(HEIGHT)).intValue();
        pdfi.fi = (FontInfo) context.getProperty(PDF_FONT_INFO);
        pdfi.currentFontName = (String) context.getProperty(PDF_FONT_NAME);
        pdfi.currentFontSize = ((Integer) context.getProperty(PDF_FONT_SIZE))
                .intValue();
        pdfi.currentXPosition = ((Integer) context.getProperty(XPOS))
                .intValue();
        pdfi.currentYPosition = ((Integer) context.getProperty(YPOS))
                .intValue();
        pdfi.cfg = (Configuration) context.getProperty(HANDLER_CONFIGURATION);
        final Map foreign = (Map) context
                .getProperty(RendererContextConstants.FOREIGN_ATTRIBUTES);
        pdfi.paintAsBitmap = ImageHandlerUtil.isConversionModeBitmap(foreign);
        return pdfi;
    }

    /**
     * PDF information structure for drawing the XML document.
     */
    public static class PDFInfo {
        /** see PDF_DOCUMENT */
        public PDFDocument pdfDoc;
        /** see OUTPUT_STREAM */
        public OutputStream outputStream;
        /** see PDF_PAGE */
        public PDFPage pdfPage;
        /** see PDF_CONTEXT */
        public PDFResourceContext pdfContext;
        /** see PDF_STREAM */
        // public PDFStream currentStream;
        /** see PDF_WIDTH */
        public int width;
        /** see PDF_HEIGHT */
        public int height;
        /** see PDF_FONT_INFO */
        public FontInfo fi;
        /** see PDF_FONT_NAME */
        public String currentFontName;
        /** see PDF_FONT_SIZE */
        public int currentFontSize;
        /** see PDF_XPOS */
        public int currentXPosition;
        /** see PDF_YPOS */
        public int currentYPosition;
        /** see PDF_HANDLER_CONFIGURATION */
        public Configuration cfg;
        /** true if SVG should be rendered as a bitmap instead of natively */
        public boolean paintAsBitmap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderSVGDocument(final RendererContext context,
            final Document doc) {
        final PDFRenderer renderer = (PDFRenderer) context.getRenderer();
        final PDFInfo pdfInfo = getPDFInfo(context);
        if (pdfInfo.paintAsBitmap) {
            try {
                super.renderSVGDocument(context, doc);
            } catch (final IOException ioe) {
                final SVGEventProducer eventProducer = SVGEventProducer.Provider
                        .get(context.getUserAgent().getEventBroadcaster());
                eventProducer.svgRenderingError(this, ioe, getDocumentURI(doc));
            }
            return;
        }
        final int xOffset = pdfInfo.currentXPosition;
        final int yOffset = pdfInfo.currentYPosition;

        final FOUserAgent userAgent = context.getUserAgent();
        final float deviceResolution = userAgent.getTargetResolution();
        if (log.isDebugEnabled()) {
            log.debug("Generating SVG at " + deviceResolution + "dpi.");
        }

        final float uaResolution = userAgent.getSourceResolution();
        final SVGUserAgent ua = new SVGUserAgent(userAgent,
                new AffineTransform());

        // Scale for higher resolution on-the-fly images from Batik
        final double s = uaResolution / deviceResolution;
        final AffineTransform resolutionScaling = new AffineTransform();
        resolutionScaling.scale(s, s);

        // Controls whether text painted by Batik is generated using text or
        // path operations
        boolean strokeText = false;
        final Configuration cfg = pdfInfo.cfg;
        if (cfg != null) {
            strokeText = cfg.getChild("stroke-text", true).getValueAsBoolean(
                    strokeText);
        }

        final BridgeContext ctx = new PDFBridgeContext(ua, strokeText ? null
                : pdfInfo.fi, userAgent.getFactory().getImageManager(),
                userAgent.getImageSessionContext(), new AffineTransform());

        // Cloning SVG DOM as Batik attaches non-thread-safe facilities (like
        // the CSS engine)
        // to it.
        final Document clonedDoc = BatikUtil.cloneSVGDocument(doc);

        GraphicsNode root;
        try {
            final GVTBuilder builder = new GVTBuilder();
            root = builder.build(ctx, clonedDoc);
        } catch (final Exception e) {
            final SVGEventProducer eventProducer = SVGEventProducer.Provider
                    .get(context.getUserAgent().getEventBroadcaster());
            eventProducer.svgNotBuilt(this, e, getDocumentURI(doc));
            return;
        }
        // get the 'width' and 'height' attributes of the SVG document
        final float w = (float) ctx.getDocumentSize().getWidth() * 1000f;
        final float h = (float) ctx.getDocumentSize().getHeight() * 1000f;

        final float sx = pdfInfo.width / w;
        final float sy = pdfInfo.height / h;

        // Scaling and translation for the bounding box of the image
        final AffineTransform scaling = new AffineTransform(sx, 0, 0, sy,
                xOffset / 1000f, yOffset / 1000f);

        // Transformation matrix that establishes the local coordinate system
        // for the SVG graphic
        // in relation to the current coordinate system
        final AffineTransform imageTransform = new AffineTransform();
        imageTransform.concatenate(scaling);
        imageTransform.concatenate(resolutionScaling);

        /*
         * Clip to the svg area. Note: To have the svg overlay (under) a text
         * area then use an fo:block-container
         */
        final PDFContentGenerator generator = renderer.getGenerator();
        generator.comment("SVG setup");
        generator.saveGraphicsState();
        generator.setColor(Color.black, false);
        generator.setColor(Color.black, true);

        if (!scaling.isIdentity()) {
            generator.comment("viewbox");
            generator.add(CTMHelper.toPDFString(scaling, false) + " cm\n");
        }

        // SVGSVGElement svg = ((SVGDocument)doc).getRootElement();

        if (pdfInfo.pdfContext == null) {
            pdfInfo.pdfContext = pdfInfo.pdfPage;
        }
        final PDFGraphics2D graphics = new PDFGraphics2D(true, pdfInfo.fi,
                pdfInfo.pdfDoc, pdfInfo.pdfContext,
                pdfInfo.pdfPage.referencePDF(), pdfInfo.currentFontName,
                pdfInfo.currentFontSize);
        graphics.setGraphicContext(new org.apache.xmlgraphics.java2d.GraphicContext());

        if (!resolutionScaling.isIdentity()) {
            generator.comment("resolution scaling for " + uaResolution + " -> "
                    + deviceResolution + "\n");
            generator.add(CTMHelper.toPDFString(resolutionScaling, false)
                    + " cm\n");
            graphics.scale(1 / s, 1 / s);
        }

        generator.comment("SVG start");

        // Save state and update coordinate system for the SVG image
        generator.getState().save();
        generator.getState().concatenate(imageTransform);

        // Now that we have the complete transformation matrix for the image, we
        // can update the
        // transformation matrix for the AElementBridge.
        final PDFAElementBridge aBridge = (PDFAElementBridge) ctx.getBridge(
                SVGDOMImplementation.SVG_NAMESPACE_URI, SVGConstants.SVG_A_TAG);
        aBridge.getCurrentTransform().setTransform(
                generator.getState().getTransform());

        graphics.setPaintingState(generator.getState());
        graphics.setOutputStream(pdfInfo.outputStream);
        try {
            root.paint(graphics);
            generator.add(graphics.getString());
        } catch (final Exception e) {
            final SVGEventProducer eventProducer = SVGEventProducer.Provider
                    .get(context.getUserAgent().getEventBroadcaster());
            eventProducer.svgRenderingError(this, e, getDocumentURI(doc));
        }
        generator.getState().restore();
        generator.restoreGraphicsState();
        generator.comment("SVG end");
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsRenderer(final Renderer renderer) {
        return renderer instanceof PDFRenderer;
    }
}
