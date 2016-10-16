package cceclipseplugin;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import cceclipseplugin.core.PluginManager;
import cceclipseplugin.ui.ControlPanel;
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
		
//		boolean welcomeDialogShown = getPreferenceStore().getBoolean("welcomeDialogShown");
//		if (!welcomeDialogShown) {
//			WelcomeDialog dialog = new WelcomeDialog(new Shell());
//			dialog.open();
//		}
		
//		ControlPanel cp = (ControlPanel) PluginManager.getInstance().getUIManager().getControlView();
//		cp.setEnabled(false);
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
