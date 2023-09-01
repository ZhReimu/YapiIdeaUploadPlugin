package com.qbb.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * yapi 返回结果
 *
 * @author chengsheng@qbb6.com
 * @since 2019/1/31 12:08 PM
 */
@Data
public class YapiResponse<T> implements Serializable {
    /**
     * 状态码
     */
    private Integer errcode;
    /**
     * 状态信息
     */
    private String errmsg;
    /**
     * 返回结果
     */
    private T data;
    /**
     * 分类
     */
    private String catId;

    public YapiResponse() {
        this.errcode = 0;
        this.errmsg = "success";
    }

    public YapiResponse(Integer errcode, String errmsg) {
        this.errcode = errcode;
        this.errmsg = errmsg;
    }

}
