package cceclipseplugin.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;

/**
 * Logs Eclipse created status logs to the logging system configured in
 * ClientCore
 *
 */
public class LogListener implements ILogListener {

	private Logger logger = LogManager.getLogger("eclipseLogger");

	@Override
	public void logging(IStatus status, String plugin) {
		logger.info(status.toString());
	}
}
