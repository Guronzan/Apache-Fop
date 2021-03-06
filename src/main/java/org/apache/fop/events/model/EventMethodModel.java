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

/* $Id: EventMethodModel.java 676307 2008-07-13 12:11:17Z jeremias $ */

package org.apache.fop.events.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.xmlgraphics.util.XMLizable;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Represents an event method. Each method in an event producer interface will
 * result in one instance of <code>EventMethodModel</code>.
 */
public class EventMethodModel implements Serializable, XMLizable {

    private static final long serialVersionUID = -7548882973341444354L;

    private String methodName;
    private EventSeverity severity;
    private final List<Parameter> params = new ArrayList<>();
    private String exceptionClass;

    /**
     * Creates an new instance.
     *
     * @param methodName
     *            the event method's name
     * @param severity
     *            the event severity
     */
    public EventMethodModel(final String methodName,
            final EventSeverity severity) {
        this.methodName = methodName;
        this.severity = severity;
    }

    /**
     * Adds a method parameter.
     *
     * @param param
     *            the method parameter
     */
    public void addParameter(final Parameter param) {
        this.params.add(param);
    }

    /**
     * Adds a method parameter.
     *
     * @param type
     *            the type of the parameter
     * @param name
     *            the name of the parameter
     * @return the resulting Parameter instance
     */
    public Parameter addParameter(final Class type, final String name) {
        final Parameter param = new Parameter(type, name);
        addParameter(param);
        return param;
    }

    /**
     * Sets the event method name.
     *
     * @param name
     *            the event name
     */
    public void setMethodName(final String name) {
        this.methodName = name;
    }

    /**
     * Returns the event method name
     *
     * @return the event name
     */
    public String getMethodName() {
        return this.methodName;
    }

    /**
     * Sets the event's severity level.
     *
     * @param severity
     *            the severity
     */
    public void setSeverity(final EventSeverity severity) {
        this.severity = severity;
    }

    /**
     * Returns the event's severity level.
     *
     * @return the severity
     */
    public EventSeverity getSeverity() {
        return this.severity;
    }

    /**
     * Returns an unmodifiable list of parameters for this event method.
     *
     * @return the list of parameters
     */
    public List<Parameter> getParameters() {
        return Collections.unmodifiableList(this.params);
    }

    /**
     * Sets the primary exception class for this event method. Note: Not all
     * event methods throw exceptions!
     *
     * @param exceptionClass
     *            the exception class
     */
    public void setExceptionClass(final String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    /**
     * Returns the primary exception class for this event method. This method
     * returns null if the event is only informational or just a warning.
     *
     * @return the primary exception class or null
     */
    public String getExceptionClass() {
        return this.exceptionClass;
    }

    /** {@inheritDoc} */
    @Override
    public void toSAX(final ContentHandler handler) throws SAXException {
        final AttributesImpl atts = new AttributesImpl();
        atts.addAttribute("", "name", "name", "CDATA", getMethodName());
        atts.addAttribute("", "severity", "severity", "CDATA", getSeverity()
                .getName());
        if (getExceptionClass() != null) {
            atts.addAttribute("", "exception", "exception", "CDATA",
                    getExceptionClass());
        }
        final String elName = "method";
        handler.startElement("", elName, elName, atts);
        for (final Parameter param : this.params) {
            ((XMLizable) param).toSAX(handler);
        }
        handler.endElement("", elName, elName);
    }

}
