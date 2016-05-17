package cceclipseplugin.startup;

import org.eclipse.ui.IStartup;

import cceclipseplugin.core.PluginManager;

public class StartupHandler implements IStartup {

	@Override
	public void earlyStartup() {
		PluginManager.getInstance();
	}
}
