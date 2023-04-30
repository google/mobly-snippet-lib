# Default and Optional RPCs Example

This example shows you how to use `RpcDefault` and `RpcOptional` which is built
into Mobly snippet lib to annotate RPC's parameters.

## Why this is needed?

These annotations can be used to specify the default and optional parameters for
RPC methods, which allows developers to create more flexible and reusable RPC
methods.

Here are some additional benefits of using `RpcDefault` and `RpcOptional`:

  - Improve the readability and maintainability of RPC methods.
  - Prevent errors caused by missing or invalid parameters.
  - Make it easier to test RPC methods.

See the source code ExampleDefaultAndOptionalRpcSnippet.java for details.

## Running the example code

This folder contains a fully working example of a standalone snippet apk.

1.  Compile the example

        ./gradlew examples:ex7_default_and_optional_rpc:assembleDebug

1.  Install the apk on your phone

        adb install -r ./examples/ex7_default_and_optional_rpc/build/outputs/apk/debug/ex7_default_and_optional_rpc-debug.apk

1.  Use `snippet_shell` from mobly to trigger `makeToast()`:

        snippet_shell.py com.google.android.mobly.snippet.example7

        >>> s.makeToast('Hello')

        Wait for `Hello, bool:true` message to show up on the screen. Here we
        didn't provide a Boolean to the RPC, so a default value, true, is used.

        >>> s.makeToast('Hello', False)

        Wait for `Hello, bool:false` message to show up on the screen. Here we
        provide a Boolean to the RPC, so the value is used instead of using
        default value.

        >>> s.makeToast('Hello', False, 1)

        Wait for `Hello, bool:false, number: 1` message to show up on the
        screen. The number is an optional parameter, it only shows up when we
        pass a value to the RPC.
