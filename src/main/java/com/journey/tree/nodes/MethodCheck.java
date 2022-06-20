package com.journey.tree.nodes;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.journey.tree.config.Constants;
import com.sun.identity.authentication.client.AuthClientUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.auth.node.api.Action.ActionBuilder;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author Sacumen (www.sacumen.com)
 * @category Node
 * @Descrition
 */
@Node.Metadata(outcomeProvider = MethodCheck.MethodCheckOutcomeProvider.class, configClass =
        MethodCheck.Config.class)
public class MethodCheck implements Node {

    private final Config config;
    private static final String BUNDLE = "com/journey/tree/nodes/MethodCheck";
    private Logger logger = LoggerFactory.getLogger(AuthClientUtils.class);

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
    public MethodCheck(@Assisted Config config) {
        this.config = config;
    }

    /**
     * Main logic of the node.
     */
    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        JsonValue sharedState = context.sharedState;
        try {
            if (sharedState.get(Constants.METHOD_NAME).isNotNull()) {
                String methodName = sharedState.get(Constants.METHOD_NAME).asString();
                logger.debug("method name received is " + methodName);
                if (methodName == Constants.FACIAL_BIOMETRIC) {
                    return goTo(MethodCheckOutcome.Facial_Biometrics).replaceSharedState(sharedState).build();
                } else if (methodName == Constants.ONE_TIME_PASSWORD) {//for testing purpose
                    return goTo(MethodCheckOutcome.One_Time_Password).replaceSharedState(sharedState).build();
                } else if (methodName == Constants.MOBILE_APP) {
                    return goTo(MethodCheckOutcome.Mobile_App).replaceSharedState(sharedState).build();
                }

            }
        } catch (Exception e) {
            logger.error(e.getStackTrace().toString());
            e.printStackTrace();
            throw new NodeProcessException("Exception is: ", e);
        }


        return null;
    }


    private ActionBuilder goTo(MethodCheckOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    /**
     * The possible outcomes for the MethodCheck.
     */
    public enum MethodCheckOutcome {
        /**
         * selection of Facial_Biometrics.
         */
        Facial_Biometrics,
        /**
         * selection of One_Time_Password.
         */
        One_Time_Password,
        /**
         * selection for Mobile_App.
         */
        Mobile_App

    }

    /**
     * Defines the possible outcomes from this EnrollmentMethodCheck node.
     */
    public static class MethodCheckOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(MethodCheck.BUNDLE,
                    MethodCheckOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(new Outcome(MethodCheckOutcome.Facial_Biometrics.name(), bundle.getString("facialBiometrics")),
                    new Outcome(MethodCheckOutcome.One_Time_Password.name(), bundle.getString("oneTimePassword")),
                    new Outcome(MethodCheckOutcome.Mobile_App.name(), bundle.getString("mobileApp")));
        }
    }
}