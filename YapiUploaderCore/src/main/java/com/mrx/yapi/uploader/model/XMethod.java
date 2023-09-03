package com.mrx.yapi.uploader.model;

import com.mrx.yapi.uploader.model.constants.Modifier;
import lombok.Data;

import java.util.List;

/**
 * 方法对象
 */
@Data
public class XMethod implements IElement {
    /**
     * 方法修饰符
     */
    private List<Modifier> modifiers;
    /**
     * 方法名
     */
    private String name;
    /**
     * 方法入参
     */
    private List<XParameter> parameters;
    /**
     * 方法返回值
     */
    private XClass result;
}
