package cceclipseplugin;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import cceclipseplugin.core.PluginManager;
import cceclipseplugin.preferences.PreferenceConstants;
import cceclipseplugin.ui.dialogs.WelcomeDialog;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "CCEclipsePlugin"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		PluginManager.getInstance();
		
		IPreferenceStore prefStore = getPreferenceStore();
		String username = prefStore.getString(PreferenceConstants.USERNAME);
		String password = prefStore.getString(PreferenceConstants.PASSWORD);
		boolean showWelcomeDialog = (username == null || username.equals("") || password == null || password.equals(""));
		if (showWelcomeDialog) {
			WelcomeDialog dialog = new WelcomeDialog(new Shell(), prefStore);
			Display.getDefault().asyncExec(() -> dialog.open());
		} else {
			if (prefStore.getBoolean(PreferenceConstants.AUTO_CONNECT)) {
				new Thread(() -> {
					PluginManager.getInstance().getRequestManager().loginAndSubscribe(username, password);
				}).start();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}
