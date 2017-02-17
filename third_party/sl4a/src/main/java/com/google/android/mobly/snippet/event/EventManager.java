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
import com.google.android.mobly.snippet.util.Log;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Manage the event queue.
 *
 * <p>EventManager APIs interact with the Event Queue (a data buffer containing up to 1024 event
 * entries). The Event Queue provides a useful means of recording background events (such as sensor
 * data) when the phone is busy with foreground activities.
 */
public class EventManager implements Snippet {
    private final String EVENT_DEQUE_ID_TEMPLATE = "%s|%s";
    private final int DEFAULT_TIMEOUT_MILISECOND = 60 * 1000;
    /**
     * The maximum length of the event queue. Old events will be discarded when this limit is
     * exceeded.
     */
    private static final int MAX_DEQUE_SIZE = 1024;

    /**
     * A Map with each value being the queue for a particular type of event, and the key being the
     * unique ID of the queue. The ID is composed of a callback ID and an event's name.
     *
     * <p>For all user-facing purposes, the
     */
    private final Map<String, LinkedBlockingDeque<Event>> mEventDeques = new ConcurrentHashMap<>();

    private static EventManager mEventManager;

    private EventManager() {}

    public static EventManager getInstance() {
        if (mEventManager == null) {
            mEventManager = new EventManager();
        }
        return mEventManager;
    }

    private String getQueueId(String callbackId, String name) {
        return String.format(EVENT_DEQUE_ID_TEMPLATE, callbackId, name);
    }

    private LinkedBlockingDeque<Event> getEventDeque(String qId) {
        synchronized (mEventDeques) {
            if (mEventDeques.containsKey(qId)) {
                return mEventDeques.get(qId);
            }
            LinkedBlockingDeque<Event> newQueue = new LinkedBlockingDeque<>();
            mEventDeques.put(qId, newQueue);
            return newQueue;
        }
    }

    @Rpc(description = "Clears all cached events.")
    public void eventClearAll() {
        synchronized (mEventDeques) {
            mEventDeques.clear();
        }
    }

    @Rpc(description = "Blocks until an event of a specified type has been received, or timeout")
    public boolean wait(String callbackId, String eventName, @Nullable Integer timeout)
            throws InterruptedException, JSONException {
        String qId = getQueueId(callbackId, eventName);
        LinkedBlockingDeque<Event> q = getEventDeque(qId);
        /**
         * The server side should never wait forever, so we'll use a default timeout is one is not
         * provided.
         */
        if (timeout == null) {
            timeout = DEFAULT_TIMEOUT_MILISECOND;
        }
        Event result = q.pollFirst(timeout, TimeUnit.MILLISECONDS);
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
                "Blocks until an event of a specified type has been received. The returned event is removed from the cache."
    )
    public JSONObject waitAndGet(String callbackId, String eventName, @Nullable Integer timeout)
            throws InterruptedException, JSONException {
        String qId = getQueueId(callbackId, eventName);
        LinkedBlockingDeque<Event> q = getEventDeque(qId);
        /**
         * The server side should never wait forever, so we'll use a default timeout is one is not
         * provided.
         */
        if (timeout == null) {
            timeout = DEFAULT_TIMEOUT_MILISECOND;
        }
        Event result = q.pollFirst(timeout, TimeUnit.MILLISECONDS);
        if (result == null) {
            return null;
        }
        return result.toJson();
    }

    @Rpc(
        description =
                "Gets all the events of a certain name that have been received so far. Non-blocking."
    )
    public ArrayList<JSONObject> getAll(String callbackId, String eventName)
            throws InterruptedException, JSONException {
        String qId = getQueueId(callbackId, eventName);
        LinkedBlockingDeque<Event> q = getEventDeque(qId);
        ArrayList<JSONObject> results = new ArrayList<>();
        for (Event event : q) {
            results.add(event.toJson());
        }
        if (results.size() == 0) {
            return null;
        }
        return results;
    }
    /**
     * Post an event to the event cache.
     *
     * <p>Snippet classes should use this method to post events.
     *
     * @param event The event to post.
     */
    public void postEvent(Event event) {
        String qId = getQueueId(event.getCallbackId(), event.getName());
        Queue<Event> q = getEventDeque(qId);
        q.add(event);
        Log.v(String.format("postEvent(%s)", qId));
    }

    @Override
    public void shutdown() {
        eventClearAll();
    }
}
