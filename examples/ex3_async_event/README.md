# Async Event Rpc Example

This example shows you how to use the @AsyncRpc of Mobly snippet lib
to handle asynchornous callbacks.

See the source code ExampleAsyncSnippet.java for details.

## Running the example code

This folder contains a fully working example of a standalone snippet apk.

1.  Compile the example

        ./gradlew examples:ex3_async_event:assembleDebug

1.  Install the apk on your phone

        adb install -r ./examples/ex3_async_event/build/outputs/apk/ex3_async_event-debug.apk

1.  Use `snippet_shell` from mobly to trigger `tryEvent()`:

        snippet_shell.py com.google.android.mobly.snippet.example3

        >>> eventHandler = s.tryEvent(42)
        >>> print("Not blocked, can do stuff here")
        >>> eventHandler.waitAndGet('ExampleEvent') // Blocks until the event is posted on the server side
        {'callbackId': '2-1', 'name': 'ExampleEvent', 'time': 20381008148, 'data': {'exampleData': "Here's a simple event.", 'secretNumber': 42, 'isSecretive': True, 'moreData': {'evenMoreData': 'More Data!'}}}
