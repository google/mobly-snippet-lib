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
package com.google.android.mobly.snippet;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Process;
import android.support.test.runner.AndroidJUnitRunner;

import com.google.android.mobly.snippet.rpc.AndroidProxy;
import com.google.android.mobly.snippet.util.Log;
import com.google.android.mobly.snippet.util.NotificationIdFactory;

/**
 * A launcher that starts the snippet server as an instrumentation so that it has access to the
 * target app's context.
 *
 * It is written this way to be compatible with 'am instrument'.
 */
public class SnippetRunner extends AndroidJUnitRunner {
    private static final String ARG_ACTION = "action";
    private static final String ARG_PORT = "port";

    private static final int NOTIFICATION_ID = NotificationIdFactory.create();

    private Bundle mArguments;
    private NotificationManager mNotificationManager;
    private Notification mNotification;

    @Override
    public void onCreate(Bundle arguments) {
        mArguments = arguments;
        mNotificationManager = (NotificationManager)
                getTargetContext().getSystemService(Context.NOTIFICATION_SERVICE);
        super.onCreate(arguments);
    }

    @Override
    public void onStart() {
        String action = mArguments.getString(ARG_ACTION);
        if (action == null) {
            throw new IllegalArgumentException("\"--e action <action>\" was not specified");
        }
        if (action.equals("start")) {
            String servicePort = mArguments.getString(ARG_PORT);
            if (servicePort == null) {
                throw new IllegalArgumentException("\"--e port <port>\" was not specified");
            }
            int port = Integer.parseInt(servicePort);
            startServer(port);
        } else if (action.equals("stop")) {
            mNotificationManager.cancel(NOTIFICATION_ID);
            mNotificationManager.cancelAll();
            super.onStart();
        } else {
            throw new IllegalArgumentException("Unknown action type \"" + action + "\"");
        }
    }

    private void startServer(int port) {
        AndroidProxy androidProxy = new AndroidProxy(getContext());
        if (androidProxy.startLocal(port) == null) {
            throw new RuntimeException("Failed to start server on port " + port);
        }
        createNotification();
        Log.i("Snippet server started for process " + Process.myPid() + " on port " + port);
    }

    private void createNotification() {
        Notification.Builder builder = new Notification.Builder(getTargetContext());
        builder.setSmallIcon(android.R.drawable.btn_star)
                .setTicker(null)
                .setWhen(System.currentTimeMillis())
                .setContentTitle("Snippet Service");
        mNotification = builder.getNotification();
        mNotification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }
}
