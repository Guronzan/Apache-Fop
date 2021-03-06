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

/* $Id: PageProvider.java 827621 2009-10-20 14:53:23Z vhennebert $ */

package org.apache.fop.layoutmgr;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.area.AreaTreeHandler;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.pagination.PageSequence;
import org.apache.fop.fo.pagination.Region;
import org.apache.fop.fo.pagination.SimplePageMaster;

/**
 * <p>
 * This class delivers Page instances. It also caches them as necessary.
 * </p>
 * <p>
 * Additional functionality makes sure that surplus instances that are requested
 * by the page breaker are properly discarded, especially in situations where
 * hard breaks cause blank pages. The reason for that: The page breaker
 * sometimes needs to preallocate additional pages since it doesn't know exactly
 * until the end how many pages it really needs.
 * </p>
 */
@Slf4j
public class PageProvider implements Constants {

    /** Indices are evaluated relative to the first page in the page-sequence. */
    public static final int RELTO_PAGE_SEQUENCE = 0;
    /**
     * Indices are evaluated relative to the first page in the current element
     * list.
     */
    public static final int RELTO_CURRENT_ELEMENT_LIST = 1;

    private final int startPageOfPageSequence;
    private int startPageOfCurrentElementList;
    private int startColumnOfCurrentElementList;
    private final List<Page> cachedPages = new ArrayList<>();

    private int lastPageIndex = -1;
    private int indexOfCachedLastPage = -1;

    // Cache to optimize getAvailableBPD() calls
    private int lastRequestedIndex = -1;
    private int lastReportedBPD = -1;

    /**
     * AreaTreeHandler which activates the PSLM and controls the rendering of
     * its pages.
     */
    private final AreaTreeHandler areaTreeHandler;

    /**
     * fo:page-sequence formatting object being processed by this class
     */
    private final PageSequence pageSeq;

    /**
     * Main constructor.
     *
     * @param ath
     *            the area tree handler
     * @param ps
     *            The page-sequence the provider operates on
     */
    public PageProvider(final AreaTreeHandler ath, final PageSequence ps) {
        this.areaTreeHandler = ath;
        this.pageSeq = ps;
        this.startPageOfPageSequence = ps.getStartingPageNumber();
    }

    /**
     * The page breaker notifies the provider about the page number an element
     * list starts on so it can later retrieve PageViewports relative to this
     * first page.
     *
     * @param startPage
     *            the number of the first page for the element list.
     * @param startColumn
     *            the starting column number for the element list.
     */
    public void setStartOfNextElementList(final int startPage,
            final int startColumn) {
        log.debug("start of the next element list is:" + " page=" + startPage
                + " col=" + startColumn);
        this.startPageOfCurrentElementList = startPage
                - this.startPageOfPageSequence + 1;
        this.startColumnOfCurrentElementList = startColumn;
        // Reset Cache
        this.lastRequestedIndex = -1;
        this.lastReportedBPD = -1;
    }

    /**
     * Sets the index of the last page. This is done as soon as the position of
     * the last page is known or assumed.
     *
     * @param index
     *            the index relative to the first page in the page-sequence
     */
    public void setLastPageIndex(final int index) {
        this.lastPageIndex = index;
    }

    /**
     * Returns the available BPD for the part/page indicated by the index
     * parameter. The index is the part/page relative to the start of the
     * current element list. This method takes multiple columns into account.
     *
     * @param index
     *            zero-based index of the requested part/page
     * @return the available BPD
     */
    public int getAvailableBPD(final int index) {
        // Special optimization: There may be many equal calls by the
        // BreakingAlgorithm
        if (this.lastRequestedIndex == index) {
            if (log.isTraceEnabled()) {
                log.trace("getAvailableBPD(" + index + ") -> (cached) "
                        + this.lastReportedBPD);
            }
            return this.lastReportedBPD;
        }
        int c = index;
        int pageIndex = 0;
        int colIndex = this.startColumnOfCurrentElementList;
        Page page = getPage(false, pageIndex, RELTO_CURRENT_ELEMENT_LIST);
        while (c > 0) {
            colIndex++;
            if (colIndex >= page.getPageViewport().getCurrentSpan()
                    .getColumnCount()) {
                colIndex = 0;
                pageIndex++;
                page = getPage(false, pageIndex, RELTO_CURRENT_ELEMENT_LIST);
            }
            c--;
        }
        this.lastRequestedIndex = index;
        this.lastReportedBPD = page.getPageViewport().getBodyRegion()
                .getRemainingBPD();
        if (log.isTraceEnabled()) {
            log.trace("getAvailableBPD(" + index + ") -> "
                    + this.lastReportedBPD);
        }
        return this.lastReportedBPD;
    }

    // Wish there were a more elegant way to do this in Java
    private int[] getColIndexAndColCount(final int index) {
        int columnCount = 0;
        int colIndex = this.startColumnOfCurrentElementList + index;
        int pageIndex = -1;
        do {
            colIndex -= columnCount;
            pageIndex++;
            final Page page = getPage(false, pageIndex,
                    RELTO_CURRENT_ELEMENT_LIST);
            columnCount = page.getPageViewport().getCurrentSpan()
                    .getColumnCount();
        } while (colIndex >= columnCount);
        return new int[] { colIndex, columnCount };
    }

    /**
     * Compares the IPD of the given part with the following one.
     *
     * @param index
     *            index of the current part
     * @return a negative integer, zero or a positive integer as the current IPD
     *         is less than, equal to or greater than the IPD of the following
     *         part
     */
    public int compareIPDs(final int index) {
        int columnCount = 0;
        int colIndex = this.startColumnOfCurrentElementList + index;
        int pageIndex = -1;
        Page page;
        do {
            colIndex -= columnCount;
            pageIndex++;
            page = getPage(false, pageIndex, RELTO_CURRENT_ELEMENT_LIST);
            columnCount = page.getPageViewport().getCurrentSpan()
                    .getColumnCount();
        } while (colIndex >= columnCount);
        if (colIndex + 1 < columnCount) {
            // Next part is a column on same page => same IPD
            return 0;
        } else {
            final Page nextPage = getPage(false, pageIndex + 1,
                    RELTO_CURRENT_ELEMENT_LIST);
            return page.getPageViewport().getBodyRegion().getIPD()
                    - nextPage.getPageViewport().getBodyRegion().getIPD();
        }
    }

    /**
     * Checks if a break at the passed index would start a new page
     *
     * @param index
     *            the index of the element before the break
     * @return {@code true} if the break starts a new page
     */
    boolean startPage(final int index) {
        return getColIndexAndColCount(index)[0] == 0;
    }

    /**
     * Checks if a break at the passed index would end a page
     *
     * @param index
     *            the index of the element before the break
     * @return {@code true} if the break ends a page
     */
    boolean endPage(final int index) {
        final int[] colIndexAndColCount = getColIndexAndColCount(index);
        return colIndexAndColCount[0] == colIndexAndColCount[1] - 1;
    }

    /**
     * Obtain the applicable column-count for the element at the passed index
     *
     * @param index
     *            the index of the element
     * @return the number of columns
     */
    int getColumnCount(final int index) {
        return getColIndexAndColCount(index)[1];
    }

    /**
     * Returns the part index (0<x<partCount) which denotes the first part on
     * the last page generated by the current element list.
     *
     * @param partCount
     *            Number of parts determined by the breaking algorithm
     * @return the requested part index
     */
    public int getStartingPartIndexForLastPage(final int partCount) {
        int result = 0;
        int idx = 0;
        int pageIndex = 0;
        int colIndex = this.startColumnOfCurrentElementList;
        Page page = getPage(false, pageIndex, RELTO_CURRENT_ELEMENT_LIST);
        while (idx < partCount) {
            if (colIndex >= page.getPageViewport().getCurrentSpan()
                    .getColumnCount()) {
                colIndex = 0;
                pageIndex++;
                page = getPage(false, pageIndex, RELTO_CURRENT_ELEMENT_LIST);
                result = idx;
            }
            colIndex++;
            idx++;
        }
        return result;
    }

    /**
     * Returns a Page.
     *
     * @param isBlank
     *            true if this page is supposed to be blank.
     * @param index
     *            Index of the page (see relativeTo)
     * @param relativeTo
     *            Defines which value the index parameter should be evaluated
     *            relative to. (One of PageProvider.RELTO_*)
     * @return the requested Page
     */
    public Page getPage(final boolean isBlank, final int index,
            final int relativeTo) {
        if (relativeTo == RELTO_PAGE_SEQUENCE) {
            return getPage(isBlank, index);
        } else if (relativeTo == RELTO_CURRENT_ELEMENT_LIST) {
            int effIndex = this.startPageOfCurrentElementList + index;
            effIndex += this.startPageOfPageSequence - 1;
            return getPage(isBlank, effIndex);
        } else {
            throw new IllegalArgumentException("Illegal value for relativeTo: "
                    + relativeTo);
        }
    }

    /**
     * Returns a Page.
     *
     * @param isBlank
     *            true if the Page should be a blank one
     * @param index
     *            the Page's index
     * @return a Page instance
     */
    protected Page getPage(final boolean isBlank, final int index) {
        final boolean isLastPage = this.lastPageIndex >= 0
                && index == this.lastPageIndex;
        if (log.isTraceEnabled()) {
            log.trace("getPage(" + index + " "
                    + (isBlank ? "blank" : "non-blank")
                    + (isLastPage ? " <LAST>" : "") + ")");
        }
        final int intIndex = index - this.startPageOfPageSequence;
        if (log.isTraceEnabled()) {
            if (isBlank) {
                log.trace("blank page requested: " + index);
            }
            if (isLastPage) {
                log.trace("last page requested: " + index);
            }
        }
        while (intIndex >= this.cachedPages.size()) {
            if (log.isTraceEnabled()) {
                log.trace("Caching " + index);
            }
            cacheNextPage(index, isBlank, isLastPage);
        }
        Page page = this.cachedPages.get(intIndex);
        boolean replace = false;
        if (page.getPageViewport().isBlank() != isBlank) {
            log.debug("blank condition doesn't match. Replacing PageViewport.");
            replace = true;
        }
        if (isLastPage && this.indexOfCachedLastPage != intIndex || !isLastPage
                && this.indexOfCachedLastPage >= 0) {
            log.debug("last page condition doesn't match. Replacing PageViewport.");
            replace = true;
            this.indexOfCachedLastPage = isLastPage ? intIndex : -1;
        }
        if (replace) {
            discardCacheStartingWith(intIndex);
            page = cacheNextPage(index, isBlank, isLastPage);
        }
        return page;
    }

    private void discardCacheStartingWith(final int index) {
        while (index < this.cachedPages.size()) {
            this.cachedPages.remove(this.cachedPages.size() - 1);
            if (!this.pageSeq.goToPreviousSimplePageMaster()) {
                log.warn("goToPreviousSimplePageMaster() on the first page called!");
            }
        }
    }

    private Page cacheNextPage(final int index, final boolean isBlank,
            final boolean isLastPage) {
        final String pageNumberString = this.pageSeq
                .makeFormattedPageNumber(index);
        final boolean isFirstPage = this.startPageOfPageSequence == index;
        final SimplePageMaster spm = this.pageSeq.getNextSimplePageMaster(
                index, isFirstPage, isLastPage, isBlank);

        final Region body = spm.getRegion(FO_REGION_BODY);
        if (!this.pageSeq.getMainFlow().getFlowName()
                .equals(body.getRegionName())) {
            // this is fine by the XSL Rec (fo:flow's flow-name can be mapped to
            // any region), but we don't support it yet.
            final BlockLevelEventProducer eventProducer = BlockLevelEventProducer.Provider
                    .get(this.pageSeq.getUserAgent().getEventBroadcaster());
            eventProducer.flowNotMappingToRegionBody(this, this.pageSeq
                    .getMainFlow().getFlowName(), spm.getMasterName(), spm
                    .getLocator());
        }
        final Page page = new Page(spm, index, pageNumberString, isBlank);
        // Set unique key obtained from the AreaTreeHandler
        page.getPageViewport().setKey(
                this.areaTreeHandler.generatePageViewportKey());
        page.getPageViewport().setForeignAttributes(spm.getForeignAttributes());
        this.cachedPages.add(page);
        return page;
    }

}
