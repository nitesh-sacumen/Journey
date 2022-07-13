/**
 * @author Sacumen(www.sacumen.com)
 * This class will create a journey access token that will be used
 * to call other journey apis
 */

package com.journey.tree.util;

import com.journey.tree.config.Constants;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class JourneyGetAccessToken {
    private final static Logger logger = LoggerFactory.getLogger(JourneyGetAccessToken.class);

    public JSONObject createAccessToken(TreeContext context, Integer timeToLive) throws NodeProcessException {
        JSONObject jsonResponse;
        Integer responseCode;
        HttpConnectionClient connection = new HttpConnectionClient();
        try (CloseableHttpClient httpclient = connection.getHttpClient(context)) {
            JsonValue sharedState = context.sharedState;
            String refreshToken = sharedState.get(Constants.REFRESH_TOKEN).asString();
            HttpPost httpPost = connection.createPostRequest(Constants.API_TOKEN_URL);
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("ttl", timeToLive);
            httpPost.addHeader("Accept", "application/json");
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("Authorization", "Bearer " + refreshToken);
            StringEntity stringEntity = new StringEntity(jsonObj.toString());
            httpPost.setEntity(stringEntity);
            CloseableHttpResponse response = httpclient.execute(httpPost);
            responseCode = response.getStatusLine().getStatusCode();
            logger.debug("journey access token api call response code is:: " + responseCode);
            HttpEntity entityResponse = response.getEntity();
            String result = EntityUtils.toString(entityResponse);
            jsonResponse = new JSONObject(result);
        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new NodeProcessException("Exception is: " + e);
        }
        return jsonResponse;
    }
}
