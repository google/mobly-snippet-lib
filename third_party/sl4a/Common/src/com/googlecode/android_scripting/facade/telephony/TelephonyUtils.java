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
import com.android.ims.ImsConfig;
import com.android.internal.telephony.RILConstants;
import com.googlecode.android_scripting.Log;
import android.telecom.TelecomManager;
import android.telephony.DataConnectionRealTimeInfo;
import android.telephony.PreciseCallState;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.VoLteServiceState;

/**
 * Telephony utility functions
 */
public class TelephonyUtils {

    public static String getWfcModeString(int mode) {
       switch(mode) {
           case ImsConfig.WfcModeFeatureValueConstants.WIFI_PREFERRED:
               return TelephonyConstants.WFC_MODE_WIFI_PREFERRED;
           case ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED:
               return TelephonyConstants.WFC_MODE_CELLULAR_PREFERRED;
           case ImsConfig.WfcModeFeatureValueConstants.WIFI_ONLY:
               return TelephonyConstants.WFC_MODE_WIFI_ONLY;
       }
        Log.d("getWfcModeStringfromInt error. int: " + mode);
        return TelephonyConstants.WFC_MODE_UNKNOWN;
    }

    public static String getTtyModeString(int mode) {
        switch (mode) {
            case TelecomManager.TTY_MODE_FULL:
                return TelephonyConstants.TTY_MODE_FULL;
            case TelecomManager.TTY_MODE_HCO:
                return TelephonyConstants.TTY_MODE_HCO;
            case TelecomManager.TTY_MODE_OFF:
                return TelephonyConstants.TTY_MODE_OFF;
            case TelecomManager.TTY_MODE_VCO:
                return TelephonyConstants.TTY_MODE_VCO;
        }
        Log.d("getTtyModeString error. int: " + mode);
        return null;
    }

    public static String getPhoneTypeString(int type) {
        switch (type) {
            case TelephonyManager.PHONE_TYPE_GSM:
                return TelephonyConstants.PHONE_TYPE_GSM;
            case TelephonyManager.PHONE_TYPE_NONE:
                return TelephonyConstants.PHONE_TYPE_NONE;
            case TelephonyManager.PHONE_TYPE_CDMA:
                return TelephonyConstants.PHONE_TYPE_CDMA;
            case TelephonyManager.PHONE_TYPE_SIP:
                return TelephonyConstants.PHONE_TYPE_SIP;
        }
        Log.d("getPhoneTypeString error. int: " + type);
        return null;
    }

    public static String getSimStateString(int state) {
        switch (state) {
            case TelephonyManager.SIM_STATE_UNKNOWN:
                return TelephonyConstants.SIM_STATE_UNKNOWN;
            case TelephonyManager.SIM_STATE_ABSENT:
                return TelephonyConstants.SIM_STATE_ABSENT;
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                return TelephonyConstants.SIM_STATE_PIN_REQUIRED;
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                return TelephonyConstants.SIM_STATE_PUK_REQUIRED;
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                return TelephonyConstants.SIM_STATE_NETWORK_LOCKED;
            case TelephonyManager.SIM_STATE_READY:
                return TelephonyConstants.SIM_STATE_READY;
            case TelephonyManager.SIM_STATE_NOT_READY:
                return TelephonyConstants.SIM_STATE_NOT_READY;
            case TelephonyManager.SIM_STATE_PERM_DISABLED:
                return TelephonyConstants.SIM_STATE_PERM_DISABLED;
            case TelephonyManager.SIM_STATE_CARD_IO_ERROR:
                return TelephonyConstants.SIM_STATE_CARD_IO_ERROR;
        }
        Log.d("getSimStateString error. int: " + state);
        return TelephonyConstants.SIM_STATE_UNKNOWN;
    }

    public static String formatIncomingNumber(String incomingNumber) {
        String mIncomingNumber = null;
        int len = 0;
        if (incomingNumber != null) {
            len = incomingNumber.length();
        }
        if (len > 0) {
            /**
             * Currently this incomingNumber modification is specific for
             * US numbers.
             */
            if ((12 == len) && ('+' == incomingNumber.charAt(0))) {
                mIncomingNumber = incomingNumber.substring(1);
            } else if (10 == len) {
                mIncomingNumber = '1' + incomingNumber;
            } else {
                mIncomingNumber = incomingNumber;
            }
        }
        return mIncomingNumber;
    }

    public static String getTelephonyCallStateString(int callState) {
        switch (callState) {
            case TelephonyManager.CALL_STATE_IDLE:
                return TelephonyConstants.TELEPHONY_STATE_IDLE;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                return TelephonyConstants.TELEPHONY_STATE_OFFHOOK;
            case TelephonyManager.CALL_STATE_RINGING:
                return TelephonyConstants.TELEPHONY_STATE_RINGING;
        }
        Log.d("getTelephonyCallStateString error. int: " + callState);
        return TelephonyConstants.TELEPHONY_STATE_UNKNOWN;
    }

    public static String getPreciseCallStateString(int state) {
        switch (state) {
            case PreciseCallState.PRECISE_CALL_STATE_ACTIVE:
                return TelephonyConstants.PRECISE_CALL_STATE_ACTIVE;
            case PreciseCallState.PRECISE_CALL_STATE_HOLDING:
                return TelephonyConstants.PRECISE_CALL_STATE_HOLDING;
            case PreciseCallState.PRECISE_CALL_STATE_DIALING:
                return TelephonyConstants.PRECISE_CALL_STATE_DIALING;
            case PreciseCallState.PRECISE_CALL_STATE_ALERTING:
                return TelephonyConstants.PRECISE_CALL_STATE_ALERTING;
            case PreciseCallState.PRECISE_CALL_STATE_INCOMING:
                return TelephonyConstants.PRECISE_CALL_STATE_INCOMING;
            case PreciseCallState.PRECISE_CALL_STATE_WAITING:
                return TelephonyConstants.PRECISE_CALL_STATE_WAITING;
            case PreciseCallState.PRECISE_CALL_STATE_DISCONNECTED:
                return TelephonyConstants.PRECISE_CALL_STATE_DISCONNECTED;
            case PreciseCallState.PRECISE_CALL_STATE_DISCONNECTING:
                return TelephonyConstants.PRECISE_CALL_STATE_DISCONNECTING;
            case PreciseCallState.PRECISE_CALL_STATE_IDLE:
                return TelephonyConstants.PRECISE_CALL_STATE_IDLE;
            case PreciseCallState.PRECISE_CALL_STATE_NOT_VALID:
                return TelephonyConstants.PRECISE_CALL_STATE_INVALID;
        }
        Log.d("getPreciseCallStateString error. int: " + state);
        return TelephonyConstants.PRECISE_CALL_STATE_UNKNOWN;
    }

    public static String getDcPowerStateString(int state) {
        if (state == DataConnectionRealTimeInfo.DC_POWER_STATE_LOW) {
            return TelephonyConstants.DC_POWER_STATE_LOW;
        } else if (state == DataConnectionRealTimeInfo.DC_POWER_STATE_HIGH) {
            return TelephonyConstants.DC_POWER_STATE_HIGH;
        } else if (state == DataConnectionRealTimeInfo.DC_POWER_STATE_MEDIUM) {
            return TelephonyConstants.DC_POWER_STATE_MEDIUM;
        } else {
            return TelephonyConstants.DC_POWER_STATE_UNKNOWN;
        }
    }

    public static String getDataConnectionStateString(int state) {
        switch (state) {
            case TelephonyManager.DATA_DISCONNECTED:
                return TelephonyConstants.DATA_STATE_DISCONNECTED;
            case TelephonyManager.DATA_CONNECTING:
                return TelephonyConstants.DATA_STATE_CONNECTING;
            case TelephonyManager.DATA_CONNECTED:
                return TelephonyConstants.DATA_STATE_CONNECTED;
            case TelephonyManager.DATA_SUSPENDED:
                return TelephonyConstants.DATA_STATE_SUSPENDED;
            case TelephonyManager.DATA_UNKNOWN:
                return TelephonyConstants.DATA_STATE_UNKNOWN;
        }
        Log.d("getDataConnectionStateString error. int: " + state);
        return TelephonyConstants.DATA_STATE_UNKNOWN;
    }

    public static int getNetworkModeIntfromString(String networkMode) {
        switch (networkMode) {
            case TelephonyConstants.NETWORK_MODE_WCDMA_PREF:
                return RILConstants.NETWORK_MODE_WCDMA_PREF;
            case TelephonyConstants.NETWORK_MODE_GSM_ONLY:
                return RILConstants.NETWORK_MODE_GSM_ONLY;
            case TelephonyConstants.NETWORK_MODE_WCDMA_ONLY:
                return RILConstants.NETWORK_MODE_WCDMA_ONLY;
            case TelephonyConstants.NETWORK_MODE_GSM_UMTS:
                return RILConstants.NETWORK_MODE_GSM_UMTS;
            case TelephonyConstants.NETWORK_MODE_CDMA:
                return RILConstants.NETWORK_MODE_CDMA;
            case TelephonyConstants.NETWORK_MODE_CDMA_NO_EVDO:
                return RILConstants.NETWORK_MODE_CDMA_NO_EVDO;
            case TelephonyConstants.NETWORK_MODE_EVDO_NO_CDMA:
                return RILConstants.NETWORK_MODE_EVDO_NO_CDMA;
            case TelephonyConstants.NETWORK_MODE_GLOBAL:
                return RILConstants.NETWORK_MODE_GLOBAL;
            case TelephonyConstants.NETWORK_MODE_LTE_CDMA_EVDO:
                return RILConstants.NETWORK_MODE_LTE_CDMA_EVDO;
            case TelephonyConstants.NETWORK_MODE_LTE_GSM_WCDMA:
                return RILConstants.NETWORK_MODE_LTE_GSM_WCDMA;
            case TelephonyConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                return RILConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA;
            case TelephonyConstants.NETWORK_MODE_LTE_ONLY:
                return RILConstants.NETWORK_MODE_LTE_ONLY;
            case TelephonyConstants.NETWORK_MODE_LTE_WCDMA:
                return RILConstants.NETWORK_MODE_LTE_WCDMA;
            case TelephonyConstants.NETWORK_MODE_TDSCDMA_ONLY:
                return RILConstants.NETWORK_MODE_TDSCDMA_ONLY;
            case TelephonyConstants.NETWORK_MODE_TDSCDMA_WCDMA:
                return RILConstants.NETWORK_MODE_TDSCDMA_WCDMA;
            case TelephonyConstants.NETWORK_MODE_LTE_TDSCDMA:
                return RILConstants.NETWORK_MODE_LTE_TDSCDMA;
            case TelephonyConstants.NETWORK_MODE_TDSCDMA_GSM:
                return RILConstants.NETWORK_MODE_TDSCDMA_GSM;
            case TelephonyConstants.NETWORK_MODE_LTE_TDSCDMA_GSM:
                return RILConstants.NETWORK_MODE_LTE_TDSCDMA_GSM;
            case TelephonyConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA:
                return RILConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA;
            case TelephonyConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA:
                return RILConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA;
            case TelephonyConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA:
                return RILConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA;
            case TelephonyConstants.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                return RILConstants.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA;
            case TelephonyConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                return RILConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA;
        }
        Log.d("getNetworkModeIntfromString error. String: " + networkMode);
        return RILConstants.RIL_ERRNO_INVALID_RESPONSE;
    }

    public static String getNetworkModeStringfromInt(int networkMode) {
        switch (networkMode) {
            case RILConstants.NETWORK_MODE_WCDMA_PREF:
                return TelephonyConstants.NETWORK_MODE_WCDMA_PREF;
            case RILConstants.NETWORK_MODE_GSM_ONLY:
                return TelephonyConstants.NETWORK_MODE_GSM_ONLY;
            case RILConstants.NETWORK_MODE_WCDMA_ONLY:
                return TelephonyConstants.NETWORK_MODE_WCDMA_ONLY;
            case RILConstants.NETWORK_MODE_GSM_UMTS:
                return TelephonyConstants.NETWORK_MODE_GSM_UMTS;
            case RILConstants.NETWORK_MODE_CDMA:
                return TelephonyConstants.NETWORK_MODE_CDMA;
            case RILConstants.NETWORK_MODE_CDMA_NO_EVDO:
                return TelephonyConstants.NETWORK_MODE_CDMA_NO_EVDO;
            case RILConstants.NETWORK_MODE_EVDO_NO_CDMA:
                return TelephonyConstants.NETWORK_MODE_EVDO_NO_CDMA;
            case RILConstants.NETWORK_MODE_GLOBAL:
                return TelephonyConstants.NETWORK_MODE_GLOBAL;
            case RILConstants.NETWORK_MODE_LTE_CDMA_EVDO:
                return TelephonyConstants.NETWORK_MODE_LTE_CDMA_EVDO;
            case RILConstants.NETWORK_MODE_LTE_GSM_WCDMA:
                return TelephonyConstants.NETWORK_MODE_LTE_GSM_WCDMA;
            case RILConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                return TelephonyConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA;
            case RILConstants.NETWORK_MODE_LTE_ONLY:
                return TelephonyConstants.NETWORK_MODE_LTE_ONLY;
            case RILConstants.NETWORK_MODE_LTE_WCDMA:
                return TelephonyConstants.NETWORK_MODE_LTE_WCDMA;
            case RILConstants.NETWORK_MODE_TDSCDMA_ONLY:
                return TelephonyConstants.NETWORK_MODE_TDSCDMA_ONLY;
            case RILConstants.NETWORK_MODE_TDSCDMA_WCDMA:
                return TelephonyConstants.NETWORK_MODE_TDSCDMA_WCDMA;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA:
                return TelephonyConstants.NETWORK_MODE_LTE_TDSCDMA;
            case RILConstants.NETWORK_MODE_TDSCDMA_GSM:
                return TelephonyConstants.NETWORK_MODE_TDSCDMA_GSM;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA_GSM:
                return TelephonyConstants.NETWORK_MODE_LTE_TDSCDMA_GSM;
            case RILConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA:
                return TelephonyConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA:
                return TelephonyConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA:
                return TelephonyConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA;
            case RILConstants.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                return TelephonyConstants.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                return TelephonyConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA;
        }
        Log.d("getNetworkModeStringfromInt error. Int: " + networkMode);
        return TelephonyConstants.NETWORK_MODE_INVALID;
    }

    public static String getNetworkTypeString(int type) {
        switch(type) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return TelephonyConstants.RAT_GPRS;
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return TelephonyConstants.RAT_EDGE;
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return TelephonyConstants.RAT_UMTS;
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return TelephonyConstants.RAT_HSDPA;
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return TelephonyConstants.RAT_HSUPA;
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return TelephonyConstants.RAT_HSPA;
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return TelephonyConstants.RAT_CDMA;
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return TelephonyConstants.RAT_1XRTT;
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return TelephonyConstants.RAT_EVDO_0;
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return TelephonyConstants.RAT_EVDO_A;
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return TelephonyConstants.RAT_EVDO_B;
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return TelephonyConstants.RAT_EHRPD;
            case TelephonyManager.NETWORK_TYPE_LTE:
                return TelephonyConstants.RAT_LTE;
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return TelephonyConstants.RAT_HSPAP;
            case TelephonyManager.NETWORK_TYPE_GSM:
                return TelephonyConstants.RAT_GSM;
            case TelephonyManager. NETWORK_TYPE_TD_SCDMA:
                return TelephonyConstants.RAT_TD_SCDMA;
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return TelephonyConstants.RAT_IWLAN;
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return TelephonyConstants.RAT_IDEN;
        }
        return TelephonyConstants.RAT_UNKNOWN;
    }

    public static String getNetworkStateString(int state) {
        switch(state) {
            case ServiceState.STATE_EMERGENCY_ONLY:
                return TelephonyConstants.SERVICE_STATE_EMERGENCY_ONLY;
            case ServiceState.STATE_IN_SERVICE:
                return TelephonyConstants.SERVICE_STATE_IN_SERVICE;
            case ServiceState.STATE_OUT_OF_SERVICE:
                return TelephonyConstants.SERVICE_STATE_OUT_OF_SERVICE;
            case ServiceState.STATE_POWER_OFF:
                return TelephonyConstants.SERVICE_STATE_POWER_OFF;
            default:
                return TelephonyConstants.SERVICE_STATE_UNKNOWN;
        }
   }

    public static String getSrvccStateString(int srvccState) {
        switch (srvccState) {
            case VoLteServiceState.HANDOVER_STARTED:
                return TelephonyConstants.VOLTE_SERVICE_STATE_HANDOVER_STARTED;
            case VoLteServiceState.HANDOVER_COMPLETED:
                return TelephonyConstants.VOLTE_SERVICE_STATE_HANDOVER_COMPLETED;
            case VoLteServiceState.HANDOVER_FAILED:
                return TelephonyConstants.VOLTE_SERVICE_STATE_HANDOVER_FAILED;
            case VoLteServiceState.HANDOVER_CANCELED:
                return TelephonyConstants.VOLTE_SERVICE_STATE_HANDOVER_CANCELED;
            default:
                Log.e(String.format("getSrvccStateString():"
                        + "unknown state %d", srvccState));
                return TelephonyConstants.VOLTE_SERVICE_STATE_HANDOVER_UNKNOWN;
        }
    };
}
