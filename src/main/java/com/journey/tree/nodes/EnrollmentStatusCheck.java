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
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.journey.tree.util.JourneyCustomerLookUp;
import com.journey.tree.util.JourneyGetAccessToken;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;


/**
 * @author Sacumen(www.sacumen.com)
 * @category Node
 * @Descrition
 */
@Node.Metadata(outcomeProvider = EnrollmentStatusCheck.EnrollmentStatusOutcomeProvider.class, configClass = EnrollmentStatusCheck.Config.class)
public class EnrollmentStatusCheck implements Node {
    private Logger logger = LoggerFactory.getLogger(AuthClientUtils.class);

    private static final String BUNDLE = "com/journey/tree/nodes/EnrollmentStatusCheck";

    private final Config config;

    /**
     * Configuration for the node.
     */
    public interface Config {
        @Attribute(order = 100, requiredValue = true)
        String refreshToken();

        @Attribute(order = 200, requiredValue = true)
        String accountId();

    }

    @Inject
    public EnrollmentStatusCheck(@Assisted Config config) {
        this.config = config;
    }

    @Override

    public Action process(TreeContext context) throws NodeProcessException {
        if (config.refreshToken() == null || config.accountId() == null) {
            logger.error("Please configure refresh token/account id to proceed");
            System.out.println("Please configure refresh token/account id to proceed");
            return null;
        }
        JsonValue sharedState = context.sharedState;
        String refreshToken = config.refreshToken();
        sharedState.put(Constants.REFRESH_TOKEN, refreshToken);
        String username = sharedState.get("username").asString();
        String accountId = config.accountId();
        sharedState.put(Constants.ACCOUNT_ID, accountId);
        Boolean flag = false;
        String token = "";
        JSONObject jsonResponse;
        JourneyGetAccessToken journeyGetAccessToken = new JourneyGetAccessToken();
        try {
            jsonResponse = journeyGetAccessToken.getAccessToken(context);
            if (jsonResponse.has("token")) {
                token = jsonResponse.getString("token");
               logger.debug("token is " + token);
                sharedState.put(Constants.API_ACCESS_TOKEN, token);
            }

        if (token != "") {
            JourneyCustomerLookUp journeyCustomerLookUp = new JourneyCustomerLookUp();
            jsonResponse = journeyCustomerLookUp.customerLookUp(context);
            if (jsonResponse != null) {
                ArrayList<String> adminUserPriorities = new ArrayList<>();
                adminUserPriorities.add(Constants.FACIAL_BIOMETRIC);
                adminUserPriorities.add(Constants.ONE_TIME_PASSWORD);//for testing purpose
                adminUserPriorities.add(Constants.MOBILE_APP);

                ArrayList<String> otherUserPriorities = new ArrayList<>();
                otherUserPriorities.add(Constants.ONE_TIME_PASSWORD);//for testing purpose
                otherUserPriorities.add(Constants.MOBILE_APP);
                String userRoles = "{admin}";//hard coded for testing, fetch user roles from forgerock
                Boolean isAdmin = userRoles.contains("admin");
                sharedState.put(Constants.IS_ADMIN, isAdmin);
                String forgeRockUserPhoneNumber = "";
                forgeRockUserPhoneNumber = "+919812345678";//hard coded for testing, fetch forgerock user phone number
                sharedState.put(Constants.FORGEROCK_PHONE_NUMBER, forgeRockUserPhoneNumber);
                Integer responseCode = sharedState.get(Constants.CUSTOMER_LOOKUP_RESPONSE_CODE).asInteger();
                if (responseCode == 464)//Non Existant customer
                {
                    logger.debug("nonexistant customer::" + username);

                    if (isAdmin) {
                        sharedState.put(Constants.METHOD_NAME, adminUserPriorities.get(0));
                    } else {
                        sharedState.put(Constants.METHOD_NAME, otherUserPriorities.get(0));
                    }

                } else {
                    try {
                        JSONArray enrollments;
                        String forgeRockSessionId = "";//hard coded, fetch session id from forgerock
                        sharedState.put(Constants.FORGEROCK_SESSION_ID, forgeRockSessionId);
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
                        if (jsonResponse.has("enrollments")) {
                            enrollments = (JSONArray) jsonResponse.get("enrollments");

                            // this code block will ensure that the user enrollment/authentication policy set by the forgerock admin will be implemented
                            // as an example we are considering here that a forgerock admin set up the policy where admin users can only enroll/authenticate
                            // only via facial-biometrics/mobile app and any other user will be using mobile app method for enrollment/authentication

                            System.out.println("existing enrollments are:: " + enrollments.toString());
                            if (isAdmin) {
                                flag = methodCheck(adminUserPriorities, enrollments, sharedState);
                            } else//not admin
                            {
                                flag = methodCheck(otherUserPriorities, enrollments, sharedState);
                            }
                        } else {//case of no enrollment
                            if (isAdmin) {
                                sharedState.put(Constants.METHOD_NAME, adminUserPriorities.get(0));
                            } else {
                                sharedState.put(Constants.METHOD_NAME, otherUserPriorities.get(0));
                            }
                        }
                    } catch (Exception e) {
                        logger.error(e.getStackTrace().toString());
                        e.printStackTrace();
                    }
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
            logger.error(e.getStackTrace().toString());
            e.printStackTrace();
            throw new NodeProcessException("Exception is: " + e);
        }

        return null;

    }

    public Boolean methodCheck(ArrayList<String> priorities, JSONArray enrollments, JsonValue sharedState) {

        for (String method : priorities) {
            if (enrollments.toString().contains(method)) {
                logger.debug("choosen method name is " + method);
                sharedState.put(Constants.METHOD_NAME, method);
                return true;
            }
        }
        logger.debug("choosen method name is " + priorities.get(0));
        sharedState.put(Constants.METHOD_NAME, priorities.get(0));
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
        No_Enrollments

    }


    public static class EnrollmentStatusOutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(EnrollmentStatusCheck.BUNDLE,
                    EnrollmentStatusCheck.EnrollmentStatusOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(new Outcome(EnrollmentStatusCheckOutcome.Has_Enrollments.name(), bundle.getString("hasEnrollments")),
                    new Outcome(EnrollmentStatusCheckOutcome.No_Enrollments.name(), bundle.getString("noEnrollments")));
        }

    }
}