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

/* $Id: ColumnSetup.java 681307 2008-07-31 09:06:10Z jeremias $ */

package org.apache.fop.layoutmgr.table;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.datatypes.Length;
import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.expr.RelativeNumericProperty;
import org.apache.fop.fo.flow.table.Table;
import org.apache.fop.fo.flow.table.TableColumn;
import org.apache.fop.fo.properties.TableColLength;

/**
 * Class holding a number of columns making up the column setup of a row.
 */
@Slf4j
public class ColumnSetup {

    private final Table table;
    private final List<TableColumn> columns = new ArrayList<>();
    private final List<Object> colWidths = new ArrayList<>();

    private int maxColIndexReferenced = 0;

    /**
     * Main Constructor.
     *
     * @param table
     *            the table to construct this column setup for
     */
    public ColumnSetup(final Table table) {
        this.table = table;
        prepareColumns();
        initializeColumnWidths();
    }

    private void prepareColumns() {
        final List<TableColumn> rawCols = this.table.getColumns();
        if (rawCols != null) {
            int colnum = 1;
            final ListIterator<TableColumn> iter = rawCols.listIterator();
            while (iter.hasNext()) {
                final TableColumn col = iter.next();
                if (col == null) {
                    continue;
                }
                colnum = col.getColumnNumber();
                for (int i = 0; i < col.getNumberColumnsRepeated(); ++i) {
                    while (colnum > this.columns.size()) {
                        this.columns.add(null);
                    }
                    this.columns.set(colnum - 1, col);
                    colnum++;
                }
            }
            // Post-processing the list (looking for gaps)
            // TODO The following block could possibly be removed
            int pos = 1;
            final ListIterator<TableColumn> ppIter = this.columns
                    .listIterator();
            while (ppIter.hasNext()) {
                final TableColumn col = ppIter.next();
                if (col == null) {
                    assert false; // Gaps are filled earlier by
                    // fo.flow.table.Table.finalizeColumns()
                    // log.error("Found a gap in the table-columns at position "
                    // + pos);
                }
                ++pos;
            }
        }
    }

    /**
     * Returns a column. If the index of the column is bigger than the number of
     * explicitly defined columns the last column is returned.
     *
     * @param index
     *            index of the column (1 is the first column)
     * @return the requested column
     */
    public TableColumn getColumn(final int index) {
        final int size = this.columns.size();
        if (index > size) {
            if (index > this.maxColIndexReferenced) {
                this.maxColIndexReferenced = index;
                final TableColumn col = getColumn(1);
                if (!(size == 1 && col.isImplicitColumn())) {
                    assert false; // TODO Seems to be removable as this is now
                    // done in the FO tree
                    log.warn(FONode
                            .decorateWithContextInfo(
                                    "There are fewer table-columns than are needed. "
                                            + "Column "
                                            + index
                                            + " was accessed, but only "
                                            + size
                                            + " columns have been defined. "
                                            + "The last defined column will be reused.",
                                            this.table));
                    if (!this.table.isAutoLayout()) {
                        log.warn("Please note that according XSL-FO 1.0 (7.26.9) says that "
                                + "the 'column-width' property must be specified for every "
                                + "column, unless the automatic table layout is used.");
                    }
                }
            }
            return this.columns.get(size - 1);
        } else {
            return this.columns.get(index - 1);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.columns.toString();
    }

    /** @return the number of columns in the setup. */
    public int getColumnCount() {
        if (this.maxColIndexReferenced > this.columns.size()) {
            return this.maxColIndexReferenced;
        } else {
            return this.columns.size();
        }
    }

    /** @return an Iterator over all columns */
    public Iterator<TableColumn> iterator() {
        return this.columns.iterator();
    }

    /**
     * Initializes the column's widths
     *
     */
    private void initializeColumnWidths() {

        TableColumn col;
        Length colWidth;

        for (int i = this.columns.size(); --i >= 0;) {
            if (this.columns.get(i) != null) {
                col = this.columns.get(i);
                colWidth = col.getColumnWidth();
                this.colWidths.add(0, colWidth);
            }
        }
        this.colWidths.add(0, null);
    }

    /**
     * Works out the base unit for resolving proportional-column-width()
     * [p-c-w(x) = x * base_unit_ipd]
     *
     * @param tlm
     *            the TableLayoutManager
     * @return the computed base unit (in millipoint)
     */
    protected double computeTableUnit(final TableLayoutManager tlm) {
        return computeTableUnit(tlm, tlm.getContentAreaIPD());
    }

    /**
     * Works out the base unit for resolving proportional-column-width()
     * [p-c-w(x) = x * base_unit_ipd]
     *
     * @param percentBaseContext
     *            the percent base context for relative values
     * @param contentAreaIPD
     *            the IPD of the available content area
     * @return the computed base unit (in millipoints)
     */
    public float computeTableUnit(final PercentBaseContext percentBaseContext,
            final int contentAreaIPD) {

        int sumCols = 0;
        float factors = 0;
        float unit = 0;

        /*
         * calculate the total width (specified absolute/percentages), and work
         * out the total number of factors to use to distribute the remaining
         * space (if any)
         */
        for (final Object object : this.colWidths) {
            final Length colWidth = (Length) object;
            if (colWidth != null) {
                sumCols += colWidth.getValue(percentBaseContext);
                if (colWidth instanceof RelativeNumericProperty) {
                    factors += ((RelativeNumericProperty) colWidth)
                            .getTableUnits();
                } else if (colWidth instanceof TableColLength) {
                    factors += ((TableColLength) colWidth).getTableUnits();
                }
            }
        }

        /*
         * distribute the remaining space over the accumulated factors (if any)
         */
        if (factors > 0) {
            if (sumCols < contentAreaIPD) {
                unit = (contentAreaIPD - sumCols) / factors;
            } else {
                log.warn("No space remaining to distribute over columns.");
            }
        }

        return unit;
    }

    /**
     * @param col
     *            column index (1 is first column)
     * @param context
     *            the context for percentage based calculations
     * @return the X offset of the requested column
     */
    public int getXOffset(final int col, final PercentBaseContext context) {
        int xoffset = 0;
        for (int i = col; --i >= 0;) {
            int effCol;
            if (i < this.colWidths.size()) {
                effCol = i;
            } else {
                effCol = this.colWidths.size() - 1;
            }
            if (this.colWidths.get(effCol) != null) {
                xoffset += ((Length) this.colWidths.get(effCol))
                        .getValue(context);
            }
        }
        return xoffset;
    }

    /**
     * Calculates the sum of all column widths.
     *
     * @param context
     *            the context for percentage based calculations
     * @return the requested sum in millipoints
     */
    public int getSumOfColumnWidths(final PercentBaseContext context) {
        int sum = 0;
        for (int i = 1, c = getColumnCount(); i <= c; ++i) {
            int effIndex = i;
            if (i >= this.colWidths.size()) {
                effIndex = this.colWidths.size() - 1;
            }
            if (this.colWidths.get(effIndex) != null) {
                sum += ((Length) this.colWidths.get(effIndex))
                        .getValue(context);
            }
        }
        return sum;
    }

}
