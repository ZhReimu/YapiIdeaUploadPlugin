package com.mrx.yapi.uploader.model;

import com.mrx.yapi.uploader.model.constants.Modifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 抽象的元素对象
 */
public interface IElement {
    /**
     * 获取该对象的访问等级, 不能为空
     */
    @NotNull
    List<Modifier> getModifiers();

    /**
     * 该元素的类型的全限定类名, 对于 method 等元素, 那就是 null, 可能为空
     */
    @Nullable
    default String getTypeName() {
        return null;
    }

    /**
     * 该元素的名称, 如果是字段, 那就是字段名, 如果是注解等非变量的对象, 那就是 null, 可以为空
     */
    @Nullable
    default String getName() {
        return null;
    }
}
