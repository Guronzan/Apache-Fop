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

/* $Id: AFPRendererContext.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.render.afp;

import java.util.Map;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.fop.afp.AFPPaintingState;
import org.apache.fop.afp.AFPResourceInfo;
import org.apache.fop.afp.AFPResourceManager;
import org.apache.fop.afp.modca.ResourceObject;
import org.apache.fop.render.AbstractRenderer;
import org.apache.fop.render.ImageHandlerUtil;
import org.apache.fop.render.RendererContext;
import org.apache.fop.render.RendererContextConstants;
import org.apache.xmlgraphics.util.QName;

/**
 * AFP-specific renderer context class.
 */
public class AFPRendererContext extends RendererContext {

    /**
     * Main constructor
     *
     * @param renderer
     *            the current renderer
     * @param mime
     *            the MIME type of the output that's generated.
     */
    public AFPRendererContext(final AbstractRenderer renderer, final String mime) {
        super(renderer, mime);
    }

    /**
     * Returns a new AFPInfo for this renderer context
     *
     * @return an AFPInfo for this renderer context
     */
    public AFPInfo getInfo() {
        final AFPInfo info = new AFPInfo();
        info.setWidth(((Integer) getProperty(RendererContextConstants.WIDTH))
                .intValue());
        info.setHeight(((Integer) getProperty(RendererContextConstants.HEIGHT))
                .intValue());
        info.setX(((Integer) getProperty(RendererContextConstants.XPOS))
                .intValue());
        info.setY(((Integer) getProperty(RendererContextConstants.YPOS))
                .intValue());
        info.setHandlerConfiguration((Configuration) getProperty(RendererContextConstants.HANDLER_CONFIGURATION));
        info.setFontInfo((org.apache.fop.fonts.FontInfo) getProperty(AFPRendererContextConstants.AFP_FONT_INFO));
        info.setPaintingState((AFPPaintingState) getProperty(AFPRendererContextConstants.AFP_PAINTING_STATE));
        info.setResourceManager((AFPResourceManager) getProperty(AFPRendererContextConstants.AFP_RESOURCE_MANAGER));

        final Map<QName, String> foreignAttributes = (Map<QName, String>) getProperty(RendererContextConstants.FOREIGN_ATTRIBUTES);
        if (foreignAttributes != null) {
            final boolean paintAsBitmap = ImageHandlerUtil
                    .isConversionModeBitmap(foreignAttributes);
            info.setPaintAsBitmap(paintAsBitmap);

            final AFPForeignAttributeReader foreignAttributeReader = new AFPForeignAttributeReader();
            final AFPResourceInfo resourceInfo = foreignAttributeReader
                    .getResourceInfo(foreignAttributes);
            // set default resource level if an explicit one hasn't been set
            if (!resourceInfo.levelChanged()) {
                final byte resourceType = paintAsBitmap ? ResourceObject.TYPE_IMAGE
                        : ResourceObject.TYPE_GRAPHIC;
                resourceInfo.setLevel(info.getResourceManager()
                        .getResourceLevelDefaults()
                        .getDefaultResourceLevel(resourceType));
            }
            info.setResourceInfo(resourceInfo);
        }
        return info;
    }
}
