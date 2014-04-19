/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at

 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id: PDFLaunch.java 815358 2009-09-15 15:07:51Z maxberger $ */
package org.apache.fop.pdf;

/**
 * This class represents the /Launch action.
 */
public class PDFLaunch extends PDFAction {

    private final PDFFileSpec externalFileSpec;

    public PDFLaunch(final PDFFileSpec fileSpec) {
        this.externalFileSpec = fileSpec;
    }

    @Override
    public String getAction() {
        return referencePDF();
    }

    @Override
    public String toPDFString() {
        final StringBuilder sb = new StringBuilder(64);
        sb.append(getObjectID());
        sb.append("<<\n/S /Launch\n/F ");
        sb.append(this.externalFileSpec.referencePDF());
        sb.append(" \n>>\nendobj\n");

        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean contentEquals(final PDFObject obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof PDFLaunch)) {
            return false;
        }

        final PDFLaunch launch = (PDFLaunch) obj;

        if (!launch.externalFileSpec.referencePDF().equals(
                this.externalFileSpec.referencePDF())) {
            return false;
        }

        return true;
    }
}
