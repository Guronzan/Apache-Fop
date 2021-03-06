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

/* $Id: ImageLoaderSVG.java 706276 2008-10-20 13:20:26Z jeremias $ */

package org.apache.fop.image.loader.batik;

import java.io.IOException;
import java.util.Map;

import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.image.loader.XMLNamespaceEnabledImageFlavor;
import org.apache.xmlgraphics.image.loader.impl.AbstractImageLoader;
import org.apache.xmlgraphics.image.loader.impl.ImageXMLDOM;
import org.apache.xmlgraphics.util.MimeConstants;

/**
 * ImageLoader for SVG (using Apache Batik).
 */
public class ImageLoaderSVG extends AbstractImageLoader {

    private final ImageFlavor targetFlavor;

    /**
     * Main constructor.
     * 
     * @param targetFlavor
     *            the target flavor
     */
    public ImageLoaderSVG(final ImageFlavor targetFlavor) {
        if (!XMLNamespaceEnabledImageFlavor.SVG_DOM.isCompatible(targetFlavor)) {
            throw new IllegalArgumentException(
                    "Incompatible target ImageFlavor: " + targetFlavor);
        }
        this.targetFlavor = targetFlavor;
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor getTargetFlavor() {
        return this.targetFlavor;
    }

    /** {@inheritDoc} */
    @Override
    public Image loadImage(final ImageInfo info, final Map hints,
            final ImageSessionContext session) throws ImageException,
            IOException {
        if (!MimeConstants.MIME_SVG.equals(info.getMimeType())) {
            throw new IllegalArgumentException(
                    "ImageInfo must be from an SVG image");
        }
        final Image img = info.getOriginalImage();
        if (!(img instanceof ImageXMLDOM)) {
            throw new IllegalArgumentException(
                    "ImageInfo was expected to contain the SVG document as DOM");
        }
        final ImageXMLDOM svgImage = (ImageXMLDOM) img;
        if (!SVGDOMImplementation.SVG_NAMESPACE_URI.equals(svgImage
                .getRootNamespace())) {
            throw new IllegalArgumentException(
                    "The Image is not in the SVG namespace: "
                            + svgImage.getRootNamespace());
        }
        return svgImage;
    }

}
