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

package com.google.android.mobly.snippet.example4;

import android.content.Context;
import android.os.Handler;
import android.support.test.InstrumentationRegistry;
import android.widget.Toast;
import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.event.EventCache;
import com.google.android.mobly.snippet.event.SnippetEvent;
import com.google.android.mobly.snippet.rpc.AsyncRpc;
import com.google.android.mobly.snippet.rpc.Rpc;
import com.google.android.mobly.snippet.util.Log;
import com.google.android.mobly.snippet.util.ScheduleRpcUtil;
import com.google.android.mobly.snippet.util.SnippetLibException;
import java.lang.Thread;
import java.util.List;
import java.util.UUID;

public class ExampleScheduleRpcSnippet implements Snippet {

    /**
     * This is a sample asynchronous task.
     *
     * In real world use cases, it can be a {@link android.content.BroadcastReceiver}, a Listener,
     * or any other kind asynchronous callback class.
     */
    public class AsyncTask implements Runnable {

        private final String mCallbackId;
        private final String mMessage;

        public AsyncTask(String callbackId, String message) {
            this.mCallbackId = callbackId;
            this.mMessage = message;
        }

        /**
         * Sleeps for 10s then make toast and post a {@link SnippetEvent} with some data.
         *
         * If the sleep is interrupted, a {@link SnippetEvent} signaling failure will be posted
         * instead.
         */
        public void run() {
            Log.d("Sleeping for 10s before posting an event.");
            SnippetEvent event = new SnippetEvent(mCallbackId, this.mMessage);
            try {
                Thread.sleep(10000);
		showToast(this.mMessage);
            } catch (InterruptedException e) {
                event.getData().putBoolean("successful", false);
                event.getData().putString("reason", "Sleep was interrupted.");
                mEventCache.postEvent(event);
            }
            event.getData().putBoolean("successful", true);
            event.getData().putString("result", "OK");
            event.getData().putString("eventName", this.mMessage);
            mEventCache.postEvent(event);
        }
    }

    private final Context mContext;
    private final EventCache mEventCache = EventCache.getInstance();
    private ScheduleRpcUtil mScheduleRpcUtil;

    /**
     * Since the APIs here deal with UI, most of them have to be called in a thread that has called
     * looper.
     */
    private final Handler mHandler;

    public ExampleScheduleRpcSnippet() {
        mContext = InstrumentationRegistry.getContext();
        mScheduleRpcUtil = new ScheduleRpcUtil(mContext);
	mHandler = new Handler(mContext.getMainLooper());
    }

    @Rpc(description = "Make a toast on screen.")
    public String makeToast(String message) throws InterruptedException {
	showToast(message);
        return "OK";
    }

    @AsyncRpc(description = "Make a toast on screen after some time.")
    public void asyncMakeToast(String callbackId, String message)
        throws SnippetLibException, Throwable {
        Runnable asyncTask = new AsyncTask(callbackId, "asyncMakeToast");
        Thread thread = new Thread(asyncTask);
        thread.start();
    }

    @AsyncRpc(description = "Delay the given RPC by provided milli-seconds.")
    public void scheduleRpc(
        String callbackId, String methodName, long delayTimerMs, String[] params)
        throws SnippetLibException, Throwable {
        Log.i("scheduleTestActionSnippetRpc: ");
        Log.w("scheduleTestActionSnippetRpc: ");
        mScheduleRpcUtil.scheduleRpc(callbackId, methodName, delayTimerMs, params);
    }

    @Override
    public void shutdown() {}

    private void showToast(final String message) {
        mHandler.post(
	    new Runnable() {
	        @Override
		public void run() {
		    Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
		}
            });
    }
}

