# Standalone Snippet App Example

This tutorial shows you how to create a standalone Mobly snippet app. To create
a snippet app that controls (instruments) another app under test, please see
[Example 2](../ex2_espresso/README.md).

## Tutorial

1.  Use Android Studio to create a new app project.

1.  Link against Mobly Snippet Lib in your `build.gradle` file

    ```
    dependencies {
      implementation 'com.google.android.mobly:mobly-snippet-lib:1.3.1'
    }
    ```

1.  Write a Java class implementing `Snippet` and add methods to trigger the
    behaviour that you want. Annotate them with `@Rpc`

    ```java
    package com.my.app;
    ...
    public class ExampleSnippet implements Snippet {
      @Rpc(description='Returns a string containing the given number.')
      public String getFoo(Integer input) {
        return "foo " + input;
      }

      @Override
      public void shutdown() {}
    }
    ```

1.  Add any classes that implement the `Snippet` interface in your
    `AndroidManifest.xml` application section as `meta-data`

    ```xml
    <manifest
        xmlns:android="http://schemas.android.com/apk/res/android"
        package="com.my.app">
      <application>
        <meta-data
            android:name="mobly-snippets"
            android:value="com.my.app.test.MySnippet1,
                           com.my.app.test.MySnippet2" />
        ...
    ```


1.  Add an `instrumentation` tag to your `AndroidManifest.xml` so that the
    framework can launch your server through an `instrument` command.

    ```xml
    <manifest
        xmlns:android="http://schemas.android.com/apk/res/android"
        package="com.my.app">
      <application>...</application>
      <instrumentation
          android:name="com.google.android.mobly.snippet.ServerRunner"
          android:targetPackage="com.my.app" />
    </manifest>
    ```

1.  Build your apk and install it on your phone

1.  In your Mobly python test, connect to your snippet .apk in `setup_class`

    ```python
    class HelloWorldTest(base_test.BaseTestClass):
      def setup_class(self):
        self.ads = self.register_controller(android_device)
        self.dut1 = self.ads[0]
        self.dut1.load_snippet(name='snippet', package='com.my.app.test')
    ```

6.  Invoke your needed functionality within your test

    ```python
    def test_get_foo(self):
      actual_foo = self.dut1.snippet.getFoo(5)
      asserts.assert_equal("foo 5", actual_foo)
    ```

## Running the example code

This folder contains a fully working example of a standalone snippet apk.

1.  Compile the example

        ./gradlew examples:ex1_standalone_app:assembleDebug

1.  Install the apk on your phone

        adb install -r ./examples/ex1_standalone_app/build/outputs/apk/debug/ex1_standalone_app-debug.apk

1.  Use `snippet_shell` from mobly to trigger `getFoo()`:

        snippet_shell.py com.google.android.mobly.snippet.example1

        >>> print(s.help())
        Known methods:
          getBar(String) returns String  // Returns the given string with the prefix "bar"
          getFoo(Integer) returns String  // Returns the given integer with the prefix "foo"

        >>> s.getFoo(5)
        u'foo 5'
