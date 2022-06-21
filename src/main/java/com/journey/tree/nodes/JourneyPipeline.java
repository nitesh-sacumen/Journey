package com.journey.tree.nodes;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.journey.tree.config.Constants;
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
import com.journey.tree.util.CreateExecution;
import com.journey.tree.util.RetrieveExecution;

import javax.inject.Inject;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author Sacumen (www.sacumen.com)
 * @category Node
 * @Descrition
 */
@Node.Metadata(outcomeProvider = JourneyPipeline.JourneyPipelineOutcomeProvider.class, configClass = JourneyPipeline.Config.class)
public class JourneyPipeline implements Node {

    private final Config config;
    private static final String BUNDLE = "com/journey/tree/nodes/JourneyPipeline";
    private final Logger logger = LoggerFactory.getLogger(AuthClientUtils.class);

    /**
     * Configuration for the node.
     */

    //this block will for setting configuration variables

    public interface Config {
        @Attribute(order = 100, requiredValue = true)
        String pipelineKey();

        @Attribute(order = 200, requiredValue = true)
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

    @Override
    public Action process(TreeContext context) throws NodeProcessException {

        //if either of pipeline key or dashboard id is empty, the flow will not continue

        if (config.pipelineKey() == null || config.dashboardId() == null) {
            logger.error("Please configure pipelineKey/dashboard Id to proceed");
            System.out.println("Please configure pipelineKey/dashboard Id to proceed");
            return null;
        }
        try {
            JsonValue sharedState = context.sharedState;

            //putting pipeline key and dashboard id in the shared memory

            sharedState.put(Constants.PIPELINE_KEY, config.pipelineKey());
            sharedState.put(Constants.DASHBOARD_ID, config.dashboardId());
            CreateExecution createExecution = new CreateExecution();

            //this statement will call execute method of CreateExecution class which will make call to create execution api
            //execution id in response of the create execution api response will be captured

            String executionId = createExecution.execute(context);
            String type;
            type = sharedState.get(Constants.TYPE).asString();
            RetrieveExecution retrieveExecution = new RetrieveExecution();

            //here call is made to retrieve method of RetrieveExecution class
            // which will make calls to execution retrieve
            // api to check if it has either of completedAt or failedAt in response else it will return timeout

            String executionStatus = retrieveExecution.retrieve(context, executionId);

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
        } catch (Exception e) {
            logger.error(e.getStackTrace().toString());
            e.printStackTrace();
            throw new NodeProcessException("Exception is: ", e);
        }
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
     * Defines the possible outcomes from this JourneyPipeline node.
     */

    //this code block will generate 3 outcomes of the journey pipeline node i.e. successful, error, timeout

    public static class JourneyPipelineOutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
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