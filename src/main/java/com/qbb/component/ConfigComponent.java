package com.qbb.component;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.qbb.dto.ConfigDTO;
import com.qbb.util.XUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 配置界面
 *
 * @author zhangyunfan
 * @version 1.0
 * @since 2020/12/25
 */
public class ConfigComponent implements SearchableConfigurable {

    private final ConfigPersistence configPersistence = ConfigPersistence.getInstance();
    private JBList<ConfigDTO> list;
    private final DefaultListModel<ConfigDTO> defaultModelList = new DefaultListModel<>();

    @NotNull
    @Override
    public String getId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    @Override
    @Nls(capitalization = Nls.Capitalization.Title)
    public String getDisplayName() {
        return "YapiUpload";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        final List<ConfigDTO> configDTOS = configPersistence.getConfigs();
        for (int i = 0, len = configDTOS == null ? 0 : configDTOS.size(); i < len; i++) {
            defaultModelList.addElement(configDTOS.get(i));
        }
        list = new JBList<>(defaultModelList);
        list.setLayout(new BorderLayout());
        list.setCellRenderer(new ItemComponent());

        // 工具栏
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(list);
        decorator.setPreferredSize(new Dimension(0, 300));
        // 新增
        decorator.setAddAction(this::addAction);
        // 编辑
        decorator.setEditAction(this::editAction);

        return decorator.createPanel();
    }

    /**
     * 编辑配置
     */
    @SuppressWarnings("DialogTitleCapitalization")
    private void editAction(AnActionButton button) {
        int index = list.getSelectedIndex();
        final Project project = ProjectUtil.guessCurrentProject(list);
        ItemAddEditDialog itemAddEditDialog = new ItemAddEditDialog(defaultModelList.get(index), project);
        if (!itemAddEditDialog.showAndGet()) {
            return;
        }
        final ConfigDTO config = itemAddEditDialog.getConfig();
        if (!config.checkValid()) {
            Messages.showErrorDialog("编辑出错, 输入框内容不能为空!", "Error");
            return;
        }
        boolean alreadyAdded = XUtils.stream(defaultModelList.elements()).anyMatch(it -> isAlreadyAdded(it, config));
        if (alreadyAdded) {
            Messages.showErrorDialog("编辑出错, 已添加该模块配置!", "Error");
            return;
        }
        defaultModelList.set(index, config);
        safeApply();
    }

    /**
     * 新增配置
     */
    @SuppressWarnings("DialogTitleCapitalization")
    private void addAction(AnActionButton button) {
        final Project project = ProjectUtil.guessCurrentProject(list);
        ItemAddEditDialog itemAddEditDialog = new ItemAddEditDialog(null, project);
        if (!itemAddEditDialog.showAndGet()) {
            return;
        }
        final ConfigDTO config = itemAddEditDialog.getConfig();
        if (!config.checkValid()) {
            Messages.showErrorDialog("添加出错, 输入框内容不能为空!", "Error");
            return;
        }
        boolean alreadyAdded = XUtils.stream(defaultModelList.elements()).anyMatch(it -> isAlreadyAdded(it, config));
        if (alreadyAdded) {
            Messages.showErrorDialog("添加出错, 已添加该模块配置!", "Error");
            return;
        }
        defaultModelList.addElement(config);
        safeApply();
    }

    /**
     * 校验两个 ConfigDTO 的项目名和模块名是否相同
     */
    private boolean isAlreadyAdded(ConfigDTO dto, ConfigDTO config) {
        return dto.getProjectName().equals(config.getProjectName()) && dto.getModuleName().equals(config.getModuleName());
    }

    @Override
    public boolean isModified() {
        if (configPersistence.getConfigs() == null) {
            return true;
        }
        //当用户修改配置参数后，在点击“OK”“Apply”按钮前，框架会自动调用该方法，判断是否有修改，进而控制按钮“OK”“Apply”的是否可用。
        return defaultModelList.size() == configPersistence.getConfigs().size();
    }

    @Override
    public void apply() throws ConfigurationException {
        // 用户点击“OK”或“Apply”按钮后会调用该方法，通常用于完成配置信息持久化。
        List<ConfigDTO> list = XUtils.stream(defaultModelList.elements()).collect(Collectors.toList());
        configPersistence.setConfigs(list);
    }

    /**
     * 抑制 apply 的 checked Exception<br/>
     * 不会吃掉异常
     */
    private void safeApply() {
        try {
            apply();
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

}
