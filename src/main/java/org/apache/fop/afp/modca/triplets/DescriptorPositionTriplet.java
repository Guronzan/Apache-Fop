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

/* $Id: DescriptorPositionTriplet.java 721430 2008-11-28 11:13:12Z acumiskey $ */

package org.apache.fop.afp.modca.triplets;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Associates an ObjectAreaPosition with and ObjectAreaDescriptor structured
 * field
 */
public class DescriptorPositionTriplet extends AbstractTriplet {

    private final byte oapId;

    /**
     * Main constructor
     *
     * @param oapId
     *            the object area position id
     */
    public DescriptorPositionTriplet(final byte oapId) {
        super(DESCRIPTOR_POSITION);
        this.oapId = oapId;
    }

    /** {@inheritDoc} */
    @Override
    public int getDataLength() {
        return 3;
    }

    /** {@inheritDoc} */
    @Override
    public void writeToStream(final OutputStream os) throws IOException {
        final byte[] data = getData();
        data[2] = this.oapId;
        os.write(data);
    }
}
