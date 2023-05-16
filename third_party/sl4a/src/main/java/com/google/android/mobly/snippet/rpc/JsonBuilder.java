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

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelUuid;
import com.google.android.mobly.snippet.manager.SnippetObjectConverterManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Builds the result for JSON RPC. */
public class JsonBuilder {

    private JsonBuilder() {}

    public static Object build(Object data) throws JSONException {
        if (data == null) {
            return JSONObject.NULL;
        }
        if (data instanceof Integer) {
            return data;
        }
        if (data instanceof Float) {
            return data;
        }
        if (data instanceof Double) {
            return data;
        }
        if (data instanceof Long) {
            return data;
        }
        if (data instanceof String) {
            return data;
        }
        if (data instanceof Boolean) {
            return data;
        }
        if (data instanceof JsonSerializable) {
            return ((JsonSerializable) data).toJSON();
        }
        if (data instanceof JSONObject) {
            return data;
        }
        if (data instanceof JSONArray) {
            return data;
        }
        if (data instanceof Set<?>) {
            List<Object> items = new ArrayList<>((Set<?>) data);
            return buildJsonList(items);
        }
        if (data instanceof Collection<?>) {
            List<Object> items = new ArrayList<>((Collection<?>) data);
            return buildJsonList(items);
        }
        if (data instanceof List<?>) {
            return buildJsonList((List<?>) data);
        }
        if (data instanceof Bundle) {
            return buildJsonBundle((Bundle) data);
        }
        if (data instanceof Intent) {
            return buildJsonIntent((Intent) data);
        }
        if (data instanceof Map<?, ?>) {
            // TODO(damonkohler): I would like to make this a checked cast if possible.
            return buildJsonMap((Map<?, ?>) data);
        }
        if (data instanceof ParcelUuid) {
            return data.toString();
        }
        if (data.getClass().isArray()) {
            return buildJSONArray(data);
        }
        // Try with custom converter provided by user.
        Object result = SnippetObjectConverterManager.getInstance().objectToJson(data);
        if (result != null) {
            return result;
        }
        return data.toString();
    }

    private static JSONArray buildJSONArray(Object data) throws JSONException {
        JSONArray result = new JSONArray();
        if (data instanceof int[]) {
            for (int i : (int []) data) {
                result.put(i);
            }
        } else if (data instanceof short[]) {
            for (short s : (short[]) data) {
                result.put(s);
            }
        } else if (data instanceof long[]) {
            for (long l : (long[]) data) {
                result.put(l);
            }
        } else if (data instanceof float[]) {
            for (float f : (float[]) data) {
                result.put(f);
            }
        } else if (data instanceof double[]) {
            for (double d : (double[]) data) {
                result.put(d);
            }
        } else if (data instanceof boolean[]) {
            for (boolean b : (boolean[]) data) {
                result.put(b);
            }
        } else if (data instanceof char[]) {
            for (char c : (char[]) data) {
                result.put(c);
            }
        } else if (data instanceof byte[]) {
            for (byte b : (byte[]) data) {
                result.put(b & 0xFF);
            }
        } else {
            for (Object o : (Object[]) data) {
                result.put(build(o));
            }
        }
        return result;
    }

    private static JSONObject buildJsonBundle(Bundle bundle) throws JSONException {
        JSONObject result = new JSONObject();
        bundle.setClassLoader(JsonBuilder.class.getClassLoader());
        for (String key : bundle.keySet()) {
            result.put(key, build(bundle.get(key)));
        }
        return result;
    }

    private static JSONObject buildJsonIntent(Intent data) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("data", data.getDataString());
        result.put("type", data.getType());
        result.put("extras", build(data.getExtras()));
        result.put("categories", build(data.getCategories()));
        result.put("action", data.getAction());
        ComponentName component = data.getComponent();
        if (component != null) {
            result.put("packagename", component.getPackageName());
            result.put("classname", component.getClassName());
        }
        result.put("flags", data.getFlags());
        return result;
    }

    private static <T> JSONArray buildJsonList(final List<T> list) throws JSONException {
        JSONArray result = new JSONArray();
        for (T item : list) {
            result.put(build(item));
        }
        return result;
    }

    private static JSONObject buildJsonMap(Map<?, ?> map) throws JSONException {
        JSONObject result = new JSONObject();
        for (Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            String keyStr = key == null ? "" : key.toString();
            result.put(keyStr, build(entry.getValue()));
        }
        return result;
    }
}
