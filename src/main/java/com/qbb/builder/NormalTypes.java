package com.qbb.builder;

import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 基本类
 *
 * @author chengsheng@qbb6.com
 * @since 2019/1/30 9:58 AM
 */
public class NormalTypes {

    private static final Map<String, String> normalTypes = new HashMap<>();

    private static final Map<String, String> normalTypePackages = new HashMap<>();

    private static final Map<String, String> collectionTypes = new HashMap<>();

    private static final Map<String, String> collectionTypePackages = new HashMap<>();

    private static final Map<String, String> java2JsonTypes = new HashMap<>();
    /**
     * 泛型列表
     */
    public static final List<String> genericList = new ArrayList<>();

    static {
        normalTypes.put("int", "1");
        normalTypes.put("boolean", "false");
        normalTypes.put("byte", "1");
        normalTypes.put("short", "1");
        normalTypes.put("long", "1");
        normalTypes.put("float", "1.0");
        normalTypes.put("double", "1.0");
        normalTypes.put("char", "a");
        normalTypes.put("Boolean", "false");
        normalTypes.put("Byte", "0");
        normalTypes.put("Short", "0");
        normalTypes.put("Integer", "0");
        normalTypes.put("Long", "0");
        normalTypes.put("Float", "0.0");
        normalTypes.put("Double", "0.0");
        normalTypes.put("String", "String");
        normalTypes.put("Date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        normalTypes.put("BigDecimal", "0.111111");
        normalTypes.put("LocalDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        normalTypes.put("LocalTime", new SimpleDateFormat("HH:mm:ss").format(new Date()));
        normalTypes.put("LocalDateTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        normalTypes.put("Timestamp", String.valueOf(System.currentTimeMillis()));

        collectionTypes.put("HashMap", "HashMap");
        collectionTypes.put("Map", "Map");
        collectionTypes.put("LinkedHashMap", "LinkedHashMap");

        genericList.add("T");
        genericList.add("E");
        genericList.add("A");
        genericList.add("B");
        genericList.add("K");
        genericList.add("V");

        java2JsonTypes.put("int", "number");
        java2JsonTypes.put("byte", "number");
        java2JsonTypes.put("short", "number");
        java2JsonTypes.put("long", "number");
        java2JsonTypes.put("float", "number");
        java2JsonTypes.put("double", "number");
        java2JsonTypes.put("Byte", "number");
        java2JsonTypes.put("Short", "number");
        java2JsonTypes.put("Integer", "number");
        java2JsonTypes.put("Long", "number");
        java2JsonTypes.put("Float", "number");
        java2JsonTypes.put("Double", "number");
        java2JsonTypes.put("BigDecimal", "number");
        java2JsonTypes.put("Timestamp", "number");
        java2JsonTypes.put("char", "string");
        java2JsonTypes.put("String", "string");
        java2JsonTypes.put("Date", "string");
        java2JsonTypes.put("LocalDate", "string");
        java2JsonTypes.put("LocalDateTime", "string");
        java2JsonTypes.put("Boolean", "boolean");
    }

    static {
        normalTypePackages.put("int", "1");
        normalTypePackages.put("boolean", "true");
        normalTypePackages.put("byte", "1");
        normalTypePackages.put("short", "1");
        normalTypePackages.put("long", "1");
        normalTypePackages.put("float", "1.0");
        normalTypePackages.put("double", "1.0");
        normalTypePackages.put("char", "a");
        normalTypePackages.put("java.lang.Boolean", "false");
        normalTypePackages.put("java.lang.Byte", "0");
        normalTypePackages.put("java.lang.Short", "0");
        normalTypePackages.put("java.lang.Integer", "1");
        normalTypePackages.put("java.lang.Long", "1");
        normalTypePackages.put("java.lang.Float", "1");
        normalTypePackages.put("java.lang.Double", "1.0");
        normalTypePackages.put("java.sql.Timestamp", String.valueOf(System.currentTimeMillis()));
        normalTypePackages.put("java.util.Date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        normalTypePackages.put("java.lang.String", "String");
        normalTypePackages.put("java.math.BigDecimal", "1");
        normalTypePackages.put("java.time.LocalDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        normalTypePackages.put("java.time.LocalTime", new SimpleDateFormat("HH:mm:ss").format(new Date()));
        normalTypePackages.put("java.time.LocalDateTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        collectionTypePackages.put("java.util.LinkedHashMap", "LinkedHashMap");
        collectionTypePackages.put("java.util.HashMap", "HashMap");
        collectionTypePackages.put("java.util.Map", "Map");
    }

    public static boolean isNormalType(String typeName) {
        return normalTypes.containsKey(typeName) || normalTypePackages.containsKey(typeName);
    }

    @Nullable
    public static String getNormalType(String typeName) {
        return normalTypes.getOrDefault(typeName, normalTypePackages.get(typeName));
    }

    public static boolean isCollectionType(String typeName) {
        return collectionTypes.containsKey(typeName) || collectionTypePackages.containsKey(typeName);
    }

    @Nullable
    public static String getCollectionType(String typeName) {
        return collectionTypes.getOrDefault(typeName, collectionTypePackages.get(typeName));
    }

    @NotNull
    public static String java2JsonType(String typeName) {
        return java2JsonTypes.getOrDefault(typeName, typeName);
    }

    /**
     * mock type
     */
    public static JsonObject formatMockType(String type, String exampleMock) {
        JsonObject mock = new JsonObject();
        // 支持传入自定义 mock
        if (StringUtils.isNotEmpty(exampleMock)) {
            mock.addProperty("mock", exampleMock);
            return mock;
        }
        switch (type) {
            case "int":
            case "short":
            case "long":
            case "Short":
            case "Integer":
            case "Long":
            case "java.lang.Short":
            case "java.lang.Integer":
            case "java.lang.Long":
                mock.addProperty("mock", "@integer");
                break;
            case "boolean":
            case "Boolean":
            case "java.lang.Boolean":
                mock.addProperty("mock", "@boolean");
                break;
            case "byte":
            case "Byte":
            case "java.lang.Byte":
                mock.addProperty("mock", "@byte");
                break;
            case "float":
            case "double":
            case "Float":
            case "Double":
            case "BigDecimal":
            case "java.lang.Float":
            case "java.lang.Double":
            case "java.math.BigDecimal":
                mock.addProperty("mock", "@float");
                break;
            case "char":
                mock.addProperty("mock", "@char");
                break;
            case "String":
            case "java.lang.String":
                mock.addProperty("mock", "@string");
                break;
            case "Date":
            case "LocalDate":
            case "LocalTime":
            case "LocalDateTime":
            case "Timestamp":
            case "java.sql.Timestamp":
            case "java.util.Date":
            case "java.time.LocalDate":
            case "java.time.LocalTime":
            case "java.time.LocalDateTime":
                mock.addProperty("mock", "@timestamp");
                break;
            default:
                mock.addProperty("mock", "mock");
                break;
        }
        return mock;
    }

}
