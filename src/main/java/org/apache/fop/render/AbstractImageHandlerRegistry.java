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

/* $Id: AbstractImageHandlerRegistry.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.render;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.util.Service;

/**
 * This class holds references to various image handlers used by the renderers.
 * It also supports automatic discovery of additional handlers available through
 * the class path.
 */
@Slf4j
public abstract class AbstractImageHandlerRegistry {

    private static final Comparator HANDLER_COMPARATOR = new Comparator() {
        @Override
        public int compare(final Object o1, final Object o2) {
            final ImageHandlerBase h1 = (ImageHandlerBase) o1;
            final ImageHandlerBase h2 = (ImageHandlerBase) o2;
            return h1.getPriority() - h2.getPriority();
        }
    };

    /** Map containing image handlers for various MIME types */
    private final Map/* <Class, ImageHandler> */handlers = new java.util.HashMap/*
     * <
     * Class
     * ,
     * ImageHandler
     * >
     */();

    /** List containing the same handlers as above but ordered by priority */
    private final List/* <ImageHandler> */handlerList = new java.util.LinkedList/*
     * <
     * ImageHandler
     * >
     */();

    /** Sorted Set of registered handlers */
    private ImageFlavor[] supportedFlavors = new ImageFlavor[0];

    private int handlerRegistrations;
    private int lastSync;

    /**
     * Default constructor.
     */
    public AbstractImageHandlerRegistry() {
        discoverHandlers();
    }

    /**
     * Add an ImageHandler. The handler itself is inspected to find out what it
     * supports.
     *
     * @param classname
     *            the fully qualified class name
     */
    public void addHandler(final String classname) {
        try {
            final ImageHandlerBase handlerInstance = (ImageHandlerBase) Class
                    .forName(classname).newInstance();
            addHandler(handlerInstance);
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find " + classname);
        } catch (final InstantiationException e) {
            throw new IllegalArgumentException("Could not instantiate "
                    + classname);
        } catch (final IllegalAccessException e) {
            throw new IllegalArgumentException("Could not access " + classname);
        } catch (final ClassCastException e) {
            throw new IllegalArgumentException(classname + " is not an "
                    + getHandlerClass().getName());
        }
    }

    /**
     * Add an image handler. The handler itself is inspected to find out what it
     * supports.
     *
     * @param handler
     *            the ImageHandler instance
     */
    public synchronized void addHandler(final ImageHandlerBase handler) {
        this.handlers.put(handler.getSupportedImageClass(), handler);

        // Sorted insert
        final ListIterator iter = this.handlerList.listIterator();
        while (iter.hasNext()) {
            final ImageHandlerBase h = (ImageHandlerBase) iter.next();
            if (getHandlerComparator().compare(handler, h) < 0) {
                iter.previous();
                break;
            }
        }
        iter.add(handler);
        this.handlerRegistrations++;
    }

    /**
     * Returns an ImageHandler which handles an specific image type given the
     * MIME type of the image.
     *
     * @param img
     *            the Image to be handled
     * @return the ImageHandler responsible for handling the image or null if
     *         none is available
     */
    public ImageHandlerBase getHandler(final Image img) {
        return getHandler(img.getClass());
    }

    /**
     * Returns an ImageHandler which handles an specific image type given the
     * MIME type of the image.
     *
     * @param imageClass
     *            the Image subclass for which to get a handler
     * @return the ImageHandler responsible for handling the image or null if
     *         none is available
     */
    public synchronized ImageHandlerBase getHandler(final Class imageClass) {
        ImageHandlerBase handler = null;
        Class cl = imageClass;
        while (cl != null) {
            handler = (ImageHandlerBase) this.handlers.get(cl);
            if (handler != null) {
                break;
            }
            cl = cl.getSuperclass();
        }
        return handler;
    }

    /**
     * Returns the ordered array of supported image flavors.
     *
     * @return the array of image flavors
     */
    public synchronized ImageFlavor[] getSupportedFlavors() {
        if (this.lastSync != this.handlerRegistrations) {
            // Extract all ImageFlavors into a single array
            final List flavors = new java.util.ArrayList();
            final Iterator iter = this.handlerList.iterator();
            while (iter.hasNext()) {
                final ImageFlavor[] f = ((ImageHandlerBase) iter.next())
                        .getSupportedImageFlavors();
                for (final ImageFlavor element : f) {
                    flavors.add(element);
                }
            }
            this.supportedFlavors = (ImageFlavor[]) flavors
                    .toArray(new ImageFlavor[flavors.size()]);
            this.lastSync = this.handlerRegistrations;
        }
        return this.supportedFlavors;
    }

    /**
     * Discovers ImageHandler implementations through the classpath and
     * dynamically registers them.
     */
    private void discoverHandlers() {
        // add mappings from available services
        final Class imageHandlerClass = getHandlerClass();
        final Iterator providers = Service.providers(imageHandlerClass);
        if (providers != null) {
            while (providers.hasNext()) {
                final ImageHandlerBase handler = (ImageHandlerBase) providers
                        .next();
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Dynamically adding ImageHandler: "
                                + handler.getClass().getName());
                    }
                    addHandler(handler);
                } catch (final IllegalArgumentException e) {
                    log.error("Error while adding ImageHandler", e);
                }

            }
        }
    }

    /**
     * Returns the ImageHandler comparator
     *
     * @return the ImageHandler comparator
     */
    public Comparator getHandlerComparator() {
        return HANDLER_COMPARATOR;
    }

    /**
     * Returns the ImageHandler implementing class
     *
     * @return the ImageHandler implementing class
     */
    public abstract Class getHandlerClass();
}
