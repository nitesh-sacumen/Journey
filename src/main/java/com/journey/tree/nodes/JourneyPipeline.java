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
import com.sun.identity.authentication.client.AuthClientUtils;
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
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import static org.forgerock.openam.auth.node.api.Action.send;
@Node.Metadata(outcomeProvider = JourneyPipeline.JourneyPipelineOutcomeProvider.class, configClass = JourneyPipeline.Config.class)
public class JourneyPipeline implements Node {

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

        //if pipeline key is empty, then the flow will not continue

        if (config.pipelineKey() == null) {
            logger.error("please provide pipeline key to proceed");
            throw new NodeProcessException("please provide pipeline key to proceed");
        }
        try {
            JsonValue sharedState = context.sharedState;
            if (!context.hasCallbacks()) {
                List<Callback> cbList = new ArrayList<>();
                ScriptTextOutputCallback scriptTextOutputCallback = new ScriptTextOutputCallback(f1(sharedState.get(Constants.TYPE).asString()));
                cbList.add(scriptTextOutputCallback);
                return send(ImmutableList.copyOf(cbList)).build();
            }
            //putting pipeline key and dashboard id in the shared memory

            sharedState.put(Constants.PIPELINE_KEY, config.pipelineKey());
            if (config.dashboardId() != null) {
                sharedState.put(Constants.DASHBOARD_ID, config.dashboardId());
            }
            CreateExecution createExecution = new CreateExecution();

            //this statement will call execute method of CreateExecution class which will make call to create execution api
            //execution id in response of the create execution api response will be captured

            String executionId = createExecution.execute(context);
            String type;
            type = sharedState.get(Constants.TYPE).asString();

            //here call is made to retrieve method of RetrieveExecution class
            // which will make calls to execution retrieve
            // api to check if it has either of completedAt or failedAt in response else it will return timeout

            String executionStatus;
            if (executionId != null) {
                RetrieveExecution retrieveExecution = new RetrieveExecution();
                executionStatus = retrieveExecution.retrieve(context, executionId);

                //comparing execution status received which could be either of completed/failed/timeout

                if (executionStatus == Constants.EXECUTION_COMPLETED) {
                    logger.debug(type + " with id " + executionId + " successfully completed");

                    //if the execution status is completed, connect it with successful outcome

                    return goTo(JourneyPipelineOutcome.Successful).replaceSharedState(sharedState).build();
                } else if (executionStatus == Constants.EXECUTION_FAILED) {
                    logger.debug(type + " with id " + executionId + " failed");

                    //if the execution status is failed, connect it with error outcome

                    return goTo(JourneyPipelineOutcome.Error).replaceSharedState(sharedState).build();
                } else {
                    logger.debug(type + " with id " + executionId + " has a timeout");

                    //if either of completedAt or failedAt is not present then connect it with timeout outcome

                    return goTo(JourneyPipelineOutcome.Timeout).replaceSharedState(sharedState).build();
                }
            } else {
                logger.debug("execution id not created/timeout");

                return goTo(JourneyPipelineOutcome.Timeout).replaceSharedState(sharedState).build();
            }
        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new NodeProcessException("Exception is: ", e);
        }
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

    private Action.ActionBuilder goTo(JourneyPipeline.JourneyPipelineOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    /**
     * The possible outcomes for the JourneyPipeline.
     */
    public enum JourneyPipelineOutcome {
        /**
         * selection of Successful.
         */
        Successful,
        /**
         * selection for Error.
         */
        Error,
        /**
         * selection for Timeout.
         */
        Timeout

    }

    /**
     * This class will create customized outcome for the node.
     */
    public static class JourneyPipelineOutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {

        /**
         * @param locales        Local property file for configuration.
         * @param nodeAttributes Node attributes for outcomes
         * @return List of possible outcomes.
         */
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(JourneyPipeline.BUNDLE, JourneyPipeline.JourneyPipelineOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(new Outcome(JourneyPipeline.JourneyPipelineOutcome.Successful.name(),
                    bundle.getString("successful")), new Outcome(JourneyPipeline.JourneyPipelineOutcome.Error.name(),
                    bundle.getString("error")), new Outcome(JourneyPipeline.JourneyPipelineOutcome.Timeout.name(),
                    bundle.getString("timeout")));
        }
    }

}