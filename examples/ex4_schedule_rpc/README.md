# Async Event Rpc Example

This example shows you how to use the ScheduleRpcUtil.java of Mobly snippet lib
to handle RPC scheduling.

See the source code ExampleScheduleRpcSnippet.java for details.

## Running the example code

This folder contains a fully working example of a standalone snippet apk.

1.  Compile the example

        ./gradlew examples:ex4_schedule_rpc:assembleDebug

1.  Install the apk on your phone

        adb install -r ./examples/ex4_schedule_rpc/build/outputs/apk/ex4_schedule_rpc-debug.apk

1.  Use `snippet_shell` from mobly to trigger `tryEvent()`:

        snippet_shell.py com.google.android.mobly.snippet.example4

        >>> callback = s.scheduleRpc('makeToast', 5000, ['message'])

        Wait for the message to show up on the screen

        >>> callback.waitAndGet('makeToast').data
        {u'callback': u'null', u'error': u'null', u'result': u'OK', u'id': u'0'}

        >>> callback = s.scheduleRpc('asyncMakeToast', 5000, ['message'])

        Wait for the message to show up on the screen

        >>> callback.waitAndGet('asyncMakeToast').data
        {u'callback': u'1-1', u'error': u'null', u'result': u'null', u'id': u'0'}
