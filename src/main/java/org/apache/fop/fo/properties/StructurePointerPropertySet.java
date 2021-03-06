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

/* $Id: StructurePointerPropertySet.java 830293 2009-10-27 19:07:52Z vhennebert $ */

package org.apache.fop.fo.properties;

/**
 * Defines property access methods for internal structure pointer extension
 * properties.
 */
public interface StructurePointerPropertySet {

    /**
     * Returns the value of the "foi:ptr" property, the internal structure
     * pointer used for tagged PDF and other formats that support a structure
     * tree in addition to paged content.
     * 
     * @return the "foi:ptr" property
     */
    String getPtr();

}
