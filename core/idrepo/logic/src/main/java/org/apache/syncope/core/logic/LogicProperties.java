/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.logic;

import org.apache.syncope.core.logic.init.ClassPathScanImplementationLookup;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("logic")
public class LogicProperties {

    private Class<? extends LogicInvocationHandler> invocationHandler = LogicInvocationHandler.class;

    private Class<? extends ImplementationLookup> implementationLookup = ClassPathScanImplementationLookup.class;

    private boolean enableJDBCAuditAppender = true;

    public Class<? extends LogicInvocationHandler> getInvocationHandler() {
        return invocationHandler;
    }

    public void setInvocationHandler(final Class<? extends LogicInvocationHandler> invocationHandler) {
        this.invocationHandler = invocationHandler;
    }

    public Class<? extends ImplementationLookup> getImplementationLookup() {
        return implementationLookup;
    }

    public void setImplementationLookup(final Class<? extends ImplementationLookup> implementationLookup) {
        this.implementationLookup = implementationLookup;
    }

    public boolean isEnableJDBCAuditAppender() {
        return enableJDBCAuditAppender;
    }

    public void setEnableJDBCAuditAppender(final boolean enableJDBCAuditAppender) {
        this.enableJDBCAuditAppender = enableJDBCAuditAppender;
    }
}
