/**
 * @author Sacumen(www.sacumen.com)
 * This class will call various forgerock apis to fetch cookiename, session id,
 * provided admin group details
 */

package com.journey.tree.util;

import com.journey.tree.config.Constants;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
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
    String hostUrl, cookieName = null, universalId, sessionId;

    public Boolean getDetails(TreeContext context, String tokenId, String groupName, String username) throws NodeProcessException {
        JsonValue sharedState = context.sharedState;
        hostUrl = sharedState.get(Constants.FORGEROCK_HOST_URL).asString();
        getForgerockCookieName(context);
        Boolean isAdmin = false;
        if (cookieName != null) {
            getForgerockSessionId(context, tokenId);
            isAdmin = getForgerockGroupDetails(context, tokenId, username, groupName);
        }
        return isAdmin;
    }

    private void getForgerockCookieName(TreeContext context) throws NodeProcessException {
        JsonValue sharedState = context.sharedState;
        HttpConnectionClient connection = new HttpConnectionClient();
        try (CloseableHttpClient httpClient = connection.getHttpClient(context)) {
            HttpGet httpGet = connection.createGetRequest(hostUrl + Constants.FORGEROCK_GET_SERVER_INFO_URL);
            httpGet.addHeader("Accept", "application/json");
            httpGet.addHeader("Content-Type", "application/json");
            httpGet.addHeader("Accept-API-Version", "protocol=1.0,resource=1.1");
            CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
            Integer responseCode = httpResponse.getStatusLine().getStatusCode();
            logger.debug("get forgerock cookie name api response code is:: " + responseCode);
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
    }

    private void getForgerockSessionId(TreeContext context, String tokenId) throws NodeProcessException {
        JsonValue sharedState = context.sharedState;
        HttpConnectionClient connection = new HttpConnectionClient();
        try (CloseableHttpClient httpClient = connection.getHttpClient(context)) {
            HttpPost httpPost = connection.createPostRequest(hostUrl + Constants.FORGEROCK_GET_SESSION_INFO_URL);
            httpPost.addHeader("Accept", "application/json");
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("Cookie", cookieName + "=" + tokenId);
            httpPost.addHeader("Accept-API-Version", "protocol=1.0,resource=3.0");
            CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
            Integer responseCode = httpResponse.getStatusLine().getStatusCode();
            logger.debug("get forgerock session details api response code is:: " + responseCode);
            HttpEntity entity = httpResponse.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(entity);
                JSONObject jsonResponse = new JSONObject(result);
                if (jsonResponse.has("universalId")) {
                    universalId = jsonResponse.getString("universalId");
                    sharedState.put(Constants.FORGEROCK_ID, universalId);
                }
                if (jsonResponse.has("properties")) {
                    JSONObject propertiesObject = jsonResponse.getJSONObject("properties");
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
    }

    private Boolean getForgerockGroupDetails(TreeContext context, String tokenId, String username, String groupName) throws NodeProcessException {
        JSONArray adminMemberList;
        HttpConnectionClient connection = new HttpConnectionClient();
        try (CloseableHttpClient httpClient = connection.getHttpClient(context)) {
            HttpGet httpGet = connection.createGetRequest(hostUrl + Constants.FORGEROCK_GET_GROUP_MEMBERS_URL + groupName);
            httpGet.addHeader("Accept", "application/json");
            httpGet.addHeader("Content-Type", "application/json");
            httpGet.addHeader("Cookie", cookieName + "=" + tokenId);
            CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
            Integer responseCode = httpResponse.getStatusLine().getStatusCode();
            logger.debug("get forgerock group details api response code is:: " + responseCode);
            if (responseCode == 404) {
                logger.debug("forgerock group cannot be found");
                throw new NodeProcessException("forgerock group cannot be found");
            }
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
        return false;
    }
}
