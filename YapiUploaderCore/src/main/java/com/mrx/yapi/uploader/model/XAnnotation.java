package com.mrx.yapi.uploader.model;

import com.mrx.yapi.uploader.model.constants.Modifier;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 注解对象
 */
@Data
public class XAnnotation implements IElement {

    /**
     * 注解修饰符
     */
    private List<Modifier> modifiers;
    /**
     * 注解全限定类名
     */
    private String typeName;
    /**
     * 注解属性, key 为属性名, value 为 属性值
     */
    private Map<String, Object> properties;

}
