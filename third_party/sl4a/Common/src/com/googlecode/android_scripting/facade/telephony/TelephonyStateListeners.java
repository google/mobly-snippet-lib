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
package com.googlecode.android_scripting.facade.telephony;

import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.telephony.TelephonyEvents;
import android.os.Bundle;
import android.os.Looper;
import android.telephony.CellInfo;
import android.telephony.DataConnectionRealTimeInfo;
import android.telephony.PhoneStateListener;
import android.telephony.PreciseCallState;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.VoLteServiceState;

import java.util.List;

/**
 * Store all subclasses of PhoneStateListener here.
 */
public class TelephonyStateListeners {

    public static class CallStateChangeListener extends PhoneStateListener {

        private final EventFacade mEventFacade;
        public static final int sListeningStates = PhoneStateListener.LISTEN_CALL_STATE |
                                                   PhoneStateListener.LISTEN_PRECISE_CALL_STATE;

        public boolean listenForeground = true;
        public boolean listenRinging = false;
        public boolean listenBackground = false;
        public int subscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        public CallStateChangeListener(EventFacade ef) {
            super();
            mEventFacade = ef;
            subscriptionId = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
        }

        public CallStateChangeListener(EventFacade ef, int subId) {
            super(subId);
            mEventFacade = ef;
            subscriptionId = subId;
        }

        public CallStateChangeListener(EventFacade ef, int subId, Looper looper) {
            super(subId, looper);
            mEventFacade = ef;
            subscriptionId = subId;
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            mEventFacade.postEvent(TelephonyConstants.EventCallStateChanged,
                new TelephonyEvents.CallStateEvent(
                    state, incomingNumber, subscriptionId));
        }

        @Override
        public void onPreciseCallStateChanged(PreciseCallState callState) {
            int foregroundState = callState.getForegroundCallState();
            int ringingState = callState.getRingingCallState();
            int backgroundState = callState.getBackgroundCallState();
            if (listenForeground &&
                foregroundState != PreciseCallState.PRECISE_CALL_STATE_NOT_VALID) {
                processCallState(foregroundState,
                        TelephonyConstants.PRECISE_CALL_STATE_LISTEN_LEVEL_FOREGROUND,
                        callState);
            }
            if (listenRinging &&
                ringingState != PreciseCallState.PRECISE_CALL_STATE_NOT_VALID) {
                processCallState(ringingState,
                        TelephonyConstants.PRECISE_CALL_STATE_LISTEN_LEVEL_RINGING,
                        callState);
            }
            if (listenBackground &&
                backgroundState != PreciseCallState.PRECISE_CALL_STATE_NOT_VALID) {
                processCallState(backgroundState,
                        TelephonyConstants.PRECISE_CALL_STATE_LISTEN_LEVEL_BACKGROUND,
                        callState);
            }
        }

        private void processCallState(
            int newState, String which, PreciseCallState callState) {
            mEventFacade.postEvent(TelephonyConstants.EventPreciseStateChanged,
                new TelephonyEvents.PreciseCallStateEvent(
                    newState, which, callState, subscriptionId));
        }
    }

    public static class DataConnectionRealTimeInfoChangeListener extends PhoneStateListener {

        private final EventFacade mEventFacade;
        public static final int sListeningStates =
                PhoneStateListener.LISTEN_DATA_CONNECTION_REAL_TIME_INFO;
        public int subscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        public DataConnectionRealTimeInfoChangeListener(EventFacade ef) {
            super();
            mEventFacade = ef;
            subscriptionId = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
        }

        public DataConnectionRealTimeInfoChangeListener(EventFacade ef, int subId) {
            super(subId);
            mEventFacade = ef;
            subscriptionId = subId;
        }

        public DataConnectionRealTimeInfoChangeListener(EventFacade ef, int subId, Looper looper) {
            super(subId, looper);
            mEventFacade = ef;
            subscriptionId = subId;
        }

        @Override
        public void onDataConnectionRealTimeInfoChanged(
            DataConnectionRealTimeInfo dcRtInfo) {
            mEventFacade.postEvent(
                TelephonyConstants.EventDataConnectionRealTimeInfoChanged,
                new TelephonyEvents.DataConnectionRealTimeInfoEvent(
                    dcRtInfo, subscriptionId));
        }
    }

    public static class DataConnectionStateChangeListener extends PhoneStateListener {

        private final EventFacade mEventFacade;
        private final TelephonyManager mTelephonyManager;
        public static final int sListeningStates =
                PhoneStateListener.LISTEN_DATA_CONNECTION_STATE;
        public int subscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        public DataConnectionStateChangeListener(EventFacade ef, TelephonyManager tm) {
            super();
            mEventFacade = ef;
            mTelephonyManager = tm;
            subscriptionId = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
        }

        public DataConnectionStateChangeListener(EventFacade ef, TelephonyManager tm, int subId) {
            super(subId);
            mEventFacade = ef;
            mTelephonyManager = tm;
            subscriptionId = subId;
        }

        public DataConnectionStateChangeListener(
                EventFacade ef, TelephonyManager tm, int subId, Looper looper) {
            super(subId, looper);
            mEventFacade = ef;
            mTelephonyManager = tm;
            subscriptionId = subId;
        }

        @Override
        public void onDataConnectionStateChanged(int state) {
            mEventFacade.postEvent(
                TelephonyConstants.EventDataConnectionStateChanged,
                new TelephonyEvents.DataConnectionStateEvent(state,
                    TelephonyUtils.getNetworkTypeString(
                                 mTelephonyManager.getDataNetworkType()),
                    subscriptionId));
        }
    }

    public static class ServiceStateChangeListener extends PhoneStateListener {

        private final EventFacade mEventFacade;
        public static final int sListeningStates = PhoneStateListener.LISTEN_SERVICE_STATE;
        public int subscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        public ServiceStateChangeListener(EventFacade ef) {
            super();
            mEventFacade = ef;
            subscriptionId = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
        }

        public ServiceStateChangeListener(EventFacade ef, int subId) {
            super(subId);
            mEventFacade = ef;
            subscriptionId = subId;
        }

        public ServiceStateChangeListener(EventFacade ef, int subId, Looper looper) {
            super(subId, looper);
            mEventFacade = ef;
            subscriptionId = subId;
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            mEventFacade.postEvent(TelephonyConstants.EventServiceStateChanged,
                new TelephonyEvents.ServiceStateEvent(
                    serviceState, subscriptionId));
        }

    }

    public static class CellInfoChangeListener
            extends PhoneStateListener {

        private final EventFacade mEventFacade;

        public CellInfoChangeListener(EventFacade ef) {
            super();
            mEventFacade = ef;
        }

        public CellInfoChangeListener(EventFacade ef, int subId) {
            super(subId);
            mEventFacade = ef;
        }

        public CellInfoChangeListener(EventFacade ef, int subId, Looper looper) {
            super(subId, looper);
            mEventFacade = ef;
        }

        @Override
        public void onCellInfoChanged(List<CellInfo> cellInfo) {
            mEventFacade.postEvent(
                TelephonyConstants.EventCellInfoChanged, cellInfo);
        }
    }

    public static class VolteServiceStateChangeListener
            extends PhoneStateListener {

        private final EventFacade mEventFacade;

        public VolteServiceStateChangeListener(EventFacade ef) {
            super();
            mEventFacade = ef;
        }

        public VolteServiceStateChangeListener(EventFacade ef, int subId) {
            super(subId);
            mEventFacade = ef;
        }

        public VolteServiceStateChangeListener(EventFacade ef, int subId, Looper looper) {
            super(subId, looper);
            mEventFacade = ef;
        }

        @Override
        public void onVoLteServiceStateChanged(VoLteServiceState volteInfo) {
            mEventFacade.postEvent(
                    TelephonyConstants.EventVolteServiceStateChanged,
                    volteInfo);
        }
    }

    public static class VoiceMailStateChangeListener extends PhoneStateListener {

        private final EventFacade mEventFacade;

        public static final int sListeningStates =
                PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR;

        public VoiceMailStateChangeListener(EventFacade ef) {
            super();
            mEventFacade = ef;
        }

        public VoiceMailStateChangeListener(EventFacade ef, int subId) {
            super(subId);
            mEventFacade = ef;
        }

        public VoiceMailStateChangeListener(EventFacade ef, int subId, Looper looper) {
            super(subId, looper);
            mEventFacade = ef;
        }

        @Override
        public void onMessageWaitingIndicatorChanged(boolean messageWaitingIndicator) {
            mEventFacade.postEvent(
                    TelephonyConstants.EventMessageWaitingIndicatorChanged,
                    new TelephonyEvents.MessageWaitingIndicatorEvent(
                        messageWaitingIndicator));
        }
    }


    public static class SignalStrengthChangeListener extends PhoneStateListener {

        private final EventFacade mEventFacade;
        public SignalStrength mSignalStrengths;
        public static final int sListeningStates = PhoneStateListener.LISTEN_SIGNAL_STRENGTHS;
        public SignalStrengthChangeListener(EventFacade ef) {
            super();
            mEventFacade = ef;
        }

        public SignalStrengthChangeListener(EventFacade ef, int subId) {
            super(subId);
            mEventFacade = ef;
        }

        public SignalStrengthChangeListener(EventFacade ef, int subId, Looper looper) {
            super(subId, looper);
            mEventFacade = ef;
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            mSignalStrengths = signalStrength;
            mEventFacade.postEvent(
                TelephonyConstants.EventSignalStrengthChanged, signalStrength);
        }
    }

}
