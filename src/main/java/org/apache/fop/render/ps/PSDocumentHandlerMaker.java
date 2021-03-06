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

/* $Id: PSDocumentHandlerMaker.java 747010 2009-02-23 13:25:08Z jeremias $ */

package org.apache.fop.render.ps;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.render.intermediate.AbstractIFDocumentHandlerMaker;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFDocumentHandler;

/**
 * Intermediate format document handler factory for PostScript output.
 */
public class PSDocumentHandlerMaker extends AbstractIFDocumentHandlerMaker {

    private static final String[] MIMES = new String[] { MimeConstants.MIME_POSTSCRIPT };

    /** {@inheritDoc} */
    @Override
    public IFDocumentHandler makeIFDocumentHandler(final FOUserAgent ua) {
        final PSDocumentHandler handler = new PSDocumentHandler();
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
        return MIMES;
    }

}
