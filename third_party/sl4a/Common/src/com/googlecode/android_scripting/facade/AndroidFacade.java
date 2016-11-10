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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.os.UserHandle;
import android.os.Vibrator;
import android.content.ClipboardManager;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.Toast;

import com.googlecode.android_scripting.BaseApplication;
import com.googlecode.android_scripting.FileUtils;
import com.googlecode.android_scripting.FutureActivityTaskExecutor;
import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.NotificationIdFactory;
import com.googlecode.android_scripting.future.FutureActivityTask;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcDefault;
import com.googlecode.android_scripting.rpc.RpcDeprecated;
import com.googlecode.android_scripting.rpc.RpcOptional;
import com.googlecode.android_scripting.rpc.RpcParameter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Some general purpose Android routines.<br>
 * <h2>Intents</h2> Intents are returned as a map, in the following form:<br>
 * <ul>
 * <li><b>action</b> - action.
 * <li><b>data</b> - url
 * <li><b>type</b> - mime type
 * <li><b>packagename</b> - name of package. If used, requires classname to be useful (optional)
 * <li><b>classname</b> - name of class. If used, requires packagename to be useful (optional)
 * <li><b>categories</b> - list of categories
 * <li><b>extras</b> - map of extras
 * <li><b>flags</b> - integer flags.
 * </ul>
 * <br>
 * An intent can be built using the {@see #makeIntent} call, but can also be constructed exterally.
 *
 */
public class AndroidFacade extends RpcReceiver {
  /**
   * An instance of this interface is passed to the facade. From this object, the resource IDs can
   * be obtained.
   */

  public interface Resources {
    int getLogo48();
  }

  private final Service mService;
  private final Handler mHandler;
  private final Intent mIntent;
  private final FutureActivityTaskExecutor mTaskQueue;

  private final Vibrator mVibrator;
  private final NotificationManager mNotificationManager;

  private final Resources mResources;
  private ClipboardManager mClipboard = null;

  @Override
  public void shutdown() {
  }

  public AndroidFacade(FacadeManager manager) {
    super(manager);
    mService = manager.getService();
    mIntent = manager.getIntent();
    BaseApplication application = ((BaseApplication) mService.getApplication());
    mTaskQueue = application.getTaskExecutor();
    mHandler = new Handler(mService.getMainLooper());
    mVibrator = (Vibrator) mService.getSystemService(Context.VIBRATOR_SERVICE);
    mNotificationManager =
        (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
    mResources = manager.getAndroidFacadeResources();
  }

  ClipboardManager getClipboardManager() {
    Object clipboard = null;
    if (mClipboard == null) {
      try {
        clipboard = mService.getSystemService(Context.CLIPBOARD_SERVICE);
      } catch (Exception e) {
        Looper.prepare(); // Clipboard manager won't work without this on higher SDK levels...
        clipboard = mService.getSystemService(Context.CLIPBOARD_SERVICE);
      }
      mClipboard = (ClipboardManager) clipboard;
      if (mClipboard == null) {
        Log.w("Clipboard managed not accessible.");
      }
    }
    return mClipboard;
  }

  public Intent startActivityForResult(final Intent intent) {
    FutureActivityTask<Intent> task = new FutureActivityTask<Intent>() {
      @Override
      public void onCreate() {
        super.onCreate();
        try {
          startActivityForResult(intent, 0);
        } catch (Exception e) {
          intent.putExtra("EXCEPTION", e.getMessage());
          setResult(intent);
        }
      }

      @Override
      public void onActivityResult(int requestCode, int resultCode, Intent data) {
        setResult(data);
      }
    };
    mTaskQueue.execute(task);

    try {
      return task.getResult();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      task.finish();
    }
  }

  public int startActivityForResultCodeWithTimeout(final Intent intent,
    final int request, final int timeout) {
    FutureActivityTask<Integer> task = new FutureActivityTask<Integer>() {
      @Override
      public void onCreate() {
        super.onCreate();
        try {
          startActivityForResult(intent, request);
        } catch (Exception e) {
          intent.putExtra("EXCEPTION", e.getMessage());
        }
      }

      @Override
      public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (request == requestCode){
            setResult(resultCode);
        }
      }
    };
    mTaskQueue.execute(task);

    try {
      return task.getResult(timeout, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      task.finish();
    }
  }

  // TODO(damonkohler): Pull this out into proper argument deserialization and support
  // complex/nested types being passed in.
  public static void putExtrasFromJsonObject(JSONObject extras,
                                             Intent intent) throws JSONException {
    JSONArray names = extras.names();
    for (int i = 0; i < names.length(); i++) {
      String name = names.getString(i);
      Object data = extras.get(name);
      if (data == null) {
        continue;
      }
      if (data instanceof Integer) {
        intent.putExtra(name, (Integer) data);
      }
      if (data instanceof Float) {
        intent.putExtra(name, (Float) data);
      }
      if (data instanceof Double) {
        intent.putExtra(name, (Double) data);
      }
      if (data instanceof Long) {
        intent.putExtra(name, (Long) data);
      }
      if (data instanceof String) {
        intent.putExtra(name, (String) data);
      }
      if (data instanceof Boolean) {
        intent.putExtra(name, (Boolean) data);
      }
      // Nested JSONObject
      if (data instanceof JSONObject) {
        Bundle nestedBundle = new Bundle();
        intent.putExtra(name, nestedBundle);
        putNestedJSONObject((JSONObject) data, nestedBundle);
      }
      // Nested JSONArray. Doesn't support mixed types in single array
      if (data instanceof JSONArray) {
        // Empty array. No way to tell what type of data to pass on, so skipping
        if (((JSONArray) data).length() == 0) {
          Log.e("Empty array not supported in JSONObject, skipping");
          continue;
        }
        // Integer
        if (((JSONArray) data).get(0) instanceof Integer) {
          Integer[] integerArrayData = new Integer[((JSONArray) data).length()];
          for (int j = 0; j < ((JSONArray) data).length(); ++j) {
            integerArrayData[j] = ((JSONArray) data).getInt(j);
          }
          intent.putExtra(name, integerArrayData);
        }
        // Double
        if (((JSONArray) data).get(0) instanceof Double) {
          Double[] doubleArrayData = new Double[((JSONArray) data).length()];
          for (int j = 0; j < ((JSONArray) data).length(); ++j) {
            doubleArrayData[j] = ((JSONArray) data).getDouble(j);
          }
          intent.putExtra(name, doubleArrayData);
        }
        // Long
        if (((JSONArray) data).get(0) instanceof Long) {
          Long[] longArrayData = new Long[((JSONArray) data).length()];
          for (int j = 0; j < ((JSONArray) data).length(); ++j) {
            longArrayData[j] = ((JSONArray) data).getLong(j);
          }
          intent.putExtra(name, longArrayData);
        }
        // String
        if (((JSONArray) data).get(0) instanceof String) {
          String[] stringArrayData = new String[((JSONArray) data).length()];
          for (int j = 0; j < ((JSONArray) data).length(); ++j) {
            stringArrayData[j] = ((JSONArray) data).getString(j);
          }
          intent.putExtra(name, stringArrayData);
        }
        // Boolean
        if (((JSONArray) data).get(0) instanceof Boolean) {
          Boolean[] booleanArrayData = new Boolean[((JSONArray) data).length()];
          for (int j = 0; j < ((JSONArray) data).length(); ++j) {
            booleanArrayData[j] = ((JSONArray) data).getBoolean(j);
          }
          intent.putExtra(name, booleanArrayData);
        }
      }
    }
  }

  // Contributed by Emmanuel T
  // Nested Array handling contributed by Sergey Zelenev
  private static void putNestedJSONObject(JSONObject jsonObject, Bundle bundle)
      throws JSONException {
    JSONArray names = jsonObject.names();
    for (int i = 0; i < names.length(); i++) {
      String name = names.getString(i);
      Object data = jsonObject.get(name);
      if (data == null) {
        continue;
      }
      if (data instanceof Integer) {
        bundle.putInt(name, ((Integer) data).intValue());
      }
      if (data instanceof Float) {
        bundle.putFloat(name, ((Float) data).floatValue());
      }
      if (data instanceof Double) {
        bundle.putDouble(name, ((Double) data).doubleValue());
      }
      if (data instanceof Long) {
        bundle.putLong(name, ((Long) data).longValue());
      }
      if (data instanceof String) {
        bundle.putString(name, (String) data);
      }
      if (data instanceof Boolean) {
        bundle.putBoolean(name, ((Boolean) data).booleanValue());
      }
      // Nested JSONObject
      if (data instanceof JSONObject) {
        Bundle nestedBundle = new Bundle();
        bundle.putBundle(name, nestedBundle);
        putNestedJSONObject((JSONObject) data, nestedBundle);
      }
      // Nested JSONArray. Doesn't support mixed types in single array
      if (data instanceof JSONArray) {
        // Empty array. No way to tell what type of data to pass on, so skipping
        if (((JSONArray) data).length() == 0) {
          Log.e("Empty array not supported in nested JSONObject, skipping");
          continue;
        }
        // Integer
        if (((JSONArray) data).get(0) instanceof Integer) {
          int[] integerArrayData = new int[((JSONArray) data).length()];
          for (int j = 0; j < ((JSONArray) data).length(); ++j) {
            integerArrayData[j] = ((JSONArray) data).getInt(j);
          }
          bundle.putIntArray(name, integerArrayData);
        }
        // Double
        if (((JSONArray) data).get(0) instanceof Double) {
          double[] doubleArrayData = new double[((JSONArray) data).length()];
          for (int j = 0; j < ((JSONArray) data).length(); ++j) {
            doubleArrayData[j] = ((JSONArray) data).getDouble(j);
          }
          bundle.putDoubleArray(name, doubleArrayData);
        }
        // Long
        if (((JSONArray) data).get(0) instanceof Long) {
          long[] longArrayData = new long[((JSONArray) data).length()];
          for (int j = 0; j < ((JSONArray) data).length(); ++j) {
            longArrayData[j] = ((JSONArray) data).getLong(j);
          }
          bundle.putLongArray(name, longArrayData);
        }
        // String
        if (((JSONArray) data).get(0) instanceof String) {
          String[] stringArrayData = new String[((JSONArray) data).length()];
          for (int j = 0; j < ((JSONArray) data).length(); ++j) {
            stringArrayData[j] = ((JSONArray) data).getString(j);
          }
          bundle.putStringArray(name, stringArrayData);
        }
        // Boolean
        if (((JSONArray) data).get(0) instanceof Boolean) {
          boolean[] booleanArrayData = new boolean[((JSONArray) data).length()];
          for (int j = 0; j < ((JSONArray) data).length(); ++j) {
            booleanArrayData[j] = ((JSONArray) data).getBoolean(j);
          }
          bundle.putBooleanArray(name, booleanArrayData);
        }
      }
    }
  }

  void startActivity(final Intent intent) {
    try {
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      mService.startActivity(intent);
    } catch (Exception e) {
      Log.e("Failed to launch intent.", e);
    }
  }

  private Intent buildIntent(String action, String uri, String type, JSONObject extras,
      String packagename, String classname, JSONArray categories) throws JSONException {
    Intent intent = new Intent();
    if (action != null) {
      intent.setAction(action);
    }
    intent.setDataAndType(uri != null ? Uri.parse(uri) : null, type);
    if (packagename != null && classname != null) {
      intent.setComponent(new ComponentName(packagename, classname));
    }
    if (extras != null) {
      putExtrasFromJsonObject(extras, intent);
    }
    if (categories != null) {
      for (int i = 0; i < categories.length(); i++) {
        intent.addCategory(categories.getString(i));
      }
    }
    return intent;
  }

  // TODO(damonkohler): It's unnecessary to add the complication of choosing between startActivity
  // and startActivityForResult. It's probably better to just always use the ForResult version.
  // However, this makes the call always blocking. We'd need to add an extra boolean parameter to
  // indicate if we should wait for a result.
  @Rpc(description = "Starts an activity and returns the result.",
       returns = "A Map representation of the result Intent.")
  public Intent startActivityForResult(
      @RpcParameter(name = "action")
      String action,
      @RpcParameter(name = "uri")
      @RpcOptional String uri,
      @RpcParameter(name = "type", description = "MIME type/subtype of the URI")
      @RpcOptional String type,
      @RpcParameter(name = "extras", description = "a Map of extras to add to the Intent")
      @RpcOptional JSONObject extras,
      @RpcParameter(name = "packagename",
                    description = "name of package. If used, requires classname to be useful")
      @RpcOptional String packagename,
      @RpcParameter(name = "classname",
                    description = "name of class. If used, requires packagename to be useful")
      @RpcOptional String classname
      ) throws JSONException {
    final Intent intent = buildIntent(action, uri, type, extras, packagename, classname, null);
    return startActivityForResult(intent);
  }

  @Rpc(description = "Starts an activity and returns the result.",
       returns = "A Map representation of the result Intent.")
  public Intent startActivityForResultIntent(
      @RpcParameter(name = "intent",
                    description = "Intent in the format as returned from makeIntent")
      Intent intent) {
    return startActivityForResult(intent);
  }

  private void doStartActivity(final Intent intent, Boolean wait) throws Exception {
    if (wait == null || wait == false) {
      startActivity(intent);
    } else {
      FutureActivityTask<Intent> task = new FutureActivityTask<Intent>() {
        private boolean mSecondResume = false;

        @Override
        public void onCreate() {
          super.onCreate();
          startActivity(intent);
        }

        @Override
        public void onResume() {
          if (mSecondResume) {
            finish();
          }
          mSecondResume = true;
        }

        @Override
        public void onDestroy() {
          setResult(null);
        }

      };
      mTaskQueue.execute(task);

      try {
        task.getResult();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Creates a new AndroidFacade that simplifies the interface to various Android APIs.
   *
   * @param service
   *          is the {@link Context} the APIs will run under
   */

  @Rpc(description = "Put a text string in the clipboard.")
  public void setTextClip(@RpcParameter(name = "text")
                          String text,
                          @RpcParameter(name = "label")
                          @RpcOptional @RpcDefault(value = "copiedText")
                          String label) {
    getClipboardManager().setPrimaryClip(ClipData.newPlainText(label, text));
  }

  @Rpc(description = "Get the device serial number.")
  public String getBuildSerial() {
      return Build.SERIAL;
  }

  @Rpc(description = "Get the name of system bootloader version number.")
  public String getBuildBootloader() {
    return android.os.Build.BOOTLOADER;
  }

  @Rpc(description = "Get the name of the industrial design.")
  public String getBuildIndustrialDesignName() {
    return Build.DEVICE;
  }

  @Rpc(description = "Get the build ID string meant for displaying to the user")
  public String getBuildDisplay() {
    return Build.DISPLAY;
  }

  @Rpc(description = "Get the string that uniquely identifies this build.")
  public String getBuildFingerprint() {
    return Build.FINGERPRINT;
  }

  @Rpc(description = "Get the name of the hardware (from the kernel command "
      + "line or /proc)..")
  public String getBuildHardware() {
    return Build.HARDWARE;
  }

  @Rpc(description = "Get the device host.")
  public String getBuildHost() {
    return Build.HOST;
  }

  @Rpc(description = "Get Either a changelist number, or a label like."
      + " \"M4-rc20\".")
  public String getBuildID() {
    return android.os.Build.ID;
  }

  @Rpc(description = "Returns true if we are running a debug build such"
      + " as \"user-debug\" or \"eng\".")
  public boolean getBuildIsDebuggable() {
    return Build.IS_DEBUGGABLE;
  }

  @Rpc(description = "Get the name of the overall product.")
  public String getBuildProduct() {
    return android.os.Build.PRODUCT;
  }

  @Rpc(description = "Get an ordered list of 32 bit ABIs supported by this "
      + "device. The most preferred ABI is the first element in the list")
  public String[] getBuildSupported32BitAbis() {
    return Build.SUPPORTED_32_BIT_ABIS;
  }

  @Rpc(description = "Get an ordered list of 64 bit ABIs supported by this "
      + "device. The most preferred ABI is the first element in the list")
  public String[] getBuildSupported64BitAbis() {
    return Build.SUPPORTED_64_BIT_ABIS;
  }

  @Rpc(description = "Get an ordered list of ABIs supported by this "
      + "device. The most preferred ABI is the first element in the list")
  public String[] getBuildSupportedBitAbis() {
    return Build.SUPPORTED_ABIS;
  }

  @Rpc(description = "Get comma-separated tags describing the build,"
      + " like \"unsigned,debug\".")
  public String getBuildTags() {
    return Build.TAGS;
  }

  @Rpc(description = "Get The type of build, like \"user\" or \"eng\".")
  public String getBuildType() {
    return Build.TYPE;
  }
  @Rpc(description = "Returns the board name.")
  public String getBuildBoard() {
    return Build.BOARD;
  }

  @Rpc(description = "Returns the brand name.")
  public String getBuildBrand() {
    return Build.BRAND;
  }

  @Rpc(description = "Returns the manufacturer name.")
  public String getBuildManufacturer() {
    return Build.MANUFACTURER;
  }

  @Rpc(description = "Returns the model name.")
  public String getBuildModel() {
    return Build.MODEL;
  }

  @Rpc(description = "Returns the build number.")
  public String getBuildNumber() {
    return Build.FINGERPRINT;
  }

  @Rpc(description = "Returns the SDK version.")
  public Integer getBuildSdkVersion() {
    return Build.VERSION.SDK_INT;
  }

  @Rpc(description = "Returns the current device time.")
  public Long getBuildTime() {
    return Build.TIME;
  }

  @Rpc(description = "Read all text strings copied by setTextClip from the clipboard.")
  public List<String> getTextClip() {
    ClipboardManager cm = getClipboardManager();
    ArrayList<String> texts = new ArrayList<String>();
    if(!cm.hasPrimaryClip()) {
      return texts;
    }
    ClipData cd = cm.getPrimaryClip();
    for(int i=0; i<cd.getItemCount(); i++) {
      texts.add(cd.getItemAt(i).coerceToText(mService).toString());
    }
    return texts;
  }

  /**
   * packagename and classname, if provided, are used in a 'setComponent' call.
   */
  @Rpc(description = "Starts an activity.")
  public void startActivity(
      @RpcParameter(name = "action")
      String action,
      @RpcParameter(name = "uri")
      @RpcOptional String uri,
      @RpcParameter(name = "type", description = "MIME type/subtype of the URI")
      @RpcOptional String type,
      @RpcParameter(name = "extras", description = "a Map of extras to add to the Intent")
      @RpcOptional JSONObject extras,
      @RpcParameter(name = "wait", description = "block until the user exits the started activity")
      @RpcOptional Boolean wait,
      @RpcParameter(name = "packagename",
                    description = "name of package. If used, requires classname to be useful")
      @RpcOptional String packagename,
      @RpcParameter(name = "classname",
                    description = "name of class. If used, requires packagename to be useful")
      @RpcOptional String classname
      ) throws Exception {
    final Intent intent = buildIntent(action, uri, type, extras, packagename, classname, null);
    doStartActivity(intent, wait);
  }

  @Rpc(description = "Send a broadcast.")
  public void sendBroadcast(
      @RpcParameter(name = "action")
      String action,
      @RpcParameter(name = "uri")
      @RpcOptional String uri,
      @RpcParameter(name = "type", description = "MIME type/subtype of the URI")
      @RpcOptional String type,
      @RpcParameter(name = "extras", description = "a Map of extras to add to the Intent")
      @RpcOptional JSONObject extras,
      @RpcParameter(name = "packagename",
                    description = "name of package. If used, requires classname to be useful")
      @RpcOptional String packagename,
      @RpcParameter(name = "classname",
                    description = "name of class. If used, requires packagename to be useful")
      @RpcOptional String classname
      ) throws JSONException {
    final Intent intent = buildIntent(action, uri, type, extras, packagename, classname, null);
    try {
      mService.sendBroadcast(intent);
    } catch (Exception e) {
      Log.e("Failed to broadcast intent.", e);
    }
  }

  @Rpc(description = "Starts a service.")
  public void startService(
      @RpcParameter(name = "uri")
      @RpcOptional String uri,
      @RpcParameter(name = "extras", description = "a Map of extras to add to the Intent")
      @RpcOptional JSONObject extras,
      @RpcParameter(name = "packagename",
                    description = "name of package. If used, requires classname to be useful")
      @RpcOptional String packagename,
      @RpcParameter(name = "classname",
                    description = "name of class. If used, requires packagename to be useful")
      @RpcOptional String classname
      ) throws Exception {
    final Intent intent = buildIntent(null /* action */, uri, null /* type */, extras, packagename,
                                      classname, null /* categories */);
    mService.startService(intent);
  }

  @Rpc(description = "Create an Intent.", returns = "An object representing an Intent")
  public Intent makeIntent(
      @RpcParameter(name = "action")
      String action,
      @RpcParameter(name = "uri")
      @RpcOptional String uri,
      @RpcParameter(name = "type", description = "MIME type/subtype of the URI")
      @RpcOptional String type,
      @RpcParameter(name = "extras", description = "a Map of extras to add to the Intent")
      @RpcOptional JSONObject extras,
      @RpcParameter(name = "categories", description = "a List of categories to add to the Intent")
      @RpcOptional JSONArray categories,
      @RpcParameter(name = "packagename",
                    description = "name of package. If used, requires classname to be useful")
      @RpcOptional String packagename,
      @RpcParameter(name = "classname",
                    description = "name of class. If used, requires packagename to be useful")
      @RpcOptional String classname,
      @RpcParameter(name = "flags", description = "Intent flags")
      @RpcOptional Integer flags
      ) throws JSONException {
    Intent intent = buildIntent(action, uri, type, extras, packagename, classname, categories);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if (flags != null) {
      intent.setFlags(flags);
    }
    return intent;
  }

  @Rpc(description = "Start Activity using Intent")
  public void startActivityIntent(
      @RpcParameter(name = "intent",
                    description = "Intent in the format as returned from makeIntent")
      Intent intent,
      @RpcParameter(name = "wait",
                    description = "block until the user exits the started activity")
      @RpcOptional Boolean wait
      ) throws Exception {
    doStartActivity(intent, wait);
  }

  @Rpc(description = "Send Broadcast Intent")
  public void sendBroadcastIntent(
      @RpcParameter(name = "intent",
                    description = "Intent in the format as returned from makeIntent")
      Intent intent
      ) throws Exception {
    mService.sendBroadcast(intent);
  }

  @Rpc(description = "Start Service using Intent")
  public void startServiceIntent(
      @RpcParameter(name = "intent",
                    description = "Intent in the format as returned from makeIntent")
      Intent intent
      ) throws Exception {
    mService.startService(intent);
  }

  @Rpc(description = "Send Broadcast Intent as system user.")
  public void sendBroadcastIntentAsUserAll(
      @RpcParameter(name = "intent",
                    description = "Intent in the format as returned from makeIntent")
      Intent intent
      ) throws Exception {
    mService.sendBroadcastAsUser(intent, UserHandle.ALL);
  }

  @Rpc(description = "Vibrates the phone or a specified duration in milliseconds.")
  public void vibrate(
      @RpcParameter(name = "duration", description = "duration in milliseconds")
      @RpcDefault("300")
      Integer duration) {
    mVibrator.vibrate(duration);
  }

  @Rpc(description = "Displays a short-duration Toast notification.")
  public void makeToast(@RpcParameter(name = "message") final String message) {
    mHandler.post(new Runnable() {
      public void run() {
        Toast.makeText(mService, message, Toast.LENGTH_SHORT).show();
      }
    });
  }

  private String getInputFromAlertDialog(final String title, final String message,
      final boolean password) {
    final FutureActivityTask<String> task = new FutureActivityTask<String>() {
      @Override
      public void onCreate() {
        super.onCreate();
        final EditText input = new EditText(getActivity());
        if (password) {
          input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
          input.setTransformationMethod(new PasswordTransformationMethod());
        }
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(title);
        alert.setMessage(message);
        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int whichButton) {
            dialog.dismiss();
            setResult(input.getText().toString());
            finish();
          }
        });
        alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            dialog.dismiss();
            setResult(null);
            finish();
          }
        });
        alert.show();
      }
    };
    mTaskQueue.execute(task);

    try {
      return task.getResult();
    } catch (Exception e) {
      Log.e("Failed to display dialog.", e);
      throw new RuntimeException(e);
    }
  }

  @Rpc(description = "Queries the user for a text input.")
  @RpcDeprecated(value = "dialogGetInput", release = "r3")
  public String getInput(
      @RpcParameter(name = "title", description = "title of the input box")
      @RpcDefault("SL4A Input")
      final String title,
      @RpcParameter(name = "message", description = "message to display above the input box")
      @RpcDefault("Please enter value:")
      final String message) {
    return getInputFromAlertDialog(title, message, false);
  }

  @Rpc(description = "Queries the user for a password.")
  @RpcDeprecated(value = "dialogGetPassword", release = "r3")
  public String getPassword(
      @RpcParameter(name = "title", description = "title of the input box")
      @RpcDefault("SL4A Password Input")
      final String title,
      @RpcParameter(name = "message", description = "message to display above the input box")
      @RpcDefault("Please enter password:")
      final String message) {
    return getInputFromAlertDialog(title, message, true);
  }

  @Rpc(description = "Displays a notification that will be canceled when the user clicks on it.")
  public void notify(@RpcParameter(name = "title", description = "title") String title,
      @RpcParameter(name = "message") String message) {
    // This contentIntent is a noop.
    PendingIntent contentIntent = PendingIntent.getService(mService, 0, new Intent(), 0);
    Notification.Builder builder = new Notification.Builder(mService);
    builder.setSmallIcon(mResources.getLogo48())
           .setTicker(message)
           .setWhen(System.currentTimeMillis())
           .setContentTitle(title)
           .setContentText(message)
           .setContentIntent(contentIntent);
    Notification notification = builder.build();
    notification.flags = Notification.FLAG_AUTO_CANCEL;
    // Get a unique notification id from the application.
    final int notificationId = NotificationIdFactory.create();
    mNotificationManager.notify(notificationId, notification);
  }

  @Rpc(description = "Returns the intent that launched the script.")
  public Object getIntent() {
    return mIntent;
  }

  @Rpc(description = "Launches an activity that sends an e-mail message to a given recipient.")
  public void sendEmail(
      @RpcParameter(name = "to", description = "A comma separated list of recipients.")
      final String to,
      @RpcParameter(name = "subject") final String subject,
      @RpcParameter(name = "body") final String body,
      @RpcParameter(name = "attachmentUri")
      @RpcOptional final String attachmentUri) {
    final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
    intent.setType("plain/text");
    intent.putExtra(android.content.Intent.EXTRA_EMAIL, to.split(","));
    intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
    intent.putExtra(android.content.Intent.EXTRA_TEXT, body);
    if (attachmentUri != null) {
      intent.putExtra(android.content.Intent.EXTRA_STREAM, Uri.parse(attachmentUri));
    }
    startActivity(intent);
  }

  @Rpc(description = "Returns package version code.")
  public int getPackageVersionCode(@RpcParameter(name = "packageName") final String packageName) {
    int result = -1;
    PackageInfo pInfo = null;
    try {
      pInfo =
          mService.getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
    } catch (NameNotFoundException e) {
      pInfo = null;
    }
    if (pInfo != null) {
      result = pInfo.versionCode;
    }
    return result;
  }

  @Rpc(description = "Returns package version name.")
  public String getPackageVersion(@RpcParameter(name = "packageName") final String packageName) {
    PackageInfo packageInfo = null;
    try {
      packageInfo =
          mService.getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
    } catch (NameNotFoundException e) {
      return null;
    }
    if (packageInfo != null) {
      return packageInfo.versionName;
    }
    return null;
  }

  @Rpc(description = "Checks if SL4A's version is >= the specified version.")
  public boolean requiredVersion(
          @RpcParameter(name = "requiredVersion") final Integer version) {
    boolean result = false;
    int packageVersion = getPackageVersionCode(
            "com.googlecode.android_scripting");
    if (version > -1) {
      result = (packageVersion >= version);
    }
    return result;
  }

  @Rpc(description = "Writes message to logcat at verbose level")
  public void logV(
          @RpcParameter(name = "message")
          String message) {
      android.util.Log.v("SL4A: ", message);
  }

  @Rpc(description = "Writes message to logcat at info level")
  public void logI(
          @RpcParameter(name = "message")
          String message) {
      android.util.Log.i("SL4A: ", message);
  }

  @Rpc(description = "Writes message to logcat at debug level")
  public void logD(
          @RpcParameter(name = "message")
          String message) {
      android.util.Log.d("SL4A: ", message);
  }

  @Rpc(description = "Writes message to logcat at warning level")
  public void logW(
          @RpcParameter(name = "message")
          String message) {
      android.util.Log.w("SL4A: ", message);
  }

  @Rpc(description = "Writes message to logcat at error level")
  public void logE(
          @RpcParameter(name = "message")
          String message) {
      android.util.Log.e("SL4A: ", message);
  }

  @Rpc(description = "Writes message to logcat at wtf level")
  public void logWTF(
          @RpcParameter(name = "message")
          String message) {
      android.util.Log.wtf("SL4A: ", message);
  }

  /**
   *
   * Map returned:
   *
   * <pre>
   *   TZ = Timezone
   *     id = Timezone ID
   *     display = Timezone display name
   *     offset = Offset from UTC (in ms)
   *   SDK = SDK Version
   *   download = default download path
   *   appcache = Location of application cache
   *   sdcard = Space on sdcard
   *     availblocks = Available blocks
   *     blockcount = Total Blocks
   *     blocksize = size of block.
   * </pre>
   */
  @Rpc(description = "A map of various useful environment details")
  public Map<String, Object> environment() {
    Map<String, Object> result = new HashMap<String, Object>();
    Map<String, Object> zone = new HashMap<String, Object>();
    Map<String, Object> space = new HashMap<String, Object>();
    TimeZone tz = TimeZone.getDefault();
    zone.put("id", tz.getID());
    zone.put("display", tz.getDisplayName());
    zone.put("offset", tz.getOffset((new Date()).getTime()));
    result.put("TZ", zone);
    result.put("SDK", android.os.Build.VERSION.SDK_INT);
    result.put("download", FileUtils.getExternalDownload().getAbsolutePath());
    result.put("appcache", mService.getCacheDir().getAbsolutePath());
    try {
      StatFs fs = new StatFs("/sdcard");
      space.put("availblocks", fs.getAvailableBlocksLong());
      space.put("blocksize", fs.getBlockSizeLong());
      space.put("blockcount", fs.getBlockCountLong());
    } catch (Exception e) {
      space.put("exception", e.toString());
    }
    result.put("sdcard", space);
    return result;
  }

  @Rpc(description = "Get list of constants (static final fields) for a class")
  public Bundle getConstants(
      @RpcParameter(name = "classname", description = "Class to get constants from")
      String classname)
      throws Exception {
    Bundle result = new Bundle();
    int flags = Modifier.FINAL | Modifier.PUBLIC | Modifier.STATIC;
    Class<?> clazz = Class.forName(classname);
    for (Field field : clazz.getFields()) {
      if ((field.getModifiers() & flags) == flags) {
        Class<?> type = field.getType();
        String name = field.getName();
        if (type == int.class) {
          result.putInt(name, field.getInt(null));
        } else if (type == long.class) {
          result.putLong(name, field.getLong(null));
        } else if (type == double.class) {
          result.putDouble(name, field.getDouble(null));
        } else if (type == char.class) {
          result.putChar(name, field.getChar(null));
        } else if (type instanceof Object) {
          result.putString(name, field.get(null).toString());
        }
      }
    }
    return result;
  }

}
