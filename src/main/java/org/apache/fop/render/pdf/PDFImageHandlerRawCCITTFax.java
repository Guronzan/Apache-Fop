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

/* $Id: PDFImageHandlerRawCCITTFax.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.render.pdf;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;

import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFImage;
import org.apache.fop.pdf.PDFResourceContext;
import org.apache.fop.pdf.PDFXObject;
import org.apache.fop.render.ImageHandler;
import org.apache.fop.render.RendererContext;
import org.apache.fop.render.RenderingContext;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.impl.ImageRawCCITTFax;

/**
 * Image handler implementation which handles CCITT encoded images (CCITT fax
 * group 3/4) for PDF output.
 */
public class PDFImageHandlerRawCCITTFax implements PDFImageHandler,
        ImageHandler {

    private static final ImageFlavor[] FLAVORS = new ImageFlavor[] { ImageFlavor.RAW_CCITTFAX, };

    /** {@inheritDoc} */
    @Override
    public PDFXObject generateImage(final RendererContext context,
            final Image image, final Point origin, final Rectangle pos)
                    throws IOException {
        final PDFRenderer renderer = (PDFRenderer) context.getRenderer();
        final ImageRawCCITTFax ccitt = (ImageRawCCITTFax) image;
        final PDFDocument pdfDoc = (PDFDocument) context
                .getProperty(PDFRendererContextConstants.PDF_DOCUMENT);
        final PDFResourceContext resContext = (PDFResourceContext) context
                .getProperty(PDFRendererContextConstants.PDF_CONTEXT);

        final PDFImage pdfimage = new ImageRawCCITTFaxAdapter(ccitt, image
                .getInfo().getOriginalURI());
        final PDFXObject xobj = pdfDoc.addImage(resContext, pdfimage);

        final float x = (float) pos.getX() / 1000f;
        final float y = (float) pos.getY() / 1000f;
        final float w = (float) pos.getWidth() / 1000f;
        final float h = (float) pos.getHeight() / 1000f;
        renderer.placeImage(x, y, w, h, xobj);

        return xobj;
    }

    /** {@inheritDoc} */
    @Override
    public void handleImage(final RenderingContext context, final Image image,
            final Rectangle pos) throws IOException {
        final PDFRenderingContext pdfContext = (PDFRenderingContext) context;
        final PDFContentGenerator generator = pdfContext.getGenerator();
        final ImageRawCCITTFax ccitt = (ImageRawCCITTFax) image;

        final PDFImage pdfimage = new ImageRawCCITTFaxAdapter(ccitt, image
                .getInfo().getOriginalURI());
        final PDFXObject xobj = generator.getDocument().addImage(
                generator.getResourceContext(), pdfimage);

        final float x = (float) pos.getX() / 1000f;
        final float y = (float) pos.getY() / 1000f;
        final float w = (float) pos.getWidth() / 1000f;
        final float h = (float) pos.getHeight() / 1000f;
        generator.placeImage(x, y, w, h, xobj);
    }

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 100;
    }

    /** {@inheritDoc} */
    @Override
    public Class getSupportedImageClass() {
        return ImageRawCCITTFax.class;
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
        return (image == null || image instanceof ImageRawCCITTFax)
                && targetContext instanceof PDFRenderingContext;
    }

}
