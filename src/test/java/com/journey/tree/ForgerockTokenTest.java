package com.journey.tree;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.journey.tree.config.Constants;
import com.journey.tree.util.CreateExecution;
import com.journey.tree.util.ForgerockToken;
import com.journey.tree.util.HttpConnectionClient;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.junit.Rule;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.MockitoAnnotations.initMocks;

public class ForgerockTokenTest {
    @InjectMocks
    ForgerockToken forgerockToken;

    @Rule
    public WireMockRule wireMockRule;


    String wireMockPort;

    @BeforeMethod
    public void before()
    {
        forgerockToken = new ForgerockToken(new HttpConnectionClient());
        wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());
        wireMockRule.start();
        wireMockPort = String.valueOf(wireMockRule.port());
        initMocks(this);
    }

    @Test
    public void testCreateForgerockTokenSuccess() throws NodeProcessException {
        wireMockRule.stubFor(post(WireMock.urlPathMatching("/json/realms/root/authenticate"))
                .willReturn(aResponse().withBody("{\n" +
                                "  \"tokenId\":\"testToken\"\n" +
                                "}")
                        .withStatus(200).withHeader("Content-Type", "application/x-www-form-urlencoded")));

        TreeContext treeContext = buildThreeContext(Collections.emptyList());
        boolean result = forgerockToken.createToken("userName","userPassword",treeContext);
        Assert.assertTrue(result);
        Assert.assertEquals(treeContext.sharedState.get(Constants.TOKEN_ID).asString(),"testToken");

    }
    @Test
    public void testCreateForgerockTokenFailure() throws NodeProcessException {
        wireMockRule.stubFor(post(WireMock.urlPathMatching("/json/realms/root/authenticate"))
                .willReturn(aResponse().withBody("{}")
                        .withStatus(200).withHeader("Content-Type", "application/x-www-form-urlencoded")));

        TreeContext treeContext = buildThreeContext(Collections.emptyList());
        boolean result = forgerockToken.createToken("userName","userPassword",treeContext);
        Assert.assertTrue(!result);
        Assert.assertEquals(treeContext.sharedState.get(Constants.TOKEN_ID).asString(),null);

    }

    @Test
    public void testCreateForgerockToken401() throws NodeProcessException {
        wireMockRule.stubFor(post(WireMock.urlPathMatching("/json/realms/root/authenticate"))
                .willReturn(aResponse()
                        .withStatus(401).withHeader("Content-Type", "application/x-www-form-urlencoded")));

        TreeContext treeContext = buildThreeContext(Collections.emptyList());
        boolean result = forgerockToken.createToken("userName","userPassword",treeContext);
        Assert.assertTrue(!result);
        Assert.assertEquals(treeContext.sharedState.get(Constants.TOKEN_ID).asString(),null);

    }

    private TreeContext buildThreeContext(List<Callback> callbacks) {
        return new TreeContext(retrieveSharedState(), json(object()),
                new ExternalRequestContext.Builder().build(), callbacks
                , Optional.of("mockUserId"));
    }

    private JsonValue retrieveSharedState() {
        return json(object(field(USERNAME, "demo"),
                field(Constants.FORGEROCK_HOST_URL,"http://localhost:"+wireMockPort),
                field(Constants.REQUEST_TIMEOUT,30)));
    }
}
