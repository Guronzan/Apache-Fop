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

/* $Id: InternalElementMapping.java 830293 2009-10-27 19:07:52Z vhennebert $ */

package org.apache.fop.fo.extensions;

import java.util.HashMap;
import java.util.Set;

import org.apache.fop.fo.ElementMapping;
import org.apache.xmlgraphics.util.QName;

/**
 * Element mapping for FOP's internal extension to XSL-FO.
 */
public class InternalElementMapping extends ElementMapping {

    /** The FOP extension namespace URI */
    public static final String URI = "http://xmlgraphics.apache.org/fop/internal";

    private static final Set PROPERTY_ATTRIBUTES = new java.util.HashSet();

    static {
        // These are FOP's extension properties for accessibility
        PROPERTY_ATTRIBUTES.add("ptr");
    }

    /**
     * Constructor.
     */
    public InternalElementMapping() {
        this.namespaceURI = URI;
    }

    /**
     * Initialize the data structures.
     */
    @Override
    protected void initialize() {
        if (this.foObjs == null) {
            this.foObjs = new HashMap<>();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getStandardPrefix() {
        return "foi";
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAttributeProperty(final QName attributeName) {
        if (!URI.equals(attributeName.getNamespaceURI())) {
            throw new IllegalArgumentException("The namespace URIs don't match");
        }
        return PROPERTY_ATTRIBUTES.contains(attributeName.getLocalName());
    }

}
