package com.qbb.interaction;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.qbb.builder.BuildJsonForDubbo;
import com.qbb.builder.BuildJsonForYapi;
import com.qbb.constant.ProjectTypeConstant;
import com.qbb.dto.*;
import com.qbb.upload.UploadYapi;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 上传策略枚举类
 */
public enum ApiUploadersEnum {

    DUBBO(ProjectTypeConstant.dubbo) {
        @Override
        public String uploadToYapi(AnActionEvent event, ConfigDTO config) {
            return uploadForDubbo(event, config);
        }
    },
    API(ProjectTypeConstant.api) {
        @Override
        public String uploadToYapi(AnActionEvent event, ConfigDTO config) {
            return uploadForApi(event, config);
        }
    };
    private final String type;
    private static final Set<ApiUploadersEnum> handlers = EnumSet.allOf(ApiUploadersEnum.class);

    ApiUploadersEnum(String type) {
        this.type = type;
    }

    public abstract String uploadToYapi(AnActionEvent event, ConfigDTO config);

    public static ApiUploadersEnum ofType(String type) {
        return handlers.stream().filter(it -> it.type.equals(type)).findFirst().orElseThrow(IllegalArgumentException::new);
    }

    private static String uploadForDubbo(AnActionEvent event, ConfigDTO config) {
        // 获得dubbo需上传的接口列表 参数对象
        List<YapiDubboDTO> yapiDubboDTOs = new BuildJsonForDubbo().actionPerformedList(event);
        String yapiUrl = config.getYapiUrl();
        String projectToken = config.getProjectToken();
        String projectId = config.getProjectId();
        if (yapiDubboDTOs != null) {
            for (YapiDubboDTO yapiDubboDTO : yapiDubboDTOs) {
                YapiSaveParam yapiSaveParam = YapiSaveParam.ofDubbo(yapiDubboDTO, projectToken, projectId, yapiUrl);
                return upload(yapiSaveParam, config);
            }
        }
        return null;
    }

    private static String uploadForApi(AnActionEvent event, ConfigDTO config) {
        String yapiUrl = config.getYapiUrl();
        String projectToken = config.getProjectToken();
        String projectId = config.getProjectId();
        //获得api 需上传的接口列表 参数对象
        List<YapiApiDTO> yapiApiDTOS = new BuildJsonForYapi().actionPerformedList(event, null, null);
        if (yapiApiDTOS != null) {
            for (YapiApiDTO yapiApiDTO : yapiApiDTOS) {
                YapiSaveParam yapiSaveParam = YapiSaveParam.ofApi(yapiApiDTO, projectToken, projectId, yapiUrl);
                return upload(yapiSaveParam, config);
            }
        }
        return null;
    }

    private static String upload(YapiSaveParam yapiSaveParam, ConfigDTO config) {
        try {
            // 上传
            YapiResponse<?> yapiResponse = UploadYapi.uploadSave(yapiSaveParam, null, null);
            if (yapiResponse.getErrcode() != 0) {
                Messages.showInfoMessage("上传失败，原因:  " + yapiResponse.getErrmsg(), "上传失败！");
            } else {
                String url = config.getYapiUrl() + "/project/" + config.getProjectId() + "/interface/api/cat_" + yapiResponse.getCatId();
                Messages.showInfoMessage("上传成功！接口文档url地址:  " + url, "上传成功！");
                return url;
            }
        } catch (Exception e1) {
            Messages.showErrorDialog("上传失败！异常:  " + e1, "上传失败！");
        }
        return null;
    }
}
