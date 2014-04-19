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

/* $Id: BlockLevelEventProducer.java 932510 2010-04-09 17:05:34Z vhennebert $ */

package org.apache.fop.layoutmgr;

import org.apache.fop.events.EventBroadcaster;
import org.apache.fop.events.EventProducer;
import org.apache.fop.fo.pagination.PageProductionException;
import org.xml.sax.Locator;

/**
 * Event producer interface for block-level layout managers.
 */
public interface BlockLevelEventProducer extends EventProducer {

    /**
     * Provider class for the event producer.
     */
    final class Provider {

        private Provider() {
        }

        /**
         * Returns an event producer.
         * 
         * @param broadcaster
         *            the event broadcaster to use
         * @return the event producer
         */
        public static BlockLevelEventProducer get(
                final EventBroadcaster broadcaster) {
            return (BlockLevelEventProducer) broadcaster
                    .getEventProducerFor(BlockLevelEventProducer.class);
        }
    }

    /**
     * The contents of a table-row are too big to fit in the constraints.
     * 
     * @param source
     *            the event source
     * @param row
     *            the row number
     * @param effCellBPD
     *            the effective extent in block-progression direction of the
     *            cell
     * @param maxCellBPD
     *            the maximum extent in block-progression direction of the cell
     * @param loc
     *            the location of the error or null
     * @event.severity WARN
     */
    void rowTooTall(final Object source, final int row, final int effCellBPD,
            final int maxCellBPD, final Locator loc);

    /**
     * Auto-table layout is not supported, yet.
     * 
     * @param source
     *            the event source
     * @param loc
     *            the location of the error or null
     * @event.severity INFO
     */
    void tableFixedAutoWidthNotSupported(final Object source, final Locator loc);

    /**
     * An formatting object is too wide.
     * 
     * @param source
     *            the event source
     * @param elementName
     *            the formatting object
     * @param effIPD
     *            the effective extent in inline-progression direction of the
     *            table contents
     * @param maxIPD
     *            the maximum extent in inline-progression direction available
     * @param loc
     *            the location of the error or null
     * @event.severity WARN
     */
    void objectTooWide(final Object source, final String elementName,
            final int effIPD, final int maxIPD, final Locator loc);

    /**
     * An overconstrained geometry adjustment rule was triggered (5.3.4, XSL
     * 1.0).
     * 
     * @param source
     *            the event source
     * @param elementName
     *            the formatting object
     * @param amount
     *            the amount of the adjustment (in mpt)
     * @param loc
     *            the location of the error or null
     * @event.severity INFO
     */
    void overconstrainedAdjustEndIndent(final Object source,
            final String elementName, final int amount, final Locator loc);

    /**
     * Contents overflow a viewport.
     * 
     * @param source
     *            the event source
     * @param elementName
     *            the formatting object
     * @param amount
     *            the amount by which the contents overflow (in mpt)
     * @param clip
     *            true if the content will be clipped
     * @param canRecover
     *            indicates whether FOP can recover from this problem and
     *            continue working
     * @param loc
     *            the location of the error or null
     * @throws LayoutException
     *             the layout error provoked by the method call
     * @event.severity FATAL
     */
    void viewportOverflow(final Object source, final String elementName,
            final int amount, final boolean clip, final boolean canRecover,
            final Locator loc) throws LayoutException;

    /**
     * Contents overflow a region viewport.
     * 
     * @param source
     *            the event source
     * @param elementName
     *            the formatting object
     * @param page
     *            the page number/name where the overflow happened
     * @param amount
     *            the amount by which the contents overflow (in mpt)
     * @param clip
     *            true if the content will be clipped
     * @param canRecover
     *            indicates whether FOP can recover from this problem and
     *            continue working
     * @param loc
     *            the location of the error or null
     * @throws LayoutException
     *             the layout error provoked by the method call
     * @event.severity FATAL
     */
    void regionOverflow(final Object source, final String elementName,
            final String page, final int amount, final boolean clip,
            final boolean canRecover, final Locator loc) throws LayoutException;

    /**
     * Indicates that FOP doesn't support flows that are not mapped to
     * region-body, yet.
     * 
     * @param source
     *            the event source
     * @param flowName
     *            the flow name
     * @param masterName
     *            the page master name
     * @param loc
     *            the location of the error or null
     * @throws UnsupportedOperationException
     *             the layout error provoked by the method call
     * @event.severity FATAL
     */
    void flowNotMappingToRegionBody(final Object source, final String flowName,
            final String masterName, final Locator loc)
            throws UnsupportedOperationException;

    /**
     * A page sequence master is exhausted.
     * 
     * @param source
     *            the event source
     * @param pageSequenceMasterName
     *            the name of the page sequence master
     * @param canRecover
     *            indicates whether FOP can recover from this problem and
     *            continue working
     * @param loc
     *            the location of the error or null
     * @throws PageProductionException
     *             the error provoked by the method call
     * @event.severity FATAL
     */
    void pageSequenceMasterExhausted(final Object source,
            final String pageSequenceMasterName, final boolean canRecover,
            final Locator loc) throws PageProductionException;

    /**
     * No subsequences in page sequence master.
     * 
     * @param source
     *            the event source
     * @param pageSequenceMasterName
     *            the name of the page sequence master
     * @param loc
     *            the location of the error or null
     * @throws PageProductionException
     *             the error provoked by the method call
     * @event.severity FATAL
     */
    void missingSubsequencesInPageSequenceMaster(final Object source,
            final String pageSequenceMasterName, final Locator loc)
            throws PageProductionException;

    /**
     * No single-page-master matching in page sequence master.
     * 
     * @param source
     *            the event source
     * @param pageSequenceMasterName
     *            the name of the page sequence master
     * @param pageMasterName
     *            the name of the page master not matching
     * @param loc
     *            the location of the error or null
     * @throws PageProductionException
     *             the error provoked by the method call
     * @event.severity FATAL
     */
    void noMatchingPageMaster(final Object source,
            final String pageSequenceMasterName, final String pageMasterName,
            final Locator loc) throws PageProductionException;

    /**
     * An element that cannot handle changing IPD (list, table) is flowing to a
     * narrower page. Some content may be lost.
     *
     * @param source
     *            the event source
     * @event.severity WARN
     */
    void nonRestartableContentFlowingToNarrowerPage(final Object source);
}
