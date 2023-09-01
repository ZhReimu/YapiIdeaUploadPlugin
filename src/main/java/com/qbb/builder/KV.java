package com.qbb.builder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author chengsheng@qbb6.com
 * @since 2018/10/27
 */
public class KV<K, V> extends LinkedHashMap<K, V> {

    @SuppressWarnings("unchecked")
    public static <K, V> KV<K, V> by(K key, V value) {
        return (KV<K, V>) create().set(key, value);
    }

    public static <K, V> KV<K, V> create() {
        return new KV<>();
    }

    public KV<K, V> set(K key, V value) {
        super.put(key, value);
        return this;
    }

    public KV<K, V> set(Map<K, V> map) {
        super.putAll(map);
        return this;
    }

    public KV<K, V> set(KV<K, V> kv) {
        super.putAll(kv);
        return this;
    }

    public KV<K, V> delete(Object key) {
        super.remove(key);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAs(Object key) {
        return (T) get(key);
    }

    public String getStr(Object key) {
        return (String) get(key);
    }

    public Integer getInt(Object key) {
        return (Integer) get(key);
    }

    public Long getLong(Object key) {
        return (Long) get(key);
    }

    public Boolean getBoolean(Object key) {
        return (Boolean) get(key);
    }

    public Float getFloat(Object key) {
        return (Float) get(key);
    }

    /**
     * key 存在，并且 value 不为 null
     */
    public boolean notNull(Object key) {
        return get(key) != null;
    }

    /**
     * key 不存在，或者 key 存在但 value 为null
     */
    public boolean isNull(Object key) {
        return get(key) == null;
    }

    /**
     * key 存在，并且 value 为 true，则返回 true
     */
    public boolean isTrue(Object key) {
        Object value = get(key);
        return (value instanceof Boolean && ((Boolean) value));
    }

    /**
     * key 存在，并且 value 为 false，则返回 true
     */
    public boolean isFalse(Object key) {
        Object value = get(key);
        return (value instanceof Boolean && (!((Boolean) value)));
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public String toPrettyJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }

    public boolean equals(Object KV) {
        return KV instanceof KV && super.equals(KV);
    }
    
}
