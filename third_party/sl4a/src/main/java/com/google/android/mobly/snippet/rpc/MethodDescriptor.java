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

import android.content.Intent;
import android.net.Uri;
import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.manager.SnippetManager;
import com.google.android.mobly.snippet.util.AndroidUtil;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** An adapter that wraps {@code Method}. */
public final class MethodDescriptor {
    private final Method mMethod;
    private final Class<? extends Snippet> mClass;

    public MethodDescriptor(Class<? extends Snippet> clazz, Method method) {
        mClass = clazz;
        mMethod = method;
    }

    @Override
    public String toString() {
        return mMethod.getDeclaringClass().getCanonicalName() + "." + mMethod.getName();
    }

    /** Collects all methods with {@code RPC} annotation from given class. */
    public static Collection<MethodDescriptor> collectFrom(Class<? extends Snippet> clazz) {
        List<MethodDescriptor> descriptors = new ArrayList<MethodDescriptor>();
        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(Rpc.class)
                    || method.isAnnotationPresent(AsyncRpc.class)) {
                descriptors.add(new MethodDescriptor(clazz, method));
            }
        }
        return descriptors;
    }

    /**
     * Invokes the call that belongs to this object with the given parameters. Wraps the response
     * (possibly an exception) in a JSONObject.
     *
     * @param parameters {@code JSONArray} containing the parameters
     * @return result
     * @throws Throwable the exception raised from executing the RPC method.
     */
    public Object invoke(SnippetManager manager, final JSONArray parameters) throws Throwable {
        final Type[] parameterTypes = getGenericParameterTypes();
        final Object[] args = new Object[parameterTypes.length];

        if (parameters.length() > args.length) {
            throw new RpcError("Too many parameters specified.");
        }

        for (int i = 0; i < args.length; i++) {
            final Type parameterType = parameterTypes[i];
            if (i < parameters.length()) {
                args[i] = convertParameter(parameters, i, parameterType);
            } else {
                throw new RpcError("Argument " + (i + 1) + " is not present");
            }
        }

        return manager.invoke(mClass, mMethod, args);
    }

    /**
     * Converts a parameter from JSON into a Java Object.
     *
     * @return TODO
     */
    // TODO(damonkohler): This signature is a bit weird (auto-refactored). The obvious alternative
    // would be to work on one supplied parameter and return the converted parameter. However, that's
    // problematic because you lose the ability to call the getXXX methods on the JSON array.
    //@VisibleForTesting
    static Object convertParameter(final JSONArray parameters, int index, Type type)
            throws JSONException, RpcError {
        try {
            // We must handle null and numbers explicitly because we cannot magically cast them. We
            // also need to convert implicitly from numbers to bools.
            if (parameters.isNull(index)) {
                return null;
            } else if (type == Boolean.class || type == boolean.class) {
                try {
                    return parameters.getBoolean(index);
                } catch (JSONException e) {
                    return new Boolean(parameters.getInt(index) != 0);
                }
            } else if (type == Long.class || type == long.class) {
                return parameters.getLong(index);
            } else if (type == Double.class || type == double.class) {
                return parameters.getDouble(index);
            } else if (type == Integer.class || type == int.class) {
                return parameters.getInt(index);
            } else if (type == Intent.class) {
                return buildIntent(parameters.getJSONObject(index));
            } else if (type == Integer[].class || type == int[].class) {
                JSONArray list = parameters.getJSONArray(index);
                Integer[] result = new Integer[list.length()];
                for (int i = 0; i < list.length(); i++) {
                    result[i] = list.getInt(i);
                }
                return result;
            } else if (type == Byte.class || type == byte[].class) {
                JSONArray list = parameters.getJSONArray(index);
                byte[] result = new byte[list.length()];
                for (int i = 0; i < list.length(); i++) {
                    result[i] = (byte) list.getInt(i);
                }
                return result;
            } else if (type == String[].class) {
                JSONArray list = parameters.getJSONArray(index);
                String[] result = new String[list.length()];
                for (int i = 0; i < list.length(); i++) {
                    result[i] = list.getString(i);
                }
                return result;
            } else if (type == JSONObject.class) {
                return parameters.getJSONObject(index);
            } else {
                // Magically cast the parameter to the right Java type.
                return ((Class<?>) type).cast(parameters.get(index));
            }
        } catch (ClassCastException e) {
            throw new RpcError(
                    "Argument "
                            + (index + 1)
                            + " should be of type "
                            + ((Class<?>) type).getSimpleName()
                            + ", but is of type "
                            + parameters.get(index).getClass().getSimpleName());
        }
    }

    public static Object buildIntent(JSONObject jsonObject) throws JSONException {
        Intent intent = new Intent();
        if (jsonObject.has("action")) {
            intent.setAction(jsonObject.getString("action"));
        }
        if (jsonObject.has("data") && jsonObject.has("type")) {
            intent.setDataAndType(
                    Uri.parse(jsonObject.optString("data", null)),
                    jsonObject.optString("type", null));
        } else if (jsonObject.has("data")) {
            intent.setData(Uri.parse(jsonObject.optString("data", null)));
        } else if (jsonObject.has("type")) {
            intent.setType(jsonObject.optString("type", null));
        }
        if (jsonObject.has("packagename") && jsonObject.has("classname")) {
            intent.setClassName(
                    jsonObject.getString("packagename"), jsonObject.getString("classname"));
        }
        if (jsonObject.has("flags")) {
            intent.setFlags(jsonObject.getInt("flags"));
        }
        if (!jsonObject.isNull("extras")) {
            AndroidUtil.putExtrasFromJsonObject(jsonObject.getJSONObject("extras"), intent);
        }
        if (!jsonObject.isNull("categories")) {
            JSONArray categories = jsonObject.getJSONArray("categories");
            for (int i = 0; i < categories.length(); i++) {
                intent.addCategory(categories.getString(i));
            }
        }
        return intent;
    }

    public String getName() {
        return mMethod.getName();
    }

    public Type[] getGenericParameterTypes() {
        return mMethod.getGenericParameterTypes();
    }

    public boolean isAsync() {
        return mMethod.isAnnotationPresent(AsyncRpc.class);
    }

    public Class<? extends Snippet> getSnippetClass() {
        return mClass;
    }

    private String getAnnotationDescription() {
        if (isAsync()) {
            AsyncRpc annotation = mMethod.getAnnotation(AsyncRpc.class);
            return annotation.description();
        }
        Rpc annotation = mMethod.getAnnotation(Rpc.class);
        return annotation.description();
    }
    /**
     * Returns a human-readable help text for this RPC, based on annotations in the source code.
     *
     * @return derived help string
     */
    public String getHelp() {
        StringBuilder paramBuilder = new StringBuilder();
        Class<?>[] parameterTypes = mMethod.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i != 0) {
                paramBuilder.append(", ");
            }
            paramBuilder.append(parameterTypes[i].getSimpleName());
        }
        String help =
                String.format(
                        "%s %s(%s) returns %s  // %s",
                        isAsync() ? "@AsyncRpc" : "@Rpc",
                        mMethod.getName(),
                        paramBuilder,
                        mMethod.getReturnType().getSimpleName(),
                        getAnnotationDescription());
        return help;
    }
}
