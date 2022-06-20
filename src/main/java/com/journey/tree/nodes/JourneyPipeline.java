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
        if (config.pipelineKey() == null || config.dashboardId() == null) {
            logger.error("Please configure pipelineKey/dashboard Id to proceed");
            System.out.println("Please configure pipelineKey/dashboard Id to proceed");
            return null;
        }
        try {
            JsonValue sharedState = context.sharedState;
            sharedState.put(Constants.PIPELINE_KEY, config.pipelineKey());
            sharedState.put(Constants.DASHBOARD_ID, config.dashboardId());
            CreateExecution createExecution = new CreateExecution();
            String executionId = createExecution.execute(context);
            String type;
            type = sharedState.get(Constants.TYPE).asString();
            RetrieveExecution retrieveExecution = new RetrieveExecution();
            String executionStatus = retrieveExecution.retrieve(context, executionId);
            if (executionStatus == Constants.EXECUTION_COMPLETED) {
                logger.debug(type + " with id " + executionId + " successfully completed");
                return goTo(JourneyPipelineOutcome.Successful).replaceSharedState(sharedState).build();
            } else if (executionStatus == Constants.EXECUTION_FAILED) {
                logger.debug(type + " with id " + executionId + " failed");
                return goTo(JourneyPipelineOutcome.Error).replaceSharedState(sharedState).build();
            } else {
                logger.debug(type + " with id " + executionId + " has a timeout");
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
    public static class JourneyPipelineOutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(JourneyPipeline.BUNDLE, JourneyPipeline.JourneyPipelineOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(new Outcome(JourneyPipeline.JourneyPipelineOutcome.Successful.name(), bundle.getString("successful")), new Outcome(JourneyPipeline.JourneyPipelineOutcome.Error.name(), bundle.getString("error")), new Outcome(JourneyPipeline.JourneyPipelineOutcome.Timeout.name(), bundle.getString("timeout")));
        }
    }

}