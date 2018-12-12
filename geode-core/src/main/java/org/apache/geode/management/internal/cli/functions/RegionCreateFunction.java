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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Logger;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheListener;
import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheWriter;
import org.apache.geode.cache.CustomExpiry;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.ExpirationAction;
import org.apache.geode.cache.ExpirationAttributes;
import org.apache.geode.cache.PartitionAttributes;
import org.apache.geode.cache.PartitionResolver;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionExistsException;
import org.apache.geode.cache.RegionFactory;
import org.apache.geode.cache.configuration.DeclarableType;
import org.apache.geode.cache.configuration.RegionAttributesType;
import org.apache.geode.cache.configuration.RegionConfig;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.ResultSender;
import org.apache.geode.compression.Compressor;
import org.apache.geode.internal.ClassPathLoader;
import org.apache.geode.internal.cache.EvictionAttributesImpl;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.internal.cache.execute.InternalFunction;
import org.apache.geode.internal.cache.xmlcache.CacheXml;
import org.apache.geode.internal.logging.LogService;
import org.apache.geode.management.internal.cli.CliUtil;
import org.apache.geode.management.internal.cli.commands.RegionCommandsUtils;
import org.apache.geode.management.internal.cli.i18n.CliStrings;
import org.apache.geode.management.internal.cli.util.RegionPath;
import org.apache.geode.management.internal.configuration.domain.XmlEntity;

/**
 *
 * @since GemFire 7.0
 */
public class RegionCreateFunction implements InternalFunction {

  private static final Logger logger = LogService.getLogger();

  private static final long serialVersionUID = 8746830191680509335L;

  private static final String ID = RegionCreateFunction.class.getName();

  public static RegionCreateFunction INSTANCE = new RegionCreateFunction();

  @Override
  public boolean isHA() {
    return false;
  }

  @Override
  public void execute(FunctionContext context) {
    ResultSender<Object> resultSender = context.getResultSender();

    Cache cache = ((InternalCache) context.getCache()).getCacheForProcessingClientRequests();
    String memberNameOrId = context.getMemberName();

    CreateRegionFunctionArgs regionCreateArgs = (CreateRegionFunctionArgs) context.getArguments();

    if (regionCreateArgs.isIfNotExists()) {
      Region<Object, Object> region = cache.getRegion(regionCreateArgs.getRegionPath());
      if (region != null) {
        resultSender
            .lastResult(new CliFunctionResult(memberNameOrId, CliFunctionResult.StatusState.OK,
                CliStrings.format(
                    CliStrings.CREATE_REGION__MSG__SKIPPING_0_REGION_PATH_1_ALREADY_EXISTS,
                    memberNameOrId, regionCreateArgs.getRegionPath())));
        return;
      }
    }

    try {
      Region<?, ?> createdRegion =
          createRegion(cache, regionCreateArgs.getConfig(), regionCreateArgs.getRegionPath());
      XmlEntity xmlEntity = getXmlEntityForRegion(createdRegion);
      resultSender.lastResult(new CliFunctionResult(memberNameOrId, xmlEntity.getXmlDefinition(),
          CliStrings.format(CliStrings.CREATE_REGION__MSG__REGION_0_CREATED_ON_1,
              createdRegion.getFullPath(), memberNameOrId)));
    } catch (IllegalStateException e) {
      String exceptionMsg = e.getMessage();
      String localizedString =
          "Only regions with persistence or overflow to disk can specify DiskStore";
      if (localizedString.equals(e.getMessage())) {
        exceptionMsg = exceptionMsg + " "
            + CliStrings.format(CliStrings.CREATE_REGION__MSG__USE_ONE_OF_THESE_SHORTCUTS_0,
                new Object[] {String.valueOf(RegionCommandsUtils.PERSISTENT_OVERFLOW_SHORTCUTS)});
      }
      resultSender.lastResult(handleException(memberNameOrId, exceptionMsg, null/* do not log */));
    } catch (IllegalArgumentException e) {
      resultSender.lastResult(handleException(memberNameOrId, e.getMessage(), e));
    } catch (RegionExistsException e) {
      String exceptionMsg =
          CliStrings.format(CliStrings.CREATE_REGION__MSG__REGION_PATH_0_ALREADY_EXISTS_ON_1,
              regionCreateArgs.getRegionPath(), memberNameOrId);
      resultSender.lastResult(handleException(memberNameOrId, exceptionMsg, e));
    } catch (Exception e) {
      String exceptionMsg = e.getMessage();
      if (exceptionMsg == null) {
        exceptionMsg = ExceptionUtils.getStackTrace(e);
      }
      resultSender.lastResult(handleException(memberNameOrId, exceptionMsg, e));
    }
  }

  private XmlEntity getXmlEntityForRegion(Region<?, ?> region) {
    Region<?, ?> curRegion = region;
    while (curRegion != null && curRegion.getParentRegion() != null) {
      curRegion = curRegion.getParentRegion();
    }

    return new XmlEntity(CacheXml.REGION, "name", curRegion.getName());
  }

  private CliFunctionResult handleException(final String memberNameOrId, final String exceptionMsg,
      final Exception e) {
    if (e != null && logger.isDebugEnabled()) {
      logger.debug(e.getMessage(), e);
    }

    if (exceptionMsg != null) {
      return new CliFunctionResult(memberNameOrId, CliFunctionResult.StatusState.ERROR,
          exceptionMsg);
    }

    return new CliFunctionResult(memberNameOrId, CliFunctionResult.StatusState.ERROR);
  }

  private <K, V> Region<?, ?> createRegion(Cache cache, RegionConfig config, String regionPath)
      throws RuntimeException {
    RegionAttributesType regionAttributes = config.getRegionAttributes();

    // Perform classpath validations
    if (regionAttributes.getEntryIdleTime() != null
        && regionAttributes.getEntryIdleTime().getExpirationAttributes()
            .getCustomExpiry() != null) {
      String customExpiry =
          regionAttributes.getEntryIdleTime().getExpirationAttributes().getCustomExpiry()
              .getClassName();
      String neededFor = CliStrings.ENTRY_IDLE_TIME_CUSTOM_EXPIRY;
      Class<CustomExpiry> customExpiryClass = InternalFunction.forName(customExpiry, neededFor);
      InternalFunction.newInstance(customExpiryClass, neededFor);
    }

    if (regionAttributes.getEntryTimeToLive() != null
        && regionAttributes.getEntryTimeToLive().getExpirationAttributes()
            .getCustomExpiry() != null) {
      String customExpiry =
          regionAttributes.getEntryTimeToLive().getExpirationAttributes().getCustomExpiry()
              .getClassName();
      String neededFor = CliStrings.ENTRY_TTL_CUSTOM_EXPIRY;
      Class<CustomExpiry> customExpiryClass = InternalFunction.forName(customExpiry, neededFor);
      InternalFunction.newInstance(customExpiryClass, neededFor);
    }

    if (regionAttributes.getPartitionAttributes() != null
        && regionAttributes.getPartitionAttributes().getPartitionResolver() != null) {
      String partitionResolver =
          regionAttributes.getPartitionAttributes().getPartitionResolver().getClassName();
      String neededFor = CliStrings.CREATE_REGION__PARTITION_RESOLVER;
      Class<PartitionResolver> partitionResolverClass =
          InternalFunction.forName(partitionResolver, neededFor);
      InternalFunction.newInstance(partitionResolverClass, neededFor);
    }

    // TODO: Add validations for cache loader, cache writer and cache listeners

    Region<K, V> createdRegion;

    // create the region factory using the arguments
    RegionFactory<K, V> factory = cache.createRegionFactory();

    if (regionAttributes.getPartitionAttributes() != null) {
      factory.setPartitionAttributes(
          PartitionAttributes.fromConfig(regionAttributes.getPartitionAttributes()));
    }

    factory
        .setDataPolicy(DataPolicy.fromString(regionAttributes.getDataPolicy().value().toUpperCase()
            .replace("-", "_")));

    // Set Constraints
    final String keyConstraint = (String) regionAttributes.getKeyConstraint();
    final String valueConstraint = regionAttributes.getValueConstraint();
    if (keyConstraint != null && !keyConstraint.isEmpty()) {
      Class<K> keyConstraintClass =
          CliUtil.forName(keyConstraint, CliStrings.CREATE_REGION__KEYCONSTRAINT);
      factory.setKeyConstraint(keyConstraintClass);
    }

    if (valueConstraint != null && !valueConstraint.isEmpty()) {
      Class<V> valueConstraintClass =
          CliUtil.forName(valueConstraint, CliStrings.CREATE_REGION__VALUECONSTRAINT);
      factory.setValueConstraint(valueConstraintClass);
    }

    // Expiration attributes
    if (regionAttributes.getEntryIdleTime() != null) {
      RegionAttributesType.EntryIdleTime eitl = regionAttributes.getEntryIdleTime();
      factory.setEntryIdleTimeout(
          new ExpirationAttributes(Integer.valueOf(eitl.getExpirationAttributes().getTimeout()),
              ExpirationAction.fromString(eitl.getExpirationAttributes().getAction().toUpperCase()
                  .replace("-", "_"))));

      try {
        if (eitl.getExpirationAttributes().getCustomExpiry() != null) {
          factory.setCustomEntryIdleTimeout((CustomExpiry) ClassPathLoader.getLatest()
              .forName(eitl.getExpirationAttributes().getCustomExpiry().getClassName())
              .newInstance());
        }
      } catch (Exception e) {
      }
    }

    if (regionAttributes.getEntryTimeToLive() != null) {
      RegionAttributesType.EntryTimeToLive ettl = regionAttributes.getEntryTimeToLive();
      factory.setEntryTimeToLive(
          new ExpirationAttributes(Integer.valueOf(ettl.getExpirationAttributes().getTimeout()),
              ExpirationAction.fromString(ettl.getExpirationAttributes().getAction().toUpperCase()
                  .replace("-", "_"))));

      try {
        if (ettl.getExpirationAttributes().getCustomExpiry() != null) {
          factory.setCustomEntryTimeToLive((CustomExpiry) ClassPathLoader.getLatest()
              .forName(ettl.getExpirationAttributes().getCustomExpiry().getClassName())
              .newInstance());
        }
      } catch (Exception e) {
      }
    }

    if (regionAttributes.getRegionIdleTime() != null) {
      RegionAttributesType.RegionIdleTime ritl = regionAttributes.getRegionIdleTime();
      factory.setRegionIdleTimeout(
          new ExpirationAttributes(Integer.valueOf(ritl.getExpirationAttributes().getTimeout()),
              ExpirationAction.fromString(ritl.getExpirationAttributes().getAction().toUpperCase()
                  .replace("-", "_"))));
    }

    if (regionAttributes.getRegionTimeToLive() != null) {
      RegionAttributesType.RegionTimeToLive rttl = regionAttributes.getRegionTimeToLive();
      factory.setRegionTimeToLive(
          new ExpirationAttributes(Integer.valueOf(rttl.getExpirationAttributes().getTimeout()),
              ExpirationAction.fromString(rttl.getExpirationAttributes().getAction().toUpperCase()
                  .replace("-", "_"))));
    }

    if (regionAttributes.getEvictionAttributes() != null) {
      factory.setEvictionAttributes(
          EvictionAttributesImpl.fromConfig(regionAttributes.getEvictionAttributes()));
    }

    // Associate a Disk Store
    if (regionAttributes.getDiskStoreName() != null) {
      factory.setDiskStoreName(regionAttributes.getDiskStoreName());
    }

    if (regionAttributes.isDiskSynchronous() != null) {
      factory.setDiskSynchronous(regionAttributes.isDiskSynchronous());
    }

    if (regionAttributes.isOffHeap() != null) {
      factory.setOffHeap(regionAttributes.isOffHeap());
    }

    if (regionAttributes.isStatisticsEnabled() != null) {
      factory.setStatisticsEnabled(regionAttributes.isStatisticsEnabled());
    }

    if (regionAttributes.isEnableAsyncConflation() != null) {
      factory.setEnableAsyncConflation(regionAttributes.isEnableAsyncConflation());
    }

    if (regionAttributes.isEnableSubscriptionConflation() != null) {
      factory.setEnableSubscriptionConflation(regionAttributes.isEnableSubscriptionConflation());
    }

    // Gateway Sender Ids
    if (regionAttributes.getGatewaySenderIds() != null) {
      Arrays.stream(regionAttributes.getGatewaySenderIds().split(","))
          .forEach(gsi -> factory.addGatewaySenderId(gsi));
    }

    // Async Queue Ids
    if (regionAttributes.getAsyncEventQueueIds() != null) {
      Arrays.stream(regionAttributes.getAsyncEventQueueIds().split(","))
          .forEach(gsi -> factory.addAsyncEventQueueId(gsi));
    }

    factory.setConcurrencyChecksEnabled(regionAttributes.isConcurrencyChecksEnabled());

    if (regionAttributes.getConcurrencyLevel() != null) {
      factory.setConcurrencyLevel(Integer.valueOf(regionAttributes.getConcurrencyLevel()));
    }

    if (regionAttributes.isCloningEnabled() != null) {
      factory.setCloningEnabled(regionAttributes.isCloningEnabled());
    }

    if (regionAttributes.isMulticastEnabled() != null) {
      factory.setMulticastEnabled(regionAttributes.isMulticastEnabled());
    }

    // Set plugins
    if (regionAttributes.getCacheListeners() != null) {
      List<DeclarableType> configListeners = regionAttributes.getCacheListeners();
      CacheListener[] listeners = new CacheListener[configListeners.size()];
      try {
        for (int i = 0; i < configListeners.size(); i++) {
          listeners[i] = (CacheListener) ClassPathLoader.getLatest()
              .forName(configListeners.get(i).getClassName())
              .newInstance();
        }
      } catch (Exception e) {
      }
      factory.initCacheListeners(listeners);
    }

    // Compression provider
    if (regionAttributes.getCompressor() != null) {
      Class<Compressor> compressorKlass =
          CliUtil.forName(regionAttributes.getCompressor().getClassName(),
              CliStrings.CREATE_REGION__COMPRESSOR);
      factory.setCompressor(
          CliUtil.newInstance(compressorKlass, CliStrings.CREATE_REGION__COMPRESSOR));
    }

    if (regionAttributes.getCacheLoader() != null) {
      try {
        factory.setCacheLoader(
            (CacheLoader) ClassPathLoader.getLatest().forName(regionAttributes.getCacheLoader()
                .getClassName()).newInstance());
      } catch (Exception e) {
      }
    }

    if (regionAttributes.getCacheWriter() != null) {
      try {
        factory.setCacheWriter(
            (CacheWriter) ClassPathLoader.getLatest().forName(regionAttributes.getCacheWriter()
                .getClassName()).newInstance());
      } catch (Exception e) {
      }
    }

    // If a region path indicates a sub-region,
    RegionPath regionPathData = new RegionPath(regionPath);
    String regionName = regionPathData.getName();
    String parentRegionPath = regionPathData.getParent();
    if (parentRegionPath != null && !Region.SEPARATOR.equals(parentRegionPath)) {
      Region<?, ?> parentRegion = cache.getRegion(parentRegionPath);
      createdRegion = factory.createSubregion(parentRegion, regionName);
    } else {
      createdRegion = factory.create(regionName);
    }

    return createdRegion;
  }


  @Override
  public String getId() {
    return ID;
  }
}
