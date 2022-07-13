package com.journey.tree.util;

import com.journey.tree.config.Constants;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpConnectionClient {
    private static final Logger logger = LoggerFactory.getLogger(HttpConnectionClient.class);

    public CloseableHttpClient getHttpClient(TreeContext context) {
        return buildDefaultClient(context);
    }

    public CloseableHttpClient buildDefaultClient(TreeContext context) {
        logger.debug("requesting http client connection client open");
        JsonValue sharedState = context.sharedState;
        Integer timeout = sharedState.get(Constants.REQUEST_TIMEOUT).asInteger();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 1000).setConnectionRequestTimeout(timeout * 1000).setSocketTimeout(timeout * 1000).build();
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        return clientBuilder.setDefaultRequestConfig(config).build();
    }

    public HttpPost createPostRequest(String url) {
        return new HttpPost(url);
    }
    public HttpGet createGetRequest(String url) {
        return new HttpGet(url);
    }
}
