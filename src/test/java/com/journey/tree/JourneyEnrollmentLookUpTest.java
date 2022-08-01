package com.journey.tree;

import com.journey.tree.config.Constants;
import com.journey.tree.nodes.JourneyEnrollmentLookUp;
import com.journey.tree.util.*;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class JourneyEnrollmentLookUpTest {
    @InjectMocks
    JourneyEnrollmentLookUp journeyEnrollmentLookUp;

    @Mock
    JourneyEnrollmentLookUp.Config config;

    @Mock
    CoreWrapper coreWrapper;

    @Mock
    JourneyCustomerLookUp journeyCustomerLookUp;


    @BeforeMethod
    public void before() {
        initMocks(this);
        journeyEnrollmentLookUp = new JourneyEnrollmentLookUp(config,coreWrapper,journeyCustomerLookUp);
    }


    @Test
    public void testJourneyEnrollmentLookUpTestWithMessageOutcome() throws NodeProcessException {
        TreeContext treeContext = buildThreeContext(Collections.emptyList(),null);
        Mockito.when(config.journeyApiToken()).thenReturn("");
        Mockito.when(config.journeyAccountId()).thenReturn("");
       // Mockito.when(config.uniqueIdentifier()).thenReturn("");
        Action action = journeyEnrollmentLookUp.process(treeContext);
        String outcome  = action.outcome;
        Assert.assertEquals(outcome,"Message");
    }

    @Test
    public void testJourneyEnrollmentLookUpTestWithCallback() throws NodeProcessException {
        TreeContext treeContext = buildThreeContext(Collections.emptyList(),null);
        Mockito.when(config.journeyApiToken()).thenReturn("refreshToken");
        Mockito.when(config.journeyAccountId()).thenReturn("accountID");
//        Mockito.when(config.uniqueIdentifier()).thenReturn("");
//        Mockito.when(config.adminUsername()).thenReturn("admin");
//        Mockito.when(config.adminPassword()).thenReturn("password");
//        Mockito.when(config.groupName()).thenReturn("group1");
//        Mockito.when(config.forgerockHostUrl()).thenReturn("testUrl");
//        Mockito.when(forgerockToken.createToken(any(),any(),any())).thenReturn(true);
        try (MockedStatic<ForgerockUser> theMock = Mockito.mockStatic(ForgerockUser.class)) {
            theMock.when(() -> ForgerockUser.getDetails(any(),any(),any()))
                    .thenReturn(true);

            Action action = journeyEnrollmentLookUp.process(treeContext);
            List<Callback> callbacks = action.callbacks;
            Assert.assertEquals(callbacks.size(),0);
        }

    }

    @Test
    public void testJourneyEnrollmentLookUpTestWithNOCallback() throws NodeProcessException {
        TreeContext treeContext = buildThreeContext(Collections.emptyList(),null);
        Mockito.when(config.journeyApiToken()).thenReturn("refreshToken");
        Mockito.when(config.journeyAccountId()).thenReturn("accountID");
//        Mockito.when(config.uniqueIdentifier()).thenReturn("uniqueIdentifier");
//        Mockito.when(config.adminUsername()).thenReturn("admin");
//        Mockito.when(config.adminPassword()).thenReturn("password");
//        Mockito.when(config.groupName()).thenReturn("group1");
//        Mockito.when(config.forgerockHostUrl()).thenReturn("testUrl");
//        Mockito.when(forgerockToken.createToken(any(),any(),any())).thenReturn(false);
//        try (MockedStatic<ForgerockUser> theMock = Mockito.mockStatic(ForgerockUser.class)) {
//            theMock.when(() -> ForgerockUser.getDetails(any(),any(),any()))
//                    .thenReturn(true);
//
//            Action action = journeyEnrollmentLookUp.process(treeContext);
//            String outcome  = action.outcome;
//            Assert.assertEquals(outcome,"Message");
//        }

    }

    @Test
    public void testJourneyEnrollmentLookUpTest() throws NodeProcessException {
        TreeContext treeContext = buildThreeContext(Collections.emptyList(),1);
        Mockito.when(config.journeyApiToken()).thenReturn("refreshToken");
        Mockito.when(config.journeyAccountId()).thenReturn("accountID");
//        Mockito.when(config.uniqueIdentifier()).thenReturn("");
//        Mockito.when(config.adminUsername()).thenReturn("admin");
//        Mockito.when(config.adminPassword()).thenReturn("password");
//        Mockito.when(config.groupName()).thenReturn("group1");
//        Mockito.when(config.forgerockHostUrl()).thenReturn("testUrl");
//        Mockito.when(forgerockToken.createToken(any(),any(),any())).thenReturn(false);
//        try (MockedStatic<ForgerockUser> theMock = Mockito.mockStatic(ForgerockUser.class)) {
//            theMock.when(() -> ForgerockUser.getDetails(any(),any(),any()))
//                    .thenReturn(true);
//
//            Action action = journeyEnrollmentLookUp.process(treeContext);
//            String outcome  = action.outcome;
//            Assert.assertEquals(outcome,"Message");
//        }

    }

    @Test
    public void testJourneyEnrollmentLookUpTestWithUserDetailsTrue() throws NodeProcessException, JSONException {
        TreeContext treeContext = buildThreeContext(Collections.emptyList(),1);
        Mockito.when(config.journeyApiToken()).thenReturn("refreshToken");
        Mockito.when(config.journeyAccountId()).thenReturn("accountID");
//        Mockito.when(config.uniqueIdentifier()).thenReturn("");
//        Mockito.when(config.adminUsername()).thenReturn("admin");
//        Mockito.when(config.adminPassword()).thenReturn("password");
//        Mockito.when(config.groupName()).thenReturn("group1");
//        Mockito.when(config.forgerockHostUrl()).thenReturn("testUrl");
//        Mockito.when(forgerockToken.createToken(any(),any(),any())).thenReturn(false);
//        Mockito.when(userDetails.getDetails(any(),any(),any(),any())).thenReturn(true);
//        JSONObject jsonResponse = new JSONObject();
//        jsonResponse.put("token","testToken");
//        Mockito.when(journeyGetAccessToken.createAccessToken(any(),any())).thenReturn(jsonResponse);
//        try (MockedStatic<ForgerockUser> theMock = Mockito.mockStatic(ForgerockUser.class)) {
//            theMock.when(() -> ForgerockUser.getDetails(any(),any(),any()))
//                    .thenReturn(true);
//
//            Action action = journeyEnrollmentLookUp.process(treeContext);
//            String outcome  = action.outcome;
//            Assert.assertEquals(outcome,"Message");
//        }

    }


    private TreeContext buildThreeContext(List<Callback> callbacks,Integer counter) {
        return new TreeContext(retrieveSharedState(counter), json(object()),
                new ExternalRequestContext.Builder().build(), callbacks
                , Optional.of("mockUserId"));
    }

    private JsonValue retrieveSharedState(Integer counter) {
        if(counter!=null){
            return json(object(field(USERNAME, "demo"),
                    field(Constants.COUNTER,counter),
                    field(Constants.CUSTOMER_LOOKUP_RESPONSE_CODE,464)));
        }
        return json(object(field(USERNAME, "demo")));
    }

}
