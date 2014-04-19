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

/* $Id: PNGRenderer_onthefly.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.render.bitmap;

import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.fop.apps.FOPException;
import org.apache.fop.area.PageViewport;
import org.apache.fop.render.java2d.Java2DRenderer;
import org.apache.xmlgraphics.image.codec.png.PNGEncodeParam;
import org.apache.xmlgraphics.image.codec.png.PNGImageEncoder;

/**
 * PNG Renderer This class actually does not render itself, instead it extends
 * <code>org.apache.fop.render.java2D.Java2DRenderer</code> and just encode
 * rendering results into PNG format using Batik's image codec
 */
@Slf4j
public class PNGRenderer_onthefly extends Java2DRenderer {

    /** The MIME type for png-Rendering */
    public static final String MIME_TYPE = "image/png";

    /** The file syntax prefix, eg. "page" will output "page1.png" etc */
    private String fileSyntax;

    /** The output directory where images are to be written */
    private File outputDir;

    /** The PNGEncodeParam for the image */
    private PNGEncodeParam renderParams;

    /** The OutputStream for the first Image */
    private OutputStream firstOutputStream;

    /** {@inheritDoc} */
    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsOutOfOrder() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void startRenderer(final OutputStream outputStream)
            throws IOException {
        log.info("rendering areas to PNG");
        setOutputDirectory();
        this.firstOutputStream = outputStream;
    }

    /**
     * Sets the output directory, either from the outfile specified on the
     * command line, or from the directory specified in configuration file. Also
     * sets the file name syntax, eg. "page"
     */
    private void setOutputDirectory() {

        // the file provided on the command line
        final File f = getUserAgent().getOutputFile();

        this.outputDir = f.getParentFile();

        // extracting file name syntax
        final String s = f.getName();
        int i = s.lastIndexOf(".");
        if (s.charAt(i - 1) == '1') {
            i--; // getting rid of the "1"
        }
        this.fileSyntax = s.substring(0, i);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderPage(final PageViewport pageViewport) throws IOException {

        // Do the rendering: get the image for this page
        final RenderedImage image = getPageImage(pageViewport);

        // Encode this image
        log.debug("Encoding page" + (getCurrentPageNumber() + 1));
        this.renderParams = PNGEncodeParam.getDefaultEncodeParam(image);
        final OutputStream os = getCurrentOutputStream(getCurrentPageNumber());
        if (os != null) {
            try {
                final PNGImageEncoder encoder = new PNGImageEncoder(os,
                        this.renderParams);
                encoder.encode(image);
            } finally {
                // Only close self-created OutputStreams
                if (os != this.firstOutputStream) {
                    IOUtils.closeQuietly(os);
                }
            }
        }

        setCurrentPageNumber(getCurrentPageNumber() + 1);
    }

    /**
     * Builds the OutputStream corresponding to this page
     *
     * @param 0-based pageNumber
     * @return the corresponding OutputStream
     */
    private OutputStream getCurrentOutputStream(final int pageNumber) {

        if (pageNumber == 0) {
            return this.firstOutputStream;
        }

        final File f = new File(this.outputDir + File.separator
                + this.fileSyntax + (pageNumber + 1) + ".png");
        try {
            final OutputStream os = new BufferedOutputStream(
                    new FileOutputStream(f));
            return os;
        } catch (final FileNotFoundException e) {
            new FOPException("Can't build the OutputStream\n" + e);
            return null;
        }
    }
}
