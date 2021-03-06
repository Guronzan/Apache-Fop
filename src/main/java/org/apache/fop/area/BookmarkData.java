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

/* $Id: BookmarkData.java 684575 2008-08-10 19:18:22Z jeremias $ */

package org.apache.fop.area;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fop.fo.pagination.bookmarks.Bookmark;
import org.apache.fop.fo.pagination.bookmarks.BookmarkTree;

/**
 * An instance of this class is either a PDF bookmark-tree and its child
 * bookmark-items, or a bookmark-item and the child child bookmark-items under
 * it.
 */
public class BookmarkData extends AbstractOffDocumentItem implements Resolvable {

    private final List<BookmarkData> subData = new ArrayList<>();

    // bookmark-title for this fo:bookmark
    private String bookmarkTitle = null;

    // indicator of whether to initially display/hide child bookmarks of this
    // object
    private boolean bShow = true;

    // ID Reference for this bookmark
    private final String idRef;

    // PageViewport that the idRef item refers to
    private PageViewport pageRef = null;

    // unresolved idrefs by this bookmark and child bookmarks below it
    private final Map<String, List<BookmarkData>> unresolvedIDRefs = new HashMap<>();

    /**
     * Create a new bookmark data object. This should only be called by the
     * bookmark-tree item because it has no idref item that needs to be
     * resolved.
     *
     * @param bookmarkTree
     *            fo:bookmark-tree for this document
     */
    public BookmarkData(final BookmarkTree bookmarkTree) {
        this.idRef = null;
        this.whenToProcess = END_OF_DOC;
        // top level defined in Rec to show all child bookmarks
        this.bShow = true;

        for (int count = 0; count < bookmarkTree.getBookmarks().size(); count++) {
            final Bookmark bkmk = bookmarkTree.getBookmarks().get(count);
            addSubData(createBookmarkData(bkmk));
        }
    }

    /**
     * Create a new pdf bookmark data object. This is used by the bookmark-items
     * to create a data object with a idref. During processing, this idref will
     * be subsequently resolved to a particular PageViewport.
     *
     * @param bookmark
     *            the fo:bookmark object
     */
    public BookmarkData(final Bookmark bookmark) {
        this.bookmarkTitle = bookmark.getBookmarkTitle();
        this.bShow = bookmark.showChildItems();
        this.idRef = bookmark.getInternalDestination();
    }

    private void putUnresolved(final String id, final BookmarkData bd) {
        List<BookmarkData> refs = this.unresolvedIDRefs.get(id);
        if (refs == null) {
            refs = new ArrayList<>();
            this.unresolvedIDRefs.put(id, refs);
        }
        refs.add(bd);
    }

    /**
     * Create a new bookmark data root object. This constructor is called by the
     * AreaTreeParser when the <bookmarkTree> element is read from the XML file
     */
    public BookmarkData() {
        this.idRef = null;
        this.whenToProcess = END_OF_DOC;
        this.bShow = true;
    }

    /**
     * Create a new bookmark data object. This constructor is called by the
     * AreaTreeParser when a <bookmark> element is read from the XML file.
     *
     * @param title
     *            the bookmark's title
     * @param showChildren
     *            whether to initially display the bookmark's children
     * @param pv
     *            the target PageViewport
     * @param idRef
     *            the target ID
     */
    public BookmarkData(final String title, final boolean showChildren,
            final PageViewport pv, final String idRef) {
        this.bookmarkTitle = title;
        this.bShow = showChildren;
        this.pageRef = pv;
        this.idRef = idRef;
    }

    /**
     * Get the idref for this bookmark-item
     *
     * @return the idref for the bookmark-item
     */
    public String getIDRef() {
        return this.idRef;
    }

    /**
     * Add a child bookmark data object. This adds a child bookmark in the
     * bookmark hierarchy.
     *
     * @param sub
     *            the child bookmark data
     */
    public void addSubData(final BookmarkData sub) {
        this.subData.add(sub);
        if (sub.pageRef == null || sub.pageRef.equals("")) {
            putUnresolved(sub.getIDRef(), sub);
            final String[] ids = sub.getIDRefs();
            for (final String id : ids) {
                putUnresolved(id, sub);
            }
        }
    }

    /**
     * Get the title for this bookmark object.
     *
     * @return the bookmark title
     */
    public String getBookmarkTitle() {
        return this.bookmarkTitle;
    }

    /**
     * Indicator of whether to initially display child bookmarks.
     *
     * @return true to initially display child bookmarks, false otherwise
     */
    public boolean showChildItems() {
        return this.bShow;
    }

    /**
     * Get the size of child data objects.
     *
     * @return the number of child bookmark data
     */
    public int getCount() {
        return this.subData.size();
    }

    /**
     * Get the child data object.
     *
     * @param count
     *            the index to get
     * @return the child bookmark data
     */
    public BookmarkData getSubData(final int count) {
        return this.subData.get(count);
    }

    /**
     * Get the PageViewport object that this bookmark refers to
     *
     * @return the PageViewport that this bookmark points to
     */
    public PageViewport getPageViewport() {
        return this.pageRef;
    }

    /**
     * Check if this resolvable object has been resolved. A BookmarkData object
     * is considered resolved once the idrefs for it and for all of its child
     * bookmark-items have been resolved.
     *
     * @return true if this object has been resolved
     */
    @Override
    public boolean isResolved() {
        return this.unresolvedIDRefs == null
                || this.unresolvedIDRefs.size() == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getIDRefs() {
        return this.unresolvedIDRefs.keySet().toArray(new String[] {});
    }

    /**
     * Resolve this resolvable object. This resolves the idref of this object
     * and if possible also resolves id references of child elements that have
     * the same id reference.
     *
     * {@inheritDoc} List)
     */
    @Override
    public void resolveIDRef(final String id, final List<PageViewport> pages) {
        if (id.equals(this.idRef)) {
            // Own ID has been resolved, so note the page
            this.pageRef = pages.get(0);
            // Note: Determining the placement inside the page is the renderer's
            // job.
        }

        // Notify all child bookmarks
        final Collection<BookmarkData> refs = this.unresolvedIDRefs.get(id);
        if (refs != null) {
            for (final BookmarkData bd : refs) {
                bd.resolveIDRef(id, pages);
            }
        }
        this.unresolvedIDRefs.remove(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "Bookmarks";
    }

    /**
     * Create and return the bookmark data for this bookmark This creates a
     * bookmark data with the destination and adds all the data from child
     * bookmarks
     *
     * @param bookmark
     *            the Bookmark object for which a bookmark entry should be
     *            created
     * @return the new bookmark data
     */
    private BookmarkData createBookmarkData(final Bookmark bookmark) {
        final BookmarkData data = new BookmarkData(bookmark);
        for (int count = 0; count < bookmark.getChildBookmarks().size(); count++) {
            final Bookmark bkmk = bookmark.getChildBookmarks().get(count);
            data.addSubData(createBookmarkData(bkmk));
        }
        return data;
    }

}
