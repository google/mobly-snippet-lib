# Scheduling RPCs Example

This example shows you how to use `scheduleRpc` which is built into
Mobly snippet lib to handle RPC scheduling.

## Why this is needed?

Some tests may need a snippet RPC to execute when the snippet client is unable
to reach the device, e.g., performing test actions while USB is disconnected.
For example, for battery testing (with Monsoon devices), we may want to measure
power consumed during certain test actions (e.g., phone calls). However
a Monsoon device turns off USB during battery data measurement, and a regular
snippet RPC won't work when the client is not connected to the device.
Therefore, prior to starting the Monsoon measurement we need to schedule a phone
call RPC prior to soccur during the measurement period.

In this scenario, the test steps would be:

1. Schedule the `makePhoneCall('123456')` to execute after (e.g., 10 seconds):

        s.scheduleRpc('makePhoneCall', 10000, ['123456'])

2. Start a Monsoon device to collect battery data, while simultaneously USB is
   turned off.
3. After 10 seconds, the phone call starts while USB is off.
4. Finally, after the phone call is finished, Monsoon data collection completes
   and USB is re-enabled.
5. The test retrieves any cached events or data from the device.



See the source code ExampleScheduleRpcSnippet.java for details.

## Running the example code

This folder contains a fully working example of a standalone snippet apk.

1.  Compile the example

        ./gradlew examples:ex5_schedule_rpc:assembleDebug

1.  Install the apk on your phone

        adb install -r ./examples/ex5_schedule_rpc/build/outputs/apk/debug/ex5_schedule_rpc-debug.apk

1.  Use `snippet_shell` from mobly to trigger `tryEvent()`:

        snippet_shell.py com.google.android.mobly.snippet.example5

        >>> callback = s.scheduleRpc('makeToast', 5000, ['message'])

        Wait for the message to show up on the screen (sync RPC call)

        >>> callback.waitAndGet('makeToast').data
        {u'callback': u'null', u'error': u'null', u'result': u'OK', u'id': u'0'}

        >>> callback = s.scheduleRpc('asyncMakeToast', 5000, ['message'])

        Wait for the message to show up on the screen (async RPC call)

        >>> callback.waitAndGet('asyncMakeToast').data
        {u'callback': u'1-1', u'error': u'null', u'result': u'null', u'id': u'0'}
