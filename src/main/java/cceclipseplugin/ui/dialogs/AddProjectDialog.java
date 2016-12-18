package cceclipseplugin.ui.dialogs;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import cceclipseplugin.core.PluginManager;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Button;

public class AddProjectDialog extends Dialog {

	private IProject[] localProjects;
	private Combo combo;
	
	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public AddProjectDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, false));
		
		Label lblProjectsArePulled = new Label(container, SWT.WRAP | SWT.CENTER);
		GridData gd_lblProjectsArePulled = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
		lblProjectsArePulled.setLayoutData(gd_lblProjectsArePulled);
		lblProjectsArePulled.setText(DialogStrings.AddProjectDialog_Label1);
		
		combo = new Combo(container, SWT.NONE);
		GridData gd_combo = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		combo.setLayoutData(gd_combo);
		
		localProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		
		for (IProject p : localProjects) {
			combo.add(p.getName());
		}

		return container;
	}
	
	@Override 
	protected void okPressed() {
		if (combo.getItemCount() == 0) {
			MessageDialog.createDialog(DialogStrings.AddProjectDialog_NoProjectsErr).open();
			return;
		}
		if (combo.getSelectionIndex() == -1) {
			return;
		}
		IProject selectedProject = localProjects[combo.getSelectionIndex()];
		
		PluginManager.getInstance().getRequestManager().createProject(selectedProject.getName());
		
		super.okPressed();
	}

	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button button = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		button.setText(DialogStrings.AddProjectDialog_AddButton);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected void configureShell(Shell shell) {
	      super.configureShell(shell);
	      shell.setText(DialogStrings.AddProjectDialog_WindowTitle);
	}

}
