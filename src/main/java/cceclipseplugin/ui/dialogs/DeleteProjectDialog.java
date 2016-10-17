package cceclipseplugin.ui.dialogs;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.wb.swt.SWTResourceManager;

import cceclipseplugin.core.PluginManager;
import cceclipseplugin.ui.RequestConfigurations;
import cceclipseplugin.ui.UIRequestErrorHandler;
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
		lblAreYouSure.setLayoutData(gd_lblAreYouSure);
		lblAreYouSure.setText(DialogStrings.DeleteProjectDialog_AreYouSure + project.getName() + "?"); //$NON-NLS-2$
		
		Label lblWarningThisAction = new Label(container, SWT.WRAP | SWT.CENTER);
		lblWarningThisAction.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
		GridData gd_lblWarningThisAction = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
		lblWarningThisAction.setLayoutData(gd_lblWarningThisAction);
		lblWarningThisAction.setText(DialogStrings.DeleteProjectDialog_DeleteWarning);

		return container;
	}

	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button button = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		Shell shell = getShell();
		Display display = shell.getDisplay();
		// TODO: Move this listener to okPressed()
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Semaphore waiter = new Semaphore(0);
				Request req = (new ProjectDeleteRequest(project.getProjectID())).getRequest(
						response -> {
										MessageDialog errDialog;
										int status = response.getStatus();
										if (status == 200)
											errDialog = new MessageDialog(shell, DialogStrings.DeleteProjectDialog_SuccessMsg);
										else
											errDialog = new MessageDialog(shell, DialogStrings.DeleteProjectDialog_FailedWithStatus + status + "."); //$NON-NLS-2$
										
										waiter.release();
										
										display.asyncExec(() -> errDialog.open());
						},
						new UIRequestErrorHandler(shell, DialogStrings.DeleteProjectDialog_ProjDeleteErr));
				try {
					PluginManager.getInstance().getWSManager().sendRequest(req);
					if (!waiter.tryAcquire(1, RequestConfigurations.REQUST_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
			            MessageDialog errDialog = new MessageDialog(getShell(), DialogStrings.DeleteProjectDialog_TimeoutErr);
						display.asyncExec(() -> errDialog.open());
					}
				} catch (InterruptedException e1) {
					String message = e1.getMessage();
					MessageDialog errDialog = new MessageDialog(getShell(), message);
					display.asyncExec(() -> errDialog.open());
				}
				
			}
		});
		button.setText(DialogStrings.DeleteProjectDialog_ConfirmButton);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void configureShell(Shell shell) {
	      super.configureShell(shell);
	      shell.setText(DialogStrings.DeleteProjectDialog_Title);
	}

}
