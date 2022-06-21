package com.journey.tree.util;

import com.journey.tree.config.Constants;
import org.apache.http.HttpEntity;
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

public class RetrieveExecution {
    private Logger logger = LoggerFactory.getLogger(RetrieveExecution.class);
    String apiAccessToken = "";
    String result = "";
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
            System.out.println("execution id is " + executionId);

            //appending execution id as a query parameter for retrieve api endpoint

            HttpGet httpGet = createGetRequest(Constants.EXECUTION_RETRIEVE + executionId);

            //setting headers for retrieve execution api

            httpGet.addHeader("Authorization", "Bearer " + apiAccessToken);
            httpGet.addHeader("Accept", "application/json");
            System.out.println("before 3 seconds");

            //this statement will delay the first call to retrieve execution api for a span of 3 seconds

            Thread.sleep(3000);
            System.out.println("after 3 seconds");


            //in this loop retrieve api will be called at max for
            // 60 times at the interval of 2 seconds

            for (Integer j = 1; j <= 60; j++) {
                CloseableHttpResponse response = httpclient.execute(httpGet);
                responseCode = response.getStatusLine().getStatusCode();
                logger.debug("retieve api response code is: " + responseCode);
                if (response != null) {
                    HttpEntity entityResponse = response.getEntity();
                    if (entityResponse != null) {
                        result = EntityUtils.toString(entityResponse);
                    }
                    if (result != "") {
                        JSONObject jsonResponse = new JSONObject(result);

                        //this code block will execute if either of completedAt or failedAt is present in the retrieve
                        //execution api response

                        if (jsonResponse != null && (jsonResponse.has("completedAt") || jsonResponse.has("failedAt"))) {

                            //if completedAt is present, then set isCompleted to true

                            if (jsonResponse.has("completedAt")) {//apply logic for handling garbage value
                                isCompleted = true;
                            }

                            //if failedAt is present, then set isFailed to true

                            else if (jsonResponse.has("failedAt")) {
                                isFailed = true;
                            }
                            break;
                        }
                    }
                }

                System.out.println(j + " api call");

                //making 2 seconds delay before making next retrieve execution api call

                Thread.sleep(2000);
            }
        } catch (Exception e) {
            logger.error(e.getStackTrace().toString());
            e.printStackTrace();
        }

        //below blocks will return execution status either of completed/failed/timeout

        if (isCompleted) {
            return Constants.EXECUTION_COMPLETED;
        } else if (isFailed) {
            return Constants.EXECUTION_FAILED;
        }

        //this block will execute if in the span of 120 seconds
        // retrieve execution api doesn't have either of completedAt or failedAt, then timeout will be returned

        else {
            return Constants.EXECUTION_TIMEOUT;
        }
    }

    public CloseableHttpClient getHttpClient() {
        return buildDefaultClient();
    }

    public CloseableHttpClient buildDefaultClient() {
        logger.debug("requesting http client connection client open");

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        return clientBuilder.build();
    }

    public HttpGet createGetRequest(String url) {
        return new HttpGet(url);
    }

}
