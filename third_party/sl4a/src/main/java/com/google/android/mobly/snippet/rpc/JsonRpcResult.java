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

package com.google.android.mobly.snippet.rpc;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a JSON RPC result.
 *
 * @see http://json-rpc.org/wiki/specification
 *
 */
public class JsonRpcResult {

  private JsonRpcResult() {
    // Utility class.
  }

  public static JSONObject empty(int id) throws JSONException {
    JSONObject json = new JSONObject();
    json.put("id", id);
    json.put("result", JSONObject.NULL);
    json.put("error", JSONObject.NULL);
    return json;
  }

  public static JSONObject result(int id, Object data) throws JSONException {
    JSONObject json = new JSONObject();
    json.put("id", id);
    json.put("result", JsonBuilder.build(data));
    json.put("error", JSONObject.NULL);
    return json;
  }

  public static JSONObject error(int id, Throwable t) throws JSONException {
    StringWriter stackTraceWriter = new StringWriter();
    stackTraceWriter.write("\n-------------- Java Stacktrace ---------------\n");
    t.printStackTrace(new PrintWriter(stackTraceWriter));
    stackTraceWriter.write("----------------------------------------------");
    String stackTrace = stackTraceWriter.toString();

    JSONObject json = new JSONObject();
    json.put("id", id);
    json.put("result", JSONObject.NULL);
    json.put("error", stackTrace);
    return json;
  }
}
