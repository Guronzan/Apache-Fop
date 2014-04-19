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

/* $Id: AFPImageHandlerRenderedImage.java 953952 2010-06-12 08:19:48Z jeremias $ */

package org.apache.fop.render.afp;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.fop.afp.AFPDataObjectInfo;
import org.apache.fop.afp.AFPImageObjectInfo;
import org.apache.fop.afp.AFPObjectAreaInfo;
import org.apache.fop.afp.AFPPaintingState;
import org.apache.fop.afp.AFPResourceInfo;
import org.apache.fop.afp.AFPResourceManager;
import org.apache.fop.afp.modca.ResourceObject;
import org.apache.fop.render.ImageHandler;
import org.apache.fop.render.RenderingContext;
import org.apache.fop.util.bitmap.BitmapImageUtil;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageSize;
import org.apache.xmlgraphics.image.loader.impl.ImageRendered;
import org.apache.xmlgraphics.ps.ImageEncodingHelper;
import org.apache.xmlgraphics.util.MimeConstants;
import org.apache.xmlgraphics.util.UnitConv;

/**
 * PDFImageHandler implementation which handles RenderedImage instances.
 */
@Slf4j
public class AFPImageHandlerRenderedImage extends AFPImageHandler implements
ImageHandler {

    private static final ImageFlavor[] FLAVORS = new ImageFlavor[] {
        ImageFlavor.BUFFERED_IMAGE, ImageFlavor.RENDERED_IMAGE };

    /** {@inheritDoc} */
    @Override
    public AFPDataObjectInfo generateDataObjectInfo(
            final AFPRendererImageInfo rendererImageInfo) throws IOException {
        final AFPImageObjectInfo imageObjectInfo = (AFPImageObjectInfo) super
                .generateDataObjectInfo(rendererImageInfo);

        final AFPRendererContext rendererContext = (AFPRendererContext) rendererImageInfo
                .getRendererContext();
        final AFPInfo afpInfo = rendererContext.getInfo();

        setDefaultResourceLevel(imageObjectInfo, afpInfo.getResourceManager());

        final AFPPaintingState paintingState = afpInfo.getPaintingState();
        final ImageRendered imageRendered = (ImageRendered) rendererImageInfo.img;
        final Dimension targetSize = new Dimension(afpInfo.getWidth(),
                afpInfo.getHeight());

        updateDataObjectInfo(imageObjectInfo, paintingState, imageRendered,
                targetSize);
        return imageObjectInfo;
    }

    private AFPDataObjectInfo updateDataObjectInfo(
            final AFPImageObjectInfo imageObjectInfo,
            final AFPPaintingState paintingState,
            final ImageRendered imageRendered, final Dimension targetSize)
            throws IOException {

        final long start = System.currentTimeMillis();

        final int resolution = paintingState.getResolution();
        int maxPixelSize = paintingState.getBitsPerPixel();
        if (paintingState.isColorImages()) {
            if (paintingState.isCMYKImagesSupported()) {
                maxPixelSize *= 4; // CMYK is maximum
            } else {
                maxPixelSize *= 3; // RGB is maximum
            }
        }
        final float ditheringQuality = paintingState.getDitheringQuality();
        RenderedImage renderedImage = imageRendered.getRenderedImage();

        final ImageInfo imageInfo = imageRendered.getInfo();
        final ImageSize intrinsicSize = imageInfo.getSize();

        final boolean useFS10 = maxPixelSize == 1
                || BitmapImageUtil.isMonochromeImage(renderedImage);
        int functionSet = useFS10 ? 10 : 11;
        final boolean usePageSegments = useFS10
                && !imageObjectInfo.getResourceInfo().getLevel().isInline();

        ImageSize effIntrinsicSize = intrinsicSize;
        if (usePageSegments) {
            // Resize, optionally resample and convert image
            final Dimension resampledDim = new Dimension(
                    (int) Math.ceil(UnitConv.mpt2px(targetSize.getWidth(),
                            resolution)), (int) Math.ceil(UnitConv.mpt2px(
                                    targetSize.getHeight(), resolution)));

            imageObjectInfo.setCreatePageSegment(true);
            imageObjectInfo.getResourceInfo().setImageDimension(resampledDim);

            // Only resample/downsample if image is smaller than its intrinsic
            // size
            // to make print file smaller
            final boolean resample = resampledDim.width < renderedImage
                    .getWidth()
                    && resampledDim.height < renderedImage.getHeight();
            if (resample) {
                if (log.isDebugEnabled()) {
                    log.debug("Resample from " + intrinsicSize.getDimensionPx()
                            + " to " + resampledDim);
                }
                renderedImage = BitmapImageUtil.convertToMonochrome(
                        renderedImage, resampledDim, ditheringQuality);
                effIntrinsicSize = new ImageSize(resampledDim.width,
                        resampledDim.height, resolution);
            } else if (ditheringQuality >= 0.5f) {
                renderedImage = BitmapImageUtil.convertToMonochrome(
                        renderedImage, intrinsicSize.getDimensionPx(),
                        ditheringQuality);
            }
        }

        imageObjectInfo.setDataHeightRes((int) Math.round(effIntrinsicSize
                .getDpiHorizontal() * 10));
        imageObjectInfo.setDataWidthRes((int) Math.round(effIntrinsicSize
                .getDpiVertical() * 10));

        final int dataHeight = renderedImage.getHeight();
        imageObjectInfo.setDataHeight(dataHeight);

        final int dataWidth = renderedImage.getWidth();
        imageObjectInfo.setDataWidth(dataWidth);

        // TODO To reduce AFP file size, investigate using a compression scheme.
        // Currently, all image data is uncompressed.
        final ColorModel cm = renderedImage.getColorModel();
        if (log.isTraceEnabled()) {
            log.trace("ColorModel: " + cm);
        }
        int pixelSize = cm.getPixelSize();
        if (cm.hasAlpha()) {
            pixelSize -= 8;
        }

        byte[] imageData = null;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final boolean allowDirectEncoding = true;
        if (allowDirectEncoding && pixelSize <= maxPixelSize) {
            // Attempt to encode without resampling the image
            final ImageEncodingHelper helper = new ImageEncodingHelper(
                    renderedImage, pixelSize == 32);
            final ColorModel encodedColorModel = helper.getEncodedColorModel();
            boolean directEncode = true;
            if (helper.getEncodedColorModel().getPixelSize() > maxPixelSize) {
                directEncode = false; // pixel size needs to be reduced
            }
            if (BitmapImageUtil.getColorIndexSize(renderedImage) > 2) {
                directEncode = false; // Lookup tables are not implemented, yet
            }
            if (useFS10 && BitmapImageUtil.isMonochromeImage(renderedImage)
                    && BitmapImageUtil.isZeroBlack(renderedImage)) {
                directEncode = false;
                // need a special method to invert the bit-stream since setting
                // the subtractive mode
                // in AFP alone doesn't seem to do the trick.
                if (encodeInvertedBilevel(helper, imageObjectInfo, baos)) {
                    imageData = baos.toByteArray();
                }
            }
            if (directEncode) {
                log.debug("Encoding image directly...");
                imageObjectInfo.setBitsPerPixel(encodedColorModel
                        .getPixelSize());
                if (pixelSize == 32) {
                    functionSet = 45; // IOCA FS45 required for CMYK
                }

                helper.encode(baos);
                imageData = baos.toByteArray();
            }
        }
        if (imageData == null) {
            log.debug("Encoding image via RGB...");

            // Convert image to 24bit RGB
            ImageEncodingHelper.encodeRenderedImageAsRGB(renderedImage, baos);
            imageData = baos.toByteArray();
            imageObjectInfo.setBitsPerPixel(24);

            final boolean colorImages = paintingState.isColorImages();
            imageObjectInfo.setColor(colorImages);

            // convert to grayscale
            if (!colorImages) {
                log.debug("Converting RGB image to grayscale...");
                baos.reset();
                final int bitsPerPixel = paintingState.getBitsPerPixel();
                imageObjectInfo.setBitsPerPixel(bitsPerPixel);
                // TODO this should be done off the RenderedImage to avoid
                // buffering the
                // intermediate 24bit image
                ImageEncodingHelper.encodeRGBAsGrayScale(imageData, dataWidth,
                        dataHeight, bitsPerPixel, baos);
                imageData = baos.toByteArray();
                if (bitsPerPixel == 1) {
                    imageObjectInfo.setSubtractive(true);
                }
            }
        }

        switch (functionSet) {
        case 10:
            imageObjectInfo.setMimeType(MimeConstants.MIME_AFP_IOCA_FS10);
            break;
        case 11:
            imageObjectInfo.setMimeType(MimeConstants.MIME_AFP_IOCA_FS11);
            break;
        case 45:
            imageObjectInfo.setMimeType(MimeConstants.MIME_AFP_IOCA_FS45);
            break;
        default:
            throw new IllegalStateException("Invalid IOCA function set: "
                    + functionSet);
        }

        imageObjectInfo.setData(imageData);

        // set object area info
        final AFPObjectAreaInfo objectAreaInfo = imageObjectInfo
                .getObjectAreaInfo();
        objectAreaInfo.setWidthRes(resolution);
        objectAreaInfo.setHeightRes(resolution);

        if (log.isDebugEnabled()) {
            final long duration = System.currentTimeMillis() - start;
            log.debug("Image encoding took " + duration + "ms.");
        }

        return imageObjectInfo;
    }

    /**
     * Efficiently encodes a bi-level image in inverted form as a plain
     * bit-stream.
     *
     * @param helper
     *            the image encoding helper used to analyze the image
     * @param imageObjectInfo
     *            the AFP image object
     * @param out
     *            the output stream
     * @return true if the image was encoded, false if there was something
     *         prohibiting that
     * @throws IOException
     *             if an I/O error occurs
     */
    private boolean encodeInvertedBilevel(final ImageEncodingHelper helper,
            final AFPImageObjectInfo imageObjectInfo, final OutputStream out)
                    throws IOException {
        final RenderedImage renderedImage = helper.getImage();
        if (!BitmapImageUtil.isMonochromeImage(renderedImage)) {
            throw new IllegalStateException(
                    "This method only supports binary images!");
        }
        final int tiles = renderedImage.getNumXTiles()
                * renderedImage.getNumYTiles();
        if (tiles > 1) {
            return false;
        }

        imageObjectInfo.setBitsPerPixel(1);

        final Raster raster = renderedImage.getTile(0, 0);
        final DataBuffer buffer = raster.getDataBuffer();
        if (buffer instanceof DataBufferByte) {
            final DataBufferByte byteBuffer = (DataBufferByte) buffer;
            log.debug("Encoding image as inverted bi-level...");
            final byte[] rawData = byteBuffer.getData();
            int remaining = rawData.length;
            int pos = 0;
            final byte[] data = new byte[4096];
            while (remaining > 0) {
                final int size = Math.min(remaining, data.length);
                for (int i = 0; i < size; i++) {
                    data[i] = (byte) ~rawData[pos]; // invert bits
                    pos++;
                }
                out.write(data, 0, size);
                remaining -= size;
            }
            return true;
        }
        return false;
    }

    private void setDefaultResourceLevel(
            final AFPImageObjectInfo imageObjectInfo,
            final AFPResourceManager resourceManager) {
        final AFPResourceInfo resourceInfo = imageObjectInfo.getResourceInfo();
        if (!resourceInfo.levelChanged()) {
            resourceInfo.setLevel(resourceManager.getResourceLevelDefaults()
                    .getDefaultResourceLevel(ResourceObject.TYPE_IMAGE));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected AFPDataObjectInfo createDataObjectInfo() {
        return new AFPImageObjectInfo();
    }

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 300;
    }

    /** {@inheritDoc} */
    @Override
    public Class getSupportedImageClass() {
        return ImageRendered.class;
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor[] getSupportedImageFlavors() {
        return FLAVORS;
    }

    /** {@inheritDoc} */
    @Override
    public void handleImage(final RenderingContext context, final Image image,
            final Rectangle pos) throws IOException {
        final AFPRenderingContext afpContext = (AFPRenderingContext) context;

        final AFPImageObjectInfo imageObjectInfo = (AFPImageObjectInfo) createDataObjectInfo();

        // set resource information
        setResourceInformation(imageObjectInfo, image.getInfo()
                .getOriginalURI(), afpContext.getForeignAttributes());
        setDefaultResourceLevel(imageObjectInfo,
                afpContext.getResourceManager());

        // Positioning
        imageObjectInfo.setObjectAreaInfo(createObjectAreaInfo(
                afpContext.getPaintingState(), pos));
        final Dimension targetSize = pos.getSize();

        // Image content
        final ImageRendered imageRend = (ImageRendered) image;
        updateDataObjectInfo(imageObjectInfo, afpContext.getPaintingState(),
                imageRend, targetSize);

        // Create image
        afpContext.getResourceManager().createObject(imageObjectInfo);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompatible(final RenderingContext targetContext,
            final Image image) {
        return (image == null || image instanceof ImageRendered)
                && targetContext instanceof AFPRenderingContext;
    }

}
