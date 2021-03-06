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

/* $Id: PDFFileSpec.java 815358 2009-09-15 15:07:51Z maxberger $ */

package org.apache.fop.pdf;

/**
 * class representing a /FileSpec object.
 *
 */
public class PDFFileSpec extends PDFObject {

    /**
     * the filename
     */
    protected String filename;

    /**
     * create a /FileSpec object.
     *
     * @param filename
     *            the filename represented by this object
     */
    public PDFFileSpec(final String filename) {

        /* generic creation of object */
        super();

        this.filename = filename;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toPDFString() {
        return getObjectID() + "<<\n/Type /FileSpec\n" + "/F (" + this.filename
                + ")\n" + ">>\nendobj\n";
    }

    /*
     * example 29 0 obj << /Type /FileSpec /F (table1.pdf) >> endobj
     */

    /** {@inheritDoc} */
    @Override
    protected boolean contentEquals(final PDFObject obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof PDFFileSpec)) {
            return false;
        }

        final PDFFileSpec spec = (PDFFileSpec) obj;

        if (!spec.filename.equals(this.filename)) {
            return false;
        }

        return true;
    }
}
