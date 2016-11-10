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

package com.googlecode.android_scripting.facade;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.collect.Maps;
import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.bluetooth.BluetoothA2dpFacade;
import com.googlecode.android_scripting.facade.bluetooth.BluetoothA2dpSinkFacade;
import com.googlecode.android_scripting.facade.bluetooth.BluetoothAvrcpFacade;
import com.googlecode.android_scripting.facade.bluetooth.BluetoothConnectionFacade;
import com.googlecode.android_scripting.facade.bluetooth.BluetoothFacade;
import com.googlecode.android_scripting.facade.bluetooth.BluetoothHfpClientFacade;
import com.googlecode.android_scripting.facade.bluetooth.BluetoothHidFacade;
import com.googlecode.android_scripting.facade.bluetooth.BluetoothHspFacade;
import com.googlecode.android_scripting.facade.bluetooth.BluetoothLeAdvertiseFacade;
import com.googlecode.android_scripting.facade.bluetooth.BluetoothLeScanFacade;
import com.googlecode.android_scripting.facade.bluetooth.BluetoothMapClientFacade;
import com.googlecode.android_scripting.facade.bluetooth.BluetoothMapFacade;
import com.googlecode.android_scripting.facade.bluetooth.BluetoothMediaFacade;
import com.googlecode.android_scripting.facade.bluetooth.BluetoothPbapClientFacade;
import com.googlecode.android_scripting.facade.bluetooth.BluetoothPanFacade;
import com.googlecode.android_scripting.facade.bluetooth.BluetoothRfcommFacade;
import com.googlecode.android_scripting.facade.bluetooth.GattClientFacade;
import com.googlecode.android_scripting.facade.bluetooth.GattServerFacade;
import com.googlecode.android_scripting.facade.media.AudioManagerFacade;
import com.googlecode.android_scripting.facade.media.MediaPlayerFacade;
import com.googlecode.android_scripting.facade.media.MediaRecorderFacade;
import com.googlecode.android_scripting.facade.media.MediaScannerFacade;
import com.googlecode.android_scripting.facade.media.MediaSessionFacade;
import com.googlecode.android_scripting.facade.net.nsd.NsdManagerFacade;
import com.googlecode.android_scripting.facade.telephony.CarrierConfigFacade;
import com.googlecode.android_scripting.facade.telephony.ImsManagerFacade;
import com.googlecode.android_scripting.facade.telephony.SmsFacade;
import com.googlecode.android_scripting.facade.telephony.SubscriptionManagerFacade;
import com.googlecode.android_scripting.facade.telephony.TelecomCallFacade;
import com.googlecode.android_scripting.facade.telephony.TelecomManagerFacade;
import com.googlecode.android_scripting.facade.telephony.TelephonyManagerFacade;
import com.googlecode.android_scripting.facade.ui.UiFacade;
import com.googlecode.android_scripting.facade.wifi.HttpFacade;
import com.googlecode.android_scripting.facade.wifi.WifiManagerFacade;
import com.googlecode.android_scripting.facade.wifi.WifiAwareManagerFacade;
import com.googlecode.android_scripting.facade.wifi.WifiP2pManagerFacade;
import com.googlecode.android_scripting.facade.wifi.WifiRttManagerFacade;
import com.googlecode.android_scripting.facade.wifi.WifiScannerFacade;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.MethodDescriptor;
import com.googlecode.android_scripting.rpc.RpcDeprecated;
import com.googlecode.android_scripting.rpc.RpcMinSdk;
import com.googlecode.android_scripting.rpc.RpcStartEvent;
import com.googlecode.android_scripting.rpc.RpcStopEvent;
import com.googlecode.android_scripting.webcam.WebCamFacade;

/**
 * Encapsulates the list of supported facades and their construction.
 */
public class FacadeConfiguration {
    private final static Set<Class<? extends RpcReceiver>> sFacadeClassList;
    private final static SortedMap<String, MethodDescriptor> sRpcs =
            new TreeMap<String, MethodDescriptor>();

    private static int sSdkLevel;

    static {
        sSdkLevel = android.os.Build.VERSION.SDK_INT;

        sFacadeClassList = new HashSet<Class<? extends RpcReceiver>>();
        sFacadeClassList.add(ActivityResultFacade.class);
        sFacadeClassList.add(AndroidFacade.class);
        sFacadeClassList.add(ApplicationManagerFacade.class);
        sFacadeClassList.add(AudioManagerFacade.class);
        sFacadeClassList.add(BatteryManagerFacade.class);
        sFacadeClassList.add(CallLogFacade.class);
        sFacadeClassList.add(CameraFacade.class);
        sFacadeClassList.add(CommonIntentsFacade.class);
        sFacadeClassList.add(ContactsFacade.class);
        sFacadeClassList.add(EventFacade.class);
        sFacadeClassList.add(ImsManagerFacade.class);
        sFacadeClassList.add(LocationFacade.class);
        sFacadeClassList.add(TelephonyManagerFacade.class);
        sFacadeClassList.add(PreferencesFacade.class);
        sFacadeClassList.add(MediaPlayerFacade.class);
        sFacadeClassList.add(MediaRecorderFacade.class);
        sFacadeClassList.add(MediaScannerFacade.class);
        sFacadeClassList.add(MediaSessionFacade.class);
        sFacadeClassList.add(SensorManagerFacade.class);
        sFacadeClassList.add(SettingsFacade.class);
        sFacadeClassList.add(SmsFacade.class);
        sFacadeClassList.add(SpeechRecognitionFacade.class);
        sFacadeClassList.add(ToneGeneratorFacade.class);
        sFacadeClassList.add(WakeLockFacade.class);
        sFacadeClassList.add(HttpFacade.class);
        sFacadeClassList.add(WifiManagerFacade.class);
        sFacadeClassList.add(UiFacade.class);
        sFacadeClassList.add(TextToSpeechFacade.class);
        sFacadeClassList.add(BluetoothFacade.class);
        sFacadeClassList.add(BluetoothA2dpFacade.class);
        sFacadeClassList.add(BluetoothAvrcpFacade.class);
        sFacadeClassList.add(BluetoothConnectionFacade.class);
        sFacadeClassList.add(BluetoothHspFacade.class);
        sFacadeClassList.add(BluetoothHidFacade.class);
        sFacadeClassList.add(BluetoothMapFacade.class);
        sFacadeClassList.add(BluetoothPanFacade.class);
        sFacadeClassList.add(BluetoothMediaFacade.class);
        sFacadeClassList.add(BluetoothRfcommFacade.class);
        sFacadeClassList.add(WebCamFacade.class);
        sFacadeClassList.add(WifiP2pManagerFacade.class);
        sFacadeClassList.add(BluetoothLeScanFacade.class);
        sFacadeClassList.add(BluetoothLeAdvertiseFacade.class);
        sFacadeClassList.add(GattClientFacade.class);
        sFacadeClassList.add(GattServerFacade.class);
        sFacadeClassList.add(ConnectivityManagerFacade.class);
        sFacadeClassList.add(DisplayFacade.class);
        sFacadeClassList.add(TelecomManagerFacade.class);
        sFacadeClassList.add(WifiRttManagerFacade.class);
        sFacadeClassList.add(WifiScannerFacade.class);
        sFacadeClassList.add(SubscriptionManagerFacade.class);
        sFacadeClassList.add(TelecomCallFacade.class);
        sFacadeClassList.add(CarrierConfigFacade.class);
        sFacadeClassList.add(NfcManagerFacade.class);
        sFacadeClassList.add(VpnFacade.class);

        /* Compatibility reset to >= Marshmallow */
        if (sSdkLevel >= 23) {
            // add new facades here
            sFacadeClassList.add(WifiAwareManagerFacade.class);
            sFacadeClassList.add(BluetoothHfpClientFacade.class);
            sFacadeClassList.add(BluetoothA2dpSinkFacade.class);
            sFacadeClassList.add(BluetoothPbapClientFacade.class);
            sFacadeClassList.add(NsdManagerFacade.class);
            sFacadeClassList.add(BluetoothMapClientFacade.class);
        }

        for (Class<? extends RpcReceiver> recieverClass : sFacadeClassList) {
            for (MethodDescriptor rpcMethod : MethodDescriptor.collectFrom(recieverClass)) {
                sRpcs.put(rpcMethod.getName(), rpcMethod);
            }
        }
    }

    private FacadeConfiguration() {
        // Utility class.
    }

    public static int getSdkLevel() {
        return sSdkLevel;
    }

    /** Returns a list of {@link MethodDescriptor} objects for all facades. */
    public static List<MethodDescriptor> collectMethodDescriptors() {
        return new ArrayList<MethodDescriptor>(sRpcs.values());
    }

    /**
     * Returns a list of not deprecated {@link MethodDescriptor} objects for facades supported by
     * the current SDK version.
     */
    public static List<MethodDescriptor> collectSupportedMethodDescriptors() {
        List<MethodDescriptor> list = new ArrayList<MethodDescriptor>();
        for (MethodDescriptor descriptor : sRpcs.values()) {
            Method method = descriptor.getMethod();
            if (method.isAnnotationPresent(RpcDeprecated.class)) {
                continue;
            } else if (method.isAnnotationPresent(RpcMinSdk.class)) {
                int requiredSdkLevel = method.getAnnotation(RpcMinSdk.class).value();
                if (sSdkLevel < requiredSdkLevel) {
                    continue;
                }
            }
            list.add(descriptor);
        }
        return list;
    }

    public static Map<String, MethodDescriptor> collectStartEventMethodDescriptors() {
        Map<String, MethodDescriptor> map = Maps.newHashMap();
        for (MethodDescriptor descriptor : sRpcs.values()) {
            Method method = descriptor.getMethod();
            if (method.isAnnotationPresent(RpcStartEvent.class)) {
                String eventName = method.getAnnotation(RpcStartEvent.class).value();
                if (map.containsKey(eventName)) {
                    Log.d("Duplicate eventName " + eventName);
                    throw new RuntimeException("Duplicate start event method descriptor found.");
                }
                map.put(eventName, descriptor);
            }
        }
        return map;
    }

    public static Map<String, MethodDescriptor> collectStopEventMethodDescriptors() {
        Map<String, MethodDescriptor> map = Maps.newHashMap();
        for (MethodDescriptor descriptor : sRpcs.values()) {
            Method method = descriptor.getMethod();
            if (method.isAnnotationPresent(RpcStopEvent.class)) {
                String eventName = method.getAnnotation(RpcStopEvent.class).value();
                if (map.containsKey(eventName)) {
                    throw new RuntimeException("Duplicate stop event method descriptor found.");
                }
                map.put(eventName, descriptor);
            }
        }
        return map;
    }

    /** Returns a method by name. */
    public static MethodDescriptor getMethodDescriptor(String name) {
        return sRpcs.get(name);
    }

    public static Collection<Class<? extends RpcReceiver>> getFacadeClasses() {
        return sFacadeClassList;
    }
}
