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

/* $Id: PDFGoToRemote.java 815358 2009-09-15 15:07:51Z maxberger $ */

package org.apache.fop.pdf;

/**
 * Class representing a /GoToR object.
 */
public class PDFGoToRemote extends PDFAction {

    /**
     * the file specification
     */
    private final PDFFileSpec pdfFileSpec;
    private int pageReference = 0;
    private String destination = null;
    private boolean newWindow = false;

    /**
     * Create an GoToR object.
     *
     * @param pdfFileSpec
     *            the fileSpec associated with the action
     * @param newWindow
     *            boolean indicating whether the target should be displayed in a
     *            new window
     */
    public PDFGoToRemote(final PDFFileSpec pdfFileSpec, final boolean newWindow) {
        /* generic creation of object */
        super();

        this.pdfFileSpec = pdfFileSpec;
        this.newWindow = newWindow;
    }

    /**
     * create an GoToR object.
     *
     * @param pdfFileSpec
     *            the fileSpec associated with the action
     * @param page
     *            a page reference within the remote document
     * @param newWindow
     *            boolean indicating whether the target should be displayed in a
     *            new window
     */
    public PDFGoToRemote(final PDFFileSpec pdfFileSpec, final int page,
            final boolean newWindow) {
        /* generic creation of object */
        super();

        this.pdfFileSpec = pdfFileSpec;
        this.pageReference = page;
        this.newWindow = newWindow;
    }

    /**
     * create an GoToR object.
     *
     * @param pdfFileSpec
     *            the fileSpec associated with the action
     * @param dest
     *            a named destination within the remote document
     * @param newWindow
     *            boolean indicating whether the target should be displayed in a
     *            new window
     */
    public PDFGoToRemote(final PDFFileSpec pdfFileSpec, final String dest,
            final boolean newWindow) {
        /* generic creation of object */
        super();

        this.pdfFileSpec = pdfFileSpec;
        this.destination = dest;
        this.newWindow = newWindow;
    }

    /**
     * return the action string which will reference this object
     *
     * @return the action String
     */
    @Override
    public String getAction() {
        return referencePDF();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toPDFString() {
        final StringBuilder sb = new StringBuilder(64);
        sb.append(getObjectID());
        sb.append("<<\n/S /GoToR\n/F ");
        sb.append(this.pdfFileSpec.referencePDF());
        sb.append("\n");

        if (this.destination != null) {
            sb.append("/D (").append(this.destination).append(")");
        } else {
            sb.append("/D [ ").append(this.pageReference)
            .append(" /XYZ null null null ]");
        }

        if (this.newWindow) {
            sb.append("/NewWindow true");
        }

        sb.append(" \n>>\nendobj\n");

        return sb.toString();
    }

    /*
     * example 28 0 obj << /S /GoToR /F 29 0 R /D [ 0 /XYZ -6 797 null ] >>
     * endobj
     */

    /** {@inheritDoc} */
    @Override
    protected boolean contentEquals(final PDFObject obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof PDFGoToRemote)) {
            return false;
        }

        final PDFGoToRemote remote = (PDFGoToRemote) obj;

        if (!remote.pdfFileSpec.referencePDF().equals(
                this.pdfFileSpec.referencePDF())) {
            return false;
        }

        if (this.destination != null) {
            if (!this.destination.equals(remote.destination)) {
                return false;
            }
        } else {
            if (this.pageReference != remote.pageReference) {
                return false;
            }
        }

        return this.newWindow == remote.newWindow;
    }
}
