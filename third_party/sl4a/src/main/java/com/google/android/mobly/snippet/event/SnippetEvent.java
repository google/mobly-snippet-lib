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

import android.os.Bundle;
import com.google.android.mobly.snippet.rpc.JsonBuilder;
import org.json.JSONException;
import org.json.JSONObject;

/** Class used to store information from a callback event. */
public class SnippetEvent {

    // The ID used to associate an event to a callback object on the client side.
    private final String mCallbackId;
    // The name of this event, e.g. startXxxServiceOnSuccess.
    private final String mName;
    // The content of this event. We use Android's Bundle because it adheres to Android convention
    // and adding data to it does not throw checked exceptions, which makes the world a better
    // place.
    private final Bundle mData = new Bundle();

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
     * Get the internal bundle of this event.
     *
     * <p>This is the only way to add data to the event, because we can't inherit Bundle type and we
     * don't want to dup all the getter and setters of {@link Bundle}.
     *
     * @return The Bundle that holds user data for this {@link SnippetEvent}.
     */
    public Bundle getData() {
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
        result.put("data", JsonBuilder.build(mData));
        return result;
    }
}
