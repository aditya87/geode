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

package org.apache.geode.management.internal.cli.domain;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Declarable;
import org.apache.geode.cache.configuration.ConfigTypeInstantiator;
import org.apache.geode.internal.ClassPathLoader;

public class ClassNameInstantiator implements ConfigTypeInstantiator<ClassName<?>> {

  private final Cache cache;

  public ClassNameInstantiator(Cache cache) {
    this.cache = cache;
  }

  @Override
  public <V> V newInstance(ClassName<?> type) {
    try {
      Class<V> loadedClass = (Class<V>) ClassPathLoader.getLatest().forName(type.getClassName());
      V object = loadedClass.newInstance();
      if (object instanceof Declarable) {
        Declarable declarable = (Declarable) object;
        declarable.initialize(cache, type.getInitProperties());
        declarable.init(type.getInitProperties()); // for backwards compatibility
      }
      return object;
    } catch (Exception e) {
      throw new RuntimeException("Error instantiating class: <" + type.getClassName() + ">", e);
    }
  }

}
