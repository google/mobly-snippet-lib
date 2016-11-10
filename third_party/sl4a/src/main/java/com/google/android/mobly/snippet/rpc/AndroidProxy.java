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

import android.app.Service;
import android.content.Context;
import android.content.Intent;

import com.google.android.mobly.snippet.facade.ReflectionFacadeManagerFactory;

import java.net.InetSocketAddress;
import java.util.UUID;

public class AndroidProxy {

    private InetSocketAddress mAddress;
    private final JsonRpcServer mJsonRpcServer;
    private final UUID mSecret;
    private final RpcReceiverManagerFactory mFacadeManagerFactory;

    /**
     *
     * @param context
     *          Android context (required to build facades).
     * @param requiresHandshake
     *          indicates whether RPC security protocol should be enabled.
     */
    public AndroidProxy(Context context, boolean requiresHandshake) {
        if (requiresHandshake) {
            mSecret = UUID.randomUUID();
        } else {
            mSecret = null;
        }
        mFacadeManagerFactory = new ReflectionFacadeManagerFactory(context);
        mJsonRpcServer = new JsonRpcServer(mFacadeManagerFactory, getSecret());
    }

    public InetSocketAddress startLocal(int port) {
        mAddress = mJsonRpcServer.startLocal(port);
        return mAddress;
    }

    private String getSecret() {
        if (mSecret == null) {
            return null;
        }
        return mSecret.toString();
    }
}
