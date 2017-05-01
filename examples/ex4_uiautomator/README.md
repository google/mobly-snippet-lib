# UIAutomator Snippet Example

This example shows you how to create snippets that automate the UI of another
app using UIAutomator.

See the [Espresso snippet tutorial](../ex2_espresso/README.md) for more
information about the app this example automates. In this example we are
automating it without access to its source or classpath.

## Running the example code

This folder contains a fully working example of a snippet apk that uses
UIAutomator to automate a simple app.

1.  Compile the main app and automation

        ./gradlew examples:ex2_espresso:assembleDebug examples:ex4_uiautomator:assembleDebug

1.  Install the apks on your phone

        adb install -r ./examples/ex2_espresso/build/outputs/apk/ex2_espresso-main-debug.apk
        adb install -r ./examples/ex4_uiautomator/build/outputs/apk/ex4_uiautomator-debug.apk

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
