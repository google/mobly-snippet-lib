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

package com.google.android.mobly.snippet.event;

import android.support.annotation.Nullable;
import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.rpc.Rpc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;

public class EventSnippet implements Snippet {

    private static final int DEFAULT_TIMEOUT_MILLISECOND = 60 * 1000;
    EventManager mEventManager = EventManager.getInstance();

    @Rpc(
        description =
                "Blocks until an event of a specified type has been received. Default timeout is 60s."
    )
    public boolean eventWait(String callbackId, String eventName, @Nullable Integer timeout)
            throws InterruptedException, JSONException {
        String qId = EventManager.getQueueId(callbackId, eventName);
        LinkedBlockingDeque<SnippetEvent> q = mEventManager.getEventDeque(qId);
        /**
         * The server side should never wait forever, so we'll use a default timeout is one is not
         * provided.
         */
        if (timeout == null) {
            timeout = DEFAULT_TIMEOUT_MILLISECOND;
        }
        SnippetEvent result = q.pollFirst(timeout, TimeUnit.MILLISECONDS);
        /**
         * Since there's no reliable way of detecting which type of exception was thrown on the
         * client side right now, we signal failure with return false. TODO(angli): Add a way for
         * client side to identify exception type.
         */
        if (result == null) {
            return false;
        }
        q.putFirst(result);
        return true;
    }

    @Rpc(
        description =
                "Blocks until an event of a specified type has been received. The returned event is removed from the cache. Default timeout is 60s."
    )
    public JSONObject eventWaitAndGet(
            String callbackId, String eventName, @Nullable Integer timeout)
            throws InterruptedException, JSONException {
        String qId = EventManager.getQueueId(callbackId, eventName);
        LinkedBlockingDeque<SnippetEvent> q = mEventManager.getEventDeque(qId);
        /**
         * The server side should never wait forever, so we'll use a default timeout is one is not
         * provided.
         */
        if (timeout == null) {
            timeout = DEFAULT_TIMEOUT_MILLISECOND;
        }
        SnippetEvent result = q.pollFirst(timeout, TimeUnit.MILLISECONDS);
        if (result == null) {
            return null;
        }
        return result.toJson();
    }

    @Rpc(
        description =
                "Gets all the events of a certain name that have been received so far. Non-blocking."
    )
    public List<JSONObject> eventGetAll(String callbackId, String eventName)
            throws InterruptedException, JSONException {
        String qId = EventManager.getQueueId(callbackId, eventName);
        LinkedBlockingDeque<SnippetEvent> q = mEventManager.getEventDeque(qId);
        ArrayList<JSONObject> results = new ArrayList<>(q.size());
        for (SnippetEvent snippetEvent : q) {
            results.add(snippetEvent.toJson());
        }
        if (results.size() == 0) {
            return Collections.emptyList();
        }
        return results;
    }

    @Override
    public void shutdown() {
        mEventManager.eventClearAll();
    }
}
