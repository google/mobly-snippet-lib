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

import android.app.Service;
import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;

import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;

import java.util.List;

/**
 * Exposes SubscriptionManager functionality.
 */
public class SubscriptionManagerFacade extends RpcReceiver {

    private final Service mService;
    private final Context mContext;
    private final SubscriptionManager mSubscriptionManager;

    public SubscriptionManagerFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mContext = mService.getBaseContext();
        mSubscriptionManager = SubscriptionManager.from(mContext);
    }

    @Rpc(description = "Return the default subscription ID")
    public Integer subscriptionGetDefaultSubId() {
        return SubscriptionManager.getDefaultSubscriptionId();
    }

    @Rpc(description = "Return the default data subscription ID")
    public Integer subscriptionGetDefaultDataSubId() {
        return SubscriptionManager.getDefaultDataSubscriptionId();
    }

    @Rpc(description = "Set the default data subscription ID")
    public void subscriptionSetDefaultDataSubId(
            @RpcParameter(name = "subId")
            Integer subId) {
        mSubscriptionManager.setDefaultDataSubId(subId);
    }

    @Rpc(description = "Return the default voice subscription ID")
    public Integer subscriptionGetDefaultVoiceSubId() {
        return SubscriptionManager.getDefaultVoiceSubscriptionId();
    }

    @Rpc(description = "Set the default voice subscription ID")
    public void subscriptionSetDefaultVoiceSubId(
            @RpcParameter(name = "subId")
            Integer subId) {
        mSubscriptionManager.setDefaultVoiceSubId(subId);
    }

    @Rpc(description = "Return the default sms subscription ID")
    public Integer subscriptionGetDefaultSmsSubId() {
        return SubscriptionManager.getDefaultSmsSubscriptionId();
    }

    @Rpc(description = "Set the default sms subscription ID")
    public void subscriptionSetDefaultSmsSubId(
            @RpcParameter(name = "subId")
            Integer subId) {
        mSubscriptionManager.setDefaultSmsSubId(subId);
    }

    @Rpc(description = "Return a List of all Subscription Info Records")
    public List<SubscriptionInfo> subscriptionGetAllSubInfoList() {
        return mSubscriptionManager.getAllSubscriptionInfoList();
    }

    @Rpc(description = "Return a List of all Active Subscription Info Records")
    public List<SubscriptionInfo> subscriptionGetActiveSubInfoList() {
        return mSubscriptionManager.getActiveSubscriptionInfoList();
    }

    @Rpc(description = "Return the Subscription Info for a Particular Subscription ID")
    public SubscriptionInfo subscriptionGetSubInfoForSubscriber(
            @RpcParameter(name = "subId")
            Integer subId) {
        return mSubscriptionManager.getActiveSubscriptionInfo(subId);
    }

    @Rpc(description = "Set Data Roaming Enabled or Disabled for a particular Subscription ID")
    public Integer subscriptionSetDataRoaming(
            @RpcParameter(name = "roaming") Integer roaming,
            @RpcParameter(name = "subId") Integer subId) {
        if (roaming != SubscriptionManager.DATA_ROAMING_DISABLE) {
            return mSubscriptionManager.setDataRoaming(
                    SubscriptionManager.DATA_ROAMING_ENABLE, subId);
        } else {
            return mSubscriptionManager.setDataRoaming(
                    SubscriptionManager.DATA_ROAMING_DISABLE, subId);
        }
    }

    @Override
    public void shutdown() {

    }
}
