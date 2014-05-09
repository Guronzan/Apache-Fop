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

/* $Id: TableHFPenaltyPosition.java 635508 2008-03-10 10:06:37Z jeremias $ */

package org.apache.fop.layoutmgr.table;

import java.util.List;

import lombok.ToString;

import org.apache.fop.layoutmgr.LayoutManager;
import org.apache.fop.layoutmgr.ListElement;
import org.apache.fop.layoutmgr.Position;

/**
 * This class represents a Position specific to TableContentLayoutManager. Used
 * for table headers and footers at breaks.
 */
// TODO ToString via lombok
@ToString
class TableHFPenaltyPosition extends Position {

    /** Element list for the header */
    protected List<ListElement> headerElements;
    /** Element list for the footer */
    protected List<ListElement> footerElements;

    /**
     * Creates a new TableHFPenaltyPosition
     *
     * @param lm
     *            applicable layout manager
     */
    protected TableHFPenaltyPosition(final LayoutManager lm) {
        super(lm);
    }

    /** {@inheritDoc} */
    @Override
    public boolean generatesAreas() {
        return true;
    }

}
