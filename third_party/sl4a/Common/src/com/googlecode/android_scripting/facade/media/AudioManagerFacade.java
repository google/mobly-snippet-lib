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

import android.app.Service;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;

import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;

public class AudioManagerFacade extends RpcReceiver {

    private final Service mService;
    private final EventFacade mEventFacade;
    private final AudioManager mAudio;
    private final OnAudioFocusChangeListener mFocusChangeListener;
    private boolean mIsFocused;

    public AudioManagerFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mEventFacade = manager.getReceiver(EventFacade.class);
        mAudio = (AudioManager) mService.getSystemService(Context.AUDIO_SERVICE);
        mFocusChangeListener = new OnAudioFocusChangeListener() {
            public void onAudioFocusChange(int focusChange) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    mIsFocused = false;
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    mIsFocused = true;
                }
            }
        };
    }

    @Rpc(description = "Checks whether any music is active.")
    public Boolean audioIsMusicActive() {
        return mAudio.isMusicActive();
    }

    @Rpc(description = "Checks whether A2DP audio routing to the Bluetooth headset is on or off.")
    public Boolean audioIsBluetoothA2dpOn() {
        return mAudio.isBluetoothA2dpOn();
    }

    @Rpc(description = "Request audio focus for sl4a.")
    public Boolean audioRequestAudioFocus() {
        int status = mAudio.requestAudioFocus(mFocusChangeListener,
                                              AudioManager.STREAM_MUSIC,
                                              AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        if (status == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mIsFocused = true;
            return true;
        }
        mIsFocused = false;
        return false;
    }

    @Rpc(description = "Whether sl4a has the audio focus or not.")
    public Boolean audioIsFocused() {
        return mIsFocused;
    }

    @Override
    public void shutdown() {
        mAudio.abandonAudioFocus(mFocusChangeListener);
    }
}
