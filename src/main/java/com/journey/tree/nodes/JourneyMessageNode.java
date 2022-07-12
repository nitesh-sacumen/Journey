/**
 * @author Sacumen(www.sacumen.com) JourneyMessageNode node with
 * single outcome. This node will display error message
 * that is not allowing authentication/enrollment process to initiate
 */

package com.journey.tree.nodes;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.journey.tree.config.Constants;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.TextOutputCallback;
import java.util.ArrayList;
import java.util.List;

import static org.forgerock.openam.auth.node.api.Action.send;
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class, configClass =
        JourneyMessageNode.Config.class)
public class JourneyMessageNode extends SingleOutcomeNode {

    private final Config config;
    private final static Logger logger = LoggerFactory.getLogger(JourneyMessageNode.class);

    /**
     * Configuration for the node.
     */
    public interface Config {

    }

    /**
     * Create the node.
     *
     * @param config The service config.
     */
    @Inject
    public JourneyMessageNode(@Assisted Config config) {
        this.config = config;
    }

    /**
     * @param context
     * @return Action, Which will redirect to next action.
     */
    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("*********************JourneyMessageNode node********************");
        List<Callback> cbList = new ArrayList<>();
        if (!context.hasCallbacks()) {
            JsonValue sharedState = context.sharedState;
            String errorMessage;
            if (sharedState.get(Constants.ERROR_MESSAGE).isNotNull()) {
                cbList.add(getTextOutputCallbackObject("Oops! There was an error in processing your request due to the following:"));
                errorMessage = "** " + sharedState.get(Constants.ERROR_MESSAGE).asString();
                cbList.add(getTextOutputCallbackObject(errorMessage));
                cbList.add(getTextOutputCallbackObject("Please contact administrator"));
            }
            String[] submitButton = {"Call Support"};
            cbList.add(new ConfirmationCallback(0, submitButton, 0));
            return send(ImmutableList.copyOf(cbList)).build();
        }
        logger.debug("Unexpected error occurred, please contact administrator");
        throw new NodeProcessException("Unexpected error occurred, please contact administrator");
    }

    /**
     * @param msg Message that needs to be rendered to the user.
     * @return Text output callback
     */
    private TextOutputCallback getTextOutputCallbackObject(String msg) {
        return new TextOutputCallback(0, msg);
    }
}