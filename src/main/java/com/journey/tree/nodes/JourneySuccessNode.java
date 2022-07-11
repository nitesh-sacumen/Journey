package com.journey.tree.nodes;

import com.google.common.collect.ImmutableList;
import com.journey.tree.config.Constants;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.TextOutputCallback;
import java.util.ArrayList;
import java.util.List;

import static org.forgerock.openam.auth.node.api.Action.send;

/**
 * @author Saucmen(www.sacumen.com) Verification Success node with
 * single outcome. This node will render Success message to user.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class, configClass = JourneySuccessNode.Config.class)
public class JourneySuccessNode extends SingleOutcomeNode {

    private final static Logger logger = LoggerFactory.getLogger(JourneySuccessNode.class);

    /**
     * Configuration for the node.
     */
    public interface Config {

    }

    List<Callback> cbList = new ArrayList<>();

    private Action collectRegField(TreeContext context) {
        JsonValue sharedState = context.sharedState;
        String type = sharedState.get(Constants.TYPE).asString();
        cbList.add(getTextOutputCallbackObject(type + " Complete"));
        cbList.add(getTextOutputCallbackObject("Thank you!"));
        cbList.add(getTextOutputCallbackObject("Your " + type + " is complete."));
        String[] choices = {"Next"};
        cbList.add(new ConfirmationCallback(0, choices, 0));
        return send(ImmutableList.copyOf(cbList)).build();
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("*********************Success node********************");
        if (!context.hasCallbacks()) {
            List<Callback> cbList = new ArrayList<>();
            HiddenValueCallback hiddenValueCallback=new HiddenValueCallback("h1");
            cbList.add(hiddenValueCallback);
            ScriptTextOutputCallback scriptTextOutputCallback = new ScriptTextOutputCallback(f1());
            cbList.add(scriptTextOutputCallback);
            return send(ImmutableList.copyOf(cbList)).build();
        }
        else if(context.getCallback(HiddenValueCallback.class).isPresent())
        {
            return collectRegField(context);
        }
        else {
            return goToNext().build();
        }
    }

    String f1() {
        return "if (document.contains(document.getElementById('waitHeader'))) {\n" +
                "document.getElementById('waitHeader').remove();\n" +
                "document.getElementById('loginButton_0').click();\n"+
                "}\n";
    }
    private TextOutputCallback getTextOutputCallbackObject(String msg) {
        return new TextOutputCallback(0, msg);
    }
}