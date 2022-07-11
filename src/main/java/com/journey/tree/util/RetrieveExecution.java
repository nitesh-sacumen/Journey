package com.journey.tree.util;

import com.journey.tree.config.Constants;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

public class RetrieveExecution {
    private static final Logger logger = LoggerFactory.getLogger(RetrieveExecution.class);
    String apiAccessToken = "";
    String result = null;
    CloseableHttpResponse response;
    JSONObject jsonResponse;
    Boolean isCompleted = false;
    Boolean isFailed = false;
    Integer responseCode;

    public String retrieve(TreeContext context, String executionId) {
        JsonValue sharedState = context.sharedState;

        //this code block will make maximum 60 repetitive calls after every 2 seconds delay to retrieve execution api
        // to check if completedAt or failedAt is present in api response

        try (CloseableHttpClient httpclient = getHttpClient()) {
            if (sharedState.get(Constants.API_ACCESS_TOKEN).isNotNull()) {
                apiAccessToken = sharedState.get(Constants.API_ACCESS_TOKEN).asString();
            }

            //appending execution id as a query parameter for retrieve api endpoint

            HttpGet httpGet = createGetRequest(Constants.EXECUTION_RETRIEVE + executionId);

            //setting headers for retrieve execution api

            httpGet.addHeader("Authorization", "Bearer " + apiAccessToken);
            httpGet.addHeader("Accept", "application/json");

            //this statement will delay the first call to retrieve execution api for a span of 3 seconds

            Thread.sleep(3000);

            Integer retrieveTimeout=sharedState.get(Constants.RETRIEVE_TIMEOUT).asInteger();
            Integer retrieveDelay=sharedState.get(Constants.RETRIEVE_DELAY).asInteger();

            //in this loop retrieve api will be called at max for
            // retrieveTimeout value times at the interval of retrieveDelay seconds

            for (Integer j = 1; j <= retrieveTimeout; j++) {
                CloseableHttpResponse response = httpclient.execute(httpGet);
                responseCode = response.getStatusLine().getStatusCode();
                logger.debug("retrieve api response code is: " + responseCode);
                HttpEntity entityResponse = response.getEntity();
                if (entityResponse != null) {
                    result = EntityUtils.toString(entityResponse);
                }
                if (result != null) {
                    JSONObject jsonResponse = new JSONObject(result);

                    //this code block will execute if either of completedAt or failedAt is present in the retrieve
                    //execution api response

                    if (jsonResponse.has("completedAt") || jsonResponse.has("failedAt")) {

                        //if completedAt is present, then set isCompleted to true
                        Boolean flag;
                        String date;
                        if (jsonResponse.has("completedAt")) {
                            date = jsonResponse.getString("completedAt");
                            flag = checkDate(date);
                            if (flag) {
                                isCompleted = true;
                                break;
                            }

                        }

                        //if failedAt is present, then set isFailed to true

                        else if (jsonResponse.has("failedAt")) {
                            date = jsonResponse.getString("failedAt");
                            flag = checkDate(date);
                            if (flag) {
                                isFailed = true;
                                break;
                            }

                        }
                    }
                }


                //making retrieveDelay seconds delay before making next retrieve execution api call
                Thread.sleep(retrieveDelay);
            }
        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }

        //below blocks will return execution status either of completed/failed/timeout

        if (isCompleted) {
            return Constants.EXECUTION_COMPLETED;
        } else if (isFailed) {
            return Constants.EXECUTION_FAILED;
        }

        //this block will execute if in the span of 180 seconds
        // retrieve execution api doesn't have either of completedAt or failedAt, then timeout will be returned

        else {
            return Constants.EXECUTION_TIMEOUT;
        }
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

    public CloseableHttpClient getHttpClient() {
        return buildDefaultClient();
    }

    public CloseableHttpClient buildDefaultClient() {
        Integer timeout = Constants.REQUEST_TIMEOUT;
        RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 1000).setConnectionRequestTimeout(timeout * 1000).setSocketTimeout(timeout * 1000).build();
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        return clientBuilder.setDefaultRequestConfig(config).build();
    }

    public HttpGet createGetRequest(String url) {
        return new HttpGet(url);
    }

}
