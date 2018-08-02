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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ZRangeByScoreExecutor extends SortedSetExecutor implements Extendable {

  private final String ERROR_NOT_NUMERIC = "The number provided is not numeric";

  private final String ERROR_LIMIT = "The offset and count cannot be negative";

  @Override
  public void executeCommand(Command command, ExecutionHandlerContext context) {
    List<byte[]> commandElems = command.getProcessedCommand();

    if (commandElems.size() < 4) {
      command.setResponse(Coder.getErrorResponse(context.getByteBufAllocator(), getArgsError()));
      return;
    }

    boolean withScores = false;
    byte[] elem4Array = null;
    int offset = 0;
    int limit = -1;
    if (commandElems.size() >= 5) {
      elem4Array = commandElems.get(4);
      String elem4 = Coder.bytesToString(elem4Array);
      int limitIndex = 4;
      if (elem4.equalsIgnoreCase("WITHSCORES")) {
        withScores = true;
        limitIndex++;
      }

      if (commandElems.size() >= limitIndex + 2) {
        String limitString = Coder.bytesToString(commandElems.get(limitIndex));
        if (limitString.equalsIgnoreCase("LIMIT")) {
          try {
            byte[] offsetArray = commandElems.get(limitIndex + 1);
            byte[] limitArray = commandElems.get(limitIndex + 2);
            offset = Coder.bytesToInt(offsetArray);
            limit = Coder.bytesToInt(limitArray);
          } catch (NumberFormatException e) {
            command.setResponse(
                Coder.getErrorResponse(context.getByteBufAllocator(), ERROR_NOT_NUMERIC));
            return;
          }
        }

        if (offset < 0 || limit < 0) {
          command.setResponse(Coder.getErrorResponse(context.getByteBufAllocator(), ERROR_LIMIT));
          return;
        }

        if (limitIndex == 4 && commandElems.size() >= 8) {
          byte[] lastElemArray = commandElems.get(7);
          String lastString = Coder.bytesToString(lastElemArray);
          if (lastString.equalsIgnoreCase("WITHSCORES")) {
            withScores = true;
          }
        }
      }
    }

    ByteArrayWrapper key = command.getKey();
    Region<ByteArrayWrapper, ZSet> zsetRegion = context.getRegionProvider().getZsetRegion();

    if (!zsetRegion.containsKey(key)) {
      command.setResponse(Coder.getEmptyArrayResponse(context.getByteBufAllocator()));
      return;
    }

    int startIndex = isReverse() ? 3 : 2;
    int stopIndex = isReverse() ? 2 : 3;
    boolean startInclusive = true;
    boolean stopInclusive = true;
    double start;
    double stop;

    byte[] startArray = commandElems.get(startIndex);
    byte[] stopArray = commandElems.get(stopIndex);
    String startString = Coder.bytesToString(startArray);
    String stopString = Coder.bytesToString(stopArray);

    if (startArray[0] == Coder.OPEN_BRACE_ID) {
      startString = startString.substring(1);
      startInclusive = false;
    }
    if (stopArray[0] == Coder.OPEN_BRACE_ID) {
      stopString = stopString.substring(1);
      stopInclusive = false;
    }

    try {
      start = Coder.stringToDouble(startString);
      stop = Coder.stringToDouble(stopString);
    } catch (NumberFormatException e) {
      command.setResponse(Coder.getErrorResponse(context.getByteBufAllocator(), ERROR_NOT_NUMERIC));
      return;
    }

    ZSet zset = zsetRegion.get(key);
    List<Pair<String, Double>> results = zset.getMembersInRangeByScore(start, stop, startInclusive, stopInclusive);

    if (isReverse()) {
      List<Pair<String, Double>> revResults = new ArrayList<>();
      for (int i = results.size() - 1; i >= 0; i--) {
        revResults.add(results.get(i));
      }

      results = revResults;
    }

    if (limit != -1) {
      if (offset > results.size() - 1) {
        results = Collections.emptyList();
      } else {
        results = results.subList(offset, Math.min(results.size(), offset + limit));
      }
    }

    command.setResponse(Coder.zRangeResponse(context.getByteBufAllocator(), results, withScores));
  }

  protected boolean isReverse() {
    return false;
  }

  @Override
  public String getArgsError() {
    return ArityDef.ZRANGEBYSCORE;
  }

}
