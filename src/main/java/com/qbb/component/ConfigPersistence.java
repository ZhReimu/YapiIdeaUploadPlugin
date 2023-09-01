package com.qbb.component;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.qbb.dto.ConfigDTO;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 配置持久化
 *
 * @author zhangyunfan
 * @version 1.0
 * @since 2020/12/25
 */
@Getter
@State(name = "yapiUploads", storages = {@Storage("yapiUploads.xml")})
public class ConfigPersistence implements PersistentStateComponent<List<ConfigDTO>> {

    private List<ConfigDTO> configs;

    private static final ConfigPersistence EMPTY = new ConfigPersistence();

    /**
     * 获取该持久化类的实例, 永不为空
     */
    @NotNull
    public static ConfigPersistence getInstance() {
        ConfigPersistence service = ServiceManager.getService(ConfigPersistence.class);
        return service == null ? EMPTY : service;
    }

    public void setConfigs(List<ConfigDTO> configs) {
        this.configs = configs;
    }

    @Nullable
    @Override
    public List<ConfigDTO> getState() {
        return this.configs;
    }

    @Override
    public void loadState(@NotNull List<ConfigDTO> element) {
        this.configs = element;
    }
}
