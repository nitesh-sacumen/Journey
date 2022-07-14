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

    private static final String BUNDLE = "com/journey/tree/nodes/JourneyEnrollmentLookUp";

    private final Config config;

    /**
     * Configuration for the node.
     */
    public interface Config {
        @Attribute(order = 100, requiredValue = true)
        String refreshToken();

        @Attribute(order = 200, requiredValue = true)
        Integer timeToLive();

        @Attribute(order = 300, requiredValue = true)
        String accountId();

        @Attribute(order = 400, requiredValue = true)
        String uniqueIdentifier();

        @Attribute(order = 500, requiredValue = true)
        String adminUsername();

        @Attribute(order = 600, requiredValue = true)
        String adminPassword();

        @Attribute(order = 700, requiredValue = true)
        String groupName();

        @Attribute(order = 800, requiredValue = true)
        Integer retrieveTimeout();

        @Attribute(order = 900, requiredValue = true)
        Integer retrieveDelay();

        @Attribute(order = 1000, requiredValue = true)
        String forgerockHostUrl();

        @Attribute(order = 1100, requiredValue = true)
        Integer requestTimeout();

    }

    /**
     * Create the node.
     *
     * @param config The service config and coreWrapper
     */
    @Inject
    public JourneyEnrollmentLookUp(@Assisted Config config, CoreWrapper coreWrapper) {
        this.config = config;
        this.coreWrapper = coreWrapper;
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
        sharedState.put(Constants.REQUEST_TIMEOUT, config.requestTimeout());
        String username = sharedState.get(USERNAME).asString();
        Integer counter = setCounterValue(context);
        String uniqueIdentifier = config.uniqueIdentifier();
        Boolean flag = false;
        JSONObject jsonResponse;
        JourneyGetAccessToken journeyGetAccessToken;
        JourneyCustomerLookUp journeyCustomerLookUp;
        String token = null;
        Integer timeToLive;
        if (counter == 1) {
            action = checkRequiredValues(context);
            if (action != null) {
                return action;
            }
            logger.debug("selected unique identifier is: " + uniqueIdentifier);
            action = checkUniqueIdentifier(uniqueIdentifier, context, username);
            if (action != null) {
                return action;
            }
            action = checkUserAndCreateToken(context, username);
            if (action != null) {
                return action;
            }
        } else if (counter == 2) {
            sharedState.put(Constants.COUNTER, null);
            String tokenId = sharedState.get(Constants.TOKEN_ID).asString();
            String groupName = config.groupName();
            String accountId = config.accountId();
            timeToLive = config.timeToLive();
            sharedState.put(Constants.ACCOUNT_ID, accountId);
            UserDetails userDetails = new UserDetails();
            Boolean isAdmin = userDetails.getDetails(context, tokenId, groupName, username);
            sharedState.put(Constants.IS_ADMIN, isAdmin);
            journeyGetAccessToken = new JourneyGetAccessToken();
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
                journeyCustomerLookUp = new JourneyCustomerLookUp();
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
                                System.out.println("No suitable enrollment method found for journey customer");
                                sharedState.put(Constants.ERROR_MESSAGE, "No suitable enrollment method found for journey customer");
                                return goTo(Outcome.Message).replaceSharedState(sharedState).build();
                            }
                        }
                    } else {//this code block will only execute if the customer doesn't have any enrollment
                        if (isAdmin) {
                            sharedState.put(Constants.METHOD_NAME, adminUserPriorities.get(0));
                        } else {
                            logger.debug("No enrollment method found for journey customer");
                            System.out.println("No enrollment method found for journey customer");
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
        if (uniqueIdentifier.equalsIgnoreCase(Constants.UNIQUE_IDENTIFIER_USERNAME)) {
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
            sharedState.put(Constants.ERROR_MESSAGE, "Invalid user identifier provided");
            return goTo(Outcome.Message).replaceSharedState(sharedState).build();
        }
        return null;
    }

    private Integer setCounterValue(TreeContext context) {
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
            System.out.println("No suitable enrollment method found for non journey existent user");
            sharedState.put(Constants.ERROR_MESSAGE, "No enrollment method found for non journey existent user");
            return goTo(Outcome.Message).replaceSharedState(sharedState).build();
        }
        return null;
    }

    private Action checkRequiredValues(TreeContext context) {
        JsonValue sharedState = context.sharedState;
        if (config.refreshToken() == null || config.accountId() == null || config.uniqueIdentifier() == null || config.adminUsername() == null || config.adminPassword() == null || config.groupName() == null || config.retrieveTimeout() == null || config.retrieveDelay() == null || config.forgerockHostUrl() == null || config.requestTimeout() == null || config.timeToLive() == null) {
            logger.error("Please configure refresh token/timeToLive/account id/unique identifier/adminUsername/adminPassword/groupName/retrieveTimeout/retrieveDelay/ForgeRock Host URL/Request timeout to proceed");
            sharedState.put(Constants.ERROR_MESSAGE, "Please configure refresh token/timeToLive/account id/unique identifier/adminUsername/adminPassword/groupName/retrieveTimeout/retrieveDelay/ForgeRock Host URL/Request timeout to proceed");
            return goTo(JourneyEnrollmentLookUp.Outcome.Message).replaceSharedState(sharedState).build();
        }
        sharedState.put(Constants.RETRIEVE_TIMEOUT, config.retrieveTimeout());
        sharedState.put(Constants.RETRIEVE_DELAY, config.retrieveDelay());
        sharedState.put(Constants.REFRESH_TOKEN, config.refreshToken());
        return null;
    }

    private Action checkUserAndCreateToken(TreeContext context, String username) throws NodeProcessException {
        JsonValue sharedState = context.sharedState;
        sharedState.put(Constants.FORGEROCK_HOST_URL, config.forgerockHostUrl());
        Boolean flag = ForgerockUser.getDetails(username, coreWrapper, context);
        if (!flag) {
            sharedState.put(Constants.ERROR_MESSAGE, "Invalid forgerock username/ minimum length should be 8 characters");
            return goTo(JourneyEnrollmentLookUp.Outcome.Message).replaceSharedState(sharedState).build();
        }
        String adminUsername = config.adminUsername();
        String adminPassword = config.adminPassword();
        ForgerockToken forgerockToken = new ForgerockToken();
        forgerockToken.createToken(adminUsername, adminPassword, context);
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