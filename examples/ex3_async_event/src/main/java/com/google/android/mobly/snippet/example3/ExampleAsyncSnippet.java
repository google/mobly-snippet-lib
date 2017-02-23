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

import org.json.JSONException;
import org.json.JSONObject;

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
         * Sleeps for 10s then post an {@link SnippetEvent}.
         */
        public void run() {
            try {
                Log.d("Sleeping for 10s before posting an event.");
                Thread.sleep(10000);
                SnippetEvent event = new SnippetEvent(mCallbackId, "ExampleEvent");
                event.addData("exampleData", "Here's a simple event.");
                event.addData("mSecretNumber", mSecretNumber);
                event.addData("isSecretive", true);
                JSONObject moreData = new JSONObject();
                moreData.put("evenMoreData", "More Data!");
                event.addData("moreData", moreData);
                mEventCache.postEvent(event);
            } catch (InterruptedException e) {
                Log.e("Thread sleep was interrupted: " + e);
            } catch (JSONException e) {
                Log.e("Failed to create the event to post: " + e);
            }
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
     * {
     *    'mCallbackId': <callbackID>,
     *    'name': 'ExampleEvent',
     *    'time': <timestamp>,
     *    'data': {
     *        'exampleData': "Here's a simple event.",
     *        'mSecretNumber': 22,
     *        'isSecretive': True,
     *        'moreData': {
     *            'evenMoreData': 'More Data!'
     *        }
     *    }
     * }
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
