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
package org.apache.geode.management.internal.cli.functions;

import java.io.Serializable;

import org.apache.geode.cache.configuration.RegionConfig;

public class CreateRegionFunctionArgs implements Serializable {
  private static final long serialVersionUID = 8103109952945727865L;

  private final String regionPath;
  private final RegionConfig config;
  private final boolean ifNotExists;

  public CreateRegionFunctionArgs(String path, RegionConfig config, boolean ifNotExists) {
    this.regionPath = path;
    this.config = config;
    this.ifNotExists = ifNotExists;
  }

  public boolean isIfNotExists() {
    return ifNotExists;
  }

  public String getRegionPath() {
    return regionPath;
  }

  public RegionConfig getConfig() {
    return config;
  }
}
