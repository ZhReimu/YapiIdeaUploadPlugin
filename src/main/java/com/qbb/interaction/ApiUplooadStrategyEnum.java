package com.qbb.interaction;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.qbb.builder.BuildJsonForDubbo;
import com.qbb.builder.BuildJsonForYapi;
import com.qbb.constant.ProjectTypeConstant;
import com.qbb.dto.*;
import com.qbb.upload.UploadYapi;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * 上传策略枚举类
 */
@SuppressWarnings("SpellCheckingInspection")
public enum ApiUplooadStrategyEnum {

    /**
     * 处理 dubbo 接口
     */
    DUBBO(ProjectTypeConstant.dubbo, ApiUplooadStrategyEnum::uploadForDubbo),
    /**
     * 处理 restAPI 接口
     */
    API(ProjectTypeConstant.api, ApiUplooadStrategyEnum::uploadForApi);
    /**
     * 该策略支持的接口类型
     */
    private final String type;
    /**
     * 执行策略的方法
     */
    private final BiFunction<AnActionEvent, ConfigDTO, String> uploadFunction;
    /**
     * 枚举 set, 方便通过 type 查找策略
     */
    private static final Set<ApiUplooadStrategyEnum> handlers = EnumSet.allOf(ApiUplooadStrategyEnum.class);

    ApiUplooadStrategyEnum(String type, BiFunction<AnActionEvent, ConfigDTO, String> uploadFunction) {
        this.type = type;
        this.uploadFunction = uploadFunction;
    }

    @Nullable
    public String uploadToYapi(AnActionEvent event, ConfigDTO config) {
        String url = uploadFunction.apply(event, config);
        Messages.showInfoMessage("上传成功, 接口文档 url 地址:  " + url, "上传成功！");
        return url;
    }

    public static ApiUplooadStrategyEnum ofType(String type) {
        return handlers.stream().filter(it -> it.type.equals(type)).findFirst().orElseThrow(IllegalArgumentException::new);
    }

    @Nullable
    private static String uploadForDubbo(AnActionEvent event, ConfigDTO config) {
        // 获得dubbo需上传的接口列表 参数对象
        List<YapiDubboDTO> yapiDubboDTOs = BuildJsonForDubbo.actionPerformedList(event);
        String yapiUrl = config.getYapiUrl();
        String projectToken = config.getProjectToken();
        String projectId = config.getProjectId();
        List<String> docUrls = yapiDubboDTOs.stream()
                .map(it -> upload(YapiSaveParam.ofDubbo(it, projectToken, projectId, yapiUrl), config))
                .collect(Collectors.toList());
        return docUrls.get(0);
    }

    @Nullable
    private static String uploadForApi(AnActionEvent event, ConfigDTO config) {
        String yapiUrl = config.getYapiUrl();
        String projectToken = config.getProjectToken();
        String projectId = config.getProjectId();
        // 获得 api 需上传的接口列表 参数对象
        List<YapiApiDTO> yapiApiDTOS = BuildJsonForYapi.actionPerformedList(event, null, null);
        List<String> docUrls = yapiApiDTOS.stream()
                .map(it -> upload(YapiSaveParam.ofApi(it, projectToken, projectId, yapiUrl), config))
                .collect(Collectors.toList());
        return docUrls.get(0);
    }

    private static String upload(YapiSaveParam yapiSaveParam, ConfigDTO config) {
        try {
            // 上传
            YapiResponse<?> yapiResponse = UploadYapi.uploadSave(yapiSaveParam, null, null);
            if (yapiResponse.getErrcode() != 0) {
                Messages.showInfoMessage("上传失败，原因:  " + yapiResponse.getErrmsg(), "上传失败！");
            } else {
                return config.getYapiUrl() + "/project/" + config.getProjectId() + "/interface/api/cat_" + yapiResponse.getCatId();
            }
        } catch (Exception e) {
            Messages.showErrorDialog("上传失败！异常:  " + e, "上传失败！");
        }
        return null;
    }
}
