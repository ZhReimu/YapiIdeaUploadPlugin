package com.qbb.upload;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qbb.constant.YapiConstant;
import com.qbb.dto.*;
import com.qbb.util.XUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 上传到 yapi
 *
 * @author chengsheng@qbb6.com
 * @since 2019/1/31 11:41 AM
 */
public class UploadYapi {

    private static final Gson gson = new Gson();

    /**
     * 调用保存接口
     *
     * @author chengsheng@qbb6.com
     * @since 2019/5/15
     */
    public static YapiResponse<?> uploadSave(YapiSaveParam yapiSaveParam, String attachUpload, String path) throws IOException {
        if (Strings.isNullOrEmpty(yapiSaveParam.getTitle())) {
            yapiSaveParam.setTitle(yapiSaveParam.getPath());
        }
        YapiHeaderDTO yapiHeaderDTO = new YapiHeaderDTO();
        if ("form".equals(yapiSaveParam.getReq_body_type())) {
            yapiHeaderDTO.setName("Content-Type");
            yapiHeaderDTO.setValue("application/x-www-form-urlencoded");
            yapiSaveParam.setReq_body_form(yapiSaveParam.getReq_body_form());
        } else {
            yapiHeaderDTO.setName("Content-Type");
            yapiHeaderDTO.setValue("application/json");
            yapiSaveParam.setReq_body_type("json");
        }
        List<YapiHeaderDTO> headers = Optional.ofNullable(yapiSaveParam.getReq_headers()).orElseGet(ArrayList::new);
        headers.add(yapiHeaderDTO);
        yapiSaveParam.setReq_headers(headers);
        // changeDesByPath(yapiSaveParam);
        YapiResponse<?> yapiResponse = getOrCreateCatId(yapiSaveParam);
        if (yapiResponse.getErrcode() == 0) {
            String response = XUtils.doPost(yapiSaveParam.getYapiUrl() + YapiConstant.yapiSave, gson.toJson(yapiSaveParam));
            YapiResponse<?> yapiResponseResult = gson.fromJson(response, YapiResponse.class);
            yapiResponseResult.setCatId(yapiSaveParam.getCatid());
            return yapiResponseResult;
        } else {
            return yapiResponse;
        }
    }

    /**
     * 获得描述
     *
     * @author chengsheng@qbb6.com
     * @since 2019/7/28
     */
    public static void changeDesByPath(YapiSaveParam yapiSaveParam) throws IOException {
        String url = yapiSaveParam.getYapiUrl() + YapiConstant.yapiGetByPath + "?token=" + yapiSaveParam.getToken() + "&path=" + yapiSaveParam.getPath();
        String response = XUtils.doGet(url);
        YapiResponse<?> yapiResponse = gson.fromJson(response, YapiResponse.class);
        if (yapiResponse.getErrcode() == 0) {
            YapiInterfaceResponse yapiInterfaceResponse = gson.fromJson(gson.toJson(yapiResponse.getData()), YapiInterfaceResponse.class);
            if (!Strings.isNullOrEmpty(yapiInterfaceResponse.getDesc())) {
                //如果原来描述不为空，那么就将当前描述+上一个版本描述的自定义部分
                if (yapiInterfaceResponse.getDesc().contains("java类")) {
                    yapiSaveParam.setDesc(yapiInterfaceResponse.getDesc().substring(0, yapiInterfaceResponse.getDesc().indexOf("java类")) + yapiSaveParam.getDesc() + yapiInterfaceResponse.getDesc().substring(yapiInterfaceResponse.getDesc().indexOf("</pre>")));
                } else {
                    yapiSaveParam.setDesc(yapiInterfaceResponse.getDesc().substring(0, yapiInterfaceResponse.getDesc().indexOf("<pre>")) + yapiSaveParam.getDesc() + yapiInterfaceResponse.getDesc().substring(yapiInterfaceResponse.getDesc().indexOf("</pre>")));
                }
            }
            if (Objects.nonNull(yapiInterfaceResponse.getCatid())) {
                yapiSaveParam.setCatid(yapiInterfaceResponse.getCatid().toString());
            }
        }
    }

    /**
     * 获得分类或者创建分类
     *
     * @author chengsheng@qbb6.com
     * @since 2019/5/15
     */
    public static YapiResponse<?> getOrCreateCatId(YapiSaveParam yapiSaveParam) {
        // 如果缓存不存在，切自定义菜单为空，则使用默认目录
        if (Strings.isNullOrEmpty(yapiSaveParam.getMenu())) {
            yapiSaveParam.setMenu(YapiConstant.menu);
        }
        try {
            String url = yapiSaveParam.getYapiUrl() + YapiConstant.yapiCatMenu + "?project_id="
                    + yapiSaveParam.getProjectId() + "&token=" + yapiSaveParam.getToken();
            YapiResponse<List<YapiCatResponse>> yapiResponse = XUtils.doGet(url, new TypeToken<YapiResponse<List<YapiCatResponse>>>() {
            });
            if (yapiResponse.getErrcode() == 0) {
                List<YapiCatResponse> list = yapiResponse.getData();
                String[] menus = yapiSaveParam.getMenu().split("/");
                // 循环多级菜单，判断是否存在，如果不存在就创建
                // 解决多级菜单创建问题
                Integer parent_id = -1;
                Integer now_id;
                for (int i = 0; i < menus.length; i++) {
                    if (Strings.isNullOrEmpty(menus[i])) {
                        continue;
                    }
                    boolean needAdd = true;
                    now_id = null;
                    for (YapiCatResponse yapiCatResponse : list) {
                        if (yapiCatResponse.getName().equals(menus[i])) {
                            needAdd = false;
                            now_id = yapiCatResponse.get_id();
                            break;
                        }
                    }
                    if (needAdd) {
                        now_id = addMenu(yapiSaveParam, parent_id, menus[i]);
                    }
                    if (i == (menus.length - 1)) {
                        yapiSaveParam.setCatid(now_id.toString());
                    } else {
                        parent_id = now_id;
                    }
                }
            }
            return new YapiResponse<>();
        } catch (Exception e) {
            try {
                // 出现这种情况可能是 yapi 版本不支持
                yapiSaveParam.setCatid(addMenu(yapiSaveParam, -1, yapiSaveParam.getMenu()).toString());
                return new YapiResponse<>();
            } catch (IOException ignored) {
            }
            return new YapiResponse<>(0, e.toString());
        }
    }

    /**
     * 新增菜单
     *
     * @author chengsheng@qbb6.com
     * @since 2019/7/28
     */
    private static Integer addMenu(YapiSaveParam yapiSaveParam, Integer parent_id, String menu) throws IOException {
        YapiCatMenuParam yapiCatMenuParam = new YapiCatMenuParam(menu, yapiSaveParam.getProjectId(), yapiSaveParam.getToken(), parent_id);
        String url = yapiSaveParam.getYapiUrl() + YapiConstant.yapiAddCat;
        String body = gson.toJson(yapiCatMenuParam);
        String responseCat = XUtils.doPost(url, body);
        YapiCatResponse yapiCatResponse = gson.fromJson(gson.fromJson(responseCat, YapiResponse.class).getData().toString(), YapiCatResponse.class);
        return yapiCatResponse.get_id();
    }


}
