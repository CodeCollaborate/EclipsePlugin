package cceclipseplugin.preferences;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import cceclipseplugin.Activator;
import cceclipseplugin.core.PluginManager;
import cceclipseplugin.ui.dialogs.RecoverPasswordDialog;
import cceclipseplugin.ui.dialogs.WelcomeDialog;
import websocket.ConnectException;

public class GeneralPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public GeneralPreferencesPage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("CodeCollaborate preferences");
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	public void createFieldEditors() {
		Button loginButton = new Button(getFieldEditorParent(), SWT.PUSH);
		loginButton.setText("Login to CodeCollaborate");
		loginButton.addListener(SWT.Selection, (event) -> {
			Display.getDefault().asyncExec(() -> {
				Shell shell = Display.getDefault().getActiveShell();
				ISecurePreferences secureStore = SecurePreferencesFactory.getDefault();
				new WelcomeDialog(shell, secureStore).open();
			});
		});
		
		BooleanFieldEditor autoConnect = new BooleanFieldEditor(PreferenceConstants.AUTO_CONNECT, "Auto-connect on startup",
				getFieldEditorParent());
		// TODO: change when implementing other ways to connect other than on startup
		autoConnect.setEnabled(false, getFieldEditorParent());
		
		Button reconnect = new Button(getFieldEditorParent(), SWT.PUSH);
		reconnect.setText("Reconnect to Server");
		reconnect.addListener(SWT.Selection, (event) -> {
			 new Thread(() -> {
				 try {
					 PluginManager.getInstance().getWSManager().connect();
				 } catch (ConnectException e) {
					 e.printStackTrace();
				 }
			 }).start();
		});

		Button forgotPassword = new Button(getFieldEditorParent(), SWT.PUSH);
		forgotPassword.setText("Forgotten Password?");
		forgotPassword.addListener(SWT.Selection, (event) -> {
			new Thread(() -> {
				Display.getDefault().asyncExec(() -> new RecoverPasswordDialog(getShell()).open());
			}).start();
		});
		addField(autoConnect);
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

}