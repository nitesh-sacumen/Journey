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

        try (CloseableHttpClient httpclient = getHttpClient()) {
            if (sharedState.get(Constants.API_ACCESS_TOKEN).isNotNull()) {
                apiAccessToken = sharedState.get(Constants.API_ACCESS_TOKEN).asString();
            }
            //execution wait logic begins
            System.out.println("execution id is " + executionId);
            HttpGet httpGet = createGetRequest(Constants.EXECUTION_RETRIEVE + executionId);
            httpGet.addHeader("Authorization", "Bearer " + apiAccessToken);
            httpGet.addHeader("Accept", "application/json");

            System.out.println("before 3 seconds");
            Thread.sleep(3000);
            System.out.println("after 3 seconds");


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
                        if (jsonResponse != null && (jsonResponse.has("completedAt") || jsonResponse.has("failedAt"))) {

                            if (jsonResponse.has("completedAt")) {//apply logic for handling garbage value
                                isCompleted = true;
                            }
                            if (jsonResponse.has("failedAt")) {
                                isFailed = true;
                            }
                            break;
                        }
                    }
                }

                System.out.println(j + " api call");
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            logger.error(e.getStackTrace().toString());
            e.printStackTrace();
        }
        if (isCompleted) {
            return Constants.EXECUTION_COMPLETED;
        } else if (isFailed) {
            return Constants.EXECUTION_FAILED;
        } else {
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
