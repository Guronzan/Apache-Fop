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

/* $Id: ImageEncoderJPEG.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.render.ps;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.xmlgraphics.image.loader.impl.ImageRawJPEG;
import org.apache.xmlgraphics.ps.ImageEncoder;

/**
 * ImageEncoder implementation for JPEG images.
 */
public class ImageEncoderJPEG implements ImageEncoder {
    private final ImageRawJPEG jpeg;

    /**
     * Main constructor
     * 
     * @param jpeg
     *            the JPEG image
     */
    public ImageEncoderJPEG(final ImageRawJPEG jpeg) {
        this.jpeg = jpeg;
    }

    /** {@inheritDoc} */
    @Override
    public void writeTo(final OutputStream out) throws IOException {
        this.jpeg.writeTo(out);
    }

    /** {@inheritDoc} */
    @Override
    public String getImplicitFilter() {
        return "<< >> /DCTDecode";
    }
}