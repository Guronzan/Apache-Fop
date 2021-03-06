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

/* $Id: PageSequenceWrapper.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fo.pagination;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.xml.sax.Locator;

/**
 * Class modelling the <a
 * href="http://www.w3.org/TR/xsl/#fo_page-sequence-wrapper">
 * <code>fo:page-sequence-wrapper</code></a> object, first introduced in the XSL
 * 1.1 WD.
 */
public class PageSequenceWrapper extends FObj {
    // The value of properties relevant for this FO
    private String indexClass;
    private String indexKey;

    // End of property values

    /**
     * Create a PageSequenceWrapper instance that is a child of the given parent
     * {@link FONode}.
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public PageSequenceWrapper(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        super.bind(pList);
        this.indexClass = pList.get(PR_INDEX_CLASS).getString();
        this.indexKey = pList.get(PR_INDEX_KEY).getString();
    }

    /**
     * {@inheritDoc} <br>
     * XSL/FOP: (bookmark+)
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (!(localName.equals("page-sequence") || localName
                    .equals("page-sequence-wrapper"))) {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /**
     * Get the value of the <code>index-class</code> property.
     *
     * @return the "index-class" property
     */
    public String getIndexClass() {
        return this.indexClass;
    }

    /**
     * Get the value of the <code>index-key</code> property.
     *
     * @return the "index-key" property
     */
    public String getIndexKey() {
        return this.indexKey;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "page-sequence-wrapper";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link org.apache.fop.fo.Constants#FO_PAGE_SEQUENCE_WRAPPER}
     */
    @Override
    public int getNameId() {
        return FO_PAGE_SEQUENCE_WRAPPER;
    }
}
