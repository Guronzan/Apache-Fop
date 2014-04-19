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

/* $Id: SVGObj.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fo.extensions.svg;

import org.apache.fop.fo.FONode;
import org.apache.fop.fo.XMLObj;

/**
 * Class for SVG element objects. This aids in the construction of the SVG
 * Document.
 */
public class SVGObj extends XMLObj {

    /**
     * Constructs an SVG object (called by Maker).
     *
     * @param parent
     *            the parent formatting object
     */
    public SVGObj(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public String getNamespaceURI() {
        return SVGElementMapping.URI;
    }

    /** {@inheritDoc} */
    @Override
    public String getNormalNamespacePrefix() {
        return "svg";
    }

}
