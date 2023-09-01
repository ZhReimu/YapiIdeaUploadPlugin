package com.qbb.dto;

import com.google.common.base.Strings;
import com.qbb.constant.YapiConstant;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * yapi 保存请求参数
 *
 * @author chengsheng@qbb6.com
 * @since 2019/1/31 11:43 AM
 */
@Data
public class YapiSaveParam implements Serializable {
    /**
     * 项目 token  唯一标识
     */
    private String token;

    /**
     * 请求参数
     */
    private List<YapiQueryDTO> req_query;
    /**
     * header
     */
    private List<YapiHeaderDTO> req_headers;
    /**
     * 请求参数 form 类型
     */
    private List<Map<String, String>> req_body_form;
    /**
     * 标题
     */
    private String title;
    /**
     * 分类id
     */
    private String catid;
    /**
     * 请求数据类型   raw,form,json
     */
    private String req_body_type = "json";
    /**
     * 请求数据body
     */
    private String req_body_other;
    /**
     * 请求参数body 是否为json_schema
     */
    private boolean req_body_is_json_schema;
    /**
     * 路径
     */
    private String path;
    /**
     * 状态 undone,默认done
     */
    private String status = "undone";
    /**
     * 返回参数类型  json
     */
    private String res_body_type = "json";

    /**
     * 返回参数
     */
    private String res_body;

    /**
     * 返回参数是否为json_schema
     */
    private boolean res_body_is_json_schema = true;

    /**
     * 创建的用户名
     */
    private Integer edit_uid = 11;
    /**
     * 用户名称
     */
    private String username;

    /**
     * 邮件开关
     */
    private boolean switch_notice;

    private String message = " ";
    /**
     * 文档描述
     */
    private String desc = "<h3>请补充描述</h3>";

    /**
     * 请求方式
     */
    private String method = "POST";
    /**
     * 请求参数
     */
    private List<YapiPathVariableDTO> req_params;

    private String id;
    /**
     * 项目id
     */
    private Integer projectId;

    /**
     * yapi 地址
     */
    private String yapiUrl;
    /**
     * 菜单名称
     */
    private String menu;

    public static YapiSaveParam ofDubbo(YapiDubboDTO dubbo, String projectToken, String projectId, String yapiUrl) {
        YapiSaveParam saveParam = new YapiSaveParam();
        saveParam.setToken(projectToken);
        saveParam.setTitle(dubbo.getTitle());
        saveParam.setPath(dubbo.getPath());
        saveParam.setRes_body(dubbo.getResponse());
        saveParam.setReq_body_other(dubbo.getParams());
        saveParam.setProjectId(Integer.valueOf(projectId));
        saveParam.setYapiUrl(yapiUrl);
        saveParam.setDesc(dubbo.getDesc());
        saveParam.setStatus(dubbo.getStatus());
        String dubboMenu = dubbo.getMenu();
        if (!Strings.isNullOrEmpty(dubboMenu)) {
            saveParam.setMenu(dubboMenu);
        } else {
            saveParam.setMenu(YapiConstant.menu);
        }
        return saveParam;
    }

    public static YapiSaveParam ofApi(YapiApiDTO api, String projectToken, String projectId, String yapiUrl) {
        YapiSaveParam saveParam = new YapiSaveParam();
        saveParam.setToken(projectToken);
        saveParam.setTitle(api.getTitle());
        saveParam.setPath(api.getPath());
        saveParam.setReq_query(api.getParams());
        saveParam.setReq_body_other(api.getRequestBody());
        saveParam.setRes_body(api.getResponse());
        saveParam.setProjectId(Integer.valueOf(projectId));
        saveParam.setYapiUrl(yapiUrl);
        saveParam.setReq_body_is_json_schema(true);
        saveParam.setMethod(api.getMethod());
        saveParam.setDesc(api.getDesc());
        saveParam.setReq_headers(api.getHeader());
        saveParam.setReq_body_form(api.getReq_body_form());
        saveParam.setReq_body_type(api.getReq_body_type());
        saveParam.setReq_params(api.getReq_params());
        saveParam.setStatus(api.getStatus());
        String apiMenu = api.getMenu();
        if (!Strings.isNullOrEmpty(apiMenu)) {
            saveParam.setMenu(apiMenu);
        } else {
            saveParam.setMenu(YapiConstant.menu);
        }
        return saveParam;
    }

}
