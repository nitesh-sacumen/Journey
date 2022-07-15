/**
 * @author Sacumen(www.sacumen.com) EnrollmentStatusCheck node with
 * two outcomes. This node will check whether a user exists or not and
 * has/doesn't have enrollments.
 * This node contains two outcomes - Has Enrollments and No Enrollments.
 * If the user has required enrollments, he will be connected with has enrollments
 * If the user doesn't have any enrollments or doesn't have minimum required enrollment,
 * he will be connected with no enrollments
 */

package com.journey.tree.nodes;


import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.journey.tree.config.Constants;
import com.journey.tree.util.*;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.util.i18n.PreferredLocales;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

@Node.Metadata(outcomeProvider = JourneyEnrollmentLookUp.OutcomeProvider.class, configClass = JourneyEnrollmentLookUp.Config.class)
public class JourneyEnrollmentLookUp implements Node {
    private static final Logger logger = LoggerFactory.getLogger(JourneyEnrollmentLookUp.class);
    private final CoreWrapper coreWrapper;
    JourneyGetAccessToken journeyGetAccessToken;
    JourneyCustomerLookUp journeyCustomerLookUp;
    ForgerockToken forgerockToken;
    private static final String BUNDLE = "com/journey/tree/nodes/JourneyEnrollmentLookUp";

    private final Config config;
    UserDetails userDetails;

    /**
     * Configuration for the node.
     */
    public interface Config {
        @Attribute(order = 100, requiredValue = true)
        default String refreshToken() {
            return "";
        }

        @Attribute(order = 200, requiredValue = true)
        default Integer timeToLive() {
            return 0;
        }

        @Attribute(order = 300, requiredValue = true)
        default String accountId() {
            return "";
        }

        @Attribute(order = 400, requiredValue = true)
        default String uniqueIdentifier() {
            return "username";
        }

        @Attribute(order = 500, requiredValue = true)
        default String adminUsername() {
            return "";
        }

        @Attribute(order = 600, requiredValue = true)
        default String adminPassword() {
            return "";
        }

        @Attribute(order = 700, requiredValue = true)
        default String groupName() {
            return "";
        }

        @Attribute(order = 800, requiredValue = true)
        default Integer retrieveTimeout() {
            return 0;
        }

        @Attribute(order = 900, requiredValue = true)
        default Integer retrieveDelay() {
            return 0;
        }

        @Attribute(order = 1000, requiredValue = true)
        default String forgerockHostUrl() {
            return "";
        }

        @Attribute(order = 1100, requiredValue = true)
        default Integer requestTimeout() {
            return 0;
        }
    }

    /**
     * Create the node.
     *
     * @param config The service config and coreWrapper
     */
    @Inject
    public JourneyEnrollmentLookUp(@Assisted Config config, CoreWrapper coreWrapper, JourneyGetAccessToken journeyGetAccessToken, JourneyCustomerLookUp journeyCustomerLookUp,
                                   ForgerockToken forgerockToken, UserDetails userDetails) {
        this.config = config;
        this.coreWrapper = coreWrapper;
        this.journeyGetAccessToken = journeyGetAccessToken;
        this.journeyCustomerLookUp = journeyCustomerLookUp;
        this.forgerockToken = forgerockToken;
        this.userDetails = userDetails;
    }

    /**
     * @param context
     * @return Action, Which will redirect to next action.
     */
    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("*********************JourneyEnrollmentLookUp node********************");
        JsonValue sharedState = context.sharedState;
        Action action;
        String username = sharedState.get(USERNAME).asString();
        Integer counter = setCounterValue(context);
        String uniqueIdentifier = config.uniqueIdentifier();
        Boolean flag = false;
        JSONObject jsonResponse;
        String token = null;
        Integer timeToLive;
        if (counter == 1) {
            action = checkRequiredValues(context);
            if (action != null) {
                return action;
            }
            action = checkUserAndCreateToken(context, username);
            if (action != null) {
                return action;
            }

        } else if (counter == 2) {
            sharedState.put(Constants.COUNTER, null);
            logger.debug("selected unique identifier is: " + uniqueIdentifier);
            action = checkUniqueIdentifier(uniqueIdentifier, context, username);
            if (action != null) {
                return action;
            }
            String tokenId = sharedState.get(Constants.TOKEN_ID).asString();
            String groupName = config.groupName();
            String accountId = config.accountId();
            timeToLive = sharedState.get(Constants.TIME_TO_LIVE).asInteger();
            sharedState.put(Constants.ACCOUNT_ID, accountId);
            Boolean result = userDetails.getDetails(context, tokenId, groupName, username);
            if (!result) {
                sharedState.put(Constants.ERROR_MESSAGE, "Invalid forgerock group");
                return goTo(Outcome.Message).replaceSharedState(sharedState).build();
            }
            Boolean isAdmin = sharedState.get(Constants.IS_ADMIN).asBoolean();
            jsonResponse = journeyGetAccessToken.createAccessToken(context, timeToLive);
            try {
                if (jsonResponse.has("token")) {
                    token = jsonResponse.getString("token");
                    sharedState.put(Constants.API_ACCESS_TOKEN, token);
                }
            } catch (Exception e) {
                logger.error(Arrays.toString(e.getStackTrace()));
                throw new NodeProcessException("Exception is: " + e);
            }
            if (token != null) {
                JSONArray enrollments = journeyCustomerLookUp.customerLookUp(context);
                ArrayList<String> adminUserPriorities = Priorities.getAdminUserPriorities();
                ArrayList<String> nonAdminUserPriorities = Priorities.getNonAdminUserPriorities();
                Integer responseCode = sharedState.get(Constants.CUSTOMER_LOOKUP_RESPONSE_CODE).asInteger();
                if (responseCode == 464) {
                    action = nonExistentCustomer(isAdmin, context, username, adminUserPriorities);
                    if (action != null) {
                        return action;
                    }
                } else {//customer found in journey customer lookup call api
                    if (enrollments != null) {

                        // this code block will ensure that the user enrollment/authentication policy set by the forgerock admin will be implemented
                        // as an example we are considering here that a forgerock admin set up the policy where admin users can only enroll/authenticate
                        // only via facial-biometrics/mobile app and any other user will be using mobile app method for enrollment/authentication

                        if (isAdmin) {
                            flag = Priorities.methodCheck(adminUserPriorities, enrollments, sharedState, isAdmin);
                        } else {
                            flag = Priorities.methodCheck(nonAdminUserPriorities, enrollments, sharedState, isAdmin);
                            if (!flag) {
                                logger.debug("No suitable enrollment method found for journey customer");
                                sharedState.put(Constants.ERROR_MESSAGE, "No suitable enrollment method found for journey customer");
                                return goTo(Outcome.Message).replaceSharedState(sharedState).build();
                            }
                        }
                    } else {//this code block will only execute if the customer doesn't have any enrollment
                        if (isAdmin) {
                            sharedState.put(Constants.METHOD_NAME, adminUserPriorities.get(0));
                        } else {
                            logger.debug("No enrollment method found for journey customer");
                            sharedState.put(Constants.ERROR_MESSAGE, "No enrollment method found for journey customer");
                            return goTo(Outcome.Message).replaceSharedState(sharedState).build();
                        }
                    }
                }
                if ((sharedState.get(Constants.METHOD_NAME).asString() == Constants.FACIAL_BIOMETRIC) && (sharedState.get(Constants.JOURNEY_PHONE_NUMBER).isNull() && sharedState.get(Constants.FORGEROCK_PHONE_NUMBER).isNull())) {
                    sharedState.put(Constants.ERROR_MESSAGE, "User phone number is required to proceed");
                    return goTo(Outcome.Message).replaceSharedState(sharedState).build();
                } else if ((sharedState.get(Constants.METHOD_NAME).asString() == Constants.MOBILE_APP) && sharedState.get(Constants.DEVICE_ID).isNull()) {
                    sharedState.put(Constants.ERROR_MESSAGE, "Mobile device id is required to proceed");
                    return goTo(Outcome.Message).replaceSharedState(sharedState).build();
                }
                if (flag) {
                    logger.debug("connecting with has enrollments");
                    sharedState.put(Constants.TYPE, "Authentication");
                    return goTo(Outcome.Has_Enrollments).replaceSharedState(sharedState).build();
                } else {
                    logger.debug("connecting with no enrollment");
                    sharedState.put(Constants.TYPE, "Enrollment");
                    return goTo(Outcome.No_Enrollments).replaceSharedState(sharedState).build();
                }
            }
        }
        logger.debug("An unexpected error occurred");
        sharedState.put(Constants.ERROR_MESSAGE, "An unexpected error occurred");
        return goTo(Outcome.Message).replaceSharedState(sharedState).build();
    }

    private Action checkUniqueIdentifier(String uniqueIdentifier, TreeContext context, String username) {
        JsonValue sharedState = context.sharedState;
        if (uniqueIdentifier == null || uniqueIdentifier.equalsIgnoreCase(Constants.UNIQUE_IDENTIFIER_USERNAME)) {
            sharedState.put(Constants.UNIQUE_ID, username);
        } else if (uniqueIdentifier.equalsIgnoreCase(Constants.UNIQUE_IDENTIFIER_EMAIL)) {
            if (sharedState.get(Constants.FORGEROCK_EMAIL).isNotNull()) {
                sharedState.put(Constants.UNIQUE_ID, sharedState.get(Constants.FORGEROCK_EMAIL));
            } else {
                sharedState.put(Constants.ERROR_MESSAGE, "Forgerock email id is required to proceed");
                return goTo(Outcome.Message).replaceSharedState(sharedState).build();
            }
        } else if (uniqueIdentifier.equalsIgnoreCase(Constants.UNIQUE_IDENTIFIER_FORGEROCK_ID)) {
            if (sharedState.get(Constants.FORGEROCK_ID).isNotNull()) {
                sharedState.put(Constants.UNIQUE_ID, sharedState.get(Constants.FORGEROCK_ID));
            } else {
                sharedState.put(Constants.ERROR_MESSAGE, "Forgerock id is required to proceed");
                return goTo(Outcome.Message).replaceSharedState(sharedState).build();
            }
        } else {
            sharedState.put(Constants.ERROR_MESSAGE, "Invalid unique identifier provided");
            return goTo(Outcome.Message).replaceSharedState(sharedState).build();
        }
        return null;
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

    private Action nonExistentCustomer(Boolean isAdmin, TreeContext context, String username, ArrayList<String> adminUserPriorities) {
        logger.debug("non journey existent customer:: " + username);
        JsonValue sharedState = context.sharedState;
        if (isAdmin) {
            sharedState.put(Constants.METHOD_NAME, adminUserPriorities.get(0));
        } else {
            logger.debug("No suitable enrollment method found for non journey existent user");
            sharedState.put(Constants.ERROR_MESSAGE, "No enrollment method found for non journey existent user");
            return goTo(Outcome.Message).replaceSharedState(sharedState).build();
        }
        return null;
    }

    private Action checkRequiredValues(TreeContext context) {
        JsonValue sharedState = context.sharedState;
        Integer retrieveTimeout = config.retrieveTimeout() == null || (config.retrieveTimeout() != null && String.valueOf(config.retrieveTimeout()).isBlank()) ? 0 : Math.abs(config.retrieveTimeout());
        Integer retrieveDelay = config.retrieveDelay() == null || (config.retrieveDelay() != null && String.valueOf(config.retrieveDelay()).isBlank()) ? 0 : Math.abs(config.retrieveDelay());
        Integer requestTimeout = config.requestTimeout() == null || (config.requestTimeout() != null && String.valueOf(config.requestTimeout()).isBlank()) ? 0 : Math.abs(config.requestTimeout());
        Integer timeToLive = config.timeToLive() == null || (config.timeToLive() != null && String.valueOf(config.timeToLive()).isBlank()) ? 0 : Math.abs(config.timeToLive());
        if (config.refreshToken() == null || config.accountId() == null || config.uniqueIdentifier() == null ||
                config.adminUsername() == null || config.adminPassword() == null || config.groupName() == null ||
                config.forgerockHostUrl() == null || retrieveTimeout == 0 || retrieveDelay == 0 || requestTimeout == 0 || timeToLive == 0) {
            logger.error("Please configure refresh token/account id/unique identifier/adminUsername/adminPassword/groupName/ForgeRock Host URL/retrieveTimeout/retrieveDelay/requestTimeout/timeToLive to proceed");
            sharedState.put(Constants.ERROR_MESSAGE, "Please configure refresh token/account id/unique identifier/adminUsername/adminPassword/groupName/ForgeRock Host URL/retrieveTimeout/retrieveDelay/requestTimeout/timeToLive to proceed");
            return goTo(JourneyEnrollmentLookUp.Outcome.Message).replaceSharedState(sharedState).build();
        }
        sharedState.put(Constants.RETRIEVE_TIMEOUT, retrieveTimeout);
        sharedState.put(Constants.RETRIEVE_DELAY, retrieveDelay);
        sharedState.put(Constants.REFRESH_TOKEN, config.refreshToken());
        sharedState.put(Constants.REQUEST_TIMEOUT, requestTimeout);
        sharedState.put(Constants.TIME_TO_LIVE, timeToLive);

        return null;
    }

    private Action checkUserAndCreateToken(TreeContext context, String username) throws NodeProcessException {
        JsonValue sharedState = context.sharedState;
        sharedState.put(Constants.FORGEROCK_HOST_URL, config.forgerockHostUrl());
        Boolean flag = ForgerockUser.getDetails(username, coreWrapper, context);
        if (!flag) {
            sharedState.put(Constants.ERROR_MESSAGE, "Invalid forgerock username");
            return goTo(JourneyEnrollmentLookUp.Outcome.Message).replaceSharedState(sharedState).build();
        }
        String adminUsername = config.adminUsername();
        String adminPassword = config.adminPassword();
        Boolean result = forgerockToken.createToken(adminUsername, adminPassword, context);
        if (!result) {
            sharedState.put(Constants.ERROR_MESSAGE, "Invalid forgerock admin username/password");
            return goTo(JourneyEnrollmentLookUp.Outcome.Message).replaceSharedState(sharedState).build();
        }
        List<Callback> cbList = new ArrayList<>();
        ScriptTextOutputCallback scriptTextOutputCallback = new ScriptTextOutputCallback(f1());
        cbList.add(scriptTextOutputCallback);
        return send(ImmutableList.copyOf(cbList)).build();
    }

    private Action.ActionBuilder goTo(JourneyEnrollmentLookUp.Outcome outcome) {
        return Action.goTo(outcome.name());
    }

    /**
     * The possible outcomes for the EnrollmentStatusCheck.
     */
    public enum Outcome {
        /**
         * selection of Has_Enrollments.
         */
        Has_Enrollments,
        /**
         * selection for No_Enrollments.
         */
        No_Enrollments,
        /**
         * selection for Message.
         */
        Message

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
            ResourceBundle bundle = locales.getBundleInPreferredLocale(JourneyEnrollmentLookUp.BUNDLE, JourneyEnrollmentLookUp.Outcome.class.getClassLoader());
            return ImmutableList.of(new Outcome(JourneyEnrollmentLookUp.Outcome.Has_Enrollments.name(),
                            bundle.getString("hasEnrollments")),
                    new Outcome(JourneyEnrollmentLookUp.Outcome.No_Enrollments.name(),
                            bundle.getString("noEnrollments")), new Outcome(JourneyEnrollmentLookUp.Outcome.Message.name(),
                            bundle.getString("message")));
        }
    }

    /**
     * This function will create return a javascript based script .
     */
    public String f1() {
        return "document.getElementById('loginButton_0').style.display='none';\n" + "document.getElementById('loginButton_0').click()";
    }
}