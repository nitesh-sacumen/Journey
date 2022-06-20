package com.journey.tree.util;

import com.journey.tree.config.Constants;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JourneyGetAccessToken {
    private Logger logger = LoggerFactory.getLogger(JourneyGetAccessToken.class);

    public JSONObject getAccessToken(TreeContext context) {
        JSONObject jsonResponse = null;
        try (CloseableHttpClient httpclient = getHttpClient()) {
            JsonValue sharedState = context.sharedState;
            String refreshToken = sharedState.get(Constants.REFRESH_TOKEN).asString();
            HttpPost httpPost = createPostRequest(Constants.API_TOKEN_URL);
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("ttl", 1800000);//30 minutes

            httpPost.addHeader("Accept", "application/json");
            httpPost.addHeader("Content-Type", "application/json");

            httpPost.addHeader("Authorization", "Bearer " + refreshToken);

            StringEntity stringEntity = new StringEntity(jsonObj.toString());
            httpPost.setEntity(stringEntity);
            CloseableHttpResponse response = httpclient.execute(httpPost);
            Integer responseCode = response.getStatusLine().getStatusCode();
            logger.debug("access token api call response code is::" + responseCode);
            HttpEntity entityResponse = response.getEntity();
            String result = EntityUtils.toString(entityResponse);
            jsonResponse = new JSONObject(result);
        } catch (Exception e) {
            logger.error(e.getStackTrace().toString());
            e.printStackTrace();
        }
        return jsonResponse;
    }

    public CloseableHttpClient getHttpClient() {
        return buildDefaultClient();
    }

    public HttpPost createPostRequest(String url) {
        return new HttpPost(url);
    }

    public CloseableHttpClient buildDefaultClient() {
        logger.debug("requesting http client connection client open");
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        return clientBuilder.build();
    }
}
