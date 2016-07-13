package cceclipseplugin.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import cceclipseplugin.Activator;

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

		StringFieldEditor hostBox = new StringFieldEditor(constants.StringConstants.PREFERENCES_HOSTNAME,
				constants.StringConstants.PREFERENCES_HOSTNAME, getFieldEditorParent());
		StringFieldEditor userBox = new StringFieldEditor(constants.StringConstants.PREFERENCES_USERNAME,
				constants.StringConstants.PREFERENCES_USERNAME, getFieldEditorParent());
		StringFieldEditor pwBox = new StringFieldEditor(constants.StringConstants.PREFERENCES_PASSWORD,
				constants.StringConstants.PREFERENCES_PASSWORD, getFieldEditorParent()) {
			@Override
			protected void doFillIntoGrid(Composite parent, int numColumns) {
				super.doFillIntoGrid(parent, numColumns);
				getTextControl().setEchoChar('*');
			}
		};

		addField(hostBox);
		addField(userBox);
		addField(pwBox);
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