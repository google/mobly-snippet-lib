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

    private static class EventSnippetException extends Exception {
        public EventSnippetException(String msg) {
            super(msg);
        }
    }

    private static final int DEFAULT_TIMEOUT_MILLISECOND = 60 * 1000;
    EventCache mEventCache = EventCache.getInstance();

    @Rpc(
        description =
                "Blocks until an event of a specified type has been received. Default timeout is 60s."
    )
    public void eventWait(String callbackId, String eventName, @Nullable Integer timeout)
            throws InterruptedException, JSONException, EventSnippetException {
        // The server side should never wait forever, so we'll use a default timeout is one is not
        // provided.
        if (timeout == null) {
            timeout = DEFAULT_TIMEOUT_MILLISECOND;
        }
        String qId = EventCache.getQueueId(callbackId, eventName);
        LinkedBlockingDeque<SnippetEvent> q = mEventCache.getEventDeque(qId);
        // Synchronize here so we don't have the conflict of q reaching max capacity during this
        // block.
        synchronized (q) {
            // Have to poll the event first then put it back if event exists because peekFirst is
            // non-blocking.
            SnippetEvent result = q.pollFirst(timeout, TimeUnit.MILLISECONDS);
            if (result != null) {
                // Put the event back to the front of the deque so it can still be consumed.
                q.putFirst(result);
                return;
            }
        }
        throw new EventSnippetException("timeout.");
    }

    @Rpc(
        description =
                "Blocks until an event of a specified type has been received. The returned event is removed from the cache. Default timeout is 60s."
    )
    public JSONObject eventWaitAndGet(
            String callbackId, String eventName, @Nullable Integer timeout)
            throws InterruptedException, JSONException, EventSnippetException {
        String qId = EventCache.getQueueId(callbackId, eventName);
        LinkedBlockingDeque<SnippetEvent> q = mEventCache.getEventDeque(qId);
        // The server side should never wait forever, so we'll use a default timeout is one is not
        // provided.
        if (timeout == null) {
            timeout = DEFAULT_TIMEOUT_MILLISECOND;
        }
        SnippetEvent result = q.pollFirst(timeout, TimeUnit.MILLISECONDS);
        if (result == null) {
            throw new EventSnippetException("timeout.");
        }
        return result.toJson();
    }

    @Rpc(
        description =
                "Gets all the events of a certain name that have been received so far. Non-blocking"
                        + ". Potentially racey since it does not guarantee no event of the same "
                        + "name will occur after the call."
    )
    public List<JSONObject> eventGetAll(String callbackId, String eventName)
            throws InterruptedException, JSONException {
        String qId = EventCache.getQueueId(callbackId, eventName);
        LinkedBlockingDeque<SnippetEvent> q = mEventCache.getEventDeque(qId);
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
        mEventCache.eventClearAll();
    }
}
