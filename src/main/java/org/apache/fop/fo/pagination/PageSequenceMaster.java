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

/* $Id: PageSequenceMaster.java 698280 2008-09-23 18:48:34Z adelmelle $ */

package org.apache.fop.fo.pagination;

// Java
import java.util.ArrayList;
import java.util.List;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.layoutmgr.BlockLevelEventProducer;
import org.xml.sax.Locator;

/**
 * Class modelling the <a
 * href="http://www.w3.org/TR/xsl/#fo_page-sequence-master">
 * <code>fo:page-sequence-master</code></a> object.
 *
 * This class handles a list of subsequence specifiers which are simple or
 * complex references to page-masters.
 */
public class PageSequenceMaster extends FObj {
    // The value of properties relevant for fo:page-sequence-master.
    private String masterName;
    // End of property values

    private LayoutMasterSet layoutMasterSet;
    private List subSequenceSpecifiers;
    private SubSequenceSpecifier currentSubSequence;
    private int currentSubSequenceNumber = -1;

    // The terminology may be confusing. A 'page-sequence-master' consists
    // of a sequence of what the XSL spec refers to as
    // 'sub-sequence-specifiers'. These are, in fact, simple or complex
    // references to page-masters. So the methods use the former
    // terminology ('sub-sequence-specifiers', or SSS),
    // but the actual FO's are MasterReferences.

    /**
     * Create a PageSequenceMaster instance that is a child of the given
     * {@link FONode}.
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public PageSequenceMaster(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        this.masterName = pList.get(PR_MASTER_NAME).getString();

        if (this.masterName == null || this.masterName.equals("")) {
            missingPropertyError("master-name");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        this.subSequenceSpecifiers = new ArrayList<>();
        this.layoutMasterSet = this.parent.getRoot().getLayoutMasterSet();
        this.layoutMasterSet.addPageSequenceMaster(this.masterName, this);
    }

    /** {@inheritDoc} */
    @Override
    protected void endOfNode() throws FOPException {
        if (this.firstChild == null) {
            missingChildElementError("(single-page-master-reference|"
                    + "repeatable-page-master-reference|repeatable-page-master-alternatives)+");
        }
    }

    /**
     * {@inheritDoc} <br>
     * XSL/FOP: (single-page-master-reference|repeatable-page-master-reference|
     * repeatable-page-master-alternatives)+
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (!"single-page-master-reference".equals(localName)
                    && !"repeatable-page-master-reference".equals(localName)
                    && !"repeatable-page-master-alternatives".equals(localName)) {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /**
     * Adds a new suqsequence specifier to the page sequence master.
     *
     * @param pageMasterReference
     *            the subsequence to add
     */
    protected void addSubsequenceSpecifier(
            final SubSequenceSpecifier pageMasterReference) {
        this.subSequenceSpecifiers.add(pageMasterReference);
    }

    /**
     * Returns the next subsequence specifier
     *
     * @return a subsequence specifier
     */
    private SubSequenceSpecifier getNextSubSequence() {
        this.currentSubSequenceNumber++;
        if (this.currentSubSequenceNumber >= 0
                && this.currentSubSequenceNumber < this.subSequenceSpecifiers
                        .size()) {
            return (SubSequenceSpecifier) this.subSequenceSpecifiers
                    .get(this.currentSubSequenceNumber);
        }
        return null;
    }

    /**
     * Resets the subsequence specifiers subsystem.
     */
    public void reset() {
        this.currentSubSequenceNumber = -1;
        this.currentSubSequence = null;
        if (this.subSequenceSpecifiers != null) {
            for (int i = 0; i < this.subSequenceSpecifiers.size(); ++i) {
                ((SubSequenceSpecifier) this.subSequenceSpecifiers.get(i))
                        .reset();
            }
        }
    }

    /**
     * Used to set the "cursor position" for the page masters to the previous
     * item.
     *
     * @return true if there is a previous item, false if the current one was
     *         the first one.
     */
    public boolean goToPreviousSimplePageMaster() {
        if (this.currentSubSequence != null) {
            final boolean success = this.currentSubSequence.goToPrevious();
            if (!success) {
                if (this.currentSubSequenceNumber > 0) {
                    this.currentSubSequenceNumber--;
                    this.currentSubSequence = (SubSequenceSpecifier) this.subSequenceSpecifiers
                            .get(this.currentSubSequenceNumber);
                } else {
                    this.currentSubSequence = null;
                }
            }
        }
        return this.currentSubSequence != null;
    }

    /**
     * @return true if the page-sequence-master has a page-master with
     *         page-position="last"
     */
    public boolean hasPagePositionLast() {
        return this.currentSubSequence != null
                && this.currentSubSequence.hasPagePositionLast();
    }

    /**
     * @return true if the page-sequence-master has a page-master with
     *         page-position="only"
     */
    public boolean hasPagePositionOnly() {
        return this.currentSubSequence != null
                && this.currentSubSequence.hasPagePositionOnly();
    }

    /**
     * Returns the next simple-page-master.
     *
     * @param isOddPage
     *            True if the next page number is odd
     * @param isFirstPage
     *            True if the next page is the first
     * @param isLastPage
     *            True if the next page is the last
     * @param isBlankPage
     *            True if the next page is blank
     * @return the requested page master
     * @throws PageProductionException
     *             if there's a problem determining the next page master
     */
    public SimplePageMaster getNextSimplePageMaster(final boolean isOddPage,
            final boolean isFirstPage, final boolean isLastPage,
            final boolean isBlankPage) throws PageProductionException {
        if (this.currentSubSequence == null) {
            this.currentSubSequence = getNextSubSequence();
            if (this.currentSubSequence == null) {
                final BlockLevelEventProducer eventProducer = BlockLevelEventProducer.Provider
                        .get(getUserAgent().getEventBroadcaster());
                eventProducer.missingSubsequencesInPageSequenceMaster(this,
                        this.masterName, getLocator());
            }
        }
        String pageMasterName = this.currentSubSequence.getNextPageMasterName(
                isOddPage, isFirstPage, isLastPage, isBlankPage);
        boolean canRecover = true;
        while (pageMasterName == null) {
            final SubSequenceSpecifier nextSubSequence = getNextSubSequence();
            if (nextSubSequence == null) {
                final BlockLevelEventProducer eventProducer = BlockLevelEventProducer.Provider
                        .get(getUserAgent().getEventBroadcaster());
                eventProducer.pageSequenceMasterExhausted(this,
                        this.masterName, canRecover, getLocator());
                this.currentSubSequence.reset();
                canRecover = false;
            } else {
                this.currentSubSequence = nextSubSequence;
            }
            pageMasterName = this.currentSubSequence.getNextPageMasterName(
                    isOddPage, isFirstPage, isLastPage, isBlankPage);
        }
        final SimplePageMaster pageMaster = this.layoutMasterSet
                .getSimplePageMaster(pageMasterName);
        if (pageMaster == null) {
            final BlockLevelEventProducer eventProducer = BlockLevelEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.noMatchingPageMaster(this, this.masterName,
                    pageMasterName, getLocator());
        }
        return pageMaster;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "page-sequence-master";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link org.apache.fop.fo.Constants#FO_PAGE_SEQUENCE_MASTER}
     */
    @Override
    public int getNameId() {
        return FO_PAGE_SEQUENCE_MASTER;
    }
}
