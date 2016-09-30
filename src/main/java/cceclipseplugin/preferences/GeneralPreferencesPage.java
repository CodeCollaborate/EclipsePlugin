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
import cceclipseplugin.constants.StringConstants;

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

		StringFieldEditor hostBox = new StringFieldEditor(StringConstants.PREFERENCES_HOSTNAME,
				StringConstants.PREFERENCES_HOSTNAME, getFieldEditorParent());
		StringFieldEditor userBox = new StringFieldEditor(StringConstants.PREFERENCES_USERNAME,
				StringConstants.PREFERENCES_USERNAME, getFieldEditorParent());
		StringFieldEditor pwBox = new StringFieldEditor(StringConstants.PREFERENCES_PASSWORD,
				StringConstants.PREFERENCES_PASSWORD, getFieldEditorParent()) {
			@Override
			protected void doFillIntoGrid(Composite parent, int numColumns) {
				super.doFillIntoGrid(parent, numColumns);
				getTextControl().setEchoChar('*');
			}
		};
		BooleanFieldEditor autoConnect = new BooleanFieldEditor("autoConnect", "Auto-connect on startup",
				getFieldEditorParent()); // perhaps make new constants in client
											// core for this preference

		Button login = new Button(getFieldEditorParent(), SWT.PUSH);
		login.setText("Login");
		Button forgotPassword = new Button(getFieldEditorParent(), SWT.PUSH);
		forgotPassword.setText("Forgot Password");
		Button changePassword = new Button(getFieldEditorParent(), SWT.PUSH);
		changePassword.setText("Change Password");

		addField(hostBox);
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