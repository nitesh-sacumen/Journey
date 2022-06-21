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

    //this block will for setting configuration variables
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
        //if refresh token/accountId is empty, the flow will not proceed
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
            //making call to getAccessToken method of JourneyGetAccessToken to get the token
            jsonResponse = journeyGetAccessToken.getAccessToken(context);
            if (jsonResponse.has("token")) {
                token = jsonResponse.getString("token");
               logger.debug("token is " + token);
                sharedState.put(Constants.API_ACCESS_TOKEN, token);
            }
        //this code block will only execute if we have a token

        if (token != "") {
            JourneyCustomerLookUp journeyCustomerLookUp = new JourneyCustomerLookUp();

            //calling customerLookUp method

            jsonResponse = journeyCustomerLookUp.customerLookUp(context);

            // of JourneyCustomerLookUp class which will make call to customer lookup api

            if (jsonResponse != null) {

                //setting enrollment/authentication priority array for admin user

                ArrayList<String> adminUserPriorities = new ArrayList<>();
                adminUserPriorities.add(Constants.FACIAL_BIOMETRIC);

                //for testing purpose

                adminUserPriorities.add(Constants.ONE_TIME_PASSWORD);
                adminUserPriorities.add(Constants.MOBILE_APP);

                //setting enrollment/authentication priority array for non admin user

                ArrayList<String> otherUserPriorities = new ArrayList<>();

                //for testing purpose

                otherUserPriorities.add(Constants.ONE_TIME_PASSWORD);
                otherUserPriorities.add(Constants.MOBILE_APP);

                //hard coded for now, fetch user roles from forgerock

                String userRoles = "{admin}";
                Boolean isAdmin = userRoles.contains("admin");
                sharedState.put(Constants.IS_ADMIN, isAdmin);
                String forgeRockUserPhoneNumber;

                //hard coded for now, fetch forgerock user phone number

                forgeRockUserPhoneNumber = "+919812345678";
                sharedState.put(Constants.FORGEROCK_PHONE_NUMBER, forgeRockUserPhoneNumber);

                //fetching response code of customer lookup call api

                Integer responseCode = sharedState.get(Constants.CUSTOMER_LOOKUP_RESPONSE_CODE).asInteger();

                //response code 464 means that the customer is non existant
                //so he will be directly connected with no enrollments flow

                if (responseCode == 464)
                {
                    logger.debug("nonexistant customer::" + username);

                    //if the user is admin then assign first method name from the admin priority array

                    if (isAdmin) {
                        sharedState.put(Constants.METHOD_NAME, adminUserPriorities.get(0));
                    }

                    //if the user is non admin then assign first method name from the non admin priority array

                    else {
                        sharedState.put(Constants.METHOD_NAME, otherUserPriorities.get(0));
                    }

                }
                //this code block will execute if the customer is already registered with journey

                else {
                    try {
                        JSONArray enrollments;
                        String forgeRockSessionId = "";//hard coded, fetch session id from forgerock
                        sharedState.put(Constants.FORGEROCK_SESSION_ID, forgeRockSessionId);
                        String customerJourneyId;

                        //fetching customer's journey id from the customer lookup api response

                        if (jsonResponse.has("id")) {
                            customerJourneyId = (String) jsonResponse.get("id");
                            sharedState.put(Constants.CUSTOMER_JOURNEY_ID, customerJourneyId);
                        }

                        //fetching customer's phone number from the customer lookup api response

                        if (jsonResponse.has("phoneNumbers")) {
                            JSONArray phoneNumbers = (JSONArray) jsonResponse.get("phoneNumbers");
                            if (phoneNumbers.length() > 0) {
                                String phoneNumber = phoneNumbers.getString(0);
                                sharedState.put(Constants.JOURNEY_PHONE_NUMBER, phoneNumber);
                            }
                        }
                        //fetching customer's enrollments from the customer lookup api response

                        //this code block will only execute if the customer is having any enrollments

                        if (jsonResponse.has("enrollments")) {

                            //fetching the enrollments

                            enrollments = (JSONArray) jsonResponse.get("enrollments");

                            // this code block will ensure that the user enrollment/authentication policy set by the forgerock admin will be implemented
                            // as an example we are considering here that a forgerock admin set up the policy where admin users can only enroll/authenticate
                            // only via facial-biometrics/mobile app and any other user will be using mobile app method for enrollment/authentication

                            System.out.println("existing enrollments are:: " + enrollments.toString());

                            //if the customer has admin role

                            if (isAdmin) {

                                //methodCheck method will receive priorities array along with existing enrollments
                                // to determine if the customer should be connected with has or no enrollments

                                //if the flag value returned is true then the customer will be connected with has enrollments
                                // else no enrollments in case of flag value false

                                flag = methodCheck(adminUserPriorities, enrollments, sharedState);
                            }

                            //if the user doesnt has admin role


                            else
                            {
                                flag = methodCheck(otherUserPriorities, enrollments, sharedState);
                            }
                        }

                        //this code block will only execute if the customer doesn't have any enrollment

                        else {
                            if (isAdmin) {
                                sharedState.put(Constants.METHOD_NAME, adminUserPriorities.get(0));
                            } else {
                                sharedState.put(Constants.METHOD_NAME, otherUserPriorities.get(0));
                            }
                        }
                    }

                    //if any exception is thrown, it will be handled by this code block

                    catch (Exception e) {
                        logger.error(e.getStackTrace().toString());
                        e.printStackTrace();
                    }
                }

                //if the flag value is true, connect the customer with has enrollments

                if (flag) {
                    logger.debug("connecting with has enrollments");
                    sharedState.put(Constants.TYPE, "Authentication");
                    return goTo(EnrollmentStatusCheckOutcome.Has_Enrollments).replaceSharedState(sharedState).build();
                }

                //if the flag value is false, connect the customer with no enrollment

                else {
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

    //this method will decide whether the customer should be connected with has enrollments or no enrollment flow
    public Boolean methodCheck(ArrayList<String> priorities, JSONArray enrollments, JsonValue sharedState) {

        //iterating the priorities array received

        for (String method : priorities) {

            //if the nth method name of priorities array has an occurrence
            // in the enrollments array (fetched from customer look up call)
            //set that method name to be used for enrollments/authentication

            if (enrollments.toString().contains(method)) {
                logger.debug("choosen method name is " + method);
                sharedState.put(Constants.METHOD_NAME, method);
                return true;
            }
        }

        //if the nth method name of priorities array doesn't have an occurrence
        // in the enrollments array (fetched from customer look up call)
        //set first method name from the priorities array to be used for enrollments/authentication

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


    //this code block will generate 2 node outcomes i.e. has enrollments and no enrollments
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