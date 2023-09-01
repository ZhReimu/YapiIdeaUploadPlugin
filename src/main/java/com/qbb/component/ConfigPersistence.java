package com.qbb.component;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.qbb.dto.ConfigDTO;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置持久化
 *
 * @author zhangyunfan
 * @version 1.0
 * @since 2020/12/25
 */
@State(name = "yapiUploads", storages = {@Storage("$APP_CONFIG$/yapiUploads.xml")})
public class ConfigPersistence implements PersistentStateComponent<ConfigPersistence.ConfigState> {

    private final ConfigState state = new ConfigState();

    public List<ConfigDTO> getConfigs() {
        return state.configs;
    }

    @Data
    public static class ConfigState {
        private List<ConfigDTO> configs = new ArrayList<>();
    }

    /**
     * 获取该持久化类的实例, 永不为空
     */
    @NotNull
    public static ConfigPersistence getInstance() {
        return ServiceManager.getService(ConfigPersistence.class);
    }

    public void setConfigs(List<ConfigDTO> configs) {
        state.configs = configs;
    }

    @Override
    public @Nullable ConfigPersistence.ConfigState getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull ConfigState state) {
        this.state.configs = state.configs;
    }

}
