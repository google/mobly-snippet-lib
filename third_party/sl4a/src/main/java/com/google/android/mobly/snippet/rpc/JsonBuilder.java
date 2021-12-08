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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonBuilder {

    private JsonBuilder() {}

    @SuppressWarnings("unchecked")
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
            return buildJsonMap((Map<String, ?>) data);
        }
        if (data instanceof ParcelUuid) {
            return data.toString();
        }
        // TODO(xpconanfan): Deprecate the following default non-primitive type builders.
        if (data instanceof InetSocketAddress) {
            return buildInetSocketAddress((InetSocketAddress) data);
        }
        if (data instanceof InetAddress) {
            return buildInetAddress((InetAddress) data);
        }
        if (data instanceof URL) {
            return buildURL((URL) data);
        }
        if (data instanceof byte[]) {
            JSONArray result = new JSONArray();
            for (byte b : (byte[]) data) {
                result.put(b & 0xFF);
            }
            return result;
        }
        if (data instanceof Object[]) {
            return buildJSONArray((Object[]) data);
        }
        // Try with custom converter provided by user.
        Object result = SnippetObjectConverterManager.getInstance().objectToJson(data);
        if (result != null) {
            return result;
        }
        return data.toString();
    }

    private static Object buildInetAddress(InetAddress data) {
        JSONArray address = new JSONArray();
        address.put(data.getHostName());
        address.put(data.getHostAddress());
        return address;
    }

    private static Object buildInetSocketAddress(InetSocketAddress data) {
        JSONArray address = new JSONArray();
        address.put(data.getHostName());
        address.put(data.getPort());
        return address;
    }

    private static JSONArray buildJSONArray(Object[] data) throws JSONException {
        JSONArray result = new JSONArray();
        for (Object o : data) {
            result.put(build(o));
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

    private static JSONObject buildJsonMap(Map<String, ?> map) throws JSONException {
        JSONObject result = new JSONObject();
        for (Entry<String, ?> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                key = "";
            }
            result.put(key, build(entry.getValue()));
        }
        return result;
    }

    private static Object buildURL(URL data) throws JSONException {
        JSONObject url = new JSONObject();
        url.put("Authority", data.getAuthority());
        url.put("Host", data.getHost());
        url.put("Path", data.getPath());
        url.put("Port", data.getPort());
        url.put("Protocol", data.getProtocol());
        return url;
    }
}
