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

public class Event {

    private String mId;
    private String mName;
    private JSONObject mData = new JSONObject();
    private double mCreationTime;

    public Event(String eventId, String typeName) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event name shall not be null.");
        }
        mId = eventId;
        mName = typeName;
        mCreationTime = System.currentTimeMillis();
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public void add(String name, Object data) throws JSONException {
        mData.put(name, data);
    }

    public Object getData() {
        return mData;
    }

    public double getCreationTime() {
        return mCreationTime;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject result = new JSONObject();
        result.put("name", getName());
        result.put("time", getCreationTime());
        result.put("data", getData());
        return result;
    }
}
