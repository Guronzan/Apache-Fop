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

/* $Id: BatikUtil.java 724310 2008-12-08 11:30:11Z jeremias $ */

package org.apache.fop.image.loader.batik;

import lombok.extern.slf4j.Slf4j;

import org.apache.batik.dom.AbstractDocument;
import org.apache.batik.dom.util.DOMUtilities;
import org.w3c.dom.Document;

/**
 * Helper utilities for Apache Batik.
 */
@Slf4j
public class BatikUtil {

    /**
     * Checks whether Apache Batik is available in the classpath.
     *
     * @return true if Apache Batik is available
     */
    public static boolean isBatikAvailable() {
        try {
            Class.forName("org.apache.batik.dom.svg.SVGDOMImplementation");
            return true;
        } catch (final Exception e) {
            log.error("Exception", e);
        }
        return false;
    }

    /**
     * Clones an SVG DOM document. This is used for making SVG production
     * thread-safe when the SVG document is cached and re-used.
     *
     * @param doc
     *            the SVG DOM to be cloned
     * @return the cloned SVG DOM
     */
    public static Document cloneSVGDocument(final Document doc) {
        final Document clonedDoc = DOMUtilities.deepCloneDocument(doc,
                doc.getImplementation());
        if (clonedDoc instanceof AbstractDocument) {
            // TODO a retablir
            // ((AbstractDocument) clonedDoc)
            // .setDocumentURI(((AbstractDocument) doc)
            // .getDocumentElement());
        }
        return clonedDoc;
    }

}
