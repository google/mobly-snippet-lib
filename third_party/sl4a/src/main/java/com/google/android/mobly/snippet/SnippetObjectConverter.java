package com.google.android.mobly.snippet;

import java.lang.reflect.Type;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Interface for a converter that serializes and de-serializes objects.
 *
 * <p>Classes implementing this interface are meant to provide custom serialization/de-serialization
 * logic for complex types.
 *
 * <p>Serialization here means converting a Java object to {@link JSONObject}, which can be
 * transported over Snippet's Rpc protocol. De-serialization is this process in reverse.
 */
public interface SnippetObjectConverter {
    /**
     * Serializes a complex type object to {@link JSONObject}.
     *
     * <p>Return null to signify the complex type is not supported.
     *
     * @param object The object to convert to "serialize".
     * @return A JSONObject representation of the input object, or `null` if the input object type
     *     is not supported.
     * @throws JSONException
     */
    JSONObject serialize(Object object) throws JSONException;

    /**
     * Deserializes a {@link JSONObject} to a Java complex type object.
     *
     * @param jsonObject A {@link JSONObject} passed from the Rpc client.
     * @param type The expected {@link Type} of the Java object.
     * @return A Java object of the specified {@link Type}, or `null` if the {@link Type} is not
     *     supported.
     * @throws JSONException
     */
    Object deserialize(JSONObject jsonObject, Type type) throws JSONException;
}
