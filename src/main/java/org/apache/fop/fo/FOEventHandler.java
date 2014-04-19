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

/* $Id: FOEventHandler.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fo;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fo.extensions.ExternalDocument;
import org.apache.fop.fo.flow.BasicLink;
import org.apache.fop.fo.flow.Block;
import org.apache.fop.fo.flow.BlockContainer;
import org.apache.fop.fo.flow.Character;
import org.apache.fop.fo.flow.ExternalGraphic;
import org.apache.fop.fo.flow.Footnote;
import org.apache.fop.fo.flow.FootnoteBody;
import org.apache.fop.fo.flow.Inline;
import org.apache.fop.fo.flow.InstreamForeignObject;
import org.apache.fop.fo.flow.Leader;
import org.apache.fop.fo.flow.ListBlock;
import org.apache.fop.fo.flow.ListItem;
import org.apache.fop.fo.flow.PageNumber;
import org.apache.fop.fo.flow.PageNumberCitation;
import org.apache.fop.fo.flow.PageNumberCitationLast;
import org.apache.fop.fo.flow.table.Table;
import org.apache.fop.fo.flow.table.TableBody;
import org.apache.fop.fo.flow.table.TableCell;
import org.apache.fop.fo.flow.table.TableColumn;
import org.apache.fop.fo.flow.table.TableFooter;
import org.apache.fop.fo.flow.table.TableHeader;
import org.apache.fop.fo.flow.table.TableRow;
import org.apache.fop.fo.pagination.Flow;
import org.apache.fop.fo.pagination.PageSequence;
import org.apache.fop.fonts.FontEventAdapter;
import org.apache.fop.fonts.FontInfo;
import org.xml.sax.SAXException;

/**
 * Abstract class defining what should be done with SAX events that map to
 * XSL-FO input. The events are actually captured by fo/FOTreeBuilder, passed to
 * the various fo Objects, which in turn, if needed, pass them to an instance of
 * FOEventHandler.
 *
 * Sub-classes will generally fall into one of two categories: 1) a handler that
 * actually builds an FO Tree from the events, or 2) a handler that builds a
 * structured (as opposed to formatted) document, such as our MIF and RTF output
 * targets.
 */
public abstract class FOEventHandler {

    /**
     * The FOUserAgent for this process
     */
    protected FOUserAgent foUserAgent;

    /**
     * The Font information relevant for this document
     */
    protected FontInfo fontInfo;

    /**
     * Main constructor
     * 
     * @param foUserAgent
     *            the apps.FOUserAgent instance for this process
     */
    public FOEventHandler(final FOUserAgent foUserAgent) {
        this.foUserAgent = foUserAgent;
        this.fontInfo = new FontInfo();
        this.fontInfo.setEventListener(new FontEventAdapter(foUserAgent
                .getEventBroadcaster()));
    }

    /**
     * Returns the User Agent object associated with this FOEventHandler.
     * 
     * @return the User Agent object
     */
    public FOUserAgent getUserAgent() {
        return this.foUserAgent;
    }

    /**
     * Retrieve the font information for this document
     * 
     * @return the FontInfo instance for this document
     */
    public FontInfo getFontInfo() {
        return this.fontInfo;
    }

    /**
     * This method is called to indicate the start of a new document run.
     * 
     * @throws SAXException
     *             In case of a problem
     */
    public void startDocument() throws SAXException {
    }

    /**
     * This method is called to indicate the end of a document run.
     * 
     * @throws SAXException
     *             In case of a problem
     */
    public void endDocument() throws SAXException {
    }

    /**
     *
     * @param pageSeq
     *            PageSequence that is starting.
     */
    public void startPageSequence(final PageSequence pageSeq) {
    }

    /**
     * @param pageSeq
     *            PageSequence that is ending.
     */
    public void endPageSequence(final PageSequence pageSeq) {
    }

    /**
     *
     * @param pagenum
     *            PageNumber that is starting.
     */
    public void startPageNumber(final PageNumber pagenum) {
    }

    /**
     *
     * @param pagenum
     *            PageNumber that is ending.
     */
    public void endPageNumber(final PageNumber pagenum) {
    }

    /**
     *
     * @param pageCite
     *            PageNumberCitation that is starting.
     */
    public void startPageNumberCitation(final PageNumberCitation pageCite) {
    }

    /**
     *
     * @param pageCite
     *            PageNumberCitation that is ending.
     */
    public void endPageNumberCitation(final PageNumberCitation pageCite) {
    }

    /**
     *
     * @param pageLast
     *            PageNumberCitationLast that is starting.
     */
    public void startPageNumberCitationLast(
            final PageNumberCitationLast pageLast) {
    }

    /**
     *
     * @param pageLast
     *            PageNumberCitationLast that is ending.
     */
    public void endPageNumberCitationLast(final PageNumberCitationLast pageLast) {
    }

    /**
     * This method is called to indicate the start of a new fo:flow or
     * fo:static-content. This method also handles fo:static-content tags,
     * because the StaticContent class is derived from the Flow class.
     *
     * @param fl
     *            Flow that is starting.
     */
    public void startFlow(final Flow fl) {
    }

    /**
     *
     * @param fl
     *            Flow that is ending.
     */
    public void endFlow(final Flow fl) {
    }

    /**
     *
     * @param bl
     *            Block that is starting.
     */
    public void startBlock(final Block bl) {
    }

    /**
     *
     * @param bl
     *            Block that is ending.
     */
    public void endBlock(final Block bl) {
    }

    /**
     *
     * @param blc
     *            BlockContainer that is starting.
     */
    public void startBlockContainer(final BlockContainer blc) {
    }

    /**
     *
     * @param blc
     *            BlockContainer that is ending.
     */
    public void endBlockContainer(final BlockContainer blc) {
    }

    /**
     *
     * @param inl
     *            Inline that is starting.
     */
    public void startInline(final Inline inl) {
    }

    /**
     *
     * @param inl
     *            Inline that is ending.
     */
    public void endInline(final Inline inl) {
    }

    // Tables
    /**
     *
     * @param tbl
     *            Table that is starting.
     */
    public void startTable(final Table tbl) {
    }

    /**
     *
     * @param tbl
     *            Table that is ending.
     */
    public void endTable(final Table tbl) {
    }

    /**
     *
     * @param tc
     *            TableColumn that is starting;
     */
    public void startColumn(final TableColumn tc) {
    }

    /**
     *
     * @param tc
     *            TableColumn that is ending;
     */
    public void endColumn(final TableColumn tc) {
    }

    /**
     *
     * @param header
     *            TableHeader that is starting;
     */
    public void startHeader(final TableHeader header) {
    }

    /**
     *
     * @param header
     *            TableHeader that is ending.
     */
    public void endHeader(final TableHeader header) {
    }

    /**
     *
     * @param footer
     *            TableFooter that is starting.
     */
    public void startFooter(final TableFooter footer) {
    }

    /**
     *
     * @param footer
     *            TableFooter that is ending.
     */
    public void endFooter(final TableFooter footer) {
    }

    /**
     *
     * @param body
     *            TableBody that is starting.
     */
    public void startBody(final TableBody body) {
    }

    /**
     *
     * @param body
     *            TableBody that is ending.
     */
    public void endBody(final TableBody body) {
    }

    /**
     *
     * @param tr
     *            TableRow that is starting.
     */
    public void startRow(final TableRow tr) {
    }

    /**
     *
     * @param tr
     *            TableRow that is ending.
     */
    public void endRow(final TableRow tr) {
    }

    /**
     *
     * @param tc
     *            TableCell that is starting.
     */
    public void startCell(final TableCell tc) {
    }

    /**
     *
     * @param tc
     *            TableCell that is ending.
     */
    public void endCell(final TableCell tc) {
    }

    // Lists
    /**
     *
     * @param lb
     *            ListBlock that is starting.
     */
    public void startList(final ListBlock lb) {
    }

    /**
     *
     * @param lb
     *            ListBlock that is ending.
     */
    public void endList(final ListBlock lb) {
    }

    /**
     *
     * @param li
     *            ListItem that is starting.
     */
    public void startListItem(final ListItem li) {
    }

    /**
     *
     * @param li
     *            ListItem that is ending.
     */
    public void endListItem(final ListItem li) {
    }

    /**
     * Process start of a ListLabel.
     */
    public void startListLabel() {
    }

    /**
     * Process end of a ListLabel.
     */
    public void endListLabel() {
    }

    /**
     * Process start of a ListBody.
     */
    public void startListBody() {
    }

    /**
     * Process end of a ListBody.
     */
    public void endListBody() {
    }

    // Static Regions
    /**
     * Process start of a Static.
     */
    public void startStatic() {
    }

    /**
     * Process end of a Static.
     */
    public void endStatic() {
    }

    /**
     * Process start of a Markup.
     */
    public void startMarkup() {
    }

    /**
     * Process end of a Markup.
     */
    public void endMarkup() {
    }

    /**
     * Process start of a Link.
     * 
     * @param basicLink
     *            BasicLink that is ending
     */
    public void startLink(final BasicLink basicLink) {
    }

    /**
     * Process end of a Link.
     */
    public void endLink() {
    }

    /**
     * Process an ExternalGraphic.
     * 
     * @param eg
     *            ExternalGraphic to process.
     */
    public void image(final ExternalGraphic eg) {
    }

    /**
     * Process a pageRef.
     */
    public void pageRef() {
    }

    /**
     * Process an InstreamForeignObject.
     * 
     * @param ifo
     *            InstreamForeignObject to process.
     */
    public void foreignObject(final InstreamForeignObject ifo) {
    }

    /**
     * Process the start of a footnote.
     * 
     * @param footnote
     *            Footnote that is starting
     */
    public void startFootnote(final Footnote footnote) {
    }

    /**
     * Process the ending of a footnote.
     * 
     * @param footnote
     *            Footnote that is ending
     */
    public void endFootnote(final Footnote footnote) {
    }

    /**
     * Process the start of a footnote body.
     * 
     * @param body
     *            FootnoteBody that is starting
     */
    public void startFootnoteBody(final FootnoteBody body) {
    }

    /**
     * Process the ending of a footnote body.
     * 
     * @param body
     *            FootnoteBody that is ending
     */
    public void endFootnoteBody(final FootnoteBody body) {
    }

    /**
     * Process a Leader.
     * 
     * @param l
     *            Leader to process.
     */
    public void leader(final Leader l) {
    }

    /**
     * Process a Character.
     * 
     * @param c
     *            Character to process.
     */
    public void character(final Character c) {
    }

    /**
     * Process character data.
     * 
     * @param data
     *            Array of characters to process.
     * @param start
     *            Offset for characters to process.
     * @param length
     *            Portion of array to process.
     */
    public void characters(final char[] data, final int start, final int length) {
    }

    /**
     * Process the start of the external-document extension.
     * 
     * @param document
     *            the external-document node
     */
    public void startExternalDocument(final ExternalDocument document) {
    }

    /**
     * Process the end of the external-document extension.
     * 
     * @param document
     *            the external-document node
     */
    public void endExternalDocument(final ExternalDocument document) {
    }

}
