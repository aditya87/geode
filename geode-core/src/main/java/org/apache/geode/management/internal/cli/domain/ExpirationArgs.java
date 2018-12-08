package org.apache.geode.management.internal.cli.domain;

import org.apache.geode.cache.ExpirationAction;
import org.apache.geode.cache.ExpirationAttributes;

public class ExpirationArgs {
  private final Integer time;
  private final ExpirationAction action;

  public ExpirationArgs(Integer time, ExpirationAction action) {
    this.time = time;
    this.action = action;
  }

  public Integer getTime() {
    return time;
  }

  public ExpirationAction getAction() {
    return action;
  }

  public ExpirationAttributes getExpirationAttributes() {
    return getExpirationAttributes(null);
  }

  public ExpirationAttributes getExpirationAttributes(ExpirationAttributes existing) {
    // default values
    int timeToUse = 0;
    ExpirationAction actionToUse = ExpirationAction.INVALIDATE;

    if (existing != null) {
      timeToUse = existing.getTimeout();
      actionToUse = existing.getAction();
    }
    if (time != null) {
      timeToUse = time;
    }

    if (action != null) {
      actionToUse = action;
    }
    return new ExpirationAttributes(timeToUse, actionToUse);
  }
}
