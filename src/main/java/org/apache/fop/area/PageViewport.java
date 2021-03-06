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

/* $Id: PageViewport.java 798511 2009-07-28 12:53:58Z jeremias $ */

package org.apache.fop.area;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.fo.Constants;
import org.apache.fop.fo.flow.Marker;
import org.apache.fop.fo.pagination.SimplePageMaster;

/**
 * Page viewport that specifies the viewport area and holds the page contents.
 * This is the top level object for a page and remains valid for the life of the
 * document and the area tree. This object may be used as a key to reference a
 * page. This is the level that creates the page. The page (reference area) is
 * then rendered inside the page object
 */
@Slf4j
public class PageViewport extends AreaTreeObject implements Resolvable,
        Cloneable {

    private Page page;
    private final Rectangle viewArea;
    private final String simplePageMasterName;

    /**
     * Unique key to identify the page. pageNumberString and pageIndex are both
     * no option for this.
     */
    private String pageKey;

    private int pageNumber = -1;
    private String pageNumberString = null;
    private int pageIndex = -1; // -1 = undetermined
    private final boolean blank;

    private transient PageSequence pageSequence;

    // list of id references and the rectangle on the page
    // private Map idReferences = null;

    // set of IDs that appear first (or exclusively) on this page:
    private final Set<String> idFirsts = new java.util.HashSet<>();

    // this keeps a list of currently unresolved areas or extensions
    // once an idref is resolved it is removed
    // when this is empty the page can be rendered
    private Map<String, List<Resolvable>> unresolvedIDRefs = new java.util.HashMap<>();

    private Map<String, List<PageViewport>> pendingResolved = null;

    // hashmap of markers for this page
    // start and end are added by the fo that contains the markers
    private Map<String, Marker> markerFirstStart = null;
    private Map<String, Marker> markerLastStart = null;
    private Map<String, Marker> markerFirstAny = null;
    private Map<String, Marker> markerLastEnd = null;
    private Map<String, Marker> markerLastAny = null;

    /**
     * Create a page viewport.
     *
     * @param spm
     *            SimplePageMaster indicating the page and region dimensions
     * @param pageNumber
     *            the page number
     * @param pageStr
     *            String representation of the page number
     * @param blank
     *            true if this is a blank page
     */
    public PageViewport(final SimplePageMaster spm, final int pageNumber,
            final String pageStr, final boolean blank) {
        this.simplePageMasterName = spm.getMasterName();
        setExtensionAttachments(spm.getExtensionAttachments());
        setForeignAttributes(spm.getForeignAttributes());
        this.blank = blank;
        final int pageWidth = spm.getPageWidth().getValue();
        final int pageHeight = spm.getPageHeight().getValue();
        this.pageNumber = pageNumber;
        this.pageNumberString = pageStr;
        this.viewArea = new Rectangle(0, 0, pageWidth, pageHeight);
        this.page = new Page(spm);
        createSpan(false);
    }

    /**
     * Copy constructor.
     *
     * @param original
     *            the original PageViewport to copy from
     */
    public PageViewport(final PageViewport original) {
        if (original.extensionAttachments != null) {
            setExtensionAttachments(original.extensionAttachments);
        }
        if (original.foreignAttributes != null) {
            setForeignAttributes(original.foreignAttributes);
        }
        this.pageIndex = original.pageIndex;
        this.pageNumber = original.pageNumber;
        this.pageNumberString = original.pageNumberString;
        this.page = (Page) original.page.clone();
        this.viewArea = new Rectangle(original.viewArea);
        this.simplePageMasterName = original.simplePageMasterName;
        this.blank = original.blank;
    }

    /**
     * Constructor used by the area tree parser.
     *
     * @param viewArea
     *            the view area
     * @param pageNumber
     *            the page number
     * @param pageStr
     *            String representation of the page number
     * @param simplePageMasterName
     *            name of the original simple-page-master that generated this
     *            page
     * @param blank
     *            true if this is a blank page
     */
    public PageViewport(final Rectangle viewArea, final int pageNumber,
            final String pageStr, final String simplePageMasterName,
            final boolean blank) {
        this.viewArea = viewArea;
        this.pageNumber = pageNumber;
        this.pageNumberString = pageStr;
        this.simplePageMasterName = simplePageMasterName;
        this.blank = blank;
    }

    /**
     * Sets the page sequence this page belongs to
     *
     * @param seq
     *            the page sequence
     */
    public void setPageSequence(final PageSequence seq) {
        this.pageSequence = seq;
    }

    /** @return the page sequence this page belongs to */
    public PageSequence getPageSequence() {
        return this.pageSequence;
    }

    /**
     * Get the view area rectangle of this viewport.
     *
     * @return the rectangle for this viewport
     */
    public Rectangle getViewArea() {
        return this.viewArea;
    }

    /**
     * Get the page reference area with the contents.
     *
     * @return the page reference area
     */
    public Page getPage() {
        return this.page;
    }

    /**
     * Sets the page object for this PageViewport.
     *
     * @param page
     *            the page
     */
    public void setPage(final Page page) {
        this.page = page;
    }

    /**
     * Get the page number of this page.
     *
     * @return the integer value that represents this page
     */
    public int getPageNumber() {
        return this.pageNumber;
    }

    /**
     * Get the page number of this page.
     *
     * @return the string that represents this page
     */
    public String getPageNumberString() {
        return this.pageNumberString;
    }

    /**
     * Sets the page index of the page in this rendering run. (This is not the
     * same as the page number!)
     *
     * @param index
     *            the page index (zero-based), -1 if it is undetermined
     */
    public void setPageIndex(final int index) {
        this.pageIndex = index;
    }

    /**
     * @return the overall page index of the page in this rendering run
     *         (zero-based, -1 if it is undetermined).
     */
    public int getPageIndex() {
        return this.pageIndex;
    }

    /**
     * Sets the unique key for this PageViewport that will be used to reference
     * this page.
     *
     * @param key
     *            the unique key.
     */
    public void setKey(final String key) {
        this.pageKey = key;
    }

    /**
     * Get the key for this page viewport. This is used so that a serializable
     * key can be used to lookup the page or some other reference.
     *
     * @return a unique page viewport key for this area tree
     */
    public String getKey() {
        if (this.pageKey == null) {
            throw new IllegalStateException(
                    "No page key set on the PageViewport: " + toString());
        }
        return this.pageKey;
    }

    /**
     * Add an "ID-first" to this page. This is typically called by the
     * AreaTreeHandler when associating an ID with a PageViewport.
     *
     * @param id
     *            the id to be registered as first appearing on this page
     */
    public void setFirstWithID(final String id) {
        if (id != null) {
            this.idFirsts.add(id);
        }
    }

    /**
     * Check whether a certain id first appears on this page
     *
     * @param id
     *            the id to be checked
     * @return true if this page is the first where the id appears
     */
    public boolean isFirstWithID(final String id) {
        return this.idFirsts.contains(id);
    }

    /**
     * Add an idref to this page. All idrefs found for child areas of this
     * PageViewport are added to unresolvedIDRefs, for subsequent resolution by
     * AreaTreeHandler calls to this object's resolveIDRef().
     *
     * @param idref
     *            the idref
     * @param res
     *            the child element of this page that needs this idref resolved
     */
    public void addUnresolvedIDRef(final String idref, final Resolvable res) {
        if (this.unresolvedIDRefs == null) {
            this.unresolvedIDRefs = new HashMap<>();
        }
        List<Resolvable> list = this.unresolvedIDRefs.get(idref);
        if (list == null) {
            list = new ArrayList<>();
            this.unresolvedIDRefs.put(idref, list);
        }
        list.add(res);
    }

    /**
     * Check if this page has been fully resolved.
     *
     * @return true if the page is resolved and can be rendered
     */
    @Override
    public boolean isResolved() {
        return this.unresolvedIDRefs == null
                || this.unresolvedIDRefs.size() == 0;
    }

    /**
     * Get the unresolved idrefs for this page.
     *
     * @return String array of idref's that still have not been resolved
     */
    @Override
    public String[] getIDRefs() {
        return this.unresolvedIDRefs == null ? null : this.unresolvedIDRefs
                .keySet().toArray(new String[] {});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resolveIDRef(final String id, final List<PageViewport> pages) {
        if (this.page == null) {
            if (this.pendingResolved == null) {
                this.pendingResolved = new HashMap<>();
            }
            this.pendingResolved.put(id, pages);
        } else {
            if (this.unresolvedIDRefs != null) {
                final List<Resolvable> todo = this.unresolvedIDRefs.get(id);
                if (todo != null) {
                    for (int count = 0; count < todo.size(); count++) {
                        final Resolvable res = todo.get(count);
                        res.resolveIDRef(id, pages);
                    }
                }
            }
        }
        if (this.unresolvedIDRefs != null && pages != null) {
            this.unresolvedIDRefs.remove(id);
            if (this.unresolvedIDRefs.isEmpty()) {
                this.unresolvedIDRefs = null;
            }
        }
    }

    /**
     * Add the markers for this page. Only the required markers are kept. For
     * "first-starting-within-page" it adds the markers that are starting only
     * if the marker class name is not already added. For
     * "first-including-carryover" it adds any starting marker if the marker
     * class name is not already added. For "last-starting-within-page" it adds
     * all marks that are starting, replacing earlier markers. For
     * "last-ending-within-page" it adds all markers that are ending, replacing
     * earlier markers.
     *
     * Should this logic be placed in the Page layout manager.
     *
     * @param marks
     *            the map of markers to add
     * @param starting
     *            if the area being added is starting or ending
     * @param isfirst
     *            if the area being added has is-first trait
     * @param islast
     *            if the area being added has is-last trait
     */
    public void addMarkers(final Map<String, Marker> marks,
            final boolean starting, final boolean isfirst, final boolean islast) {

        if (marks == null) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("--" + marks.keySet() + ": "
                    + (starting ? "starting" : "ending")
                    + (isfirst ? ", first" : "") + (islast ? ", last" : ""));
        }

        // at the start of the area, register is-first and any areas
        if (starting) {
            if (isfirst) {
                if (this.markerFirstStart == null) {
                    this.markerFirstStart = new HashMap<>();
                }
                if (this.markerFirstAny == null) {
                    this.markerFirstAny = new HashMap<>();
                }
                // first on page: only put in new values, leave current
                for (final Entry<String, Marker> entry : marks.entrySet()) {
                    final String key = entry.getKey();
                    final Marker value = entry.getValue();
                    if (!this.markerFirstStart.containsKey(key)) {
                        this.markerFirstStart.put(key, value);
                        if (log.isTraceEnabled()) {
                            log.trace("page " + this.pageNumberString + ": "
                                    + "Adding marker " + key + " to FirstStart");
                        }
                    }
                    if (!this.markerFirstAny.containsKey(key)) {
                        this.markerFirstAny.put(key, value);
                        if (log.isTraceEnabled()) {
                            log.trace("page " + this.pageNumberString + ": "
                                    + "Adding marker " + key + " to FirstAny");
                        }
                    }
                }
                if (this.markerLastStart == null) {
                    this.markerLastStart = new HashMap<>();
                }
                // last on page: replace all
                this.markerLastStart.putAll(marks);
                if (log.isTraceEnabled()) {
                    log.trace("page " + this.pageNumberString + ": "
                            + "Adding all markers to LastStart");
                }
            } else {
                if (this.markerFirstAny == null) {
                    this.markerFirstAny = new HashMap<>();
                }
                // first on page: only put in new values, leave current
                for (final Entry<String, Marker> entry : marks.entrySet()) {
                    final String key = entry.getKey();
                    final Marker value = entry.getValue();
                    if (!this.markerFirstAny.containsKey(key)) {
                        this.markerFirstAny.put(key, value);
                        if (log.isTraceEnabled()) {
                            log.trace("page " + this.pageNumberString + ": "
                                    + "Adding marker " + key + " to FirstAny");
                        }
                    }
                }
            }
        } else {
            // at the end of the area, register is-last and any areas
            if (islast) {
                if (this.markerLastEnd == null) {
                    this.markerLastEnd = new HashMap<>();
                }
                // last on page: replace all
                this.markerLastEnd.putAll(marks);
                if (log.isTraceEnabled()) {
                    log.trace("page " + this.pageNumberString + ": "
                            + "Adding all markers to LastEnd");
                }
            }
            if (this.markerLastAny == null) {
                this.markerLastAny = new HashMap<>();
            }
            // last on page: replace all
            this.markerLastAny.putAll(marks);
            if (log.isTraceEnabled()) {
                log.trace("page " + this.pageNumberString + ": "
                        + "Adding all markers to LastAny");
            }
        }
    }

    /**
     * Get a marker from this page. This will retrieve a marker with the class
     * name and position.
     *
     * @param name
     *            The class name of the marker to retrieve
     * @param pos
     *            the position to retrieve
     * @return Object the marker found or null
     */
    public Marker getMarker(final String name, final int pos) {
        Marker mark = null;
        String posName = null;
        switch (pos) {
        case Constants.EN_FSWP:
            if (this.markerFirstStart != null) {
                mark = this.markerFirstStart.get(name);
                posName = "FSWP";
            }
            if (mark == null && this.markerFirstAny != null) {
                mark = this.markerFirstAny.get(name);
                posName = "FirstAny after " + posName;
            }
            break;
        case Constants.EN_FIC:
            if (this.markerFirstAny != null) {
                mark = this.markerFirstAny.get(name);
                posName = "FIC";
            }
            break;
        case Constants.EN_LSWP:
            if (this.markerLastStart != null) {
                mark = this.markerLastStart.get(name);
                posName = "LSWP";
            }
            if (mark == null && this.markerLastAny != null) {
                mark = this.markerLastAny.get(name);
                posName = "LastAny after " + posName;
            }
            break;
        case Constants.EN_LEWP:
            if (this.markerLastEnd != null) {
                mark = this.markerLastEnd.get(name);
                posName = "LEWP";
            }
            if (mark == null && this.markerLastAny != null) {
                mark = this.markerLastAny.get(name);
                posName = "LastAny after " + posName;
            }
            break;
        default:
            throw new RuntimeException();
        }
        if (log.isTraceEnabled()) {
            log.trace("page " + this.pageNumberString + ": "
                    + "Retrieving marker " + name + " at position " + posName);
        }
        return mark;
    }

    /** Dumps the current marker data to the logger. */
    public void dumpMarkers() {
        if (log.isTraceEnabled()) {
            log.trace("FirstAny: {}", this.markerFirstAny);
            log.trace("FirstStart: {}", this.markerFirstStart);
            log.trace("LastAny: {}", this.markerLastAny);
            log.trace("LastEnd: {}", this.markerLastEnd);
            log.trace("LastStart: {}", this.markerLastStart);
        }
    }

    /**
     * Save the page contents to an object stream. The map of unresolved
     * references are set on the page so that the resolvers can be properly
     * serialized and reloaded.
     *
     * @param out
     *            the object output stream to write the contents
     * @throws IOException
     *             in case of an I/O error while serializing the page
     */
    public void savePage(final ObjectOutputStream out) throws IOException {
        // set the unresolved references so they are serialized
        this.page.setUnresolvedReferences(this.unresolvedIDRefs);
        out.writeObject(this.page);
        this.page = null;
    }

    /**
     * Load the page contents from an object stream. This loads the page
     * contents from the stream and if there are any unresolved references that
     * were resolved while saved they will be resolved on the page contents.
     *
     * @param in
     *            the object input stream to read the page from
     * @throws ClassNotFoundException
     *             if a class was not found while loading the page
     * @throws IOException
     *             if an I/O error occurred while loading the page
     */
    public void loadPage(final ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        this.page = (Page) in.readObject();
        this.unresolvedIDRefs = this.page.getUnresolvedReferences();
        if (this.unresolvedIDRefs != null && this.pendingResolved != null) {
            for (final String id : this.pendingResolved.keySet()) {
                resolveIDRef(id, this.pendingResolved.get(id));
            }
            this.pendingResolved = null;
        }
    }

    /**
     * Clone this page. Used by the page master to create a copy of an original
     * page.
     *
     * @return a copy of this page and associated viewports
     */
    @Override
    public Object clone() {
        return new PageViewport(this);
    }

    /**
     * Clear the page contents to save memory. This object is kept for the life
     * of the area tree since it holds id and marker information and is used as
     * a key.
     */
    public void clear() {
        this.page = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(64);
        sb.append("PageViewport: page=");
        sb.append(getPageNumberString());
        return sb.toString();
    }

    /** @return the name of the simple-page-master that created this page */
    public String getSimplePageMasterName() {
        return this.simplePageMasterName;
    }

    /** @return True if this is a blank page. */
    public boolean isBlank() {
        return this.blank;
    }

    /**
     * Convenience method to get BodyRegion of this PageViewport
     *
     * @return BodyRegion object
     */
    public BodyRegion getBodyRegion() {
        return (BodyRegion) getPage().getRegionViewport(
                Constants.FO_REGION_BODY).getRegionReference();
    }

    /**
     * Convenience method to create a new Span for this this PageViewport.
     *
     * @param spanAll
     *            whether this is a single-column span
     * @return Span object created
     */
    public Span createSpan(final boolean spanAll) {
        return getBodyRegion().getMainReference().createSpan(spanAll);
    }

    /**
     * Convenience method to get the span-reference-area currently being
     * processed
     *
     * @return span currently being processed.
     */
    public Span getCurrentSpan() {
        return getBodyRegion().getMainReference().getCurrentSpan();
    }

    /**
     * Convenience method to get the normal-flow-reference-area currently being
     * processed
     *
     * @return span currently being processed.
     */
    public NormalFlow getCurrentFlow() {
        return getCurrentSpan().getCurrentFlow();
    }

    /**
     * Convenience method to increment the Span to the next NormalFlow to be
     * processed, and to return that flow.
     *
     * @return the next NormalFlow in the Span.
     */
    public NormalFlow moveToNextFlow() {
        return getCurrentSpan().moveToNextFlow();
    }

    /**
     * Convenience method to return a given region-reference-area, keyed by the
     * Constants class identifier for the corresponding formatting object (ie.
     * Constants.FO_REGION_BODY, FO_REGION_START, etc.)
     *
     * @param id
     *            the Constants class identifier for the region.
     * @return the corresponding region-reference-area for this page.
     */
    public RegionReference getRegionReference(final int id) {
        return getPage().getRegionViewport(id).getRegionReference();
    }

}
