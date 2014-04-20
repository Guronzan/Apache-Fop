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

/* $Id: IDTracker.java 721430 2008-11-28 11:13:12Z acumiskey $ */

package org.apache.fop.area;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * Used by the AreaTreeHandler to keep track of ID reference usage on a
 * PageViewport level.
 */
@Slf4j
public class IDTracker {

    // HashMap of ID's whose area is located on one or more consecutive
    // PageViewports. Each ID has an arraylist of PageViewports that
    // form the defined area of this ID
    private final Map<String, List<PageViewport>> idLocations = new HashMap<>();

    // idref's whose target PageViewports have yet to be identified
    // Each idref has a HashSet of Resolvable objects containing that idref
    private final Map<String, Set<Resolvable>> unresolvedIDRefs = new HashMap<>();

    private final Set<String> unfinishedIDs = new HashSet<>();

    private final Set<String> alreadyResolvedIDs = new HashSet<>();

    /**
     * Tie a PageViewport with an ID found on a child area of the PV. Note that
     * an area with a given ID may be on more than one PV, hence an ID may have
     * more than one PV associated with it.
     *
     * @param id
     *            the property ID of the area
     * @param pv
     *            a page viewport that contains the area with this ID
     */
    public void associateIDWithPageViewport(final String id,
            final PageViewport pv) {
        if (log.isDebugEnabled()) {
            log.debug("associateIDWithPageViewport(" + id + ", " + pv + ")");
        }
        List<PageViewport> pvList = this.idLocations.get(id);
        if (pvList == null) { // first time ID located
            pvList = new ArrayList<>();
            this.idLocations.put(id, pvList);
            pvList.add(pv);
            // signal the PageViewport that it is the first PV to contain this
            // id:
            pv.setFirstWithID(id);
            /*
             * See if this ID is in the unresolved idref list, if so resolve
             * Resolvable objects tied to it.
             */
            if (!this.unfinishedIDs.contains(id)) {
                tryIDResolution(id, pv, pvList);
            }
        } else {
            /*
             * TODO: The check is a quick-fix to avoid a waste when adding
             * inline-ids to the page
             */
            if (!pvList.contains(pv)) {
                pvList.add(pv);
            }
        }
    }

    /**
     * This method tie an ID to the areaTreeHandler until this one is ready to
     * be processed. This is used in page-number-citation-last processing so we
     * know when an id can be resolved.
     *
     * @param id
     *            the id of the object being processed
     */
    public void signalPendingID(final String id) {
        if (log.isDebugEnabled()) {
            log.debug("signalPendingID(" + id + ")");
        }
        this.unfinishedIDs.add(id);
    }

    /**
     * Signals that all areas for the formatting object with the given ID have
     * been generated. This is used to determine when page-number-citation-last
     * ref-ids can be resolved.
     *
     * @param id
     *            the id of the formatting object which was just finished
     */
    public void signalIDProcessed(final String id) {
        if (log.isDebugEnabled()) {
            log.debug("signalIDProcessed(" + id + ")");
        }

        this.alreadyResolvedIDs.add(id);
        if (!this.unfinishedIDs.contains(id)) {
            return;
        }
        this.unfinishedIDs.remove(id);

        final List<PageViewport> pvList = this.idLocations.get(id);
        final Set<Resolvable> todo = this.unresolvedIDRefs.get(id);
        if (todo != null) {
            for (final Resolvable res : todo) {
                res.resolveIDRef(id, pvList);
            }
            this.unresolvedIDRefs.remove(id);
        }
    }

    /**
     * Check if an ID has already been resolved
     *
     * @param id
     *            the id to check
     * @return true if the ID has been resolved
     */
    public boolean alreadyResolvedID(final String id) {
        return this.alreadyResolvedIDs.contains(id);
    }

    /**
     * Tries to resolve all unresolved ID references on the given page.
     *
     * @param id
     *            ID to resolve
     * @param pv
     *            page viewport whose ID refs to resolve
     * @param pvList
     *            of PageViewports
     */
    private void tryIDResolution(final String id, final PageViewport pv,
            final List<PageViewport> pvList) {
        final Set<Resolvable> todo = this.unresolvedIDRefs.get(id);
        if (todo != null) {
            for (final Resolvable res : todo) {
                if (!this.unfinishedIDs.contains(id)) {
                    res.resolveIDRef(id, pvList);
                } else {
                    return;
                }
            }
            this.alreadyResolvedIDs.add(id);
            this.unresolvedIDRefs.remove(id);
        }
    }

    /**
     * Tries to resolve all unresolved ID references on the given page.
     *
     * @param pv
     *            page viewport whose ID refs to resolve
     */
    public void tryIDResolution(final PageViewport pv) {
        final String[] ids = pv.getIDRefs();
        if (ids != null) {
            for (final String id : ids) {
                final List<PageViewport> pvList = this.idLocations.get(id);
                if (pvList != null) {
                    tryIDResolution(id, pv, pvList);
                }
            }
        }
    }

    /**
     * Get the list of page viewports that have an area with a given id.
     *
     * @param id
     *            the id to lookup
     * @return the list of PageViewports
     */
    public List<PageViewport> getPageViewportsContainingID(final String id) {
        return this.idLocations.get(id);
    }

    /**
     * Add an Resolvable object with an unresolved idref
     *
     * @param idref
     *            the idref whose target id has not yet been located
     * @param res
     *            the Resolvable object needing the idref to be resolved
     */
    public void addUnresolvedIDRef(final String idref, final Resolvable res) {
        Set<Resolvable> todo = this.unresolvedIDRefs.get(idref);
        if (todo == null) {
            todo = new HashSet<>();
            this.unresolvedIDRefs.put(idref, todo);
        }
        // add Resolvable object to this HashSet
        todo.add(res);
    }
}
