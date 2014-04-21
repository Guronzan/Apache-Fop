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

/* $Id: AFPImageHandler.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.render.afp;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Map;

import org.apache.fop.afp.AFPDataObjectInfo;
import org.apache.fop.afp.AFPObjectAreaInfo;
import org.apache.fop.afp.AFPPaintingState;
import org.apache.fop.afp.AFPResourceInfo;
import org.apache.fop.afp.AFPUnitConverter;
import org.apache.fop.render.ImageHandlerBase;
import org.apache.xmlgraphics.util.QName;

/**
 * A base abstract AFP image handler
 */
public abstract class AFPImageHandler implements ImageHandlerBase {
    private static final int X = 0;
    private static final int Y = 1;

    /** foreign attribute reader */
    private final AFPForeignAttributeReader foreignAttributeReader = new AFPForeignAttributeReader();

    /**
     * Generates an intermediate AFPDataObjectInfo that is later used to
     * construct the appropriate data object in the AFP DataStream.
     *
     * @param rendererImageInfo
     *            the renderer image info
     * @return a data object info object
     * @throws IOException
     *             thrown if an I/O exception of some sort has occurred.
     */
    public AFPDataObjectInfo generateDataObjectInfo(
            final AFPRendererImageInfo rendererImageInfo) throws IOException {
        final AFPDataObjectInfo dataObjectInfo = createDataObjectInfo();

        // set resource information
        setResourceInformation(dataObjectInfo, rendererImageInfo.getURI(),
                rendererImageInfo.getForeignAttributes());

        final Point origin = rendererImageInfo.getOrigin();
        final Rectangle2D position = rendererImageInfo.getPosition();
        final int srcX = Math.round(origin.x + (float) position.getX());
        final int srcY = Math.round(origin.y + (float) position.getY());
        final Rectangle targetRect = new Rectangle(srcX, srcY,
                (int) Math.round(position.getWidth()),
                (int) Math.round(position.getHeight()));

        final AFPRendererContext rendererContext = (AFPRendererContext) rendererImageInfo
                .getRendererContext();
        final AFPInfo afpInfo = rendererContext.getInfo();
        final AFPPaintingState paintingState = afpInfo.getPaintingState();

        dataObjectInfo.setObjectAreaInfo(createObjectAreaInfo(paintingState,
                targetRect));

        return dataObjectInfo;
    }

    /**
     * Sets resource information on the data object info.
     *
     * @param dataObjectInfo
     *            the data object info instance
     * @param uri
     *            the image's URI (or null if no URI is available)
     * @param foreignAttributes
     *            a Map of foreign attributes (or null)
     */
    protected void setResourceInformation(
            final AFPDataObjectInfo dataObjectInfo, final String uri,
            final Map<QName, String> foreignAttributes) {
        final AFPResourceInfo resourceInfo = this.foreignAttributeReader
                .getResourceInfo(foreignAttributes);
        resourceInfo.setUri(uri);
        dataObjectInfo.setResourceInfo(resourceInfo);
    }

    /**
     * Creates and returns an {@link AFPObjectAreaInfo} instance for the
     * placement of the image.
     *
     * @param paintingState
     *            the painting state
     * @param targetRect
     *            the target rectangle in which to place the image (coordinates
     *            in mpt)
     * @return the newly created object area info instance
     */
    public static AFPObjectAreaInfo createObjectAreaInfo(
            final AFPPaintingState paintingState, final Rectangle targetRect) {
        final AFPObjectAreaInfo objectAreaInfo = new AFPObjectAreaInfo();
        final AFPUnitConverter unitConv = paintingState.getUnitConverter();

        final int[] coords = unitConv.mpts2units(new float[] { targetRect.x,
                targetRect.y });
        objectAreaInfo.setX(coords[X]);
        objectAreaInfo.setY(coords[Y]);

        final int width = Math.round(unitConv.mpt2units(targetRect.width));
        objectAreaInfo.setWidth(width);

        final int height = Math.round(unitConv.mpt2units(targetRect.height));
        objectAreaInfo.setHeight(height);

        final int resolution = paintingState.getResolution();
        objectAreaInfo.setHeightRes(resolution);
        objectAreaInfo.setWidthRes(resolution);

        objectAreaInfo.setRotation(paintingState.getRotation());
        return objectAreaInfo;
    }

    /**
     * Creates the data object information object
     *
     * @return the data object information object
     */
    protected abstract AFPDataObjectInfo createDataObjectInfo();
}
