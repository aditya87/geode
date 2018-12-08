package org.apache.geode.management.internal.cli.domain;

import java.util.HashSet;
import java.util.Set;

import org.apache.geode.management.internal.cli.i18n.CliStrings;

public class PartitionArgs {
  public String prColocatedWith;
  public Integer prLocalMaxMemory;
  public Long prRecoveryDelay;
  public Integer prRedundantCopies;
  public Long prStartupRecoveryDelay;
  public Long prTotalMaxMemory;
  public Integer prTotalNumBuckets;
  public String partitionResolver;

  public PartitionArgs(String prColocatedWith, Integer prLocalMaxMemory, Long prRecoveryDelay,
      Integer prRedundantCopies, Long prStartupRecoveryDelay, Long prTotalMaxMemory,
      Integer prTotalNumBuckets, String partitionResolver) {
    this.prColocatedWith = prColocatedWith;
    this.prLocalMaxMemory = prLocalMaxMemory;
    this.prRecoveryDelay = prRecoveryDelay;
    this.prRedundantCopies = prRedundantCopies;
    this.prStartupRecoveryDelay = prStartupRecoveryDelay;
    this.prTotalMaxMemory = prTotalMaxMemory;
    this.prTotalNumBuckets = prTotalNumBuckets;
    this.partitionResolver = partitionResolver;
  }

  public boolean isEmpty() {
    return prColocatedWith == null &&
        prLocalMaxMemory == null &&
        prRecoveryDelay == null &&
        prRedundantCopies == null &&
        prStartupRecoveryDelay == null &&
        prTotalMaxMemory == null &&
        prTotalNumBuckets == null &&
        partitionResolver == null;
  }

  public Set<String> getUserSpecifiedPartitionAttributes() {
    Set<String> userSpecifiedPartitionAttributes = new HashSet<>();

    if (this.prColocatedWith != null) {
      userSpecifiedPartitionAttributes.add(CliStrings.CREATE_REGION__COLOCATEDWITH);
    }
    if (this.prLocalMaxMemory != null) {
      userSpecifiedPartitionAttributes.add(CliStrings.CREATE_REGION__LOCALMAXMEMORY);
    }
    if (this.prRecoveryDelay != null) {
      userSpecifiedPartitionAttributes.add(CliStrings.CREATE_REGION__RECOVERYDELAY);
    }
    if (this.prRedundantCopies != null) {
      userSpecifiedPartitionAttributes.add(CliStrings.CREATE_REGION__REDUNDANTCOPIES);
    }
    if (this.prStartupRecoveryDelay != null) {
      userSpecifiedPartitionAttributes.add(CliStrings.CREATE_REGION__STARTUPRECOVERYDDELAY);
    }
    if (this.prTotalMaxMemory != null) {
      userSpecifiedPartitionAttributes.add(CliStrings.CREATE_REGION__TOTALMAXMEMORY);
    }
    if (this.prTotalNumBuckets != null) {
      userSpecifiedPartitionAttributes.add(CliStrings.CREATE_REGION__TOTALNUMBUCKETS);
    }
    if (this.partitionResolver != null) {
      userSpecifiedPartitionAttributes.add(CliStrings.CREATE_REGION__PARTITION_RESOLVER);
    }

    return userSpecifiedPartitionAttributes;
  }
}
