package com.journey.tree.util;

import com.journey.tree.config.Constants;

import java.util.ArrayList;

public class Priorities {
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
}
