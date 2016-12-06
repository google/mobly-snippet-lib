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

import android.content.Context;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.android.mobly.snippet.util.Log;

public abstract class SnippetManager {

    private final Map<Class<? extends Snippet>, Snippet> mReceivers;

    /**
     * A map of strings to known RPCs.
     */
    private final Map<String, MethodDescriptor> mKnownRpcs = new HashMap<String, MethodDescriptor>();

    public SnippetManager(Collection<Class<? extends Snippet>> classList) {
        mReceivers = new HashMap<>();
        for (Class<? extends Snippet> receiverClass : classList) {
            mReceivers.put(receiverClass, null);
            Collection<MethodDescriptor> methodList = MethodDescriptor.collectFrom(receiverClass);
            for (MethodDescriptor m : methodList) {
                if (mKnownRpcs.containsKey(m.getName())) {
                    // We already know an RPC of the same name. We don't catch this anywhere because
                    // this is a programming error.
                    throw new RuntimeException("An RPC with the name " + m.getName()
                            + " is already known.");
                }
                mKnownRpcs.put(m.getName(), m);
            }
        }
    }

    private Snippet get(Class<? extends Snippet> clazz) {
        Snippet object = mReceivers.get(clazz);
        if (object != null) {
            return object;
        }

        Constructor<? extends Snippet> constructor;
        try {
            constructor = clazz.getConstructor();
            object = constructor.newInstance();
            mReceivers.put(clazz, object);
        } catch (Exception e) {
            Log.e(e);
        }

        return object;
    }

    public MethodDescriptor getMethodDescriptor(String methodName) {
        return mKnownRpcs.get(methodName);
    }

    public SortedSet<String> getMethodNames() {
        return new TreeSet<>(mKnownRpcs.keySet());
    }

    public Object invoke(Class<? extends Snippet> clazz, Method method, Object[] args)
            throws Exception {
        Snippet object = get(clazz);
        return method.invoke(object, args);
    }

    public void shutdown() {
        for (Snippet receiver : mReceivers.values()) {
            try {
                if (receiver != null) {
                    receiver.shutdown();
                }
            } catch (Exception e) {
                Log.e("Failed to shut down an Snippet", e);
            }
        }
    }
}
