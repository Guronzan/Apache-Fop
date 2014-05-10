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

/* $Id: AFPElementMapping.java 1055034 2011-01-04 13:36:10Z spepping $ */

package org.apache.fop.render.afp.extensions;

import java.util.HashMap;

import org.apache.fop.fo.ElementMapping;
import org.apache.fop.fo.FONode;

/**
 * AFPElementMapping object provides the ability to extract information from the
 * formatted object that reside in the afp namespace. This is used for custom
 * AFP extensions not supported by the FO schema. Examples include adding
 * overlays or indexing a document using the tag logical element structured
 * field.
 * <p/>
 */
public class AFPElementMapping extends ElementMapping {

    /** tag logical element */
    public static final String TAG_LOGICAL_ELEMENT = "tag-logical-element";

    /** include page overlay element */
    public static final String INCLUDE_PAGE_OVERLAY = "include-page-overlay";

    /** include page segment element */
    public static final String INCLUDE_PAGE_SEGMENT = "include-page-segment";

    /** include form map element */
    public static final String INCLUDE_FORM_MAP = "include-form-map";

    /** NOP */
    public static final String NO_OPERATION = "no-operation";

    /** IMM: Invoke Medium Map (on fo:page-sequence) */
    public static final String INVOKE_MEDIUM_MAP = "invoke-medium-map";

    /**
     * The namespace used for AFP extensions
     */
    public static final String NAMESPACE = "http://xmlgraphics.apache.org/fop/extensions/afp";

    /**
     * The usual namespace prefix used for AFP extensions
     */
    public static final String NAMESPACE_PREFIX = "afp";

    /** Main constructor */
    public AFPElementMapping() {
        this.namespaceURI = NAMESPACE;
    }

    /**
     * Private static synchronized method to set up the element and attribute
     * HashMaps, this defines what elements and attributes are extracted.
     */
    @Override
    protected void initialize() {

        if (this.foObjs == null) {
            super.foObjs = new HashMap<>();
            this.foObjs.put(TAG_LOGICAL_ELEMENT,
                    new AFPTagLogicalElementMaker());
            this.foObjs.put(INCLUDE_PAGE_SEGMENT,
                    new AFPIncludePageSegmentMaker());
            this.foObjs.put(INCLUDE_PAGE_OVERLAY,
                    new AFPIncludePageOverlayMaker());
            this.foObjs.put(INCLUDE_FORM_MAP, new AFPIncludeFormMapMaker());
            this.foObjs.put(NO_OPERATION, new AFPNoOperationMaker());
            this.foObjs.put(INVOKE_MEDIUM_MAP, new AFPInvokeMediumMapMaker());
        }
    }

    static class AFPIncludePageOverlayMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new AFPPageOverlayElement(parent, INCLUDE_PAGE_OVERLAY);
        }
    }

    static class AFPIncludePageSegmentMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new AFPPageSegmentElement(parent, INCLUDE_PAGE_SEGMENT);
        }
    }

    static class AFPIncludeFormMapMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new AFPIncludeFormMapElement(parent, INCLUDE_FORM_MAP);
        }
    }

    static class AFPTagLogicalElementMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new AFPPageSetupElement(parent, TAG_LOGICAL_ELEMENT);
        }
    }

    static class AFPNoOperationMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new AFPPageSetupElement(parent, NO_OPERATION);
        }
    }

    static class AFPInvokeMediumMapMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new AFPInvokeMediumMapElement(parent);
        }
    }

}
