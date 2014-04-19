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

/* $Id: DefaultErrorListener.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.util;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import lombok.extern.slf4j.Slf4j;

/**
 * Standard ErrorListener implementation for in-FOP use. Some Xalan-J versions
 * don't properly re-throw exceptions.
 */
@Slf4j
public class DefaultErrorListener implements ErrorListener {

    /**
     * Main constructor
     *
     * @param log
     *            the log instance to send log events to
     */
    public DefaultErrorListener() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void warning(final TransformerException exc) {
        log.warn(exc.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void error(final TransformerException exc)
            throws TransformerException {
        throw exc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fatalError(final TransformerException exc)
            throws TransformerException {
        throw exc;
    }

}