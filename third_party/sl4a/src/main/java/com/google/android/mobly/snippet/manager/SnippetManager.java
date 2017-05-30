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

package com.google.android.mobly.snippet.manager;

import android.os.Build;
import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.rpc.MethodDescriptor;
import com.google.android.mobly.snippet.rpc.RpcMinSdk;
import com.google.android.mobly.snippet.rpc.RunOnUiThread;
import com.google.android.mobly.snippet.util.Log;
import com.google.android.mobly.snippet.util.MainThread;
import com.google.android.mobly.snippet.util.SnippetLibException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;

public class SnippetManager {
    private final Map<Class<? extends Snippet>, Snippet> mSnippets;
    /** A map of strings to known RPCs. */
    private final Map<String, MethodDescriptor> mKnownRpcs;

    private static SnippetManager mInstance = null;

    protected SnippetManager(Collection<Class<? extends Snippet>> classList) {
        // Synchronized for multiple connections on the same session. Can't use ConcurrentHashMap
        // because we have to put in a value of 'null' before the class is constructed, but
        // ConcurrentHashMap does not allow null values.
        mSnippets = Collections.synchronizedMap(new HashMap<Class<? extends Snippet>, Snippet>());
        Map<String, MethodDescriptor> knownRpcs = new HashMap<>();
        for (Class<? extends Snippet> receiverClass : classList) {
            mSnippets.put(receiverClass, null);
            Collection<MethodDescriptor> methodList = MethodDescriptor.collectFrom(receiverClass);
            for (MethodDescriptor m : methodList) {
                if (knownRpcs.containsKey(m.getName())) {
                    // We already know an RPC of the same name. We don't catch this anywhere because
                    // this is a programming error.
                    throw new RuntimeException(
                            "An RPC with the name " + m.getName() + " is already known.");
                }
                knownRpcs.put(m.getName(), m);
            }
        }
        // Does not need to be concurrent because this map is read only, so it is safe to access
        // from multiple threads. Wrap in an unmodifiableMap to enforce this.
        mKnownRpcs = Collections.unmodifiableMap(knownRpcs);
    }

    public static SnippetManager getInstance(Collection<Class<? extends Snippet>> classList) {
        if (mInstance == null) {
            mInstance = new SnippetManager(classList);
        }
        return mInstance;
    }

    public MethodDescriptor getMethodDescriptor(String methodName) {
        return mKnownRpcs.get(methodName);
    }

    public SortedSet<String> getMethodNames() {
        return new TreeSet<>(mKnownRpcs.keySet());
    }

    public Object invoke(Class<? extends Snippet> clazz, Method method, Object[] args)
            throws Throwable {
        if (method.isAnnotationPresent(RpcMinSdk.class)) {
            int requiredSdkLevel = method.getAnnotation(RpcMinSdk.class).value();
            if (Build.VERSION.SDK_INT < requiredSdkLevel) {
                throw new SnippetLibException(
                        String.format(
                                "%s requires API level %d, current level is %d",
                                method.getName(), requiredSdkLevel, Build.VERSION.SDK_INT));
            }
        }
        Snippet object;
        try {
            object = get(clazz);
            return invoke(object, method, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public void shutdown() throws Exception {
        for (final Entry<Class<? extends Snippet>, Snippet> entry : mSnippets.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            Method method = entry.getKey().getMethod("shutdown");
            if (method.isAnnotationPresent(RunOnUiThread.class)) {
                Log.d("Shutting down " + entry.getKey().getName() + " on the main thread");
                MainThread.run(
                        new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                entry.getValue().shutdown();
                                return null;
                            }
                        });
            } else {
                Log.d("Shutting down " + entry.getKey().getName());
                entry.getValue().shutdown();
            }
        }
    }

    private Snippet get(Class<? extends Snippet> clazz) throws Exception {
        Snippet snippetImpl = mSnippets.get(clazz);
        if (snippetImpl == null) {
            // First time calling an RPC for this snippet; construct an instance under lock.
            synchronized (clazz) {
                snippetImpl = mSnippets.get(clazz);
                if (snippetImpl == null) {
                    final Constructor<? extends Snippet> constructor = clazz.getConstructor();
                    if (constructor.isAnnotationPresent(RunOnUiThread.class)) {
                        Log.d("Constructing " + clazz + " on the main thread");
                        snippetImpl =
                                MainThread.run(
                                        new Callable<Snippet>() {
                                            @Override
                                            public Snippet call() throws Exception {
                                                return constructor.newInstance();
                                            }
                                        });
                    } else {
                        Log.d("Constructing " + clazz);
                        snippetImpl = constructor.newInstance();
                    }
                    mSnippets.put(clazz, snippetImpl);
                }
            }
        }
        return snippetImpl;
    }

    private Object invoke(final Snippet snippetImpl, final Method method, final Object[] args)
            throws Exception {
        if (method.isAnnotationPresent(RunOnUiThread.class)) {
            Log.d(
                    "Invoking RPC method "
                            + method.getDeclaringClass()
                            + "#"
                            + method.getName()
                            + " on the main thread");
            return MainThread.run(
                    new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            return method.invoke(snippetImpl, args);
                        }
                    });
        } else {
            Log.d("Invoking RPC method " + method.getDeclaringClass() + "#" + method.getName());
            return method.invoke(snippetImpl, args);
        }
    }
}
