package com.qbb.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * http client 请求工具类
 *
 * @author haowentao
 * @date 2018/1/26.
 */
public class HttpClientUtil {
    private static final int socketTimeout = 10000;
    private static final int connectionTimeout = 10000;
    private static final int connectionRequestTimeout = 10000;
    private static final int maxTotal = 100;
    private static final int defaultMaxPerRoute = 100;

    private static final RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout)
            .setConnectTimeout(connectionTimeout)
            .setConnectionRequestTimeout(connectionRequestTimeout)
            .build();

    private static volatile CloseableHttpClient httpclient;
    private static volatile CloseableHttpClient tlsClient;//TLSv1.2协议对应client

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    private HttpClientUtil() {

    }

    /**
     * 初始化连接池
     */
    private static void init() throws Exception {
        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, (x509Certificates, s) -> true);
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                builder.build(),
                new String[]{"SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"},
                null, (hostname, session) -> true
        );
        Registry<ConnectionSocketFactory> socketFactoryRegistry
                = RegistryBuilder.<ConnectionSocketFactory>create().register("http",
                PlainConnectionSocketFactory.INSTANCE).register("https",
                sslsf).build();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        cm.setMaxTotal(maxTotal);
        cm.setDefaultMaxPerRoute(defaultMaxPerRoute);
        httpclient = HttpClients.custom()
                .setConnectionManager(cm).setDefaultRequestConfig(requestConfig)
                .build();

        //支持TLSv1.2协议
        Registry<ConnectionSocketFactory> tlsRegistry
                = RegistryBuilder.<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", new SSLConnectionSocketFactory(createIgnoreVerifySSL()))
                .build();
        PoolingHttpClientConnectionManager tlsCM = new PoolingHttpClientConnectionManager(tlsRegistry);
        tlsCM.setMaxTotal(maxTotal);
        tlsCM.setDefaultMaxPerRoute(defaultMaxPerRoute);
        tlsClient = HttpClients.custom()
                .setConnectionManager(tlsCM).setDefaultRequestConfig(requestConfig)
                .build();
    }

    public static CloseableHttpClient getHttpclient() {
        if (httpclient == null) {
            synchronized (HttpClientUtil.class) {
                if (httpclient == null) {
                    try {
                        init();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return httpclient;
    }

    public static CloseableHttpClient getTlsClient() {
        if (tlsClient == null) {
            synchronized (HttpClientUtil.class) {
                if (tlsClient == null) {
                    try {
                        init();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return tlsClient;
    }

    /**
     * 获取HttpGet
     */
    public static HttpGet getHttpGet(String url, String accept, String contentType) throws IOException {
        HttpGet httpGet;
        httpGet = new HttpGet(url);
        if (accept != null) {
            httpGet.setHeader("Accept", accept);
        }

        if (contentType != null) {
            httpGet.setHeader("Content-Type", contentType);
        }
        return httpGet;
    }

    /**
     * 执行 HttpPost 请求
     */
    public static CloseableHttpResponse executeHttpPost(HttpPost httpPost) throws IOException {
        return httpclient.execute(httpPost);
    }

    /**
     * 执行 HttpGet 请求
     */
    public static CloseableHttpResponse executeHttpGet(HttpGet httpGet) throws IOException {
        return httpclient.execute(httpGet);
    }

    /**
     * CloseableHttpResponse 转字符串
     */
    public static String ObjectToString(CloseableHttpResponse response, String charset) throws IOException {
        try {
            HttpEntity resEntity = response.getEntity();
            String responseBaby = null;
            if (resEntity != null) {
                responseBaby = EntityUtils.toString(resEntity, charset);
            }
            return responseBaby;
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    public static SSLContext createIgnoreVerifySSL() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sc = SSLContext.getInstance("TLSv1.2");

        // 实现一个X509TrustManager接口，用于绕过验证，不用修改里面的方法
        X509TrustManager trustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }
        };

        sc.init(null, new TrustManager[]{trustManager}, null);
        return sc;
    }

    @PreDestroy
    public void destroy() {
        try {
            httpclient.close();
            tlsClient.close();
        } catch (Exception e) {
            logger.error("httpclient pool destroy error :", e);
        }
    }

    /**
     * 以byte数组获取HttpPost
     */
    public HttpPost getHttpPost(byte[] content, String url, String accept, String contentType) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        if (accept != null) {
            httpPost.setHeader("Accept", accept);
        }

        if (contentType != null) {
            httpPost.setHeader("Content-type", contentType);
        }

        HttpEntity reqEntity = new ByteArrayEntity(content);
        httpPost.setEntity(reqEntity);
        return httpPost;
    }

    /**
     * 以json获取HttpPost
     */
    public HttpPost getHttpPost(String body, String url, String accept, String contentType, String charset) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        if (accept != null) {
            httpPost.setHeader("Accept", accept);
        }

        if (contentType != null) {
            httpPost.setHeader("Content-type", contentType);
        }

        HttpEntity reqEntity = new StringEntity(body == null ? "" : body, charset);
        httpPost.setEntity(reqEntity);
        return httpPost;
    }

    /**
     * 以map获取HttpPost
     */
    public HttpPost getHttpPost(Map<String, String> params, String url, String accept, String contentType, String charset) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        if (accept != null) {
            httpPost.setHeader("Accept", accept);
        }

        if (contentType != null) {
            httpPost.setHeader("Content-type", contentType);
        }

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String parameterName = entry.getKey();
            Object parameterValue = entry.getValue();
            nvps.add(new BasicNameValuePair(parameterName, String
                    .valueOf(parameterValue)));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(nvps, charset));
        return httpPost;
    }

    /**
     * 执行 tlsHttpPost 请求
     */
    public CloseableHttpResponse executeTlsHttpPost(HttpPost httpPost) throws IOException {
        return tlsClient.execute(httpPost);
    }

    /**
     * 执行 tlsHttpGet 请求
     */
    public CloseableHttpResponse executeTlsHttpGet(HttpGet httpGet) throws IOException {
        return tlsClient.execute(httpGet);
    }

    /**
     * CloseableHttpResponse 转byte 数组
     */
    public byte[] ObjectToByte(CloseableHttpResponse response) throws IOException {
        try {
            HttpEntity resEntity = response.getEntity();
            if (resEntity != null) {
                InputStream inputStream = resEntity.getContent();
                if (inputStream != null) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int n = 0;
                    while (-1 != (n = inputStream.read(buffer))) {
                        output.write(buffer, 0, n);
                    }
                    return output.toByteArray();
                }
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }

    private static class StringResponseHandler implements
            ResponseHandler<String> {

        private final String charset;

        public StringResponseHandler(String charset) {
            this.charset = charset;
        }

        @Override
        public String handleResponse(HttpResponse response)
                throws IOException {
            StatusLine statusLine = response.getStatusLine();
            HttpEntity entity = response.getEntity();
            if (statusLine.getStatusCode() >= 300) {
                EntityUtils.consume(entity);
                throw new HttpResponseException(statusLine.getStatusCode(),
                        statusLine.getReasonPhrase());
            }
            if (entity != null) {
                return (charset == null) ? EntityUtils.toString(entity)
                        : EntityUtils.toString(entity, charset);
            } else {
                return null;
            }
        }

    }


}
