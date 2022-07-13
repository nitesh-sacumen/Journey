/**
 * @author Sacumen(www.sacumen.com) JourneyPipeline node with
 * three outcomes. This node will create an execution for enrollment/authentication,
 * then will retrieve its result whether success/failure/timeout
 * node outcomes will be Successful, Error/failure, Timeout
 */

package com.journey.tree.nodes;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.journey.tree.config.Constants;
import com.journey.tree.util.CreateExecution;
import com.journey.tree.util.RetrieveExecution;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.forgerock.openam.auth.node.api.Action.send;

@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class, configClass = JourneyPipeline.Config.class)
public class JourneyPipeline extends SingleOutcomeNode {

    private final Config config;
    private static final String BUNDLE = "com/journey/tree/nodes/JourneyPipeline";
    private static final Logger logger = LoggerFactory.getLogger(JourneyPipeline.class);

    /**
     * Configuration for the node.
     */

    public interface Config {
        @Attribute(order = 100, requiredValue = true)
        String pipelineKey();

        @Attribute(order = 200)
        String dashboardId();

    }

    /**
     * Create the node.
     *
     * @param config The service config.
     */
    @Inject
    public JourneyPipeline(@Assisted JourneyPipeline.Config config) {
        this.config = config;
    }

    /**
     * @param context
     * @return Action, Which will redirect to next action.
     */

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        if (config.pipelineKey() == null) {
            logger.error("please provide pipeline key to proceed");
            throw new NodeProcessException("please provide pipeline key to proceed");
        }
        JsonValue sharedState = context.sharedState;
        try {

            if (!context.hasCallbacks()) {
                List<Callback> cbList = new ArrayList<>();
                ScriptTextOutputCallback scriptTextOutputCallback = new ScriptTextOutputCallback(f1(sharedState.get(Constants.TYPE).asString()));
                cbList.add(scriptTextOutputCallback);
                return send(ImmutableList.copyOf(cbList)).build();
            }
            sharedState.put(Constants.PIPELINE_KEY, config.pipelineKey());
            if (config.dashboardId() != null) {
                sharedState.put(Constants.DASHBOARD_ID, config.dashboardId());
            }
            CreateExecution createExecution = new CreateExecution();
            String executionId = createExecution.execute(context);
            String type;
            type = sharedState.get(Constants.TYPE).asString();
            String executionStatus;
            if (executionId != null) {
                RetrieveExecution retrieveExecution = new RetrieveExecution();
                executionStatus = retrieveExecution.retrieve(context, executionId);
                if (executionStatus == Constants.EXECUTION_COMPLETED) {
                    logger.debug(type + " with id " + executionId + " successfully completed");
                    sharedState.put(Constants.EXECUTION_STATUS, Constants.EXECUTION_COMPLETED);
                } else if (executionStatus == Constants.EXECUTION_FAILED) {
                    logger.debug(type + " with id " + executionId + " failed");
                    sharedState.put(Constants.EXECUTION_STATUS, Constants.EXECUTION_FAILED);
                } else {
                    logger.debug(type + " with id " + executionId + " has a timeout");
                    sharedState.put(Constants.EXECUTION_STATUS, Constants.EXECUTION_TIMEOUT);
                }
            } else {
                logger.debug("execution id not created/timeout");
                sharedState.put(Constants.EXECUTION_STATUS, Constants.EXECUTION_TIMEOUT);
            }
        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
            throw new NodeProcessException("Exception is: ", e);
        }
        return goToNext().replaceSharedState(sharedState).build();
    }

    /**
     * This function will create return a javascript based script .
     */
    String f1(String type) {
        return "document.getElementById('loginButton_0').style.display = 'none';\r\n" +
                "var header = document.createElement('h3');\r\n" +
                "header.id='waitHeader';\r\n" +
                "header.style.textAlign='center';\r\n" +
                "header.innerHTML ='" + type + " initiated, please wait.';\r\n" +
                "document.body.appendChild(header);\r\n" +
                "document.getElementById('loginButton_0').click();\r\n";
    }


}