package com.journey.tree.util;

import com.journey.tree.config.Constants;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
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

import java.util.ArrayList;
import java.util.Arrays;

public class CreateExecution {
    private static final Logger logger = LoggerFactory.getLogger(CreateExecution.class);
    String pipelineKey, dashboardId = null, uniqueId, deviceId = null, forgeRockSessionId = null, apiAccessToken = null, phoneNumber = null,
            methodName = null, executionId = null;
    JSONObject delivery;

    public String execute(TreeContext context) throws NodeProcessException {
        JsonValue sharedState = context.sharedState;
        try {

            //fetching details from the shared memory

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
            delivery = new JSONObject();

            //this code block will only execute if the method received is either
            // facial-biometrics or one-time-password (only for testing) as both will have
            // sms as method name and will be requiring phone number

            if (methodName.equalsIgnoreCase(Constants.FACIAL_BIOMETRIC)) {

                //if the phone number is not present in the customer look up call,
                // then it will be fetched from the forgerock user profile

                if (phoneNumber == null) {
                    if (sharedState.get(Constants.FORGEROCK_PHONE_NUMBER).isNotNull()) {
                        phoneNumber = sharedState.get(Constants.FORGEROCK_PHONE_NUMBER).asString();
                    } else {
                        logger.debug("phone number is not available");
                        throw new NodeProcessException("phone number is not available");
                    }
                }

                //setting delivery object which will have method name as sms and phone number

//                delivery.put("method", "sms");
//                delivery.put("phoneNumber", phoneNumber);

                String email = null;
                if (sharedState.get(Constants.JOURNEY_EMAIL).isNotNull()) {
                    email = sharedState.get(Constants.JOURNEY_EMAIL).asString();
                } else if (sharedState.get(Constants.FORGEROCK_EMAIL).isNotNull()) {
                    email = sharedState.get(Constants.FORGEROCK_EMAIL).asString();
                }
                //hard coded for testing purpose
                delivery.put("method", "email");
                delivery.put("email", email);
            }

            //this code block will only execute if the method name is mobile app and device id is present

            else if (methodName.equalsIgnoreCase(Constants.MOBILE_APP) && deviceId != null) {
                delivery.put("method", "push-notification");
                delivery.put("deviceId", deviceId);
            } else {
                logger.debug("no device id found");
                throw new NodeProcessException("no device id found");
            }

        }

        //this code block will handle any exception

        catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new NodeProcessException("Exception is: " + e);
        }
        HttpEntity entityResponse;
        String result, customerJourneyId;
        JSONObject jsonResponse;
        JSONObject jsonObj, customer, session;
        HttpPost httpPost;
        StringEntity stringEntity;
        CloseableHttpResponse response;
        ArrayList<String> urls;
        JSONArray callbackUrls;
        try (CloseableHttpClient httpclient = getHttpClient()) {
            jsonObj = new JSONObject();

            //putting the data into the json object
            //preparing payload for create execution api

            jsonObj.put("pipelineKey", pipelineKey);
            jsonObj.put("delivery", delivery);
            customer = new JSONObject();
            customer.put("uniqueId", uniqueId);
            jsonObj.put("customer", customer);
            if (dashboardId != null) {
                jsonObj.put("dashboardId", dashboardId);
            }
            urls = new ArrayList<>();

            //providing empty array will give regex error of missing https, so given localhost address
            // as the input to callback url

            urls.add("https://localhost");
            callbackUrls = new JSONArray(urls);
            jsonObj.put("callbackUrls", callbackUrls);
            jsonObj.put("language", "en-US");
            session = new JSONObject();

            //if the forge rock session id is not empty, it will be passed as payload

            if (forgeRockSessionId != null) {
                session.put("externalRef", forgeRockSessionId);
            }

            //if the customer journey id is not empty, it will be passed as payload

            else if (sharedState.get(Constants.CUSTOMER_JOURNEY_ID).isNotNull()) {
                customerJourneyId = sharedState.get(Constants.CUSTOMER_JOURNEY_ID).asString();
                session.put("id", customerJourneyId);
            }
            jsonObj.put("session", session);
            httpPost = createPostRequest(Constants.CREATE_EXECUTION_URL);

            //headers added for the create execution api

            httpPost.addHeader("Accept", "application/json");
            httpPost.addHeader("Authorization", "Bearer " + apiAccessToken);
            httpPost.addHeader("Content-Type", "application/json");
            stringEntity = new StringEntity(jsonObj.toString());
            httpPost.setEntity(stringEntity);

            //here call has been made to create execution api

            response = httpclient.execute(httpPost);

            //capturing and logging the create execution api response code

            Integer responseCode = response.getStatusLine().getStatusCode();
            logger.debug("create execution response code: " + responseCode);

            //this code block will execute only if the create execution api has a response

            entityResponse = response.getEntity();
            result = EntityUtils.toString(entityResponse);
            jsonResponse = new JSONObject(result);

            //this block will check and fetch execution id from the api response

            if (jsonResponse.has("id")) {
                executionId = (String) jsonResponse.get("id");
            }

        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new NodeProcessException("Exception is: " + e);
        }

        //execution id will be returned to the caller method

        return executionId;
    }

    public CloseableHttpClient getHttpClient() {
        return buildDefaultClient();
    }

    public HttpPost createPostRequest(String url) {
        return new HttpPost(url);
    }

    public CloseableHttpClient buildDefaultClient() {
        Integer timeout = Constants.REQUEST_TIMEOUT;
        RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 1000).setConnectionRequestTimeout(timeout * 1000).setSocketTimeout(timeout * 1000).build();
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        return clientBuilder.setDefaultRequestConfig(config).build();
    }
}
