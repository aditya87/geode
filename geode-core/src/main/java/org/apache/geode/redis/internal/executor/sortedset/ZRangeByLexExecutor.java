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
import org.apache.geode.redis.internal.Pair;
import org.apache.geode.redis.internal.RedisConstants.ArityDef;
import org.apache.geode.redis.internal.ZSet;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ZRangeByLexExecutor extends SortedSetExecutor {

  private final String ERROR_NOT_NUMERIC = "The index provided is not numeric";

  private final String ERROR_ILLEGAL_SYNTAX =
      "The min and max strings must either start with a (, [ or be - or +";

  private final String ERROR_LIMIT = "The offset and count cannot be negative";

  @Override
  public void executeCommand(Command command, ExecutionHandlerContext context) {
    List<byte[]> commandElems = command.getProcessedCommand();

    if (commandElems.size() < 4) {
      command
          .setResponse(Coder.getErrorResponse(context.getByteBufAllocator(), ArityDef.ZRANGEBYLEX));
      return;
    }

    boolean existsLimit = false;

    if (commandElems.size() >= 7) {
      byte[] fifthElem = commandElems.get(4);
      existsLimit = Coder.bytesToString(fifthElem).equalsIgnoreCase("LIMIT");
    }

    int offset = 0;
    int limit = 0;

    if (existsLimit) {
      try {
        byte[] offsetArray = commandElems.get(5);
        byte[] limitArray = commandElems.get(6);
        offset = Coder.bytesToInt(offsetArray);
        limit = Coder.bytesToInt(limitArray);
      } catch (NumberFormatException e) {
        command
            .setResponse(Coder.getErrorResponse(context.getByteBufAllocator(), ERROR_NOT_NUMERIC));
        return;
      }
    }

    if (offset < 0 || limit < 0) {
      command.setResponse(Coder.getErrorResponse(context.getByteBufAllocator(), ERROR_LIMIT));
      return;
    }

    ByteArrayWrapper key = command.getKey();
    Region<ByteArrayWrapper, ZSet> zsetRegion = context.getRegionProvider().getZsetRegion();

    if (!zsetRegion.containsKey(key)) {
      command.setResponse(Coder.getEmptyArrayResponse(context.getByteBufAllocator()));
      return;
    }

    boolean minInclusive = false;
    boolean maxInclusive = false;

    byte[] minArray = commandElems.get(2);
    byte[] maxArray = commandElems.get(3);
    String startString = Coder.bytesToString(minArray);
    String stopString = Coder.bytesToString(maxArray);
    if (minArray[0] == Coder.OPEN_BRACE_ID) {
      startString = startString.substring(1);
      minInclusive = false;
    } else if (minArray[0] == Coder.OPEN_BRACKET_ID) {
      startString = startString.substring(1);
      minInclusive = true;
    } else if (minArray[0] != Coder.HYPHEN_ID) {
      command.setResponse(Coder.getErrorResponse(context.getByteBufAllocator(), ERROR_ILLEGAL_SYNTAX));
      return;
    }

    if (maxArray[0] == Coder.OPEN_BRACE_ID) {
      stopString = stopString.substring(1);
      maxInclusive = false;
    } else if (maxArray[0] == Coder.OPEN_BRACKET_ID) {
      stopString = stopString.substring(1);
      maxInclusive = true;
    } else if (maxArray[0] != Coder.PLUS_ID) {
      command.setResponse(Coder.getErrorResponse(context.getByteBufAllocator(), ERROR_ILLEGAL_SYNTAX));
      return;
    }

    ZSet zset = zsetRegion.get(key);
    List<Pair<String, Double>> results = zset.getMembersInRangeByLex(startString, stopString,
            minInclusive, maxInclusive);

    if (existsLimit) {
      if (offset > results.size() - 1) {
        results = Collections.emptyList();
      } else {
        results = results.subList(offset, Math.min(results.size(), offset + limit));
      }
    }

    List<String> members = results.stream().map(r -> r.fst).collect(Collectors.toList());
    respondBulkStrings(command, context, members);
  }
}
