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

package com.googlecode.android_scripting.facade.media;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener;
import android.media.session.PlaybackState;
import android.media.session.MediaSession.Callback;
import android.view.KeyEvent;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.MainThread;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcDefault;
import com.googlecode.android_scripting.rpc.RpcParameter;

/**
 * Expose functionalities of MediaSession related classes
 * like MediaSession, MediaSessionManager, MediaController.
 *
 */
public class MediaSessionFacade extends RpcReceiver {

    private final Service mService;
    private final EventFacade mEventFacade;
    private final MediaSession mSession;
    private final MediaSessionManager mManager;
    private final OnActiveSessionsChangedListener mSessionListener;
    private final Callback mCallback;

    private List<MediaController> mActiveControllers = null;

    public MediaSessionFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mEventFacade = manager.getReceiver(EventFacade.class);
        Log.d("Creating MediaSession.");
        mSession = new MediaSession(mService, "SL4A");
        mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);
        mManager = (MediaSessionManager) mService.getSystemService(Context.MEDIA_SESSION_SERVICE);
        mCallback = new MediaButtonCallback(mEventFacade);
        mSessionListener = new MediaSessionListener();
        mManager.addOnActiveSessionsChangedListener(mSessionListener,
                new ComponentName(mService.getPackageName(), this.getClass().getName()));
        mSession.setActive(true);
    }

    private class MediaSessionListener implements OnActiveSessionsChangedListener {

        @Override
        public void onActiveSessionsChanged(List<MediaController> controllers) {
            Log.d("Active MediaSessions have changed. Update current controller.");
            int size = controllers.size();
            for (int i = 0; i < size; i++) {
                MediaController controller = controllers.get(i);
                long flags = controller.getFlags();
                // We only care about sessions that handle transport controls,
                // which will be true for apps using RCC
                if ((flags & MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS) != 0) {
                    Log.d("The current active MediaSessions is " + controller.getTag());
                    return;
                }
            }
        }
    }

    @Rpc(description = "Retrieve a list of active sessions.")
    public List<String> mediaGetActiveSessions() {
        mActiveControllers = mManager.getActiveSessions(null);
        List<String> results = new ArrayList<String>();
        for (MediaController mc : mActiveControllers) {
            results.add(mc.getTag());
        }
        return results;
    }

    @Rpc(description = "Add callback to media session.")
    public void mediaSessionAddCallback() {
        MainThread.run(mService, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Log.d("Adding callback.");
                mSession.setCallback(mCallback);
                PlaybackState.Builder bob = new PlaybackState.Builder();
                bob.setActions(PlaybackState.ACTION_PLAY |
                               PlaybackState.ACTION_PAUSE |
                               PlaybackState.ACTION_STOP);
                bob.setState(PlaybackState.STATE_PLAYING, 0, 1);
                mSession.setPlaybackState(bob.build());
                return null;
            }
        });
    }

    @Rpc(description = "Whether current media session is active.")
    public Boolean mediaSessionIsActive() {
        return mSession.isActive();
    }

    @Rpc(description = "Simulate a media key press.")
    public void mediaDispatchMediaKeyEvent(String key) {
        int keyCode;
        if (key.equals("Play")) {
            keyCode = KeyEvent.KEYCODE_MEDIA_PLAY;
        } else if (key.equals("Pause")) {
            keyCode = KeyEvent.KEYCODE_MEDIA_PAUSE;
        } else if (key.equals("Stop")) {
            keyCode = KeyEvent.KEYCODE_MEDIA_STOP;
        } else if (key.equals("Next")) {
            keyCode = KeyEvent.KEYCODE_MEDIA_NEXT;
        } else if (key.equals("Previous")) {
            keyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
        } else if (key.equals("Forward")) {
            keyCode = KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
        } else if (key.equals("Rewind")) {
            keyCode = KeyEvent.KEYCODE_MEDIA_REWIND;
        } else {
            Log.d("Unrecognized media key.");
            return;
        }
        KeyEvent keyDown = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        KeyEvent keyUp = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        mManager.dispatchMediaKeyEvent(keyDown);
        mManager.dispatchMediaKeyEvent(keyUp);
    }

    private MediaController getMediaController(int idx) {
        return mActiveControllers.get(idx);
    }

    @Rpc(description = "Call Play on the currently active media session.")
    public void mediaSessionPlay(@RpcParameter(name = "index") @RpcDefault(value = "0")
                                 Integer idx) {
        getMediaController(idx).getTransportControls().play();
    }

    @Rpc(description = "Call Pause on the currently active media session.")
    public void mediaSessionPause(@RpcParameter(name = "index") @RpcDefault(value = "0")
                                  Integer idx) {
        getMediaController(idx).getTransportControls().pause();
    }

    @Rpc(description = "Call Stop on the currently active media session.")
    public void mediaSessionStop(@RpcParameter(name = "index") @RpcDefault(value = "0")
                                 Integer idx) {
        getMediaController(idx).getTransportControls().stop();
    }

    @Rpc(description = "Call Next on the currently active media session.")
    public void mediaSessionNext(@RpcParameter(name = "index") @RpcDefault(value = "0")
                                 Integer idx) {
        getMediaController(idx).getTransportControls().skipToNext();
    }

    @Override
    public void shutdown() {
        mSession.setCallback(null);
        mSession.release();
    }
}
