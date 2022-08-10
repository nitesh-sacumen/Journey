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
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static org.forgerock.openam.auth.node.api.Action.send;

@Node.Metadata(outcomeProvider = JourneyPipeline.OutcomeProvider.class, configClass = JourneyPipeline.Config.class)
public class JourneyPipeline implements Node {

    private final Config config;
    private static final String BUNDLE = "com/journey/tree/nodes/JourneyPipeline";
    private static final Logger logger = LoggerFactory.getLogger(JourneyPipeline.class);
    CreateExecution createExecution;
    RetrieveExecution retrieveExecution;

    /**
     * Configuration for the node.
     */

    public interface Config {
        @Attribute(order = 100, requiredValue = true)
        default String pipelineKey() {
            return "";
        }

        @Attribute(order = 200)
        default String dashboardId() {
            return "";
        }

    }

    /**
     * Create the node.
     *
     * @param config The service config.
     */
    @Inject
    public JourneyPipeline(@Assisted JourneyPipeline.Config config, CreateExecution createExecution, RetrieveExecution retrieveExecution) {
        this.config = config;
        this.createExecution = createExecution;
        this.retrieveExecution = retrieveExecution;
    }

    /**
     * @param context
     * @return Action, Which will redirect to next action.
     */

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("*********************JourneyPipeline node********************");
        if (config.pipelineKey() == null) {
            logger.error("please provide pipeline key to proceed");
            throw new NodeProcessException("please provide pipeline key to proceed");
        }
        JsonValue sharedState = context.sharedState;
        Integer counter = setCounterValue(context);
        String executionId, type = "", executionStatus;
        try {
            if (counter == 1) {
                sharedState.put(Constants.PIPELINE_KEY, config.pipelineKey());
                if (config.dashboardId() != null) {
                    sharedState.put(Constants.DASHBOARD_ID, config.dashboardId());
                }
                executionId = createExecution.execute(context);
                sharedState.put(Constants.EXECUTION_ID, executionId);
                List<Callback> cbList = new ArrayList<>();
                if (executionId != null) {
                    ScriptTextOutputCallback scriptTextOutputCallback = new ScriptTextOutputCallback(f1());
                    cbList.add(scriptTextOutputCallback);
                } else {
                    throw new NodeProcessException("Execution id not created");
                }

                return send(ImmutableList.copyOf(cbList)).build();

            } else if (counter == 2) {
                if (sharedState.get(Constants.TYPE).isNotNull()) {
                    type = sharedState.get(Constants.TYPE).asString();
                }
                List<Callback> cbList = new ArrayList<>();
                ScriptTextOutputCallback scriptTextOutputCallback = new ScriptTextOutputCallback(f2(type));
                cbList.add(scriptTextOutputCallback);
                return send(ImmutableList.copyOf(cbList)).build();
            } else if (counter == 3) {
                executionId = sharedState.get(Constants.EXECUTION_ID).asString();
                executionStatus = retrieveExecution.retrieve(context, executionId);
                sharedState.put(Constants.EXECUTION_STATUS, executionStatus);
                List<Callback> cbList = new ArrayList<>();
                ScriptTextOutputCallback scriptTextOutputCallback = new ScriptTextOutputCallback(f3());
                cbList.add(scriptTextOutputCallback);
                return send(ImmutableList.copyOf(cbList)).build();
            } else if (counter == 4) {
                sharedState.put(Constants.COUNTER, null);
                executionId = sharedState.get(Constants.EXECUTION_ID).asString();
                executionStatus = sharedState.get(Constants.EXECUTION_STATUS).asString();
                if (executionStatus.equals(Constants.EXECUTION_COMPLETED)) {
                    logger.debug(executionId + " successfully completed");
                    sharedState.put(Constants.EXECUTION_STATUS, Constants.EXECUTION_COMPLETED);
                    return goTo(Outcome.Successful).replaceSharedState(sharedState).build();
                } else if (executionStatus.equals(Constants.EXECUTION_FAILED)) {
                    logger.debug(executionId + " failed");
                    sharedState.put(Constants.EXECUTION_STATUS, Constants.EXECUTION_FAILED);
                    return goTo(Outcome.Failure).replaceSharedState(sharedState).build();
                } else {
                    logger.debug(executionId + " has a timeout");
                    sharedState.put(Constants.EXECUTION_STATUS, Constants.EXECUTION_TIMEOUT);
                    return goTo(Outcome.Timeout).replaceSharedState(sharedState).build();
                }
            }

        } catch (Exception e) {
            logger.error("Exception is: " + e.getLocalizedMessage());
            throw new NodeProcessException("Exception is: " + e.getLocalizedMessage());
        }
        logger.debug("An error occurred, please contact administrator");
        return goTo(Outcome.Failure).replaceSharedState(sharedState).build();
    }

    String f1() {
        return "document.getElementById('loginButton_0').style.display = 'none';\r\n" +
                "document.getElementById('loginButton_0').click();\r\n";
    }

    /**
     * This function will create return a javascript based script .
     */
    String f2(String type) {
        return "document.getElementById('loginButton_0').style.display = 'none';\r\n" +
                "if('" + type + "'.length>0){\r\n" +
                "var header = document.createElement('h3');\r\n" +
                "header.id='waitHeader';\r\n" +
                "header.style.textAlign='center';\r\n" +
                "header.innerHTML ='" + type + " initiated, please wait.';\r\n" +
                "document.body.appendChild(header);\r\n" +
                "}\r\n" +
                "document.getElementById('loginButton_0').click();\r\n";
    }

    String f3() {
        return "document.getElementById('loginButton_0').style.display = 'none';\r\n" +
                "if (document.contains(document.getElementById('waitHeader'))) {\n" +
                "document.getElementById('waitHeader').remove();\n" +
                "}\n" +
                "document.getElementById('loginButton_0').click();\r\n";
    }

    private Action.ActionBuilder goTo(Outcome outcome) {
        return Action.goTo(outcome.name());
    }

    /**
     * The possible outcomes for the EnrollmentStatusCheck.
     */
    public enum Outcome {
        /**
         * selection of Successful.
         */
        Successful,
        /**
         * selection for Failure.
         */
        Failure,
        /**
         * selection for Timeout.
         */
        Timeout

    }


    public static class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        /**
         * @param locales        Local property file for configuration.
         * @param nodeAttributes Node attributes for outcomes
         * @return List of possible outcomes.
         */
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(JourneyPipeline.BUNDLE, JourneyPipeline.OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(new Outcome(JourneyPipeline.Outcome.Successful.name(), bundle.getString("successful")),
                    new Outcome(JourneyPipeline.Outcome.Failure.name(), bundle.getString("failure")),
                    new Outcome(JourneyPipeline.Outcome.Timeout.name(), bundle.getString("timeout")));
        }
    }

    public static Integer setCounterValue(TreeContext context) {
        Integer counter;
        JsonValue sharedState = context.sharedState;
        if (sharedState.get(Constants.COUNTER).isNull()) {
            counter = 1;
            sharedState.put(Constants.COUNTER, counter);
        } else {
            counter = sharedState.get(Constants.COUNTER).asInteger();
            counter++;
            sharedState.put(Constants.COUNTER, counter);
        }
        return counter;
    }
}