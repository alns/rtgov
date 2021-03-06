/*
 * 2012-3 Red Hat Inc. and/or its affiliates and other contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.overlord.rtgov.common.service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.codehaus.jackson.annotate.JsonTypeInfo;

/**
 * This abstract class defines a service that is available to
 * other runtime governance components.
 *
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class Service {
    
    /**
     * This method initializes the service.
     * 
     * @throws Exception Failed to initialize
     */
    @PostConstruct
    public void init() throws Exception {
    }
    
    /**
     * This method closes the service.
     * 
     * @throws Exception Failed to close
     */
    @PreDestroy
    public void close() throws Exception {
    }
    
}
