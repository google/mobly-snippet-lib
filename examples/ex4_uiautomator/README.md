# UIAutomator Snippet Example

This tutorial shows you how to create snippets that automate the UI of another app using UIAutomator.

## Tutorial

1.  Set up your build rules following the examples in the
    [Espresso snippet tutorial](../ex2_espresso/README.md).

1.  Instead of Espresso automation code, write UIAutomator code:

    ```java
    package com.my.app;
    ...
    public class UiAutomatorSnippet implements Snippet {
      @Rpc(description="Clicks the main app button")
      public void clickButton() {
        UiObject2 button = mDevice.findObject(By.text("PUSH THE BUTTON!"));
        button.click();
      }

      @Override
      public void shutdown() {}
    }
    ```

1.  Follow the rest of the
    [Espresso snippet tutorial](../ex2_espresso/README.md) to compile and
    launch your snippet.


## Running the example code

This folder contains a fully working example of a snippet apk that uses
UIAutomator to automate a simple app.

1.  Compile the example

        ./gradlew examples:ex4_uiautomator:assembleSnippetDebug

1.  Install the apk on your phone

        adb install -r ./examples/ex4_uiautomator/build/outputs/apk/ex4_uiautomator-snippet-debug.apk

1.  Use `snippet_shell` from mobly to trigger `click()`:

        snippet_shell.py com.google.android.mobly.snippet.example4

        >>> print(s.help())
        Known methods:
          click() returns void  // Clicks the button
          firstClick() returns void  // Clicks the button for the first time and checks the label change
          startMainActivity() returns void  // Opens the main activity of the app

        >>> s.startMainActivity()
        >>> s.click()
