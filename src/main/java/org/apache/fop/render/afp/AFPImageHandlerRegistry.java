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

/* $Id: AFPImageHandlerRegistry.java 721430 2008-11-28 11:13:12Z acumiskey $ */

package org.apache.fop.render.afp;

import org.apache.fop.render.AbstractImageHandlerRegistry;

/**
 * This class holds references to various image handlers used by the AFP
 * renderer. It also supports automatic discovery of additional handlers
 * available through the class path.
 */
public class AFPImageHandlerRegistry extends AbstractImageHandlerRegistry {

    /**
     * Main constructor
     */
    public AFPImageHandlerRegistry() {
    }

    /** {@inheritDoc} */
    @Override
    public Class getHandlerClass() {
        return AFPImageHandler.class;
    }

}
