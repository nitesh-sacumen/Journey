/**
 * @author Sacumen(www.sacumen.com)
 * This class will create a forgerock token that will be used to call other
 * forgerock apis
 */

package com.journey.tree.util;

import com.journey.tree.config.Constants;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class ForgerockToken {
    private static final Logger logger = LoggerFactory.getLogger(ForgerockToken.class);

    public void createToken(String adminUsername, String adminPassword, TreeContext context) throws NodeProcessException {
        JsonValue sharedState = context.sharedState;
        String hostUrl = sharedState.get(Constants.HOST_URL).asString();
        try (CloseableHttpClient httpClient = getHttpClient()) {
            HttpPost httpPost1 = createPostRequest(hostUrl + Constants.FORGEROCK_GET_TOKEN_URL);
            httpPost1.addHeader("Accept", "application/json");
            httpPost1.addHeader("Content-Type", "application/json");
            httpPost1.addHeader("X-OpenAM-Username", adminUsername);
            httpPost1.addHeader("X-OpenAM-Password", adminPassword);
            CloseableHttpResponse httpResponse = httpClient.execute(httpPost1);
            HttpEntity entity = httpResponse.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(entity);
                JSONObject jsonResponse = new JSONObject(result);
                if (jsonResponse.has("tokenId")) {
                    String tokenId = jsonResponse.getString("tokenId");
                    logger.debug("forgerock token created");
                    sharedState.put(Constants.TOKEN_ID, tokenId);
                }
            }

        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new NodeProcessException("Exception is: " + e);
        }
    }

    public CloseableHttpClient getHttpClient() {
        return buildDefaultClient();
    }

    public CloseableHttpClient buildDefaultClient() {
        Integer timeout = Constants.REQUEST_TIMEOUT;
        RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 1000).setConnectionRequestTimeout(timeout * 1000).setSocketTimeout(timeout * 1000).build();
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        return clientBuilder.setDefaultRequestConfig(config).build();
    }

    public HttpPost createPostRequest(String url) {
        return new HttpPost(url);
    }
}
