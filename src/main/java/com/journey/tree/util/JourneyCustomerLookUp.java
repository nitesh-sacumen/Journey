/**
 * @author Sacumen(www.sacumen.com)
 * This class will make customer lookup api call and will store the response
 */

package com.journey.tree.util;

import com.journey.tree.config.Constants;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
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

public class JourneyCustomerLookUp {
    private static final Logger logger = LoggerFactory.getLogger(JourneyCustomerLookUp.class);
    HttpConnectionClient httpConnectionClient;

    @Inject
    public JourneyCustomerLookUp(HttpConnectionClient httpConnectionClient) {
        this.httpConnectionClient = httpConnectionClient;
    }

    public void customerLookUp(TreeContext context) throws NodeProcessException {
        JSONObject jsonResponse;
        Integer responseCode;
        JsonValue sharedState = context.sharedState;
        try (CloseableHttpClient httpclient = httpConnectionClient.getHttpClient(context)) {
            String uniqueId = sharedState.get(Constants.UNIQUE_ID).asString();
            String accountId = sharedState.get(Constants.JOURNEY_ACCOUNT_ID).asString();
            String token = sharedState.get(Constants.JOURNEY_API_TOKEN).asString();
            HttpGet httpGet = httpConnectionClient.createGetRequest(Constants.ENROLLMENTS_CHECK_URL + "?unique_id=" + uniqueId + "&account_id=" + accountId);
            httpGet.addHeader("Authorization", "Bearer " + token);
            httpGet.addHeader("Accept", "application/json");
            CloseableHttpResponse response = httpclient.execute(httpGet);
            responseCode = response.getStatusLine().getStatusCode();
            sharedState.put(Constants.CUSTOMER_LOOKUP_RESPONSE_CODE, responseCode);
            logger.debug("journey customer look up api call response code is::" + responseCode);
            HttpEntity entityResponse = response.getEntity();
            String result = EntityUtils.toString(entityResponse);
            jsonResponse = new JSONObject(result);
            if (responseCode != 262) {
                logger.debug("customer details not fetched as: " + jsonResponse);
            }
            if (jsonResponse.has("errors")) {
                JSONObject errorObj = (JSONObject) jsonResponse.get("errors");
                logger.debug(errorObj.toString());
                throw new NodeProcessException("Api responded with errors, please check logs for errors.");
            }
            populateJourneyCustomerDetails(context, jsonResponse);
            sharedState.put(Constants.JOURNEY_USER, jsonResponse.toString());
        } catch (ConnectTimeoutException | SocketTimeoutException e) {
            logger.error(e.getMessage());
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            throw new NodeProcessException("Exception is: " + e);
        }
    }

    private void populateJourneyCustomerDetails(TreeContext context, JSONObject jsonResponse) {
        JsonValue sharedState = context.sharedState;
        try {
            if (jsonResponse.has("phoneNumbers")) {
                JSONArray phoneNumbers = (JSONArray) jsonResponse.get("phoneNumbers");
                if (phoneNumbers.length() > 0) {
                    String phoneNumber = phoneNumbers.getString(0);
                    sharedState.put(Constants.JOURNEY_PHONE_NUMBER, phoneNumber);
                }
            }
            if (jsonResponse.has("devices")) {
                JSONArray devices = (JSONArray) jsonResponse.get("devices");
                if (devices.length() > 0) {
                    JSONObject deviceObject = devices.getJSONObject(0);
                    if (deviceObject.has("id")) {
                        String deviceId = deviceObject.getString("id");
                        sharedState.put(Constants.DEVICE_ID, deviceId);
                    }
                }
            }
            if (jsonResponse.has("email")) {
                String email = jsonResponse.getString("email");
                sharedState.put(Constants.JOURNEY_EMAIL, email);
            }
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
    }
}
