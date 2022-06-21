package com.journey.tree.util;

import com.journey.tree.config.Constants;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class CreateExecution {
    private Logger logger = LoggerFactory.getLogger(CreateExecution.class);
    String pipelineKey, dashboardId = "", username, deviceId = "", forgeRockSessionId = "", apiAccessToken = "", phoneNumber = "", methodName = "", executionId = "";
    JSONObject delivery;

    public String execute(TreeContext context) {
        JsonValue sharedState = context.sharedState;
        try {
            pipelineKey = sharedState.get(Constants.PIPELINE_KEY).asString();
            username = sharedState.get("username").asString();
            if (sharedState.get(Constants.DASHBOARD_ID).isNotNull()) {
                dashboardId = sharedState.get(Constants.DASHBOARD_ID).asString();
            }
            if (sharedState.get(Constants.DEVICE_ID).isNotNull()) {
                deviceId = sharedState.get(Constants.DEVICE_ID).asString();//code part remaining in enrollment status node
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
            if (methodName.equalsIgnoreCase(Constants.FACIAL_BIOMETRIC) || methodName.equalsIgnoreCase(Constants.ONE_TIME_PASSWORD)) {
                if (phoneNumber == "") {
                    if (sharedState.get(Constants.FORGEROCK_PHONE_NUMBER).isNotNull()) {
                        phoneNumber = sharedState.get(Constants.FORGEROCK_PHONE_NUMBER).asString();
                    } else {
                        //apply code for user input phone number
                    }
                    //here apply login to get phone number from foregrock if not avaialble there then user input
                }
                delivery.put("method", "sms");
                delivery.put("phoneNumber", phoneNumber);
            } else if (methodName.equalsIgnoreCase(Constants.MOBILE_APP) && deviceId != "") {
                delivery.put("method", "push-notification");
                delivery.put("deviceId", deviceId);
            }

        } catch (Exception e) {
            logger.error(e.getStackTrace().toString());
            e.printStackTrace();
        }
        HttpEntity entityResponse;
        String result, customerJourneyId = "";
        JSONObject jsonResponse;
        JSONObject jsonObj, customer, session;
        HttpPost httpPost;
        StringEntity stringEntity;
        CloseableHttpResponse response;
        ArrayList<String> urls;
        JSONArray callbackUrls;
        try (CloseableHttpClient httpclient = getHttpClient()) {
            jsonObj = new JSONObject();
            jsonObj.put("pipelineKey", pipelineKey);
            jsonObj.put("delivery", delivery);
            customer = new JSONObject();
            System.out.println("username is " + username);
            customer.put("uniqueId", username);
            jsonObj.put("customer", customer);
            if (dashboardId != "") {
                jsonObj.put("dashboardId", dashboardId);
            }
            urls = new ArrayList<>();
            urls.add("https://localhost");//remove this code later
            callbackUrls = new JSONArray(urls);
            jsonObj.put("callbackUrls", callbackUrls);
            jsonObj.put("language", "en-US");
            session = new JSONObject();
            if (sharedState.get(Constants.FORGEROCK_SESSION_ID).isNotNull()) {
                forgeRockSessionId = sharedState.get(Constants.FORGEROCK_SESSION_ID).asString();
                session.put("externalRef", forgeRockSessionId);
            } else if (sharedState.get(Constants.CUSTOMER_JOURNEY_ID).isNotNull()) {
                customerJourneyId = sharedState.get(Constants.CUSTOMER_JOURNEY_ID).asString();
                session.put("id", customerJourneyId);
                System.out.println("customer journey id is " + customerJourneyId);//remove this line
            }
            httpPost = createPostRequest(Constants.CREATE_EXECUTION_URL);
            httpPost.addHeader("Accept", "application/json");
            httpPost.addHeader("Authorization", "Bearer " + apiAccessToken);
            httpPost.addHeader("Content-Type", "application/json");
            System.out.println(jsonObj.toString(4));
            stringEntity = new StringEntity(jsonObj.toString());
            httpPost.setEntity(stringEntity);
            response = httpclient.execute(httpPost);
            Integer responseCode = response.getStatusLine().getStatusCode();
            System.out.println("create execution response code: " + responseCode);
            if (response != null) {
                entityResponse = response.getEntity();
                result = EntityUtils.toString(entityResponse);
                jsonResponse = new JSONObject(result);
                System.out.println(jsonResponse.toString(4));
                if (jsonResponse != null && jsonResponse.has("id")) {
                    executionId = (String) jsonResponse.get("id");
                }
            }
        } catch (Exception e) {
            logger.error(e.getStackTrace().toString());
            e.printStackTrace();
        }
        return executionId;
    }

    public CloseableHttpClient getHttpClient() {
        return buildDefaultClient();
    }

    public HttpPost createPostRequest(String url) {
        return new HttpPost(url);
    }

    public CloseableHttpClient buildDefaultClient() {
        logger.debug("requesting http client connection client open");

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        return clientBuilder.build();
    }
}
