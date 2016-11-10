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

import com.google.android.mms.ContentType;
import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.pdu.CharacterSets;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPart;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.SendConf;
import com.google.android.mms.pdu.SendReq;
import com.google.android.mms.MmsException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony.Sms.Intents;
import android.provider.Telephony.Mms;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SmsCbMessage;
import com.android.internal.telephony.gsm.SmsCbConstants;
import com.android.internal.telephony.cdma.sms.SmsEnvelope;
import android.telephony.SmsCbEtwsInfo;
import android.telephony.SmsCbCmasInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcDefault;
import com.googlecode.android_scripting.rpc.RpcOptional;
import com.googlecode.android_scripting.rpc.RpcParameter;
import com.googlecode.android_scripting.facade.telephony.TelephonyConstants;

//FIXME: Change the build order to use constants defined in here
//import com.googlecode.android_scripting.provider.TelephonyTestProvider;

/**
 * Exposes SmsManager functionality.
 */
public class SmsFacade extends RpcReceiver {

    static final boolean DBG = false;

    private final EventFacade mEventFacade;
    private final SmsManager mSms;
    private final Context mContext;
    private final Service mService;
    private BroadcastReceiver mSmsSendListener;
    private BroadcastReceiver mSmsIncomingListener;
    private int mNumExpectedSentEvents;
    private int mNumExpectedDeliveredEvents;
    private boolean mListeningIncomingSms;
    private IntentFilter mEmergencyCBMessage;
    private BroadcastReceiver mGsmEmergencyCBMessageListener;
    private BroadcastReceiver mCdmaEmergencyCBMessageListener;
    private boolean mGsmEmergencyCBListenerRegistered;
    private boolean mCdmaEmergencyCBListenerRegistered;
    private boolean mSentReceiversRegistered;
    private Object lock = new Object();

    private BroadcastReceiver mMmsSendListener;
    private BroadcastReceiver mMmsIncomingListener;
    private boolean mListeningIncomingMms;

    TelephonyManager mTelephonyManager;

    private static final String SMS_MESSAGE_STATUS_DELIVERED_ACTION =
            "com.googlecode.android_scripting.sms.MESSAGE_STATUS_DELIVERED";
    private static final String SMS_MESSAGE_SENT_ACTION =
            "com.googlecode.android_scripting.sms.MESSAGE_SENT";

    private static final String EMERGENCY_CB_MESSAGE_RECEIVED_ACTION =
            "android.provider.Telephony.SMS_EMERGENCY_CB_RECEIVED";

    private static final String MMS_MESSAGE_SENT_ACTION =
            "com.googlecode.android_scripting.mms.MESSAGE_SENT";

    private final int MAX_MESSAGE_LENGTH = 160;
    private final int INTERNATIONAL_NUMBER_LENGTH = 12;
    private final int DOMESTIC_NUMBER_LENGTH = 10;

    private static final String DEFAULT_FROM_PHONE_NUMBER = new String("8675309");

    private final int[] mGsmCbMessageIdList = {
            SmsCbConstants.MESSAGE_ID_ETWS_EARTHQUAKE_WARNING,
            SmsCbConstants.MESSAGE_ID_ETWS_TSUNAMI_WARNING,
            SmsCbConstants.MESSAGE_ID_ETWS_EARTHQUAKE_AND_TSUNAMI_WARNING,
            SmsCbConstants.MESSAGE_ID_ETWS_TEST_MESSAGE,
            SmsCbConstants.MESSAGE_ID_ETWS_OTHER_EMERGENCY_TYPE,
            SmsCbConstants.MESSAGE_ID_CMAS_ALERT_PRESIDENTIAL_LEVEL,
            SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_IMMEDIATE_OBSERVED,
            SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_IMMEDIATE_LIKELY,
            SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_EXPECTED_OBSERVED,
            SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_EXPECTED_LIKELY,
            SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_IMMEDIATE_LIKELY,
            SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_EXPECTED_OBSERVED,
            SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_EXPECTED_LIKELY,
            SmsCbConstants.MESSAGE_ID_CMAS_ALERT_CHILD_ABDUCTION_EMERGENCY,
            SmsCbConstants.MESSAGE_ID_CMAS_ALERT_REQUIRED_MONTHLY_TEST,
            SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXERCISE
    };

    private final int[] mCdmaCbMessageIdList = {
            SmsEnvelope.SERVICE_CATEGORY_CMAS_PRESIDENTIAL_LEVEL_ALERT,
            SmsEnvelope.SERVICE_CATEGORY_CMAS_EXTREME_THREAT,
            SmsEnvelope.SERVICE_CATEGORY_CMAS_SEVERE_THREAT,
            SmsEnvelope.SERVICE_CATEGORY_CMAS_CHILD_ABDUCTION_EMERGENCY,
            SmsEnvelope.SERVICE_CATEGORY_CMAS_TEST_MESSAGE
    };

    public SmsFacade(FacadeManager manager) {

        super(manager);
        mService = manager.getService();
        mContext = mService;
        mSms = SmsManager.getDefault();
        mEventFacade = manager.getReceiver(EventFacade.class);
        mSmsSendListener = new SmsSendListener();
        mSmsIncomingListener = new SmsIncomingListener();
        mNumExpectedSentEvents = 0;
        mNumExpectedDeliveredEvents = 0;
        mListeningIncomingSms = false;
        mGsmEmergencyCBMessageListener = new SmsEmergencyCBMessageListener();
        mCdmaEmergencyCBMessageListener = new SmsEmergencyCBMessageListener();
        mGsmEmergencyCBListenerRegistered = false;
        mCdmaEmergencyCBListenerRegistered = false;
        mSentReceiversRegistered = false;

        mMmsIncomingListener = new MmsIncomingListener();
        mMmsSendListener = new MmsSendListener();

        mListeningIncomingMms = false;

        IntentFilter smsFilter = new IntentFilter(SMS_MESSAGE_SENT_ACTION);
        smsFilter.addAction(SMS_MESSAGE_STATUS_DELIVERED_ACTION);

        IntentFilter mmsFilter = new IntentFilter(MMS_MESSAGE_SENT_ACTION);

        synchronized (lock) {
            mService.registerReceiver(mSmsSendListener, smsFilter);
            mService.registerReceiver(mMmsSendListener, mmsFilter);
            mSentReceiversRegistered = true;
        }

        mTelephonyManager =
                (TelephonyManager) mService.getSystemService(Context.TELEPHONY_SERVICE);
    }

    // FIXME: Move to a utility class
    // FIXME: remove the MODE_WORLD_READABLE once we verify the use case
    @SuppressWarnings("deprecation")
    private boolean writeBytesToFile(String fileName, byte[] pdu) {
        FileOutputStream writer = null;
        try {
            writer = mContext.openFileOutput(fileName, Context.MODE_WORLD_READABLE);
            writer.write(pdu);
            return true;
        } catch (final IOException e) {
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
        }
    }

    // FIXME: Move to a utility class
    private boolean writeBytesToCacheFile(String fileName, byte[] pdu) {
        File mmsFile = new File(mContext.getCacheDir(), fileName);
        Log.d(String.format("filename:%s, directory:%s", fileName,
                mContext.getCacheDir().toString()));
        FileOutputStream writer = null;
        try {
            writer = new FileOutputStream(mmsFile);
            writer.write(pdu);
            return true;
        } catch (final IOException e) {
            Log.d("writeBytesToCacheFile() failed with " + e.toString());
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Deprecated
    @Rpc(description = "Starts tracking incoming SMS.")
    public void smsStartTrackingIncomingMessage() {
        Log.d("Using Deprecated smsStartTrackingIncomingMessage!");
        smsStartTrackingIncomingSmsMessage();
    }

    @Rpc(description = "Starts tracking incoming SMS.")
    public void smsStartTrackingIncomingSmsMessage() {
        mService.registerReceiver(mSmsIncomingListener,
                new IntentFilter(Intents.SMS_RECEIVED_ACTION));
        mListeningIncomingSms = true;
    }

    @Deprecated
    @Rpc(description = "Stops tracking incoming SMS.")
    public void smsStopTrackingIncomingMessage() {
        Log.d("Using Deprecated smsStopTrackingIncomingMessage!");
        smsStopTrackingIncomingSmsMessage();
    }

    @Rpc(description = "Stops tracking incoming SMS.")
    public void smsStopTrackingIncomingSmsMessage() {
        if (mListeningIncomingSms) {
            mListeningIncomingSms = false;
            try {
                mService.unregisterReceiver(mSmsIncomingListener);
            } catch (Exception e) {
                Log.e("Tried to unregister nonexistent SMS Listener!");
            }
        }
    }

    @Rpc(description = "Starts tracking incoming MMS.")
    public void smsStartTrackingIncomingMmsMessage() {
        IntentFilter mmsReceived = new IntentFilter(Intents.MMS_DOWNLOADED_ACTION);
        mmsReceived.addAction(Intents.WAP_PUSH_RECEIVED_ACTION);
        mmsReceived.addAction(Intents.DATA_SMS_RECEIVED_ACTION);
        mService.registerReceiver(mMmsIncomingListener, mmsReceived);
        mListeningIncomingSms = true;
    }

    @Rpc(description = "Stops tracking incoming MMS.")
    public void smsStopTrackingIncomingMmsMessage() {
        if (mListeningIncomingMms) {
            mListeningIncomingMms = false;
            try {
                mService.unregisterReceiver(mMmsIncomingListener);
            } catch (Exception e) {
                Log.e("Tried to unregister nonexistent MMS Listener!");
            }
        }
    }

    // Currently requires 'adb shell su root setenforce 0'
    @Rpc(description = "Send a multimedia message to a specified number.")
    public void smsSendMultimediaMessage(
                        @RpcParameter(name = "toPhoneNumber")
            String toPhoneNumber,
                        @RpcParameter(name = "subject")
            String subject,
                        @RpcParameter(name = "message")
            String message,
            @RpcParameter(name = "fromPhoneNumber")
            @RpcOptional
            String fromPhoneNumber,
            @RpcParameter(name = "fileName")
            @RpcOptional
            String fileName) {

        MmsBuilder mms = new MmsBuilder();

        mms.setToPhoneNumber(toPhoneNumber);
        if (fromPhoneNumber == null) {
            mTelephonyManager.getLine1Number(); //TODO: b/21592513 - multi-sim awareness
        }

        if (DBG) {
            Log.d(String.format(
                    "Params:toPhoneNumber(%s),subject(%s),message(%s),fromPhoneNumber(%s),filename(%s)",
                    toPhoneNumber, subject, message,
                    (fromPhoneNumber != null) ? fromPhoneNumber : "",
                            (fileName != null) ? fileName : ""));
        }

        mms.setFromPhoneNumber((fromPhoneNumber != null) ? fromPhoneNumber : DEFAULT_FROM_PHONE_NUMBER);
        mms.setSubject(subject);
        mms.setDate();
        mms.addMessageBody(message);
        mms.setMessageClass(MmsBuilder.MESSAGE_CLASS_PERSONAL);
        mms.setMessagePriority(MmsBuilder.DEFAULT_PRIORITY);
        mms.setDeliveryReport(true);
        mms.setReadReport(true);
        // Default to 1 week;
        mms.setExpirySeconds(MmsBuilder.DEFAULT_EXPIRY_TIME);

        Uri contentUri = null;

        String randomFileName = "mms." + String.valueOf(System.currentTimeMillis()) + ".dat";

        byte[] mmsBytes = mms.build();
        if (mmsBytes.length == 0) {
            Log.e("Failed to build PDU!");
            return;
        }

        if (writeBytesToCacheFile(randomFileName, mmsBytes) == false) {
            Log.e("Failed to write PDU to file");
            return;
        }

        contentUri = (new Uri.Builder())
                .authority("com.googlecode.android_scripting.provider.telephonytestprovider")
                .path("mms/" + randomFileName)
                .scheme(ContentResolver.SCHEME_CONTENT)
                .build();

        if (contentUri != null) {
            Log.d(String.format("URI String: %s", contentUri.toString()));

            SmsManager.getDefault().sendMultimediaMessage(mContext,
                    contentUri, null/* locationUrl */, null/* configOverrides */,
                    PendingIntent.getBroadcast(mService, 0,
                            new Intent(MMS_MESSAGE_SENT_ACTION), 0)
                    );
        }
        else {
            Log.d("smsSendMultimediaMessage():Content URI String is null");
        }
    }

    @Rpc(description = "Send a text message to a specified number.")
    public void smsSendTextMessage(
                        @RpcParameter(name = "phoneNumber")
            String phoneNumber,
                        @RpcParameter(name = "message")
            String message,
                        @RpcParameter(name = "deliveryReportRequired")
            Boolean deliveryReportRequired) {

        if (message.length() > MAX_MESSAGE_LENGTH) {
            ArrayList<String> messagesParts = mSms.divideMessage(message);
            mNumExpectedSentEvents = mNumExpectedDeliveredEvents = messagesParts.size();
            ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();
            ArrayList<PendingIntent> deliveredIntents = new ArrayList<PendingIntent>();
            for (int i = 0; i < messagesParts.size(); i++) {
                sentIntents.add(PendingIntent.getBroadcast(mService, 0,
                        new Intent(SMS_MESSAGE_SENT_ACTION), 0));
                if (deliveryReportRequired) {
                    deliveredIntents.add(
                            PendingIntent.getBroadcast(mService, 0,
                                    new Intent(SMS_MESSAGE_STATUS_DELIVERED_ACTION), 0));
                }
            }
            mSms.sendMultipartTextMessage(
                    phoneNumber, null, messagesParts,
                    sentIntents, deliveryReportRequired ? deliveredIntents : null);
        } else {
            mNumExpectedSentEvents = mNumExpectedDeliveredEvents = 1;
            PendingIntent sentIntent = PendingIntent.getBroadcast(mService, 0,
                    new Intent(SMS_MESSAGE_SENT_ACTION), 0);
            PendingIntent deliveredIntent = PendingIntent.getBroadcast(mService, 0,
                    new Intent(SMS_MESSAGE_STATUS_DELIVERED_ACTION), 0);
            mSms.sendTextMessage(
                    phoneNumber, null, message, sentIntent,
                    deliveryReportRequired ? deliveredIntent : null);
        }
    }

    @Rpc(description = "Retrieves all messages currently stored on ICC.")
    public ArrayList<SmsMessage> smsGetAllMessagesFromIcc() {
        return SmsManager.getDefault().getAllMessagesFromIcc();
    }

    @Rpc(description = "Starts tracking GSM Emergency CB Messages.")
    public void smsStartTrackingGsmEmergencyCBMessage() {
        if (!mGsmEmergencyCBListenerRegistered) {
            for (int messageId : mGsmCbMessageIdList) {
                mSms.enableCellBroadcast(
                        messageId,
                        SmsManager.CELL_BROADCAST_RAN_TYPE_GSM);
            }

            mEmergencyCBMessage = new IntentFilter(EMERGENCY_CB_MESSAGE_RECEIVED_ACTION);
            mService.registerReceiver(mGsmEmergencyCBMessageListener,
                    mEmergencyCBMessage);
            mGsmEmergencyCBListenerRegistered = true;
        }
    }

    @Rpc(description = "Stop tracking GSM Emergency CB Messages")
    public void smsStopTrackingGsmEmergencyCBMessage() {
        if (mGsmEmergencyCBListenerRegistered) {
            mService.unregisterReceiver(mGsmEmergencyCBMessageListener);
            mGsmEmergencyCBListenerRegistered = false;
            for (int messageId : mGsmCbMessageIdList) {
                mSms.disableCellBroadcast(
                        messageId,
                        SmsManager.CELL_BROADCAST_RAN_TYPE_GSM);
            }
        }
    }

    @Rpc(description = "Starts tracking CDMA Emergency CB Messages")
    public void smsStartTrackingCdmaEmergencyCBMessage() {
        if (!mCdmaEmergencyCBListenerRegistered) {
            for (int messageId : mCdmaCbMessageIdList) {
                mSms.enableCellBroadcast(
                        messageId,
                        SmsManager.CELL_BROADCAST_RAN_TYPE_CDMA);
            }
            mEmergencyCBMessage = new IntentFilter(EMERGENCY_CB_MESSAGE_RECEIVED_ACTION);
            mService.registerReceiver(mCdmaEmergencyCBMessageListener,
                    mEmergencyCBMessage);
            mCdmaEmergencyCBListenerRegistered = true;
        }
    }

    @Rpc(description = "Stop tracking CDMA Emergency CB Message.")
    public void smsStopTrackingCdmaEmergencyCBMessage() {
        if (mCdmaEmergencyCBListenerRegistered) {
            mService.unregisterReceiver(mCdmaEmergencyCBMessageListener);
            mCdmaEmergencyCBListenerRegistered = false;
            for (int messageId : mCdmaCbMessageIdList) {
                mSms.disableCellBroadcast(
                        messageId,
                        SmsManager.CELL_BROADCAST_RAN_TYPE_CDMA);
            }
        }
    }

    private class SmsSendListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle event = new Bundle();
            event.putString("Type", "SmsDeliverStatus");
            String action = intent.getAction();
            int resultCode = getResultCode();
            if (SMS_MESSAGE_STATUS_DELIVERED_ACTION.equals(action)) {
                if (resultCode == Activity.RESULT_OK) {
                    if (mNumExpectedDeliveredEvents == 1) {
                        Log.d("SMS Message delivered successfully");
                        mEventFacade.postEvent(TelephonyConstants.EventSmsDeliverSuccess, event);
                    }
                    if (mNumExpectedDeliveredEvents > 0) {
                        mNumExpectedDeliveredEvents--;
                    }
                } else {
                    Log.e("SMS Message delivery failed");
                    // TODO . Need to find the reason for failure from pdu
                    mEventFacade.postEvent(TelephonyConstants.EventSmsDeliverFailure, event);
                }
            } else if (SMS_MESSAGE_SENT_ACTION.equals(action)) {
                if (resultCode == Activity.RESULT_OK) {
                    if (mNumExpectedSentEvents == 1) {
                        event.putString("Type", "SmsSentSuccess");
                        Log.d("SMS Message sent successfully");
                        mEventFacade.postEvent(TelephonyConstants.EventSmsSentSuccess, event);
                    }
                    if (mNumExpectedSentEvents > 0) {
                        mNumExpectedSentEvents--;
                    }
                } else {
                    Log.e("SMS Message send failed");
                    event.putString("Type", "SmsSentFailure");
                    switch (resultCode) {
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            event.putString("Reason", "GenericFailure");
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            event.putString("Reason", "RadioOff");
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            event.putString("Reason", "NullPdu");
                            break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            event.putString("Reason", "NoService");
                            break;
                        case SmsManager.RESULT_ERROR_LIMIT_EXCEEDED:
                            event.putString("Reason", "LimitExceeded");
                            break;
                        case SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE:
                            event.putString("Reason", "FdnCheckFailure");
                            break;
                        default:
                            event.putString("Reason", "Unknown");
                            break;
                    }
                    mEventFacade.postEvent(TelephonyConstants.EventSmsSentFailure, event);
                }
            }
        }
    }

    private class SmsIncomingListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intents.SMS_RECEIVED_ACTION.equals(action)) {
                Log.d("New SMS Received");
                Bundle extras = intent.getExtras();
                int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
                if (extras != null) {
                    Bundle event = new Bundle();
                    event.putString("Type", "NewSmsReceived");
                    SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
                    StringBuilder smsMsg = new StringBuilder();

                    SmsMessage sms = msgs[0];
                    String sender = sms.getOriginatingAddress();
                    event.putString("Sender", formatPhoneNumber(sender));

                    for (int i = 0; i < msgs.length; i++) {
                        sms = msgs[i];
                        smsMsg.append(sms.getMessageBody());
                    }
                    event.putString("Text", smsMsg.toString());
                    // TODO
                    // Need to explore how to get subId information.
                    event.putInt("subscriptionId", subId);
                    mEventFacade.postEvent(TelephonyConstants.EventSmsReceived, event);
                }
            }
        }
    }

    private class MmsSendListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle event = new Bundle();
            String action = intent.getAction();
            int resultCode = getResultCode();
            event.putString("ResultCode", Integer.toString(resultCode));
            if (MMS_MESSAGE_SENT_ACTION.equals(action)) {
                if (resultCode == Activity.RESULT_OK) {
                    Log.d("MMS Message sent successfully");
                    mEventFacade.postEvent(TelephonyConstants.EventMmsSentSuccess, event);
                } else {
                    Log.e(String.format("MMS Message send failed: %d", resultCode));
                    mEventFacade.postEvent(TelephonyConstants.EventMmsSentFailure, event);
                }
            } else {
                Log.e("MMS Send Listener Received Invalid Event" + intent.toString());
            }
        }
    }

    // b/21569494 - Never receiving ANY of these events: requires debugging
    private class MmsIncomingListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("MmsIncomingListener Received an Intent " + intent.toString());
            String action = intent.getAction();
            if (Intents.MMS_DOWNLOADED_ACTION.equals(action)) {
                Log.d("New MMS Downloaded");
                mEventFacade.postEvent(TelephonyConstants.EventMmsDownloaded, new Bundle());
            }
            else if (Intents.WAP_PUSH_RECEIVED_ACTION.equals(action)) {
                Log.d("New Wap Push Received");
                mEventFacade.postEvent(TelephonyConstants.EventWapPushReceived, new Bundle());
            }
            else if (Intents.DATA_SMS_RECEIVED_ACTION.equals(action)) {
                Log.d("New Data SMS Received");
                mEventFacade.postEvent(TelephonyConstants.EventDataSmsReceived, new Bundle());
            }
            else {
                Log.e("MmsIncomingListener Received Unexpected Event" + intent.toString());
            }
        }
    }

    String formatPhoneNumber(String phoneNumber) {
        String senderNumberStr = null;
        int len = phoneNumber.length();
        if (len > 0) {
            /**
             * Currently this incomingNumber modification is specific for US numbers.
             */
            if ((INTERNATIONAL_NUMBER_LENGTH == len) && ('+' == phoneNumber.charAt(0))) {
                senderNumberStr = phoneNumber.substring(1);
            } else if (DOMESTIC_NUMBER_LENGTH == len) {
                senderNumberStr = '1' + phoneNumber;
            } else {
                senderNumberStr = phoneNumber;
            }
        }
        return senderNumberStr;
    }

    private class SmsEmergencyCBMessageListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (EMERGENCY_CB_MESSAGE_RECEIVED_ACTION.equals(intent.getAction())) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    Bundle event = new Bundle();
                    String eventName = null;
                    SmsCbMessage message = (SmsCbMessage) extras.get("message");
                    if (message != null) {
                        if (message.isEmergencyMessage()) {
                            event.putString("geographicalScope", getGeographicalScope(
                                    message.getGeographicalScope()));
                            event.putInt("serialNumber", message.getSerialNumber());
                            event.putString("location", message.getLocation().toString());
                            event.putInt("serviceCategory", message.getServiceCategory());
                            event.putString("language", message.getLanguageCode());
                            event.putString("message", message.getMessageBody());
                            event.putString("priority", getPriority(message.getMessagePriority()));
                            if (message.isCmasMessage()) {
                                // CMAS message
                                eventName = TelephonyConstants.EventCmasReceived;
                                event.putString("cmasMessageClass", getCMASMessageClass(
                                        message.getCmasWarningInfo().getMessageClass()));
                                event.putString("cmasCategory", getCMASCategory(
                                        message.getCmasWarningInfo().getCategory()));
                                event.putString("cmasResponseType", getCMASResponseType(
                                        message.getCmasWarningInfo().getResponseType()));
                                event.putString("cmasSeverity", getCMASSeverity(
                                        message.getCmasWarningInfo().getSeverity()));
                                event.putString("cmasUrgency", getCMASUrgency(
                                        message.getCmasWarningInfo().getUrgency()));
                                event.putString("cmasCertainty", getCMASCertainty(
                                        message.getCmasWarningInfo().getCertainty()));
                            } else if (message.isEtwsMessage()) {
                                // ETWS message
                                eventName = TelephonyConstants.EventEtwsReceived;
                                event.putString("etwsWarningType", getETWSWarningType(
                                        message.getEtwsWarningInfo().getWarningType()));
                                event.putBoolean("etwsIsEmergencyUserAlert",
                                        message.getEtwsWarningInfo().isEmergencyUserAlert());
                                event.putBoolean("etwsActivatePopup",
                                        message.getEtwsWarningInfo().isPopupAlert());
                            } else {
                                Log.d("Received message is not CMAS or ETWS");
                            }
                            if (eventName != null)
                                mEventFacade.postEvent(eventName, event);
                        }
                    }
                } else {
                    Log.d("Received  Emergency CB without extras");
                }
            }
        }
    }

    private static String getETWSWarningType(int type) {
        switch (type) {
            case SmsCbEtwsInfo.ETWS_WARNING_TYPE_EARTHQUAKE:
                return "EARTHQUAKE";
            case SmsCbEtwsInfo.ETWS_WARNING_TYPE_TSUNAMI:
                return "TSUNAMI";
            case SmsCbEtwsInfo.ETWS_WARNING_TYPE_EARTHQUAKE_AND_TSUNAMI:
                return "EARTHQUAKE_AND_TSUNAMI";
            case SmsCbEtwsInfo.ETWS_WARNING_TYPE_TEST_MESSAGE:
                return "TEST_MESSAGE";
            case SmsCbEtwsInfo.ETWS_WARNING_TYPE_OTHER_EMERGENCY:
                return "OTHER_EMERGENCY";
        }
        return "UNKNOWN";
    }

    private static String getCMASMessageClass(int messageclass) {
        switch (messageclass) {
            case SmsCbCmasInfo.CMAS_CLASS_PRESIDENTIAL_LEVEL_ALERT:
                return "PRESIDENTIAL_LEVEL_ALERT";
            case SmsCbCmasInfo.CMAS_CLASS_EXTREME_THREAT:
                return "EXTREME_THREAT";
            case SmsCbCmasInfo.CMAS_CLASS_SEVERE_THREAT:
                return "SEVERE_THREAT";
            case SmsCbCmasInfo.CMAS_CLASS_CHILD_ABDUCTION_EMERGENCY:
                return "CHILD_ABDUCTION_EMERGENCY";
            case SmsCbCmasInfo.CMAS_CLASS_REQUIRED_MONTHLY_TEST:
                return "REQUIRED_MONTHLY_TEST";
            case SmsCbCmasInfo.CMAS_CLASS_CMAS_EXERCISE:
                return "CMAS_EXERCISE";
        }
        return "UNKNOWN";
    }

    private static String getCMASCategory(int category) {
        switch (category) {
            case SmsCbCmasInfo.CMAS_CATEGORY_GEO:
                return "GEOPHYSICAL";
            case SmsCbCmasInfo.CMAS_CATEGORY_MET:
                return "METEOROLOGICAL";
            case SmsCbCmasInfo.CMAS_CATEGORY_SAFETY:
                return "SAFETY";
            case SmsCbCmasInfo.CMAS_CATEGORY_SECURITY:
                return "SECURITY";
            case SmsCbCmasInfo.CMAS_CATEGORY_RESCUE:
                return "RESCUE";
            case SmsCbCmasInfo.CMAS_CATEGORY_FIRE:
                return "FIRE";
            case SmsCbCmasInfo.CMAS_CATEGORY_HEALTH:
                return "HEALTH";
            case SmsCbCmasInfo.CMAS_CATEGORY_ENV:
                return "ENVIRONMENTAL";
            case SmsCbCmasInfo.CMAS_CATEGORY_TRANSPORT:
                return "TRANSPORTATION";
            case SmsCbCmasInfo.CMAS_CATEGORY_INFRA:
                return "INFRASTRUCTURE";
            case SmsCbCmasInfo.CMAS_CATEGORY_CBRNE:
                return "CHEMICAL";
            case SmsCbCmasInfo.CMAS_CATEGORY_OTHER:
                return "OTHER";
        }
        return "UNKNOWN";
    }

    private static String getCMASResponseType(int type) {
        switch (type) {
            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_SHELTER:
                return "SHELTER";
            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_EVACUATE:
                return "EVACUATE";
            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_PREPARE:
                return "PREPARE";
            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_EXECUTE:
                return "EXECUTE";
            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_MONITOR:
                return "MONITOR";
            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_AVOID:
                return "AVOID";
            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_ASSESS:
                return "ASSESS";
            case SmsCbCmasInfo.CMAS_RESPONSE_TYPE_NONE:
                return "NONE";
        }
        return "UNKNOWN";
    }

    private static String getCMASSeverity(int severity) {
        switch (severity) {
            case SmsCbCmasInfo.CMAS_SEVERITY_EXTREME:
                return "EXTREME";
            case SmsCbCmasInfo.CMAS_SEVERITY_SEVERE:
                return "SEVERE";
        }
        return "UNKNOWN";
    }

    private static String getCMASUrgency(int urgency) {
        switch (urgency) {
            case SmsCbCmasInfo.CMAS_URGENCY_IMMEDIATE:
                return "IMMEDIATE";
            case SmsCbCmasInfo.CMAS_URGENCY_EXPECTED:
                return "EXPECTED";
        }
        return "UNKNOWN";
    }

    private static String getCMASCertainty(int certainty) {
        switch (certainty) {
            case SmsCbCmasInfo.CMAS_CERTAINTY_OBSERVED:
                return "IMMEDIATE";
            case SmsCbCmasInfo.CMAS_CERTAINTY_LIKELY:
                return "LIKELY";
        }
        return "UNKNOWN";
    }

    private static String getGeographicalScope(int scope) {
        switch (scope) {
            case SmsCbMessage.GEOGRAPHICAL_SCOPE_CELL_WIDE_IMMEDIATE:
                return "CELL_WIDE_IMMEDIATE";
            case SmsCbMessage.GEOGRAPHICAL_SCOPE_PLMN_WIDE:
                return "PLMN_WIDE ";
            case SmsCbMessage.GEOGRAPHICAL_SCOPE_LA_WIDE:
                return "LA_WIDE";
            case SmsCbMessage.GEOGRAPHICAL_SCOPE_CELL_WIDE:
                return "CELL_WIDE";
        }
        return "UNKNOWN";
    }

    private static String getPriority(int priority) {
        switch (priority) {
            case SmsCbMessage.MESSAGE_PRIORITY_NORMAL:
                return "NORMAL";
            case SmsCbMessage.MESSAGE_PRIORITY_INTERACTIVE:
                return "INTERACTIVE";
            case SmsCbMessage.MESSAGE_PRIORITY_URGENT:
                return "URGENT";
            case SmsCbMessage.MESSAGE_PRIORITY_EMERGENCY:
                return "EMERGENCY";
        }
        return "UNKNOWN";
    }

    @Override
    public void shutdown() {

        smsStopTrackingIncomingSmsMessage();
        smsStopTrackingIncomingMmsMessage();
        smsStopTrackingGsmEmergencyCBMessage();
        smsStopTrackingCdmaEmergencyCBMessage();

        synchronized (lock) {
            if (mSentReceiversRegistered) {
                mService.unregisterReceiver(mSmsSendListener);
                mService.unregisterReceiver(mMmsSendListener);
                mSentReceiversRegistered = false;
            }
        }
    }

    private class MmsBuilder {

        public static final String MESSAGE_CLASS_PERSONAL =
                PduHeaders.MESSAGE_CLASS_PERSONAL_STR;

        public static final String MESSAGE_CLASS_ADVERTISEMENT =
                PduHeaders.MESSAGE_CLASS_ADVERTISEMENT_STR;

        public static final String MESSAGE_CLASS_INFORMATIONAL =
                PduHeaders.MESSAGE_CLASS_INFORMATIONAL_STR;

        public static final String MESSAGE_CLASS_AUTO =
                PduHeaders.MESSAGE_CLASS_AUTO_STR;

        public static final int MESSAGE_PRIORITY_LOW = PduHeaders.PRIORITY_LOW;
        public static final int MESSAGE_PRIORITY_NORMAL = PduHeaders.PRIORITY_LOW;
        public static final int MESSAGE_PRIORITY_HIGH = PduHeaders.PRIORITY_LOW;

        private static final int DEFAULT_EXPIRY_TIME = 7 * 24 * 60 * 60;
        private static final int DEFAULT_PRIORITY = PduHeaders.PRIORITY_NORMAL;

        private SendReq mRequest;
        private PduBody mBody;

        // FIXME: Eventually this should be exposed as a parameter
        private static final String TEMP_CONTENT_FILE_NAME = "text0.txt";

        // Synchronized Multimedia Internet Language
        // Fragment for compatibility
        private static final String sSmilText =
                "<smil>" +
                        "<head>" +
                        "<layout>" +
                        "<root-layout/>" +
                        "<region height=\"100%%\" id=\"Text\" left=\"0%%\"" +
                        " top=\"0%%\" width=\"100%%\"/>" +
                        "</layout>" +
                        "</head>" +
                        "<body>" +
                        "<par dur=\"8000ms\">" +
                        "<text src=\"%s\" region=\"Text\"/>" +
                        "</par>" +
                        "</body>" +
                        "</smil>";

        public MmsBuilder() {
            mRequest = new SendReq();
            mBody = new PduBody();
        }

        public void setFromPhoneNumber(String number) {
            mRequest.setFrom(new EncodedStringValue(number));
        }

        public void setToPhoneNumber(String number) {
            mRequest.setTo(new EncodedStringValue[] {
                    new EncodedStringValue(number) });
        }

        public void setToPhoneNumbers(List<String> number) {
            mRequest.setTo(EncodedStringValue.encodeStrings((String[]) number.toArray()));
        }

        public void setSubject(String subject) {
            mRequest.setSubject(new EncodedStringValue(subject));
        }

        public void setDate() {
            setDate(System.currentTimeMillis() / 1000);
        }

        public void setDate(long time) {
            mRequest.setDate(time);
        }

        public void addMessageBody(String message) {
            addMessageBody(message, true);
        }

        public void setMessageClass(String messageClass) {
            mRequest.setMessageClass(messageClass.getBytes());
        }

        public void setMessagePriority(int priority) {
            try {
                mRequest.setPriority(priority);
            } catch (InvalidHeaderValueException e) {
                Log.e("Invalid Header Value "+e.toString());
            }
        }

        public void setDeliveryReport(boolean report) {
            try {
                mRequest.setDeliveryReport((report) ? PduHeaders.VALUE_YES : PduHeaders.VALUE_NO);
            } catch (InvalidHeaderValueException e) {
                Log.e("Invalid Header Value "+e.toString());
            }
        }

        public void setReadReport(boolean report) {
            try {
                mRequest.setReadReport((report) ? PduHeaders.VALUE_YES : PduHeaders.VALUE_NO);
            } catch (InvalidHeaderValueException e) {
                Log.e("Invalid Header Value "+e.toString());
            }
        }

        public void setExpirySeconds(int seconds) {
            mRequest.setExpiry(seconds);
        }

        public byte[] build() {
            mRequest.setBody(mBody);

            int msgSize = 0;
            for (int i = 0; i < mBody.getPartsNum(); i++) {
                msgSize += mBody.getPart(i).getDataLength();
            }
            mRequest.setMessageSize(msgSize);

            return new PduComposer(mContext, mRequest).make();
        }

        public void addMessageBody(String message, boolean addSmilFragment) {
            final PduPart part = new PduPart();
            part.setCharset(CharacterSets.UTF_8);
            part.setContentType(ContentType.TEXT_PLAIN.getBytes());
            part.setContentLocation("text0".getBytes());
            int index = TEMP_CONTENT_FILE_NAME.lastIndexOf(".");
            String contentId = (index == -1) ? TEMP_CONTENT_FILE_NAME
                    : TEMP_CONTENT_FILE_NAME.substring(0, index);
            part.setContentId(contentId.getBytes());
            part.setContentId("txt".getBytes());
            part.setData(message.getBytes());
            mBody.addPart(part);
            if (addSmilFragment) {
                addSmilTextFragment(TEMP_CONTENT_FILE_NAME);
            }
        }

        private void addSmilTextFragment(String contentFilename) {

            final String smil = String.format(sSmilText, contentFilename);
            final PduPart smilPart = new PduPart();
            smilPart.setContentId("smil".getBytes());
            smilPart.setContentLocation("smil.xml".getBytes());
            smilPart.setContentType(ContentType.APP_SMIL.getBytes());
            smilPart.setData(smil.getBytes());
            mBody.addPart(0, smilPart);
        }
    }

}
