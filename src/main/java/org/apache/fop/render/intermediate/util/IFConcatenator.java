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

/* $Id: IFConcatenator.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.render.intermediate.util;

import java.awt.Dimension;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.apache.fop.render.intermediate.IFDocumentHandler;
import org.apache.fop.render.intermediate.IFDocumentNavigationHandler;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFParser;
import org.apache.xmlgraphics.xmp.Metadata;

/**
 * This class allows to concatenate multiple intermediate format files to a
 * single output file in the final format. It is based on the SAX API and is
 * therefore very fast and does not require to load the individual documents
 * into memory as DOM documents, for example.
 * <p>
 * Note: This class will filter/ignore any document navigation events. Support
 * for this may be added later.
 */
public class IFConcatenator {

    private final IFDocumentHandler targetHandler;

    private int nextPageIndex = 0;

    /**
     * Creates a new IF concatenator.
     *
     * @param targetHandler
     *            the target document handler
     * @param metadata
     *            the metadata object for the generated file (may be null)
     * @throws IFException
     *             @ * if an IF-related error occurs
     */
    public IFConcatenator(final IFDocumentHandler targetHandler,
            final Metadata metadata) throws IFException {
        this.targetHandler = targetHandler;
        startDocument(metadata);
    }

    private void startDocument(final Metadata metadata) throws IFException {
        this.targetHandler.startDocument();
        this.targetHandler.startDocumentHeader();
        if (metadata != null) {
            this.targetHandler.handleExtensionObject(metadata);
        }
        this.targetHandler.endDocumentHeader();
    }

    private void endDocument() throws IFException {
        this.targetHandler.startPageTrailer();
        this.targetHandler.endPageTrailer();
        this.targetHandler.endDocument();
    }

    /**
     * Returns the target document handler.
     *
     * @return the target document handler
     */
    protected IFDocumentHandler getTargetHandler() {
        return this.targetHandler;
    }

    /**
     * Properly finishes the current output file by creating an empty document
     * trailer and calling {@link IFDocumentHandler#endDocument()}.
     * 
     * @throws IFException
     *
     *             @ * if an IF-related error occurs
     */
    public void finish() throws IFException {
        endDocument();
    }

    /**
     * Appends another intermediate format document to the current output file.
     * All document-level content (i.e. the document header and trailer) is
     * ignored. This method shall not be called after {@link #finish()} has been
     * called.
     *
     * @param src
     *            the JAXP Source identifying the input document
     * @throws TransformerException
     *             if an XML-related exception occurs during @ * if an
     *             IF-related error occurs
     */
    public void appendDocument(final Source src) throws TransformerException,
    IFException {
        final IFParser parser = new IFParser();
        parser.parse(src, new IFPageSequenceFilter(getTargetHandler()),
                getTargetHandler().getContext().getUserAgent());
    }

    private class IFPageSequenceFilter extends IFDocumentHandlerProxy {

        private boolean inPageSequence = false;

        public IFPageSequenceFilter(final IFDocumentHandler delegate) {
            super(delegate);
        }

        /** {@inheritDoc} */
        @Override
        public void startDocument() {
            // ignore
        }

        /** {@inheritDoc} */
        @Override
        public void startDocumentHeader() {
            // ignore
        }

        /** {@inheritDoc} */
        @Override
        public void endDocumentHeader() {
            // ignore
        }

        /**
         * {@inheritDoc}
         * 
         * @throws IFException
         */
        @Override
        public void startPageSequence(final String id) throws IFException {
            assert !this.inPageSequence;
            this.inPageSequence = true;

            super.startPageSequence(id);
        }

        /**
         * {@inheritDoc}
         * 
         * @throws IFException
         */
        @Override
        public void startPage(final int index, final String name,
                final String pageMasterName, final Dimension size)
                throws IFException {
            // Adjust page indices
            super.startPage(IFConcatenator.this.nextPageIndex, name,
                    pageMasterName, size);
            IFConcatenator.this.nextPageIndex++;
        }

        /**
         * {@inheritDoc}
         * 
         * @throws IFException
         */
        @Override
        public void endPageSequence() throws IFException {
            super.endPageSequence();

            assert this.inPageSequence;
            this.inPageSequence = false;
        }

        /** {@inheritDoc} */
        @Override
        public void startDocumentTrailer() {
            // ignore
        }

        /** {@inheritDoc} */
        @Override
        public void endDocumentTrailer() {
            // ignore
        }

        /** {@inheritDoc} */
        @Override
        public void endDocument() {
            // ignore
        }

        /**
         * {@inheritDoc}
         * 
         * @throws IFException
         */
        @Override
        public void handleExtensionObject(final Object extension)
                throws IFException {
            if (this.inPageSequence) {
                // Only pass through when inside page-sequence
                super.handleExtensionObject(extension);
            }
        }

        /** {@inheritDoc} */
        @Override
        public IFDocumentNavigationHandler getDocumentNavigationHandler() {
            return null; // Document Navigation is filtered!!!
        }

    }
}
