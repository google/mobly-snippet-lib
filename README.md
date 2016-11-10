# Getting Started with Snippets for Mobly

Mobly Snippet Lib is a library for triggering device-side code from host-side
[Mobly](http://github.com/google/mobly) tests. This tutorial teaches you how to
use the snippet lib to trigger custom device-side actions.

This is not an official Google product.

## Prerequisites
-   This tutorial assumes basic familiarity with the Mobly framework, so please
    follow the [Mobly tutorial](http://github.com/google/mobly) before doing
    this one.
-   You should know how to create an Android app and build it with gradle. If
    not, follow the
    [Android app tutorial](https://developer.android.com/training/basics/firstapp/index.html).

## Using Mobly Snippet Lib

1.  Link against Mobly Snippet Lib in your `build.gradle` file

    ```
    apply plugin: 'com.android.application'
    dependencies {
      androidTestCompile 'com.google.android.mobly:snippetlib:0.0.1'
    }
    ```

2.  In your `androidTest` source tree, write a Java class implementing `Snippet`
    and add methods to trigger the behaviour that you want. Annotate them with
    `@Rpc`

    ```java
    package com.my.app.test;

    ...

    public class ExampleSnippet implements Snippet {
      public ExampleSnippet(Context context) {}

      @Rpc(description='Returns a string containing the given number.')
      public String getFoo(Integer input) {
        return 'foo ' + input;
      }

      @Override
      public void shutdown() {}
    }
    ```

3.  Add any classes that implement the `Snippet` interface in your
    `AndroidManifest.xml` application section as `meta-data`

    ```xml
    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
        package="com.my.app.test">
      <application>
        <meta-data
            android:name="mobly-snippets"
            android:value="com.my.app.test.MySnippet1,
                           com.my.app.test.MySnippet2" />
        ...
    ```

4.  Build your apk and install it on your phone

5.  In your Mobly python test, connect to your snippet .apk in `setup_class`

    ```python
    class HelloWorldTest(base_test.BaseTestClass):
      def setup_class(self):
        self.ads = self.register_controller(android_device)
        self.dut1 = self.ads[0]
        self.dut1.load_snippets(name='snippet', package='com.my.app.test')

      if __name__ == '__main__':
        test_runner.main()
    ```

6.  Invoke your needed functionality within your test

    ```python
    def test_get_foo(self):
      actual_foo = self.dut1.snippet.getFoo(5)
      asserts.assert_equal("foo 5", actual_foo)
    ```
