package com.qbb.util;

import com.google.gson.Gson;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class XUtils {

    private static final Logger logger = LoggerFactory.getLogger(XUtils.class);
    private static final Gson gson = new Gson();
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .addNetworkInterceptor(chain -> {
                Request request = chain.request();
                logger.debug("发起请求: {}", request.url());
                return chain.proceed(request);
            }).build();

    /**
     * 为 {@link Enumeration} 类创建一个 Stream
     */
    public static <T> Stream<T> stream(Enumeration<T> enumeration) {
        Iterator<T> iterator = asIterator(enumeration);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    /**
     * 为 {@link Enumeration} 类创建一个 Iterator
     */
    public static <T> Iterator<T> asIterator(Enumeration<T> enumeration) {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return enumeration.hasMoreElements();
            }

            @Override
            public T next() {
                return enumeration.nextElement();
            }
        };
    }

    /**
     * 对指定 url 发起一次 get 请求并将响应体视作 String
     *
     * @param url 请求的 url
     */
    public static String doGet(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return sendRequest(request);
    }

    /**
     * 对指定 url 发起一次 post 请求并将响应体视作 String
     *
     * @param url  请求的 url
     * @param body 请求体, 如果不是 String 类型将会自动使用 gson 将其转换成 json 字符串
     */
    public static String doPost(String url, Object body) throws IOException {
        String json = body instanceof String ? body.toString() : gson.toJson(body);
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json, MediaType.parse("application/json;charset=utf-8")))
                .build();
        return sendRequest(request);
    }

    /**
     * 对指定的 url 发起一次 post 请求上传指定路径的文件并将响应体视作 String
     *
     * @param url  请求的 url
     * @param path 要上传的文件的路径
     */
    public static String uploadFile(String url, String path) throws IOException {
        File file = new File(path);
        RequestBody body = new MultipartBody.Builder()
                .addFormDataPart("file", file.getName(), RequestBody.create(Files.readAllBytes(file.toPath())))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        return sendRequest(request);
    }

    /**
     * 执行一次请求并将响应体作为 String 返回
     *
     * @param request 请求对象
     */
    @NotNull
    private static String sendRequest(Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            return Objects.requireNonNull(response.body()).string();
        }
    }

}
