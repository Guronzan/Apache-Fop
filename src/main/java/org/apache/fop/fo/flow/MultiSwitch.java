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

/* $Id: MultiSwitch.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fo.flow;

// XML
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.xml.sax.Locator;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_multi-switch">
 * <code>fo:multi-switch</code></a> object.
 */
public class MultiSwitch extends FObj {
    // The value of properties relevant for fo:multi-switch.
    // private ToBeImplementedProperty autoRestore;
    // Unused but valid items, commented out for performance:
    // private CommonAccessibility commonAccessibility;
    // End of property values

    static boolean notImplementedWarningGiven = false;

    /**
     * Base constructor
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public MultiSwitch(final FONode parent) {
        super(parent);

        if (!notImplementedWarningGiven) {
            getFOValidationEventProducer().unimplementedFeature(this,
                    getName(), getName(), getLocator());
            notImplementedWarningGiven = true;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        super.bind(pList);
        // autoRestore = pList.get(PR_AUTO_RESTORE);
    }

    /** {@inheritDoc} */
    @Override
    protected void endOfNode() throws FOPException {
        if (this.firstChild == null) {
            missingChildElementError("(multi-case+)");
        }
    }

    /**
     * {@inheritDoc} <br>
     * XSL Content Model: (multi-case+)
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (!localName.equals("multi-case")) {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "multi-switch";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link org.apache.fop.fo.Constants#FO_MULTI_SWITCH}
     */
    @Override
    public int getNameId() {
        return FO_MULTI_SWITCH;
    }
}
