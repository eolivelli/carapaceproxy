/*
 Licensed to Diennea S.r.l. under one
 or more contributor license agreements. See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership. Diennea S.r.l. licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.

 */
package org.carapaceproxy.user;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.carapaceproxy.configstore.ConfigurationStore;
import org.carapaceproxy.server.config.ConfigurationNotValidException;

/**
 * Simple implementation of an {@link UserRealm} that has only one user and it's
 * always authorized.
 *
 * @author matteo.minardi
 */
public class SimpleUserRealm implements UserRealm {

    public static final String USERNAME = "admin";

    @Override
    public void configure(ConfigurationStore properties) throws ConfigurationNotValidException {
    }

    @Override
    public Collection<String> listUsers() {
        return Collections.unmodifiableCollection(Arrays.asList(USERNAME));
    }

    @Override
    public String login(String username, String password) {
        return USERNAME;
    }

}
