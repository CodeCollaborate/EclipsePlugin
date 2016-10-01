package cceclipseplugin.ui.dialogs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import cceclipseplugin.core.PluginManager;
import cceclipseplugin.ui.UIRequestErrorHandler;
import websocket.models.Request;
import websocket.models.requests.FileCreateRequest;
import websocket.models.requests.ProjectCreateRequest;
import websocket.models.responses.ProjectCreateResponse;

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
		gd_lblProjectsArePulled.heightHint = 33;
		gd_lblProjectsArePulled.widthHint = 380;
		lblProjectsArePulled.setLayoutData(gd_lblProjectsArePulled);
		lblProjectsArePulled.setText(DialogStrings.AddProjectDialog_Label1);
		
		combo = new Combo(container, SWT.NONE);
		GridData gd_combo = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_combo.widthHint = 409;
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
			MessageDialog err = new MessageDialog(new Shell(), DialogStrings.AddProjectDialog_NoProjectsErr);
			err.open();
			return;
		}
		
		IProject selectedProject = localProjects[combo.getSelectionIndex()];
		
		Semaphore waiter = new Semaphore(0);
		
		Request req = (new ProjectCreateRequest(selectedProject.getName())).getRequest(
				response -> {
					
					int status = response.getStatus();
					if (status != 200) {
						MessageDialog err = new MessageDialog(new Shell(), DialogStrings.AddProjectDialog_FailedWithStatus + status + "."); //$NON-NLS-2$
						err.open();
						waiter.release();
						return;
					}
					
					IProject p = localProjects[combo.getSelectionIndex()];
					IFolder baseFolder = p.getFolder(p.getProjectRelativePath());
					
					long id = ((ProjectCreateResponse) response.getData()).getProjectID();
					try {
						sendCreateFileRequests(id, recursivelyGetFiles(baseFolder));
					} catch (Exception e) {
						MessageDialog err = new MessageDialog(new Shell(), DialogStrings.AddProjectDialog_ReadFileErr);
						err.open();
						e.printStackTrace();
					}
					
					waiter.release();
				}, 
				new UIRequestErrorHandler(new Shell(), DialogStrings.AddProjectDialog_ProjCreateErr));
		
		PluginManager.getInstance().getWSManager().sendRequest(req);
		
		try {
			if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
	            MessageDialog errDialog = new MessageDialog(new Shell(), DialogStrings.AddProjectDialog_TimeoutErr);
	            errDialog.open();
	            return;
			}
		} catch (InterruptedException e1) {
			String message = e1.getMessage();
			MessageDialog errDialog = new MessageDialog(new Shell(), message);
			errDialog.open();
			return;
		}
		
		super.okPressed();
	}
	
	private int fileCreateStatusCode;
	
	private void sendCreateFileRequests(long projectID, List<IFile> files) throws CoreException, IOException {
		Semaphore waiter = new Semaphore(0);
		
		for (IFile f : files) {
			Request req = (new FileCreateRequest(f.getName(), 
					f.getProjectRelativePath().toString(), 
					projectID,
					inputStreamToByteArray(f.getContents()))).getRequest(
							response -> {
								fileCreateStatusCode = response.getStatus();
								
								if (fileCreateStatusCode != 200) {
									MessageDialog err = new MessageDialog(new Shell(), DialogStrings.AddProjectDialog_FailedWithStatus + fileCreateStatusCode + "."); //$NON-NLS-2$
									err.open();
									waiter.release();
									return;
								}
							}, new UIRequestErrorHandler(new Shell(), DialogStrings.AddProjectDialog_FileCreateErr));
			
			PluginManager.getInstance().getWSManager().sendRequest(req);
			
			try {
				if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
		            MessageDialog errDialog = new MessageDialog(new Shell(), DialogStrings.AddProjectDialog_TimeoutErr);
		            errDialog.open();
		            return;
				}
			} catch (InterruptedException e1) {
				String message = e1.getMessage();
				MessageDialog errDialog = new MessageDialog(new Shell(), message);
				errDialog.open();
				return;
			}
		}
	}
	
	private byte[] inputStreamToByteArray(InputStream is) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		byte curr;
		while(true) {
			curr = (byte) is.read();
			if (curr == -1)
				break;
			out.write(curr);
		}
		
		return out.toByteArray();
	}
	
	private List<IFile> recursivelyGetFiles(IFolder f) {
		ArrayList<IFile> files = new ArrayList<>();
		ArrayList<IFolder> folders = new ArrayList<>();
		try {
			files = (ArrayList<IFile>) Arrays.asList((IFile[]) f.members(IResource.FILE));
		} catch (CoreException e) {
			e.printStackTrace();
		}
		try {
		folders = (ArrayList<IFolder>) Arrays.asList((IFolder[]) f.members(IResource.FOLDER));
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		if (!folders.isEmpty()) {
			for (IFolder folder : folders) {
				files.addAll(recursivelyGetFiles(folder));
			}
		}
		
		return files;
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

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(417, 153);
	}
	
	@Override
	protected void configureShell(Shell shell) {
	      super.configureShell(shell);
	      shell.setText(DialogStrings.AddProjectDialog_WindowTitle);
	}

}
