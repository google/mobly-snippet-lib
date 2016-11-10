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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.telecom.AudioState;
import android.telecom.Call;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.net.Uri;
import android.provider.ContactsContract;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.AndroidFacade;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcDefault;
import com.googlecode.android_scripting.rpc.RpcOptional;
import com.googlecode.android_scripting.rpc.RpcParameter;

/**
 * Exposes TelecomManager functionality.
 */
public class TelecomManagerFacade extends RpcReceiver {

    private final Service mService;
    private final AndroidFacade mAndroidFacade;

    private final TelecomManager mTelecomManager;
    private final TelephonyManager mTelephonyManager;

    private List<PhoneAccountHandle> mEnabledAccountHandles = null;

    public TelecomManagerFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mTelecomManager = new TelecomManager(mService);
        mTelephonyManager = new TelephonyManager(mService);
        mAndroidFacade = manager.getReceiver(AndroidFacade.class);
        InCallServiceImpl.setEventFacade(
                manager.getReceiver(EventFacade.class));
    }

    @Override
    public void shutdown() {
        InCallServiceImpl.setEventFacade(null);
    }

    @Rpc(description = "If there's a ringing call, accept on behalf of the user.")
    public void telecomAcceptRingingCall(
            @RpcOptional
            String videoState) {

        if (videoState == null) {
            mTelecomManager.acceptRingingCall();
        }
        else {
            int state = InCallServiceImpl.getVideoCallState(videoState);

            if (state == InCallServiceImpl.STATE_INVALID) {
                Log.e("telecomAcceptRingingCall: video state is invalid!");
                return;
            }

            mTelecomManager.acceptRingingCall(state);
        }
    }

    @Rpc(description = "Removes the missed-call notification if one is present.")
    public void telecomCancelMissedCallsNotification() {
        mTelecomManager.cancelMissedCallsNotification();
    }

    @Rpc(description = "Remove all Accounts that belong to the calling package from the system.")
    public void telecomClearAccounts() {
        mTelecomManager.clearAccounts();
    }

    @Rpc(description = "End an ongoing call.")
    public Boolean telecomEndCall() {
        return mTelecomManager.endCall();
    }

    @Rpc(description = "Get a list of all PhoneAccounts.")
    public List<PhoneAccount> telecomGetAllPhoneAccounts() {
        return mTelecomManager.getAllPhoneAccounts();
    }

    @Rpc(description = "Get the current call state.")
    public String telecomGetCallState() {
        int state = mTelecomManager.getCallState();
        return TelephonyUtils.getTelephonyCallStateString(state);
    }

    @Rpc(description = "Get the current tty mode.")
    public String telecomGetCurrentTtyMode() {
        int mode = mTelecomManager.getCurrentTtyMode();
        return TelephonyUtils.getTtyModeString(mode);
    }

    @Rpc(description = "Bring incallUI to foreground.")
    public void telecomShowInCallScreen(
            @RpcParameter(name = "showDialpad")
            @RpcOptional
            @RpcDefault("false")
            Boolean showDialpad) {
        mTelecomManager.showInCallScreen(showDialpad);
    }

    @Rpc(description = "Get the list of PhoneAccountHandles with calling capability.")
    public List<PhoneAccountHandle> telecomGetEnabledPhoneAccounts() {
        mEnabledAccountHandles = mTelecomManager.getCallCapablePhoneAccounts();
        return mEnabledAccountHandles;
    }

    @Rpc(description = "Set the user-chosen default PhoneAccount for making outgoing phone calls.")
    public void telecomSetUserSelectedOutgoingPhoneAccount(
                        @RpcParameter(name = "phoneAccountHandleId")
            String phoneAccountHandleId) throws Exception {

        List<PhoneAccountHandle> accountHandles = mTelecomManager
                .getAllPhoneAccountHandles();
        for (PhoneAccountHandle handle : accountHandles) {
            if (handle.getId().equals(phoneAccountHandleId)) {
                mTelecomManager.setUserSelectedOutgoingPhoneAccount(handle);
                Log.d(String.format("Set default Outgoing Phone Account(%s)",
                        phoneAccountHandleId));
                return;
            }
        }
        Log.d(String.format(
                "Failed to find a matching phoneAccountHandleId(%s).",
                phoneAccountHandleId));
        throw new Exception(String.format(
                "Failed to find a matching phoneAccountHandleId(%s).",
                phoneAccountHandleId));
    }

    @Rpc(description = "Get the user-chosen default PhoneAccount for making outgoing phone calls.")
    public PhoneAccountHandle telecomGetUserSelectedOutgoingPhoneAccount() {
        return mTelecomManager.getUserSelectedOutgoingPhoneAccount();
    }

    @Rpc(description = "Set the PhoneAccount corresponding to user selected subscription id " +
                       " for making outgoing phone calls.")
    public void telecomSetUserSelectedOutgoingPhoneAccountBySubId(
                        @RpcParameter(name = "subId")
                        Integer subId) throws Exception {
          Iterator<PhoneAccountHandle> phoneAccounts =
               mTelecomManager.getCallCapablePhoneAccounts().listIterator();

          while (phoneAccounts.hasNext()) {
              PhoneAccountHandle phoneAccountHandle = phoneAccounts.next();
              PhoneAccount phoneAccount =
                       mTelecomManager.getPhoneAccount(phoneAccountHandle);
              if (subId == mTelephonyManager.getSubIdForPhoneAccount(phoneAccount)) {
                  mTelecomManager.setUserSelectedOutgoingPhoneAccount(phoneAccountHandle);
                  Log.d(String.format(
                      "Set default Outgoing Phone Account for subscription(%s)", subId));
                  return;
              }
          }
          Log.d(String.format(
                  "Failed to find a matching Phone Account for subscription (%s).",
                  subId));
          throw new Exception(String.format(
                  "Failed to find a matching Phone Account for subscription (%s).",
                   subId));
    }

    @Rpc(description = "Returns whether there is an ongoing phone call.")
    public Boolean telecomIsInCall() {
        return mTelecomManager.isInCall();
    }

    @Rpc(description = "Returns whether there is a ringing incoming call.")
    public Boolean telecomIsRinging() {
        return mTelecomManager.isRinging();
    }

    @Rpc(description = "Silences the rigner if there's a ringing call.")
    public void telecomSilenceRinger() {
        mTelecomManager.silenceRinger();
    }

    @Rpc(description = "Swap two calls")
    public void telecomSwapCalls() {
        // TODO: b/26273475 Add logic to swap the foreground and back ground calls
    }

    @Rpc(description = "Start listening for added calls")
    public void telecomStartListeningForCallAdded() {
        InCallServiceImpl.CallListener.startListeningForEvent(
                InCallServiceImpl.CallListener.LISTEN_CALL_ADDED);
    }

    @Rpc(description = "Stop listening for added calls")
    public void telecomStopListeningForCallAdded() {
        InCallServiceImpl.CallListener.stopListeningForEvent(
                InCallServiceImpl.CallListener.LISTEN_CALL_ADDED);
    }

    @Rpc(description = "Start listening for removed calls")
    public void telecomStartListeningForCallRemoved() {
        InCallServiceImpl.CallListener.startListeningForEvent(
                InCallServiceImpl.CallListener.LISTEN_CALL_REMOVED);
    }

    @Rpc(description = "Stop listening for removed calls")
    public void telecomStopListeningForCallRemoved() {
        InCallServiceImpl.CallListener.stopListeningForEvent(
                InCallServiceImpl.CallListener.LISTEN_CALL_REMOVED);
    }

    @Rpc(description = "Toggles call waiting feature on or off for default voice subscription id.")
    public void toggleCallWaiting(
            @RpcParameter(name = "enabled")
            @RpcOptional
            Boolean enabled) {
        toggleCallWaitingForSubscription(
                SubscriptionManager.getDefaultVoiceSubscriptionId(), enabled);
    }

    @Rpc(description = "Toggles call waiting feature on or off for specified subscription id.")
    public void toggleCallWaitingForSubscription(
            @RpcParameter(name = "subId")
            @RpcOptional
            Integer subId,
            @RpcParameter(name = "enabled")
            @RpcOptional
            Boolean enabled) {
        // TODO: b/26273478 Enable or Disable the call waiting feature
    }

    @Rpc(description = "Sends an MMI string to Telecom for processing")
    public void telecomHandleMmi(
                        @RpcParameter(name = "dialString")
            String dialString) {
        mTelecomManager.handleMmi(dialString);
    }

    // TODO: b/20917712 add support to pass arbitrary "Extras" object
    // for videoCall parameter
    @Deprecated
    @Rpc(description = "Calls a phone by resolving a generic URI.")
    public void telecomCall(
                        @RpcParameter(name = "uriString")
            final String uriString,
            @RpcParameter(name = "videoCall")
            @RpcOptional
            @RpcDefault("false")
            Boolean videoCall) throws Exception {

        Log.w("Function telecomCall is deprecated; please use a URI-specific call");

        Uri uri = Uri.parse(uriString);
        if (uri.getScheme().equals("content")) {
            telecomCallContentUri(uriString, videoCall);
        }
        else {
            telecomCallNumber(uriString, videoCall);
        }
    }

    // TODO: b/20917712 add support to pass arbitrary "Extras" object
    // for videoCall parameter
    @Rpc(description = "Calls a phone by resolving a Content-type URI.")
    public void telecomCallContentUri(
                        @RpcParameter(name = "uriString")
            final String uriString,
            @RpcParameter(name = "videoCall")
            @RpcOptional
            @RpcDefault("false")
            Boolean videoCall)
            throws Exception {
        Uri uri = Uri.parse(uriString);
        if (!uri.getScheme().equals("content")) {
            Log.e("Invalid URI!!");
            return;
        }

        String phoneNumberColumn = ContactsContract.PhoneLookup.NUMBER;
        String selectWhere = null;
        if ((FacadeManager.class.cast(mManager)).getSdkLevel() >= 5) {
            Class<?> contactsContract_Data_class =
                    Class.forName("android.provider.ContactsContract$Data");
            Field RAW_CONTACT_ID_field =
                    contactsContract_Data_class.getField("RAW_CONTACT_ID");
            selectWhere = RAW_CONTACT_ID_field.get(null).toString() + "="
                    + uri.getLastPathSegment();
            Field CONTENT_URI_field =
                    contactsContract_Data_class.getField("CONTENT_URI");
            uri = Uri.parse(CONTENT_URI_field.get(null).toString());
            Class<?> ContactsContract_CommonDataKinds_Phone_class =
                    Class.forName("android.provider.ContactsContract$CommonDataKinds$Phone");
            Field NUMBER_field =
                    ContactsContract_CommonDataKinds_Phone_class.getField("NUMBER");
            phoneNumberColumn = NUMBER_field.get(null).toString();
        }
        ContentResolver resolver = mService.getContentResolver();
        Cursor c = resolver.query(uri, new String[] {
                phoneNumberColumn
        },
                selectWhere, null, null);
        String number = "";
        if (c.moveToFirst()) {
            number = c.getString(c.getColumnIndexOrThrow(phoneNumberColumn));
        }
        c.close();
        telecomCallNumber(number, videoCall);
    }

    // TODO: b/20917712 add support to pass arbitrary "Extras" object
    // for videoCall parameter
    @Rpc(description = "Calls a phone number.")
    public void telecomCallNumber(
                        @RpcParameter(name = "number")
            final String number,
            @RpcParameter(name = "videoCall")
            @RpcOptional
            @RpcDefault("false")
            Boolean videoCall)
            throws Exception {
        telecomCallTelUri("tel:" + URLEncoder.encode(number, "ASCII"), videoCall);
    }

    // TODO: b/20917712 add support to pass arbitrary "Extras" object
    // for videoCall parameter
    @Rpc(description = "Calls a phone by Tel-URI.")
    public void telecomCallTelUri(
            @RpcParameter(name = "uriString")
    final String uriString,
            @RpcParameter(name = "videoCall")
            @RpcOptional
            @RpcDefault("false")
            Boolean videoCall) throws Exception {
        if (!uriString.startsWith("tel:")) {
            Log.w("Invalid tel URI" + uriString);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setDataAndType(Uri.parse(uriString).normalizeScheme(), null);

        if (videoCall) {
            Log.d("Placing a bi-directional video call");
            intent.putExtra(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
                    VideoProfile.STATE_BIDIRECTIONAL);
        }

        mAndroidFacade.startActivityIntent(intent, false);
    }

    @Rpc(description = "Calls an Emergency number.")
    public void telecomCallEmergencyNumber(
                        @RpcParameter(name = "number")
            final String number)
            throws Exception {
        String uriString = "tel:" + URLEncoder.encode(number, "ASCII");
        mAndroidFacade.startActivity(Intent.ACTION_CALL_PRIVILEGED, uriString,
                null, null, null, null, null);
    }

    @Rpc(description = "Dials a contact/phone number by URI.")
    public void telecomDial(
            @RpcParameter(name = "uri")
    final String uri)
            throws Exception {
        mAndroidFacade.startActivity(Intent.ACTION_DIAL, uri, null, null, null,
                null, null);
    }

    @Rpc(description = "Dials a phone number.")
    public void telecomDialNumber(@RpcParameter(name = "phone number")
    final String number)
            throws Exception, UnsupportedEncodingException {
        telecomDial("tel:" + URLEncoder.encode(number, "ASCII"));
    }
}
