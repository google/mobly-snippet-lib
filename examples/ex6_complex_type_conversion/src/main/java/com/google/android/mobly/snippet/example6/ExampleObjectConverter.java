package com.google.android.mobly.snippet.example6;

import com.google.android.mobly.snippet.SnippetObjectConverter;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;


/**
 * Example showing how to supply custom object converter to Mobly Snippet Lib.
 */

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
