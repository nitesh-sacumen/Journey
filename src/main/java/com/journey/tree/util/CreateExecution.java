/**
 * @author Sacumen(www.sacumen.com)
 * This class will create an execution for enrollment/authentication
 * and will return execution id
 */

package com.journey.tree.util;

import com.journey.tree.config.Constants;
import org.apache.commons.lang.exception.ExceptionUtils;
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
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.SocketTimeoutException;

public class CreateExecution {
    private static final Logger logger = LoggerFactory.getLogger(CreateExecution.class);
    String pipelineKey, dashboardId = null, uniqueId, deviceId = null, apiAccessToken = null, journeyPhoneNumber = null,
            journeyEmail = null, forgerockPhoneNumber = null, forgerockEmail = null, methodName = null, executionId = null,
            forgerockSessionId = null, journeySessionId = null, externalRef;
    JSONObject delivery;
    HttpConnectionClient httpConnectionClient;

    @Inject
    public CreateExecution(HttpConnectionClient httpConnectionClient) {
        this.httpConnectionClient = httpConnectionClient;
    }

    public String execute(TreeContext context) throws NodeProcessException {
        initializeVariables(context);
        prepareDeliveryObject();
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
        if (sharedState.get(Constants.JOURNEY_API_TOKEN).isNotNull()) {
            apiAccessToken = sharedState.get(Constants.JOURNEY_API_TOKEN).asString();
        }
        if (sharedState.get(Constants.JOURNEY_PHONE_NUMBER).isNotNull()) {
            journeyPhoneNumber = sharedState.get(Constants.JOURNEY_PHONE_NUMBER).asString();
        }
        if (sharedState.get(Constants.JOURNEY_EMAIL).isNotNull()) {
            journeyEmail = sharedState.get(Constants.JOURNEY_EMAIL).asString();
        }
        if (sharedState.get(Constants.FORGEROCK_PHONE_NUMBER).isNotNull()) {
            forgerockPhoneNumber = sharedState.get(Constants.FORGEROCK_PHONE_NUMBER).asString();
        }
        if (sharedState.get(Constants.FORGEROCK_EMAIL).isNotNull()) {
            forgerockEmail = sharedState.get(Constants.FORGEROCK_EMAIL).asString();
        }
        if (sharedState.get(Constants.METHOD_NAME).isNotNull()) {
            methodName = sharedState.get(Constants.METHOD_NAME).asString();
        }
        if (sharedState.get(Constants.FORGEROCK_SESSION_ID).isNotNull()) {
            forgerockSessionId = sharedState.get(Constants.FORGEROCK_SESSION_ID).asString();
        }
        if (sharedState.get(Constants.JOURNEY_SESSION_ID).isNotNull()) {
            journeySessionId = sharedState.get(Constants.JOURNEY_SESSION_ID).asString();
        }

    }

    private void prepareDeliveryObject() throws NodeProcessException {
        delivery = new JSONObject();
        try {
            if (methodName.equalsIgnoreCase(Constants.FACIAL_BIOMETRIC)) {

                if (journeyPhoneNumber == null &&
                        journeyEmail == null &&
                        forgerockPhoneNumber == null &&
                        forgerockEmail == null) {
                    logger.debug("atleast one among journey phone number,journey email, forgerock phone number, forgerock email should have a value to proceed");
                    throw new NodeProcessException("atleast one among journey phone number,journey email, forgerock phone number, forgerock email should have a value to proceed");
                }

                if (journeyPhoneNumber != null ||
                        forgerockPhoneNumber != null) {
                    String phoneNumber = journeyPhoneNumber != null ?
                            journeyPhoneNumber : forgerockPhoneNumber;
                    delivery.put("method", "sms");
                    delivery.put("phoneNumber", phoneNumber);
                } else {
                    String email = journeyEmail != null ?
                            journeyEmail : forgerockEmail;
                    delivery.put("method", "email");
                    delivery.put("email", email);
                }
            } else if (methodName.equalsIgnoreCase(Constants.MOBILE_APP)) {
                if (deviceId == null) {
                    logger.debug("mobile device id is required to proceed");
                    throw new NodeProcessException("mobile device id is required to proceed");
                }
                delivery.put("method", "push-notification");
                delivery.put("deviceId", deviceId);
            }
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            throw new NodeProcessException("Exception is:" + e.getLocalizedMessage());
        }
    }

    private String createExecution(TreeContext context) throws NodeProcessException {
        HttpEntity entityResponse;
        String result, executionId = null;
        JSONObject jsonResponse, jsonObj, customer, sessionObject;
        HttpPost httpPost;
        StringEntity stringEntity;
        CloseableHttpResponse response;
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
            jsonObj.put("language", "en-US");
            if (forgerockSessionId != null || journeySessionId != null) {
                sessionObject = new JSONObject();
                externalRef = journeySessionId != null ? journeySessionId : forgerockSessionId;
                sessionObject.put("externalRef", externalRef);
                jsonObj.put("session", sessionObject);
            }
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
            if (responseCode != 260) {
                logger.debug("execution id not created as: " + jsonResponse);
                throw new NodeProcessException("execution id not created");
            }
            if (jsonResponse.has("id")) {
                executionId = (String) jsonResponse.get("id");
            }
        } catch (ConnectTimeoutException | SocketTimeoutException e) {
            logger.error(e.getMessage());
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            throw new NodeProcessException("Exception is: " + e);
        }
        return executionId;
    }
}
