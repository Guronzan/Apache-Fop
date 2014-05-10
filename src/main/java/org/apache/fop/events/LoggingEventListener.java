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

/* $Id: LoggingEventListener.java 1301445 2012-03-16 11:44:09Z mehdi $ */

package org.apache.fop.events;

import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.events.model.EventSeverity;

/**
 * EventListener implementation that redirects events to Commons Logging. The
 * events are converted to localized messages.
 */
@Slf4j
 public class LoggingEventListener implements EventListener {

    private final boolean skipFatal;

     private final Set<String> loggedMessages = new HashSet<String>();

    /**
      * Creates an instance logging to a given logger. Events with fatal severity
     * level will be skipped.
     * 
     * @param log
     *            the target logger
      */
     public LoggingEventListener() {
         this(true);
     }

     /**
      * Creates an instance logging to a given logger.
      * 
     * @param log
     *            the target logger
     * @param skipFatal
     *            true if events with fatal severity level should be skipped
     *            (i.e. not logged)
      */
     public LoggingEventListener(final boolean skipFatal) {
         this.skipFatal = skipFatal;
     }

     /** {@inheritDoc} */
     @Override
     public void processEvent(final Event event) {
         final String msg = EventFormatter.format(event);
         final EventSeverity severity = event.getSeverity();
         if (severity == EventSeverity.INFO) {
             log.info(msg);
         } else if (severity == EventSeverity.WARN) {
             // we want to prevent logging of duplicate messages in situations
            // where they are likely
             // to occur; for instance, warning related to layout do not repeat
            // (since line number
             // will be different) and as such we do not try to filter them here;
            // on the other hand,
             // font related warnings are very likely to repeat and we try to
            // filter them out here;
             // the same may happen with missing images (but not implemented
            // yet).
             final String eventGroupID = event.getEventGroupID();
             if (eventGroupID.equals("org.apache.fop.fonts.FontEventProducer")) {
                 if (!this.loggedMessages.contains(msg)) {
                     this.loggedMessages.add(msg);
                     log.warn(msg);
                 }
             } else {
                 log.warn(msg);
             }
         } else if (severity == EventSeverity.ERROR) {
             if (event.getParam("e") != null) {
                 log.error(msg, (Throwable) event.getParam("e"));
             } else {
                 log.error(msg);
             }
         } else if (severity == EventSeverity.FATAL) {
             if (!this.skipFatal) {
                 if (event.getParam("e") != null) {
                     log.error(msg, (Throwable) event.getParam("e"));
                 } else {
                     log.error(msg);
                 }
             }
         } else {
             assert false;
         }
     }
 }
