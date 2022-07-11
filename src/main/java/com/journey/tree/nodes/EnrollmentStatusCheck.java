package com.journey.tree.nodes;


import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.journey.tree.config.Constants;
import com.journey.tree.util.*;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.authentication.client.AuthClientUtils;
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


/**
 * @author Sacumen(www.sacumen.com)
 * @category Node
 * @Descrition
 */
@Node.Metadata(outcomeProvider = EnrollmentStatusCheck.EnrollmentStatusOutcomeProvider.class, configClass = EnrollmentStatusCheck.Config.class)
public class EnrollmentStatusCheck extends Thread implements Node {
    private Logger logger = LoggerFactory.getLogger(EnrollmentStatusCheck.class);
    private final CoreWrapper coreWrapper;

    private static final String BUNDLE = "com/journey/tree/nodes/EnrollmentStatusCheck";

    private final Config config;

    /**
     * Configuration for the node.
     */

    //this block will for setting configuration variables
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

    }
    @Inject
    public EnrollmentStatusCheck(@Assisted Config config, CoreWrapper coreWrapper) {
        this.config = config;
        this.coreWrapper = coreWrapper;
    }

    @Override

    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("*********************Enrollment Status Check********************");
        if (config.refreshToken() == null || config.accountId() == null || config.uniqueIdentifier() == null || config.adminUsername() == null || config.adminPassword() == null || config.groupName() == null) {
            logger.error("Please configure refresh token/timeToLive/account id/unique identifier/adminUsername/adminPassword/groupName/retrieveTimeout/retrieveDelay to proceed");
            throw new NodeProcessException("Please configure refresh token/timeToLive/account id/unique identifier/adminUsername/adminPassword/groupName/retrieveTimeout/retrieveDelay to proceed");
        }
        JsonValue sharedState = context.sharedState;
        String username = context.sharedState.get(USERNAME).asString();
        Integer counter;
        if (sharedState.get(Constants.COUNTER).isNull()) {
            counter = 1;
            sharedState.put(Constants.COUNTER, counter);
        } else {
            counter = sharedState.get(Constants.COUNTER).asInteger();
            counter++;
            sharedState.put(Constants.COUNTER, counter);
        }
        if (counter == 1) {
            {
                Boolean flag;
                flag = ForgerockUser.getDetails(username, coreWrapper, context);
                if (!flag) {
                    sharedState.put(Constants.ERROR_MESSAGE, "Invalid forgerock username/ minimum length should be 8 characters");
                    return goTo(EnrollmentStatusCheck.EnrollmentStatusCheckOutcome.Message).replaceSharedState(sharedState).build();
                }
            }
            String adminUsername = config.adminUsername();
            String adminPassword = config.adminPassword();
            ForgerockToken forgerockToken = new ForgerockToken();
            forgerockToken.getToken(adminUsername, adminPassword, context);
            List<Callback> cbList = new ArrayList<>();
            ScriptTextOutputCallback scriptTextOutputCallback = new ScriptTextOutputCallback(f1());
            cbList.add(scriptTextOutputCallback);
            return send(ImmutableList.copyOf(cbList)).build();
        } else if (counter == 2) {
            sharedState.put(Constants.COUNTER, null);
            sharedState.put(Constants.RETRIEVE_TIMEOUT,config.retrieveTimeout());
            sharedState.put(Constants.RETRIEVE_DELAY,config.retrieveDelay());
            String tokenId = sharedState.get(Constants.TOKEN_ID).asString();
            String groupName = config.groupName();
            String uniqueIdentifier = config.uniqueIdentifier();
            logger.debug("selected unique identifier is: " + uniqueIdentifier);
            if (uniqueIdentifier.equalsIgnoreCase(Constants.UNIQUE_IDENTIFIER_USERNAME)) {
                sharedState.put(Constants.UNIQUE_ID, username);
            } else if (uniqueIdentifier.equalsIgnoreCase(Constants.UNIQUE_IDENTIFIER_EMAIL)) {
                if (sharedState.get(Constants.FORGEROCK_EMAIL).isNotNull()) {
                    sharedState.put(Constants.UNIQUE_ID, sharedState.get(Constants.FORGEROCK_EMAIL));
                } else {
                    sharedState.put(Constants.ERROR_MESSAGE, "Forgerock email id is required to proceed");
                    return goTo(EnrollmentStatusCheckOutcome.Message).replaceSharedState(sharedState).build();
                }
            } else if (uniqueIdentifier.equalsIgnoreCase(Constants.UNIQUE_IDENTIFIER_FORGEROCK_ID)) {
                if (sharedState.get(Constants.FORGEROCK_ID).isNotNull()) {
                    sharedState.put(Constants.UNIQUE_ID, sharedState.get(Constants.FORGEROCK_ID));
                } else {
                    sharedState.put(Constants.ERROR_MESSAGE, "Forgerock id is required to proceed");
                    return goTo(EnrollmentStatusCheckOutcome.Message).replaceSharedState(sharedState).build();
                }
            } else {
                sharedState.put(Constants.ERROR_MESSAGE, "Invalid user identifier provided");
                return goTo(EnrollmentStatusCheckOutcome.Message).replaceSharedState(sharedState).build();
            }
            UserDetails userDetails = new UserDetails();
            Boolean isAdmin = userDetails.getDetails(context, tokenId, groupName, username);
            logger.debug("isAdmin status is:: " + isAdmin);
            sharedState.put(Constants.IS_ADMIN, isAdmin);
            String refreshToken = config.refreshToken();
            sharedState.put(Constants.REFRESH_TOKEN, refreshToken);
            String accountId = config.accountId();
            sharedState.put(Constants.ACCOUNT_ID, accountId);
            Boolean flag = false;
            JSONObject jsonResponse;
            JourneyGetAccessToken journeyGetAccessToken;
            JourneyCustomerLookUp journeyCustomerLookUp;
            String token = null;
            Integer timeToLive;
            try {
                journeyGetAccessToken = new JourneyGetAccessToken();
                timeToLive = config.timeToLive();
                jsonResponse = journeyGetAccessToken.getAccessToken(context, timeToLive);
                if (jsonResponse.has("token")) {
                    token = jsonResponse.getString("token");
                    sharedState.put(Constants.API_ACCESS_TOKEN, token);
                }
                if (token != null) {
                    journeyCustomerLookUp = new JourneyCustomerLookUp();
                    jsonResponse = journeyCustomerLookUp.customerLookUp(context);
                    if (jsonResponse != null) {
                        ArrayList<String> adminUserPriorities = Priorities.getAdminUserPriorities();
                        ArrayList<String> nonAdminUserPriorities = Priorities.getNonAdminUserPriorities();
                        Integer responseCode = sharedState.get(Constants.CUSTOMER_LOOKUP_RESPONSE_CODE).asInteger();
                        if (responseCode == 464) {
                            logger.debug("non existent customer:: " + username);
                            if (isAdmin) {
                                sharedState.put(Constants.METHOD_NAME, adminUserPriorities.get(0));
                            } else {
                                logger.debug("No suitable enrollment method found for non journey existent user");
                                System.out.println("No suitable enrollment method found for non journey existent user");
                                sharedState.put(Constants.ERROR_MESSAGE, "No enrollment method found for non journey existent user");
                                return goTo(EnrollmentStatusCheckOutcome.Message).replaceSharedState(sharedState).build();
                            }
                        } else {//customer found in journey customer lookup call api
                            try {
                                JSONArray enrollments;
                                String customerJourneyId;
                                if (jsonResponse.has("id")) {
                                    customerJourneyId = (String) jsonResponse.get("id");
                                    sharedState.put(Constants.CUSTOMER_JOURNEY_ID, customerJourneyId);
                                }
                                if (jsonResponse.has("phoneNumbers")) {
                                    JSONArray phoneNumbers = (JSONArray) jsonResponse.get("phoneNumbers");
                                    if (phoneNumbers.length() > 0) {
                                        String phoneNumber = phoneNumbers.getString(0);
                                        sharedState.put(Constants.JOURNEY_PHONE_NUMBER, phoneNumber);
                                    }
                                }
                                if (jsonResponse.has("devices")) {
                                    JSONArray devices = (JSONArray) jsonResponse.get("devices");
                                    if (devices.length() > 0) {
                                        JSONObject deviceObject = devices.getJSONObject(0);
                                        if (deviceObject.has("id")) {
                                            String deviceId = deviceObject.getString("id");
                                            sharedState.put(Constants.DEVICE_ID, deviceId);
                                        }
                                    }
                                }
                                if (jsonResponse.has("email")) {
                                    String email = jsonResponse.getString("email");
                                    sharedState.put(Constants.JOURNEY_EMAIL, email);
                                }
                                if (jsonResponse.has("enrollments")) {
                                    enrollments = (JSONArray) jsonResponse.get("enrollments");

                                    // this code block will ensure that the user enrollment/authentication policy set by the forgerock admin will be implemented
                                    // as an example we are considering here that a forgerock admin set up the policy where admin users can only enroll/authenticate
                                    // only via facial-biometrics/mobile app and any other user will be using mobile app method for enrollment/authentication

                                    if (isAdmin) {
                                        flag = methodCheck(adminUserPriorities, enrollments, sharedState, isAdmin);
                                    } else {
                                        flag = methodCheck(nonAdminUserPriorities, enrollments, sharedState, isAdmin);
                                        if (!flag) {
                                            logger.debug("No suitable enrollment method found for journey customer");
                                            System.out.println("No suitable enrollment method found for journey customer");
                                            sharedState.put(Constants.ERROR_MESSAGE, "No suitable enrollment method found for journey customer");
                                            return goTo(EnrollmentStatusCheckOutcome.Message).replaceSharedState(sharedState).build();
                                        }
                                    }
                                } else {//this code block will only execute if the customer doesn't have any enrollment
                                    if (isAdmin) {
                                        sharedState.put(Constants.METHOD_NAME, adminUserPriorities.get(0));
                                    } else {
                                        logger.debug("No enrollment method found for journey customer");
                                        System.out.println("No enrollment method found for journey customer");
                                        sharedState.put(Constants.ERROR_MESSAGE, "No enrollment method found for journey customer");
                                        return goTo(EnrollmentStatusCheckOutcome.Message).replaceSharedState(sharedState).build();
                                    }
                                }
                            } catch (Exception e) {
                                logger.error(Arrays.toString(e.getStackTrace()));
                                throw new NodeProcessException("Exception is: " + e);
                            }
                        }
                        if ((sharedState.get(Constants.METHOD_NAME).asString() == Constants.FACIAL_BIOMETRIC)
                                && (sharedState.get(Constants.JOURNEY_PHONE_NUMBER).isNull()
                                && sharedState.get(Constants.FORGEROCK_PHONE_NUMBER).isNull())) {
                            sharedState.put(Constants.ERROR_MESSAGE, "User phone number is required to proceed");
                            return goTo(EnrollmentStatusCheckOutcome.Message).replaceSharedState(sharedState).build();
                        } else if ((sharedState.get(Constants.METHOD_NAME).asString() == Constants.MOBILE_APP)
                                && sharedState.get(Constants.DEVICE_ID).isNull()) {
                            sharedState.put(Constants.ERROR_MESSAGE, "Mobile device id is required to proceed");
                            return goTo(EnrollmentStatusCheckOutcome.Message).replaceSharedState(sharedState).build();
                        }


                        if (flag) {
                            logger.debug("connecting with has enrollments");
                            sharedState.put(Constants.TYPE, "Authentication");
                            return goTo(EnrollmentStatusCheckOutcome.Has_Enrollments).replaceSharedState(sharedState).build();
                        } else {
                            logger.debug("connecting with no enrollment");
                            sharedState.put(Constants.TYPE, "Enrollment");
                            return goTo(EnrollmentStatusCheckOutcome.No_Enrollments).replaceSharedState(sharedState).build();
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(Arrays.toString(e.getStackTrace()));
                throw new NodeProcessException("Exception is: " + e);
            }
        }
        logger.debug("An unexpected error occurred");
        System.out.println("An unexpected error occurred");
        sharedState.put(Constants.ERROR_MESSAGE, "An unexpected error occurred");
        return goTo(EnrollmentStatusCheckOutcome.Message).replaceSharedState(sharedState).build();
    }

    //this method will decide whether the customer should be connected with has enrollments or no enrollment flow
    public Boolean methodCheck(ArrayList<String> priorities, JSONArray enrollments, JsonValue sharedState, Boolean isAdmin) {

        //iterating the priorities array received

        for (String method : priorities) {

            //if the nth method name of priorities array has an occurrence
            // in the enrollments array (fetched from customer look up call)
            //set that method name to be used for enrollments/authentication

            if (enrollments.toString().contains(method)) {
                logger.debug("chosen method name is " + method);
                sharedState.put(Constants.METHOD_NAME, method);
                return true;
            }
        }

        //if the nth method name of priorities array doesn't have an occurrence
        // in the enrollments array (fetched from customer look up call)
        //set first method name from the priorities array to be used for enrollments/authentication
        if (isAdmin) {
            logger.debug("choosen method name is " + priorities.get(0));
            sharedState.put(Constants.METHOD_NAME, priorities.get(0));
        }
        return false;
    }

    private Action.ActionBuilder goTo(EnrollmentStatusCheck.EnrollmentStatusCheckOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    /**
     * The possible outcomes for the EnrollmentStatusCheck.
     */
    public enum EnrollmentStatusCheckOutcome {
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

    //this code block will generate 2 node outcomes i.e. has enrollments and no enrollments
    public static class EnrollmentStatusOutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(EnrollmentStatusCheck.BUNDLE, EnrollmentStatusCheck.EnrollmentStatusOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(new Outcome(EnrollmentStatusCheckOutcome.Has_Enrollments.name(), bundle.getString("hasEnrollments")), new Outcome(EnrollmentStatusCheckOutcome.No_Enrollments.name(), bundle.getString("noEnrollments")), new Outcome(EnrollmentStatusCheckOutcome.Message.name(), bundle.getString("message")));
        }
    }

    public String f1() {
        return "document.getElementById('loginButton_0').style.display='none';\n" + "document.getElementById('loginButton_0').click()";
    }
}