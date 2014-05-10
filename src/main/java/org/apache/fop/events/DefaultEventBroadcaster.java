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

/* $Id: DefaultEventBroadcaster.java 932510 2010-04-09 17:05:34Z vhennebert $ */

package org.apache.fop.events;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;

import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.fop.events.model.EventMethodModel;
import org.apache.fop.events.model.EventModel;
import org.apache.fop.events.model.EventModelParser;
import org.apache.fop.events.model.EventProducerModel;
import org.apache.fop.events.model.EventSeverity;

/**
 * Default implementation of the EventBroadcaster interface. It holds a list of
 * event listeners and can provide {@link EventProducer} instances for type-safe
 * event production.
 */
public class DefaultEventBroadcaster implements EventBroadcaster {

    /** Holds all registered event listeners */
    protected CompositeEventListener listeners = new CompositeEventListener();

    /** {@inheritDoc} */
    @Override
    public void addEventListener(final EventListener listener) {
        this.listeners.addEventListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void removeEventListener(final EventListener listener) {
        this.listeners.removeEventListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasEventListeners() {
        return this.listeners.hasEventListeners();
    }

    /** {@inheritDoc} */
    @Override
    public void broadcastEvent(final Event event) {
        this.listeners.processEvent(event);
    }

    private static List<EventModel> eventModels = new ArrayList<>();
    private final Map<Class, EventProducer> proxies = new HashMap<>();

    /**
     * Loads an event model and returns its instance.
     *
     * @param resourceBaseClass
     *            base class to use for loading resources
     * @return the newly loaded event model.
     */
    private static EventModel loadModel(final Class resourceBaseClass) {
        final String resourceName = "event-model.xml";
        final InputStream in = resourceBaseClass
                .getResourceAsStream(resourceName);
        if (in == null) {
            throw new MissingResourceException("File " + resourceName
                    + " not found", DefaultEventBroadcaster.class.getName(), "");
        }
        try {
            return EventModelParser.parse(new StreamSource(in));
        } catch (final TransformerException e) {
            throw new MissingResourceException("Error reading " + resourceName
                    + ": " + e.getMessage(),
                    DefaultEventBroadcaster.class.getName(), "");
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Adds a new {@link EventModel} to the list of registered event models.
     *
     * @param eventModel
     *            the event model instance
     */
    public static synchronized void addEventModel(final EventModel eventModel) {
        eventModels.add(eventModel);
    }

    private static synchronized EventProducerModel getEventProducerModel(
            final Class clazz) {
        for (int i = 0, c = eventModels.size(); i < c; ++i) {
            final EventModel eventModel = eventModels.get(i);
            final EventProducerModel producerModel = eventModel
                    .getProducer(clazz);
            if (producerModel != null) {
                return producerModel;
            }
        }
        final EventModel model = loadModel(clazz);
        addEventModel(model);
        return model.getProducer(clazz);
    }

    /** {@inheritDoc} */
    @Override
    public EventProducer getEventProducerFor(final Class clazz) {
        if (!EventProducer.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException(
                    "Class must be an implementation of the EventProducer interface: "
                            + clazz.getName());
        }
        EventProducer producer;
        producer = this.proxies.get(clazz);
        if (producer == null) {
            producer = createProxyFor(clazz);
            this.proxies.put(clazz, producer);
        }
        return producer;
    }

    /**
     * Creates a dynamic proxy for the given EventProducer interface that will
     * handle the conversion of the method call into the broadcasting of an
     * event instance.
     *
     * @param clazz
     *            a descendant interface of EventProducer
     * @return the EventProducer instance
     */
    protected EventProducer createProxyFor(final Class clazz) {
        final EventProducerModel producerModel = getEventProducerModel(clazz);
        if (producerModel == null) {
            throw new IllegalStateException(
                    "Event model doesn't contain the definition for "
                            + clazz.getName());
        }
        return (EventProducer) Proxy.newProxyInstance(clazz.getClassLoader(),
                new Class[] { clazz }, new InvocationHandler() {
            @Override
            public Object invoke(final Object proxy,
                            final Method method, final Object[] args)
                            throws Throwable {
                final String methodName = method.getName();
                final EventMethodModel methodModel = producerModel
                                .getMethod(methodName);
                final String eventID = producerModel.getInterfaceName()
                                + "." + methodName;
                if (methodModel == null) {
                    throw new IllegalStateException(
                            "Event model isn't consistent"
                                            + " with the EventProducer interface. Please rebuild FOP!"
                                            + " Affected method: " + eventID);
                }
                final Map params = new HashMap<>();
                int i = 1;
                final Iterator iter = methodModel.getParameters()
                                .iterator();
                while (iter.hasNext()) {
                    final EventMethodModel.Parameter param = (EventMethodModel.Parameter) iter
                                    .next();
                    params.put(param.getName(), args[i]);
                    i++;
                }
                final Event ev = new Event(args[0], eventID,
                                methodModel.getSeverity(), params);
                broadcastEvent(ev);

                if (ev.getSeverity() == EventSeverity.FATAL) {
                    EventExceptionManager.throwException(ev,
                            methodModel.getExceptionClass());
                }
                return null;
            }
        });
    }

}
