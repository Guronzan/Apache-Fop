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

/* $Id: PDFPages.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.pdf;

// Java
import java.util.ArrayList;
import java.util.List;

/**
 * class representing a /Pages object.
 *
 * A /Pages object is an ordered collection of pages (/Page objects) (Actually,
 * /Pages can contain further /Pages as well but this implementation doesn't
 * allow this)
 */
public class PDFPages extends PDFObject {

    /**
     * the /Page objects
     */
    protected List<String> kids = new ArrayList<>();

    /**
     * the number of /Page objects
     */
    protected int count = 0;

    // private PDFPages parent;

    /**
     * create a /Pages object. NOTE: The PDFPages object must be created before
     * the PDF document is generated, but it is not written to the stream
     * immediately. It must also be allocated an object ID (so that the kids can
     * refer to the parent) so that the XRef table needs to be updated before
     * this object is written.
     *
     * @param objnum
     *            the object's number
     */
    public PDFPages(final int objnum) {
        setObjectNumber(objnum);
    }

    /**
     * add a /Page object.
     *
     * @param page
     *            the PDFPage to add.
     */
    public void addPage(final PDFPage page) {
        page.setParent(this);
        incrementCount();
    }

    /**
     * Use this method to notify the PDFPages object that a child page
     *
     * @param page
     *            the child page
     */
    public void notifyKidRegistered(final PDFPage page) {
        final int idx = page.getPageIndex();
        if (idx >= 0) {
            while (idx > this.kids.size() - 1) {
                this.kids.add(null);
            }
            if (this.kids.get(idx) != null) {
                throw new IllegalStateException(
                        "A page already exists at index " + idx
                                + " (zero-based).");
            }
            this.kids.set(idx, page.referencePDF());
        } else {
            this.kids.add(page.referencePDF());
        }
    }

    /**
     * get the count of /Page objects
     *
     * @return the number of pages
     */
    public int getCount() {
        return this.count;
    }

    /**
     * increment the count of /Page objects
     */
    public void incrementCount() {
        this.count++;
        // log.debug("Incrementing count to " + this.getCount());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toPDFString() {
        final StringBuilder sb = new StringBuilder(64);
        sb.append(getObjectID()).append("<< /Type /Pages\n/Count ")
                .append(getCount()).append("\n/Kids [");
        for (int i = 0; i < this.kids.size(); ++i) {
            final Object kid = this.kids.get(i);
            if (kid == null) {
                throw new IllegalStateException("Gap in the kids list!");
            }
            sb.append(kid).append(" ");
        }
        sb.append("] >>\nendobj\n");
        return sb.toString();
    }

}
