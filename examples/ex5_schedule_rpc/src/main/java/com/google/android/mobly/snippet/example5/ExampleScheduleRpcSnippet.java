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

package com.google.android.mobly.snippet.example5;

import android.content.Context;
import android.os.Handler;
import androidx.test.InstrumentationRegistry;
import android.widget.Toast;
import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.event.EventCache;
import com.google.android.mobly.snippet.event.SnippetEvent;
import com.google.android.mobly.snippet.rpc.AsyncRpc;
import com.google.android.mobly.snippet.rpc.Rpc;
import com.google.android.mobly.snippet.util.Log;

/**
 * Demonstrates how to schedule an RPC.
 */
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
         * <p>If the sleep is interrupted, a {@link SnippetEvent} signaling failure will be posted
         * instead.
         */
        @Override
        public void run() {
            Log.d("Sleeping for 10s before posting an event.");
            SnippetEvent event = new SnippetEvent(mCallbackId, mMessage);
            try {
                Thread.sleep(10000);
                showToast(mMessage);
            } catch (InterruptedException e) {
                event.getData().putBoolean("successful", false);
                event.getData().putString("reason", "Sleep was interrupted.");
                mEventCache.postEvent(event);
            }
            event.getData().putBoolean("successful", true);
            event.getData().putString("eventName", mMessage);
            mEventCache.postEvent(event);
        }
    }

    private final Context mContext;
    private final EventCache mEventCache = EventCache.getInstance();

    /**
     * Since the APIs here deal with UI, most of them have to be called in a thread that has called
     * looper.
     */
    private final Handler mHandler;

    public ExampleScheduleRpcSnippet() {
        mContext = InstrumentationRegistry.getContext();
        mHandler = new Handler(mContext.getMainLooper());
    }

    @Rpc(description = "Make a toast on screen.")
    public String makeToast(String message) throws InterruptedException {
        showToast(message);
        return "OK";
    }

    @AsyncRpc(description = "Make a toast on screen after some time.")
    public void asyncMakeToast(String callbackId, String message)
        throws Throwable {
        Runnable asyncTask = new AsyncTask(callbackId, "asyncMakeToast");
        Thread thread = new Thread(asyncTask);
        thread.start();
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

