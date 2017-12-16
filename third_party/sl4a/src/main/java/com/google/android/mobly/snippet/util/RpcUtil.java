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

import com.google.android.mobly.snippet.event.EventCache;
import com.google.android.mobly.snippet.event.SnippetEvent;
import com.google.android.mobly.snippet.manager.SnippetManager;
import com.google.android.mobly.snippet.rpc.JsonRpcResult;
import com.google.android.mobly.snippet.rpc.MethodDescriptor;
import com.google.android.mobly.snippet.rpc.RpcError;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
public class RpcUtil {
    // RPC ID is used for reporting responses back to the client. However, the results of
    // scheduled RPCs are reported back to the client via events instead of through synchronous
    // responses, so the RPC ID is unused. We pass an arbitrary value of 0.
    private static final int DEFAULT_ID = 0;
    private final SnippetManager mReceiverManager;
    private final EventCache mEventCache = EventCache.getInstance();

    public RpcUtil() {
        mReceiverManager = SnippetManager.getInstance();
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
            final JSONArray params)
            throws Throwable {
        Timer timer = new Timer();
        TimerTask task =
                new TimerTask() {
                    @Override
                    public void run() {
                        SnippetEvent event = new SnippetEvent(callbackId, methodName);
                        try {
                            JSONObject obj = invokeRpc(methodName, params, DEFAULT_ID, callbackId);
                            // Cache RPC method return value.
                            for (int i = 0; i < obj.names().length(); i++) {
                                String key = obj.names().getString(i);
                                event.getData().putString(key, obj.get(key).toString());
                            }
                        } catch (JSONException e) {
                            String stackTrace = JsonRpcResult.getStackTrace(e);
                            event.getData().putString("error", stackTrace);
                        } finally {
                            mEventCache.postEvent(event);
                        }
                    }
                };
        timer.schedule(task, delayMs);
    }

    /**
     * Invoke the RPC.
     *
     * @param methodName The RPC name to be invoked.
     * @param params Array of the parameters to the RPC
     * @param id The ID that identifies an RPC
     * @param UID Globally unique session ID.
     */
    public JSONObject invokeRpc(String methodName, JSONArray params, int id, Integer UID)
            throws JSONException {
        return invokeRpc(methodName, params, id, String.format(Locale.US, "%d-%d", UID, id));
    }

    /**
     * Invoke the RPC.
     *
     * @param methodName The RPC name to be invoked.
     * @param params Array of the parameters to the RPC
     * @param id The ID that identifies an RPC
     * @param callbackId The callback ID used to cache RPC results.
     */
    public JSONObject invokeRpc(String methodName, JSONArray params, int id, String callbackId)
            throws JSONException {
        MethodDescriptor rpc = mReceiverManager.getMethodDescriptor(methodName);
        if (rpc == null) {
            return JsonRpcResult.error(id, new RpcError("Unknown RPC: " + methodName));
        }
        try {
            JSONArray newParams = new JSONArray();
            /** If calling an {@link AsyncRpc}, put the message ID as the first param. */
            if (rpc.isAsync()) {
                newParams.put(callbackId);
                for (int i = 0; i < params.length(); i++) {
                    newParams.put(params.get(i));
                }
                Object returnValue = rpc.invoke(mReceiverManager, newParams);
                return JsonRpcResult.callback(id, returnValue, callbackId);
            } else {
                Object returnValue = rpc.invoke(mReceiverManager, params);
                return JsonRpcResult.result(id, returnValue);
            }
        } catch (Throwable t) {
            Log.e("Invocation error.", t);
            return JsonRpcResult.error(id, t);
        }
    }
}
