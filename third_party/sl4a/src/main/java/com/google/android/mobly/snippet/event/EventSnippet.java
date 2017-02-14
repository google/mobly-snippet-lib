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

import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.future.FutureResult;
import com.google.android.mobly.snippet.rpc.Rpc;
import com.google.android.mobly.snippet.util.Log;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Manage the event queue.
 *
 * <p>EventSnippet APIs interact with the Event Queue (a data buffer containing up to 1024 event
 * entries). The Event Queue provides a useful means of recording background events (such as sensor
 * data) when the phone is busy with foreground activities.
 */
public class EventSnippet implements Snippet {
    /**
     * The maximum length of the event queue. Old events will be discarded when this limit is
     * exceeded.
     */
    private static final int MAX_QUEUE_SIZE = 1024;

    private final Queue<Event> mEventQueue = new ConcurrentLinkedQueue<Event>();
    private static EventSnippet mEventSnippet;

    private EventSnippet() {}

    public static EventSnippet getInstance() {
        if (mEventSnippet == null) {
            mEventSnippet = new EventSnippet();
        }
        return mEventSnippet;
    }

    @Rpc(description = "Clears all events from the event buffer.")
    public void eventClearBuffer() {
        mEventQueue.clear();
    }

    @Rpc(
        description = "Blocks until an event occurs. The returned event is removed from the buffer."
    )
    public JSONObject eventWait(Integer timeout) throws InterruptedException, JSONException {
        Event result;
        final FutureResult<Event> futureEvent = new FutureResult<Event>();
        synchronized (mEventQueue) { // Anything in queue?
            if (mEventQueue.size() > 0) {
                return mEventQueue.poll().toJson(); // return it.
            }
        }
        if (timeout != null) {
            result = futureEvent.get(timeout, TimeUnit.MILLISECONDS);
        } else {
            result = futureEvent.get();
        }
        if (result != null) {
            mEventQueue.remove(result);
        }
        return result.toJson();
    }

    public void postEvent(Event event) {
        synchronized (mEventQueue) {
            while (mEventQueue.size() >= MAX_QUEUE_SIZE) {
                mEventQueue.remove();
            }
            mEventQueue.add(event);
        }
        Log.v(String.format("postEvent(%s:%s)", event.getId(), event.getName()));
    }

    @Override
    public void shutdown() {
        mEventQueue.clear();
    }
}
