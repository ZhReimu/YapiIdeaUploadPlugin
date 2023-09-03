package com.mrx.yapi.uploader.model;

import com.mrx.yapi.uploader.model.constants.Modifier;
import lombok.Data;

import java.util.List;

/**
 * class 对象
 */
@Data
public class XClass implements IElement {

    /**
     * 类的修饰符
     */
    private List<Modifier> modifiers;
    /**
     * class 的全限定类名
     */
    private String typeName;
    /**
     * class 的所有方法
     */
    private List<XMethod> methods;
    /**
     * class 的所有字段
     */
    private List<XField> fields;
    /**
     * 实际上的泛型
     */
    private List<XClass> genericTypes;
}
