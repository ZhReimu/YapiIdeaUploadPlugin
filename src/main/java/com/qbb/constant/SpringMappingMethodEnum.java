package com.qbb.constant;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@Getter
public enum SpringMappingMethodEnum {

    GET(SpringMVCConstant.GetMapping, HttpMethodConstant.GET),
    PUT(SpringMVCConstant.PutMapping, HttpMethodConstant.PUT),
    POST(SpringMVCConstant.PostMapping, HttpMethodConstant.POST),
    DELETE(SpringMVCConstant.DeleteMapping, HttpMethodConstant.DELETE),
    PATCH(SpringMVCConstant.PatchMapping, HttpMethodConstant.PATCH),
    ;

    private final String springMapping;
    private final String httpMethod;

    SpringMappingMethodEnum(String springMapping, String httpMethod) {
        this.springMapping = springMapping;
        this.httpMethod = httpMethod;
    }

    @NotNull
    public static SpringMappingMethodEnum ofSpringMapping(@Nullable String springMapping) {
        return Arrays.stream(values()).filter(it -> it.springMapping.equals(springMapping))
                .findFirst().orElseThrow(IllegalArgumentException::new);
    }

}
