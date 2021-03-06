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

/* $Id: AttributeQualifierTriplet.java 830929 2009-10-29 13:19:36Z maxberger $ */

package org.apache.fop.afp.modca.triplets;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.fop.afp.util.BinaryUtils;

/**
 * The attribute qualifier triplet is used to specify a qualifier for a document
 * attribute.
 */
public class AttributeQualifierTriplet extends AbstractTriplet {

    private final int seqNumber;
    private final int levNumber;

    /**
     * Main constructor
     *
     * @param seqNumber
     *            the attribute qualifier sequence number
     * @param levNumber
     *            the attribute qualifier level number
     */
    public AttributeQualifierTriplet(final int seqNumber, final int levNumber) {
        super(ATTRIBUTE_QUALIFIER);
        this.seqNumber = seqNumber;
        this.levNumber = levNumber;
    }

    /** {@inheritDoc} */
    @Override
    public void writeToStream(final OutputStream os) throws IOException {
        final byte[] data = getData();
        final byte[] id = BinaryUtils.convert(this.seqNumber, 4);
        System.arraycopy(id, 0, data, 2, id.length);
        final byte[] level = BinaryUtils.convert(this.levNumber, 4);
        System.arraycopy(level, 0, data, 6, level.length);
        os.write(data);
    }

    /** {@inheritDoc} */
    @Override
    public int getDataLength() {
        return 10;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "seqNumber=" + this.seqNumber + ", levNumber=" + this.levNumber;
    }
}
