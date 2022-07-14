/**
 * @author Sacumen(www.sacumen.com) MethodCheck node with
 * 3 outcomes. This node will connect with one of the 3 methods i.e.
 * facial-biometrics, mobile app and one-time-password
 */

package com.journey.tree.nodes;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.journey.tree.config.Constants;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Action.ActionBuilder;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.ResourceBundle;

@Node.Metadata(outcomeProvider = LogicToDetermineMethod.OutcomeProvider.class, configClass =
        LogicToDetermineMethod.Config.class)
public class LogicToDetermineMethod implements Node {

    private final Config config;
    private static final String BUNDLE = "com/journey/tree/nodes/LogicToDetermineMethod";
    private final static Logger logger = LoggerFactory.getLogger(LogicToDetermineMethod.class);

    /**
     * Configuration for the node.
     */
    public interface Config {

    }

    /**
     * Create the node.
     *
     * @param config The service config.
     */
    @Inject
    public LogicToDetermineMethod(@Assisted Config config) {
        this.config = config;
    }

    /**
     * @param context
     * @return Action, Which will redirect to next action.
     */
    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("*********************LogicToDetermineMethod node********************");
        JsonValue sharedState = context.sharedState;
        if (sharedState.get(Constants.METHOD_NAME).isNotNull()) {
            String methodName = sharedState.get(Constants.METHOD_NAME).asString();
            logger.debug("method name received is " + methodName);
            if (methodName == Constants.FACIAL_BIOMETRIC) {
                return goTo(Outcome.Facial_Biometrics).replaceSharedState(sharedState).build();
            }
//                else if (methodName == Constants.ONE_TIME_PASSWORD) {
//                    return goTo(Outcome.One_Time_Password).replaceSharedState(sharedState).build();
//                }
            else if (methodName == Constants.MOBILE_APP) {
                return goTo(Outcome.Mobile_App).replaceSharedState(sharedState).build();
            }
        }
        logger.debug("Unexpected error occurred, please contact administrator");
        throw new NodeProcessException("Unexpected error occurred, please contact administrator");
    }

    private ActionBuilder goTo(Outcome outcome) {
        return Action.goTo(outcome.name());
    }

    /**
     * The possible outcomes for the MethodCheck.
     */
    public enum Outcome {
        /**
         * selection of Facial_Biometrics.
         */
        Facial_Biometrics,
        /**
         * selection of One_Time_Password.
         */
        // One_Time_Password,
        /**
         * selection for Mobile_App.
         */
        Mobile_App

    }

    /**
     * This class will create customized outcome for the node.
     */
    public static class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {

        /**
         * @param locales        Local property file for configuration.
         * @param nodeAttributes Node attributes for outcomes
         * @return List of possible outcomes.
         */
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(LogicToDetermineMethod.BUNDLE,
                    JourneyEnrollmentLookUp.OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(new Outcome(LogicToDetermineMethod.Outcome.Facial_Biometrics.name(), bundle.getString("facialBiometrics")),
                    // new Outcome(LogicToDetermineMethod.Outcome.One_Time_Password.name(), bundle.getString("oneTimePassword")),
                    new Outcome(LogicToDetermineMethod.Outcome.Mobile_App.name(), bundle.getString("mobileApp")));
        }
    }
}