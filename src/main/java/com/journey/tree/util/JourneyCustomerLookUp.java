package com.journey.tree.util;

import com.journey.tree.config.Constants;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JourneyCustomerLookUp {
    private Logger logger = LoggerFactory.getLogger(JourneyCustomerLookUp.class);

    public JSONObject customerLookUp(TreeContext context) {
        JSONObject jsonResponse = null;
        try (CloseableHttpClient httpclient = getHttpClient()) {
            JsonValue sharedState = context.sharedState;
            String username = sharedState.get("username").asString();
            String accountId = sharedState.get(Constants.ACCOUNT_ID).asString();
            String token = sharedState.get(Constants.API_ACCESS_TOKEN).asString();
            HttpGet httpGet = createGetRequest(Constants.ENROLLMENTS_CHECK_URL + "?unique_id=" + username + "&account_id=" + accountId);
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
            logger.error(e.getStackTrace().toString());
            e.printStackTrace();
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
        logger.debug("requesting http client connection client open");
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        return clientBuilder.build();
    }
}
