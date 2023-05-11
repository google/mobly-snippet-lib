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
import com.google.android.mobly.snippet.manager.SnippetObjectConverterManager;
import com.google.android.mobly.snippet.util.AndroidUtil;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** An adapter that wraps {@code Method}. */
public final class MethodDescriptor {
    private static final Map<Class<?>, TypeConverter<?>> typeConverters = populateConverters();

    private final Method mMethod;
    private final Class<? extends Snippet> mClass;

    private MethodDescriptor(Class<? extends Snippet> clazz, Method method) {
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
        final Annotation[][] annotations = getParameterAnnotations();
        final Type[] parameterTypes = getGenericParameterTypes();
        final Object[] args = new Object[parameterTypes.length];

        if (parameters.length() > args.length) {
            throw new RpcError("Too many parameters specified.");
        }

        for (int i = 0; i < args.length; i++) {
            final Type parameterType = parameterTypes[i];
            if (i < parameters.length()) {
                args[i] = convertParameter(parameters, i, parameterType);
            } else if (MethodDescriptor.hasDefaultValue(Arrays.asList(annotations[i]))) {
                args[i] = MethodDescriptor.getDefaultValue(
                        parameterType, Arrays.asList(annotations[i]));
            } else if (MethodDescriptor.isOptional(Arrays.asList(annotations[i]))) {
                args[i] = MethodDescriptor.getOptionalValue(
                        parameterType, Arrays.asList(annotations[i]));
            } else {
                throw new RpcError("Argument " + (i + 1) + " is not present");
            }
        }

        return manager.invoke(mClass, mMethod, args);
    }

    /** Converts a parameter from JSON into a Java Object. */
    private static Object convertParameter(final JSONArray parameters, int index, Type type)
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
                    return parameters.getInt(index) != 0;
                }
            } else if (type == Long.class || type == long.class) {
                return parameters.getLong(index);
            } else if (type == Double.class || type == double.class) {
                return parameters.getDouble(index);
            } else if (type == Integer.class || type == int.class) {
                return parameters.getInt(index);
            } else if (type == Intent.class) {
                return buildIntent(parameters.getJSONObject(index));
            } else if (type == String.class) {
                return parameters.getString(index);
            } else if (type == Integer[].class || type == int[].class) {
                JSONArray list = parameters.getJSONArray(index);
                Integer[] result = new Integer[list.length()];
                for (int i = 0; i < list.length(); i++) {
                    result[i] = list.getInt(i);
                }
                return result;
            } else if (type == Long[].class || type == long[].class) {
                JSONArray list = parameters.getJSONArray(index);
                Long[] result = new Long[list.length()];
                for (int i = 0; i < list.length(); i++) {
                    result[i] = list.getLong(i);
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
            } else if (type == JSONArray.class) {
                return parameters.getJSONArray(index);
            } else {
                // Try any custom converter provided.
                Object object =
                        SnippetObjectConverterManager.getInstance()
                                .jsonToObject(parameters.getJSONObject(index), type);
                if (object != null) {
                    return object;
                }
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
                            + parameters.get(index).getClass().getSimpleName(),
                    e);
        }
    }

    private static Object buildIntent(JSONObject jsonObject) throws JSONException {
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

    private Type[] getGenericParameterTypes() {
        return mMethod.getGenericParameterTypes();
    }

    public boolean isAsync() {
        return mMethod.isAnnotationPresent(AsyncRpc.class);
    }

    Class<? extends Snippet> getSnippetClass() {
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

    public Annotation[][] getParameterAnnotations() {
        return mMethod.getParameterAnnotations();
    }

    /**
     * Returns a human-readable help text for this RPC, based on annotations in the source code.
     *
     * @return derived help string
     */
    String getHelp() {
        StringBuilder paramBuilder = new StringBuilder();
        Class<?>[] parameterTypes = mMethod.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i != 0) {
                paramBuilder.append(", ");
            }
            paramBuilder.append(parameterTypes[i].getSimpleName());
        }
        return String.format(
                Locale.US,
                "%s %s(%s) returns %s  // %s",
                isAsync() ? "@AsyncRpc" : "@Rpc",
                mMethod.getName(),
                paramBuilder,
                mMethod.getReturnType().getSimpleName(),
                getAnnotationDescription());
    }

    /**
     * Returns the default value for a parameter which has a default value.
     *
     * @param parameterType parameterType
     * @param annotations   annotations of the parameter
     */
    public static Object getDefaultValue(Type parameterType, Iterable<Annotation> annotations) {
        for (Annotation a : annotations) {
            if (a instanceof RpcDefault) {
                RpcDefault defaultAnnotation = (RpcDefault) a;
                TypeConverter<?> converter =
                        converterFor(parameterType, defaultAnnotation.converter());
                return converter.convert(defaultAnnotation.value());
            }
        }
        throw new IllegalStateException("No default value for " + parameterType);
    }

    /**
     * Returns null for an optional parameter.
     *
     * @param parameterType parameterType
     * @param annotations   annotations of the parameter
     */
    public static Object getOptionalValue(Type parameterType, Iterable<Annotation> annotations) {
        for (Annotation a : annotations) {
            if (a instanceof RpcOptional) {
                return null;
            }
        }
        throw new IllegalStateException("No default value for " + parameterType);
    }

    @SuppressWarnings("rawtypes")
    private static TypeConverter<?> converterFor(
            Type parameterType, Class<? extends TypeConverter> converterClass) {
        if (converterClass == TypeConverter.class) {
            TypeConverter<?> converter = typeConverters.get(parameterType);
            if (converter == null) {
                throw new IllegalArgumentException(
                        String.format("No predefined converter found for %s", parameterType));
            }
            return converter;
        }
        try {
            Constructor<?> constructor = converterClass.getConstructor(new Class<?>[0]);
            return (TypeConverter<?>) constructor.newInstance(new Object[0]);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format(
                            "Cannot create converter from %s", converterClass.getCanonicalName()),
                    e);
        }
    }

    /**
     * Determines whether or not this parameter has default value.
     *
     * @param annotations annotations of the parameter
     */
    public static boolean hasDefaultValue(Iterable<Annotation> annotations) {
        for (Annotation a : annotations) {
            if (a instanceof RpcDefault) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether or not this parameter is optional.
     *
     * @param annotations annotations of the parameter
     */
    public static boolean isOptional(Iterable<Annotation> annotations) {
        for (Annotation a : annotations) {
            if (a instanceof RpcOptional) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the converters for {@code String}, {@code Integer}, {@code Long},
     * and {@code Boolean}.
     */
    private static Map<Class<?>, TypeConverter<?>> populateConverters() {
        Map<Class<?>, TypeConverter<?>> converters = new HashMap<>();
        converters.put(String.class, new TypeConverter<String>() {
            @Override
            public String convert(String value) {
                return value;
            }
        });
        converters.put(Integer.class, new TypeConverter<Integer>() {
            @Override
            public Integer convert(String input) {
                try {
                    return Integer.decode(input);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            String.format("'%s' is not a Integer", input), e);
                }
            }
        });
        converters.put(Long.class, new TypeConverter<Long>() {
            @Override
            public Long convert(String input) {
                try {
                    return Long.decode(input);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            String.format("'%s' is not a Long", input), e);
                }
            }
        });
        converters.put(Boolean.class, new TypeConverter<Boolean>() {
            @Override
            public Boolean convert(String input) {
                if (input == null) {
                    return null;
                }
                input = input.toLowerCase(Locale.ROOT);
                if (input.equals("true")) {
                    return Boolean.TRUE;
                }
                if (input.equals("false")) {
                    return Boolean.FALSE;
                }
                throw new IllegalArgumentException(String.format("'%s' is not a Boolean", input));
            }
        });
        return converters;
    }
}
