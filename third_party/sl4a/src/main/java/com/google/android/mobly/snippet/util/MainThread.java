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

package com.google.android.mobly.snippet.util;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

public class MainThread {
    /**
     * Wraps a {@link Callable} in a {@link Runnable} that has a way to get the return value and
     * exception after the fact.
     */
    private static class CallableWrapper<T> implements Runnable {
        private final Callable<T> mCallable;
        private final CountDownLatch mLatch = new CountDownLatch(1);
        private T mReturnValue;
        private Throwable mException;

        public CallableWrapper(Callable<T> callable) {
            mCallable = callable;
        }

        @Override
        public final void run() {
            try {
                mReturnValue = mCallable.call();
            } catch (Throwable t) {
                mException = t;
            } finally {
                mLatch.countDown();
            }
        }

        public void awaitTermination() throws InterruptedException {
            mLatch.await();
        }

        public T getReturnValue() {
            return mReturnValue;
        }

        public Throwable getException() {
            return mException;
        }
    }

    private static final Handler sMainThreadHandler = new Handler(Looper.getMainLooper());

    private MainThread() {
        // Utility class.
    }

    /** Executed in the main thread. Returns the result of an execution or any exception thrown. */
    public static <T> T run(final Callable<T> task) throws Exception {
        CallableWrapper<T> wrapper = new CallableWrapper<>(task);
        return runCallableWrapper(wrapper);
    }

    private static <T> T runCallableWrapper(CallableWrapper<T> wrapper) throws Exception {
        sMainThreadHandler.post(wrapper);
        wrapper.awaitTermination();
        Throwable exception = wrapper.getException();
        if (exception != null) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            }
            if (exception instanceof Error) {
                throw (Error) exception;
            }
            throw (Exception) exception;
        }
        return wrapper.getReturnValue();
    }
}
