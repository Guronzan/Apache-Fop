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

/* $Id: FunctionBase.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fo.expr;

import org.apache.fop.datatypes.PercentBase;

/**
 * Abstract Base class for XSL-FO functions
 */
public abstract class FunctionBase implements Function {

    /**
     * @return null (by default, functions have no percent-based arguments)
     */
    @Override
    public PercentBase getPercentBase() {
        return null;
    }

    /**
     * @return false (by default don't pad arglist with property-name)
     */
    @Override
    public boolean padArgsWithPropertyName() {
        return false;
    }
}
