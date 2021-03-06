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

/* $Id: ExternalGraphic.java 932497 2010-04-09 16:34:29Z vhennebert $ */

package org.apache.fop.fo.flow;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.fop.ResourceEventProducer;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.datatypes.Length;
import org.apache.fop.datatypes.URISpecification;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.FixedLength;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageManager;
import org.xml.sax.Locator;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_external-graphic">
 * <code>fo:external-graphic</code></a> object. This FO node handles the
 * external graphic. It creates an image inline area that can be added to the
 * area tree.
 */
public class ExternalGraphic extends AbstractGraphics {

    // The value of properties relevant for fo:external-graphic.
    // All but one of the e-g properties are kept in AbstractGraphics
    private String src;
    // End of property values

    // Additional values
    private String url;
    private int intrinsicWidth;
    private int intrinsicHeight;
    private Length intrinsicAlignmentAdjust;

    /**
     * Create a new ExternalGraphic node that is a child of the given
     * {@link FONode}.
     *
     * @param parent
     *            the parent of this node
     */
    public ExternalGraphic(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        super.bind(pList);
        this.src = pList.get(PR_SRC).getString();

        // Additional processing: obtain the image's intrinsic size and baseline
        // information
        this.url = URISpecification.getURL(this.src);
        final FOUserAgent userAgent = getUserAgent();
        final ImageManager manager = userAgent.getFactory().getImageManager();
        ImageInfo info = null;
        try {
            info = manager.getImageInfo(this.url,
                    userAgent.getImageSessionContext());
        } catch (final ImageException e) {
            final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.imageError(this, this.url, e, getLocator());
        } catch (final FileNotFoundException fnfe) {
            final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.imageNotFound(this, this.url, fnfe, getLocator());
        } catch (final IOException ioe) {
            final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.imageIOError(this, this.url, ioe, getLocator());
        }
        if (info != null) {
            this.intrinsicWidth = info.getSize().getWidthMpt();
            this.intrinsicHeight = info.getSize().getHeightMpt();
            final int baseline = info.getSize().getBaselinePositionFromBottom();
            if (baseline != 0) {
                this.intrinsicAlignmentAdjust = FixedLength
                        .getInstance(-baseline);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        super.startOfNode();
        getFOEventHandler().image(this);
    }

    /**
     * {@inheritDoc} <br>
     * XSL Content Model: empty
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            invalidChildError(loc, nsURI, localName);
        }
    }

    /** @return the "src" property */
    public String getSrc() {
        return this.src;
    }

    /** @return Get the resulting URL based on the src property */
    public String getURL() {
        return this.url;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "external-graphic";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link org.apache.fop.fo.Constants#FO_EXTERNAL_GRAPHIC}
     */
    @Override
    public int getNameId() {
        return FO_EXTERNAL_GRAPHIC;
    }

    /** {@inheritDoc} */
    @Override
    public int getIntrinsicWidth() {
        return this.intrinsicWidth;
    }

    /** {@inheritDoc} */
    @Override
    public int getIntrinsicHeight() {
        return this.intrinsicHeight;
    }

    /** {@inheritDoc} */
    @Override
    public Length getIntrinsicAlignmentAdjust() {
        return this.intrinsicAlignmentAdjust;
    }

}
