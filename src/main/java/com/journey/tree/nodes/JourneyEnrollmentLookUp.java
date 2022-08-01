/**
 * @author Sacumen(www.sacumen.com) EnrollmentStatusCheck node with
 * single outcome. This node will check for user existence in the forgerock and
 * will make journey customer lookup api call and store the result in the key name
 * journeyUser
 */

package com.journey.tree.nodes;


import com.google.inject.assistedinject.Assisted;
import com.journey.tree.config.Constants;
import com.journey.tree.util.ForgerockUser;
import com.journey.tree.util.JourneyCustomerLookUp;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.CoreWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class, configClass = JourneyEnrollmentLookUp.Config.class)
public class JourneyEnrollmentLookUp extends SingleOutcomeNode {
    private static final Logger logger = LoggerFactory.getLogger(JourneyEnrollmentLookUp.class);
    private final CoreWrapper coreWrapper;
    JourneyCustomerLookUp journeyCustomerLookUp;

    private static final String BUNDLE = "com/journey/tree/nodes/JourneyEnrollmentLookUp";

    private final Config config;

    /**
     * Configuration for the node.
     */
    public interface Config {
        @Attribute(order = 100, requiredValue = true)
        default String journeyApiToken() {
            return "";
        }

        @Attribute(order = 200, requiredValue = true)
        default String journeyAccountId() {
            return "";
        }

        @Attribute(order = 300, requiredValue = true)
        default TypeOfIdentifier uniqueIdentifier() {
            return TypeOfIdentifier.username;
        }
    }

    public enum TypeOfIdentifier {
        username,
        email,
    }

    /**
     * Create the node.
     *
     * @param config The service config and coreWrapper
     */
    @Inject
    public JourneyEnrollmentLookUp(@Assisted Config config, CoreWrapper coreWrapper, JourneyCustomerLookUp journeyCustomerLookUp) {
        this.config = config;
        this.coreWrapper = coreWrapper;
        this.journeyCustomerLookUp = journeyCustomerLookUp;
    }

    /**
     * @param context
     * @return Action, Which will redirect to next action.
     */
    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("*********************JourneyEnrollmentLookUp node********************");
        JsonValue sharedState = context.sharedState;
        String username = sharedState.get(USERNAME).asString();

        String journeyAccountId = config.journeyAccountId();
        sharedState.put(Constants.JOURNEY_ACCOUNT_ID, journeyAccountId);
        String journeyApiToken = config.journeyApiToken();
        sharedState.put(Constants.JOURNEY_API_TOKEN, journeyApiToken);
        try {
            Boolean flag = ForgerockUser.getDetails(username, coreWrapper, context);
            if (!flag) {
                logger.debug("Invalid forgerock username");
                throw new NodeProcessException("Invalid forgerock username");
            }
            String uniqueIdentifier = config.uniqueIdentifier().toString();
            logger.debug("selected unique identifier is: " + uniqueIdentifier);
            flag = checkUniqueIdentifier(uniqueIdentifier, context, username);
            if (!flag) {
                logger.debug("Invalid unique identifier/forgerock email is missing");
                throw new NodeProcessException("Invalid unique identifier/forgerock email is missing");
            }
            journeyCustomerLookUp.customerLookUp(context);
        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            throw new NodeProcessException("Exception is: " + e.getLocalizedMessage());
        }
        return goToNext().build();
    }

    private Boolean checkUniqueIdentifier(String uniqueIdentifier, TreeContext context, String username) {
        JsonValue sharedState = context.sharedState;
        if (uniqueIdentifier.equalsIgnoreCase(Constants.UNIQUE_IDENTIFIER_USERNAME)) {
            sharedState.put(Constants.UNIQUE_ID, username);
            return true;
        } else if (uniqueIdentifier.equalsIgnoreCase(Constants.UNIQUE_IDENTIFIER_EMAIL)) {
            if (sharedState.get(Constants.FORGEROCK_EMAIL).isNotNull()) {
                sharedState.put(Constants.UNIQUE_ID, sharedState.get(Constants.FORGEROCK_EMAIL));
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}