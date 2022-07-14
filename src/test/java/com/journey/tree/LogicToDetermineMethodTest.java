package com.journey.tree;

import com.journey.tree.config.Constants;
import com.journey.tree.nodes.ErrorMessageNode;
import com.journey.tree.nodes.LogicToDetermineMethod;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.mockito.InjectMocks;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.MockitoAnnotations.initMocks;

public class LogicToDetermineMethodTest {
    @InjectMocks
    LogicToDetermineMethod logicToDetermineMethod;

    @BeforeMethod
    public void before() {
        initMocks(this);
    }

    @Test
    public void testLogicToDetermineMethodWithNullMethod() throws NodeProcessException {
        TreeContext treeContext = buildThreeContext(Collections.emptyList(),null);
        Exception exception = Assert.expectThrows(NodeProcessException.class, () -> {
            logicToDetermineMethod.process(treeContext);
        });

        String expectedMessage = "Unexpected error occurred, please contact administrator";
        String actualMessage = exception.getMessage();
        Assert.assertEquals(actualMessage,expectedMessage);
    }


    @Test
    public void testLogicToDetermineMethodWithFacialBiometricsMethod() throws NodeProcessException {
        TreeContext treeContext = buildThreeContext(Collections.emptyList(),"facial-biometrics");
        Action action = logicToDetermineMethod.process(treeContext);
        String outcome = action.outcome;
        Assert.assertEquals(outcome,"Facial_Biometrics");
    }

    @Test
    public void testLogicToDetermineMethodWithMobileAppMethod() throws NodeProcessException {
        TreeContext treeContext = buildThreeContext(Collections.emptyList(),"mobile-app");
        Action action = logicToDetermineMethod.process(treeContext);
        String outcome = action.outcome;
        Assert.assertEquals(outcome,"Mobile_App");
    }


    private TreeContext buildThreeContext(List<Callback> callbacks,String errorMessage) {
        return new TreeContext(retrieveSharedState(errorMessage), json(object()),
                new ExternalRequestContext.Builder().build(), callbacks
                , Optional.of("mockUserId"));
    }

    private JsonValue retrieveSharedState(String methodName) {
        if(methodName!=null){
            return json(object(field(USERNAME, "demo"),
                    field(Constants.METHOD_NAME,methodName)));
        }
        return json(object(field(USERNAME, "demo")));
    }

}
