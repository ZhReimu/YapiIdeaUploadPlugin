package com.mrx.yapi.uploader.model;

import com.mrx.yapi.uploader.model.constants.Modifier;
import lombok.Data;

import java.util.List;

/**
 * 参数对象
 */
@Data
public class XParameter implements IElement {
    /**
     * 参数修饰符
     */
    private List<Modifier> modifiers;
    /**
     * 参数注解
     */
    private List<XAnnotation> annotations;
    /**
     * 参数类型全限定名
     */
    private XClass typeName;
    /**
     * 参数名
     */
    private String name;
}
