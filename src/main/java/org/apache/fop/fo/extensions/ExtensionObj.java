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

/* $Id: ExtensionObj.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fo.extensions;

import org.apache.fop.fo.FOEventHandler;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * Base class for pdf bookmark extension objects.
 */
public abstract class ExtensionObj extends FObj {

    /**
     * Create a new extension object.
     *
     * @param parent
     *            the parent formatting object
     */
    public ExtensionObj(final FONode parent) {
        super(parent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processNode(final String elementName, final Locator locator,
            final Attributes attlist, final PropertyList pList) {
        // Empty
    }

    /**
     * Create a default property list for this element.
     */
    @Override
    protected PropertyList createPropertyList(final PropertyList parent,
            final FOEventHandler foEventHandler) {
        return null;
    }
}
