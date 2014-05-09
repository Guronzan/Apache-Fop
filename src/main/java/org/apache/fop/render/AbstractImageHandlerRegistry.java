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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
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

    private static final Comparator<ImageHandlerBase> HANDLER_COMPARATOR = new Comparator<ImageHandlerBase>() {
        @Override
        public int compare(final ImageHandlerBase h1, final ImageHandlerBase h2) {
            return h1.getPriority() - h2.getPriority();
        }
    };

    /** Map containing image handlers for various MIME types */
    private final Map<Class, ImageHandlerBase> handlers = new HashMap<>();

    /** List containing the same handlers as above but ordered by priority */
    private final List<ImageHandlerBase> handlerList = new LinkedList<>();

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
        final ListIterator<ImageHandlerBase> iter = this.handlerList
                .listIterator();
        while (iter.hasNext()) {
            final ImageHandlerBase h = iter.next();
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
            handler = this.handlers.get(cl);
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
            final List<ImageFlavor> flavors = new ArrayList<>();
            final Iterator<ImageHandlerBase> iter = this.handlerList.iterator();
            while (iter.hasNext()) {
                final ImageFlavor[] f = iter.next().getSupportedImageFlavors();
                for (final ImageFlavor element : f) {
                    flavors.add(element);
                }
            }
            this.supportedFlavors = flavors.toArray(new ImageFlavor[flavors
                                                                    .size()]);
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
        final Class<? extends ImageHandlerBase> imageHandlerClass = getHandlerClass();
        final Iterator<ImageHandlerBase> providers = Service
                .providers(imageHandlerClass);
        if (providers != null) {
            while (providers.hasNext()) {
                final ImageHandlerBase handler = providers.next();
                try {
                    log.debug("Dynamically adding ImageHandler: {}", handler
                            .getClass().getName());
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
    public Comparator<ImageHandlerBase> getHandlerComparator() {
        return HANDLER_COMPARATOR;
    }

    /**
     * Returns the ImageHandler implementing class
     *
     * @return the ImageHandler implementing class
     */
    public abstract Class<? extends ImageHandlerBase> getHandlerClass();
}
