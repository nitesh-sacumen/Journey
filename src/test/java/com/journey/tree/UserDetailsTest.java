package com.journey.tree;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.journey.tree.config.Constants;
import com.journey.tree.util.ForgerockToken;
import com.journey.tree.util.HttpConnectionClient;
import com.journey.tree.util.UserDetails;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.junit.Rule;
import org.mockito.InjectMocks;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.MockitoAnnotations.initMocks;

public class UserDetailsTest {
    @InjectMocks
    UserDetails userDetails;

    @Rule
    public WireMockRule wireMockRule;


    String wireMockPort;

    @BeforeMethod
    public void before()
    {
        userDetails = new UserDetails(new HttpConnectionClient());
        wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());
        wireMockRule.start();
        wireMockPort = String.valueOf(wireMockRule.port());
        initMocks(this);
    }

    @Test
    public void testUserDetailsSuccess() throws NodeProcessException {
        wireMockRule.stubFor(get(WireMock.urlPathMatching("/json/realms/root/groups/"))
                .willReturn(aResponse().withBody("{\n" +
                                "  \"members\":{\"uniqueMember\":[\"testUser\"]}\n" +
                                "}")
                        .withStatus(200).withHeader("Content-Type", "application/x-www-form-urlencoded")));
        wireMockRule.stubFor(get(WireMock.urlPathMatching("/json/serverinfo/*"))
                .willReturn(aResponse().withBody("{\n" +
                                "  \"cookieName\":\"testCookie\"\n" +
                                "}")
                        .withStatus(200).withHeader("Content-Type", "application/x-www-form-urlencoded")));
        wireMockRule.stubFor(post(WireMock.urlPathEqualTo("/json/sessions?_action=getSessionInfo"))
                .withHeader("Cookie",containing("testCookie=testToken"))
                .withHeader("Accept",containing("application/json"))
                .withHeader("Content-Type",containing("application/json"))
                .withHeader("Accept-API-Version",containing("protocol=1.0,resource=3.0"))
                .willReturn(aResponse().withBody("{\n" +
                                "  \"universalId\":\"testUniversalId\",\n" +
                                "  \"properties\" :{\"AMCtxId\" : \"testId\"}\n" +
                                "}")
                        .withStatus(200).withHeader("Content-Type", "application/x-www-form-urlencoded")));

        TreeContext treeContext = buildThreeContext(Collections.emptyList());
        boolean result = userDetails.getDetails(treeContext,"testToken","testGroup","testUser");
        Assert.assertTrue(result);
        Assert.assertEquals(treeContext.sharedState.get(Constants.COOKIE_NAME).asString(),"testCookie");
        Assert.assertEquals(treeContext.sharedState.get(Constants.FORGEROCK_ID).asString(),"testUniversalId");
        Assert.assertEquals(treeContext.sharedState.get(Constants.FORGEROCK_SESSION_ID).asString(),"testId");
        Assert.assertEquals(treeContext.sharedState.get(Constants.IS_ADMIN).asBoolean(),true);

    }
    @Test
    public void testUserDetailsfailure() throws NodeProcessException {
        wireMockRule.stubFor(get(WireMock.urlPathMatching("/json/realms/root/groups/"))
                .willReturn(aResponse()
                        .withStatus(404)));
        wireMockRule.stubFor(get(WireMock.urlPathMatching("/json/serverinfo/*"))
                .willReturn(aResponse().withBody("{\n" +
                                "  \"cookieName\":\"testCookie\"\n" +
                                "}")
                        .withStatus(200).withHeader("Content-Type", "application/x-www-form-urlencoded")));
        wireMockRule.stubFor(post(WireMock.urlPathEqualTo("/json/sessions?_action=getSessionInfo"))
                .withHeader("Cookie",containing("testCookie=testToken"))
                .withHeader("Accept",containing("application/json"))
                .withHeader("Content-Type",containing("application/json"))
                .withHeader("Accept-API-Version",containing("protocol=1.0,resource=3.0"))
                .willReturn(aResponse().withBody("{\n" +
                                "  \"universalId\":\"testUniversalId\",\n" +
                                "  \"properties\" :{\"AMCtxId\" : \"testId\"}\n" +
                                "}")
                        .withStatus(200).withHeader("Content-Type", "application/x-www-form-urlencoded")));

        TreeContext treeContext = buildThreeContext(Collections.emptyList());
        boolean result = userDetails.getDetails(treeContext,"testToken","testGroup","testUser");
        Assert.assertTrue(!result);
        Assert.assertEquals(treeContext.sharedState.get(Constants.COOKIE_NAME).asString(),"testCookie");
        Assert.assertEquals(treeContext.sharedState.get(Constants.FORGEROCK_ID).asString(),"testUniversalId");
        Assert.assertEquals(treeContext.sharedState.get(Constants.FORGEROCK_SESSION_ID).asString(),"testId");

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
