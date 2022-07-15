/**
 * @author Sacumen(www.sacumen.com)
 * This class will create an execution for enrollment/authentication
 * and will return execution id
 */

package com.journey.tree.util;

import com.journey.tree.config.Constants;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;

public class CreateExecution {
    private static final Logger logger = LoggerFactory.getLogger(CreateExecution.class);
    String pipelineKey, dashboardId = null, uniqueId, deviceId = null, forgeRockSessionId = null, apiAccessToken = null, phoneNumber = null,
            methodName = null, executionId = null;
    JSONObject delivery;
    HttpConnectionClient httpConnectionClient;

    @Inject
    public CreateExecution(HttpConnectionClient httpConnectionClient) {
        this.httpConnectionClient = httpConnectionClient;
    }

    public String execute(TreeContext context) throws NodeProcessException {
        initializeVariables(context);
        prepareDeliveryObject(context);
        executionId = createExecution(context);
        return executionId;
    }

    private void initializeVariables(TreeContext context) {
        JsonValue sharedState = context.sharedState;
        pipelineKey = sharedState.get(Constants.PIPELINE_KEY).asString();
        uniqueId = sharedState.get(Constants.UNIQUE_ID).asString();
        if (sharedState.get(Constants.DASHBOARD_ID).isNotNull()) {
            dashboardId = sharedState.get(Constants.DASHBOARD_ID).asString();
        }
        if (sharedState.get(Constants.DEVICE_ID).isNotNull()) {
            deviceId = sharedState.get(Constants.DEVICE_ID).asString();
        }
        if (sharedState.get(Constants.FORGEROCK_SESSION_ID).isNotNull()) {
            forgeRockSessionId = sharedState.get(Constants.FORGEROCK_SESSION_ID).asString();
        }
        if (sharedState.get(Constants.API_ACCESS_TOKEN).isNotNull()) {
            apiAccessToken = sharedState.get(Constants.API_ACCESS_TOKEN).asString();
        }
        if (sharedState.get(Constants.JOURNEY_PHONE_NUMBER).isNotNull()) {
            phoneNumber = sharedState.get(Constants.JOURNEY_PHONE_NUMBER).asString();
        }
        if (sharedState.get(Constants.METHOD_NAME).isNotNull()) {
            methodName = sharedState.get(Constants.METHOD_NAME).asString();
        }
    }

    private void prepareDeliveryObject(TreeContext context) throws NodeProcessException {
        JsonValue sharedState = context.sharedState;
        delivery = new JSONObject();
        try {
            if (methodName.equalsIgnoreCase(Constants.FACIAL_BIOMETRIC)) {
                if (sharedState.get(Constants.JOURNEY_PHONE_NUMBER).isNotNull() ||
                        sharedState.get(Constants.FORGEROCK_PHONE_NUMBER).isNotNull()) {
                    String phoneNumber = sharedState.get(Constants.JOURNEY_PHONE_NUMBER).isNotNull() ?
                            sharedState.get(Constants.JOURNEY_PHONE_NUMBER).asString() : sharedState.get(Constants.FORGEROCK_PHONE_NUMBER).asString();
                    delivery.put("method", "sms");
                    delivery.put("phoneNumber", phoneNumber);
                } else {
                    String email = sharedState.get(Constants.JOURNEY_EMAIL).isNotNull() ?
                            sharedState.get(Constants.JOURNEY_EMAIL).asString() : sharedState.get(Constants.FORGEROCK_EMAIL).asString();
                    delivery.put("method", "email");
                    delivery.put("email", email);
                }
            } else {
                delivery.put("method", "push-notification");
                delivery.put("deviceId", deviceId);
            }
        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new NodeProcessException("Exception is:" + e);
        }
    }

    private String createExecution(TreeContext context) throws NodeProcessException {
        JsonValue sharedState = context.sharedState;
        HttpEntity entityResponse;
        String result, customerJourneyId, executionId = null;
        JSONObject jsonResponse, jsonObj, customer, session;
        HttpPost httpPost;
        StringEntity stringEntity;
        CloseableHttpResponse response;
        ArrayList<String> urls;
        JSONArray callbackUrls;
        Integer responseCode;
        try (CloseableHttpClient httpclient = httpConnectionClient.getHttpClient(context)) {
            jsonObj = new JSONObject();
            jsonObj.put("pipelineKey", pipelineKey);
            jsonObj.put("delivery", delivery);
            customer = new JSONObject();
            customer.put("uniqueId", uniqueId);
            jsonObj.put("customer", customer);
            if (dashboardId != null) {
                jsonObj.put("dashboardId", dashboardId);
            }
            urls = new ArrayList<>();
            urls.add("https://localhost");
            callbackUrls = new JSONArray(urls);
            jsonObj.put("callbackUrls", callbackUrls);
            jsonObj.put("language", "en-US");
            session = new JSONObject();
            if (forgeRockSessionId != null) {
                session.put("externalRef", forgeRockSessionId);
            } else if (sharedState.get(Constants.CUSTOMER_JOURNEY_ID).isNotNull()) {
                customerJourneyId = sharedState.get(Constants.CUSTOMER_JOURNEY_ID).asString();
                session.put("id", customerJourneyId);
            }
            jsonObj.put("session", session);
            httpPost = httpConnectionClient.createPostRequest(Constants.CREATE_EXECUTION_URL);
            httpPost.addHeader("Accept", "application/json");
            httpPost.addHeader("Authorization", "Bearer " + apiAccessToken);
            httpPost.addHeader("Content-Type", "application/json");
            stringEntity = new StringEntity(jsonObj.toString());
            httpPost.setEntity(stringEntity);
            response = httpclient.execute(httpPost);
            responseCode = response.getStatusLine().getStatusCode();
            logger.debug("create execution response code: " + responseCode);
            entityResponse = response.getEntity();
            result = EntityUtils.toString(entityResponse);
            jsonResponse = new JSONObject(result);
            if (jsonResponse.has("errors")) {
                JSONObject errorObj = (JSONObject) jsonResponse.get("errors");
                logger.debug(errorObj.toString());
                throw new NodeProcessException("Api responded with errors, please check logs for errors.");
            }
            if (jsonResponse.has("id")) {
                executionId = (String) jsonResponse.get("id");
            }
        } catch (ConnectTimeoutException | SocketTimeoutException e) {
            logger.error(e.getMessage());
        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new NodeProcessException("Exception is: " + e);
        }
        return executionId;
    }
}
