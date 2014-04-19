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

/* $Id: AFPPageSetup.java 748981 2009-03-01 08:55:35Z jeremias $ */

package org.apache.fop.render.afp.extensions;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * This is the pass-through value object for the AFP extension.
 */
public class AFPPageSetup extends AFPExtensionAttachment {

    /** value attribute */
    protected static final String ATT_VALUE = "value";

    /**
     * the extension content
     */
    protected String content;

    /**
     * the extension value attribute
     */
    protected String value;

    /**
     * Default constructor.
     *
     * @param elementName
     *            the name of the setup code object, may be null
     */
    public AFPPageSetup(final String elementName) {
        super(elementName);
    }

    private static final long serialVersionUID = -549941295384013190L;

    /**
     * Returns the value of the extension.
     * 
     * @return the value
     */
    public String getValue() {
        return this.value;
    }

    /**
     * Sets the value
     * 
     * @param source
     *            The value name to set.
     */
    public void setValue(final String source) {
        this.value = source;
    }

    /**
     * Returns the content of the extension.
     * 
     * @return the data
     */
    public String getContent() {
        return this.content;
    }

    /**
     * Sets the data
     * 
     * @param content
     *            The byte data to set.
     */
    public void setContent(final String content) {
        this.content = content;
    }

    /** {@inheritDoc} */
    @Override
    public void toSAX(final ContentHandler handler) throws SAXException {
        final AttributesImpl atts = new AttributesImpl();
        if (this.name != null && this.name.length() > 0) {
            atts.addAttribute(null, ATT_NAME, ATT_NAME, "CDATA", this.name);
        }
        if (this.value != null && this.value.length() > 0) {
            atts.addAttribute(null, ATT_VALUE, ATT_VALUE, "CDATA", this.value);
        }
        handler.startElement(CATEGORY, this.elementName, this.elementName, atts);
        if (this.content != null && this.content.length() > 0) {
            final char[] chars = this.content.toCharArray();
            handler.characters(chars, 0, chars.length);
        }
        handler.endElement(CATEGORY, this.elementName, this.elementName);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "AFPPageSetup(element-name=" + getElementName() + " name="
                + getName() + " value=" + getValue() + ")";
    }
}
