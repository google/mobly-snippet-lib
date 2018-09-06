# Getting Started with Snippets for Mobly

Mobly Snippet Lib is a library for triggering device-side code from host-side
[Mobly](http://github.com/google/mobly) tests. This tutorial teaches you how to
use the snippet lib to trigger custom device-side actions.

Note: Mobly and the snippet lib are not official Google products.


## Prerequisites

-   These examples and tutorials assume basic familiarity with the Mobly
    framework, so please follow the
    [Mobly tutorial](http://github.com/google/mobly) before doing this one.
-   You should know how to create an Android app and build it with gradle. If
    not, follow the
    [Android app tutorial](https://developer.android.com/training/basics/firstapp/index.html).


## Overview

The Mobly Snippet Lib allows you to write Java methods that run on Android
devices, and trigger the methods from inside a Mobly test case. The Java methods
invoked this way are called `snippets`.

The `snippet` code can either be written in its own standalone apk, or as a
[product flavor](https://developer.android.com/studio/build/build-variants.html#product-flavors)
of an existing apk. This allows you to write snippets that instrument or
automate another app.


## Under The Hood

A snippet is launched by an `am instrument` call. Snippets use a custom
`InstrumentationTestRunner` derived from `AndroidJUnitRunner`. This allows
for snippets that interact with a main app's classes, such as Espresso snippets,
and allows you to get either the test app's or the main app's context from
`InstrumentationRegistry`.

Once started, the special runner starts a web server which listens for requests
to trigger snippets. The server's handler locates the corresponding methods by
reflection, runs them, and returns results over the TCP socket. All common
built-in variable types are supported as arguments.


## Usage

The [examples/](examples/) folder contains examples of how to use the
mobly snippet lib along with detailed tutorials.

*   [ex1_standalone_app](examples/ex1_standalone_app): Basic example of a
    snippet which is compiled into its own standalone apk.
*   [ex2_espresso](examples/ex2_espresso): Example of a snippet which
    instruments a primary app to drive its UI using
    [Espresso](https://google.github.io/android-testing-support-library/docs/espresso/).
*   [ex3_async_event](examples/ex3_async_event): Example of how to use the
    @AsyncRpc annotation to handle asynchronous callbacks.
*   [ex4_uiautomator](examples/ex4_uiautomator): Example of how to create
    snippets that automate the UI actions using UIAutomator. Unlike Espresso
    UIAutomator works even without access to app source code.
*   [ex5_schedule_rpc](examples/ex5_schedule_rpc): Example of how to use the
    'scheduleRpc' RPC to execute another RPC at a later time, potentially after
    device disconnection.
*   [ex6_complex_type_conversion](examples/ex6_complex_type_conversion): Example of how to pass a
    non-primitive type to the Rpc methods and return non-primitive type from Rpc methods by
    supplying a type converter.
