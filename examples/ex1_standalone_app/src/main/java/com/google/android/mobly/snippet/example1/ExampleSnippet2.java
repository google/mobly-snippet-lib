/*
 * Copyright (C) 2016 Google Inc.
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

package com.google.android.mobly.snippet.example1;

import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.event.Event;
import com.google.android.mobly.snippet.event.EventSnippet;
import com.google.android.mobly.snippet.rpc.AsyncRpc;
import com.google.android.mobly.snippet.rpc.Rpc;
import com.google.android.mobly.snippet.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ExampleSnippet2 implements Snippet {

    private final EventSnippet mEventQueue = EventSnippet.getInstance();

    @Rpc(description = "Returns the given string with the prefix \"bar\"")
    public String getBar(String input) {
        return "bar " + input;
    }

    @Rpc(description = "Throws an exception")
    public String throwSomething() throws IOException {
        throw new IOException("Example exception from throwSomething()");
    }

    /**
     * An rpc demonstrating event mechanism.
     *
     * Expect to see an event on the client side that looks like:
     * {
     *  'name': 'ExampleEvent',
     *  'time': <timestamp>,
     *  'data': {
     *      'exampleData': "Here's a simple event.",
     *      'secret': 42.24,
     *      'isSecretive': True
     *  }
     * }
     *
     * @param eventId
     * @throws JSONException
     */
    @AsyncRpc(description = "This call puts an event in the event queue.")
    public void tryEvent(String eventId) throws JSONException {
        Event event = new Event(eventId, "ExampleEvent");
        event.add("exampleData", "Here's a simple event.");
        event.add("secret", 42.24);
        event.add("isSecretive", true);
        mEventQueue.postEvent(event);
    }

    @Override
    public void shutdown() {}
}
