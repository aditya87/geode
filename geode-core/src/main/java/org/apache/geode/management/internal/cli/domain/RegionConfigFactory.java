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

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.geode.cache.CacheListener;
import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheWriter;
import org.apache.geode.cache.CustomExpiry;
import org.apache.geode.cache.ExpirationAction;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.configuration.ClassNameType;
import org.apache.geode.cache.configuration.DeclarableType;
import org.apache.geode.cache.configuration.EnumActionDestroyOverflow;
import org.apache.geode.cache.configuration.ExpirationAttributesType;
import org.apache.geode.cache.configuration.RegionAttributesScope;
import org.apache.geode.cache.configuration.RegionAttributesType;
import org.apache.geode.cache.configuration.RegionConfig;

public class RegionConfigFactory {
  public RegionConfig generate(
      String regionPath,
      String keyConstraint,
      String valueConstraint,
      Boolean statisticsEnabled,
      Integer entryExpirationIdleTime,
      ExpirationAction entryExpirationIdleAction,
      Integer entryExpirationTTL,
      ExpirationAction entryExpirationTTLAction,
      ClassName<CustomExpiry> entryIdleTimeCustomExpiry,
      ClassName<CustomExpiry> entryTTLCustomExpiry,
      Integer regionExpirationIdleTime,
      ExpirationAction regionExpirationIdleAction,
      Integer regionExpirationTTL,
      ExpirationAction regionExpirationTTLAction,
      String evictionAction,
      Integer evictionMaxMemory,
      Integer evictionEntryCount,
      String evictionObjectSizer,
      String diskStore,
      Boolean diskSynchronous,
      Boolean enableAsyncConflation,
      Boolean enableSubscriptionConflation,
      Set<ClassName<CacheListener>> cacheListeners,
      ClassName<CacheLoader> cacheLoader,
      ClassName<CacheWriter> cacheWriter,
      Set<String> asyncEventQueueIds,
      Set<String> gatewaySenderIds,
      Boolean concurrencyChecksEnabled,
      Boolean cloningEnabled,
      Boolean mcastEnabled,
      Integer concurrencyLevel,
      PartitionArgs partitionArgs,
      String compressor,
      Boolean offHeap,
      RegionAttributes<?, ?> regionAttributes) {
    RegionConfig regionConfig = new RegionConfig();
    regionConfig.setName(getLeafRegion(regionPath));

    if (keyConstraint != null) {
      addAttribute(regionConfig, a -> a.setKeyConstraint(keyConstraint));
    }

    if (valueConstraint != null) {
      addAttribute(regionConfig, a -> a.setValueConstraint(valueConstraint));
    }

    if (statisticsEnabled != null) {
      addAttribute(regionConfig, a -> a.setStatisticsEnabled(statisticsEnabled));
    } else if (regionAttributes != null) {
      addAttribute(regionConfig, a -> a.setStatisticsEnabled(regionAttributes
          .getStatisticsEnabled()));
    }

    if (entryExpirationIdleTime != null) {
      RegionAttributesType.EntryIdleTime entryIdleTime = new RegionAttributesType.EntryIdleTime();
      entryIdleTime.setExpirationAttributes(new ExpirationAttributesType(entryExpirationIdleAction,
          entryExpirationIdleTime));
      addAttribute(regionConfig, a -> a.setEntryIdleTime(entryIdleTime));
    } else if (regionAttributes != null &&
        regionAttributes.getEntryIdleTimeout() != null &&
        !regionAttributes.getEntryIdleTimeout().isDefault()) {
      RegionAttributesType.EntryIdleTime entryIdleTime = new RegionAttributesType.EntryIdleTime();
      entryIdleTime.setExpirationAttributes(regionAttributes
          .getEntryIdleTimeout().toConfigType());
      addAttribute(regionConfig, a -> a.setEntryIdleTime(entryIdleTime));
    }

    if (entryIdleTimeCustomExpiry != null) {
      Object maybeEntryIdleAttr = getAttribute(regionConfig, a -> a.getEntryIdleTime());
      RegionAttributesType.EntryIdleTime entryIdleTime =
          maybeEntryIdleAttr != null ? (RegionAttributesType.EntryIdleTime) maybeEntryIdleAttr
              : new RegionAttributesType.EntryIdleTime();

      ExpirationAttributesType expirationAttributes;
      if (entryIdleTime.getExpirationAttributes() == null) {
        expirationAttributes = new ExpirationAttributesType();
        expirationAttributes.setTimeout("0");
      } else {
        expirationAttributes = entryIdleTime.getExpirationAttributes();
      }

      DeclarableType customExpiry = new DeclarableType();
      customExpiry.setClassName(entryIdleTimeCustomExpiry.getClassName());
      expirationAttributes.setCustomExpiry(customExpiry);
      entryIdleTime.setExpirationAttributes(expirationAttributes);

      if (maybeEntryIdleAttr == null) {
        addAttribute(regionConfig, a -> a.setEntryIdleTime(entryIdleTime));
      }
    }

    if (entryExpirationTTL != null) {
      RegionAttributesType.EntryTimeToLive entryExpTime =
          new RegionAttributesType.EntryTimeToLive();
      entryExpTime.setExpirationAttributes(new ExpirationAttributesType(entryExpirationTTLAction,
          entryExpirationTTL));
      addAttribute(regionConfig, a -> a.setEntryTimeToLive(entryExpTime));
    } else if (regionAttributes != null
        && regionAttributes.getEntryTimeToLive() != null
        && !regionAttributes.getEntryTimeToLive().isDefault()) {
      RegionAttributesType.EntryTimeToLive entryExpTime =
          new RegionAttributesType.EntryTimeToLive();
      entryExpTime.setExpirationAttributes(
          regionAttributes.getEntryTimeToLive().toConfigType());
      addAttribute(regionConfig, a -> a.setEntryTimeToLive(entryExpTime));
    }

    if (regionExpirationIdleTime != null) {
      RegionAttributesType.RegionIdleTime regionIdleTime =
          new RegionAttributesType.RegionIdleTime();
      regionIdleTime
          .setExpirationAttributes(new ExpirationAttributesType(regionExpirationIdleAction,
              regionExpirationIdleTime));
      addAttribute(regionConfig, a -> a.setRegionIdleTime(regionIdleTime));
    } else if (regionAttributes != null
        && regionAttributes.getRegionIdleTimeout() != null
        && !regionAttributes.getRegionIdleTimeout().isDefault()) {
      RegionAttributesType.RegionIdleTime regionIdleTime =
          new RegionAttributesType.RegionIdleTime();
      regionIdleTime.setExpirationAttributes(
          regionAttributes.getRegionIdleTimeout().toConfigType());
      addAttribute(regionConfig, a -> a.setRegionIdleTime(regionIdleTime));
    }

    if (regionExpirationTTL != null) {
      RegionAttributesType.RegionTimeToLive regionExpTime =
          new RegionAttributesType.RegionTimeToLive();
      regionExpTime.setExpirationAttributes(new ExpirationAttributesType(regionExpirationTTLAction,
          regionExpirationTTL));
      addAttribute(regionConfig, a -> a.setRegionTimeToLive(regionExpTime));
    } else if (regionAttributes != null
        && regionAttributes.getRegionTimeToLive() != null
        && !regionAttributes.getRegionTimeToLive().isDefault()) {
      RegionAttributesType.RegionTimeToLive regionExpTime =
          new RegionAttributesType.RegionTimeToLive();
      regionExpTime.setExpirationAttributes(
          regionAttributes.getRegionTimeToLive().toConfigType());
      addAttribute(regionConfig, a -> a.setRegionTimeToLive(regionExpTime));
    }

    if (entryTTLCustomExpiry != null) {
      Object maybeEntryTTLAttr = getAttribute(regionConfig, a -> a.getEntryTimeToLive());
      RegionAttributesType.EntryTimeToLive entryTimeToLive =
          maybeEntryTTLAttr != null ? (RegionAttributesType.EntryTimeToLive) maybeEntryTTLAttr
              : new RegionAttributesType.EntryTimeToLive();

      ExpirationAttributesType expirationAttributes;
      if (entryTimeToLive.getExpirationAttributes() == null) {
        expirationAttributes = new ExpirationAttributesType();
        expirationAttributes.setTimeout("0");
      } else {
        expirationAttributes = entryTimeToLive.getExpirationAttributes();
      }

      DeclarableType customExpiry = new DeclarableType();
      customExpiry.setClassName(entryTTLCustomExpiry.getClassName());
      expirationAttributes.setCustomExpiry(customExpiry);
      entryTimeToLive.setExpirationAttributes(expirationAttributes);

      if (maybeEntryTTLAttr == null) {
        addAttribute(regionConfig, a -> a.setEntryTimeToLive(entryTimeToLive));
      }
    }

    if (diskStore != null) {
      addAttribute(regionConfig, a -> a.setDiskStoreName(diskStore));
    } else if (regionAttributes != null) {
      addAttribute(regionConfig, a -> a.setDiskStoreName(regionAttributes.getDiskStoreName()));
    }

    if (diskSynchronous != null) {
      addAttribute(regionConfig, a -> a.setDiskSynchronous(diskSynchronous));
    } else if (regionAttributes != null) {
      addAttribute(regionConfig, a -> a.setDiskSynchronous(regionAttributes.isDiskSynchronous()));
    }

    if (enableAsyncConflation != null) {
      addAttribute(regionConfig, a -> a.setEnableAsyncConflation(enableAsyncConflation));
    } else if (regionAttributes != null) {
      addAttribute(regionConfig, a -> a.setEnableAsyncConflation(regionAttributes
          .getEnableAsyncConflation()));
    }

    if (enableSubscriptionConflation != null) {
      addAttribute(regionConfig,
          a -> a.setEnableSubscriptionConflation(enableSubscriptionConflation));
    } else if (regionAttributes != null) {
      addAttribute(regionConfig, a -> a.setEnableSubscriptionConflation(regionAttributes
          .getEnableSubscriptionConflation()));
    }

    if (concurrencyChecksEnabled != null) {
      addAttribute(regionConfig, a -> a.setConcurrencyChecksEnabled(
          concurrencyChecksEnabled));
    } else if (regionAttributes != null) {
      addAttribute(regionConfig, a -> a.setConcurrencyChecksEnabled(regionAttributes
          .getConcurrencyChecksEnabled()));
    }

    if (cloningEnabled != null) {
      addAttribute(regionConfig, a -> a.setCloningEnabled(cloningEnabled));
    } else if (regionAttributes != null) {
      addAttribute(regionConfig, a -> a.setCloningEnabled(regionAttributes
          .getCloningEnabled()));
    }

    if (offHeap != null) {
      addAttribute(regionConfig, a -> a.setOffHeap(offHeap));
    } else if (regionAttributes != null) {
      addAttribute(regionConfig, a -> a.setOffHeap(regionAttributes.getOffHeap()));
    }

    if (mcastEnabled != null) {
      addAttribute(regionConfig, a -> a.setMulticastEnabled(mcastEnabled));
    } else if (regionAttributes != null) {
      addAttribute(regionConfig, a -> a.setMulticastEnabled(regionAttributes
          .getMulticastEnabled()));
    }

    if (partitionArgs != null && !partitionArgs.isEmpty()) {
      RegionAttributesType.PartitionAttributes partitionAttributes =
          new RegionAttributesType.PartitionAttributes();

      partitionAttributes.setColocatedWith(partitionArgs.prColocatedWith);
      partitionAttributes.setLocalMaxMemory(int2string(partitionArgs.prLocalMaxMemory));
      partitionAttributes.setRecoveryDelay(long2string(partitionArgs.prRecoveryDelay));
      partitionAttributes.setRedundantCopies(int2string(partitionArgs.prRedundantCopies));
      partitionAttributes
          .setStartupRecoveryDelay(long2string(partitionArgs.prStartupRecoveryDelay));
      partitionAttributes.setTotalMaxMemory(long2string(partitionArgs.prTotalMaxMemory));
      partitionAttributes.setTotalNumBuckets(int2string(partitionArgs.prTotalNumBuckets));

      if (partitionArgs.partitionResolver != null) {
        DeclarableType partitionResolverType = new DeclarableType();
        partitionResolverType.setClassName(partitionArgs.partitionResolver);
        partitionAttributes.setPartitionResolver(partitionResolverType);
      }

      addAttribute(regionConfig, a -> a.setPartitionAttributes(partitionAttributes));
    } else if (regionAttributes != null && regionAttributes.getPartitionAttributes() != null) {
      addAttribute(regionConfig, a -> a.setPartitionAttributes(
          regionAttributes.getPartitionAttributes().convertToConfigPartitionAttributes()));
    }

    if (gatewaySenderIds != null && !gatewaySenderIds.isEmpty()) {
      addAttribute(regionConfig, a -> a.setGatewaySenderIds(String.join(",", gatewaySenderIds)));
    }

    if (evictionAction != null) {
      RegionAttributesType.EvictionAttributes evictionAttributes =
          generateEvictionAttributes(evictionAction, evictionMaxMemory, evictionEntryCount,
              evictionObjectSizer);
      addAttribute(regionConfig, a -> a.setEvictionAttributes(evictionAttributes));
    } else if (regionAttributes != null &&
        regionAttributes.getEvictionAttributes() != null &&
        !regionAttributes.getEvictionAttributes().isEmpty()) {
      addAttribute(regionConfig,
          a -> a.setEvictionAttributes(regionAttributes.getEvictionAttributes()
              .convertToConfigEvictionAttributes()));
    }

    if (asyncEventQueueIds != null && !asyncEventQueueIds.isEmpty()) {
      addAttribute(regionConfig,
          a -> a.setAsyncEventQueueIds(String.join(",", asyncEventQueueIds)));
    }

    if (cacheListeners != null && !cacheListeners.isEmpty()) {
      addAttribute(regionConfig, a -> a.getCacheListeners().addAll(
          cacheListeners.stream().map(l -> {
            DeclarableType declarableType = new DeclarableType();
            declarableType.setClassName(l.getClassName());
            return declarableType;
          }).collect(Collectors.toList())));
    }

    if (cacheLoader != null) {
      DeclarableType declarableType = new DeclarableType();
      declarableType.setClassName(cacheLoader.getClassName());
      addAttribute(regionConfig, a -> a.setCacheLoader(declarableType));
    }

    if (cacheWriter != null) {
      DeclarableType declarableType = new DeclarableType();
      declarableType.setClassName(cacheWriter.getClassName());
      addAttribute(regionConfig, a -> a.setCacheWriter(declarableType));
    }

    if (compressor != null) {
      addAttribute(regionConfig, a -> a.setCompressor(new ClassNameType(compressor)));
      addAttribute(regionConfig, a -> a.setCloningEnabled(true));
    }

    if (concurrencyLevel != null) {
      addAttribute(regionConfig, a -> a.setConcurrencyLevel(concurrencyLevel.toString()));
    } else if (regionAttributes != null) {
      addAttribute(regionConfig, a -> a.setConcurrencyLevel(Integer.toString(
          regionAttributes.getConcurrencyLevel())));
    }

    if (regionAttributes != null && regionAttributes.getDataPolicy() != null) {
      addAttribute(regionConfig,
          a -> a.setDataPolicy(regionAttributes.getDataPolicy().toConfigType()));
    }

    if (regionAttributes != null && regionAttributes.getScope() != null
        && !regionAttributes.getDataPolicy().withPartitioning()) {
      addAttribute(regionConfig,
          a -> a.setScope(
              RegionAttributesScope.fromValue(regionAttributes.getScope().toConfigTypeString())));
    }

    return regionConfig;
  }

  private RegionAttributesType.EvictionAttributes generateEvictionAttributes(String evictionAction,
      Integer maxMemory,
      Integer maxEntryCount, String objectSizer) {
    RegionAttributesType.EvictionAttributes configAttributes =
        new RegionAttributesType.EvictionAttributes();
    EnumActionDestroyOverflow action = EnumActionDestroyOverflow.fromValue(evictionAction);

    if (maxMemory == null && maxEntryCount == null) {
      RegionAttributesType.EvictionAttributes.LruHeapPercentage heapPercentage =
          new RegionAttributesType.EvictionAttributes.LruHeapPercentage();
      heapPercentage.setAction(action);
      heapPercentage.setClassName(objectSizer);
      configAttributes.setLruHeapPercentage(heapPercentage);
    } else if (maxMemory != null) {
      RegionAttributesType.EvictionAttributes.LruMemorySize memorySize =
          new RegionAttributesType.EvictionAttributes.LruMemorySize();
      memorySize.setAction(action);
      memorySize.setClassName(objectSizer);
      memorySize.setMaximum(maxMemory.toString());
      configAttributes.setLruMemorySize(memorySize);
    } else {
      RegionAttributesType.EvictionAttributes.LruEntryCount entryCount =
          new RegionAttributesType.EvictionAttributes.LruEntryCount();
      entryCount.setAction(action);
      entryCount.setMaximum(maxEntryCount.toString());
      configAttributes.setLruEntryCount(entryCount);
    }

    return configAttributes;
  }

  private String int2string(Integer i) {
    return Optional.ofNullable(i).map(j -> j.toString()).orElse(null);
  }

  private String long2string(Long i) {
    return Optional.ofNullable(i).map(j -> j.toString()).orElse(null);
  }

  private String getLeafRegion(String fullPath) {
    String regionPath = fullPath;
    String[] regions = regionPath.split("/");

    return regions[regions.length - 1];
  }

  private void addAttribute(RegionConfig config, Consumer<RegionAttributesType> consumer) {
    if (config.getRegionAttributes() == null) {
      config.setRegionAttributes(new RegionAttributesType());
    }

    consumer.accept(config.getRegionAttributes());
  }

  private Object getAttribute(RegionConfig config,
      Function<RegionAttributesType, Object> function) {
    if (config.getRegionAttributes() == null) {
      return null;
    }

    return function.apply(config.getRegionAttributes());
  }
}
