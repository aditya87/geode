/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.geode.management.client;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import org.apache.geode.annotations.Experimental;
import org.apache.geode.cache.configuration.CacheElement;
import org.apache.geode.management.api.ClusterManagementResult;
import org.apache.geode.management.api.ClusterManagementService;

/**
 * Implementation of {@link ClusterManagementService} interface which represents the cluster
 * management
 * service as used by a Java client.
 */
@Experimental
public class JavaClientClusterManagementService implements ClusterManagementService {
  private final String clusterURL;

  public JavaClientClusterManagementService(String clusterURL) {
    this.clusterURL = clusterURL;
  }

  @Override
  public ClusterManagementResult createCacheElement(CacheElement config) {
    throw new NotImplementedException();
  }

  @Override
  public ClusterManagementResult deleteCacheElement(CacheElement config) {
    throw new NotImplementedException();
  }

  @Override
  public ClusterManagementResult updateCacheElement(CacheElement config) {
    throw new NotImplementedException();
  }

  public String getClusterURL() {
    return clusterURL;
  }
}
