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

/* $Id: DelegatingContentHandler.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.util;

import java.io.IOException;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;

/**
 * SAX 2 Event Handler which simply delegates all calls to another ContentHandler. Subclasses can
 * do additional processing. This class is the passive counterpart to XMLFilterImpl.
 * <p>
 * The ContentHandler is the only instance that is required. All others (DTDHandler,
 * EntityResolver, LexicalHandler and ErrorHandler) may be ignored.
 */
public class DelegatingContentHandler
        implements EntityResolver, DTDHandler, ContentHandler, LexicalHandler, ErrorHandler {

    private ContentHandler delegate;
    private EntityResolver entityResolver;
    private DTDHandler dtdHandler;
    private LexicalHandler lexicalHandler;
    private ErrorHandler errorHandler;

    /**
     * Main constructor.
     */
    public DelegatingContentHandler() {
        //nop
    }

    /**
     * Convenience constructor. If the given handler also implements any of the EntityResolver,
     * DTDHandler, LexicalHandler or ErrorHandler interfaces, these are set automatically.
     * @param handler the content handler to delegate to
     */
    public DelegatingContentHandler(ContentHandler handler) {
        setDelegateContentHandler(handler);
        if (handler instanceof EntityResolver) {
            setDelegateEntityResolver((EntityResolver)handler);
        }
        if (handler instanceof DTDHandler) {
            setDelegateDTDHandler((DTDHandler)handler);
        }
        if (handler instanceof LexicalHandler) {
            setDelegateLexicalHandler((LexicalHandler)handler);
        }
        if (handler instanceof ErrorHandler) {
            setDelegateErrorHandler((ErrorHandler)handler);
        }
    }

    /**
     * @return the delegate that all ContentHandler events are forwarded to
     */
    public ContentHandler getDelegateContentHandler() {
        return this.delegate;
    }

    /**
     * Sets the delegate ContentHandler that all events are forwarded to.
     * @param handler the delegate instance
     */
    public void setDelegateContentHandler(ContentHandler handler) {
        this.delegate = handler;
    }

    /**
     * Sets the delegate EntityResolver.
     * @param resolver the delegate instance
     */
    public void setDelegateEntityResolver(EntityResolver resolver) {
        this.entityResolver = resolver;
    }

    /**
     * Sets the delegate DTDHandler.
     * @param handler the delegate instance
     */
    public void setDelegateDTDHandler(DTDHandler handler) {
        this.dtdHandler = handler;
    }

    /**
     * Sets the delegate LexicalHandler.
     * @param handler the delegate instance
     */
    public void setDelegateLexicalHandler(LexicalHandler handler) {
        this.lexicalHandler = handler;
    }

    /**
     * Sets the delegate ErrorHandler.
     * @param handler the delegate instance
     */
    public void setDelegateErrorHandler(ErrorHandler handler) {
        this.errorHandler = handler;
    }

    // ==== EntityResolver

    /** {@inheritDoc} */
    @Override
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
        if (entityResolver != null) {
            return entityResolver.resolveEntity(publicId, systemId);
        } else {
            return null;
        }
    }

    // ==== DTDHandler

    /** {@inheritDoc} */
    @Override
    public void notationDecl(String name, String publicId, String systemId) throws SAXException {
        if (dtdHandler != null) {
            dtdHandler.notationDecl(name, publicId, systemId);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void unparsedEntityDecl(String name, String publicId, String systemId,
            String notationName) throws SAXException {
        if (dtdHandler != null) {
            dtdHandler.unparsedEntityDecl(name, publicId, systemId, notationName);
        }
    }

    // ==== ContentHandler

    /** {@inheritDoc} */
    @Override
    public void setDocumentLocator(Locator locator) {
        delegate.setDocumentLocator(locator);
    }

    /** {@inheritDoc} */
    @Override
    public void startDocument() throws SAXException {
        delegate.startDocument();
    }

    /** {@inheritDoc} */
    @Override
    public void endDocument() throws SAXException {
        delegate.endDocument();
    }

    /** {@inheritDoc} */
    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        delegate.startPrefixMapping(prefix, uri);
    }

    /** {@inheritDoc} */
    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        delegate.endPrefixMapping(prefix);
    }

    /** {@inheritDoc} */
    @Override
    public void startElement(String uri, String localName, String qName,
                Attributes atts) throws SAXException {
        delegate.startElement(uri, localName, qName, atts);
    }

    /** {@inheritDoc} */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        delegate.endElement(uri, localName, qName);
    }

    /** {@inheritDoc} */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        delegate.characters(ch, start, length);
    }

    /** {@inheritDoc} */
    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        delegate.ignorableWhitespace(ch, start, length);
    }

    /** {@inheritDoc} */
    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        delegate.processingInstruction(target, data);
    }

    /** {@inheritDoc} */
    @Override
    public void skippedEntity(String name) throws SAXException {
        delegate.skippedEntity(name);
    }

    // ==== LexicalHandler

    /** {@inheritDoc} */
    @Override
    public void startDTD(String name, String publicId, String systemId) throws SAXException {
        if (lexicalHandler != null) {
            lexicalHandler.startDTD(name, publicId, systemId);
        }

    }

    /** {@inheritDoc} */
    @Override
    public void endDTD() throws SAXException {
        if (lexicalHandler != null) {
            lexicalHandler.endDTD();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startEntity(String name) throws SAXException {
        if (lexicalHandler != null) {
            lexicalHandler.startEntity(name);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endEntity(String name) throws SAXException {
        if (lexicalHandler != null) {
            lexicalHandler.endEntity(name);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startCDATA() throws SAXException {
        if (lexicalHandler != null) {
            lexicalHandler.startCDATA();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endCDATA() throws SAXException {
        if (lexicalHandler != null) {
            lexicalHandler.endCDATA();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void comment(char[] ch, int start, int length) throws SAXException {
        if (lexicalHandler != null) {
            lexicalHandler.comment(ch, start, length);
        }
    }

    // ==== ErrorHandler

    /** {@inheritDoc} */
    @Override
    public void warning(SAXParseException exception) throws SAXException {
        if (errorHandler != null) {
            errorHandler.warning(exception);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void error(SAXParseException exception) throws SAXException {
        if (errorHandler != null) {
            errorHandler.error(exception);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        if (errorHandler != null) {
            errorHandler.fatalError(exception);
        }
    }

}
