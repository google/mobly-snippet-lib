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

package com.google.android.mobly.snippet.example3;

import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.event.EventCache;
import com.google.android.mobly.snippet.event.SnippetEvent;
import com.google.android.mobly.snippet.rpc.AsyncRpc;
import com.google.android.mobly.snippet.util.Log;

public class ExampleAsyncSnippet implements Snippet {

    private final EventCache mEventCache = EventCache.getInstance();

    /**
     * This is a sample asynchronous task.
     *
     * In real world use cases, it can be a {@link android.content.BroadcastReceiver}, a Listener,
     * or any other kind asynchronous callback class.
     */
    public class AsyncTask implements Runnable {

        private final String mCallbackId;
        private final int mSecretNumber;

        public AsyncTask(String callbackId, int secreteNumber) {
            this.mCallbackId = callbackId;
            this.mSecretNumber = secreteNumber;
        }

        /**
         * Sleeps for 10s then post a {@link SnippetEvent} with some data.
         *
         * If the sleep is interrupted, a {@link SnippetEvent} signaling failure will be posted instead.
         */
        public void run() {
            Log.d("Sleeping for 10s before posting an event.");
            SnippetEvent event = new SnippetEvent(mCallbackId, "AsyncTaskResult");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                event.getData().putBoolean("successful", false);
                event.getData().putString("reason", "Sleep was interrupted.");
                mEventCache.postEvent(event);
            }
            event.getData().putBoolean("successful", true);
            event.getData().putString("exampleData", "Here's a simple event.");
            event.getData().putInt("secretNumber", mSecretNumber);
            mEventCache.postEvent(event);
        }
    }

    /**
     * An Rpc method demonstrating the async event mechanism.
     *
     * This call returns immediately, but starts a task in a separate thread which will post an
     * event 10s after the task was started.
     *
     * Expect to see an event on the client side that looks like:
     *
     *    {
     *        'callbackId': '2-1',
     *        'name': 'AsyncTaskResult',
     *        'time': 20460228696,
     *        'data': {
     *            'exampleData': "Here's a simple event.",
     *            'successful': True,
     *            'secretNumber': 12
     *        }
     *    }
     *
     * @param callbackId The ID that should be used to tag {@link SnippetEvent} objects triggered by
     *                   this method.
     * @throws InterruptedException
     */
    @AsyncRpc(description = "This triggers an async event and returns.")
    public void tryEvent(String callbackId, int secretNumber) throws InterruptedException {
        Runnable asyncTask = new AsyncTask(callbackId, secretNumber);
        Thread thread = new Thread(asyncTask);
        thread.start();
    }
    @Override
    public void shutdown() {}
}
