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

/* $Id: PageSegment.java 1039179 2010-11-25 21:04:09Z vhennebert $ */

package org.apache.fop.afp.modca;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.fop.afp.Streamable;

/**
 * A page segment is a MO:DCA-P resource object. It may be stored in an external
 * resource library or it may be carried in a resource group. Page segments
 * contain any combination of IOCA image objects and GOCA graphics objects.
 */
public class PageSegment extends AbstractNamedAFPObject {

    private List<Streamable> objects = null;

    /**
     * Main constructor
     *
     * @param name
     *            the name of this object
     */
    public PageSegment(final String name) {
        super(name);
    }

    /**
     * Returns a list of objects contained withing this page segment
     *
     * @return a list of objects contained within this page segment
     */
    public List<Streamable> getObjects() {
        if (this.objects == null) {
            this.objects = new ArrayList<>();
        }
        return this.objects;
    }

    /**
     * Adds a resource object (image/graphic) to this page segment
     *
     * @param object
     *            the resource objec to add to this page segment
     */
    public void addObject(final AbstractAFPObject object) {
        getObjects().add(object);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeStart(final OutputStream os) throws IOException {
        final byte[] data = new byte[17];
        copySF(data, Type.BEGIN, Category.PAGE_SEGMENT);
        os.write(data);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final OutputStream os) throws IOException {
        super.writeContent(os);
        writeObjects(this.objects, os);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeEnd(final OutputStream os) throws IOException {
        final byte[] data = new byte[17];
        copySF(data, Type.END, Category.PAGE_SEGMENT);
        os.write(data);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.name;
    }
}
