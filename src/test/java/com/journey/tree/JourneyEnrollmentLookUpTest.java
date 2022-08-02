package com.journey.tree;

import com.iplanet.sso.SSOException;
import com.journey.tree.config.Constants;
import com.journey.tree.nodes.JourneyEnrollmentLookUp;
import com.journey.tree.util.*;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

public class JourneyEnrollmentLookUpTest {
    @InjectMocks
    JourneyEnrollmentLookUp journeyEnrollmentLookUp;

    @Mock
    JourneyEnrollmentLookUp.Config config;

    @Mock
    CoreWrapper coreWrapper;

    @Mock
    JourneyCustomerLookUp journeyCustomerLookUp;

    @Mock
    AMIdentity userIdentity;


    @BeforeMethod
    public void before() {
        initMocks(this);
        journeyEnrollmentLookUp = new JourneyEnrollmentLookUp(config,coreWrapper,journeyCustomerLookUp);
    }


    @Test
    public void testJourneyEnrollmentLookUpTestWithTrueUserDetails() throws NodeProcessException, IdRepoException, SSOException {
        TreeContext treeContext = buildThreeContext(Collections.emptyList(),null);
        Mockito.when(config.journeyApiToken()).thenReturn("");
        Mockito.when(config.journeyAccountId()).thenReturn("");
        Mockito.when(coreWrapper.getIdentity("demo","test")).thenReturn(userIdentity);
        Mockito.when(userIdentity.getAttribute("mail")).thenReturn(new HashSet<>());
        Mockito.when(config.uniqueIdentifier()).thenReturn(JourneyEnrollmentLookUp.TypeOfIdentifier.username);
        Action action = journeyEnrollmentLookUp.process(treeContext);
        Assert.assertNotNull(action);
    }

    @Test
    public void testJourneyEnrollmentLookUpTestWithFalseUserDetails() throws NodeProcessException, IdRepoException, SSOException {
        TreeContext treeContext = buildThreeContext(Collections.emptyList(),null);
        Mockito.when(config.journeyApiToken()).thenReturn("");
        Mockito.when(config.journeyAccountId()).thenReturn("");
        Mockito.when(coreWrapper.getIdentity("demo","test")).thenReturn(null);

        NodeProcessException exception = Assert.expectThrows(NodeProcessException.class, () -> {
            journeyEnrollmentLookUp.process(treeContext);
        });
        assertEquals("Exception is: Invalid forgerock username", exception.getMessage());
    }

    @Test
    public void testJourneyEnrollmentLookUpTestWithNullEmailIdentifier() throws NodeProcessException, IdRepoException, SSOException {
        TreeContext treeContext = buildThreeContext(Collections.emptyList(),null);
        Mockito.when(config.journeyApiToken()).thenReturn("");
        Mockito.when(config.journeyAccountId()).thenReturn("");
        Mockito.when(coreWrapper.getIdentity("demo","test")).thenReturn(userIdentity);

        Mockito.when(userIdentity.getAttribute("mail")).thenReturn(new HashSet<>());
        Mockito.when(config.uniqueIdentifier()).thenReturn(JourneyEnrollmentLookUp.TypeOfIdentifier.email);

        NodeProcessException exception = Assert.expectThrows(NodeProcessException.class, () -> {
            journeyEnrollmentLookUp.process(treeContext);
        });
        assertEquals("Exception is: Invalid unique identifier/forgerock email is missing", exception.getMessage());
    }

    @Test
    public void testJourneyEnrollmentLookUpTestWithEmailIdentifier() throws NodeProcessException, IdRepoException, SSOException {
        TreeContext treeContext = buildThreeContext(Collections.emptyList(),null);
        Mockito.when(config.journeyApiToken()).thenReturn("");
        Mockito.when(config.journeyAccountId()).thenReturn("");
        Mockito.when(coreWrapper.getIdentity("demo","test")).thenReturn(userIdentity);

        HashSet set = new HashSet();
        set.add("test.com");

        Mockito.when(userIdentity.getAttribute("mail")).thenReturn(set);
        Mockito.when(config.uniqueIdentifier()).thenReturn(JourneyEnrollmentLookUp.TypeOfIdentifier.email);

        Action action = journeyEnrollmentLookUp.process(treeContext);
        Assert.assertNotNull(action);
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
        return json(object(field(USERNAME, "demo"),
                field(REALM, "test")));
    }

}
