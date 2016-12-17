package cceclipseplugin.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import cceclipseplugin.Activator;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(PreferenceConstants.P_BOOLEAN, false);
		store.setDefault(PreferenceConstants.P_CHOICE, "choice2");
		store.setDefault(PreferenceConstants.P_STRING,
				"Default value");
		store.setDefault(PreferenceConstants.AUTO_CONNECT, true);
	}
}
