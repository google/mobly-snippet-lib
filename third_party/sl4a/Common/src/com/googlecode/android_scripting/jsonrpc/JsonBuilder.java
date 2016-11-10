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

package com.googlecode.android_scripting.jsonrpc;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.binary.Base64Codec;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.internal.net.LegacyVpnInfo;
import com.googlecode.android_scripting.ConvertUtils;
import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.event.Event;
//FIXME: Refactor classes, constants and conversions out of here
import com.googlecode.android_scripting.facade.telephony.InCallServiceImpl;
import com.googlecode.android_scripting.facade.telephony.TelephonyConstants;
import com.googlecode.android_scripting.facade.telephony.TelephonyUtils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.AdvertiseSettings;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Point;
import android.location.Address;
import android.location.Location;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.RttManager.RttCapabilities;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiActivityEnergyInfo;
import android.net.wifi.WifiChannel;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiScanner.ScanData;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.VideoProfile;
import android.telecom.VideoProfile.CameraCapabilities;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.ModemActivityInfo;
import android.telephony.NeighboringCellInfo;
import android.telephony.SignalStrength;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.VoLteServiceState;
import android.telephony.gsm.GsmCellLocation;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.SparseArray;

public class JsonBuilder {

    @SuppressWarnings("unchecked")
    public static Object build(Object data) throws JSONException {
        if (data == null) {
            return JSONObject.NULL;
        }
        if (data instanceof Integer) {
            return data;
        }
        if (data instanceof Float) {
            return data;
        }
        if (data instanceof Double) {
            return data;
        }
        if (data instanceof Long) {
            return data;
        }
        if (data instanceof String) {
            return data;
        }
        if (data instanceof Boolean) {
            return data;
        }
        if (data instanceof JsonSerializable) {
            return ((JsonSerializable) data).toJSON();
        }
        if (data instanceof JSONObject) {
            return data;
        }
        if (data instanceof JSONArray) {
            return data;
        }
        if (data instanceof Set<?>) {
            List<Object> items = new ArrayList<Object>((Set<?>) data);
            return buildJsonList(items);
        }
        if (data instanceof Collection<?>) {
            List<Object> items = new ArrayList<Object>((Collection<?>) data);
            return buildJsonList(items);
        }
        if (data instanceof List<?>) {
            return buildJsonList((List<?>) data);
        }
        if (data instanceof Address) {
            return buildJsonAddress((Address) data);
        }
        if (data instanceof CallAudioState) {
            return buildJsonAudioState((CallAudioState) data);
        }
        if (data instanceof Location) {
            return buildJsonLocation((Location) data);
        }
        if (data instanceof Bundle) {
            return buildJsonBundle((Bundle) data);
        }
        if (data instanceof Intent) {
            return buildJsonIntent((Intent) data);
        }
        if (data instanceof Event) {
            return buildJsonEvent((Event) data);
        }
        if (data instanceof Map<?, ?>) {
            // TODO(damonkohler): I would like to make this a checked cast if
            // possible.
            return buildJsonMap((Map<String, ?>) data);
        }
        if (data instanceof ParcelUuid) {
            return data.toString();
        }
        if (data instanceof ScanResult) {
            return buildJsonScanResult((ScanResult) data);
        }
        if (data instanceof ScanData) {
            return buildJsonScanData((ScanData) data);
        }
        if (data instanceof android.bluetooth.le.ScanResult) {
            return buildJsonBleScanResult((android.bluetooth.le.ScanResult) data);
        }
        if (data instanceof AdvertiseSettings) {
            return buildJsonBleAdvertiseSettings((AdvertiseSettings) data);
        }
        if (data instanceof BluetoothGattService) {
            return buildJsonBluetoothGattService((BluetoothGattService) data);
        }
        if (data instanceof BluetoothGattCharacteristic) {
            return buildJsonBluetoothGattCharacteristic((BluetoothGattCharacteristic) data);
        }
        if (data instanceof BluetoothGattDescriptor) {
            return buildJsonBluetoothGattDescriptor((BluetoothGattDescriptor) data);
        }
        if (data instanceof BluetoothDevice) {
            return buildJsonBluetoothDevice((BluetoothDevice) data);
        }
        if (data instanceof CellLocation) {
            return buildJsonCellLocation((CellLocation) data);
        }
        if (data instanceof WifiInfo) {
            return buildJsonWifiInfo((WifiInfo) data);
        }
        if (data instanceof NeighboringCellInfo) {
            return buildNeighboringCellInfo((NeighboringCellInfo) data);
        }
        if (data instanceof Network) {
            return buildNetwork((Network) data);
        }
        if (data instanceof NetworkInfo) {
            return buildNetworkInfo((NetworkInfo) data);
        }
        if (data instanceof HttpURLConnection) {
            return buildHttpURLConnection((HttpURLConnection) data);
        }
        if (data instanceof InetSocketAddress) {
            return buildInetSocketAddress((InetSocketAddress) data);
        }
        if (data instanceof InetAddress) {
            return buildInetAddress((InetAddress) data);
        }
        if (data instanceof URL) {
            return buildURL((URL) data);
        }
        if (data instanceof Point) {
            return buildPoint((Point) data);
        }
        if (data instanceof SmsMessage) {
            return buildSmsMessage((SmsMessage) data);
        }
        if (data instanceof PhoneAccount) {
            return buildPhoneAccount((PhoneAccount) data);
        }
        if (data instanceof PhoneAccountHandle) {
            return buildPhoneAccountHandle((PhoneAccountHandle) data);
        }
        if (data instanceof SubscriptionInfo) {
            return buildSubscriptionInfoRecord((SubscriptionInfo) data);
        }
        if (data instanceof DhcpInfo) {
            return buildDhcpInfo((DhcpInfo) data);
        }
        if (data instanceof DisplayMetrics) {
            return buildDisplayMetrics((DisplayMetrics) data);
        }
        if (data instanceof RttCapabilities) {
            return buildRttCapabilities((RttCapabilities) data);
        }
        if (data instanceof WifiActivityEnergyInfo) {
            return buildWifiActivityEnergyInfo((WifiActivityEnergyInfo) data);
        }
        if (data instanceof WifiChannel) {
            return buildWifiChannel((WifiChannel) data);
        }
        if (data instanceof WifiConfiguration) {
            return buildWifiConfiguration((WifiConfiguration) data);
        }
        if (data instanceof WifiP2pDevice) {
            return buildWifiP2pDevice((WifiP2pDevice) data);
        }
        if (data instanceof WifiP2pInfo) {
            return buildWifiP2pInfo((WifiP2pInfo) data);
        }
        if (data instanceof WifiP2pGroup) {
            return buildWifiP2pGroup((WifiP2pGroup) data);
        }
        if (data instanceof byte[]) {
            JSONArray result = new JSONArray();
            for (byte b : (byte[]) data) {
                result.put(b & 0xFF);
            }
            return result;
        }
        if (data instanceof Object[]) {
            return buildJSONArray((Object[]) data);
        }
        if (data instanceof CellInfoLte) {
            return buildCellInfoLte((CellInfoLte) data);
        }
        if (data instanceof CellInfoWcdma) {
            return buildCellInfoWcdma((CellInfoWcdma) data);
        }
        if (data instanceof CellInfoGsm) {
            return buildCellInfoGsm((CellInfoGsm) data);
        }
        if (data instanceof CellInfoCdma) {
            return buildCellInfoCdma((CellInfoCdma) data);
        }
        if (data instanceof Call) {
            return buildCall((Call) data);
        }
        if (data instanceof Call.Details) {
            return buildCallDetails((Call.Details) data);
        }
        if (data instanceof InCallServiceImpl.CallEvent<?>) {
            return buildCallEvent((InCallServiceImpl.CallEvent<?>) data);
        }
        if (data instanceof VideoProfile) {
            return buildVideoProfile((VideoProfile) data);
        }
        if (data instanceof CameraCapabilities) {
            return buildCameraCapabilities((CameraCapabilities) data);
        }
        if (data instanceof VoLteServiceState) {
            return buildVoLteServiceStateEvent((VoLteServiceState) data);
        }
        if (data instanceof LegacyVpnInfo) {
            return buildLegacyVpnInfo((LegacyVpnInfo) data);
        }
        if (data instanceof ModemActivityInfo) {
            return buildModemActivityInfo((ModemActivityInfo) data);
        }
        if (data instanceof SignalStrength) {
            return buildSignalStrength((SignalStrength) data);
        }

        return data.toString();
        // throw new JSONException("Failed to build JSON result. " +
        // data.getClass().getName());
    }

    private static JSONObject buildJsonAudioState(CallAudioState data)
            throws JSONException {
        JSONObject state = new JSONObject();
        state.put("isMuted", data.isMuted());
        state.put("AudioRoute", InCallServiceImpl.getAudioRouteString(data.getRoute()));
        return state;
    }

    private static Object buildDisplayMetrics(DisplayMetrics data)
            throws JSONException {
        JSONObject dm = new JSONObject();
        dm.put("widthPixels", data.widthPixels);
        dm.put("heightPixels", data.heightPixels);
        dm.put("noncompatHeightPixels", data.noncompatHeightPixels);
        dm.put("noncompatWidthPixels", data.noncompatWidthPixels);
        return dm;
    }

    private static Object buildInetAddress(InetAddress data) {
        JSONArray address = new JSONArray();
        address.put(data.getHostName());
        address.put(data.getHostAddress());
        return address;
    }

    private static Object buildInetSocketAddress(InetSocketAddress data) {
        JSONArray address = new JSONArray();
        address.put(data.getHostName());
        address.put(data.getPort());
        return address;
    }

    private static JSONObject buildJsonAddress(Address address)
            throws JSONException {
        JSONObject result = new JSONObject();
        result.put("admin_area", address.getAdminArea());
        result.put("country_code", address.getCountryCode());
        result.put("country_name", address.getCountryName());
        result.put("feature_name", address.getFeatureName());
        result.put("phone", address.getPhone());
        result.put("locality", address.getLocality());
        result.put("postal_code", address.getPostalCode());
        result.put("sub_admin_area", address.getSubAdminArea());
        result.put("thoroughfare", address.getThoroughfare());
        result.put("url", address.getUrl());
        return result;
    }

    private static JSONArray buildJSONArray(Object[] data) throws JSONException {
        JSONArray result = new JSONArray();
        for (Object o : data) {
            result.put(build(o));
        }
        return result;
    }

    private static JSONObject buildJsonBleAdvertiseSettings(
            AdvertiseSettings advertiseSettings) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("mode", advertiseSettings.getMode());
        result.put("txPowerLevel", advertiseSettings.getTxPowerLevel());
        result.put("isConnectable", advertiseSettings.isConnectable());
        return result;
    }

    private static JSONObject buildJsonBleScanResult(
            android.bluetooth.le.ScanResult scanResult) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("rssi", scanResult.getRssi());
        result.put("timestampNanos", scanResult.getTimestampNanos());
        result.put("deviceName", scanResult.getScanRecord().getDeviceName());
        result.put("txPowerLevel", scanResult.getScanRecord().getTxPowerLevel());
        result.put("advertiseFlags", scanResult.getScanRecord()
                .getAdvertiseFlags());
        ArrayList<String> manufacturerDataList = new ArrayList<String>();
        ArrayList<Integer> idList = new ArrayList<Integer>();
        if (scanResult.getScanRecord().getManufacturerSpecificData() != null) {
            SparseArray<byte[]> manufacturerSpecificData = scanResult
                    .getScanRecord().getManufacturerSpecificData();
            for (int i = 0; i < manufacturerSpecificData.size(); i++) {
                manufacturerDataList.add(ConvertUtils
                        .convertByteArrayToString(manufacturerSpecificData
                                .valueAt(i)));
                idList.add(manufacturerSpecificData.keyAt(i));
            }
        }
        result.put("manufacturerSpecificDataList", manufacturerDataList);
        result.put("manufacturerIdList", idList);
        ArrayList<String> serviceUuidList = new ArrayList<String>();
        ArrayList<String> serviceDataList = new ArrayList<String>();
        if (scanResult.getScanRecord().getServiceData() != null) {
            Map<ParcelUuid, byte[]> serviceDataMap = scanResult.getScanRecord()
                    .getServiceData();
            for (ParcelUuid serviceUuid : serviceDataMap.keySet()) {
                serviceUuidList.add(serviceUuid.toString());
                serviceDataList.add(ConvertUtils
                        .convertByteArrayToString(serviceDataMap
                                .get(serviceUuid)));
            }
        }
        result.put("serviceUuidList", serviceUuidList);
        result.put("serviceDataList", serviceDataList);
        List<ParcelUuid> serviceUuids = scanResult.getScanRecord()
                .getServiceUuids();
        String serviceUuidsString = "";
        if (serviceUuids != null && serviceUuids.size() > 0) {
            for (ParcelUuid uuid : serviceUuids) {
                serviceUuidsString = serviceUuidsString + "," + uuid;
            }
        }
        result.put("serviceUuids", serviceUuidsString);
        result.put("scanRecord",
                build(ConvertUtils.convertByteArrayToString(scanResult
                        .getScanRecord().getBytes())));
        result.put("deviceInfo", build(scanResult.getDevice()));
        return result;
    }

    private static JSONObject buildJsonBluetoothDevice(BluetoothDevice data)
            throws JSONException {
        JSONObject deviceInfo = new JSONObject();
        deviceInfo.put("address", data.getAddress());
        deviceInfo.put("state", data.getBondState());
        deviceInfo.put("name", data.getName());
        deviceInfo.put("type", data.getType());
        return deviceInfo;
    }

    private static Object buildJsonBluetoothGattCharacteristic(
            BluetoothGattCharacteristic data) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("instanceId", data.getInstanceId());
        result.put("permissions", data.getPermissions());
        result.put("properties", data.getProperties());
        result.put("writeType", data.getWriteType());
        result.put("descriptorsList", build(data.getDescriptors()));
        result.put("uuid", data.getUuid().toString());
        result.put("value", build(data.getValue()));

        return result;
    }

    private static Object buildJsonBluetoothGattDescriptor(
            BluetoothGattDescriptor data) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("instanceId", data.getInstanceId());
        result.put("permissions", data.getPermissions());
        result.put("characteristic", data.getCharacteristic());
        result.put("uuid", data.getUuid().toString());
        result.put("value", build(data.getValue()));
        return result;
    }

    private static Object buildJsonBluetoothGattService(
            BluetoothGattService data) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("instanceId", data.getInstanceId());
        result.put("type", data.getType());
        result.put("gattCharacteristicList", build(data.getCharacteristics()));
        result.put("includedServices", build(data.getIncludedServices()));
        result.put("uuid", data.getUuid().toString());
        return result;
    }

    private static JSONObject buildJsonBundle(Bundle bundle)
            throws JSONException {
        JSONObject result = new JSONObject();
        for (String key : bundle.keySet()) {
            result.put(key, build(bundle.get(key)));
        }
        return result;
    }

    private static JSONObject buildJsonCellLocation(CellLocation cellLocation)
            throws JSONException {
        JSONObject result = new JSONObject();
        if (cellLocation instanceof GsmCellLocation) {
            GsmCellLocation location = (GsmCellLocation) cellLocation;
            result.put("lac", location.getLac());
            result.put("cid", location.getCid());
        }
        // TODO(damonkohler): Add support for CdmaCellLocation. Not supported
        // until API level 5.
        return result;
    }

    private static JSONObject buildDhcpInfo(DhcpInfo data) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("ipAddress", data.ipAddress);
        result.put("dns1", data.dns1);
        result.put("dns2", data.dns2);
        result.put("gateway", data.gateway);
        result.put("serverAddress", data.serverAddress);
        result.put("leaseDuration", data.leaseDuration);
        return result;
    }

    private static JSONObject buildJsonEvent(Event event) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("name", event.getName());
        result.put("data", build(event.getData()));
        result.put("time", event.getCreationTime());
        return result;
    }

    private static JSONObject buildJsonIntent(Intent data) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("data", data.getDataString());
        result.put("type", data.getType());
        result.put("extras", build(data.getExtras()));
        result.put("categories", build(data.getCategories()));
        result.put("action", data.getAction());
        ComponentName component = data.getComponent();
        if (component != null) {
            result.put("packagename", component.getPackageName());
            result.put("classname", component.getClassName());
        }
        result.put("flags", data.getFlags());
        return result;
    }

    private static <T> JSONArray buildJsonList(final List<T> list)
            throws JSONException {
        JSONArray result = new JSONArray();
        for (T item : list) {
            result.put(build(item));
        }
        return result;
    }

    private static JSONObject buildJsonLocation(Location location)
            throws JSONException {
        JSONObject result = new JSONObject();
        result.put("altitude", location.getAltitude());
        result.put("latitude", location.getLatitude());
        result.put("longitude", location.getLongitude());
        result.put("time", location.getTime());
        result.put("accuracy", location.getAccuracy());
        result.put("speed", location.getSpeed());
        result.put("provider", location.getProvider());
        result.put("bearing", location.getBearing());
        return result;
    }

    private static JSONObject buildJsonMap(Map<String, ?> map)
            throws JSONException {
        JSONObject result = new JSONObject();
        for (Entry<String, ?> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                key = "";
            }
            result.put(key, build(entry.getValue()));
        }
        return result;
    }

    private static JSONObject buildJsonScanResult(ScanResult scanResult)
            throws JSONException {
        JSONObject result = new JSONObject();
        result.put("BSSID", scanResult.BSSID);
        result.put("SSID", scanResult.SSID);
        result.put("frequency", scanResult.frequency);
        result.put("level", scanResult.level);
        result.put("capabilities", scanResult.capabilities);
        result.put("timestamp", scanResult.timestamp);
        result.put("blackListTimestamp", scanResult.blackListTimestamp);
        result.put("centerFreq0", scanResult.centerFreq0);
        result.put("centerFreq1", scanResult.centerFreq1);
        result.put("channelWidth", scanResult.channelWidth);
        result.put("distanceCm", scanResult.distanceCm);
        result.put("distanceSdCm", scanResult.distanceSdCm);
        result.put("is80211McRTTResponder", scanResult.is80211mcResponder());
        result.put("numConnection", scanResult.numConnection);
        result.put("passpointNetwork", scanResult.isPasspointNetwork());
        result.put("numIpConfigFailures", scanResult.numIpConfigFailures);
        result.put("numUsage", scanResult.numUsage);
        result.put("seen", scanResult.seen);
        result.put("untrusted", scanResult.untrusted);
        result.put("operatorFriendlyName", scanResult.operatorFriendlyName);
        result.put("venueName", scanResult.venueName);
        if (scanResult.informationElements != null) {
            JSONArray infoEles = new JSONArray();
            for (ScanResult.InformationElement ie : scanResult.informationElements) {
                JSONObject infoEle = new JSONObject();
                infoEle.put("id", ie.id);
                infoEle.put("bytes", Base64Codec.encodeBase64(ie.bytes).toString());
                infoEles.put(infoEle);
            }
            result.put("InfomationElements", infoEles);
        } else {
            result.put("InfomationElements", null);
        }
        return result;
    }

    private static JSONObject buildJsonScanData(ScanData scanData)
            throws JSONException {
        JSONObject result = new JSONObject();
        result.put("Id", scanData.getId());
        result.put("Flags", scanData.getFlags());
        JSONArray scanResults = new JSONArray();
        for (ScanResult sr : scanData.getResults()) {
            scanResults.put(buildJsonScanResult(sr));
        }
        result.put("ScanResults", scanResults);
        return result;
    }

    private static JSONObject buildJsonWifiInfo(WifiInfo data)
            throws JSONException {
        JSONObject result = new JSONObject();
        result.put("hidden_ssid", data.getHiddenSSID());
        result.put("ip_address", data.getIpAddress());
        result.put("link_speed", data.getLinkSpeed());
        result.put("network_id", data.getNetworkId());
        result.put("rssi", data.getRssi());
        result.put("BSSID", data.getBSSID());
        result.put("mac_address", data.getMacAddress());
        // Trim the double quotes if exist
        String ssid = data.getSSID();
        if (ssid.charAt(0) == '"'
                && ssid.charAt(ssid.length() - 1) == '"') {
            result.put("SSID", ssid.substring(1, ssid.length() - 1));
        } else {
            result.put("SSID", ssid);
        }
        String supplicantState = "";
        switch (data.getSupplicantState()) {
            case ASSOCIATED:
                supplicantState = "associated";
                break;
            case ASSOCIATING:
                supplicantState = "associating";
                break;
            case COMPLETED:
                supplicantState = "completed";
                break;
            case DISCONNECTED:
                supplicantState = "disconnected";
                break;
            case DORMANT:
                supplicantState = "dormant";
                break;
            case FOUR_WAY_HANDSHAKE:
                supplicantState = "four_way_handshake";
                break;
            case GROUP_HANDSHAKE:
                supplicantState = "group_handshake";
                break;
            case INACTIVE:
                supplicantState = "inactive";
                break;
            case INVALID:
                supplicantState = "invalid";
                break;
            case SCANNING:
                supplicantState = "scanning";
                break;
            case UNINITIALIZED:
                supplicantState = "uninitialized";
                break;
            default:
                supplicantState = null;
        }
        result.put("supplicant_state", build(supplicantState));
        result.put("is_5ghz", data.is5GHz());
        result.put("is_24ghz", data.is24GHz());
        return result;
    }

    private static JSONObject buildNeighboringCellInfo(NeighboringCellInfo data)
            throws JSONException {
        JSONObject result = new JSONObject();
        result.put("cid", data.getCid());
        result.put("rssi", data.getRssi());
        result.put("lac", data.getLac());
        result.put("psc", data.getPsc());
        String networkType = TelephonyUtils.getNetworkTypeString(data.getNetworkType());
        result.put("network_type", build(networkType));
        return result;
    }

    private static JSONObject buildCellInfoLte(CellInfoLte data)
            throws JSONException {
        JSONObject result = new JSONObject();
        result.put("rat", "lte");
        result.put("registered", data.isRegistered());
        CellIdentityLte cellidentity = ((CellInfoLte) data).getCellIdentity();
        CellSignalStrengthLte signalstrength = ((CellInfoLte) data).getCellSignalStrength();
        result.put("mcc", cellidentity.getMcc());
        result.put("mnc", cellidentity.getMnc());
        result.put("cid", cellidentity.getCi());
        result.put("pcid", cellidentity.getPci());
        result.put("tac", cellidentity.getTac());
        result.put("rsrp", signalstrength.getDbm());
        result.put("asulevel", signalstrength.getAsuLevel());
        result.put("timing_advance", signalstrength.getTimingAdvance());
        return result;
    }

    private static JSONObject buildCellInfoGsm(CellInfoGsm data)
            throws JSONException {
        JSONObject result = new JSONObject();
        result.put("rat", "gsm");
        result.put("registered", data.isRegistered());
        CellIdentityGsm cellidentity = ((CellInfoGsm) data).getCellIdentity();
        CellSignalStrengthGsm signalstrength = ((CellInfoGsm) data).getCellSignalStrength();
        result.put("mcc", cellidentity.getMcc());
        result.put("mnc", cellidentity.getMnc());
        result.put("cid", cellidentity.getCid());
        result.put("lac", cellidentity.getLac());
        result.put("signal_strength", signalstrength.getDbm());
        result.put("asulevel", signalstrength.getAsuLevel());
        return result;
    }

    private static JSONObject buildCellInfoWcdma(CellInfoWcdma data)
            throws JSONException {
        JSONObject result = new JSONObject();
        result.put("rat", "wcdma");
        result.put("registered", data.isRegistered());
        CellIdentityWcdma cellidentity = ((CellInfoWcdma) data).getCellIdentity();
        CellSignalStrengthWcdma signalstrength = ((CellInfoWcdma) data).getCellSignalStrength();
        result.put("mcc", cellidentity.getMcc());
        result.put("mnc", cellidentity.getMnc());
        result.put("cid", cellidentity.getCid());
        result.put("lac", cellidentity.getLac());
        result.put("psc", cellidentity.getPsc());
        result.put("signal_strength", signalstrength.getDbm());
        result.put("asulevel", signalstrength.getAsuLevel());
        return result;
    }

    private static JSONObject buildCellInfoCdma(CellInfoCdma data)
            throws JSONException {
        JSONObject result = new JSONObject();
        result.put("rat", "cdma");
        result.put("registered", data.isRegistered());
        CellIdentityCdma cellidentity = ((CellInfoCdma) data).getCellIdentity();
        CellSignalStrengthCdma signalstrength = ((CellInfoCdma) data).getCellSignalStrength();
        result.put("network_id", cellidentity.getNetworkId());
        result.put("system_id", cellidentity.getSystemId());
        result.put("basestation_id", cellidentity.getBasestationId());
        result.put("longitude", cellidentity.getLongitude());
        result.put("latitude", cellidentity.getLatitude());
        result.put("cdma_dbm", signalstrength.getCdmaDbm());
        result.put("cdma_ecio", signalstrength.getCdmaEcio());
        result.put("evdo_dbm", signalstrength.getEvdoDbm());
        result.put("evdo_ecio", signalstrength.getEvdoEcio());
        result.put("evdo_snr", signalstrength.getEvdoSnr());
        return result;
    }

    private static Object buildHttpURLConnection(HttpURLConnection data)
            throws JSONException {
        JSONObject con = new JSONObject();
        try {
            con.put("ResponseCode", data.getResponseCode());
            con.put("ResponseMessage", data.getResponseMessage());
        } catch (IOException e) {
            e.printStackTrace();
            return con;
        }
        con.put("ContentLength", data.getContentLength());
        con.put("ContentEncoding", data.getContentEncoding());
        con.put("ContentType", data.getContentType());
        con.put("Date", data.getDate());
        con.put("ReadTimeout", data.getReadTimeout());
        con.put("HeaderFields", buildJsonMap(data.getHeaderFields()));
        con.put("URL", buildURL(data.getURL()));
        return con;
    }

    private static Object buildNetwork(Network data) throws JSONException {
        JSONObject nw = new JSONObject();
        nw.put("netId", data.netId);
        return nw;
    }

    private static Object buildNetworkInfo(NetworkInfo data)
            throws JSONException {
        JSONObject info = new JSONObject();
        info.put("isAvailable", data.isAvailable());
        info.put("isConnected", data.isConnected());
        info.put("isFailover", data.isFailover());
        info.put("isRoaming", data.isRoaming());
        info.put("ExtraInfo", data.getExtraInfo());
        info.put("FailedReason", data.getReason());
        info.put("TypeName", data.getTypeName());
        info.put("SubtypeName", data.getSubtypeName());
        info.put("State", data.getState().name().toString());
        return info;
    }

    private static Object buildURL(URL data) throws JSONException {
        JSONObject url = new JSONObject();
        url.put("Authority", data.getAuthority());
        url.put("Host", data.getHost());
        url.put("Path", data.getPath());
        url.put("Port", data.getPort());
        url.put("Protocol", data.getProtocol());
        return url;
    }

    private static JSONObject buildPhoneAccount(PhoneAccount data)
            throws JSONException {
        JSONObject acct = new JSONObject();
        acct.put("Address", data.getAddress().toSafeString());
        acct.put("SubscriptionAddress", data.getSubscriptionAddress()
                .toSafeString());
        acct.put("Label", ((data.getLabel() != null) ? data.getLabel().toString() : ""));
        acct.put("ShortDescription", ((data.getShortDescription() != null) ? data
                .getShortDescription().toString() : ""));
        return acct;
    }

    private static Object buildPhoneAccountHandle(PhoneAccountHandle data)
            throws JSONException {
        JSONObject msg = new JSONObject();
        msg.put("id", data.getId());
        msg.put("ComponentName", data.getComponentName().flattenToString());
        return msg;
    }

    private static Object buildSubscriptionInfoRecord(SubscriptionInfo data)
            throws JSONException {
        JSONObject msg = new JSONObject();
        msg.put("subscriptionId", data.getSubscriptionId());
        msg.put("iccId", data.getIccId());
        msg.put("simSlotIndex", data.getSimSlotIndex());
        msg.put("displayName", data.getDisplayName());
        msg.put("nameSource", data.getNameSource());
        msg.put("iconTint", data.getIconTint());
        msg.put("number", data.getNumber());
        msg.put("dataRoaming", data.getDataRoaming());
        msg.put("mcc", data.getMcc());
        msg.put("mnc", data.getMnc());
        return msg;
    }

    private static Object buildPoint(Point data) throws JSONException {
        JSONObject point = new JSONObject();
        point.put("x", data.x);
        point.put("y", data.y);
        return point;
    }

    private static Object buildRttCapabilities(RttCapabilities data)
            throws JSONException {
        JSONObject cap = new JSONObject();
        cap.put("bwSupported", data.bwSupported);
        cap.put("lciSupported", data.lciSupported);
        cap.put("lcrSupported", data.lcrSupported);
        cap.put("oneSidedRttSupported", data.oneSidedRttSupported);
        cap.put("preambleSupported", data.preambleSupported);
        cap.put("twoSided11McRttSupported", data.twoSided11McRttSupported);
        return cap;
    }

    private static Object buildSmsMessage(SmsMessage data) throws JSONException {
        JSONObject msg = new JSONObject();
        msg.put("originatingAddress", data.getOriginatingAddress());
        msg.put("messageBody", data.getMessageBody());
        return msg;
    }

    private static JSONObject buildWifiActivityEnergyInfo(
            WifiActivityEnergyInfo data) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("ControllerEnergyUsed", data.getControllerEnergyUsed());
        result.put("ControllerIdleTimeMillis",
                data.getControllerIdleTimeMillis());
        result.put("ControllerRxTimeMillis", data.getControllerRxTimeMillis());
        result.put("ControllerTxTimeMillis", data.getControllerTxTimeMillis());
        result.put("StackState", data.getStackState());
        result.put("TimeStamp", data.getTimeStamp());
        return result;
    }

    private static Object buildWifiChannel(WifiChannel data) throws JSONException {
        JSONObject channel = new JSONObject();
        channel.put("channelNum", data.channelNum);
        channel.put("freqMHz", data.freqMHz);
        channel.put("isDFS", data.isDFS);
        channel.put("isValid", data.isValid());
        return channel;
    }

    private static Object buildWifiConfiguration(WifiConfiguration data)
            throws JSONException {
        JSONObject config = new JSONObject();
        config.put("networkId", data.networkId);
        // Trim the double quotes if exist
        if (data.SSID.charAt(0) == '"'
                && data.SSID.charAt(data.SSID.length() - 1) == '"') {
            config.put("SSID", data.SSID.substring(1, data.SSID.length() - 1));
        } else {
            config.put("SSID", data.SSID);
        }
        config.put("BSSID", data.BSSID);
        config.put("priority", data.priority);
        config.put("hiddenSSID", data.hiddenSSID);
        config.put("FQDN", data.FQDN);
        config.put("providerFriendlyName", data.providerFriendlyName);
        config.put("isPasspoint", data.isPasspoint());
        config.put("hiddenSSID", data.hiddenSSID);
        if (data.status == WifiConfiguration.Status.CURRENT) {
            config.put("status", "CURRENT");
        } else if (data.status == WifiConfiguration.Status.DISABLED) {
            config.put("status", "DISABLED");
        } else if (data.status == WifiConfiguration.Status.ENABLED) {
            config.put("status", "ENABLED");
        } else {
            config.put("status", "UNKNOWN");
        }
        // config.put("enterpriseConfig", buildWifiEnterpriseConfig(data.enterpriseConfig));
        return config;
    }

    private static Object buildWifiEnterpriseConfig(WifiEnterpriseConfig data)
            throws JSONException, CertificateEncodingException {
        JSONObject config = new JSONObject();
        config.put(WifiEnterpriseConfig.PLMN_KEY, data.getPlmn());
        config.put(WifiEnterpriseConfig.REALM_KEY, data.getRealm());
        config.put(WifiEnterpriseConfig.EAP_KEY, data.getEapMethod());
        config.put(WifiEnterpriseConfig.PHASE2_KEY, data.getPhase2Method());
        config.put(WifiEnterpriseConfig.ALTSUBJECT_MATCH_KEY, data.getAltSubjectMatch());
        X509Certificate caCert = data.getCaCertificate();
        String caCertString = Base64.encodeToString(caCert.getEncoded(), Base64.DEFAULT);
        config.put(WifiEnterpriseConfig.CA_CERT_KEY, caCertString);
        X509Certificate clientCert = data.getClientCertificate();
        String clientCertString = Base64.encodeToString(clientCert.getEncoded(), Base64.DEFAULT);
        config.put(WifiEnterpriseConfig.CLIENT_CERT_KEY, clientCertString);
        PrivateKey pk = data.getClientPrivateKey();
        String privateKeyString = Base64.encodeToString(pk.getEncoded(), Base64.DEFAULT);
        config.put(WifiEnterpriseConfig.PRIVATE_KEY_ID_KEY, privateKeyString);
        config.put(WifiEnterpriseConfig.PASSWORD_KEY, data.getPassword());
        return config;
    }

    private static JSONObject buildWifiP2pDevice(WifiP2pDevice data)
            throws JSONException {
        JSONObject deviceInfo = new JSONObject();
        deviceInfo.put("Name", data.deviceName);
        deviceInfo.put("Address", data.deviceAddress);
        return deviceInfo;
    }

    private static JSONObject buildWifiP2pGroup(WifiP2pGroup data)
            throws JSONException {
        JSONObject group = new JSONObject();
        Log.d("build p2p group.");
        group.put("ClientList", build(data.getClientList()));
        group.put("Interface", data.getInterface());
        group.put("Networkname", data.getNetworkName());
        group.put("Owner", data.getOwner());
        group.put("Passphrase", data.getPassphrase());
        group.put("NetworkId", data.getNetworkId());
        return group;
    }

    private static JSONObject buildWifiP2pInfo(WifiP2pInfo data)
            throws JSONException {
        JSONObject info = new JSONObject();
        Log.d("build p2p info.");
        info.put("groupFormed", data.groupFormed);
        info.put("isGroupOwner", data.isGroupOwner);
        info.put("groupOwnerAddress", data.groupOwnerAddress);
        return info;
    }

    private static <T> JSONObject buildCallEvent(InCallServiceImpl.CallEvent<T> callEvent)
            throws JSONException {
        JSONObject jsonEvent = new JSONObject();
        jsonEvent.put("CallId", callEvent.getCallId());
        jsonEvent.put("Event", build(callEvent.getEvent()));
        return jsonEvent;
    }

    private static JSONObject buildUri(Uri uri) throws JSONException {
        return new JSONObject().put("Uri", build((uri != null) ? uri.toString() : ""));
    }

    private static JSONObject buildCallDetails(Call.Details details) throws JSONException {

        JSONObject callDetails = new JSONObject();

        callDetails.put("Handle", buildUri(details.getHandle()));
        callDetails.put("HandlePresentation",
                build(InCallServiceImpl.getCallPresentationInfoString(
                        details.getHandlePresentation())));
        callDetails.put("CallerDisplayName", build(details.getCallerDisplayName()));

        // TODO AccountHandle
        // callDetails.put("AccountHandle", build(""));

        callDetails.put("Capabilities",
                build(InCallServiceImpl.getCallCapabilitiesString(details.getCallCapabilities())));

        callDetails.put("Properties",
                build(InCallServiceImpl.getCallPropertiesString(details.getCallProperties())));

        // TODO Parse fields in Disconnect Cause
        callDetails.put("DisconnectCause", build((details.getDisconnectCause() != null) ? details
                .getDisconnectCause().toString() : ""));
        callDetails.put("ConnectTimeMillis", build(details.getConnectTimeMillis()));

        // TODO: GatewayInfo
        // callDetails.put("GatewayInfo", build(""));

        callDetails.put("VideoState",
                build(InCallServiceImpl.getVideoCallStateString(details.getVideoState())));

        // TODO: StatusHints
        // callDetails.put("StatusHints", build(""));

        callDetails.put("Extras", build(details.getExtras()));

        return callDetails;
    }

    private static JSONObject buildCall(Call call) throws JSONException {

        JSONObject callInfo = new JSONObject();

        callInfo.put("Parent", build(InCallServiceImpl.getCallId(call)));

        // TODO:Make a function out of this for consistency
        ArrayList<String> children = new ArrayList<String>();
        for (Call child : call.getChildren()) {
            children.add(InCallServiceImpl.getCallId(child));
        }
        callInfo.put("Children", build(children));

        // TODO:Make a function out of this for consistency
        ArrayList<String> conferenceables = new ArrayList<String>();
        for (Call conferenceable : call.getChildren()) {
            children.add(InCallServiceImpl.getCallId(conferenceable));
        }
        callInfo.put("ConferenceableCalls", build(conferenceables));

        callInfo.put("State", build(InCallServiceImpl.getCallStateString(call.getState())));
        callInfo.put("CannedTextResponses", build(call.getCannedTextResponses()));
        callInfo.put("VideoCall", InCallServiceImpl.getVideoCallId(call.getVideoCall()));
        callInfo.put("Details", build(call.getDetails()));

        return callInfo;
    }

    private static JSONObject buildVideoProfile(VideoProfile videoProfile) throws JSONException {
        JSONObject profile = new JSONObject();

        profile.put("VideoState",
                InCallServiceImpl.getVideoCallStateString(videoProfile.getVideoState()));
        profile.put("VideoQuality",
                InCallServiceImpl.getVideoCallQualityString(videoProfile.getQuality()));

        return profile;
    }

    private static JSONObject buildCameraCapabilities(CameraCapabilities cameraCapabilities)
            throws JSONException {
        JSONObject capabilities = new JSONObject();

        capabilities.put("Height", build(cameraCapabilities.getHeight()));
        capabilities.put("Width", build(cameraCapabilities.getWidth()));
        capabilities.put("ZoomSupported", build(cameraCapabilities.isZoomSupported()));
        capabilities.put("MaxZoom", build(cameraCapabilities.getMaxZoom()));

        return capabilities;
    }

    private static JSONObject buildVoLteServiceStateEvent(
            VoLteServiceState volteInfo)
            throws JSONException {
        JSONObject info = new JSONObject();
        info.put(TelephonyConstants.VoLteServiceStateContainer.SRVCC_STATE,
                TelephonyUtils.getSrvccStateString(volteInfo.getSrvccState()));
        return info;
    }

    private static JSONObject buildLegacyVpnInfo(LegacyVpnInfo data) throws JSONException {
        JSONObject info = new JSONObject();
        if (data == null) {
            return info;
        }
        info.put("state", data.state);
        info.put("key", data.key);
        String intentStr = "";
        if (data.intent != null) {
            intentStr = data.intent.toString();
        }
        info.put("intent", intentStr);
        return info;
    }

    private static JSONObject buildModemActivityInfo(ModemActivityInfo modemInfo)
            throws JSONException {
        JSONObject info = new JSONObject();

        info.put("Timestamp", modemInfo.getTimestamp());
        info.put("SleepTimeMs", modemInfo.getSleepTimeMillis());
        info.put("IdleTimeMs", modemInfo.getIdleTimeMillis());
        // convert from int[] to List<Integer> for proper JSON translation
        int[] txTimes = modemInfo.getTxTimeMillis();
        List<Integer> tmp = new ArrayList<Integer>(txTimes.length);
        for (int val : txTimes) {
            tmp.add(val);
        }
        info.put("TxTimeMs", build(tmp));
        info.put("RxTimeMs", modemInfo.getRxTimeMillis());
        info.put("EnergyUsedMw", modemInfo.getEnergyUsed());
        return info;
    }

    private static JSONObject buildSignalStrength(SignalStrength signalStrength)
            throws JSONException {
        JSONObject info = new JSONObject();
        info.put(TelephonyConstants.SignalStrengthContainer.SIGNAL_STRENGTH_GSM,
                signalStrength.getGsmSignalStrength());
        info.put(
                TelephonyConstants.SignalStrengthContainer.SIGNAL_STRENGTH_GSM_DBM,
                signalStrength.getGsmDbm());
        info.put(
                TelephonyConstants.SignalStrengthContainer.SIGNAL_STRENGTH_GSM_LEVEL,
                signalStrength.getGsmLevel());
        info.put(
                TelephonyConstants.SignalStrengthContainer.SIGNAL_STRENGTH_GSM_ASU_LEVEL,
                signalStrength.getGsmAsuLevel());
        info.put(
                TelephonyConstants.SignalStrengthContainer.SIGNAL_STRENGTH_GSM_BIT_ERROR_RATE,
                signalStrength.getGsmBitErrorRate());
        info.put(
                TelephonyConstants.SignalStrengthContainer.SIGNAL_STRENGTH_CDMA_DBM,
                signalStrength.getCdmaDbm());
        info.put(
                TelephonyConstants.SignalStrengthContainer.SIGNAL_STRENGTH_CDMA_LEVEL,
                signalStrength.getCdmaLevel());
        info.put(
                TelephonyConstants.SignalStrengthContainer.SIGNAL_STRENGTH_CDMA_ASU_LEVEL,
                signalStrength.getCdmaAsuLevel());
        info.put(
                TelephonyConstants.SignalStrengthContainer.SIGNAL_STRENGTH_CDMA_ECIO,
                signalStrength.getCdmaEcio());
        info.put(
                TelephonyConstants.SignalStrengthContainer.SIGNAL_STRENGTH_EVDO_DBM,
                signalStrength.getEvdoDbm());
        info.put(
                TelephonyConstants.SignalStrengthContainer.SIGNAL_STRENGTH_EVDO_ECIO,
                signalStrength.getEvdoEcio());
        info.put(TelephonyConstants.SignalStrengthContainer.SIGNAL_STRENGTH_LTE,
                signalStrength.getLteSignalStrength());
        info.put(
                TelephonyConstants.SignalStrengthContainer.SIGNAL_STRENGTH_LTE_DBM,
                signalStrength.getLteDbm());
        info.put(
                TelephonyConstants.SignalStrengthContainer.SIGNAL_STRENGTH_LTE_LEVEL,
                signalStrength.getLteLevel());
        info.put(
                TelephonyConstants.SignalStrengthContainer.SIGNAL_STRENGTH_LTE_ASU_LEVEL,
                signalStrength.getLteAsuLevel());
        info.put(
                TelephonyConstants.SignalStrengthContainer.SIGNAL_STRENGTH_LEVEL,
                signalStrength.getLevel());
        info.put(
                TelephonyConstants.SignalStrengthContainer.SIGNAL_STRENGTH_ASU_LEVEL,
                signalStrength.getAsuLevel());
        info.put(TelephonyConstants.SignalStrengthContainer.SIGNAL_STRENGTH_DBM,
                signalStrength.getDbm());
        return info;
    }

    private JsonBuilder() {
        // This is a utility class.
    }
}
