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

/* $Id: UnsupportedOperationExceptionFactory.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.events;

import java.util.Locale;

import org.apache.fop.events.EventExceptionManager.ExceptionFactory;

/**
 * Exception factory for {@link UnsupportedOperationException}.
 */
public class UnsupportedOperationExceptionFactory implements ExceptionFactory {

    /** {@inheritDoc} */
    @Override
    public Throwable createException(final Event event) {
        final String msg = EventFormatter.format(event, Locale.ENGLISH);
        final UnsupportedOperationException ex = new UnsupportedOperationException(
                msg);
        return ex;
    }

    /** {@inheritDoc} */
    @Override
    public Class getExceptionClass() {
        return UnsupportedOperationException.class;
    }

}