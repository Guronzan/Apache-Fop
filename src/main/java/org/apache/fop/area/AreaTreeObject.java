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

/* $Id: AreaTreeObject.java 735026 2009-01-16 14:29:39Z jeremias $ */

package org.apache.fop.area;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.apache.xmlgraphics.util.QName;

/**
 * Abstract base class for all area tree objects.
 */
public abstract class AreaTreeObject {

    /** Foreign attributes */
    protected Map foreignAttributes = null;

    /** Extension attachments */
    protected List/* <ExtensionAttachment> */extensionAttachments = null;

    /**
     * Sets a foreign attribute.
     * 
     * @param name
     *            the qualified name of the attribute
     * @param value
     *            the attribute value
     */
    public void setForeignAttribute(final QName name, final String value) {
        if (this.foreignAttributes == null) {
            this.foreignAttributes = new java.util.HashMap();
        }
        this.foreignAttributes.put(name, value);
    }

    /**
     * Set foreign attributes from a Map.
     * 
     * @param atts
     *            a Map with attributes (keys: QName, values: String)
     */
    public void setForeignAttributes(final Map atts) {
        if (atts.size() == 0) {
            return;
        }
        final Iterator iter = atts.entrySet().iterator();
        while (iter.hasNext()) {
            final Map.Entry entry = (Map.Entry) iter.next();
            final String value = (String) entry.getValue();
            // The casting is only to ensure type safety (too bad we can't use
            // generics, yet)
            setForeignAttribute((QName) entry.getKey(), value);
        }
    }

    /**
     * Returns the value of a foreign attribute on the area.
     * 
     * @param name
     *            the qualified name of the attribute
     * @return the attribute value or null if it isn't set
     */
    public String getForeignAttributeValue(final QName name) {
        if (this.foreignAttributes != null) {
            return (String) this.foreignAttributes.get(name);
        } else {
            return null;
        }
    }

    /** @return the foreign attributes associated with this area */
    public Map getForeignAttributes() {
        if (this.foreignAttributes != null) {
            return Collections.unmodifiableMap(this.foreignAttributes);
        } else {
            return Collections.EMPTY_MAP;
        }
    }

    private void prepareExtensionAttachmentContainer() {
        if (this.extensionAttachments == null) {
            this.extensionAttachments = new java.util.ArrayList/*
                                                                * <
                                                                * ExtensionAttachment
                                                                * >
                                                                */();
        }
    }

    /**
     * Adds a new ExtensionAttachment instance to this page.
     * 
     * @param attachment
     *            the ExtensionAttachment
     */
    public void addExtensionAttachment(final ExtensionAttachment attachment) {
        prepareExtensionAttachmentContainer();
        this.extensionAttachments.add(attachment);
    }

    /**
     * Set extension attachments from a List
     * 
     * @param extensionAttachments
     *            a List with extension attachments
     */
    public void setExtensionAttachments(final List extensionAttachments) {
        prepareExtensionAttachmentContainer();
        this.extensionAttachments.addAll(extensionAttachments);
    }

    /** @return the extension attachments associated with this area */
    public List getExtensionAttachments() {
        if (this.extensionAttachments != null) {
            return Collections.unmodifiableList(this.extensionAttachments);
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Indicates whether this area tree object has any extension attachments.
     * 
     * @return true if there are extension attachments
     */
    public boolean hasExtensionAttachments() {
        return this.extensionAttachments != null
                && !this.extensionAttachments.isEmpty();
    }

}
