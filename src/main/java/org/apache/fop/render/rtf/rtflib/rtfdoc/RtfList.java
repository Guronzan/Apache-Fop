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

/* $Id: RtfList.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.render.rtf.rtflib.rtfdoc;

/*
 * This file is part of the RTF library of the FOP project, which was originally
 * created by Bertrand Delacretaz <bdelacretaz@codeconsult.ch> and by other
 * contributors to the jfor project (www.jfor.org), who agreed to donate jfor to
 * the FOP project.
 */

import java.io.IOException;
import java.io.Writer;
import java.util.Random;

/**
 * Model of an RTF list, which can contain RTF list items
 * 
 * @author Bertrand Delacretaz bdelacretaz@codeconsult.ch
 * @author Christopher Scott, scottc@westinghouse.com
 * @author Peter Herweg, pherweg@web.de
 */
public class RtfList extends RtfContainer {
    private RtfListItem item;
    private final RtfListTable listTable;
    private final boolean hasTableParent;
    private RtfListStyle defaultListStyle;
    private Integer listTemplateId = null;
    private Integer listId = null;
    private static Random listIdGenerator = new Random(0);

    /** Create an RTF list as a child of given container with given attributes */
    RtfList(final RtfContainer parent, final Writer w, final RtfAttributes attr)
            throws IOException {
        super(parent, w, attr);

        // random number generator for ids
        this.listId = (listIdGenerator.nextInt());
        this.listTemplateId = (listIdGenerator.nextInt());

        // create a new list table entry for the list
        this.listTable = getRtfFile().startListTable(attr);
        this.listTable.addList(this);

        // find out if we are nested in a table
        this.hasTableParent = getParentOfClass(RtfTable.class) != null;

        setRtfListStyle(new RtfListStyleBullet());
    }

    /**
     * Close current list item and start a new one
     * 
     * @return new RtfListItem
     * @throws IOException
     *             for I/O problems
     */
    public RtfListItem newListItem() throws IOException {
        if (this.item != null) {
            this.item.close();
        }
        this.item = new RtfListItem(this, this.writer);
        return this.item;
    }

    /**
     * Returns the Id of the list.
     * 
     * @return Id of the list
     */
    public Integer getListId() {
        return this.listId;
    }

    /**
     * Returns the Id of the list template.
     * 
     * @return Id of the list template
     */
    public Integer getListTemplateId() {
        return this.listTemplateId;
    }

    /**
     * Change list style
     * 
     * @param ls
     *            ListStyle to set
     */
    public void setRtfListStyle(final RtfListStyle ls) {
        this.defaultListStyle = ls;
    }

    /**
     * Get list style
     * 
     * @return ListSytle of the List
     */
    public RtfListStyle getRtfListStyle() {
        return this.defaultListStyle;
    }

    /**
     * Returns true, if the list has a parent table.
     * 
     * @return true, if the list has a parent table
     */
    public boolean getHasTableParent() {
        return this.hasTableParent;
    }
}