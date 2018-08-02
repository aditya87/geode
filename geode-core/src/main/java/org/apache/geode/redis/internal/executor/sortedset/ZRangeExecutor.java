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
package org.apache.geode.redis.internal.executor.sortedset;

import org.apache.geode.cache.Region;
import org.apache.geode.redis.internal.ByteArrayWrapper;
import org.apache.geode.redis.internal.Coder;
import org.apache.geode.redis.internal.Command;
import org.apache.geode.redis.internal.ExecutionHandlerContext;
import org.apache.geode.redis.internal.Extendable;
import org.apache.geode.redis.internal.Pair;
import org.apache.geode.redis.internal.RedisConstants.ArityDef;
import org.apache.geode.redis.internal.ZSet;

import java.util.List;

public class ZRangeExecutor extends SortedSetExecutor implements Extendable {

  private final String ERROR_NOT_NUMERIC = "The index provided is not numeric";

  @Override
  public void executeCommand(Command command, ExecutionHandlerContext context) {
    List<byte[]> commandElems = command.getProcessedCommand();

    if (commandElems.size() < 4) {
      command.setResponse(Coder.getErrorResponse(context.getByteBufAllocator(), getArgsError()));
      return;
    }

    boolean withScores = false;

    if (commandElems.size() >= 5) {
      byte[] fifthElem = commandElems.get(4);
      withScores = Coder.bytesToString(fifthElem).equalsIgnoreCase("WITHSCORES");

    }

    Region<ByteArrayWrapper, ZSet> zSetRegion = context.getRegionProvider().getZsetRegion();
    ByteArrayWrapper key = command.getKey();

    if (!zSetRegion.containsKey(key)) {
      command.setResponse(Coder.getEmptyArrayResponse(context.getByteBufAllocator()));
      return;
    }

    ZSet zset = zSetRegion.get(key);

    int start;
    int stop;

    try {
      byte[] startArray = commandElems.get(2);
      byte[] stopArray = commandElems.get(3);
      start = Coder.bytesToInt(startArray);
      stop = Coder.bytesToInt(stopArray);
    } catch (NumberFormatException e) {
      command.setResponse(Coder.getErrorResponse(context.getByteBufAllocator(), ERROR_NOT_NUMERIC));
      return;
    }

    List<Pair<String, Double>> results = isReverse() ?
            zset.getMembersInRevRange(start, stop) :
            zset.getMembersInRange(start, stop);

    command.setResponse(Coder.zRangeResponse(context.getByteBufAllocator(), results, withScores));
  }

  protected boolean isReverse() {
    return false;
  }

  @Override
  public String getArgsError() {
    return ArityDef.ZRANGE;
  }
}
