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

import org.apache.geode.cache.asyncqueue.internal.AsyncEventQueueImpl;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.internal.cache.execute.InternalFunction;
import org.apache.geode.management.internal.cli.commands.DestroyAsyncEventQueueCommand;

/**
 * Function used by the 'destroy async-event-queue' gfsh command to destroy an asynchronous event
 * queue on a member.
 */
public class DestroyAsyncEventQueueFunction implements InternalFunction {

  private static final long serialVersionUID = -7754359270344102817L;

  @Override
  public void execute(FunctionContext context) {
    String memberId;

    DestroyAsyncEventQueueFunctionArgs aeqArgs =
        (DestroyAsyncEventQueueFunctionArgs) context.getArguments();
    String aeqId = aeqArgs.getId();
    memberId = context.getMemberName();

    try {
      AsyncEventQueueImpl aeq = (AsyncEventQueueImpl) context.getCache().getAsyncEventQueue(aeqId);
      if (aeq == null) {
        if (aeqArgs.isIfExists()) {
          context.getResultSender()
              .lastResult(new CliFunctionResult(memberId, CliFunctionResult.StatusState.OK,
                  String.format(
                      "Skipping: "
                          + DestroyAsyncEventQueueCommand.DESTROY_ASYNC_EVENT_QUEUE__AEQ_0_NOT_FOUND,
                      aeqId)));
        } else {
          context.getResultSender()
              .lastResult(new CliFunctionResult(memberId, CliFunctionResult.StatusState.ERROR,
                  String.format(
                      DestroyAsyncEventQueueCommand.DESTROY_ASYNC_EVENT_QUEUE__AEQ_0_NOT_FOUND,
                      aeqId)));
        }
      } else {
        aeq.stop();
        aeq.destroy();
        context.getResultSender()
            .lastResult(
                new CliFunctionResult(memberId, CliFunctionResult.StatusState.OK, String.format(
                    DestroyAsyncEventQueueCommand.DESTROY_ASYNC_EVENT_QUEUE__AEQ_0_DESTROYED,
                    aeqId)));
      }
    } catch (Exception e) {
      context.getResultSender().lastResult(new CliFunctionResult(memberId, e, e.getMessage()));
    }
  }

  @Override
  public String getId() {
    return DestroyAsyncEventQueueFunction.class.getName();
  }
}
