package com.qbb.interaction;

import com.google.common.base.Strings;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import com.qbb.builder.BuildJsonForDubbo;
import com.qbb.builder.BuildJsonForYapi;
import com.qbb.component.ConfigPersistence;
import com.qbb.constant.ProjectTypeConstant;
import com.qbb.constant.YapiConstant;
import com.qbb.dto.*;
import com.qbb.upload.UploadYapi;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
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

    private static final NotificationGroup notificationGroup = new NotificationGroup("Java2Json.NotificationGroup", NotificationDisplayType.BALLOON, true);

    @Override
    @SuppressWarnings("DialogTitleCapitalization")
    public void actionPerformed(AnActionEvent e) {
        Project project = Optional.ofNullable(e.getDataContext().getData(CommonDataKeys.EDITOR))
                .map(Editor::getProject).orElse(null);
        if (project == null) {
            return;
        }
        String projectToken;
        String projectId;
        String yapiUrl;
        String projectType;
        String returnClass = null;
        String attachUpload = null;
        // 获取配置
        try {
            final List<ConfigDTO> configs = ServiceManager.getService(ConfigPersistence.class).getConfigs();
            if (configs == null || configs.isEmpty()) {
                Messages.showErrorDialog("请先去配置界面配置yapi配置", "获取配置失败！");
                return;
            }
            PsiFile psiFile = e.getDataContext().getData(CommonDataKeys.PSI_FILE);
            if (psiFile == null) {
                return;
            }
            final List<ConfigDTO> collect = configs.stream().filter(it -> filterModule(it, project, psiFile)).collect(Collectors.toList());
            if (collect.isEmpty()) {
                Messages.showErrorDialog("没有找到对应的yapi配置，请在菜单 > Preferences > Other setting > YapiUpload 添加", "Error");
                return;
            }
            final ConfigDTO configDTO = collect.get(0);
            projectToken = configDTO.getProjectToken();
            projectId = configDTO.getProjectId();
            yapiUrl = configDTO.getYapiUrl();
            projectType = configDTO.getProjectType();
        } catch (Exception e2) {
            Messages.showErrorDialog("获取配置失败，异常:  " + e2.getMessage(), "获取配置失败！");
            return;
        }
//        // 配置校验
//        if (Strings.isNullOrEmpty(projectToken) || Strings.isNullOrEmpty(projectId) || Strings.isNullOrEmpty(yapiUrl) || Strings.isNullOrEmpty(projectType)) {
//            Messages.showErrorDialog("请在项目的.idea目录下的misc.xml中配置[projectToken,projectId,yapiUrl,projectType] " ,"获取配置失败！");
//            return;
//        }
        // 判断项目类型
        if (ProjectTypeConstant.dubbo.equals(projectType)) {
            // 获得dubbo需上传的接口列表 参数对象
            ArrayList<YapiDubboDTO> yapiDubboDTOs = new BuildJsonForDubbo().actionPerformedList(e);
            if (yapiDubboDTOs != null) {
                for (YapiDubboDTO yapiDubboDTO : yapiDubboDTOs) {
                    YapiSaveParam yapiSaveParam = new YapiSaveParam(projectToken, yapiDubboDTO.getTitle(), yapiDubboDTO.getPath(), yapiDubboDTO.getParams(), yapiDubboDTO.getResponse(), Integer.valueOf(projectId), yapiUrl, yapiDubboDTO.getDesc());
                    yapiSaveParam.setStatus(yapiDubboDTO.getStatus());
                    if (!Strings.isNullOrEmpty(yapiDubboDTO.getMenu())) {
                        yapiSaveParam.setMenu(yapiDubboDTO.getMenu());
                    } else {
                        yapiSaveParam.setMenu(YapiConstant.menu);
                    }
                    try {
                        // 上传
                        YapiResponse yapiResponse = new UploadYapi().uploadSave(yapiSaveParam, null, project.getBasePath());
                        if (yapiResponse.getErrcode() != 0) {
                            Messages.showErrorDialog("上传失败！异常:  " + yapiResponse.getErrmsg(), "上传失败！");
                        } else {
                            String url = yapiUrl + "/project/" + projectId + "/interface/api/cat_" + yapiResponse.getCatId();
                            this.setClipboard(url);
                            Messages.showInfoMessage("上传成功！接口文档url地址:  " + url, "上传成功！");
                        }
                    } catch (Exception e1) {
                        Messages.showErrorDialog("上传失败！异常:  " + e1, "上传失败！");
                    }
                }
            }
        } else if (ProjectTypeConstant.api.equals(projectType)) {
            //获得api 需上传的接口列表 参数对象
            List<YapiApiDTO> yapiApiDTOS = new BuildJsonForYapi().actionPerformedList(e, null, null);
            if (yapiApiDTOS != null) {
                for (YapiApiDTO yapiApiDTO : yapiApiDTOS) {
                    YapiSaveParam yapiSaveParam = new YapiSaveParam(projectToken, yapiApiDTO.getTitle(), yapiApiDTO.getPath(), yapiApiDTO.getParams(), yapiApiDTO.getRequestBody(), yapiApiDTO.getResponse(), Integer.valueOf(projectId), yapiUrl, true, yapiApiDTO.getMethod(), yapiApiDTO.getDesc(), yapiApiDTO.getHeader());
                    yapiSaveParam.setReq_body_form(yapiApiDTO.getReq_body_form());
                    yapiSaveParam.setReq_body_type(yapiApiDTO.getReq_body_type());
                    yapiSaveParam.setReq_params(yapiApiDTO.getReq_params());
                    yapiSaveParam.setStatus(yapiApiDTO.getStatus());
                    if (!Strings.isNullOrEmpty(yapiApiDTO.getMenu())) {
                        yapiSaveParam.setMenu(yapiApiDTO.getMenu());
                    } else {
                        yapiSaveParam.setMenu(YapiConstant.menu);
                    }
                    try {
                        // 上传
                        YapiResponse yapiResponse = new UploadYapi().uploadSave(yapiSaveParam, attachUpload, project.getBasePath());
                        if (yapiResponse.getErrcode() != 0) {
                            Messages.showInfoMessage("上传失败，原因:  " + yapiResponse.getErrmsg(), "上传失败！");
                        } else {
                            String url = yapiUrl + "/project/" + projectId + "/interface/api/cat_" + yapiResponse.getCatId();
                            this.setClipboard(url);
                            Messages.showInfoMessage("上传成功！接口文档url地址:  " + url, "上传成功！");
                        }
                    } catch (Exception e1) {
                        Messages.showErrorDialog("上传失败！异常:  " + e1, "上传失败！");
                    }
                }
            }
        }
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
    private void setClipboard(String content) {
        //获取系统剪切板
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        //构建String数据类型
        StringSelection selection = new StringSelection(content);
        //添加文本到系统剪切板
        clipboard.setContents(selection, null);
    }
}
