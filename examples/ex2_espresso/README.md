# Espresso Snippet Example

This tutorial shows you how to create snippets that automate the UI of another
app using Espresso.

The same approach can be used to create any snippet app that needs to access
the classes or resources of any other single app.

## Overview

To build a snippet that instruments another app, you have to create a new
[product flavor](https://developer.android.com/studio/build/build-variants.html#product-flavors)
of your existing app with the snippet code built in.

The snippet code cannot run from a regular test apk because it requires a custom
`testInstrumentationRunner`.

## Tutorial

1.  In the `build.gradle` file of your existing app, create a new product flavor called `snippet`.

    ```
    android {
      defaultConfig { ... }
      productFlavors {
        main {}
        snippet {}
      }
    }
    ```

1.  Link against Mobly Snippet Lib in your `build.gradle` file

    ```
    dependencies {
      snippetCompile 'com.google.android.mobly:mobly-snippet-lib:1.3.1'
    }
    ```

1.  Create a new source tree called `src/snippet` where you will place the
    snippet code.

1.  In Android Studio, use the `Build Variants` tab in the left hand pane to
    switch to the snippetDebug build variant. This will let you edit code in the
    new tree.

1.  Write your snippet code in a new class under `src/snippet/java`

    ```java
    package com.my.app;
    ...
    public class EspressoSnippet implements Snippet {
      @Rpc(description="Pushes the main app button.")
      public void pushMainButton() {
        onView(withId(R.id.main_button)).perform(click());
      }

      @Override
      public void shutdown() {}
    }
    ```

1.  Create `src/snippet/AndroidManifest.xml` containing an `<instrument>` block
    and any classes that implement the `Snippet` interface in `meta-data`

    ```xml
    <?xml version="1.0" encoding="utf-8"?>
    <manifest xmlns:android="http://schemas.android.com/apk/res/android">
      <application>
        <meta-data
          android:name="mobly-snippets"
          android:value="com.my.app.EspressoSnippet" />
      </application>

      <instrumentation
        android:name="com.google.android.mobly.snippet.SnippetRunner"
        android:targetPackage="com.my.app" />
    </manifest>
    ```

1.  Build your apk by invoking the new `assembleSnippetDebug` target.

1.  Install the apk on your phone. You do not need to install the main app's
    apk; the snippet-enabled apk is a complete replacement for your app.

1.  In your Mobly python test, connect to your snippet .apk in `setup_class`

    ```python
    class HelloWorldTest(base_test.BaseTestClass):
      def setup_class(self):
        self.ads = self.register_controller(android_device)
        self.dut1 = self.ads[0]
        self.dut1.load_snippet(name='snippet', package='com.my.app')
    ```

6.  Invoke your needed functionality within your test

    ```python
    def test_click_button(self):
      self.dut1.snippet.pushMainButton()
    ```

## Running the example code

This folder contains a fully working example of a snippet apk that uses espresso
to automate a simple app.

1.  Compile the example

        ./gradlew examples:ex2_espresso:assembleSnippetDebug

1.  Install the apk on your phone

        adb install -r ./examples/ex2_espresso/build/outputs/apk/snippet/debug/ex2_espresso-snippet-debug.apk

1.  Use `snippet_shell` from mobly to trigger `pushMainButton()`:

        snippet_shell.py com.google.android.mobly.snippet.example2

        >>> print(s.help())
        Known methods:
          pushMainButton(boolean) returns void  // Pushes the main app button, and checks the label if this is the first time.
          startMainActivity() returns void  // Opens the main activity of the app

        >>> s.startMainActivity()
        >>> s.pushMainButton(True)

1. Press ctrl+d to exit the shell and terminate the app.
