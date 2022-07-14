package com.journey.tree;

import com.journey.tree.config.Constants;
import com.journey.tree.nodes.ErrorMessageNode;
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
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.MockitoAnnotations.initMocks;

public class ErrorMessageNodeTest {
    @InjectMocks
    ErrorMessageNode errorMessageNode;

    @BeforeMethod
    public void before() {
        initMocks(this);
    }

    @Test
    public void testErrorMessageNodeWithNoError() throws NodeProcessException {
        TreeContext treeContext = buildThreeContext(Collections.emptyList(),null);
        Action action = errorMessageNode.process(treeContext);
        List<Callback> callbacks = action.callbacks;
        Assert.assertEquals(callbacks.size(),1);
    }

    @Test
    public void testErrorMessageNodeWithCallback() throws NodeProcessException {
        List<Callback> cbList = new ArrayList<>();

        String[] submitButton = {"Call Support"};
        cbList.add(new ConfirmationCallback(0, submitButton, 0));

        TreeContext treeContext = buildThreeContext(cbList,null);

        Exception exception = Assert.expectThrows(NodeProcessException.class, () -> {
            errorMessageNode.process(treeContext);
        });

        String expectedMessage = "Unexpected error occurred, please contact administrator";
        String actualMessage = exception.getMessage();
        Assert.assertEquals(actualMessage,expectedMessage);
    }

    @Test
    public void testErrorMessageNodeWithError() throws NodeProcessException {
        TreeContext treeContext = buildThreeContext(Collections.emptyList(),"Error while processing!!");
        Action action = errorMessageNode.process(treeContext);
        List<Callback> callbacks = action.callbacks;
        Assert.assertEquals(callbacks.size(),4);
    }


    private TreeContext buildThreeContext(List<Callback> callbacks,String errorMessage) {
        return new TreeContext(retrieveSharedState(errorMessage), json(object()),
                new ExternalRequestContext.Builder().build(), callbacks
                , Optional.of("mockUserId"));
    }

    private JsonValue retrieveSharedState(String errorMessage) {
        if(errorMessage!=null){
            return json(object(field(USERNAME, "demo"),
                    field(Constants.ERROR_MESSAGE,errorMessage)));
        }
        return json(object(field(USERNAME, "demo")));
    }

}
