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

/* $Id: AFPExtensionHandler.java 896856 2010-01-07 12:07:23Z cbowditch $ */

package org.apache.fop.render.afp.extensions;

import java.net.URI;
import java.net.URISyntaxException;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.util.ContentHandlerFactory;
import org.apache.fop.util.ContentHandlerFactory.ObjectBuiltListener;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * ContentHandler (parser) for restoring AFPExtension objects from XML.
 */
@Slf4j
public class AFPExtensionHandler extends DefaultHandler implements
ContentHandlerFactory.ObjectSource {

    private final StringBuilder content = new StringBuilder();
    private Attributes lastAttributes;

    private AFPExtensionAttachment returnedObject;
    private ObjectBuiltListener listener;

    /** {@inheritDoc} */
    @Override
    public void startElement(final String uri, final String localName,
            final String qName, final Attributes attributes)
                    throws SAXException {
        boolean handled = false;
        if (AFPExtensionAttachment.CATEGORY.equals(uri)) {
            this.lastAttributes = attributes;
            handled = true;
            if (localName.equals(AFPElementMapping.NO_OPERATION)
                    || localName.equals(AFPElementMapping.TAG_LOGICAL_ELEMENT)
                    || localName.equals(AFPElementMapping.INCLUDE_PAGE_OVERLAY)
                    || localName.equals(AFPElementMapping.INCLUDE_PAGE_SEGMENT)
                    || localName.equals(AFPElementMapping.INCLUDE_FORM_MAP)
                    || localName.equals(AFPElementMapping.INVOKE_MEDIUM_MAP)) {
                // handled in endElement
            } else {
                handled = false;
            }
        }
        if (!handled) {
            if (AFPExtensionAttachment.CATEGORY.equals(uri)) {
                throw new SAXException("Unhandled element " + localName
                        + " in namespace: " + uri);
            } else {
                log.warn("Unhandled element " + localName + " in namespace: "
                        + uri);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endElement(final String uri, final String localName,
            final String qName) throws SAXException {
        if (AFPExtensionAttachment.CATEGORY.equals(uri)) {
            if (AFPElementMapping.INCLUDE_FORM_MAP.equals(localName)) {
                final AFPIncludeFormMap formMap = new AFPIncludeFormMap();
                final String name = this.lastAttributes.getValue("name");
                formMap.setName(name);
                final String src = this.lastAttributes.getValue("src");
                try {
                    formMap.setSrc(new URI(src));
                } catch (final URISyntaxException e) {
                    throw new SAXException("Invalid URI: " + src, e);
                }
                this.returnedObject = formMap;
            } else if (AFPElementMapping.INCLUDE_PAGE_OVERLAY.equals(localName)) {
                this.returnedObject = new AFPPageOverlay();
                final String name = this.lastAttributes.getValue("name");
                if (name != null) {
                    this.returnedObject.setName(name);
                }
            } else {
                AFPPageSetup pageSetupExtn = null;
                if (AFPElementMapping.INVOKE_MEDIUM_MAP.equals(localName)) {
                    this.returnedObject = new AFPInvokeMediumMap();
                } else {
                    pageSetupExtn = new AFPPageSetup(localName);
                    this.returnedObject = pageSetupExtn;
                }
                final String name = this.lastAttributes.getValue("name");
                if (name != null) {
                    this.returnedObject.setName(name);
                }
                final String value = this.lastAttributes.getValue("value");
                if (value != null && pageSetupExtn != null) {
                    pageSetupExtn.setValue(value);
                }
                if (this.content.length() > 0 && pageSetupExtn != null) {
                    pageSetupExtn.setContent(this.content.toString());
                    this.content.setLength(0); // Reset text buffer (see
                    // characters())
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        this.content.append(ch, start, length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endDocument() throws SAXException {
        if (this.listener != null) {
            this.listener.notifyObjectBuilt(getObject());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getObject() {
        return this.returnedObject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setObjectBuiltListener(final ObjectBuiltListener listen) {
        this.listener = listen;
    }

}
