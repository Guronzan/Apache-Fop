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

/* $Id: PDFArray.java 830293 2009-10-27 19:07:52Z vhennebert $ */

package org.apache.fop.pdf;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.output.CountingOutputStream;

/**
 * Class representing an array object.
 */
public class PDFArray extends PDFObject {
    /**
     * List holding the values of this array
     */
    protected List<Object> values = new ArrayList<>();

    /**
     * Create a new, empty array object
     *
     * @param parent
     *            the array's parent if any
     */
    public PDFArray(final PDFObject parent) {
        /* generic creation of PDF object */
        super(parent);
    }

    /**
     * Create a new, empty array object with no parent.
     */
    public PDFArray() {
        this(null);
    }

    /**
     * Create an array object.
     *
     * @param parent
     *            the array's parent if any
     * @param values
     *            the actual array wrapped by this object
     */
    public PDFArray(final PDFObject parent, final int[] values) {
        /* generic creation of PDF object */
        super(parent);

        for (final int value : values) {
            this.values.add(value);
        }
    }

    /**
     * Create an array object.
     *
     * @param parent
     *            the array's parent if any
     * @param values
     *            the actual array wrapped by this object
     */
    public PDFArray(final PDFObject parent, final double[] values) {
        /* generic creation of PDF object */
        super(parent);

        for (final double value : values) {
            this.values.add(new Double(value));
        }
    }

    /**
     * Create an array object.
     *
     * @param parent
     *            the array's parent if any
     * @param values
     *            the actual values wrapped by this object
     */
    public PDFArray(final PDFObject parent, final Collection<Integer> values) {
        /* generic creation of PDF object */
        super(parent);

        this.values.addAll(values);
    }

    /**
     * Create the array object
     *
     * @param parent
     *            the array's parent if any
     * @param values
     *            the actual array wrapped by this object
     */
    public PDFArray(final PDFObject parent, final Object[] values) {
        /* generic creation of PDF object */
        super(parent);

        for (final Object value : values) {
            this.values.add(value);
        }
    }

    /**
     * Indicates whether the given object exists in the array.
     *
     * @param obj
     *            the object to look for
     * @return true if obj is contained
     */
    public boolean contains(final Object obj) {
        return this.values.contains(obj);
    }

    /**
     * Returns the length of the array
     *
     * @return the length of the array
     */
    public int length() {
        return this.values.size();
    }

    /**
     * Sets an entry at a given location.
     *
     * @param index
     *            the index of the value to set
     * @param obj
     *            the new value
     */
    public void set(final int index, final Object obj) {
        this.values.set(index, obj);
    }

    /**
     * Sets an entry at a given location.
     *
     * @param index
     *            the index of the value to set
     * @param value
     *            the new value
     */
    public void set(final int index, final double value) {
        this.values.set(index, new Double(value));
    }

    /**
     * Gets an entry at a given location.
     *
     * @param index
     *            the index of the value to set
     * @return the requested value
     */
    public Object get(final int index) {
        return this.values.get(index);
    }

    /**
     * Adds a new value to the array.
     *
     * @param obj
     *            the value
     */
    public void add(final Object obj) {
        if (obj instanceof PDFObject) {
            final PDFObject pdfObj = (PDFObject) obj;
            if (!pdfObj.hasObjectNumber()) {
                pdfObj.setParent(this);
            }
        }
        this.values.add(obj);
    }

    /**
     * Adds a new value to the array.
     *
     * @param value
     *            the value
     */
    public void add(final double value) {
        this.values.add(new Double(value));
    }

    /** {@inheritDoc} */
    @Override
    protected int output(final OutputStream stream) throws IOException {
        try (final CountingOutputStream cout = new CountingOutputStream(stream)) {
            try (final Writer writer = PDFDocument.getWriterFor(cout)) {
                if (hasObjectNumber()) {
                    writer.write(getObjectID());
                }

                writer.write('[');
                for (int i = 0; i < this.values.size(); ++i) {
                    if (i > 0) {
                        writer.write(' ');
                    }
                    final Object obj = this.values.get(i);
                    formatObject(obj, cout, writer);
                }
                writer.write(']');

                if (hasObjectNumber()) {
                    writer.write("\nendobj\n");
                }

                writer.flush();
                return cout.getCount();
            }
        }
    }

}
