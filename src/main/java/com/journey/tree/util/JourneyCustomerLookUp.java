package com.journey.tree.util;

import com.journey.tree.config.Constants;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
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

public class JourneyCustomerLookUp {
    private static final Logger logger = LoggerFactory.getLogger(JourneyCustomerLookUp.class);

    public JSONObject customerLookUp(TreeContext context) throws NodeProcessException {
        JSONObject jsonResponse = null;
        try (CloseableHttpClient httpclient = getHttpClient()) {
            JsonValue sharedState = context.sharedState;
            String uniqueId = sharedState.get(Constants.UNIQUE_ID).asString();
            String accountId = sharedState.get(Constants.ACCOUNT_ID).asString();
            String token = sharedState.get(Constants.API_ACCESS_TOKEN).asString();
            HttpGet httpGet = createGetRequest(Constants.ENROLLMENTS_CHECK_URL + "?unique_id=" + uniqueId + "&account_id=" + accountId);
            httpGet.addHeader("Authorization", "Bearer " + token);
            httpGet.addHeader("Accept", "application/json");
            CloseableHttpResponse response = httpclient.execute(httpGet);
            Integer responseCode = response.getStatusLine().getStatusCode();
            sharedState.put(Constants.CUSTOMER_LOOKUP_RESPONSE_CODE, responseCode);
            logger.debug("customer look up api call response code is::" + responseCode);
            HttpEntity entityResponse = response.getEntity();
            String result = EntityUtils.toString(entityResponse);
            jsonResponse = new JSONObject(result);

        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new NodeProcessException("Exception is: " + e);
        }

        return jsonResponse;
    }

    public CloseableHttpClient getHttpClient() {
        return buildDefaultClient();
    }

    public HttpGet createGetRequest(String url) {
        return new HttpGet(url);
    }

    public CloseableHttpClient buildDefaultClient() {
        Integer timeout = Constants.REQUEST_TIMEOUT;
        RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 1000).setConnectionRequestTimeout(timeout * 1000).setSocketTimeout(timeout * 1000).build();
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        return clientBuilder.setDefaultRequestConfig(config).build();
    }
}
