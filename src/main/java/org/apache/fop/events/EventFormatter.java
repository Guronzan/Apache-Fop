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

/* $Id: EventFormatter.java 932519 2010-04-09 17:22:31Z vhennebert $ */

package org.apache.fop.events;

import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.util.XMLResourceBundle;
import org.apache.fop.util.text.AdvancedMessageFormat;
import org.apache.fop.util.text.AdvancedMessageFormat.Part;
import org.apache.fop.util.text.AdvancedMessageFormat.PartFactory;

/**
 * Converts events into human-readable, localized messages.
 */
@Slf4j
public final class EventFormatter {

    private static final Pattern INCLUDES_PATTERN = Pattern
            .compile("\\{\\{.+\\}\\}");

    private EventFormatter() {
        // utility class
    }

    /**
     * Formats an event using the default locale.
     *
     * @param event
     *            the event
     * @return the formatted message
     */
    public static String format(final Event event) {
        ResourceBundle bundle = null;
        final String groupID = event.getEventGroupID();
        if (groupID != null) {
            try {
                bundle = XMLResourceBundle.getXMLBundle(groupID,
                        EventFormatter.class.getClassLoader());
            } catch (final MissingResourceException mre) {
                throw new IllegalStateException("No XMLResourceBundle for "
                        + groupID + " available.");
            }
        }
        return format(event, bundle);
    }

    /**
     * Formats an event using a given locale.
     *
     * @param event
     *            the event
     * @param locale
     *            the locale
     * @return the formatted message
     */
    public static String format(final Event event, final Locale locale) {
        ResourceBundle bundle = null;
        final String groupID = event.getEventGroupID();
        if (groupID != null) {
            try {
                bundle = XMLResourceBundle.getXMLBundle(groupID, locale,
                        EventFormatter.class.getClassLoader());
            } catch (final MissingResourceException mre) {
                if (log.isTraceEnabled()) {
                    log.trace("No XMLResourceBundle for " + groupID
                            + " available.");
                }
            }
        }
        if (bundle == null) {
            bundle = XMLResourceBundle.getXMLBundle(
                    EventFormatter.class.getName(), locale,
                    EventFormatter.class.getClassLoader());
        }
        return format(event, bundle);
    }

    private static String format(final Event event, final ResourceBundle bundle) {
        final String template = bundle.getString(event.getEventKey());
        return format(event, processIncludes(template, bundle));
    }

    private static String processIncludes(final String template,
            final ResourceBundle bundle) {
        CharSequence input = template;
        int replacements;
        StringBuffer sb;
        do {
            sb = new StringBuffer(Math.max(16, input.length()));
            replacements = processIncludesInner(input, sb, bundle);
            input = sb;
        } while (replacements > 0);
        final String s = sb.toString();
        return s;
    }

    private static int processIncludesInner(final CharSequence template,
            final StringBuffer sb, final ResourceBundle bundle) {
        int replacements = 0;
        final Matcher m = INCLUDES_PATTERN.matcher(template);
        while (m.find()) {
            String include = m.group();
            include = include.substring(2, include.length() - 2);
            m.appendReplacement(sb, bundle.getString(include));
            replacements++;
        }
        m.appendTail(sb);
        return replacements;
    }

    /**
     * Formats the event using a given pattern. The pattern needs to be
     * compatible with {@link AdvancedMessageFormat}.
     *
     * @param event
     *            the event
     * @param pattern
     *            the pattern (compatible with {@link AdvancedMessageFormat})
     * @return the formatted message
     */
    public static String format(final Event event, final String pattern) {
        final AdvancedMessageFormat format = new AdvancedMessageFormat(pattern);
        final Map<String, Object> params = new java.util.HashMap<>(
                event.getParams());
        params.put("source", event.getSource());
        params.put("severity", event.getSeverity());
        return format.format(params);
    }

    private static class LookupFieldPart implements Part {

        private final String fieldName;

        public LookupFieldPart(final String fieldName) {
            this.fieldName = fieldName;
        }

        @Override
        public boolean isGenerated(final Map<String, Object> params) {
            return getKey(params) != null;
        }

        @Override
        public void write(final StringBuilder sb, final Map params) {
            // TODO there's no defaultBundle anymore
            // sb.append(defaultBundle.getString(getKey(params)));
        }

        private Object getKey(final Map<String, Object> params) {
            return params.get(this.fieldName);
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "{" + this.fieldName + ", lookup}";
        }

    }

    /** PartFactory for lookups. */
    public static class LookupFieldPartFactory implements PartFactory {

        /** {@inheritDoc} */
        @Override
        public Part newPart(final String fieldName, final String values) {
            return new LookupFieldPart(fieldName);
        }

        /** {@inheritDoc} */
        @Override
        public String getFormat() {
            return "lookup";
        }

    }

}
