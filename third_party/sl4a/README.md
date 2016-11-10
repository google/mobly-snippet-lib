Scripting Layer For Android
=============================

### Introduction
Originally authored by Damon Kohler, Scripting Layer for Android, SL4A, is an automation toolset
for calling Android APIs in a platform-independent manner. It supports both remote automation via
ADB as well as execution of scripts from on-device via a series of lightweight translation layers.

### Build Instructions
Due to its inclusion in AOSP as a privileged app, building SL4A requires a system build.

For the initial build of Android:

    cd <ANDROID_SOURCE_ROOT>
    source build/envsetup.sh
    lunch aosp_<TARGET>
    make [-j15]

*where `<ANDROID_SOURCE_ROOT>` is the root directory of the android tree and `<TARGET>` is the lunch
target name*

Then Build SL4A:

    cd <ANDROID_SOURCE_ROOT>/external/sl4a
    mm [-j15]

### Adding SL4A Builds to Android Builds by Default
1) If you are not using a custom buildspec, create one as follows:

        cp <ANDROID_SOURCE_ROOT>/build/buildspec.mk.default <ANDROID_SOURCE_ROOT>/buildspec.mk

2) Modify the buildspec to build SL4A as a custom module by editing
    the line '#CUSTOM_MODULES:=' to 'CUSTOM_MODULES:=sl4a':

        sed -i 's/#CUSTOM_MODULES:=/CUSTOM_MODULES:=sl4a/' <ANDROID_SOURCE_ROOT>/buildspec.mk

### Install Instructions
Run the following command:

    adb install -r <ANDROID_SOURCE_ROOT>/out/target/product/<TARGET>/data/app/sl4a/sl4a.apk

### Run Instructions
a) SL4A may be launched from Android as a normal App; or  
b) To enable RPC access from the command prompt:

    adb forward tcp:<HOST_PORT_NUM> tcp:<DEVICE_PORT_NUM>
    adb shell "am start -a com.googlecode.android_scripting.action.LAUNCH_SERVER \
               --ei com.googlecode.android_scripting.extra.USE_SERVICE_PORT <DEVICE_PORT_NUM> \
               com.googlecode.android_scripting/.activity.ScriptingLayerServiceLauncher"
*where `<HOST_PORT_NUM>` and `<DEVICE_PORT_NUM>` are the tcp ports on the host computer and device.*

### Generate the API Documentation
From SL4A source directory run this command:

        python Docs/generate_api_reference_md.py

In the Docs directory there should now be an ApiReference.md file that
contains which RPC functions are available in SL4A as well as documentation
for the RPC functions.

