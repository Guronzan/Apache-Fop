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

/* $Id: AccessibilityPreprocessor.java 830293 2009-10-27 19:07:52Z vhennebert $ */

package org.apache.fop.accessibility;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.util.TransformerDefaultHandler;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class prepares an XSL-FO document for accessibility. It adds a unique
 * identifier to every applicable FO, then creates the structure tree, before
 * handing the document over to the regular handler.
 */
class AccessibilityPreprocessor extends TransformerDefaultHandler {

    private final ByteArrayOutputStream enrichedFOBuffer = new ByteArrayOutputStream();

    private final Transformer reduceFOTree;

    private final FOUserAgent userAgent;

    private final DefaultHandler fopHandler;

    public AccessibilityPreprocessor(final TransformerHandler addPtr,
            final Transformer reduceFOTree, final FOUserAgent userAgent,
            final DefaultHandler fopHandler) {
        super(addPtr);
        this.reduceFOTree = reduceFOTree;
        this.userAgent = userAgent;
        this.fopHandler = fopHandler;
        getTransformerHandler().setResult(
                new StreamResult(this.enrichedFOBuffer));
    }

    /** {@inheritDoc} */
    @Override
    public void endDocument() throws SAXException {
        super.endDocument();
        // do the second transform to struct
        try {
            // TODO this must be optimized, no buffering (ex. SAX-based
            // tee-proxy)
            final byte[] enrichedFO = this.enrichedFOBuffer.toByteArray();
            final Source src = new StreamSource(new ByteArrayInputStream(
                    enrichedFO));
            final DOMResult res = new DOMResult();
            this.reduceFOTree.transform(src, res);
            final StructureTree structureTree = new StructureTree();
            final NodeList pageSequences = res.getNode().getFirstChild()
                    .getChildNodes();
            for (int i = 0; i < pageSequences.getLength(); i++) {
                structureTree.addPageSequenceStructure(pageSequences.item(i)
                        .getChildNodes());
            }
            this.userAgent.setStructureTree(structureTree);

            final SAXParserFactory saxParserFactory = SAXParserFactory
                    .newInstance();
            saxParserFactory.setNamespaceAware(true);
            saxParserFactory.setValidating(false);
            final SAXParser saxParser = saxParserFactory.newSAXParser();
            final InputStream in = new ByteArrayInputStream(enrichedFO);
            saxParser.parse(in, this.fopHandler);
        } catch (final Exception e) {
            throw new SAXException(e);
        }
    }

}
