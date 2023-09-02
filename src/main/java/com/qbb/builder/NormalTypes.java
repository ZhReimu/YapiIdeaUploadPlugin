package com.qbb.builder;

import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 基本类
 *
 * @author chengsheng@qbb6.com
 * @since 2019/1/30 9:58 AM
 */
public class NormalTypes {

    @NonNls
    private static final Map<String, String> normalTypes = new HashMap<>();

    public static final Map<String, Object> noramlTypesPackages = new HashMap<>();

    public static final Map<String, Object> collectTypes = new HashMap<>();

    public static final Map<String, Object> collectTypesPackages = new HashMap<>();

    public static final Map<String, String> java2JsonTypes = new HashMap<>();
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
        collectTypes.put("HashMap", "HashMap");
        collectTypes.put("Map", "Map");
        collectTypes.put("LinkedHashMap", "LinkedHashMap");

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
        noramlTypesPackages.put("int", 1);
        noramlTypesPackages.put("boolean", true);
        noramlTypesPackages.put("byte", 1);
        noramlTypesPackages.put("short", 1);
        noramlTypesPackages.put("long", 1L);
        noramlTypesPackages.put("float", 1.0F);
        noramlTypesPackages.put("double", 1.0D);
        noramlTypesPackages.put("char", 'a');
        noramlTypesPackages.put("java.lang.Boolean", false);
        noramlTypesPackages.put("java.lang.Byte", 0);
        noramlTypesPackages.put("java.lang.Short", (short) 0);
        noramlTypesPackages.put("java.lang.Integer", 1);
        noramlTypesPackages.put("java.lang.Long", 1L);
        noramlTypesPackages.put("java.lang.Float", 1L);
        noramlTypesPackages.put("java.lang.Double", 1.0D);
        noramlTypesPackages.put("java.sql.Timestamp", new Timestamp(System.currentTimeMillis()));
        noramlTypesPackages.put("java.util.Date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        noramlTypesPackages.put("java.lang.String", "String");
        noramlTypesPackages.put("java.math.BigDecimal", 1);
        noramlTypesPackages.put("java.time.LocalDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        noramlTypesPackages.put("java.time.LocalTime", new SimpleDateFormat("HH:mm:ss").format(new Date()));
        noramlTypesPackages.put("java.time.LocalDateTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        collectTypesPackages.put("java.util.LinkedHashMap", "LinkedHashMap");
        collectTypesPackages.put("java.util.HashMap", "HashMap");
        collectTypesPackages.put("java.util.Map", "Map");
    }

    public static boolean isNormalType(String typeName) {
        return normalTypes.containsKey(typeName);
    }

    @Nullable
    public static String getNormalType(String typeName) {
        return normalTypes.get(typeName);
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
