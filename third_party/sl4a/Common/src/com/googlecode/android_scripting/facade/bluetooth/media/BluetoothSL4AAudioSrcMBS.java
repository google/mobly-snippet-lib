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

package com.googlecode.android_scripting.facade.bluetooth.media;

import android.app.Service;
import android.media.browse.MediaBrowser.MediaItem;
import android.media.session.*;
import android.os.Bundle;
import android.service.media.MediaBrowserService;

import com.googlecode.android_scripting.facade.bluetooth.BluetoothMediaFacade;
import com.googlecode.android_scripting.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link MediaBrowserService} implemented in the SL4A App to intercept Media keys and
 * commands.
 * This would be running on the Phone (AVRCP TG device) and whenever the device receives a media
 * command from Car (a AVRCP CT), this MediaBrowserService's MediaSession would intercept it.
 * Helps to verify the commands received by the Phone (AVRCP TG) are the same as what was sent from
 * Car (AVRCP CT).
 */
public class BluetoothSL4AAudioSrcMBS extends MediaBrowserService {
    private static final String TAG = "BluetoothSL4AAudioSrcMBS";
    private static final String MEDIA_ROOT_ID = "__ROOT__";

    private MediaSession mMediaSession = null;
    private MediaSession.Token mSessionToken = null;
    private BluetoothMediaPlayback mPlayback = null;
    private static BluetoothSL4AAudioSrcMBS sAvrcpMediaBrowserService;
    private static final String CMD_MEDIA_PLAY = "play";
    private static final String CMD_MEDIA_PAUSE = "pause";
    private static final String CMD_MEDIA_SKIP_NEXT = "skipNext";
    private static final String CMD_MEDIA_SKIP_PREV = "skipPrev";

    /**
     * MediaSession callback dispatching the corresponding <code>PlaybackState</code> to
     * {@link BluetoothMediaFacade}
     */
    private MediaSession.Callback mMediaSessionCallback =
            new MediaSession.Callback() {
                @Override
                public void onPlay() {
                    Log.d(TAG + " onPlay");
                    mPlayback.play();
                }

                @Override
                public void onPause() {
                    Log.d(TAG + " onPause");
                    mPlayback.pause();
                }

                @Override
                public void onRewind() {
                    Log.d(TAG + " onRewind");
                }

                @Override
                public void onFastForward() {
                    Log.d(TAG + " onFastForward");
                }

                @Override
                public void onSkipToNext() {
                    Log.d(TAG + " onSkipToNext");
                    mPlayback.skipNext();
                }

                @Override
                public void onSkipToPrevious() {
                    Log.d(TAG + " onSkipToPrevious");
                    mPlayback.skipPrev();
                }

                @Override
                public void onStop() {
                    Log.d(TAG + " onStop");
                    mPlayback.stop();
                }
            };

    /**
     * Returns the currently set instance of {@link BluetoothSL4AAudioSrcMBS}
     *
     * @return current instance of {@link BluetoothSL4AAudioSrcMBS}
     */
    public static synchronized BluetoothSL4AAudioSrcMBS getAvrcpMediaBrowserService() {
        return sAvrcpMediaBrowserService;
    }

    /**
     * Handle a Media Playback Command.
     * Pass it to the appropriate {@link BluetoothMediaPlayback} method
     *
     * @param command - command to handle
     */
    public void handleMediaCommand(String command) {
        if (mPlayback == null) {
            Log.d(TAG + " handleMediaCommand Failed since Playback is null");
            return;
        }
        switch (command) {
            case CMD_MEDIA_PLAY:
                mPlayback.play();
                break;
            case CMD_MEDIA_PAUSE:
                mPlayback.pause();
                break;
            case CMD_MEDIA_SKIP_NEXT:
                mPlayback.skipNext();
                break;
            case CMD_MEDIA_SKIP_PREV:
                mPlayback.skipPrev();
                break;
            default:
                Log.d(TAG + " Unknown command " + command);
        }
        return;
    }

    /**
     * We do the following on the BluetoothSL4AAudioSrcMBS onCreate():
     * 1. Create a new MediaSession
     * 2. Register a callback with the created MediaSession
     * 3. Set its playback state and set the session to active.
     * 4. Create a new BluetoothMediaPlayback instance to handle all the "music playing"
     * 5. Set the created MediaSession active
     */
    @Override
    public void onCreate() {
        Log.d(TAG + " onCreate");
        super.onCreate();
        sAvrcpMediaBrowserService = this;
        mMediaSession = new MediaSession(this, TAG);
        mSessionToken = mMediaSession.getSessionToken();
        setSessionToken(mSessionToken);
        mMediaSession.setCallback(mMediaSessionCallback);
        mMediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        // Note - MediaButton Intent is not received until the session has a PlaybackState
        // whose state is set to something other than STATE_STOPPED or STATE_NONE
        mPlayback = new BluetoothMediaPlayback();
        if (mPlayback == null) {
            Log.e(TAG + "Playback alloc error");
        }
        mPlayback.setMediaSession(mMediaSession);
        PlaybackState state = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE
                        | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS
                        | PlaybackState.ACTION_STOP)
                .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1)
                .build();
        mMediaSession.setPlaybackState(state);
        mMediaSession.setActive(true);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG + " onDestroy");
        mPlayback.releaseMediaPlayer();
        mMediaSession.release();
        mMediaSession = null;
        mSessionToken = null;
        sAvrcpMediaBrowserService = null;
        super.onDestroy();
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        BrowserRoot mediaRoot = new BrowserRoot(MEDIA_ROOT_ID, null);
        return mediaRoot;
    }

    @Override
    public void onLoadChildren(String parentId, Result<List<MediaItem>> result) {
        List<MediaItem> mediaList = new ArrayList<MediaItem>();
        result.sendResult(mediaList);
    }

    /**
     * Returns the TAG string
     *
     * @return <code>BluetoothSL4AAudioSrcMBS</code>'s tag
     */
    public static String getTag() {
        return TAG;
    }
}


