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

package com.google.android.mobly.snippet.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.google.android.mobly.snippet.Constants;
import com.google.android.mobly.snippet.rpc.AndroidProxy;
import com.google.android.mobly.snippet.util.NotificationIdFactory;

/**
 * A service that allows scripts and the RPC server to run in the background.
 *
 */
public class SnippetService extends ForegroundService {
    private static final int NOTIFICATION_ID = NotificationIdFactory.create();

    private final IBinder mBinder;
    private NotificationManager mNotificationManager;
    private Notification mNotification;

    public class LocalBinder extends Binder {
        public SnippetService getService() {
            return SnippetService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public SnippetService() {
        super(NOTIFICATION_ID);
        mBinder = new LocalBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    protected Notification createNotification() {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(android.R.drawable.btn_star)
                .setTicker(null)
                .setWhen(System.currentTimeMillis())
                .setContentTitle("Snippet Service");
        mNotification = builder.getNotification();
        mNotification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        return mNotification;
    }

    private void updateNotification(String tickerText) {
        if (tickerText.equals(mNotification.tickerText)) {
            // Consequent notifications with the same ticker-text are displayed without any ticker-text.
            // This is a way around. Alternatively, we can display process name and port.
            tickerText = tickerText + " ";
        }
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle("Snippet Service")
                .setWhen(mNotification.when)
                .setTicker(tickerText);

        mNotification = builder.getNotification();
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent == null) {
            return START_REDELIVER_INTENT;
        } else if (intent.getAction().equals(Constants.ACTION_LAUNCH_SERVER)) {
            launchServer(intent);
        } else {
            updateNotification("Action not implemented: " + intent.getAction());
        }
        return START_REDELIVER_INTENT;
    }

    private void launchServer(Intent intent) {
        AndroidProxy androidProxy = new AndroidProxy(this);
        int servicePort = intent.getIntExtra(Constants.EXTRA_SERVICE_PORT, 0);
        if (servicePort == 0) {
            throw new IllegalArgumentException(
                    "Intent missing required extra: " + Constants.EXTRA_SERVICE_PORT);
        }
        if (androidProxy.startLocal(servicePort) == null) {
            throw new RuntimeException("Failed to start server on port " + servicePort);
        }
    }
}
