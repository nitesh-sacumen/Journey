/**
 * @author Sacumen(www.sacumen.com) OutcomeNode node with
 * single outcome. This node will display enrollment/authentication outcome
 * message to the user
 */

package com.journey.tree.nodes;

import com.google.common.collect.ImmutableList;
import com.journey.tree.config.Constants;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.TextOutputCallback;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static org.forgerock.openam.auth.node.api.Action.send;

@Node.Metadata(outcomeProvider = OutcomeNode.OutcomeProvider.class, configClass = OutcomeNode.Config.class)
public class OutcomeNode implements Node {

    private static final Logger logger = LoggerFactory.getLogger(OutcomeNode.class);

    private static final String BUNDLE = "com/journey/tree/nodes/OutcomeNode";

    /**
     * Configuration for the node.
     */
    public interface Config {

    }

    List<Callback> cbList = new ArrayList<>();

    /**
     * Displays enrollment/authentication outcome message to the user.
     *
     * @param context
     * @return Action, Which will redirect to next action.
     */
    private Action collectRegField(TreeContext context) {
        JsonValue sharedState = context.sharedState;
        try {
            String executionStatus;
            if (sharedState.get(Constants.EXECUTION_STATUS).isNotNull()) {
                executionStatus = sharedState.get(Constants.EXECUTION_STATUS).asString();
                String type = sharedState.get(Constants.TYPE).asString();
                if (Objects.equals(executionStatus, Constants.EXECUTION_COMPLETED)) {
                    cbList.add(getTextOutputCallbackObject(type + " Complete"));
                    cbList.add(getTextOutputCallbackObject("Thank you!"));
                    cbList.add(getTextOutputCallbackObject("Your " + type + " is complete."));
                    return send(ImmutableList.copyOf(cbList)).build();
                } else if (Objects.equals(executionStatus, Constants.EXECUTION_FAILED)) {
                    cbList.add(getTextOutputCallbackObject(type + " Failed"));
                    cbList.add(getTextOutputCallbackObject("Oops, Your " + type + " has Failed."));
                    return send(ImmutableList.copyOf(cbList)).build();
                } else if (Objects.equals(executionStatus, Constants.EXECUTION_TIMEOUT)) {
                    cbList.add(getTextOutputCallbackObject("Your " + type + " has a timeout."));
                    cbList.add(getTextOutputCallbackObject("Try again!"));
                    String[] choices = {"Retry"};
                    cbList.add(new ConfirmationCallback(0, choices, 0));
                    return send(ImmutableList.copyOf(cbList)).build();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.debug("no execution status found");
        return null;
    }

    /**
     * @param context
     * @return Action, Which will redirect to next action.
     */
    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("*********************Outcome node********************");
        try {
            if (!context.hasCallbacks()) {
                List<Callback> cbList = new ArrayList<>();
                HiddenValueCallback hiddenValueCallback = new HiddenValueCallback("h1");
                cbList.add(hiddenValueCallback);
                ScriptTextOutputCallback scriptTextOutputCallback = new ScriptTextOutputCallback(f1());
                cbList.add(scriptTextOutputCallback);
                return send(ImmutableList.copyOf(cbList)).build();
            } else if (context.getCallback(HiddenValueCallback.class).isPresent()) {
                return collectRegField(context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Action action = checkExecutionStatus(context);
        return action;
    }

    /**
     * This function will create return a javascript based script .
     */
    String f1() {
        return "if (document.contains(document.getElementById('waitHeader'))) {\n" +
                "document.getElementById('waitHeader').remove();\n" +
                "}\n" +
                "document.getElementById('loginButton_0').style.display='none';\n" +
                "document.getElementById('loginButton_0').click();\n";
    }

    private Action checkExecutionStatus(TreeContext context) {
        JsonValue sharedState = context.sharedState;
        String executionStatus = sharedState.get(Constants.EXECUTION_STATUS).asString();
        if (Objects.equals(executionStatus, Constants.EXECUTION_COMPLETED)) {
            return goTo(Outcome.Success).replaceSharedState(sharedState).build();
        } else if (Objects.equals(executionStatus, Constants.EXECUTION_FAILED)) {
            return goTo(Outcome.Failure).replaceSharedState(sharedState).build();
        } else if (Objects.equals(executionStatus, Constants.EXECUTION_TIMEOUT)) {
            return goTo(Outcome.Timeout).replaceSharedState(sharedState).build();
        }
        return null;
    }

    /**
     * @param msg Message that needs to be rendered to the user.
     * @return Text output callback
     */
    private TextOutputCallback getTextOutputCallbackObject(String msg) {
        return new TextOutputCallback(0, msg);
    }


    private Action.ActionBuilder goTo(Outcome outcome) {
        return Action.goTo(outcome.name());
    }

    /**
     * The possible outcomes for the JourneyPipeline.
     */
    public enum Outcome {
        Success,
        Failure,
        Timeout

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
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeNode.OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(new Outcome(OutcomeNode.Outcome.Success.name(), bundle.getString("success")),
                    new Outcome(OutcomeNode.Outcome.Failure.name(), bundle.getString("failure")),
                    new Outcome(OutcomeNode.Outcome.Timeout.name(), bundle.getString("timeout")));
        }
    }
}