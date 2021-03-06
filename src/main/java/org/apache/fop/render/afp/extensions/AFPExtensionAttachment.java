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

/* $Id: AFPExtensionAttachment.java 815383 2009-09-15 16:15:11Z maxberger $ */

package org.apache.fop.render.afp.extensions;

import java.io.Serializable;

import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.apache.xmlgraphics.util.XMLizable;

/**
 * This is the pass-through value object for the AFP extension.
 */
public abstract class AFPExtensionAttachment implements ExtensionAttachment,
        Serializable, XMLizable {

    private static final long serialVersionUID = 7190606822558332901L;

    /** The category URI for this extension attachment. */
    public static final String CATEGORY = "apache:fop:extensions:afp";

    /** name attribute */
    protected static final String ATT_NAME = "name";

    /**
     * the extension element name
     */
    protected String elementName;

    /**
     * the extension name attribute
     */
    protected String name;

    /**
     * Default constructor.
     *
     * @param elementName
     *            the name of the afp extension attachment, may be null
     */
    public AFPExtensionAttachment(final String elementName) {
        this.elementName = elementName;
    }

    /** @return the name */
    public String getElementName() {
        return this.elementName;
    }

    /**
     * @return true if this element has a name attribute
     */
    protected boolean hasName() {
        return this.name != null;
    }

    /** @return the name */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the name of the setup code object.
     * 
     * @param name
     *            The name to set.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    @Override
    public String getCategory() {
        return CATEGORY;
    }

}
