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

import com.google.android.mobly.snippet.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Manage the event queue.
 *
 * <p>EventManager APIs interact with the SnippetEvent cache - a data structure that holds {@link
 * SnippetEvent} objects posted from snippet classes. The SnippetEvent cache provides a useful means
 * of recording background events (such as sensor data) when the phone is busy with foreground
 * activities.
 */
public class EventManager {
    private static final String EVENT_DEQUE_ID_TEMPLATE = "%s|%s";

    /**
     * A Map with each value being the queue for a particular type of event, and the key being the
     * unique ID of the queue. The ID is composed of a callback ID and an event's name.
     */
    private final Map<String, LinkedBlockingDeque<SnippetEvent>> mEventDeques = new HashMap<>();

    private static volatile EventManager mEventManager;

    private EventManager() {}

    public static synchronized EventManager getInstance() {
        if (mEventManager == null) {
            mEventManager = new EventManager();
        }
        return mEventManager;
    }

    public static String getQueueId(String callbackId, String name) {
        return String.format(EVENT_DEQUE_ID_TEMPLATE, callbackId, name);
    }

    public LinkedBlockingDeque<SnippetEvent> getEventDeque(String qId) {
        synchronized (mEventDeques) {
            LinkedBlockingDeque<SnippetEvent> eventDeque = mEventDeques.get(qId);
            if (eventDeque == null) {
                eventDeque = new LinkedBlockingDeque<>();
                mEventDeques.put(qId, eventDeque);
            }
            return eventDeque;
        }
    }

    /**
     * Post an {@link SnippetEvent} object to the Event cache.
     *
     * <p>Snippet classes should use this method to post events.
     *
     * @param snippetEvent The snippetEvent to post.
     */
    public void postEvent(SnippetEvent snippetEvent) {
        String qId = getQueueId(snippetEvent.getCallbackId(), snippetEvent.getName());
        Queue<SnippetEvent> q = getEventDeque(qId);
        q.add(snippetEvent);
        Log.v(String.format("postEvent(%s)", qId));
    }

    public void eventClearAll() {
        synchronized (mEventDeques) {
            mEventDeques.clear();
        }
    }
}
