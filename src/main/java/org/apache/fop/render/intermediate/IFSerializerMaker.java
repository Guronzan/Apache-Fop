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

/* $Id: IFSerializerMaker.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.render.intermediate;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.MimeConstants;

/**
 * Intermediate format document handler factory for intermediate format XML
 * output.
 */
public class IFSerializerMaker extends AbstractIFDocumentHandlerMaker {

    /** {@inheritDoc} */
    @Override
    public IFDocumentHandler makeIFDocumentHandler(final FOUserAgent ua) {
        final IFSerializer handler = new IFSerializer();
        handler.setContext(new IFContext(ua));
        return handler;
    }

    /** {@inheritDoc} */
    @Override
    public boolean needsOutputStream() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getSupportedMimeTypes() {
        return new String[] { MimeConstants.MIME_FOP_IF };
    }

}
