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

/* $Id: ContainerDataDescriptor.java 829057 2009-10-23 13:33:18Z jeremias $ */

package org.apache.fop.afp.modca;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.fop.afp.util.BinaryUtils;

/**
 * Container data descriptor (to maintain compatibility with pre-year 2000
 * applications)
 */
public class ContainerDataDescriptor extends AbstractDescriptor {

    /**
     * Main constructor
     *
     * @param width
     *            the container data width
     * @param height
     *            the container data height
     * @param widthRes
     *            the container width resolution
     * @param heightRes
     *            the container height resolution
     */
    public ContainerDataDescriptor(final int width, final int height,
            final int widthRes, final int heightRes) {
        super(width, height, widthRes, heightRes);
    }

    /** {@inheritDoc} */
    @Override
    public void writeToStream(final OutputStream os) throws IOException {
        final byte[] data = new byte[21];
        copySF(data, Type.DESCRIPTOR, Category.OBJECT_CONTAINER);

        // SF length
        final byte[] len = BinaryUtils.convert(data.length - 1, 2);
        data[1] = len[0];
        data[2] = len[1];

        // XocBase = 10 inches
        data[9] = 0x00;

        // YocBase = 10 inches
        data[10] = 0x00;

        // XocUnits
        final byte[] xdpi = BinaryUtils.convert(this.widthRes * 10, 2);
        data[11] = xdpi[0];
        data[12] = xdpi[1];

        // YocUnits
        final byte[] ydpi = BinaryUtils.convert(this.heightRes * 10, 2);
        data[13] = ydpi[0];
        data[14] = ydpi[1];

        // XocSize
        final byte[] xsize = BinaryUtils.convert(this.width, 3);
        data[15] = xsize[0];
        data[16] = xsize[1];
        data[17] = xsize[2];

        // YocSize
        final byte[] ysize = BinaryUtils.convert(this.height, 3);
        data[18] = ysize[0];
        data[19] = ysize[1];
        data[20] = ysize[2];

        os.write(data);
    }

}
