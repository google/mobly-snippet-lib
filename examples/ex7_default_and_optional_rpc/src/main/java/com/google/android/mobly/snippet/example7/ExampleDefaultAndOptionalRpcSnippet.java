/*
 * Copyright (C) 2017 Google Inc.
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

package com.google.android.mobly.snippet.example7;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;
import androidx.test.InstrumentationRegistry;
import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.event.EventCache;
import com.google.android.mobly.snippet.rpc.Rpc;
import com.google.android.mobly.snippet.rpc.RpcDefault;
import com.google.android.mobly.snippet.rpc.RpcOptional;

/** Demonstrates how to mark an RPC has default value or optional. */
public class ExampleDefaultAndOptionalRpcSnippet implements Snippet {

    private final Context mContext;
    private final EventCache mEventCache = EventCache.getInstance();

    /**
     * Since the APIs here deal with UI, most of them have to be called in a thread that has called
     * looper.
     */
    private final Handler mHandler;

    public ExampleDefaultAndOptionalRpcSnippet() {
        mContext = InstrumentationRegistry.getContext();
        mHandler = new Handler(mContext.getMainLooper());
    }

    @Rpc(description = "Make a toast on screen.")
    public String makeToast(
            String message, @RpcDefault("true") Boolean bool, @RpcOptional Integer number)
            throws InterruptedException {
        if (number == null) {
            showToast(String.format("%s, bool:%b", message, bool));
        } else {
            showToast(String.format("%s, bool:%b, number:%d", message, bool, number));
        }
        return "OK";
    }

    @Override
    public void shutdown() {}

    private void showToast(final String message) {
        mHandler.post(
            new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
                }
            });
    }
}
