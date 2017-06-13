# Async Event Rpc Example

This example shows you how to use the ScheduleRpcUtil.java of Mobly snippet lib
to handle RPC scheduling.

## Why this is needed?

Some tests may need to keep USB off while performing test actions. For example,
for battery test (with Monsoon devices), we may want to measure battery consumption for certain
test actions (e.g., phone calls). While a phone call is in progress, Monsoon device turns off USB
and starts to collect battery data. However, regular snippet RPC won't work while USB is off.
Therefore we need to schedule phone call RPCs before Monsoon starts.

In this scenario, test steps would be:

1. Schedule s.makePhonecCall('123456') after, e.g., 10 seconds, such as:

        s.scheduleRpc('makePhonecCall', 10000, ['123456'])

2. Start Monsoon device to collect battery data, at the same time, USB is turned off.
3. After 10 seconds, phone call starts while USB is off.
4. Finally, phonecall finished, Monsoon data collection completed and USB is turned on.


See the source code ExampleScheduleRpcSnippet.java for details.

## Running the example code

This folder contains a fully working example of a standalone snippet apk.

1.  Compile the example

        ./gradlew examples:ex5_schedule_rpc:assembleDebug

1.  Install the apk on your phone

        adb install -r ./examples/ex5_schedule_rpc/build/outputs/apk/ex5_schedule_rpc-debug.apk

1.  Use `snippet_shell` from mobly to trigger `tryEvent()`:

        snippet_shell.py com.google.android.mobly.snippet.example4

        >>> callback = s.scheduleRpc('makeToast', 5000, ['message'])

        Wait for the message to show up on the screen (sync RPC call)

        >>> callback.waitAndGet('makeToast').data
        {u'callback': u'null', u'error': u'null', u'result': u'OK', u'id': u'0'}

        >>> callback = s.scheduleRpc('asyncMakeToast', 5000, ['message'])

        Wait for the message to show up on the screen (async RPC call)

        >>> callback.waitAndGet('asyncMakeToast').data
        {u'callback': u'1-1', u'error': u'null', u'result': u'null', u'id': u'0'}
