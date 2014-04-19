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

/* $Id: PSImageHandlerSVG.java 766594 2009-04-20 06:50:59Z jeremias $ */

package org.apache.fop.render.ps;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.fop.image.loader.batik.BatikImageFlavors;
import org.apache.fop.render.ImageHandler;
import org.apache.fop.render.RenderingContext;
import org.apache.fop.svg.SVGEventProducer;
import org.apache.fop.svg.SVGUserAgent;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.impl.ImageXMLDOM;
import org.apache.xmlgraphics.java2d.ps.PSGraphics2D;
import org.apache.xmlgraphics.ps.PSGenerator;

/**
 * Image handler implementation which handles SVG images for PostScript output.
 */
public class PSImageHandlerSVG implements ImageHandler {

    private static final ImageFlavor[] FLAVORS = new ImageFlavor[] { BatikImageFlavors.SVG_DOM };

    /** {@inheritDoc} */
    @Override
    public void handleImage(final RenderingContext context, final Image image,
            final Rectangle pos) throws IOException {
        final PSRenderingContext psContext = (PSRenderingContext) context;
        final PSGenerator gen = psContext.getGenerator();
        final ImageXMLDOM imageSVG = (ImageXMLDOM) image;

        // Controls whether text painted by Batik is generated using text or
        // path operations
        final boolean strokeText = false;
        // TODO Configure text stroking

        final SVGUserAgent ua = new SVGUserAgent(context.getUserAgent(),
                new AffineTransform());

        final PSGraphics2D graphics = new PSGraphics2D(strokeText, gen);
        graphics.setGraphicContext(new org.apache.xmlgraphics.java2d.GraphicContext());

        BridgeContext ctx = new PSBridgeContext(ua, strokeText ? null
                : psContext.getFontInfo(), context.getUserAgent().getFactory()
                .getImageManager(), context.getUserAgent()
                .getImageSessionContext());

        GraphicsNode root;
        try {
            final GVTBuilder builder = new GVTBuilder();
            root = builder.build(ctx, imageSVG.getDocument());
        } catch (final Exception e) {
            final SVGEventProducer eventProducer = SVGEventProducer.Provider
                    .get(context.getUserAgent().getEventBroadcaster());
            eventProducer
                    .svgNotBuilt(this, e, image.getInfo().getOriginalURI());
            return;
        }
        // get the 'width' and 'height' attributes of the SVG document
        final float w = (float) ctx.getDocumentSize().getWidth() * 1000f;
        final float h = (float) ctx.getDocumentSize().getHeight() * 1000f;

        final float sx = pos.width / w;
        final float sy = pos.height / h;

        ctx = null;

        gen.commentln("%FOPBeginSVG");
        gen.saveGraphicsState();
        final boolean clip = false;
        if (clip) {
            /*
             * Clip to the svg area. Note: To have the svg overlay (under) a
             * text area then use an fo:block-container
             */
            gen.writeln("newpath");
            gen.defineRect(pos.getMinX() / 1000f, pos.getMinY() / 1000f,
                    pos.width / 1000f, pos.height / 1000f);
            gen.writeln("clip");
        }

        // transform so that the coordinates (0,0) is from the top left
        // and positive is down and to the right. (0,0) is where the
        // viewBox puts it.
        gen.concatMatrix(sx, 0, 0, sy, pos.getMinX() / 1000f,
                pos.getMinY() / 1000f);

        final AffineTransform transform = new AffineTransform();
        // scale to viewbox
        transform.translate(pos.getMinX(), pos.getMinY());
        gen.getCurrentState().concatMatrix(transform);
        try {
            root.paint(graphics);
        } catch (final Exception e) {
            final SVGEventProducer eventProducer = SVGEventProducer.Provider
                    .get(context.getUserAgent().getEventBroadcaster());
            eventProducer.svgRenderingError(this, e, image.getInfo()
                    .getOriginalURI());
        }

        gen.restoreGraphicsState();
        gen.commentln("%FOPEndSVG");
    }

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 400;
    }

    /** {@inheritDoc} */
    @Override
    public Class getSupportedImageClass() {
        return ImageXMLDOM.class;
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor[] getSupportedImageFlavors() {
        return FLAVORS;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompatible(final RenderingContext targetContext,
            final Image image) {
        if (targetContext instanceof PSRenderingContext) {
            final PSRenderingContext psContext = (PSRenderingContext) targetContext;
            return !psContext.isCreateForms()
                    && (image == null || image instanceof ImageXMLDOM
                            && image.getFlavor().isCompatible(
                                    BatikImageFlavors.SVG_DOM));
        }
        return false;
    }

}
