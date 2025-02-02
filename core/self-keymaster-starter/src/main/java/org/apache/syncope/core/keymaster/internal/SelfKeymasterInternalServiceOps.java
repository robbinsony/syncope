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
package org.apache.syncope.core.keymaster.internal;

import java.util.List;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.KeymasterProperties;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.logic.NetworkServiceLogic;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class SelfKeymasterInternalServiceOps implements ServiceOps {

    @Autowired
    private NetworkServiceLogic logic;

    @Autowired
    private KeymasterProperties props;

    @Override
    public List<NetworkService> list(final NetworkService.Type serviceType) {
        return AuthContextUtils.callAs(
                SyncopeConstants.MASTER_DOMAIN,
                props.getUsername(),
                List.of(),
                () -> logic.list(serviceType));
    }

    @Override
    public NetworkService get(final NetworkService.Type serviceType) {
        try {
            return AuthContextUtils.callAs(
                    SyncopeConstants.MASTER_DOMAIN,
                    props.getUsername(),
                    List.of(),
                    () -> logic.get(serviceType));
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }

    @Override
    public void register(final NetworkService service) {
        AuthContextUtils.callAs(
                SyncopeConstants.MASTER_DOMAIN,
                props.getUsername(),
                List.of(),
                () -> {
                    logic.register(service);
                    return null;
                });
    }

    @Override
    public void unregister(final NetworkService service) {
        AuthContextUtils.callAs(
                SyncopeConstants.MASTER_DOMAIN,
                props.getUsername(),
                List.of(),
                () -> {
                    logic.unregister(service);
                    return null;
                });
    }
}
