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

/* $Id $ */

package org.apache.fop.fo.pagination.bookmarks;

import java.util.ArrayList;
import java.util.List;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.xml.sax.Locator;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_bookmark">
 * <code>fo:bookmark</code></a> object, first introduced in the XSL 1.1 WD.
 */
public class Bookmark extends FObj {
    private BookmarkTitle bookmarkTitle;
    private final List<Bookmark> childBookmarks = new ArrayList<>();

    // The value of properties relevant for this FO
    private String internalDestination;
    private String externalDestination;
    private boolean bShow = true; // from starting-state property

    // Valid, but unused properties. Commented out for performance
    // private CommonAccessibility commonAccessibility;

    /**
     * Create a new Bookmark object that is a child of the given {@link FONode}.
     *
     * @param parent
     *            the parent fo node
     */
    public Bookmark(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        this.externalDestination = pList.get(PR_EXTERNAL_DESTINATION)
                .getString();
        this.internalDestination = pList.get(PR_INTERNAL_DESTINATION)
                .getString();
        this.bShow = pList.get(PR_STARTING_STATE).getEnum() == EN_SHOW;

        // per spec, internal takes precedence if both specified
        if (this.internalDestination.length() > 0) {
            this.externalDestination = null;
        } else if (this.externalDestination.length() == 0) {
            // slightly stronger than spec "should be specified"
            getFOValidationEventProducer().missingLinkDestination(this,
                    getName(), this.locator);
        } else {
            getFOValidationEventProducer().unimplementedFeature(this,
                    getName(), "external-destination", getLocator());
        }
    }

    /**
     * {@inheritDoc} <br>
     * XSL/FOP: (bookmark-title, bookmark*)
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (localName.equals("bookmark-title")) {
                if (this.bookmarkTitle != null) {
                    tooManyNodesError(loc, "fo:bookmark-title");
                }
            } else if (localName.equals("bookmark")) {
                if (this.bookmarkTitle == null) {
                    nodesOutOfOrderError(loc, "fo:bookmark-title",
                            "fo:bookmark");
                }
            } else {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void endOfNode() throws FOPException {
        if (this.bookmarkTitle == null) {
            missingChildElementError("(bookmark-title, bookmark*)");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void addChildNode(final FONode obj) {
        if (obj instanceof BookmarkTitle) {
            this.bookmarkTitle = (BookmarkTitle) obj;
        } else if (obj instanceof Bookmark) {
            this.childBookmarks.add((Bookmark) obj);
        }
    }

    /**
     * Get the bookmark title for this bookmark
     *
     * @return the bookmark title string or an empty string if not found
     */
    public String getBookmarkTitle() {
        return this.bookmarkTitle == null ? "" : this.bookmarkTitle.getTitle();
    }

    /**
     * Returns the value of the internal-destination property.
     *
     * @return the internal-destination
     */
    public String getInternalDestination() {
        return this.internalDestination;
    }

    /**
     * Returns the value of the external-destination property.
     *
     * @return the external-destination
     */
    public String getExternalDestination() {
        return this.externalDestination;
    }

    /**
     * Determines if this fo:bookmark's subitems should be initially displayed
     * or hidden, based on the starting-state property set on this FO.
     *
     * @return true if this bookmark's starting-state is "show", false if
     *         "hide".
     */
    public boolean showChildItems() {
        return this.bShow;
    }

    /**
     * Get the child <code>Bookmark</code>s in an
     * <code>java.util.ArrayList</code>.
     *
     * @return an <code>ArrayList</code> containing the child Bookmarks
     */
    public List<Bookmark> getChildBookmarks() {
        return this.childBookmarks;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "bookmark";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link org.apache.fop.fo.Constants#FO_BOOKMARK}
     */
    @Override
    public int getNameId() {
        return FO_BOOKMARK;
    }
}
