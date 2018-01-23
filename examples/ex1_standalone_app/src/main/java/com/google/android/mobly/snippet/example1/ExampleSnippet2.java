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
import com.google.android.mobly.snippet.rpc.Rpc;

import com.google.android.mobly.snippet.rpc.RunOnUiThread;

import org.json.JSONArray;

import java.io.IOException;

public class ExampleSnippet2 implements Snippet {
    @Rpc(description = "Returns the given string with the prefix \"bar\"")
    public String getBar(String input) {
        return "bar " + input;
    }

    @Rpc(description = "Returns the given JSON array with the prefix \"bar\"")
    public String getJSONArray(JSONArray input) {
        return "bar " + input;
    }

    @Rpc(description = "Throws an exception")
    public String throwSomething() throws IOException {
        throw new IOException("Example exception from throwSomething()");
    }

    @Rpc(description = "Throws an exception from the main thread")
    // @RunOnUiThread makes this method execute on the main thread, but only has effect when
    // invoked as an RPC. It does not affect how this method executes if invoked directly in Java.
    // This annotation can also be applied to the constructor and the shutdown() method.
    @RunOnUiThread
    public String throwSomethingFromMainThread() throws IOException {
        throw new IOException("Example exception from throwSomethingFromMainThread()");
    }

    @Override
    public void shutdown() {}
}
