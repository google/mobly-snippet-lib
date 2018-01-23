# UIAutomator Snippet Example

This example shows you how to create snippets that control the UI of a device
across system and multiple app views using UIAutomator. Unlike Espresso-based
UI automation, it does not require access to app source code.

This snippet is written as a [standalone snippet](../ex1_standalone_app/README.md)
and does not target another app. In particular, it doesn't need to target the
app under test, so it doesn't need its classpath or to be signed with the same
key.

See the [Espresso snippet tutorial](../ex2_espresso/README.md) for more
information about the app this example automates.

## Running the example code

This folder contains a fully working example of a snippet apk that uses
UIAutomator to automate a simple app.

1.  Compile the main app and automation. The main app of ex2 (espresso) is used
    as the app to automate. Unlike espresso, the uiautomator test does not
    depend on this apk and does not use its source or classpath, so you must
    compile and install the app separately.

        ./gradlew examples:ex2_espresso:assembleDebug examples:ex4_uiautomator:assembleDebug

1.  Install the apks on your phone

        adb install -r ./examples/ex2_espresso/build/outputs/apk/debug/ex2_espresso-main-debug.apk
        adb install -r ./examples/ex4_uiautomator/build/outputs/apk/debug/ex4_uiautomator-debug.apk

1.  Use `snippet_shell` from mobly to trigger `pushMainButton()`:

        snippet_shell.py com.google.android.mobly.snippet.example4

        >>> print(s.help())
        Known methods:
          pushMainButton(boolean) returns void  // Pushes the main app button, and checks the label if this is the first time.
          startMainActivity() returns void  // Opens the main activity of the app
          uiautomatorDump() returns String  // Perform a UIAutomator dump

        >>> s.startMainActivity()
        >>> s.pushMainButton(True)

1. Press ctrl+d to exit the shell and terminate the app.
