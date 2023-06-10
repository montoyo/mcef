package net.montoyo.mcef.utilities;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A set of functions to log messages into the MCEF log channel.
 * @author montoyo
 *
 */
public class Log {
    
    private static final Logger LOGGER = LogManager.getLogger("MCEF");
    
    public static void info(String what, Object ... data) {
        LOGGER.log(Level.INFO, String.format(what, data));
    }
    
    public static void warning(String what, Object ... data) {
        LOGGER.log(Level.WARN, String.format(what, data));
    }
    
    public static void error(String what, Object ... data) {
        LOGGER.log(Level.ERROR, String.format(what, data));
    }

    public static void errorEx(String what, Throwable t, Object ... data) {
        LOGGER.log(Level.ERROR, String.format(what, data), t);
    }

}
