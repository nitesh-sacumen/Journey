package com.journey.tree;

import com.journey.tree.config.Constants;
import com.journey.tree.nodes.JourneyPipeline;
import com.journey.tree.nodes.LogicToDetermineMethod;
import com.journey.tree.util.CreateExecution;
import com.journey.tree.util.RetrieveExecution;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.MockitoAnnotations.initMocks;

public class JourneyPipelineTest {
    @InjectMocks
    JourneyPipeline journeyPipeline;

    @Mock
    JourneyPipeline.Config config;

    @Mock
    CreateExecution createExecution;

    @Mock
    RetrieveExecution retrieveExecution;



    @BeforeMethod
    public void before() {
        initMocks(this);
    }

    @Test
    public void testJourneyPipelineWithNullPipelineKey() throws NodeProcessException {
        journeyPipeline = new JourneyPipeline(config,createExecution,retrieveExecution);
        Mockito.when(config.pipelineKey()).thenReturn(null);
        TreeContext treeContext = buildThreeContext(Collections.emptyList(),null);
        Exception exception = Assert.expectThrows(NodeProcessException.class, () -> {
            journeyPipeline.process(treeContext);
        });

        String expectedMessage = "please provide pipeline key to proceed";
        String actualMessage = exception.getMessage();
        Assert.assertEquals(actualMessage,expectedMessage);
    }


    @Test
    public void testJourneyPipelineWithNoCallbacks() throws NodeProcessException {
        journeyPipeline = new JourneyPipeline(config,createExecution,retrieveExecution);
        Mockito.when(config.pipelineKey()).thenReturn("test");
        Mockito.when(config.dashboardId()).thenReturn("");
        TreeContext treeContext = buildThreeContext(Collections.emptyList(),null);
        Mockito.when(createExecution.execute(treeContext)).thenReturn("testId");
        Action action = journeyPipeline.process(treeContext);
        List<Callback> callbacks = action.callbacks;
        Assert.assertEquals(callbacks.size(),1);
    }

    @Test
    public void testJourneyPipelineWithSuccessfulOutcome() throws NodeProcessException {
        List<Callback> cbList = new ArrayList<>();
        ScriptTextOutputCallback scriptTextOutputCallback = new ScriptTextOutputCallback("testScript");
        cbList.add(scriptTextOutputCallback);

        TreeContext treeContext = buildThreeContext(cbList,2);

        journeyPipeline = new JourneyPipeline(config,createExecution,retrieveExecution);
        Mockito.when(config.pipelineKey()).thenReturn("test");
        Mockito.when(config.dashboardId()).thenReturn("");
        Mockito.when(createExecution.execute(treeContext)).thenReturn("testId");
        Mockito.when(retrieveExecution.retrieve(Mockito.any(),Mockito.any())).thenReturn("execution_completed");
        Action action = journeyPipeline.process(treeContext);
        String outcome = action.outcome;
        Assert.assertEquals(outcome,"Successful");
    }

    @Test
    public void testJourneyPipelineWithFailedOutcome() throws NodeProcessException {
        List<Callback> cbList = new ArrayList<>();
        ScriptTextOutputCallback scriptTextOutputCallback = new ScriptTextOutputCallback("testScript");
        cbList.add(scriptTextOutputCallback);

        TreeContext treeContext = buildThreeContext(cbList,2);

        journeyPipeline = new JourneyPipeline(config,createExecution,retrieveExecution);
        Mockito.when(config.pipelineKey()).thenReturn("test");
        Mockito.when(config.dashboardId()).thenReturn("");
        Mockito.when(createExecution.execute(treeContext)).thenReturn("testId");
        Mockito.when(retrieveExecution.retrieve(Mockito.any(),Mockito.any())).thenReturn("execution_failed");
        Action action = journeyPipeline.process(treeContext);
        String outcome = action.outcome;
        Assert.assertEquals(outcome,"Error");
    }

    @Test
    public void testJourneyPipelineWithTimeoutOutcome() throws NodeProcessException {
        List<Callback> cbList = new ArrayList<>();
        ScriptTextOutputCallback scriptTextOutputCallback = new ScriptTextOutputCallback("testScript");
        cbList.add(scriptTextOutputCallback);

        TreeContext treeContext = buildThreeContext(cbList,2);

        journeyPipeline = new JourneyPipeline(config,createExecution,retrieveExecution);
        Mockito.when(config.pipelineKey()).thenReturn("test");
        Mockito.when(config.dashboardId()).thenReturn("");
        Mockito.when(createExecution.execute(treeContext)).thenReturn("testId");
        Mockito.when(retrieveExecution.retrieve(Mockito.any(),Mockito.any())).thenReturn("execution_timeout");
        Action action = journeyPipeline.process(treeContext);
        String outcome = action.outcome;
        Assert.assertEquals(outcome,"Timeout");
    }


    private TreeContext buildThreeContext(List<Callback> callbacks,Integer counter) {
        return new TreeContext(retrieveSharedState(counter), json(object()),
                new ExternalRequestContext.Builder().build(), callbacks
                , Optional.of("mockUserId"));
    }

    private JsonValue retrieveSharedState(Integer counter) {
        if(counter!=null){
            return json(object(field(USERNAME, "demo"),
                    field(Constants.COUNTER,counter)));
        }
        return json(object(field(USERNAME, "demo")));
    }

}
