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

/* $Id: PreloaderSVG.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.image.loader.batik;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.UnitProcessor;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.dom.svg.SVGOMDocument;
import org.apache.fop.svg.SimpleSVGUserAgent;
import org.apache.fop.util.UnclosableInputStream;
import org.apache.xmlgraphics.image.loader.ImageContext;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageSize;
import org.apache.xmlgraphics.image.loader.impl.AbstractImagePreloader;
import org.apache.xmlgraphics.image.loader.impl.ImageXMLDOM;
import org.apache.xmlgraphics.image.loader.util.ImageUtil;
import org.apache.xmlgraphics.util.MimeConstants;
import org.apache.xmlgraphics.util.UnitConv;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGDocument;

/**
 * Image preloader for SVG images.
 */
@Slf4j
public class PreloaderSVG extends AbstractImagePreloader {

    private boolean batikAvailable = true;

    /** {@inheritDoc} */
    @Override
    public ImageInfo preloadImage(final String uri, final Source src,
            final ImageContext context) throws IOException {
        ImageInfo info = null;
        if (this.batikAvailable) {
            try {
                final Loader loader = new Loader();
                if (!loader.isSupportedSource(src)) {
                    return null;
                }
                info = loader.getImage(uri, src, context);
            } catch (final NoClassDefFoundError e) {
                this.batikAvailable = false;
                log.warn("Batik not in class path", e);
                return null;
            }
        }
        if (info != null) {
            ImageUtil.closeQuietly(src); // Image is fully read
        }
        return info;
    }

    /**
     * Returns the fully qualified classname of an XML parser for Batik classes
     * that apparently need it (error messages, perhaps)
     *
     * @return an XML parser classname
     */
    public static String getParserName() {
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            return factory.newSAXParser().getXMLReader().getClass().getName();
        } catch (final Exception e) {
            return null;
        }
    }

    /**
     * This method is put in another class so that the class loader does not
     * attempt to load Batik related classes when constructing the SVGPreloader
     * class.
     */
    private final class Loader {

        private Loader() {
        }

        private ImageInfo getImage(final String uri, final Source src,
                final ImageContext context) {
            // parse document and get the size attributes of the svg element

            InputStream in = null;
            try {
                SVGDocument doc;
                if (src instanceof DOMSource) {
                    final DOMSource domSrc = (DOMSource) src;
                    doc = (SVGDocument) domSrc.getNode();
                } else {
                    in = new UnclosableInputStream(
                            ImageUtil.needInputStream(src));
                    final int length = in.available();
                    in.mark(length + 1);
                    final SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(
                            getParserName());
                    doc = factory.createSVGDocument(src.getSystemId(), in);
                }
                final ImageInfo info = createImageInfo(uri, context, doc);

                return info;
            } catch (final NoClassDefFoundError ncdfe) {
                if (in != null) {
                    try {
                        in.reset();
                    } catch (final IOException ioe) {
                        // we're more interested in the original exception
                    }
                }
                PreloaderSVG.this.batikAvailable = false;
                log.warn("Batik not in class path", ncdfe);
                return null;
            } catch (final IOException e) {
                // If the svg is invalid then it throws an IOException
                // so there is no way of knowing if it is an svg document

                log.debug("Error while trying to load stream as an SVG file: "
                        + e.getMessage());
                // assuming any exception means this document is not svg
                // or could not be loaded for some reason
                try {
                    in.reset();
                } catch (final IOException ioe) {
                    // we're more interested in the original exception
                }
                return null;
            }
        }

        private ImageInfo createImageInfo(final String uri,
                final ImageContext context, final SVGDocument doc) {
            final Element e = doc.getRootElement();
            final float pxUnitToMillimeter = UnitConv.IN2MM
                    / context.getSourceResolution();
            final UserAgent userAg = new SimpleSVGUserAgent(pxUnitToMillimeter,
                    new AffineTransform()) {

                /** {@inheritDoc} */
                @Override
                public void displayMessage(final String message) {
                    log.debug(message);
                }

            };
            final BridgeContext ctx = new BridgeContext(userAg);
            final UnitProcessor.Context uctx = UnitProcessor.createContext(ctx,
                    e);

            String s;
            // 'width' attribute - default is 100%
            s = e.getAttributeNS(null, SVGOMDocument.SVG_WIDTH_ATTRIBUTE);
            if (s.length() == 0) {
                s = SVGOMDocument.SVG_SVG_WIDTH_DEFAULT_VALUE;
            }
            final float width = UnitProcessor.svgHorizontalLengthToUserSpace(s,
                    SVGOMDocument.SVG_WIDTH_ATTRIBUTE, uctx);

            // 'height' attribute - default is 100%
            s = e.getAttributeNS(null, SVGOMDocument.SVG_HEIGHT_ATTRIBUTE);
            if (s.length() == 0) {
                s = SVGOMDocument.SVG_SVG_HEIGHT_DEFAULT_VALUE;
            }
            final float height = UnitProcessor.svgVerticalLengthToUserSpace(s,
                    SVGOMDocument.SVG_HEIGHT_ATTRIBUTE, uctx);

            final int widthMpt = (int) Math.round(px2mpt(width,
                    context.getSourceResolution()));
            final int heightMpt = (int) Math.round(px2mpt(height,
                    context.getSourceResolution()));

            final ImageInfo info = new ImageInfo(uri, MimeConstants.MIME_SVG);
            final ImageSize size = new ImageSize();
            size.setSizeInMillipoints(widthMpt, heightMpt);
            // Set the resolution to that of the FOUserAgent
            size.setResolution(context.getSourceResolution());
            size.calcPixelsFromSize();
            info.setSize(size);

            // The whole image had to be loaded for this, so keep it
            final ImageXMLDOM xmlImage = new ImageXMLDOM(info, doc,
                    BatikImageFlavors.SVG_DOM);
            info.getCustomObjects().put(ImageInfo.ORIGINAL_IMAGE, xmlImage);
            return info;
        }

        private boolean isSupportedSource(final Source src) throws IOException {
            if (src instanceof DOMSource) {
                final DOMSource domSrc = (DOMSource) src;
                return domSrc.getNode() instanceof SVGDocument;
            } else {
                return ImageUtil.hasInputStream(src);
            }
        }

    }

    private static double px2mpt(final double px, final double resolution) {
        return px * 1000 * UnitConv.IN2PT / resolution;
    }

}
