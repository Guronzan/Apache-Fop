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

/* $Id: Declarations.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fo.pagination;

// Java
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.xml.sax.Locator;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_declarations">
 * <code>fo:declarations</code></a> object.
 *
 * A declarations formatting object holds a set of color-profiles and optionally
 * additional non-XSL namespace elements. The color-profiles are held in a
 * hashmap for use with color-profile references.
 */
@Slf4j
public class Declarations extends FObj {

    private Map<String, ColorProfile> colorProfiles = null;

    /**
     * @param parent
     *            FONode that is the parent of this object
     */
    public Declarations(final FONode parent) {
        super(parent);
        ((Root) parent).setDeclarations(this);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) {
        // No properties defined for fo:declarations
    }

    /**
     * {@inheritDoc} <br>
     * XSL 1.0: (color-profile)+ (and non-XSL NS nodes) <br>
     * FOP/XSL 1.1: (color-profile)* (and non-XSL NS nodes)
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (!localName.equals("color-profile")) {
                invalidChildError(loc, nsURI, localName);
            }
        } // anything outside of XSL namespace is OK.
    }

    /**
     * At the end of this element sort out the children into a hashmap of color
     * profiles and a list of extension attachments.
     *
     * @throws FOPException
     *             if there's a problem during processing
     */
    @Override
    protected void endOfNode() throws FOPException {
        if (this.firstChild != null) {
            for (final FONodeIterator iter = getChildNodes(); iter.hasNext();) {
                final FONode node = iter.nextNode();
                if (node.getName().equals("fo:color-profile")) {
                    final ColorProfile cp = (ColorProfile) node;
                    if (!"".equals(cp.getColorProfileName())) {
                        addColorProfile(cp);
                    } else {
                        getFOValidationEventProducer().missingProperty(this,
                                cp.getName(), "color-profile-name",
                                this.locator);
                    }
                } else {
                    log.debug("Ignoring element " + node.getName()
                            + " inside fo:declarations.");
                }
            }
        }
        this.firstChild = null;
    }

    private void addColorProfile(final ColorProfile cp) {
        if (this.colorProfiles == null) {
            this.colorProfiles = new HashMap<>();
        }
        if (this.colorProfiles.get(cp.getColorProfileName()) != null) {
            // duplicate names
            getFOValidationEventProducer().colorProfileNameNotUnique(this,
                    cp.getName(), cp.getColorProfileName(), this.locator);
        }
        this.colorProfiles.put(cp.getColorProfileName(), cp);
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "declarations";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link org.apache.fop.fo.Constants#FO_DECLARATIONS}
     */
    @Override
    public int getNameId() {
        return FO_DECLARATIONS;
    }

    /**
     * Return ColorProfile with given name.
     *
     * @param cpName
     *            Name of ColorProfile, i.e. the value of the color-profile-name
     *            attribute of the fo:color-profile element
     * @return The org.apache.fop.fo.pagination.ColorProfile object associated
     *         with this color-profile-name or null
     */
    public ColorProfile getColorProfile(final String cpName) {
        ColorProfile profile = null;
        if (this.colorProfiles != null) {
            profile = this.colorProfiles.get(cpName);
        }
        return profile;
    }

}
