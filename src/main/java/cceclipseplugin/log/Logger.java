package cceclipseplugin.log;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;

import cceclipseplugin.Activator;

public class Logger {

	private static Logger instance;

	private ILog pluginLog;
	private boolean debugMode; // may not need debug mode

	/**
	 * Get the active instance of the Logger class.
	 *
	 * @return the instance of the Logger class
	 */
	public static Logger getInstance() {
		if (instance == null) {
			synchronized (Logger.class) {
				if (instance == null) {
					instance = new Logger();
				}
			}
		}
		return instance;
	}

	/**
	 * Gets the eclipse log for the current plugin and sets the debug mode to
	 * false by default
	 * 
	 */
	private Logger() {
		pluginLog = Activator.getDefault().getLog();
		debugMode = false;
	}

	/**
	 * Logs the given message to the logger for this plugin with the given log
	 * severity. Logging prints the message to the console in addition to
	 * writing to disk.
	 * 
	 * @param logLevel
	 * @param msg
	 */
	public void log(int logLevel, String msg) {
		pluginLog.log(new Status(logLevel, Activator.PLUGIN_ID, msg));
	}

	/**
	 * Logs given message and exception to the logger for this plugin with the
	 * given log severity. Logging prints the message and stack trace to the
	 * console in addition to writing to disk.
	 * 
	 * @param logLevel
	 * @param msg
	 * @param e
	 */
	public void logException(int logLevel, String msg, Throwable e) {
		pluginLog.log(new Status(logLevel, Activator.PLUGIN_ID, msg, e));
	}

}
