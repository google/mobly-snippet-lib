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

import org.json.JSONException;
import org.json.JSONObject;

/** Class used to store information from a callback event. */
public class SnippetEvent {

    // The ID used to associate an event to a callback object on the client side.
    private final String mCallbackId;
    // The name of this event, e.g. startXxxServiceOnSuccess.
    private final String mName;
    // The content of this event.
    private final JSONObject mData = new JSONObject();

    private final long mCreationTime;

    /**
     * Constructs an {@link SnippetEvent} object.
     *
     * <p>The object is used to store information from a callback method associated with a call to
     * an {@link com.google.android.mobly.snippet.rpc.AsyncRpc} method.
     *
     * @param callbackId The callbackId passed to the {@link
     *     com.google.android.mobly.snippet.rpc.AsyncRpc} method.
     * @param name The name of the event.
     */
    public SnippetEvent(String callbackId, String name) {
        if (callbackId == null) {
            throw new IllegalArgumentException("SnippetEvent's callback ID shall not be null.");
        }
        if (name == null) {
            throw new IllegalArgumentException("SnippetEvent's name shall not be null.");
        }
        mCallbackId = callbackId;
        mName = name;
        mCreationTime = System.currentTimeMillis();
    }

    public String getCallbackId() {
        return mCallbackId;
    }

    public String getName() {
        return mName;
    }

    /**
     * Add serializable data to the Event.
     *
     * <p>This is usually for information passed by the original callback API. The data has to be
     * JSON serializable so it can be transferred to the client side.
     *
     * @param name Name of the data set.
     * @param data Content of the data.
     * @throws JSONException
     */
    public void addData(String name, Object data) throws JSONException {
        mData.put(name, data);
    }

    private JSONObject getData() {
        return mData;
    }

    public long getCreationTime() {
        return mCreationTime;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject result = new JSONObject();
        result.put("callbackId", getCallbackId());
        result.put("name", getName());
        result.put("time", getCreationTime());
        result.put("data", getData());
        return result;
    }
}
