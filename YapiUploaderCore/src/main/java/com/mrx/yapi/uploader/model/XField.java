package com.mrx.yapi.uploader.model;

import com.mrx.yapi.uploader.model.constants.Modifier;
import lombok.Data;

import java.util.List;

/**
 * 字段对象
 */
@Data
public class XField implements IElement {

    /**
     * 字段修饰符
     */
    private List<Modifier> modifiers;
    /**
     * 字段类型全限定名
     */
    private String typeName;
    /**
     * 字段名
     */
    private String name;
    /**
     * 字段上的注解
     */
    private List<XAnnotation> annotations;
}
