/**
 * @author Sacumen(www.sacumen.com)
 * This class will define separate priority array for admin and non admin users.
 * Also this class will have a method named methodCheck that will decide
 * which enrollment/authentication method to use
 */

package com.journey.tree.util;

import com.journey.tree.config.Constants;
import com.journey.tree.nodes.EnrollmentStatusCheck;
import org.forgerock.json.JsonValue;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class Priorities {
    private static final Logger logger = LoggerFactory.getLogger(EnrollmentStatusCheck.class);
    public static ArrayList<String> getAdminUserPriorities() {
        ArrayList<String> priorities = new ArrayList<>();
        priorities.add(Constants.FACIAL_BIOMETRIC);
        priorities.add(Constants.MOBILE_APP);
        return priorities;
    }

    public static ArrayList<String> getNonAdminUserPriorities() {
        ArrayList<String> priorities = new ArrayList<>();
        priorities.add(Constants.MOBILE_APP);
        return priorities;
    }

    //this method will decide whether the customer should be connected with has enrollments or no enrollment flow
    public static Boolean methodCheck(ArrayList<String> priorities, JSONArray enrollments, JsonValue sharedState, Boolean isAdmin) {

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
            logger.debug("chosen method name is " + priorities.get(0));
            sharedState.put(Constants.METHOD_NAME, priorities.get(0));
        }
        return false;
    }

}
