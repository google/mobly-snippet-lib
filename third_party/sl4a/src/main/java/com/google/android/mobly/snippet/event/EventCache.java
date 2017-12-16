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
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Manage the event queue.
 *
 * <p>EventCache APIs interact with the SnippetEvent cache - a data structure that holds {@link
 * SnippetEvent} objects posted from snippet classes. The SnippetEvent cache provides a useful means
 * of recording background events (such as sensor data) when the phone is busy with foreground
 * activities.
 */
public class EventCache {
    private static final String EVENT_DEQUE_ID_TEMPLATE = "%s|%s";
    private static final int EVENT_DEQUE_MAX_SIZE = 1024;

    // A Map with each value being the queue for a particular type of event, and the key being the
    // unique ID of the queue. The ID is composed of a callback ID and an event's name.
    private final Map<String, LinkedBlockingDeque<SnippetEvent>> mEventDeques = new HashMap<>();

    private static volatile EventCache mEventCache;

    private EventCache() {}

    public static EventCache getInstance() {
        if (mEventCache == null) {
            synchronized (EventCache.class) {
                if (mEventCache == null) {
                    mEventCache = new EventCache();
                }
            }
        }
        return mEventCache;
    }

    public static String getQueueId(String callbackId, String name) {
        return String.format(Locale.US, EVENT_DEQUE_ID_TEMPLATE, callbackId, name);
    }

    public LinkedBlockingDeque<SnippetEvent> getEventDeque(String qId) {
        synchronized (mEventDeques) {
            LinkedBlockingDeque<SnippetEvent> eventDeque = mEventDeques.get(qId);
            if (eventDeque == null) {
                eventDeque = new LinkedBlockingDeque<>(EVENT_DEQUE_MAX_SIZE);
                mEventDeques.put(qId, eventDeque);
            }
            return eventDeque;
        }
    }

    /**
     * Post an {@link SnippetEvent} object to the Event cache.
     *
     * <p>Snippet classes should use this method to post events. If EVENT_DEQUE_MAX_SIZE is reached,
     * the oldest elements will be retired until the new event could be posted.
     *
     * @param snippetEvent The snippetEvent to post to {@link EventCache}.
     */
    public void postEvent(SnippetEvent snippetEvent) {
        String qId = getQueueId(snippetEvent.getCallbackId(), snippetEvent.getName());
        Deque<SnippetEvent> q = getEventDeque(qId);
        synchronized (q) {
            while (!q.offer(snippetEvent)) {
                SnippetEvent retiredEvent = q.removeFirst();
                Log.v(
                        String.format(
                                Locale.US,
                                "Retired event %s due to deque reaching the size limit (%s).",
                                retiredEvent,
                                EVENT_DEQUE_MAX_SIZE));
            }
        }
        Log.v(String.format(Locale.US, "Posted event(%s)", qId));
    }

    /** Clears all cached events. */
    public void clearAll() {
        synchronized (mEventDeques) {
            mEventDeques.clear();
        }
    }
}
