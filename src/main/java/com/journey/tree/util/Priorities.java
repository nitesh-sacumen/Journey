/**
 * @author Sacumen(www.sacumen.com)
 * This class will define separate priority array for admin and non admin users.
 * Also this class will have a method named methodCheck that will decide
 * which enrollment/authentication method to use
 */

package com.journey.tree.util;

import com.journey.tree.config.Constants;
import org.forgerock.json.JsonValue;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class Priorities {
    private static final Logger logger = LoggerFactory.getLogger(Priorities.class);

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

    public static Boolean methodCheck(ArrayList<String> priorities, JSONArray enrollments, JsonValue sharedState, Boolean isAdmin) {
        for (String method : priorities) {
            if (enrollments.toString().contains(method)) {
                logger.debug("chosen method name is " + method);
                sharedState.put(Constants.METHOD_NAME, method);
                return true;
            }
        }
        if (isAdmin) {
            logger.debug("chosen method name is " + priorities.get(0));
            sharedState.put(Constants.METHOD_NAME, priorities.get(0));
        }
        return false;
    }

}
