package cceclipseplugin.ui;

import java.net.ConnectException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.wb.swt.SWTResourceManager;

import cceclipseplugin.core.PluginManager;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.requests.ProjectDeleteRequest;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class DeleteProjectDialog extends Dialog {

	private Project project;
	
	/**
	 * Create the dialog.
	 * @param parentShell
	 * @wbp.parser.constructor 
	 */
	public DeleteProjectDialog(Shell parentShell) {
		super(parentShell);
	}
	
	public DeleteProjectDialog(Shell parentShell, Project p) {
		super(parentShell);
		this.project = p;
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		
		Label lblAreYouSure = new Label(container, SWT.CENTER);
		GridData gd_lblAreYouSure = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
		gd_lblAreYouSure.widthHint = 304;
		lblAreYouSure.setLayoutData(gd_lblAreYouSure);
		lblAreYouSure.setText("Are you sure you want to delete " + project.getName() + "?");
		
		Label lblWarningThisAction = new Label(container, SWT.WRAP | SWT.CENTER);
		lblWarningThisAction.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
		GridData gd_lblWarningThisAction = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
		gd_lblWarningThisAction.widthHint = 360;
		gd_lblWarningThisAction.heightHint = 36;
		lblWarningThisAction.setLayoutData(gd_lblWarningThisAction);
		lblWarningThisAction.setText("WARNING: This action is irreversible, and will result in the deletion of the project on all contributors' computers.");

		return container;
	}

	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button button = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Semaphore waiter = new Semaphore(0);
				Request req = (new ProjectDeleteRequest(project.getProjectID())).getRequest(
						response -> {
										MessageDialog errDialog;
										int status = response.getStatus();
										if (status == 200)
											errDialog = new MessageDialog(getShell(), "Project successfully deleted.");
										else
											errDialog = new MessageDialog(getShell(), "Failed with status code " + status + ".");
										
										waiter.release();
										
										errDialog.open();
						},
						new UIRequestErrorHandler(getShell(), "Failed to send project delete request."));
				try {
					PluginManager.getInstance().getWSManager().sendRequest(req);
					if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
			            MessageDialog errDialog = new MessageDialog(getShell(), "Request timed out.");
			            errDialog.open();
					}
				} catch (InterruptedException e1) {
					String message = e1.getMessage();
					MessageDialog errDialog = new MessageDialog(getShell(), message);
					errDialog.open();
				}
				
			}
		});
		button.setText("Confirm");
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(393, 159);
	}
	
	@Override
	protected void configureShell(Shell shell) {
	      super.configureShell(shell);
	      shell.setText("CodeCollaborate - Delete Project");
	}

}
