/**
 * @author Sacumen(www.sacumen.com)
 * This class will fetch forgerock email id and phone number
 * of the provided username
 */

package com.journey.tree.util;

import com.journey.tree.config.Constants;
import com.sun.identity.idm.AMIdentity;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Set;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;

public class ForgerockUser {
    private static final Logger logger = LoggerFactory.getLogger(ForgerockUser.class);

    public static Boolean getDetails(String username, CoreWrapper coreWrapper, TreeContext context) throws NodeProcessException {
        try {
            AMIdentity userIdentity = coreWrapper.getIdentity(username, context.sharedState.get(REALM).asString());
            if (userIdentity == null) {
                return false;
            }
            logger.debug("forgerock user exist");
            Set<String> mailsList = userIdentity.getAttribute("mail");
            String mail = mailsList.stream().findFirst().orElse(null);
            JsonValue sharedState = context.sharedState;
            sharedState.put(Constants.FORGEROCK_EMAIL, mail);
            Set<String> telephoneNumberList = userIdentity.getAttribute("telephoneNumber");
            String telephoneNumber = telephoneNumberList.stream().findFirst().orElse(null);
            sharedState.put(Constants.FORGEROCK_PHONE_NUMBER, telephoneNumber);
        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new NodeProcessException("Exception is: " + e);
        }
        return true;
    }
}
