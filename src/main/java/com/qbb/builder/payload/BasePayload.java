package com.qbb.builder.payload;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qbb.builder.KV;

public class BasePayload {

    protected static final Gson gson = new Gson();

    protected static <T> T copy(Class<T> clazz, KV<?, ?> kv) {
        return gson.fromJson(gson.toJson(kv), clazz);
    }

    protected static <T> T copy(KV<?, ?> kv, TypeToken<T> typeToken) {
        return gson.fromJson(gson.toJson(kv), typeToken.getType());
    }

    @SuppressWarnings("unchecked")
    public KV<String, Object> toKV() {
        return gson.fromJson(gson.toJson(this), KV.class);
    }

}
