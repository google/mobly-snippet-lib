/*
 * Copyright (C) 2016 Google Inc.
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

package com.google.android.mobly.snippet.util;

import android.content.Context;
import com.google.android.mobly.snippet.event.EventCache;
import com.google.android.mobly.snippet.event.SnippetEvent;
import com.google.android.mobly.snippet.manager.ReflectionSnippetManagerFactory;
import com.google.android.mobly.snippet.manager.SnippetManager;
import com.google.android.mobly.snippet.manager.SnippetManagerFactory;
import com.google.android.mobly.snippet.rpc.JsonBuilder;
import com.google.android.mobly.snippet.rpc.MethodDescriptor;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONArray;

/**
 * Class that implements APIs to schedule other RPCs.
 *
 * <p>If a device is required to be disconnected (e.g., USB power off), no RPCs can be made while
 * device is offline.
 *
 * <p>However, We still need snippet continue to run and execute previously scheduled RPCs
 *
 * <p>The return value of the scheduled RPC is cached in {@link EventCache} and can be retrieved
 * later after device is back online.
 */
public class ScheduleRpcUtil {

    private final SnippetManagerFactory mSnippetManagerFactory;
    private final SnippetManager receiverManager;
    private final Context mcontext;
    private final EventCache mEventCache = EventCache.getInstance();

    public ScheduleRpcUtil(Context context) {
        mcontext = context;
        mSnippetManagerFactory = new ReflectionSnippetManagerFactory(context);
        receiverManager = mSnippetManagerFactory.create(0);
    }

    /**
     * Schedule given RPC with some delay.
     *
     * @param callbackId The callback ID used to cache RPC results.
     * @param methodName The RPC name to be scheduled.
     * @param delayMs The delay in ms
     * @param params Array of the parameters to the RPC
     */
    public void scheduleRpc(
            final String callbackId,
            final String methodName,
            final long delayMs,
            final String[] params)
            throws SnippetLibException, Throwable {
        Timer timer = new Timer();
        TimerTask task =
                new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            invokeRpc(callbackId, methodName, params);
                        } catch (Throwable e) {
                            SnippetEvent event = new SnippetEvent(callbackId, methodName);
                            event.getData().putBoolean("successful", false);
                            event.getData().putString("reason", e.getMessage());
                            mEventCache.postEvent(event);
                        }
                    }
                };
        timer.schedule(task, delayMs);
    }

    /**
     * Invoke the RPC to be scheduled.
     *
     * @param callbackId The callback ID used to cache RPC results.
     * @param methodName The RPC name to be scheduled.
     * @param params Array of the parameters to the RPC
     */
    protected void invokeRpc(String callbackId, String methodName, String[] params)
            throws SnippetLibException, Throwable {
        MethodDescriptor rpc = receiverManager.getMethodDescriptor(methodName);
        if (rpc == null) {
            throw new SnippetLibException("Unknown RPC: " + methodName);
        }

        JSONArray jsonParams = new JSONArray();
        if (rpc.isAsync()) {
            /** If calling an {@link AsyncRpc}, put callback ID as the first param. */
            jsonParams.put(callbackId);
        }

        for (String param : params) {
            jsonParams.put(param);
        }

        Object returnValue = rpc.invoke(receiverManager, jsonParams);
        if (!rpc.isAsync()) {
            // Only cache event for normal RPCs, Async RPC will cache events by itself
            SnippetEvent event = new SnippetEvent(callbackId, methodName);
            event.getData().putBoolean("successful", true);
            event.getData().putString("reason", "");
            event.getData().putString("result", JsonBuilder.build(returnValue).toString());
            mEventCache.postEvent(event);
        }
    }
}
