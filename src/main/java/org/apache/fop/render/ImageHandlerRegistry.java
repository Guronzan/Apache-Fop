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

/* $Id: ImageHandlerRegistry.java 820672 2009-10-01 14:48:27Z jeremias $ */

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
 * This class holds references to various image handlers. It also supports
 * automatic discovery of additional handlers available through the class path.
 */
@Slf4j
public class ImageHandlerRegistry {

    private static final Comparator HANDLER_COMPARATOR = new Comparator() {
        @Override
        public int compare(final Object o1, final Object o2) {
            final ImageHandler h1 = (ImageHandler) o1;
            final ImageHandler h2 = (ImageHandler) o2;
            return h1.getPriority() - h2.getPriority();
        }
    };

    /** Map containing image handlers for various {@link Image} subclasses. */
    private final Map handlers = new java.util.HashMap<>();
    /** List containing the same handlers as above but ordered by priority */
    private final List handlerList = new java.util.LinkedList<>();

    private int handlerRegistrations;

    /**
     * Default constructor.
     */
    public ImageHandlerRegistry() {
        discoverHandlers();
    }

    /**
     * Add an PDFImageHandler. The handler itself is inspected to find out what
     * it supports.
     *
     * @param classname
     *            the fully qualified class name
     */
    public void addHandler(final String classname) {
        try {
            final ImageHandler handlerInstance = (ImageHandler) Class.forName(
                    classname).newInstance();
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
                    + ImageHandler.class.getName());
        }
    }

    /**
     * Add an image handler. The handler itself is inspected to find out what it
     * supports.
     *
     * @param handler
     *            the ImageHandler instance
     */
    public synchronized void addHandler(final ImageHandler handler) {
        final Class imageClass = handler.getSupportedImageClass();
        // List
        this.handlers.put(imageClass, handler);

        // Sorted insert (sort by priority)
        final ListIterator iter = this.handlerList.listIterator();
        while (iter.hasNext()) {
            final ImageHandler h = (ImageHandler) iter.next();
            if (HANDLER_COMPARATOR.compare(handler, h) < 0) {
                iter.previous();
                break;
            }
        }
        iter.add(handler);
        this.handlerRegistrations++;
    }

    /**
     * Returns an {@link ImageHandler} which handles an specific image type
     * given the MIME type of the image.
     *
     * @param targetContext
     *            the target rendering context that is used for identifying
     *            compatibility
     * @param image
     *            the Image to be handled
     * @return the image handler responsible for handling the image or null if
     *         none is available
     */
    public ImageHandler getHandler(final RenderingContext targetContext,
            final Image image) {
        final ListIterator iter = this.handlerList.listIterator();
        while (iter.hasNext()) {
            final ImageHandler h = (ImageHandler) iter.next();
            if (h.isCompatible(targetContext, image)) {
                // Return the first handler in the prioritized list that is
                // compatible
                return h;
            }
        }
        return null;
    }

    /**
     * Returns the ordered array of supported image flavors. The array needs to
     * be ordered by priority so the image loader framework can return the
     * preferred image type.
     *
     * @return the array of image flavors
     */
    public synchronized ImageFlavor[] getSupportedFlavors(
            final RenderingContext context) {
        // Extract all ImageFlavors into a single array
        final List flavors = new java.util.ArrayList();
        final Iterator iter = this.handlerList.iterator();
        while (iter.hasNext()) {
            final ImageHandler handler = (ImageHandler) iter.next();
            if (handler.isCompatible(context, null)) {
                final ImageFlavor[] f = handler.getSupportedImageFlavors();
                for (final ImageFlavor element : f) {
                    flavors.add(element);
                }
            }
        }
        return (ImageFlavor[]) flavors.toArray(new ImageFlavor[flavors.size()]);
    }

    /**
     * Discovers ImageHandler implementations through the classpath and
     * dynamically registers them.
     */
    private void discoverHandlers() {
        // add mappings from available services
        final Iterator providers = Service.providers(ImageHandler.class);
        if (providers != null) {
            while (providers.hasNext()) {
                final ImageHandler handler = (ImageHandler) providers.next();
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
}
