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

/* $Id: AFPImageHandlerXML.java 721430 2008-11-28 11:13:12Z acumiskey $ */

package org.apache.fop.render.afp;

import java.awt.geom.Rectangle2D;
import java.util.Map;

import org.apache.fop.afp.AFPDataObjectInfo;
import org.apache.fop.render.RendererContext;
import org.apache.fop.render.RendererContextConstants;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.impl.ImageXMLDOM;
import org.apache.xmlgraphics.util.QName;
import org.w3c.dom.Document;

/**
 * PDFImageHandler implementation which handles XML-based images.
 */
public class AFPImageHandlerXML extends AFPImageHandler {

    private static final ImageFlavor[] FLAVORS = new ImageFlavor[] { ImageFlavor.XML_DOM, };

    /** {@inheritDoc} */
    @Override
    public AFPDataObjectInfo generateDataObjectInfo(
            final AFPRendererImageInfo rendererImageInfo) {
        final RendererContext rendererContext = rendererImageInfo
                .getRendererContext();
        final AFPRenderer renderer = (AFPRenderer) rendererContext
                .getRenderer();
        final ImageXMLDOM imgXML = (ImageXMLDOM) rendererImageInfo.getImage();
        final Document doc = imgXML.getDocument();
        final String ns = imgXML.getRootNamespace();
        final Map<QName, String> foreignAttributes = (Map<QName, String>) rendererContext
                .getProperty(RendererContextConstants.FOREIGN_ATTRIBUTES);
        final Rectangle2D pos = rendererImageInfo.getPosition();
        renderer.renderDocument(doc, ns, pos, foreignAttributes);
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 400;
    }

    /** {@inheritDoc} */
    @Override
    public Class<ImageXMLDOM> getSupportedImageClass() {
        return ImageXMLDOM.class;
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor[] getSupportedImageFlavors() {
        return FLAVORS;
    }

    /** {@inheritDoc} */
    @Override
    protected AFPDataObjectInfo createDataObjectInfo() {
        return null;
    }

}
