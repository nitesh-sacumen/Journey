/**
 * @author Sacumen(www.sacumen.com)
 * This class will create a forgerock token that will be used to call other
 * forgerock apis
 */

package com.journey.tree.util;

import com.journey.tree.config.Constants;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;
import java.util.Arrays;

public class ForgerockToken {
    private static final Logger logger = LoggerFactory.getLogger(ForgerockToken.class);

    public void createToken(String adminUsername, String adminPassword, TreeContext context) throws NodeProcessException {
        JsonValue sharedState = context.sharedState;
        String hostUrl = sharedState.get(Constants.FORGEROCK_HOST_URL).asString();
        HttpConnectionClient connection = new HttpConnectionClient();
        try (CloseableHttpClient httpClient = connection.getHttpClient(context)) {
            HttpPost httpPost = connection.createPostRequest(hostUrl + Constants.FORGEROCK_GET_TOKEN_URL);
            httpPost.addHeader("Accept", "application/json");
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("X-OpenAM-Username", adminUsername);
            httpPost.addHeader("X-OpenAM-Password", adminPassword);
            CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
            Integer responseCode = httpResponse.getStatusLine().getStatusCode();
            logger.debug("get forgerock token api response code is:: " + responseCode);
            System.out.println("get forgerock token api response code is:: " + responseCode);
            if (responseCode == 403) {
                logger.debug("invalid forgerock admin username/password combination");
                System.out.println("invalid forgerock admin username/password combination");
                throw new NodeProcessException("invalid forgerock admin username/password combination");
            }
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

        } catch (ConnectTimeoutException | SocketTimeoutException e) {
            logger.error(e.getMessage());
        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new NodeProcessException("Exception is: " + e);
        }
    }
}