# Complex Type Conversion in Snippet Example

This tutorial shows you how to use a custom object in Snippet Lib.

This example assumes basic familiarity with Snippet Lib as demonstrated in
[Example 1](../ex1_standalone_app/README.md).

## Tutorial

1.  Use Android Studio to create a new app project, similar to
    [Example 1](../ex1_standalone_app/README.md).

1.  Create a complex type in Java:
    ```java
    public class CustomType {
        private String myValue;
        CustomType(String value) {
            myValue = value;
        }
    
        String getMyValue() {
            return myValue;
        }
        public void setMyValue(String newValue) {
            myValue = newValue;
        }
    }
    ```
1.  Create a Java class implementing `SnippetObjectConverter`, which defines how the complex type
    should be converted against `JSONObject`:
    ```java
    public class ExampleObjectConverter implements SnippetObjectConverter {
        @Override
        public JSONObject serialize(Object object) throws JSONException {
            JSONObject result = new JSONObject();
            if (object instanceof CustomType) {
                CustomType input = (CustomType) object;
                result.put("Value", input.getMyValue());
                return result;
            }
            return null;
        }
    
        @Override
        public Object deserialize(JSONObject jsonObject, Type type) throws JSONException {
            if (type == CustomType.class) {
                return new CustomType(jsonObject.getString("Value"));
            }
            return null;
        }
    }
    ```
1.  Write a Java class implementing `Snippet` and add Rpc methods that takes your complex type as
    a parameter and another Rpc method that returns the complext type directly.

    ```java
    package com.my.app;
    ...
    public class ExampleSnippet implements Snippet {
        @Rpc(description = "Pass a complex type as a snippet parameter.")
        public String passComplexTypeToSnippet(CustomType input) {
            Log.i("Old value is: " + input.getMyValue());
            return "The value in CustomType is: " + input.getMyValue();
        }
      
        @Rpc(description = "Returns a complex type from snippet.")
        public CustomType returnComplexTypeFromSnippet(String value) {
            return new CustomType(value);
        }
        @Override
        public void shutdown() {}
    }
    ```

1.  In `AndroidManifest.xml`, specify the converter class as a `meta-data` named
    `mobly-object-converter`

    ```xml
    <manifest
        xmlns:android="http://schemas.android.com/apk/res/android"
        package="com.my.app">
      <application>
        <meta-data
            android:name="mobly-object-converter"
            android:value="com.my.app.ExampleObjectConverter" />
        ...
    ```

## Running the example code

This folder contains a fully working example of a standalone snippet apk.

1.  Compile the example

        ./gradlew examples:ex6_complex_type_conversion:assembleDebug

1.  Install the apk on your phone

        adb install -r ./examples/ex6_complex_type_conversion/build/outputs/apk/debug/ex6_complex_type_conversion-debug.apk

1.  Use Mobly's `snippet_shell` from mobly to trigger the Rpc methods:

        snippet_shell.py com.google.android.mobly.snippet.example6

        >>> s.passComplexTypeToSnippet({'Value': 'Hello'})
        'The value in CustomType is: Hello'
        >>> s.returnComplexTypeFromSnippet('Bye')
        {'Value': 'Bye'}
