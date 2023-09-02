package com.qbb.interaction;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import com.qbb.component.ConfigPersistence;
import com.qbb.dto.ConfigDTO;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 入口
 *
 * @author chengsheng@qbb6.com
 * @since 2019/5/15
 */
public class UploadToYapi extends AnAction {

    @Override
    @SuppressWarnings("DialogTitleCapitalization")
    public void actionPerformed(AnActionEvent event) {
        Project project = Optional.ofNullable(event.getDataContext().getData(CommonDataKeys.EDITOR))
                .map(Editor::getProject).orElse(null);
        PsiFile psiFile = event.getDataContext().getData(CommonDataKeys.PSI_FILE);
        if (project == null || psiFile == null) {
            return;
        }
        // 获取配置
        final List<ConfigDTO> configs = ServiceManager.getService(ConfigPersistence.class).getConfigs();
        if (configs == null || configs.isEmpty()) {
            Messages.showErrorDialog("请先去配置界面配置yapi配置", "获取配置失败！");
            return;
        }
        final List<ConfigDTO> collect = configs.stream().filter(it -> filterModule(it, project, psiFile)).collect(Collectors.toList());
        if (collect.isEmpty()) {
            Messages.showErrorDialog("没有找到对应的yapi配置，请在菜单 > Preferences > Other setting > YapiUpload 添加", "Error");
            return;
        }
        final ConfigDTO configDTO = collect.get(0);
        String projectType = configDTO.getProjectType();
        // 判断项目类型
        String url = ApiUploadersEnum.ofType(projectType).uploadToYapi(event, configDTO);
        setClipboard(url);
    }

    /**
     * 过滤掉非当前选中文件模块的配置<br/>
     * 找到当前文件所在模块的配置
     */
    private static boolean filterModule(ConfigDTO config, Project project, PsiFile psiFile) {
        String projectName = config.getProjectName();
        if (!projectName.equals(project.getName())) {
            return false;
        }
        String separator = "/";
        String moduleName = config.getModuleName().equals(projectName) ? "" : (config.getModuleName() + separator);
        final String str = (separator + projectName + separator) + moduleName;
        return psiFile.getVirtualFile().getPath().contains(str);
    }

    /**
     * 复制到剪切板
     *
     * @author chengsheng@qbb6.com
     * @since 2019/7/3
     */
    private void setClipboard(@Nullable String content) {
        if (content == null) {
            return;
        }
        //获取系统剪切板
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        //构建String数据类型
        StringSelection selection = new StringSelection(content);
        //添加文本到系统剪切板
        clipboard.setContents(selection, null);
    }
}
