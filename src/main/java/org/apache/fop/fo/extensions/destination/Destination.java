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

/* $Id: Destination.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fo.extensions.destination;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.extensions.ExtensionElementMapping;
import org.apache.fop.fo.pagination.Root;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * Class for named destinations in PDF.
 */
public class Destination extends FONode {

    private String internalDestination;
    private final Root root;

    /**
     * Constructs a Destination object (called by Maker).
     *
     * @param parent
     *            the parent formatting object
     */
    public Destination(final FONode parent) {
        super(parent);
        this.root = parent.getRoot();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processNode(final String elementName, final Locator locator,
            final Attributes attlist, final PropertyList pList)
            throws FOPException {
        this.internalDestination = attlist.getValue("internal-destination");
        if (this.internalDestination == null
                || this.internalDestination.length() == 0) {
            missingPropertyError("internal-destination");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void endOfNode() throws FOPException {
        this.root.addDestination(this);
    }

    /**
     * {@inheritDoc} XSL/FOP: empty
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        invalidChildError(loc, nsURI, localName);
    }

    /**
     * Returns the internal destination (an reference of the id property of any
     * FO).
     * 
     * @return the internal destination
     */
    public String getInternalDestination() {
        return this.internalDestination;
    }

    /** {@inheritDoc} */
    @Override
    public String getNamespaceURI() {
        return ExtensionElementMapping.URI;
    }

    /** {@inheritDoc} */
    @Override
    public String getNormalNamespacePrefix() {
        return "fox";
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "destination";
    }

}
