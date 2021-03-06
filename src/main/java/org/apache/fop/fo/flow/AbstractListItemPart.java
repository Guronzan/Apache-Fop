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

/* $Id: AbstractListItemPart.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fo.flow;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.KeepProperty;
import org.xml.sax.Locator;

/**
 * Common superclass for <a href="http://www.w3.org/TR/xsl/#fo_list-item-label">
 * <code>fo:list-item-label</code></a> and <a
 * href="http://www.w3.org/TR/xsl/#fo_list-item-body">
 * <code>fo:list-item-body</code></a>.
 */
public abstract class AbstractListItemPart extends FObj {
    // The value of properties relevant for fo:list-item-label and
    // fo:list-item-body.
    private KeepProperty keepTogether;
    // Valid properties, commented out for performance:
    // private CommonAccessibility commonAccessibility;
    // End of property values

    /** used for FO validation */
    private boolean blockItemFound = false;

    /**
     * Base constructor
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public AbstractListItemPart(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        super.bind(pList);
        this.keepTogether = pList.get(PR_KEEP_TOGETHER).getKeep();
    }

    /**
     * {@inheritDoc} <br>
     * XSL Content Model: marker* (%block;)+
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (localName.equals("marker")) {
                if (this.blockItemFound) {
                    nodesOutOfOrderError(loc, "fo:marker", "(%block;)");
                }
            } else if (!isBlockItem(nsURI, localName)) {
                invalidChildError(loc, nsURI, localName);
            } else {
                this.blockItemFound = true;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void endOfNode() throws FOPException {
        if (!this.blockItemFound) {
            final String contentModel = "marker* (%block;)+";
            getFOValidationEventProducer().missingChildElement(this, getName(),
                    contentModel, true, getLocator());
        }
    }

    /** @return the "keep-together" property. */
    public KeepProperty getKeepTogether() {
        return this.keepTogether;
    }
}
