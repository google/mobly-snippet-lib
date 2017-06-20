/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.mobly.snippet.schedulerpc;

import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.rpc.AsyncRpc;
import com.google.android.mobly.snippet.util.RpcUtil;
import org.json.JSONArray;

/** Snippet that provides {@link AsyncRpc} to schedule other RPCs. */
public class ScheduleRpcSnippet implements Snippet {

    private final RpcUtil mRpcUtil;

    public ScheduleRpcSnippet() {
        mRpcUtil = new RpcUtil();
    }

    @AsyncRpc(description = "Delay the given RPC by provided milli-seconds.")
    public void scheduleRpc(
            String callbackId, String methodName, long delayTimerMs, JSONArray params)
            throws Throwable {
        mRpcUtil.scheduleRpc(callbackId, methodName, delayTimerMs, params);
    }

    @Override
    public void shutdown() {}
}
