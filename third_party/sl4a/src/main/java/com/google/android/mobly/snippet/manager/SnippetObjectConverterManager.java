package com.google.android.mobly.snippet.manager;

import com.google.android.mobly.snippet.SnippetObjectConverter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Manager for classes that implement {@link SnippetObjectConverter}.
 *
 * <p>This class is created to separate how Snippet Lib handles object conversion internally from
 * how the conversion scheme for complex types is defined for users.
 *
 * <p>Snippet Lib can pull in the custom serializers and deserializers through here in various
 * stages of execution, whereas users can have a clean interface for supplying these methods without
 * worrying about internal states of Snippet Lib.
 *
 * <p>This gives us the flexibility of changing Snippet Lib internal structure or expanding support
 * without impacting users. E.g. we can support multiple converter classes in the future.
 */
public class SnippetObjectConverterManager {
    private static SnippetObjectConverter mConverter;
    private static volatile SnippetObjectConverterManager mManager;

    private SnippetObjectConverterManager() {}

    public static synchronized SnippetObjectConverterManager getInstance() {
        if (mManager == null) {
            mManager = new SnippetObjectConverterManager();
        }
        return mManager;
    }

    static void addConverter(Class<? extends SnippetObjectConverter> converterClass) {
        if (mConverter != null) {
            throw new RuntimeException("A converter has been added, cannot add again.");
        }
        try {
            mConverter = converterClass.getConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No default constructor found for the converter class.");
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    public Object objectToJson(Object object) throws JSONException {
        if (mConverter == null) {
            return null;
        }
        return mConverter.serialize(object);
    }

    public Object jsonToObject(JSONObject jsonObject, Type type) throws JSONException {
        if (mConverter == null) {
            return null;
        }
        return mConverter.deserialize(jsonObject, type);
    }
}
