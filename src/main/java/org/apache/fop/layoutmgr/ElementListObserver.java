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

/* $Id: ElementListObserver.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.layoutmgr;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to observe Knuth element lists generated within the layout
 * managers. This is mainly used for the purpose of automated testing. This
 * implementation here does nothing. Please see the subclass within the test
 * code.
 */
public class ElementListObserver {

    private static List<Observer> activeObservers = null;

    /**
     * Adds a new Observer to the list.
     *
     * @param observer
     *            the observer implementation
     */
    public static void addObserver(final Observer observer) {
        if (!isObservationActive()) {
            activeObservers = new ArrayList<>();
        }
        activeObservers.add(observer);
    }

    /**
     * Removes an Observer from the list. This call simply returns if the
     * observer was not on the list and does nothing.
     *
     * @param observer
     *            the observer to remove
     */
    public static void removeObserver(final Observer observer) {
        if (isObservationActive()) {
            activeObservers.remove(observer);
        }
    }

    /**
     * Notifies all registered observers about the element list.
     *
     * @param elementList
     *            the Knuth element list
     * @param category
     *            the category for the element list (example: main,
     *            static-content, table-cell)
     * @param id
     *            ID for the element list (may be null)
     */
    public static void observe(final List<ListElement> elementList,
            final String category, final String id) {
        if (isObservationActive()) {
            if (category == null) {
                throw new NullPointerException("category must not be null");
            }
            for (final Observer observer : activeObservers) {
                observer.observe(elementList, category, id);
            }
        }
    }

    /** @return true if observation is active, i.e. Observers are registered. */
    public static boolean isObservationActive() {
        return activeObservers != null;
    }

    /**
     * Implement this interface to receive notifications on element lists.
     */
    public interface Observer {

        /**
         * Notifies the observer about the element list.
         *
         * @param elementList
         *            the Knuth element list
         * @param category
         *            the category for the element list (example: main,
         *            static-content or table-cell)
         * @param id
         *            ID for the element list (may be null)
         */
        void observe(final List<ListElement> elementList,
                final String category, final String id);

    }

}
