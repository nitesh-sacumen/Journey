package com.journey.tree.util;

import com.journey.tree.config.Constants;
import com.sun.identity.authentication.client.AuthClientUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class UserDetails {
    private final static Logger logger = LoggerFactory.getLogger(UserDetails.class);

    public Boolean getDetails(TreeContext context, String tokenId, String groupName, String username) throws NodeProcessException {
        String cookieName = null, universalId, sessionId;
        JSONObject propertiesObject;
        JSONArray adminMemberList;
        JsonValue sharedState = context.sharedState;
        try (CloseableHttpClient httpClient = getHttpClient()) {
            HttpGet httpGet = createGetRequest(Constants.FORGEROCK_GET_SERVER_INFO_URL);
            httpGet.addHeader("Accept", "application/json");
            httpGet.addHeader("Content-Type", "application/json");
            httpGet.addHeader("Accept-API-Version", "protocol=1.0,resource=1.1");
            CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity entity = httpResponse.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(entity);
                JSONObject jsonResponse = new JSONObject(result);
                if (jsonResponse.has("cookieName")) {
                    cookieName = jsonResponse.getString("cookieName");
                    logger.debug("cookie name fetched");
                    sharedState.put(Constants.COOKIE_NAME, cookieName);
                }
            }

        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new NodeProcessException("Exception is: " + e);
        }
        if (cookieName != null) {
            try (CloseableHttpClient httpClient = getHttpClient()) {
                HttpPost httpPost1 = createPostRequest(Constants.FORGEROCK_GET_SESSION_INFO_URL);
                httpPost1.addHeader("Accept", "application/json");
                httpPost1.addHeader("Content-Type", "application/json");
                httpPost1.addHeader("Cookie", cookieName + "=" + tokenId);
                httpPost1.addHeader("Accept-API-Version", "protocol=1.0,resource=3.0");
                CloseableHttpResponse httpResponse = httpClient.execute(httpPost1);
                HttpEntity entity = httpResponse.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    JSONObject jsonResponse = new JSONObject(result);
                    if (jsonResponse.has("universalId")) {
                        universalId = jsonResponse.getString("universalId");
                        sharedState.put(Constants.FORGEROCK_ID, universalId);
                    }
                    if (jsonResponse.has("properties")) {
                        propertiesObject = jsonResponse.getJSONObject("properties");
                        if (propertiesObject != null && propertiesObject.has("AMCtxId")) {
                            sessionId = propertiesObject.getString("AMCtxId");
                            logger.debug("session id fetched");
                            sharedState.put(Constants.FORGEROCK_SESSION_ID, sessionId);
                        }
                    }
                }

            } catch (Exception e) {
                logger.error(Arrays.toString(e.getStackTrace()));
                throw new NodeProcessException("Exception is: " + e);
            }
            try (CloseableHttpClient httpClient = getHttpClient()) {
                HttpGet httpGet = createGetRequest(Constants.FORGEROCK_GET_GROUP_MEMBERS_URL + groupName);
                httpGet.addHeader("Accept", "application/json");
                httpGet.addHeader("Content-Type", "application/json");
                httpGet.addHeader("Cookie", cookieName + "=" + tokenId);
                CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
                HttpEntity entity = httpResponse.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    JSONObject jsonResponse = new JSONObject(result);
                    if (jsonResponse.has("members")) {
                        JSONObject membersObject = jsonResponse.getJSONObject("members");
                        if (membersObject.has("uniqueMember")) {
                            adminMemberList = membersObject.getJSONArray("uniqueMember");
                            logger.debug("group member list fetched");
                            for (Integer i = 0; i < adminMemberList.length(); i++) {
                                if (adminMemberList.get(i).equals(username)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(Arrays.toString(e.getStackTrace()));
                throw new NodeProcessException("Exception is: " + e);
            }
        }
        return false;
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

    public HttpGet createGetRequest(String url) {
        return new HttpGet(url);
    }
}
