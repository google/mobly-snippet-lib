/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.mobly.snippet.example6;

import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.rpc.Rpc;
import com.google.android.mobly.snippet.util.Log;

import java.util.ArrayList;

/**
 * Example snippet showing converting complex type objects using custom logic.
 *
 * For complex types in Java, one can supply a custom object converter to Snippet Lib to specify how
 * each complex type should be serialized/de-serialized. With this, users don't have to explicitly
 * call a serializer or de-serializer in every single Rpc method, which simplifies code.
 */
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

    /**
     * Demonstrates serialization/de-serialization of a collection of custom type objects.
     */
    @Rpc(description = "Update values for multiple CustomType objects.")
    public ArrayList<CustomType> updateValues(ArrayList<CustomType> objects, String newValue) {
        for (CustomType obj : objects) {
            obj.setMyValue(newValue);
        }
        return objects;
    }

    @Override
    public void shutdown() {}
}
