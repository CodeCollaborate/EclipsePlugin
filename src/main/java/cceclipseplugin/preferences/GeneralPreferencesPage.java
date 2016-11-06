package cceclipseplugin.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import cceclipseplugin.Activator;
import cceclipseplugin.core.PluginManager;
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
		StringFieldEditor userBox = new StringFieldEditor(PreferenceConstants.USERNAME,
				"Username:", getFieldEditorParent());
		StringFieldEditor pwBox = new StringFieldEditor(PreferenceConstants.PASSWORD,
				"Password:", getFieldEditorParent()) {
			@Override
			protected void doFillIntoGrid(Composite parent, int numColumns) {
				super.doFillIntoGrid(parent, numColumns);
				getTextControl().setEchoChar('*');
			}
		};
		
		BooleanFieldEditor autoConnect = new BooleanFieldEditor(PreferenceConstants.AUTO_CONNECT, "Auto-connect on startup",
				getFieldEditorParent());
		// TODO: change when implementing other ways to connect other than on startup
		autoConnect.setEnabled(false, getFieldEditorParent());

		Button forgotPassword = new Button(getFieldEditorParent(), SWT.PUSH);
		forgotPassword.setText("Forgot Password");
		// TODO: change when forgot password is implemented
		forgotPassword.setEnabled(false);
		Button changePassword = new Button(getFieldEditorParent(), SWT.PUSH);
		changePassword.setText("Change Password");
		// TODO: change when change password is implemented
		changePassword.setEnabled(false);
		
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

		addField(userBox);
		addField(pwBox);
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