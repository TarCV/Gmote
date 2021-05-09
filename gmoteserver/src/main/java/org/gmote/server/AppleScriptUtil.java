package org.gmote.server;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppleScriptUtil {
    private static final Logger LOGGER = Logger.getLogger(AppleScriptUtil.class.getName());
    private final ScriptEngine scriptEngine;

    public AppleScriptUtil() {
        scriptEngine = new ScriptEngineManager().getEngineByName("AppleScript");
    }

    public Object eval(String script) {
        try {
            return scriptEngine.eval(script, scriptEngine.getContext());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during executing AppleScript", e);
            return null;
        }
    }

    public static int convertToInt(Object result) {
        if (result instanceof Number) {
            return ((Number)result).intValue();
        } else if (result instanceof String) {
            return Integer.parseInt((String) result);
        } else if (result == null) {
            throw new RuntimeException("Unknown result from AppleScript: null");
        } else {
            throw new RuntimeException("Unknown result from AppleScript: " + result.getClass().getCanonicalName());
        }
    }
    public static boolean convertToBoolean(Object result) {
        if (result instanceof Boolean) {
            return (boolean) result;
        } else if (result instanceof String) {
            return Boolean.parseBoolean((String) result);
        } else if (result == null) {
            return false;
        } else {
            throw new RuntimeException("Unknown result from AppleScript: " + result.getClass().getCanonicalName());
        }
    }
}
