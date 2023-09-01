package com.qbb.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 配置实体
 *
 * @author zhangyunfan
 * @version 1.0
 * @since 2020/12/25
 */
@Data
public class ConfigDTO implements Serializable {

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 模块名称
     */
    private String moduleName;

    /**
     * yapi 项目 token
     */
    private String projectToken;

    /**
     * yapi 项目 id
     */
    private String projectId;

    /**
     * yapi URL
     */
    private String yapiUrl;

    /**
     * 项目类型
     */
    private String projectType;

    /**
     * 返回值
     */
    private String returnClass;

    /**
     * 附件上传
     */
    private String attachUpload;

    public boolean checkValid() {
        return projectName != null && moduleName != null
                && projectToken != null && projectId != null
                && yapiUrl != null && projectType != null;
    }

}
