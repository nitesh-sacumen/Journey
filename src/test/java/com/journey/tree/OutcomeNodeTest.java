package com.journey.tree;

import com.journey.tree.config.Constants;
import com.journey.tree.nodes.ErrorMessageNode;
import com.journey.tree.nodes.OutcomeNode;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
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
import javax.security.auth.callback.TextOutputCallback;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.MockitoAnnotations.initMocks;

public class OutcomeNodeTest {
    @InjectMocks
    OutcomeNode outcomeNode;

    @BeforeMethod
    public void before() {
        initMocks(this);
    }

    @Test
    public void testOutcomeNodeWithNoCallback() throws NodeProcessException {
        TreeContext treeContext = buildThreeContext(Collections.emptyList(),null);
        Action action = outcomeNode.process(treeContext);
        List<Callback> callbacks = action.callbacks;
        Assert.assertEquals(callbacks.size(),2);
    }

    @Test
    public void testOutcomeNodeWithNullExecutionStatus() throws NodeProcessException {
        List<Callback> cbList = new ArrayList<>();

        HiddenValueCallback hiddenValueCallback = new HiddenValueCallback("h1");
        cbList.add(hiddenValueCallback);

        TreeContext treeContext = buildThreeContext(cbList,null);
        Action action = outcomeNode.process(treeContext);
        Assert.assertNotNull(action);
    }

    @Test
    public void testOutcomeNodeWithCompletedExecutionStatus() throws NodeProcessException {
        List<Callback> cbList = new ArrayList<>();

        HiddenValueCallback hiddenValueCallback = new HiddenValueCallback("h1");
        cbList.add(hiddenValueCallback);

        TreeContext treeContext = buildThreeContext(cbList,"execution_completed");
        Action action = outcomeNode.process(treeContext);
        Callback cb1 = action.callbacks.get(0);
        Assert.assertTrue(cb1 instanceof TextOutputCallback);
    }

    @Test
    public void testOutcomeNodeWithFailedExecutionStatus() throws NodeProcessException {
        List<Callback> cbList = new ArrayList<>();

        HiddenValueCallback hiddenValueCallback = new HiddenValueCallback("h1");
        cbList.add(hiddenValueCallback);

        TreeContext treeContext = buildThreeContext(cbList,"execution_failed");
        Action action = outcomeNode.process(treeContext);
        Callback cb1 = action.callbacks.get(0);
        Callback cb2 = action.callbacks.get(1);
        Assert.assertTrue(cb1 instanceof TextOutputCallback);
        Assert.assertTrue(cb2 instanceof TextOutputCallback);
    }

    @Test
    public void testOutcomeNodeWithTimeoutExecutionStatus() throws NodeProcessException {
        List<Callback> cbList = new ArrayList<>();

        HiddenValueCallback hiddenValueCallback = new HiddenValueCallback("h1");
        cbList.add(hiddenValueCallback);

        TreeContext treeContext = buildThreeContext(cbList,"execution_timeout");
        Action action = outcomeNode.process(treeContext);
        Callback cb1 = action.callbacks.get(0);
        Callback cb2 = action.callbacks.get(1);
        Assert.assertTrue(cb1 instanceof TextOutputCallback);
        Assert.assertTrue(cb2 instanceof TextOutputCallback);
    }

    @Test
    public void testOutcomeNodeWithCompletedFlow() throws NodeProcessException {
        List<Callback> cbList = new ArrayList<>();

        String[] choices = {"Retry"};
        cbList.add(new ConfirmationCallback(0, choices, 0));

        TreeContext treeContext = buildThreeContext(cbList,"execution_completed");
        Action action = outcomeNode.process(treeContext);
        String outcome = action.outcome;
        Assert.assertEquals(outcome,"Success");

    }

    @Test
    public void testOutcomeNodeWithFailedFlow() throws NodeProcessException {
        List<Callback> cbList = new ArrayList<>();

        String[] choices = {"Retry"};
        cbList.add(new ConfirmationCallback(0, choices, 0));

        TreeContext treeContext = buildThreeContext(cbList,"execution_failed");
        Action action = outcomeNode.process(treeContext);
        String outcome = action.outcome;
        Assert.assertEquals(outcome,"Failure");

    }
    @Test
    public void testOutcomeNodeWithTimeoutFlow() throws NodeProcessException {
        List<Callback> cbList = new ArrayList<>();

        String[] choices = {"Retry"};
        cbList.add(new ConfirmationCallback(0, choices, 0));

        TreeContext treeContext = buildThreeContext(cbList,"execution_timeout");
        Action action = outcomeNode.process(treeContext);
        String outcome = action.outcome;
        Assert.assertEquals(outcome,"Timeout");

    }



    private TreeContext buildThreeContext(List<Callback> callbacks,String executionStatus) {
        return new TreeContext(retrieveSharedState(executionStatus), json(object()),
                new ExternalRequestContext.Builder().build(), callbacks
                , Optional.of("mockUserId"));
    }

    private JsonValue retrieveSharedState(String executionStatus) {
        if(executionStatus!=null){
            return json(object(field(USERNAME, "demo"),
                    field(Constants.EXECUTION_STATUS,executionStatus)));
        }
        return json(object(field(USERNAME, "demo")));
    }

}
