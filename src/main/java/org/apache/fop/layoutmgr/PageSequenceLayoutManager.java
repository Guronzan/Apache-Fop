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

/* $Id: PageSequenceLayoutManager.java 748981 2009-03-01 08:55:35Z jeremias $ */

package org.apache.fop.layoutmgr;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.area.AreaTreeHandler;
import org.apache.fop.area.AreaTreeModel;
import org.apache.fop.area.LineArea;
import org.apache.fop.fo.pagination.PageSequence;
import org.apache.fop.fo.pagination.PageSequenceMaster;
import org.apache.fop.fo.pagination.SideRegion;
import org.apache.fop.fo.pagination.StaticContent;
import org.apache.fop.layoutmgr.inline.ContentLayoutManager;

/**
 * LayoutManager for a PageSequence. This class is instantiated by
 * area.AreaTreeHandler for each fo:page-sequence found in the input document.
 */
@Slf4j
public class PageSequenceLayoutManager extends
AbstractPageSequenceLayoutManager {

    private final PageProvider pageProvider;

    /**
     * Constructor
     *
     * @param ath
     *            the area tree handler object
     * @param pseq
     *            fo:page-sequence to process
     */
    public PageSequenceLayoutManager(final AreaTreeHandler ath,
            final PageSequence pseq) {
        super(ath, pseq);
        this.pageProvider = new PageProvider(ath, pseq);
    }

    /** @return the PageProvider applicable to this page-sequence. */
    public PageProvider getPageProvider() {
        return this.pageProvider;
    }

    /**
     * @return the PageSequence being managed by this layout manager
     */
    protected PageSequence getPageSequence() {
        return (PageSequence) this.pageSeq;
    }

    /**
     * Provides access to this object
     *
     * @return this PageSequenceLayoutManager instance
     */
    @Override
    public PageSequenceLayoutManager getPSLM() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void activateLayout() {
        initialize();

        LineArea title = null;

        if (getPageSequence().getTitleFO() != null) {
            try {
                final ContentLayoutManager clm = getLayoutManagerMaker()
                        .makeContentLayoutManager(this,
                                getPageSequence().getTitleFO());
                title = (LineArea) clm.getParentArea(null);
            } catch (final IllegalStateException e) {
                // empty title; do nothing
            }
        }

        final AreaTreeModel areaTreeModel = this.areaTreeHandler
                .getAreaTreeModel();
        final org.apache.fop.area.PageSequence pageSequenceAreaObject = new org.apache.fop.area.PageSequence(
                title);
        transferExtensions(pageSequenceAreaObject);
        pageSequenceAreaObject.setLanguage(getPageSequence().getLanguage());
        pageSequenceAreaObject.setCountry(getPageSequence().getCountry());
        areaTreeModel.startPageSequence(pageSequenceAreaObject);
        if (log.isDebugEnabled()) {
            log.debug("Starting layout");
        }

        this.curPage = makeNewPage(false, false);

        final PageBreaker breaker = new PageBreaker(this);
        final int flowBPD = getCurrentPV().getBodyRegion().getRemainingBPD();
        breaker.doLayout(flowBPD);

        finishPage();
    }

    /** {@inheritDoc} */
    @Override
    public void finishPageSequence() {
        if (this.pageSeq.hasId()) {
            this.idTracker.signalIDProcessed(this.pageSeq.getId());
        }
        this.pageSeq.getRoot().notifyPageSequenceFinished(this.currentPageNum,
                this.currentPageNum - this.startPageNum + 1);
        this.areaTreeHandler.notifyPageSequenceFinished(this.pageSeq,
                this.currentPageNum - this.startPageNum + 1);
        getPageSequence().releasePageSequence();

        // If this sequence has a page sequence master so we must reset
        // it in preparation for the next sequence
        final String masterReference = getPageSequence().getMasterReference();
        final PageSequenceMaster pageSeqMaster = this.pageSeq.getRoot()
                .getLayoutMasterSet().getPageSequenceMaster(masterReference);
        if (pageSeqMaster != null) {
            pageSeqMaster.reset();
        }

        if (log.isDebugEnabled()) {
            log.debug("Ending layout");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Page createPage(final int pageNumber, final boolean isBlank) {
        return this.pageProvider.getPage(isBlank, pageNumber,
                PageProvider.RELTO_PAGE_SEQUENCE);
    }

    private void layoutSideRegion(final int regionID) {
        final SideRegion reg = (SideRegion) this.curPage.getSimplePageMaster()
                .getRegion(regionID);
        if (reg == null) {
            return;
        }
        final StaticContent sc = getPageSequence().getStaticContent(
                reg.getRegionName());
        if (sc == null) {
            return;
        }

        final StaticContentLayoutManager lm = getLayoutManagerMaker()
                .makeStaticContentLayoutManager(this, sc, reg);
        lm.doLayout();
    }

    /** {@inheritDoc} */
    @Override
    protected void finishPage() {
        // Layout side regions
        layoutSideRegion(FO_REGION_BEFORE);
        layoutSideRegion(FO_REGION_AFTER);
        layoutSideRegion(FO_REGION_START);
        layoutSideRegion(FO_REGION_END);

        super.finishPage();
    }

}
