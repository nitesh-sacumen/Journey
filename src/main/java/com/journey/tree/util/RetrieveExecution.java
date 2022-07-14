/**
 * @author Sacumen(www.sacumen.com)
 * This class will check for an enrollment/authentication status which could be
 * either of completed/failed/timeout
 */

package com.journey.tree.util;

import com.journey.tree.config.Constants;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
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
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

public class RetrieveExecution {
    private static final Logger logger = LoggerFactory.getLogger(RetrieveExecution.class);
    String apiAccessToken = null;
    String result = null;
    Boolean isCompleted = false;
    Boolean isFailed = false;
    Integer responseCode;
    HttpConnectionClient httpConnectionClient;

    @Inject
    public RetrieveExecution(HttpConnectionClient httpConnectionClient) {
        this.httpConnectionClient = httpConnectionClient;
    }

    public String retrieve(TreeContext context, String executionId) throws NodeProcessException {
        JsonValue sharedState = context.sharedState;
        if (sharedState.get(Constants.API_ACCESS_TOKEN).isNotNull()) {
            apiAccessToken = sharedState.get(Constants.API_ACCESS_TOKEN).asString();
        }
        Integer retrieveTimeout = sharedState.get(Constants.RETRIEVE_TIMEOUT).asInteger();
        Integer retrieveDelay = sharedState.get(Constants.RETRIEVE_DELAY).asInteger();
        Boolean flag;
        sharedState.put(Constants.RETRIEVE_API_CONNECTION, true);
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new NodeProcessException("Exception is: " + e);
        }
        for (Integer j = 1; j <= retrieveTimeout; j++) {
            flag = checkExecutionResult(context, executionId);
            if (flag) {
                break;
            }
            try {
                Thread.sleep(retrieveDelay);
            } catch (Exception e) {
                logger.error(Arrays.toString(e.getStackTrace()));
            }
        }
        sharedState.put(Constants.RETRIEVE_API_CONNECTION, false);
        if (isCompleted) {
            return Constants.EXECUTION_COMPLETED;
        } else if (isFailed) {
            return Constants.EXECUTION_FAILED;
        } else {
            return Constants.EXECUTION_TIMEOUT;
        }
    }

    private Boolean checkExecutionResult(TreeContext context, String executionId) throws NodeProcessException {
        HttpGet httpGet = httpConnectionClient.createGetRequest(Constants.EXECUTION_RETRIEVE + executionId);
        httpGet.addHeader("Authorization", "Bearer " + apiAccessToken);
        httpGet.addHeader("Accept", "application/json");
        try (CloseableHttpClient httpclient = httpConnectionClient.getHttpClient(context)) {
            CloseableHttpResponse response = httpclient.execute(httpGet);
            responseCode = response.getStatusLine().getStatusCode();
            logger.debug("execution retrieve api response code is: " + responseCode);
            HttpEntity entityResponse = response.getEntity();
            result = EntityUtils.toString(entityResponse);
            if (result != null) {
                JSONObject jsonResponse = new JSONObject(result);
                if (jsonResponse.has("errors")) {
                    JSONObject errorObj = (JSONObject) jsonResponse.get("errors");
                    logger.debug(errorObj.toString());
                    throw new NodeProcessException("Api responded with errors, please check logs for errors.");
                }
                if (jsonResponse.has("completedAt") || jsonResponse.has("failedAt")) {
                    Boolean flag;
                    String date;
                    if (jsonResponse.has("completedAt")) {
                        date = jsonResponse.getString("completedAt");
                        flag = checkDate(date);
                        if (flag) {
                            isCompleted = true;
                            return true;
                        }
                    }
                    if (jsonResponse.has("failedAt")) {
                        date = jsonResponse.getString("failedAt");
                        flag = checkDate(date);
                        if (flag) {
                            isFailed = true;
                            return true;
                        }
                    }
                }
            }
        } catch (ConnectTimeoutException | SocketTimeoutException e) {
            logger.error(e.getMessage());
        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new NodeProcessException("Exception is: " + e);
        }
        return false;
    }

    private Boolean checkDate(String date) {
        try {
            Instant.from(DateTimeFormatter.ISO_INSTANT.parse(date));
            return true;
        } catch (DateTimeParseException e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
        return false;
    }
}
