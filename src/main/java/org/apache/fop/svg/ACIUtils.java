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

/* $Id: ACIUtils.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.svg;

import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.CharacterIterator;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;

/**
 * Utilities for java.text.AttributedCharacterIterator.
 */
@Slf4j
public final class ACIUtils {

    private ACIUtils() {
        // This class shouldn't be instantiated.
    }

    /**
     * Dumps the contents of an ACI to System.out. Used for debugging only.
     *
     * @param aci
     *            the ACI to dump
     */
    public static void dumpAttrs(final AttributedCharacterIterator aci) {
        aci.first();
        for (final Entry<Attribute, Object> entry : aci.getAttributes()
                .entrySet()) {
            if (entry.getValue() != null) {
                log.info("{}: {}", entry.getKey(), entry.getValue());
            }
        }
        int start = aci.getBeginIndex();
        log.info("AttrRuns: ");
        while (aci.current() != CharacterIterator.DONE) {
            final int end = aci.getRunLimit();
            log.info("" + (end - start) + ", ");
            aci.setIndex(end);
            if (start == end) {
                break;
            }
            start = end;
        }
        log.info("");
    }

}
