package net.montoyo.mcef.utilities;

import org.apache.logging.log4j.Level;

import net.minecraftforge.fml.common.FMLLog;

/**
 * A set of functions to log messages into the MCEF log channel.
 * @author montoyo
 *
 */
public class Log {
	
	public static void info(String what, Object ... data) {
		FMLLog.log("MCEF", Level.INFO, what, data);
	}
	
	public static void warning(String what, Object ... data) {
		FMLLog.log("MCEF", Level.WARN, what, data);
	}
	
	public static void error(String what, Object ... data) {
		FMLLog.log("MCEF", Level.ERROR, what, data);
	}

}
