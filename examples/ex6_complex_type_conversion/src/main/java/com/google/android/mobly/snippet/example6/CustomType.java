package com.google.android.mobly.snippet.example6;

/**
 * A data class that defines a non-primitive type.
 *
 * This type is used to demonstrate serialization and de-serialization of complex type objects in
 * Mobly Snippet Lib for Android.
 */
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
