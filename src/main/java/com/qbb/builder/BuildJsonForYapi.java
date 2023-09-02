package com.qbb.builder;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.qbb.builder.payload.FieldPayload;
import com.qbb.builder.payload.ResponsePayload;
import com.qbb.constant.*;
import com.qbb.dto.YapiApiDTO;
import com.qbb.dto.YapiHeaderDTO;
import com.qbb.dto.YapiPathVariableDTO;
import com.qbb.dto.YapiQueryDTO;
import com.qbb.util.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static com.qbb.builder.NormalTypes.TYPE_ARRAY;
import static com.qbb.builder.NormalTypes.TYPE_OBJECT;

/**
 * 构造用以生成 yapi 文档的 json 数据
 *
 * @author chengsheng@qbb6.com
 * @since 2018/10/27
 */
public class BuildJsonForYapi {

    private static final Set<String> filePaths = new CopyOnWriteArraySet<>();
    private static final NotificationGroup notificationGroup;

    static {
        notificationGroup = new NotificationGroup("BuildJson.NotificationGroup", NotificationDisplayType.BALLOON, true);
    }

    /**
     * 批量生成 接口数据
     */
    @NotNull
    @SuppressWarnings("DialogTitleCapitalization")
    public static List<YapiApiDTO> actionPerformedList(AnActionEvent event, String attachUpload, String returnClass) {
        Editor editor = event.getDataContext().getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = event.getDataContext().getData(CommonDataKeys.PSI_FILE);
        if (editor == null || psiFile == null) {
            return Collections.emptyList();
        }
        String selectedText = event.getRequiredData(CommonDataKeys.EDITOR).getSelectionModel().getSelectedText();
        Project project = editor.getProject();
        PsiElement referenceAt = psiFile.findElementAt(editor.getCaretModel().getOffset());
        PsiClass selectedClass = PsiTreeUtil.getContextOfType(referenceAt, PsiClass.class);
        if (selectedClass == null) {
            Messages.showErrorDialog("请使用光标选中一个类", "错误");
            return Collections.emptyList();
        }
        String classMenu = getClassMenu(selectedClass);
        if (Strings.isNullOrEmpty(selectedText) || selectedText.equals(selectedClass.getName())) {
            // 获取类下所有方法, 去除私有方法
            List<YapiApiDTO> yapiApiDTOList = Arrays.stream(selectedClass.getMethods())
                    .filter(it -> !it.getModifierList().hasModifierProperty(PsiModifier.PRIVATE) && it.getReturnType() != null)
                    .map(it -> actionPerformed(selectedClass, it, project, attachUpload, returnClass))
                    .filter(Objects::nonNull).collect(Collectors.toList());
            for (YapiApiDTO yapiApi : yapiApiDTOList) {
                if (yapiApi.getMenu() == null) {
                    yapiApi.setMenu(classMenu);
                }
            }
            return yapiApiDTOList;
        } else {
            // 寻找目标方法
            List<YapiApiDTO> yapiApiDTOList = Arrays.stream(selectedClass.getAllMethods())
                    .filter(it -> it.getName().equals(selectedText))
                    .map(it -> actionPerformed(selectedClass, it, project, attachUpload, returnClass))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (yapiApiDTOList.isEmpty()) {
                Notification error = notificationGroup.createNotification("can not find method:" + selectedText, NotificationType.ERROR);
                Notifications.Bus.notify(error, project);
                return Collections.emptyList();
            }
            YapiApiDTO yapiApiDTO = yapiApiDTOList.get(0);
            if (yapiApiDTO.getMenu() == null) {
                yapiApiDTO.setMenu(classMenu);
            }
            return yapiApiDTOList;
        }
    }

    private static YapiApiDTO actionPerformed(PsiClass targetClass, PsiMethod targetMethod, Project project, String attachUpload, String returnClass) {
        YapiApiDTO yapiApiDTO = new YapiApiDTO();
        // 获得路径
        StringBuilder path = new StringBuilder();
        //获得 Controller 上的 API 注解
        Optional.ofNullable(getApiValue(targetClass)).ifPresent(yapiApiDTO::setMenu);
        // 获取类上面的 RequestMapping 中的 value
        processRequestMapping(targetClass, path);
        // 获取 swagger 注解
        String operation = PsiAnnotationUtils.getPsiAnnotationValue(targetMethod, SwaggerConstants.API_OPERATION);
        if (StringUtils.isNotEmpty(operation)) {
            Notification info = notificationGroup.createNotification("apiOperation:" + operation, NotificationType.INFORMATION);
            Notifications.Bus.notify(info, project);
            yapiApiDTO.setTitle(operation);
        }
        yapiApiDTO.setPath(path.toString());
        // 处理 Controller 类里的方法注解
        PsiAnnotation mappingAnnotation = PsiAnnotationUtils.findAnnotation(targetMethod, SpringMVCConstant.RequestMapping);
        if (mappingAnnotation != null) {
            processRequestMapping(mappingAnnotation, path, yapiApiDTO);
        } else if ((mappingAnnotation = getMappingAnnoFromMethod(targetMethod)) != null) {
            processOtherMapping(mappingAnnotation, yapiApiDTO, path);
        }
        String classDesc = targetMethod.getText()
                .replace(Objects.nonNull(targetMethod.getBody()) ? targetMethod.getBody().getText() : "", "");
        if (!Strings.isNullOrEmpty(classDesc)) {
            classDesc = classDesc.replace("<", "&lt;").replace(">", "&gt;");
        }
        yapiApiDTO.setDesc(Objects.nonNull(yapiApiDTO.getDesc()) ? yapiApiDTO.getDesc() : " <pre><code>  " + classDesc + "</code></pre>");
        try {
            // 生成响应参数
            yapiApiDTO.setResponse(getResponse(project, targetMethod.getReturnType(), returnClass));
            getRequest(project, yapiApiDTO, targetMethod);
            // 处理上传附件相关
            Optional.ofNullable(processAttachUpload(project, attachUpload))
                    .map(it -> it.concat(yapiApiDTO.getDesc()))
                    .ifPresent(yapiApiDTO::setDesc);
            // 清空路径
            if (Strings.isNullOrEmpty(yapiApiDTO.getTitle())) {
                yapiApiDTO.setTitle(DesUtil.getDescription(targetMethod));
            }
            PsiDocComment docComment = targetMethod.getDocComment();
            if (Objects.nonNull(docComment)) {
                // 使用 class 注释上的菜单而不是 method 的
                String text = docComment.getText();
                // 支持状态
                String status = DesUtil.getStatus(text);
                if (!Strings.isNullOrEmpty(status)) {
                    yapiApiDTO.setStatus(status);
                }
                // 支持自定义路径
                String pathCustom = DesUtil.getPath(text);
                if (!Strings.isNullOrEmpty(pathCustom)) {
                    yapiApiDTO.setPath(pathCustom);
                }
            }
            return yapiApiDTO;
        } catch (Exception ex) {
            Notification error = notificationGroup.createNotification(Objects.nonNull(ex.getMessage()) ? ex.getMessage() : "build response/request data error", NotificationType.ERROR);
            Notifications.Bus.notify(error, project);
        }
        return null;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static String processAttachUpload(Project project, String attachUpload) throws IOException {
        // 先清空之前的文件路径
        filePaths.clear();
        Set<String> codeSet = new HashSet<>();
        long time = System.currentTimeMillis();
        String responseFileName = "/response_" + time + ".zip";
        String requestFileName = "/request_" + time + ".zip";
        String codeFileName = "/code_" + time + ".zip";
        if (!Strings.isNullOrEmpty(attachUpload)) {
            // 打包响应参数文件
            if (!filePaths.isEmpty()) {
                changeFilePath(project);
                FileToZipUtil.toZip(filePaths, project.getBasePath() + responseFileName, true);
                filePaths.clear();
                codeSet.add(project.getBasePath() + responseFileName);
            }
            // 清空路径
            // 生成请求参数
        } else {
            filePaths.clear();
        }
        if (!Strings.isNullOrEmpty(attachUpload)) {
            if (!filePaths.isEmpty()) {
                changeFilePath(project);
                FileToZipUtil.toZip(filePaths, project.getBasePath() + requestFileName, true);
                filePaths.clear();
                codeSet.add(project.getBasePath() + requestFileName);
            }
            // 打包请求参数文件
            if (!codeSet.isEmpty()) {
                FileToZipUtil.toZip(codeSet, project.getBasePath() + codeFileName, true);
                if (!Strings.isNullOrEmpty(attachUpload)) {
                    String fileUrl = XUtils.uploadFile(attachUpload, project.getBasePath() + codeFileName);
                    if (!Strings.isNullOrEmpty(fileUrl)) {
                        return "java类:<a href='" + fileUrl + "'>下载地址</a><br/>";
                    }
                }
            }
        } else {
            filePaths.clear();
        }
        //清空打包文件
        if (!Strings.isNullOrEmpty(attachUpload)) {
            File file = new File(project.getBasePath() + codeFileName);
            if (file.exists() && file.isFile()) {
                file.delete();
                file = new File(project.getBasePath() + responseFileName);
                file.delete();
                file = new File(project.getBasePath() + requestFileName);
                file.delete();
            }
            // 移除 文件
        }
        return null;
    }

    private static void processRequestMapping(PsiAnnotation requestMapping, StringBuilder path, YapiApiDTO yapiApiDTO) {
        PsiNameValuePair[] params = requestMapping.getParameterList().getAttributes();
        for (PsiNameValuePair param : params) {
            // 处理请求 path
            String name = param.getName();
            PsiAnnotationMemberValue value = param.getValue();
            if (name == null || "value".equals(name)) {
                PsiReference psiReference = Optional.ofNullable(value).map(PsiElement::getReference).orElse(null);
                if (psiReference == null) {
                    DesUtil.addPath(path, param.getLiteralValue());
                } else {
                    PsiElement resolve = psiReference.resolve();
                    if (resolve == null) {
                        continue;
                    }
                    String[] results = resolve.getText().split("=");
                    DesUtil.addPath(path, results[results.length - 1].split(";")[0].replace("\"", "").trim());
                    yapiApiDTO.setTitle(DesUtil.getUrlReferenceRDesc(resolve.getText()));
                    if (StringUtils.isBlank(yapiApiDTO.getMenu())) {
                        yapiApiDTO.setMenu(DesUtil.getMenu(resolve.getText()));
                    }
                    yapiApiDTO.setStatus(DesUtil.getStatus(resolve.getText()));
                    yapiApiDTO.setDesc("<pre><code>  " + resolve.getText() + " </code></pre> <hr>");
                }
                yapiApiDTO.setPath(path.toString());
            }
            // 处理请求 method
            if ("method".equals(name)) {
                Optional.ofNullable(getMethodFromReqMapping(value)).ifPresent(yapiApiDTO::setMethod);
            }
        }
    }

    private static void processOtherMapping(PsiAnnotation mappingAnnotation, YapiApiDTO yapiApiDTO, StringBuilder path) {
        SpringMappingMethodEnum methodEnum = SpringMappingMethodEnum.ofSpringMapping(mappingAnnotation.getQualifiedName());
        yapiApiDTO.setMethod(methodEnum.getHttpMethod());
        PsiNameValuePair[] attributes = mappingAnnotation.getParameterList().getAttributes();
        for (PsiNameValuePair attribute : attributes) {
            //获得方法上的路径
            String name = attribute.getName();
            if (Objects.isNull(name) || name.equals("value")) {
                PsiReference psiReference = Optional.ofNullable(attribute.getValue())
                        .map(PsiElement::getReference)
                        .orElse(null);
                if (psiReference == null) {
                    DesUtil.addPath(path, attribute.getLiteralValue());
                } else {
                    PsiElement resolve = psiReference.resolve();
                    if (resolve == null) {
                        continue;
                    }
                    String[] results = resolve.getText().split("=");
                    DesUtil.addPath(path, results[results.length - 1].split(";")[0].replace("\"", "").trim());
                    yapiApiDTO.setTitle(DesUtil.getUrlReferenceRDesc(resolve.getText()));
                    if (StringUtils.isBlank(yapiApiDTO.getMenu())) {
                        yapiApiDTO.setMenu(DesUtil.getMenu(resolve.getText()));
                    }
                    yapiApiDTO.setStatus(DesUtil.getStatus(resolve.getText()));
                    if (!Strings.isNullOrEmpty(resolve.getText())) {
                        String refernceDesc = resolve.getText().replace("<", "&lt;").replace(">", "&gt;");
                        yapiApiDTO.setDesc("<pre><code>  " + refernceDesc + " </code></pre> <hr>");
                    }
                }
                yapiApiDTO.setPath(path.toString().trim());
            }
        }
    }

    @Nullable
    private static PsiAnnotation getMappingAnnoFromMethod(PsiMethod targetMethod) {
        PsiAnnotation annotation = PsiAnnotationUtils.findAnnotation(targetMethod, SpringMVCConstant.GetMapping);
        if (annotation != null) {
            return annotation;
        }
        annotation = PsiAnnotationUtils.findAnnotation(targetMethod, SpringMVCConstant.PostMapping);
        if (annotation != null) {
            return annotation;
        }
        annotation = PsiAnnotationUtils.findAnnotation(targetMethod, SpringMVCConstant.PutMapping);
        if (annotation != null) {
            return annotation;
        }
        annotation = PsiAnnotationUtils.findAnnotation(targetMethod, SpringMVCConstant.DeleteMapping);
        if (annotation != null) {
            return annotation;
        }
        return PsiAnnotationUtils.findAnnotation(targetMethod, SpringMVCConstant.PatchMapping);
    }

    /**
     * 从 requestMapping 注解中获取 method 属性
     *
     * @param value 注解的 psi 对象
     * @return 获取到的 method 属性, 可能为 null
     */
    @Nullable
    private static String getMethodFromReqMapping(@Nullable PsiAnnotationMemberValue value) {
        if (value == null) {
            return null;
        }
        String upperCaseValue = value.toString().toUpperCase();
        if (upperCaseValue.contains(HttpMethodConstant.GET)) {
            // 判断是否为Get 请求
            return HttpMethodConstant.GET;
        } else if (upperCaseValue.contains(HttpMethodConstant.POST)) {
            // 判断是否为Post 请求
            return HttpMethodConstant.POST;
        } else if (upperCaseValue.contains(HttpMethodConstant.PUT)) {
            // 判断是否为 PUT 请求
            return HttpMethodConstant.PUT;
        } else if (upperCaseValue.contains(HttpMethodConstant.DELETE)) {
            // 判断是否为 DELETE 请求
            return HttpMethodConstant.DELETE;
        } else if (upperCaseValue.contains(HttpMethodConstant.PATCH)) {
            // 判断是否为 PATCH 请求
            return HttpMethodConstant.PATCH;
        }
        return null;
    }

    private static void processRequestMapping(PsiClass selectedClass, StringBuilder path) {
        PsiAnnotation psiAnnotation = PsiAnnotationUtils.findAnnotation(selectedClass, SpringMVCConstant.RequestMapping);
        if (psiAnnotation == null) {
            return;
        }
        PsiNameValuePair[] psiNameValuePairs = psiAnnotation.getParameterList().getAttributes();
        if (psiNameValuePairs.length > 0) {
            if (psiNameValuePairs[0].getLiteralValue() != null) {
                DesUtil.addPath(path, psiNameValuePairs[0].getLiteralValue());
            } else {
                Optional.ofNullable(psiAnnotation.findAttributeValue("value"))
                        .map(PsiAnnotationMemberValue::getReference)
                        .map(PsiReference::resolve)
                        .map(PsiElement::getText)
                        .map(it -> it.split("="))
                        .ifPresent(results -> addPatch(results, path));
            }
        }
    }

    @Nullable
    private static String getApiValue(PsiClass selectedClass) {
        String tags = PsiAnnotationUtils.findPsiAnnotationParam(selectedClass, SwaggerConstants.API, "tags");
        if (StringUtils.isNotBlank(tags)) {
            return tags;
        } else {
            tags = PsiAnnotationUtils.getPsiAnnotationValue(selectedClass, SwaggerConstants.API);
            if (StringUtils.isNotBlank(tags)) {
                return tags;
            }
        }
        return tags;
    }

    private static void addPatch(String[] results, StringBuilder path) {
        DesUtil.addPath(path, results[results.length - 1].split(";")[0].replace("\"", "").trim());
    }

    /**
     * 获得请求参数
     *
     * @author chengsheng@qbb6.com
     * @since 2019/2/19
     */
    private static void getRequest(Project project, YapiApiDTO yapiApiDTO, PsiMethod psiMethodTarget) throws RuntimeException {
        PsiParameter[] psiParameters = psiMethodTarget.getParameterList().getParameters();
        if (psiParameters.length > 0) {
            List<YapiQueryDTO> yapiParamList = new ArrayList<>();
            List<YapiHeaderDTO> yapiHeaderDTOList = new ArrayList<>();
            List<YapiPathVariableDTO> yapiPathVariableDTOList = new ArrayList<>();
            for (PsiParameter psiParameter : psiParameters) {
                String paramObj = psiParameter.getType().getCanonicalText();
                // 不处理 ServletRequest 和 ServletResponse 入参对象
                if (JavaConstant.HttpServletRequest.equals(paramObj) || JavaConstant.HttpServletResponse.equals(paramObj)) {
                    continue;
                }
                PsiAnnotation psiAnnotation = PsiAnnotationUtils.findAnnotation(psiParameter, SpringMVCConstant.RequestBody);
                if (psiAnnotation != null) {
                    yapiApiDTO.setRequestBody(getResponse(project, psiParameter.getType(), null));
                } else {
                    psiAnnotation = PsiAnnotationUtils.findAnnotation(psiParameter, SpringMVCConstant.RequestParam);
                    YapiHeaderDTO yapiHeaderDTO = null;
                    YapiPathVariableDTO yapiPathVariableDTO = null;
                    if (psiAnnotation == null) {
                        psiAnnotation = PsiAnnotationUtils.findAnnotation(psiParameter, SpringMVCConstant.RequestAttribute);
                        if (psiAnnotation == null) {
                            psiAnnotation = PsiAnnotationUtils.findAnnotation(psiParameter, SpringMVCConstant.RequestHeader);
                            if (psiAnnotation == null) {
                                psiAnnotation = PsiAnnotationUtils.findAnnotation(psiParameter, SpringMVCConstant.PathVariable);
                                yapiPathVariableDTO = new YapiPathVariableDTO();
                            } else {
                                yapiHeaderDTO = new YapiHeaderDTO();
                            }
                        }
                    }
                    if (psiAnnotation != null) {
                        PsiNameValuePair[] psiNameValuePairs = psiAnnotation.getParameterList().getAttributes();
                        YapiQueryDTO yapiQueryDTO = new YapiQueryDTO();

                        String typeName = psiParameter.getType().getPresentableText();
                        String normalType = NormalTypes.getNormalType(typeName);
                        if (psiNameValuePairs.length > 0) {
                            for (PsiNameValuePair psiNameValuePair : psiNameValuePairs) {
                                String name = psiNameValuePair.getName();
                                PsiAnnotationMemberValue value = psiNameValuePair.getValue();
                                if (value == null) {
                                    continue;
                                }
                                if ("name".equals(name) || "value".equals(name)) {
                                    if (yapiHeaderDTO != null) {
                                        yapiHeaderDTO.setName(value.getText().replace("\"", ""));
                                    } else if (yapiPathVariableDTO != null) {
                                        yapiPathVariableDTO.setName(value.getText().replace("\"", ""));
                                    } else {
                                        yapiQueryDTO.setName(value.getText().replace("\"", ""));
                                    }
                                } else if ("required".equals(name)) {
                                    yapiQueryDTO.setRequired(value.getText().replace("\"", "")
                                            .replace("false", "0").replace("true", "1"));
                                } else if ("defaultValue".equals(name)) {
                                    if (yapiHeaderDTO != null) {
                                        yapiHeaderDTO.setExample(value.getText().replace("\"", ""));
                                    } else {
                                        yapiQueryDTO.setExample(value.getText().replace("\"", ""));
                                    }
                                } else {
                                    if (yapiHeaderDTO != null) {
                                        yapiHeaderDTO.setName(psiNameValuePair.getLiteralValue());
                                        // 通过方法注释获得 描述 加上 类型
                                        yapiHeaderDTO.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + typeName + ")");
                                    }
                                    if (yapiPathVariableDTO != null) {
                                        yapiPathVariableDTO.setName(psiNameValuePair.getLiteralValue());
                                        // 通过方法注释获得 描述 加上 类型
                                        yapiPathVariableDTO.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + typeName + ")");
                                    } else {
                                        yapiQueryDTO.setName(psiNameValuePair.getLiteralValue());
                                        // 通过方法注释获得 描述 加上 类型
                                        yapiQueryDTO.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + typeName + ")");
                                    }
                                    if (normalType != null) {
                                        if (yapiHeaderDTO != null) {
                                            yapiHeaderDTO.setExample(normalType);
                                        } else if (yapiPathVariableDTO != null) {
                                            yapiPathVariableDTO.setExample(normalType);
                                        } else {
                                            yapiQueryDTO.setExample(normalType);
                                        }
                                    } else {
                                        yapiApiDTO.setRequestBody(getResponse(project, psiParameter.getType(), null));
                                    }
                                }
                            }
                        } else {
                            if (yapiHeaderDTO != null) {
                                yapiHeaderDTO.setName(psiParameter.getName());
                                // 通过方法注释获得 描述 加上 类型
                                yapiHeaderDTO.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + typeName + ")");
                            } else if (yapiPathVariableDTO != null) {
                                yapiPathVariableDTO.setName(psiParameter.getName());
                                // 通过方法注释获得 描述 加上 类型
                                yapiPathVariableDTO.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + typeName + ")");
                            } else {
                                yapiQueryDTO.setName(psiParameter.getName());
                                // 通过方法注释获得 描述 加上 类型
                                yapiQueryDTO.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + typeName + ")");
                            }
                            if (normalType != null) {
                                if (yapiHeaderDTO != null) {
                                    yapiHeaderDTO.setExample(normalType);
                                } else if (yapiPathVariableDTO != null) {
                                    yapiPathVariableDTO.setExample(normalType);
                                } else {
                                    yapiQueryDTO.setExample(normalType);
                                }
                            } else {
                                yapiApiDTO.setRequestBody(getResponse(project, psiParameter.getType(), null));
                            }
                        }
                        if (yapiHeaderDTO != null) {
                            if (Strings.isNullOrEmpty(yapiHeaderDTO.getDesc())) {
                                // 通过方法注释获得 描述  加上 类型
                                yapiHeaderDTO.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + typeName + ")");
                            }
                            if (Strings.isNullOrEmpty(yapiHeaderDTO.getExample()) && normalType != null) {
                                yapiHeaderDTO.setExample(normalType);
                            }
                            if (Strings.isNullOrEmpty(yapiHeaderDTO.getName())) {
                                yapiHeaderDTO.setName(psiParameter.getName());
                            }
                            yapiHeaderDTOList.add(yapiHeaderDTO);
                        } else if (yapiPathVariableDTO != null) {
                            if (Strings.isNullOrEmpty(yapiPathVariableDTO.getDesc())) {
                                // 通过方法注释获得 描述  加上 类型
                                yapiPathVariableDTO.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + typeName + ")");
                            }
                            if (Strings.isNullOrEmpty(yapiPathVariableDTO.getExample()) && normalType != null) {
                                yapiPathVariableDTO.setExample(normalType);
                            }
                            if (Strings.isNullOrEmpty(yapiPathVariableDTO.getName())) {
                                yapiPathVariableDTO.setName(psiParameter.getName());
                            }
                            String desc = PsiAnnotationUtils.getPsiAnnotationValue(psiParameter, SwaggerConstants.API_PARAM);
                            if (StringUtils.isNotEmpty(desc)) {
                                yapiPathVariableDTO.setDesc(desc);
                            }
                            yapiPathVariableDTOList.add(yapiPathVariableDTO);
                        } else {
                            if (Strings.isNullOrEmpty(yapiQueryDTO.getDesc())) {
                                // 通过方法注释获得 描述 加上 类型
                                yapiQueryDTO.setDesc(DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + typeName + ")");
                            }
                            if (Strings.isNullOrEmpty(yapiQueryDTO.getExample()) && normalType != null) {
                                yapiQueryDTO.setExample(normalType);
                            }
                            if (Strings.isNullOrEmpty(yapiQueryDTO.getName())) {
                                yapiQueryDTO.setName(psiParameter.getName());
                            }
                            String desc = PsiAnnotationUtils.getPsiAnnotationValue(psiParameter, SwaggerConstants.API_PARAM);
                            if (StringUtils.isNotEmpty(desc)) {
                                yapiQueryDTO.setDesc(desc);
                            }
                            yapiParamList.add(yapiQueryDTO);
                        }
                    } else {
                        if (HttpMethodConstant.GET.equals(yapiApiDTO.getMethod())) {
                            List<Map<String, String>> requestList = getRequestForm(project, psiParameter, psiMethodTarget);
                            for (Map<String, String> map : requestList) {
                                yapiParamList.add(new YapiQueryDTO(map.get("desc"), map.get("example"), map.get("name")));
                            }
                        } else if (HttpMethodConstant.POST.equals(yapiApiDTO.getMethod())) {
                            // 支持实体对象接收
                            yapiApiDTO.setReq_body_type("form");
                            if (yapiApiDTO.getReq_body_form() != null) {
                                yapiApiDTO.getReq_body_form().addAll(getRequestForm(project, psiParameter, psiMethodTarget));
                            } else {
                                yapiApiDTO.setReq_body_form(getRequestForm(project, psiParameter, psiMethodTarget));
                            }
                        }

                    }
                }
            }
            yapiApiDTO.setParams(yapiParamList);
            yapiApiDTO.setHeader(yapiHeaderDTOList);
            yapiApiDTO.setReq_params(yapiPathVariableDTOList);
        } else {
            yapiApiDTO.setParams(new ArrayList<>());
            yapiApiDTO.setHeader(new ArrayList<>());
            yapiApiDTO.setReq_params(new ArrayList<>());
        }
    }

    /**
     * 获得表单提交数据对象
     *
     * @author chengsheng@qbb6.com
     * @since 2019/5/17
     */
    private static List<Map<String, String>> getRequestForm(Project project, PsiParameter psiParameter, PsiMethod psiMethodTarget) {
        List<Map<String, String>> requestForm = new ArrayList<>();
        if (NormalTypes.isNormalType(psiParameter.getType().getPresentableText())) {
            Map<String, String> map = new HashMap<>();
            map.put("name", psiParameter.getName());
            map.put("type", "text");
            String remark = DesUtil.getParamDesc(psiMethodTarget, psiParameter.getName()) + "(" + psiParameter.getType().getPresentableText() + ")";
            map.put("desc", remark);
            map.put("example", NormalTypes.getNormalType(psiParameter.getType().getPresentableText()));
            requestForm.add(map);
        } else {
            PsiClass psiClass = JavaPsiFacade.getInstance(project)
                    .findClass(psiParameter.getType().getCanonicalText(), GlobalSearchScope.allScope(project));
            if (psiClass == null) {
                return Collections.emptyList();
            }
            for (PsiField field : psiClass.getAllFields()) {
                PsiModifierList modifierList = field.getModifierList();
                if (modifierList == null || modifierList.hasModifierProperty(PsiModifier.FINAL)) {
                    continue;
                }
                Map<String, String> map = new HashMap<>();
                map.put("name", field.getName());
                map.put("type", "text");
                String remark = DesUtil.getFiledDesc(field.getDocComment());
                remark = DesUtil.getLinkRemark(remark, project, field);
                map.put("desc", remark);
                field.getType().getPresentableText();
                String normalType = NormalTypes.getNormalType(field.getType().getPresentableText());
                if (normalType != null) {
                    map.put("example", normalType);
                }
                getFilePath(project, filePaths, DesUtil.getFieldLinks(project, field));
                requestForm.add(map);
            }
        }
        return requestForm;
    }

    /**
     * 获得响应参数
     *
     * @author chengsheng@qbb6.com
     * @since 2019/2/19
     */
    public static String getResponse(Project project, PsiType psiType, String returnClass) throws RuntimeException {
        // 最外层的包装类只会有一个泛型对应接口返回值
        if (!Strings.isNullOrEmpty(returnClass) && !psiType.getCanonicalText().split("<")[0].equals(returnClass)) {
            PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(returnClass, GlobalSearchScope.allScope(project));
            KV<String, Object> result = new KV<>();
            List<String> requiredList = new ArrayList<>();
            if (Objects.nonNull(psiClass)) {
                KV<String, Object> kvObject = getFields(psiClass, project, null, null, requiredList, new HashSet<>());
                for (PsiField field : psiClass.getAllFields()) {
                    if (NormalTypes.genericList.contains(field.getType().getPresentableText())) {
                        KV<String, Object> child = getPojoJson(project, psiType);
                        kvObject.set(field.getName(), child);
                    }
                }
                result.set("type", "object");
                result.set("title", psiClass.getName());
                result.set("required", requiredList);
                result.set("description", psiClass.getQualifiedName());
                result.set("properties", kvObject);
            } else {
                throw new RuntimeException("can not find class:" + returnClass);
            }
            return result.toPrettyJson();
        } else {
            KV<String, Object> kv = getPojoJson(project, psiType);
            return Objects.isNull(kv) ? "" : kv.toPrettyJson();
        }
    }

    private static KV<String, Object> getPojoJson(Project project, PsiType psiType) throws RuntimeException {
        String typeName = psiType.getPresentableText();
        if (psiType instanceof PsiPrimitiveType) {
            // 如果是基本类型
            KV<String, Object> kvClass = KV.create();
            kvClass.set(psiType.getCanonicalText(), NormalTypes.getNormalType(typeName));
        } else if (NormalTypes.isNormalType(typeName)) {
            // 如果是包装类型
            KV<String, Object> kvClass = KV.create();
            kvClass.set(psiType.getCanonicalText(), NormalTypes.getNormalType(typeName));
        } else if (typeName.startsWith("List")) {
            return getFieldPayloadForList(project, psiType, typeName);
        } else if (typeName.startsWith("Set")) {
            return getKvForSet(project, psiType, typeName);
        } else if (typeName.startsWith("Map") || typeName.startsWith("HashMap") || typeName.startsWith("LinkedHashMap")) {
            return getKvForMap(project, (PsiClassReferenceType) psiType);
        } else if (NormalTypes.isCollectionType(typeName)) {
            //如果是集合类型
            KV<String, Object> kvClass = KV.create();
            kvClass.set(psiType.getCanonicalText(), NormalTypes.getCollectionType(typeName));
        } else {
            return getKvForOther(project, psiType, typeName);
        }
        return null;
    }

    @NotNull
    private static KV<String, Object> getKvForOther(Project project, PsiType psiType, String typeName) {
        String[] types = psiType.getCanonicalText().split("<");
        // 判断是否有泛型参数
        if (types.length > 1) {
            List<String> requiredList = new ArrayList<>();
            KV<String, Object> result = KV.create();
            PsiClass psiClassChild = JavaPsiFacade.getInstance(project).findClass(types[0], GlobalSearchScope.allScope(project));
            KV<String, Object> kvObject = getFields(psiClassChild, project, types, 1, requiredList, new HashSet<>());
            result.set("type", "object");
            result.set("title", typeName);
            result.set("required", requiredList);
            processSourceFiles(psiClassChild);
            result.set("description", (typeName + " :" + psiClassChild.getName()).trim());
            result.set("properties", kvObject);
            return result;
        }
        KV<String, Object> result = KV.create();
        List<String> requiredList = new ArrayList<>();
        PsiClass psiClassChild = JavaPsiFacade.getInstance(project).findClass(psiType.getCanonicalText(), GlobalSearchScope.allScope(project));
        KV<String, Object> kvObject = getFields(psiClassChild, project, null, null, requiredList, new HashSet<>());
        processSourceFiles(psiClassChild);
        result.set("type", "object");
        result.set("required", requiredList);
        result.set("title", typeName);
        result.set("description", (typeName + " :" + psiClassChild.getName()).trim());
        result.set("properties", kvObject);
        return result;
    }

    @NotNull
    private static KV<String, Object> getKvForMap(Project project, PsiClassReferenceType psiType) {
        KV<String, Object> kv1 = new KV<>();
        kv1.set(KV.by("type", "object"));
        kv1.set(KV.by("description", "(该参数为map)"));
        if (psiType.getParameters().length > 1) {
            KV<String, Object> keyObj = new KV<>();
            keyObj.set("type", "object");
            keyObj.set("description", psiType.getParameters()[1].getPresentableText());
            keyObj.set("properties", getFields(PsiUtil.resolveClassInType(psiType.getParameters()[1]), project, null, 0, new ArrayList<>(), new HashSet<>()));

            KV<String, Object> key = new KV<>();
            key.set("type", "object");
            key.set("description", psiType.getParameters()[0].getPresentableText());

            KV<String, Object> keyObjSup = new KV<>();
            keyObjSup.set("mapKey", key);
            keyObjSup.set("mapValue", keyObj);
            kv1.set("properties", keyObjSup);
        } else {
            kv1.set(KV.by("description", "请完善Map<?,?>"));
        }
        return kv1;
    }

    @NotNull
    private static KV<String, Object> getKvForSet(Project project, PsiType psiType, String typeName) {
        String[] types = psiType.getCanonicalText().split("<");
        KV<String, Object> listKv = new KV<>();
        if (types.length > 1) {
            String childPackage = types[1].split(">")[0];
            if (NormalTypes.isNormalType(childPackage)) {
                String[] childTypes = childPackage.split("\\.");
                listKv.set("type", NormalTypes.java2JsonType(childTypes[childTypes.length - 1]));
            } else if (NormalTypes.isCollectionType(childPackage)) {
                String[] childTypes = childPackage.split("\\.");
                listKv.set("type", childTypes[childTypes.length - 1]);
            } else {
                PsiClass psiClassChild = JavaPsiFacade.getInstance(project).findClass(childPackage, GlobalSearchScope.allScope(project));
                List<String> requiredList = new ArrayList<>();
                KV<String, Object> kvObject = getFields(psiClassChild, project, null, null, requiredList, new HashSet<>());
                listKv.set("type", "object");
                processSourceFiles(psiClassChild);
                listKv.set("properties", kvObject);
                listKv.set("required", requiredList);
            }
        }
        KV<String, Object> result = new KV<>();
        result.set("type", "array");
        result.set("title", typeName);
        result.set("description", typeName);
        result.set("items", listKv);
        return result;
    }

    @NotNull
    private static KV<String, Object> getFieldPayloadForList(Project project, PsiType psiType, String typeName) {
        String[] types = psiType.getCanonicalText().split("<");
        FieldPayload fieldPayload = new FieldPayload();
        if (types.length > 1) {
            String childPackage = types[1].split(">")[0];
            if (NormalTypes.isNormalType(childPackage)) {
                String[] childTypes = childPackage.split("\\.");
                fieldPayload.setType(NormalTypes.java2JsonType(childTypes[childTypes.length - 1]));
            } else if (NormalTypes.isCollectionType(childPackage)) {
                String[] childTypes = childPackage.split("\\.");
                fieldPayload.setType(childTypes[childTypes.length - 1]);
            } else {
                PsiClass psiClassChild = JavaPsiFacade.getInstance(project).findClass(childPackage, GlobalSearchScope.allScope(project));
                List<String> requiredList = new ArrayList<>();
                KV<String, Object> kvObject = getFields(psiClassChild, project, null, null, requiredList, new HashSet<>());
                fieldPayload.setType(TYPE_OBJECT);
                processSourceFiles(psiClassChild);
                fieldPayload.setProperties(kvObject);
                fieldPayload.setRequired(requiredList);
            }
        }
        ResponsePayload payload = new ResponsePayload();
        payload.setType(TYPE_ARRAY);
        payload.setTitle(typeName);
        payload.setDescription(typeName);
        payload.setItems(fieldPayload);
        return payload.toKV();
    }

    private static void processSourceFiles(PsiClass psiClassChild) {
        addFilePaths(filePaths, psiClassChild);
        if (Objects.nonNull(psiClassChild.getSuperClass()) && !psiClassChild.getSuperClass().getName().equals("Object")) {
            addFilePaths(filePaths, psiClassChild.getSuperClass());
        }
    }

    /**
     * 获得属性列表
     *
     * @author chengsheng@qbb6.com
     * @since 2019/5/15
     */
    private static KV<String, Object> getFields(PsiClass psiClass, Project project, String[] childType, Integer index, List<String> requiredList, Set<String> pNames) {
        KV<String, Object> kv = KV.create();
        if (psiClass == null) {
            return kv;
        }
        if (Objects.nonNull(psiClass.getSuperClass()) && NormalTypes.isCollectionType(psiClass.getSuperClass().getName())) {
            for (PsiField field : psiClass.getFields()) {
                if (Objects.nonNull(PsiAnnotationUtils.findAnnotation(field, JavaConstant.Deprecate))) {
                    continue;
                }
                // 如果是有 notnull 和 notEmpty 注解就加入必填
                if (Objects.nonNull(PsiAnnotationUtils.findAnnotation(field, JavaConstant.NotNull))
                        || Objects.nonNull(PsiAnnotationUtils.findAnnotation(field, JavaConstant.NotEmpty))
                        || Objects.nonNull(PsiAnnotationUtils.findAnnotation(field, JavaConstant.NotBlank))) {
                    requiredList.add(field.getName());
                }
                Set<String> pNameList = new HashSet<>(pNames);
                pNameList.add(psiClass.getName());
                getField(field, project, kv, childType, index, pNameList);
            }
        } else {
            if (NormalTypes.genericList.contains(psiClass.getName()) && childType != null && childType.length > index) {
                String child = childType[index].split(">")[0];
                PsiClass psiClassChild = JavaPsiFacade.getInstance(project).findClass(child, GlobalSearchScope.allScope(project));
                getFilePath(project, filePaths, Collections.singletonList(psiClassChild));
                return getFields(psiClassChild, project, childType, index + 1, requiredList, pNames);
            } else {
                // 去除 @Deprecate 的字段
                List<PsiField> normalFields = Arrays.stream(psiClass.getAllFields())
                        .filter(it -> PsiAnnotationUtils.findAnnotation(it, JavaConstant.Deprecate) == null)
                        .collect(Collectors.toList());
                //如果是有 notNull 和 notEmpty 注解就加入必填
                List<String> required = normalFields.stream().filter(it ->
                        Objects.nonNull(PsiAnnotationUtils.findAnnotation(it, JavaConstant.NotNull))
                                || Objects.nonNull(PsiAnnotationUtils.findAnnotation(it, JavaConstant.NotEmpty))
                                || Objects.nonNull(PsiAnnotationUtils.findAnnotation(it, JavaConstant.NotBlank))
                ).map(PsiField::getName).collect(Collectors.toList());
                requiredList.addAll(required);
                for (PsiField field : normalFields) {
                    Set<String> pNameList = new HashSet<>(pNames);
                    pNameList.add(psiClass.getName());
                    getField(field, project, kv, childType, index, pNameList);
                }
            }
        }
        return kv;
    }

    /**
     * 获得单个属性
     *
     * @author chengsheng@qbb6.com
     * @since 2019/5/15
     */
    private static void getField(PsiField field, Project project, KV<String, Object> kv, String[] childType, Integer index, Set<String> pNames) {
        PsiModifierList modifierList = field.getModifierList();
        if (modifierList == null || modifierList.hasModifierProperty(PsiModifier.FINAL)) {
            return;
        }
        PsiType type = field.getType();
        String name = field.getName();
        // swagger 支持
        String remark = StringUtils.defaultIfEmpty(PsiAnnotationUtils.getPsiAnnotationValue(field, SwaggerConstants.API_MODEL_PROPERTY), "");
        if (field.getDocComment() != null) {
            if (Strings.isNullOrEmpty(remark)) {
                remark = DesUtil.getFiledDesc(field.getDocComment());
            }
            //获得link 备注
            remark = DesUtil.getLinkRemark(remark, project, field);
            getFilePath(project, filePaths, DesUtil.getFieldLinks(project, field));
        }
        // 如果是基本类型
        if (type instanceof PsiPrimitiveType) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("type", NormalTypes.java2JsonType(type.getPresentableText()));
            if (!Strings.isNullOrEmpty(remark)) {
                jsonObject.addProperty("description", remark);
            }
            jsonObject.add("mock", NormalTypes.formatMockType(type.getPresentableText()
                    , PsiAnnotationUtils.findPsiAnnotationParam(field, SwaggerConstants.API_MODEL_PROPERTY, "example")));
            kv.set(name, jsonObject);
        } else {
            // reference Type
            String fieldTypeName = type.getPresentableText();
            // normal Type
            if (NormalTypes.isNormalType(fieldTypeName)) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("type", NormalTypes.java2JsonType(fieldTypeName));
                if (!Strings.isNullOrEmpty(remark)) {
                    jsonObject.addProperty("description", remark);
                }
                jsonObject.add("mock", NormalTypes.formatMockType(type.getPresentableText()
                        , PsiAnnotationUtils.findPsiAnnotationParam(field, SwaggerConstants.API_MODEL_PROPERTY, "example")));
                kv.set(name, jsonObject);
            } else if (!(type instanceof PsiArrayType) && ((PsiClassReferenceType) type).resolve().isEnum()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("type", "enum");
                if (Strings.isNullOrEmpty(remark)) {
                    PsiField[] fields = ((PsiClassReferenceType) type).resolve().getAllFields();
                    List<PsiField> fieldList = Arrays.stream(fields).filter(f -> f instanceof PsiEnumConstant).collect(Collectors.toList());
                    StringBuilder remarkBuilder = new StringBuilder();
                    for (PsiField psiField : fieldList) {
                        String comment = DesUtil.getFiledDesc(psiField.getDocComment());
                        comment = Strings.isNullOrEmpty(comment) ? comment : "-" + comment;
                        remarkBuilder.append(psiField.getName()).append(comment);
                        remarkBuilder.append("\n");
                    }
                    remark = remarkBuilder.toString();
                }
                jsonObject.addProperty("description", remark);
                kv.set(name, jsonObject);
            } else if (NormalTypes.genericList.contains(fieldTypeName)) {
                if (childType != null) {
                    String child = childType[index].split(">")[0];
                    if ("?".equals(child)) {
                        KV<String, Object> kv1 = new KV<>();
                        kv.set(name, kv1);
                        kv1.set(KV.by("type", "?"));
                        kv1.set(KV.by("description", (Strings.isNullOrEmpty(remark) ? name : remark)));
                        kv1.set(KV.by("mock", NormalTypes.formatMockType("?", "?")));
                    } else if (child.contains("java.util.List") || child.contains("java.util.Set") || child.contains("java.util.HashSet")) {
                        index = index + 1;
                        PsiClass psiClassChild = JavaPsiFacade.getInstance(project)
                                .findClass(childType[index].split(">")[0], GlobalSearchScope.allScope(project));
                        if (psiClassChild == null) {
                            return;
                        }
                        KV<String, Object> kv1 = getCollect(psiClassChild.getName(), remark, psiClassChild, project, pNames, childType, index + 1);
                        kv.set(name, kv1);
                    } else if (NormalTypes.isNormalType(child)) {
                        KV<String, Object> kv1 = new KV<>();
                        PsiClass psiClassChild = JavaPsiFacade.getInstance(project).findClass(child, GlobalSearchScope.allScope(project));
                        kv1.set(KV.by("type", NormalTypes.java2JsonType(psiClassChild.getName())));
                        kv.set(name, kv1);
                        kv1.set(KV.by("description", (Strings.isNullOrEmpty(remark) ? name : remark)));
                        JsonObject example = NormalTypes.formatMockType(child, PsiAnnotationUtils.findPsiAnnotationParam(field, SwaggerConstants.API_MODEL_PROPERTY, "example"));
                        kv1.set(KV.by("mock", example));
                    } else {
                        //class type
                        KV<String, Object> kv1 = new KV<>();
                        kv1.set(KV.by("type", "object"));
                        PsiClass psiClassChild = JavaPsiFacade.getInstance(project).findClass(child, GlobalSearchScope.allScope(project));
                        kv1.set(KV.by("description", (Strings.isNullOrEmpty(remark) ? (psiClassChild.getName().trim()) : remark + " ," + psiClassChild.getName().trim())));
                        if (!pNames.contains(psiClassChild.getName())) {
                            List<String> requiredList = new ArrayList<>();
                            kv1.set(KV.by("properties", getFields(psiClassChild, project, childType, index + 1, requiredList, pNames)));
                            kv1.set("required", requiredList);
                            addFilePaths(filePaths, psiClassChild);
                        } else {
                            kv1.set(KV.by("type", psiClassChild.getName()));
                        }
                        kv.set(name, kv1);
                    }
                }
                //    getField()
            } else if (type instanceof PsiArrayType) {
                //array type
                PsiType deepType = type.getDeepComponentType();
                KV<String, Object> kvlist = new KV<>();
                String deepTypeName = deepType.getPresentableText();
                String cType = "";
                if (deepType instanceof PsiPrimitiveType) {
                    kvlist.set("type", type.getPresentableText());
                    if (!Strings.isNullOrEmpty(remark)) {
                        kvlist.set("description", remark);
                    }
                } else if (NormalTypes.isNormalType(deepTypeName)) {
                    kvlist.set("type", NormalTypes.java2JsonType(deepTypeName));
                    if (!Strings.isNullOrEmpty(remark)) {
                        kvlist.set("description", remark);
                    }
                } else {
                    kvlist.set(KV.by("type", "object"));
                    PsiClass psiClass = PsiUtil.resolveClassInType(deepType);
                    cType = psiClass.getName();
                    kvlist.set(KV.by("description", (Strings.isNullOrEmpty(remark) ? (psiClass.getName().trim()) : remark + " ," + psiClass.getName().trim())));
                    if (!pNames.contains(PsiUtil.resolveClassInType(deepType).getName())) {
                        List<String> requiredList = new ArrayList<>();
                        kvlist.set("properties", getFields(psiClass, project, null, null, requiredList, pNames));
                        kvlist.set("required", requiredList);
                        addFilePaths(filePaths, psiClass);
                    } else {
                        kvlist.set(KV.by("type", PsiUtil.resolveClassInType(deepType).getName()));
                    }
                }
                KV<String, Object> kv1 = new KV<>();
                kv1.set(KV.by("type", "array"));
                kv1.set(KV.by("description", (remark + " :" + cType).trim()));
                kv1.set("items", kvlist);
                kv.set(name, kv1);
            } else if (fieldTypeName.startsWith("List") || fieldTypeName.startsWith("Set") || fieldTypeName.startsWith("HashSet")) {
                //list type
                PsiType iterableType = PsiUtil.extractIterableTypeParameter(type, false);
                PsiClass iterableClass = PsiUtil.resolveClassInClassTypeOnly(iterableType);
                if (Objects.nonNull(iterableClass)) {
                    String classTypeName = iterableClass.getName();
                    KV<String, Object> kv1 = getCollect(classTypeName, remark, iterableClass, project, pNames, childType, index);
                    kv.set(name, kv1);
                }
            } else if (fieldTypeName.startsWith("HashMap") || fieldTypeName.startsWith("Map") || fieldTypeName.startsWith("LinkedHashMap")) {
                //HashMap or Map
                KV<String, Object> kv1 = new KV<>();
                kv1.set(KV.by("type", "object"));
                kv1.set(KV.by("description", remark + "(该参数为map)"));
                if (((PsiClassReferenceType) type).getParameters().length > 1) {
                    KV<String, Object> keyObj = new KV<>();
                    keyObj.set("type", "object");
                    keyObj.set("description", ((PsiClassReferenceType) type).getParameters()[1].getPresentableText());
                    keyObj.set("properties", getFields(PsiUtil.resolveClassInType(((PsiClassReferenceType) type).getParameters()[1]), project, childType, index, new ArrayList<>(), pNames));

                    KV<String, Object> key = new KV<>();
                    key.set("type", "object");
                    key.set("description", ((PsiClassReferenceType) type).getParameters()[0].getPresentableText());

                    KV<String, Object> keyObjSup = new KV<>();
                    keyObjSup.set("mapKey", key);
                    keyObjSup.set("mapValue", keyObj);
                    kv1.set("properties", keyObjSup);
                } else {
                    kv1.set(KV.by("description", "请完善Map<?,?>"));
                }
                kv.set(name, kv1);
            } else {
                //class type
                KV<String, Object> kv1 = new KV<>();
                PsiClass psiClass = PsiUtil.resolveClassInType(type);
                kv1.set(KV.by("type", "object"));
                kv1.set(KV.by("description", (Strings.isNullOrEmpty(remark) ? (psiClass.getName().trim()) : (remark + " ," + psiClass.getName()).trim())));
                if (!pNames.contains(((PsiClassReferenceType) type).getClassName())) {
                    addFilePaths(filePaths, psiClass);
                    List<String> requiredList = new ArrayList<>();
                    kv1.set(KV.by("properties", getFields(PsiUtil.resolveClassInType(type), project, childType, index, requiredList, pNames)));
                    kv1.set("required", requiredList);
                } else {
                    kv1.set(KV.by("type", ((PsiClassReferenceType) type).getClassName()));
                }
                kv.set(name, kv1);
            }
        }
    }

    /**
     * 获得集合
     *
     * @author chengsheng@qbb6.com
     * @since 2019/5/15
     */
    private static KV<String, Object> getCollect(String typeName, String remark, PsiClass psiClass, Project project, Set<String> pNames, String[] childType, Integer index) {
        KV<String, Object> kvList = new KV<>();
        String name = Objects.requireNonNull(psiClass.getName());
        if (NormalTypes.isNormalType(typeName) || NormalTypes.isCollectionType(typeName)) {
            kvList.set("type", NormalTypes.java2JsonType(typeName));
            if (!Strings.isNullOrEmpty(remark)) {
                kvList.set("description", remark);
            }
        } else {
            kvList.set(KV.by("type", "object"));
            kvList.set(KV.by("description", (Strings.isNullOrEmpty(remark) ? (name.trim()) : remark + " ," + name.trim())));
            if (!pNames.contains(name)) {
                List<String> requiredList = new ArrayList<>();
                kvList.set("properties", getFields(psiClass, project, childType, index, requiredList, pNames));
                kvList.set("required", requiredList);
                addFilePaths(filePaths, psiClass);
            } else {
                kvList.set(KV.by("type", name));
            }
        }
        KV<String, Object> kv1 = new KV<>();
        kv1.set(KV.by("type", "array"));
        kv1.set(KV.by("description", (Strings.isNullOrEmpty(remark) ? (name.trim()) : remark + " ," + name.trim())));
        kv1.set("items", kvList);
        return kv1;
    }

    /**
     * 添加到文件路径列表
     *
     * @author chengsheng@qbb6.com
     * @since 2019/5/6
     */
    private static boolean addFilePaths(Set<String> filePaths, PsiClass psiClass) {
        try {
            if (!filePaths.contains(((PsiJavaFileImpl) psiClass.getContext()).getViewProvider().getVirtualFile().getPath())) {
                filePaths.add(((PsiJavaFileImpl) psiClass.getContext()).getViewProvider().getVirtualFile().getPath());
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            try {
                if (!filePaths.contains(((ClsFileImpl) psiClass.getContext()).getViewProvider().getVirtualFile().getPath())) {
                    filePaths.add(((ClsFileImpl) psiClass.getContext()).getViewProvider().getVirtualFile().getPath());
                    return true;
                } else {
                    return false;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    /**
     * 转换文件路径
     *
     * @author chengsheng@qbb6.com
     * @since 2019/5/6
     */
    private static void changeFilePath(Project project) {
        Set<String> changeFilePaths = filePaths.stream().map(filePath -> {
            if (filePath.contains(".jar")) {
                String[] filePathsubs = filePath.split("\\.jar");
                String jarPath = filePathsubs[0] + "-sources.jar";
                try {
                    //去解压源码包
                    FileUnZipUtil.uncompress(new File(jarPath), new File(filePathsubs[0]));
                    filePath = filePathsubs[0] + filePathsubs[1].replace("!", "");
                    return filePath.replace(".class", ".java");
                } catch (IOException e) {
                    Notification error = notificationGroup.createNotification("can not find sources java:" + jarPath, NotificationType.ERROR);
                    Notifications.Bus.notify(error, project);
                }
            }
            return filePath;
        }).collect(Collectors.toSet());
        filePaths.clear();
        filePaths.addAll(changeFilePaths);
    }

    private static void getFilePath(Project project, Set<String> filePaths, List<PsiClass> psiClasses) {
        for (PsiClass psiClass : psiClasses) {
            if (addFilePaths(filePaths, psiClass)) {
                if (!psiClass.isEnum()) {
                    for (PsiField field : psiClass.getFields()) {
                        // 添加link 属性link
                        getFilePath(project, filePaths, DesUtil.getFieldLinks(project, field));
                        String fieldTypeName = field.getType().getPresentableText();
                        // 添加属性对象
                        if (field.getType() instanceof PsiArrayType) {
                            //array type
                            PsiType deepType = field.getType().getDeepComponentType();
                            String deepTypeName = deepType.getPresentableText();
                            if (!(deepType instanceof PsiPrimitiveType) && !NormalTypes.isNormalType(deepTypeName)) {
                                psiClass = PsiUtil.resolveClassInType(deepType);
                                getFilePath(project, filePaths, Collections.singletonList(psiClass));
                            }
                        } else if (fieldTypeName.startsWith("List") || fieldTypeName.startsWith("Set") || fieldTypeName.startsWith("HashSet")) {
                            //list type
                            PsiType iterableType = PsiUtil.extractIterableTypeParameter(field.getType(), false);
                            PsiClass iterableClass = PsiUtil.resolveClassInClassTypeOnly(iterableType);
                            if (Objects.nonNull(iterableClass)) {
                                String classTypeName = iterableClass.getName();
                                if (!NormalTypes.isNormalType(classTypeName) && !NormalTypes.isCollectionType(classTypeName)) {
                                    // addFilePaths(filePaths,iterableClass);
                                    getFilePath(project, filePaths, Collections.singletonList(iterableClass));
                                }
                            }
                        } else if (fieldTypeName.startsWith("HashMap") || fieldTypeName.startsWith("Map") || fieldTypeName.startsWith("LinkedHashMap")) {
                            //HashMap or Map
                            if (((PsiClassReferenceType) field.getType()).getParameters().length > 1) {
                                PsiClass hashClass = PsiUtil.resolveClassInType(((PsiClassReferenceType) field.getType()).getParameters()[1]);
                                getFilePath(project, filePaths, Collections.singletonList(hashClass));
                            }
                        } else if (!(field.getType() instanceof PsiPrimitiveType) && !NormalTypes.isNormalType(fieldTypeName) && !NormalTypes.isNormalType(field.getName())) {
                            //class type
                            psiClass = PsiUtil.resolveClassInType(field.getType());
                            // addFilePaths(filePaths,psiClass);
                            getFilePath(project, filePaths, Collections.singletonList(psiClass));
                        }
                    }
                }
            }
        }
    }

    @NotNull
    private static String getClassMenu(PsiClass selectedClass) {
        String className = selectedClass.getName();
        PsiElement context = selectedClass.getContext();
        String text = selectedClass.getText();
        if (context != null) {
            String classMenu = DesUtil.getMenu(context.getText().replace(text, ""));
            if (StringUtils.isNotBlank(classMenu)) {
                return classMenu + "-" + className;
            }
        }
        if (Objects.nonNull(selectedClass.getDocComment())) {
            String classMenu = DesUtil.getMenu(text);
            if (StringUtils.isNotBlank(classMenu)) {
                return classMenu + "-" + className;
            }
        }
        return DesUtil.camelToLine(className, null);
    }

}
