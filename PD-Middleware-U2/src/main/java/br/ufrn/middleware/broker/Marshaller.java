package br.ufrn.middleware.broker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.lang.reflect.Type;

/**
 * Marshaller (Basic Remoting Pattern). Converts wire payloads (JSON strings,
 * query/path strings) to/from Java types using Gson. Centralises serialization
 * so other classes never touch Gson directly.
 */
public final class Marshaller {
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    public String toJson(Object value) {
        if (value == null) return "null";
        return gson.toJson(value);
    }

    public <T> T fromJson(String json, Type type) {
        if (json == null || json.isBlank()) return null;
        return gson.fromJson(json, type);
    }

    public JsonObject parseBodyAsObject(String body) {
        if (body == null || body.isBlank()) return new JsonObject();
        JsonElement el = JsonParser.parseString(body);
        if (!el.isJsonObject()) {
            throw RemotingException.badRequest("Expected JSON object body");
        }
        return el.getAsJsonObject();
    }

    public Object convertString(String raw, Type targetType) {
        if (raw == null) return null;
        if (targetType == String.class) return raw;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(raw);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(raw);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(raw);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(raw);
        if (targetType == float.class || targetType == Float.class) return Float.parseFloat(raw);
        return gson.fromJson(raw, targetType);
    }

    public Object convertJson(JsonElement el, Type targetType) {
        if (el == null || el.isJsonNull()) return null;
        return gson.fromJson(el, targetType);
    }
}
