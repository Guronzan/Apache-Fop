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

/* $Id: ListItem.java 681307 2008-07-31 09:06:10Z jeremias $ */

package org.apache.fop.fo.flow;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.BreakPropertySet;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.CommonMarginBlock;
import org.apache.fop.fo.properties.KeepProperty;
import org.xml.sax.Locator;

/**
 * Class modelling the <a href=http://www.w3.org/TR/xsl/#fo_list-item">
 * <code>fo:list-item</code></a> object.
 */
public class ListItem extends FObj implements BreakPropertySet {
    // The value of properties relevant for fo:list-item.
    private CommonBorderPaddingBackground commonBorderPaddingBackground;
    private CommonMarginBlock commonMarginBlock;
    private int breakAfter;
    private int breakBefore;
    private KeepProperty keepTogether;
    private KeepProperty keepWithNext;
    private KeepProperty keepWithPrevious;
    // Unused but valid items, commented out for performance:
    // private CommonAccessibility commonAccessibility;
    // private CommonAural commonAural;
    // private CommonRelativePosition commonRelativePosition;
    // private int intrusionDisplace;
    // private int relativeAlign;
    // End of property values

    private ListItemLabel label = null;
    private ListItemBody body = null;

    /**
     * Base constructor
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public ListItem(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        super.bind(pList);
        this.commonBorderPaddingBackground = pList
                .getBorderPaddingBackgroundProps();
        this.commonMarginBlock = pList.getMarginBlockProps();
        this.breakAfter = pList.get(PR_BREAK_AFTER).getEnum();
        this.breakBefore = pList.get(PR_BREAK_BEFORE).getEnum();
        this.keepTogether = pList.get(PR_KEEP_TOGETHER).getKeep();
        this.keepWithNext = pList.get(PR_KEEP_WITH_NEXT).getKeep();
        this.keepWithPrevious = pList.get(PR_KEEP_WITH_PREVIOUS).getKeep();
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        super.startOfNode();
        getFOEventHandler().startListItem(this);
    }

    /** {@inheritDoc} */
    @Override
    protected void endOfNode() throws FOPException {
        if (this.label == null || this.body == null) {
            missingChildElementError("marker* (list-item-label,list-item-body)");
        }
        getFOEventHandler().endListItem(this);
    }

    /**
     * {@inheritDoc} <br>
     * XSL Content Model: marker* (list-item-label,list-item-body)
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (localName.equals("marker")) {
                if (this.label != null) {
                    nodesOutOfOrderError(loc, "fo:marker", "fo:list-item-label");
                }
            } else if (localName.equals("list-item-label")) {
                if (this.label != null) {
                    tooManyNodesError(loc, "fo:list-item-label");
                }
            } else if (localName.equals("list-item-body")) {
                if (this.label == null) {
                    nodesOutOfOrderError(loc, "fo:list-item-label",
                            "fo:list-item-body");
                } else if (this.body != null) {
                    tooManyNodesError(loc, "fo:list-item-body");
                }
            } else {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @todo see if can/should rely on base class for this (i.e., add to
     *       childNodes instead)
     */
    @Override
    public void addChildNode(final FONode child) {
        final int nameId = child.getNameId();

        if (nameId == FO_LIST_ITEM_LABEL) {
            this.label = (ListItemLabel) child;
        } else if (nameId == FO_LIST_ITEM_BODY) {
            this.body = (ListItemBody) child;
        } else if (nameId == FO_MARKER) {
            addMarker((Marker) child);
        }
    }

    /** @return the {@link CommonMarginBlock} */
    public CommonMarginBlock getCommonMarginBlock() {
        return this.commonMarginBlock;
    }

    /** @return the {@link CommonBorderPaddingBackground} */
    public CommonBorderPaddingBackground getCommonBorderPaddingBackground() {
        return this.commonBorderPaddingBackground;
    }

    /** @return the "break-after" property */
    @Override
    public int getBreakAfter() {
        return this.breakAfter;
    }

    /** @return the "break-before" property */
    @Override
    public int getBreakBefore() {
        return this.breakBefore;
    }

    /** @return the "keep-with-next" property */
    public KeepProperty getKeepWithNext() {
        return this.keepWithNext;
    }

    /** @return the "keep-with-previous" property */
    public KeepProperty getKeepWithPrevious() {
        return this.keepWithPrevious;
    }

    /** @return the "keep-together" property */
    public KeepProperty getKeepTogether() {
        return this.keepTogether;
    }

    /** @return the label of the list item */
    public ListItemLabel getLabel() {
        return this.label;
    }

    /**
     * @return the body of the list item
     */
    public ListItemBody getBody() {
        return this.body;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "list-item";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link org.apache.fop.fo.Constants#FO_LIST_ITEM}
     */
    @Override
    public int getNameId() {
        return FO_LIST_ITEM;
    }
}
