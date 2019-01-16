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
package org.apache.geode.management.internal.configuration.domain;

import java.util.Properties;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Declarable;
import org.apache.geode.cache.configuration.ConfigTypeInstantiator;
import org.apache.geode.cache.configuration.DeclarableType;
import org.apache.geode.cache.configuration.ParameterType;
import org.apache.geode.internal.ClassPathLoader;

public class DeclarableTypeInstantiator implements ConfigTypeInstantiator<DeclarableType> {

  private final Cache cache;

  public DeclarableTypeInstantiator(Cache cache) {
    this.cache = cache;
  }

  @Override
  public <T> T newInstance(DeclarableType declarableType) {
    try {
      Class<T> loadedClass =
          (Class<T>) ClassPathLoader.getLatest().forName(declarableType.getClassName());
      T object = loadedClass.newInstance();
      if (object instanceof Declarable) {
        Declarable declarable = (Declarable) object;
        Properties initProperties = new Properties();
        for (ParameterType parameter : declarableType.getParameters()) {
          initProperties.put(parameter.getName(),
              parameter.newInstance(new DeclarableTypeInstantiator(cache)));
        }
        declarable.initialize(cache, initProperties);
      }
      return object;
    } catch (Exception e) {
      throw new RuntimeException(
          "Error instantiating class: <" + declarableType.getClassName() + ">", e);
    }
  }
}
