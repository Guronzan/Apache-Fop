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

/* $Id: AFPImageHandlerGraphics2D.java 953952 2010-06-12 08:19:48Z jeremias $ */

package org.apache.fop.render.afp;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;

import org.apache.fop.afp.AFPDataObjectInfo;
import org.apache.fop.afp.AFPGraphics2D;
import org.apache.fop.afp.AFPGraphicsObjectInfo;
import org.apache.fop.afp.AFPPaintingState;
import org.apache.fop.afp.AFPResourceInfo;
import org.apache.fop.afp.AFPResourceManager;
import org.apache.fop.afp.modca.ResourceObject;
import org.apache.fop.render.ImageHandler;
import org.apache.fop.render.ImageHandlerUtil;
import org.apache.fop.render.RenderingContext;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.impl.ImageGraphics2D;
import org.apache.xmlgraphics.java2d.Graphics2DImagePainter;
import org.apache.xmlgraphics.util.MimeConstants;

/**
 * PDFImageHandler implementation which handles Graphics2D images.
 */
public class AFPImageHandlerGraphics2D extends AFPImageHandler implements
        ImageHandler {

    private static final ImageFlavor[] FLAVORS = new ImageFlavor[] { ImageFlavor.GRAPHICS2D };

    /** {@inheritDoc} */
    @Override
    public AFPDataObjectInfo generateDataObjectInfo(
            final AFPRendererImageInfo rendererImageInfo) throws IOException {

        final AFPRendererContext rendererContext = (AFPRendererContext) rendererImageInfo
                .getRendererContext();
        final AFPInfo afpInfo = rendererContext.getInfo();
        final ImageGraphics2D imageG2D = (ImageGraphics2D) rendererImageInfo
                .getImage();
        final Graphics2DImagePainter painter = imageG2D
                .getGraphics2DImagePainter();

        if (afpInfo.paintAsBitmap()) {
            final int x = afpInfo.getX();
            final int y = afpInfo.getY();
            final int width = afpInfo.getWidth();
            final int height = afpInfo.getHeight();
            final AFPPaintingState paintingState = afpInfo.getPaintingState();
            final AFPGraphics2DAdapter g2dAdapter = new AFPGraphics2DAdapter(
                    paintingState);
            g2dAdapter
                    .paintImage(painter, rendererContext, x, y, width, height);
            return null;
        } else {
            final AFPGraphicsObjectInfo graphicsObjectInfo = (AFPGraphicsObjectInfo) super
                    .generateDataObjectInfo(rendererImageInfo);

            setDefaultResourceLevel(graphicsObjectInfo,
                    afpInfo.getResourceManager());

            // set mime type (unsupported by MOD:CA registry)
            graphicsObjectInfo.setMimeType(MimeConstants.MIME_AFP_GOCA);

            // set g2d
            final boolean textAsShapes = false;
            final AFPGraphics2D g2d = afpInfo.createGraphics2D(textAsShapes);

            graphicsObjectInfo.setGraphics2D(g2d);

            // set painter
            graphicsObjectInfo.setPainter(painter);

            return graphicsObjectInfo;
        }
    }

    private void setDefaultResourceLevel(
            final AFPGraphicsObjectInfo graphicsObjectInfo,
            final AFPResourceManager resourceManager) {
        final AFPResourceInfo resourceInfo = graphicsObjectInfo
                .getResourceInfo();
        if (!resourceInfo.levelChanged()) {
            resourceInfo.setLevel(resourceManager.getResourceLevelDefaults()
                    .getDefaultResourceLevel(ResourceObject.TYPE_GRAPHIC));
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 200;
    }

    /** {@inheritDoc} */
    @Override
    public Class getSupportedImageClass() {
        return ImageGraphics2D.class;
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor[] getSupportedImageFlavors() {
        return FLAVORS;
    }

    /** {@inheritDoc} */
    @Override
    protected AFPDataObjectInfo createDataObjectInfo() {
        return new AFPGraphicsObjectInfo();
    }

    /** {@inheritDoc} */
    @Override
    public void handleImage(final RenderingContext context, final Image image,
            final Rectangle pos) throws IOException {
        final AFPRenderingContext afpContext = (AFPRenderingContext) context;

        final AFPGraphicsObjectInfo graphicsObjectInfo = (AFPGraphicsObjectInfo) createDataObjectInfo();

        // set resource information
        setResourceInformation(graphicsObjectInfo, image.getInfo()
                .getOriginalURI(), afpContext.getForeignAttributes());

        // Positioning
        graphicsObjectInfo.setObjectAreaInfo(createObjectAreaInfo(
                afpContext.getPaintingState(), pos));

        setDefaultResourceLevel(graphicsObjectInfo,
                afpContext.getResourceManager());

        final AFPPaintingState paintingState = afpContext.getPaintingState();
        paintingState.save(); // save
        final AffineTransform placement = new AffineTransform();
        placement.translate(pos.x, pos.y);
        paintingState.concatenate(placement);

        // Image content
        final ImageGraphics2D imageG2D = (ImageGraphics2D) image;
        final boolean textAsShapes = false; // TODO Make configurable
        final AFPGraphics2D g2d = new AFPGraphics2D(textAsShapes,
                afpContext.getPaintingState(), afpContext.getResourceManager(),
                graphicsObjectInfo.getResourceInfo(), afpContext.getFontInfo());
        g2d.setGraphicContext(new org.apache.xmlgraphics.java2d.GraphicContext());

        graphicsObjectInfo.setGraphics2D(g2d);
        graphicsObjectInfo.setPainter(imageG2D.getGraphics2DImagePainter());

        // Create image
        afpContext.getResourceManager().createObject(graphicsObjectInfo);

        paintingState.restore(); // resume
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompatible(final RenderingContext targetContext,
            final Image image) {
        final boolean supported = (image == null || image instanceof ImageGraphics2D)
                && targetContext instanceof AFPRenderingContext;
        if (supported) {
            final String mode = (String) targetContext
                    .getHint(ImageHandlerUtil.CONVERSION_MODE);
            if (ImageHandlerUtil.isConversionModeBitmap(mode)) {
                // Disabling this image handler automatically causes a bitmap to
                // be generated
                return false;
            }
        }
        return supported;
    }
}
