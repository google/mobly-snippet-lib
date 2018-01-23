# Async Event RPC Example

This example shows you how to use the @AsyncRpc annotation of Mobly snippet lib
to handle asynchronous callbacks.

See the source code in `ExampleAsyncSnippet.java` for details.

## Running the example code

This folder contains a fully working example of a standalone snippet apk.

1.  Compile the example

        ./gradlew examples:ex3_async_event:assembleDebug

1.  Install the apk on your phone

        adb install -r ./examples/ex3_async_event/build/outputs/apk/debug/ex3_async_event-debug.apk

1.  Use `snippet_shell` from mobly to trigger `tryEvent()`:

        snippet_shell.py com.google.android.mobly.snippet.example3

        >>> handler = s.tryEvent(42)
        >>> print("Not blocked, can do stuff here")
        >>> event = handler.waitAndGet('AsyncTaskResult') # Blocks until the event is received

        Now let's see the content of the event

        >>> import pprint
        >>> pprint.pprint(event)
        {
            'callbackId': '2-1',
            'name': 'AsyncTaskResult',
            'time': 20460228696,
            'data': {
                'exampleData': "Here's a simple event.",
                'successful': True,
                'secretNumber': 12
            }
        }
