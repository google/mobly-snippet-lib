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

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.manager.ReflectionSnippetManagerFactory;
import com.google.android.mobly.snippet.manager.SnippetManager;
import com.google.android.mobly.snippet.manager.SnippetManagerFactory;
import com.google.android.mobly.snippet.rpc.AsyncRpc;
import com.google.android.mobly.snippet.util.RpcUtil;
import com.google.android.mobly.snippet.util.SnippetLibException;

/** Snippet that provides {@link AsyncRpc} to schedule other RPCs. */
public class ScheduleRpcSnippet implements Snippet {

    private final Context mContext;
    private final SnippetManagerFactory mSnippetManagerFactory;
    private final SnippetManager mReceiverManager;
    private final RpcUtil mRpcUtil;

    public ScheduleRpcSnippet() {
        mContext = InstrumentationRegistry.getContext();
        mSnippetManagerFactory = ReflectionSnippetManagerFactory.getInstance(mContext);
        mReceiverManager = mSnippetManagerFactory.getSnippetManager();
        mRpcUtil = new RpcUtil(mReceiverManager);
    }

    @AsyncRpc(description = "Delay the given RPC by provided milli-seconds.")
    public void scheduleRpc(
            String callbackId, String methodName, long delayTimerMs, String[] params)
            throws Throwable {
        if (methodName.equals("scheduleRpc")) {
            throw new SnippetLibException(
                    String.format("Cannot schedule RPC method: scheduleRpc()"));
        }
        mRpcUtil.scheduleRpc(callbackId, methodName, delayTimerMs, params);
    }

    @Override
    public void shutdown() {}
}
