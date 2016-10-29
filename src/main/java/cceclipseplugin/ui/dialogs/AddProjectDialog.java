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

//		Request req = (new ProjectCreateRequest(selectedProject.getName())).getRequest(
//				response -> {
//					
//					int status = response.getStatus();
//					if (status == 200) {
//						long id = ((ProjectCreateResponse) response.getData()).getProjectID();
//						try {
//							sendCreateFileRequests(selectedProject.getName(), id, recursivelyGetFiles(selectedProject));
//						} catch (Exception e) {
//							Display.getDefault().asyncExec(() -> MessageDialog.createDialog(DialogStrings.AddProjectDialog_ReadFileErr).open());
//							e.printStackTrace();
//							sendProjectDeleteRequest(id);
//							return;
//						}
//						
//						long projID = ((ProjectCreateResponse) response.getData()).getProjectID();
//						lookupProjectAndStore(projID);
//						
//					} else {
//						Display.getDefault().asyncExec(() -> MessageDialog.createDialog(DialogStrings.AddProjectDialog_FailedWithStatus + status + ".").open()); //$NON-NLS-2$
//					}
//					
//				}, 
//				new UIRequestErrorHandler(DialogStrings.AddProjectDialog_ProjCreateErr));
//		
//		PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(req);
		
		super.okPressed();
	}
	
//	private void lookupProjectAndStore(long projectID) {
//		Request lookupReq = (new ProjectLookupRequest(new Long[] { projectID })).getRequest(
//				lookupResponse -> {
//					int lookupStatus = lookupResponse.getStatus();
//					
//					if (lookupStatus == 200) {
//						SessionStorage storage = PluginManager.getInstance().getDataManager().getSessionStorage();
//						storage.setProject(((ProjectLookupResponse) lookupResponse.getData()).getProjects()[0]);
//					} else {
//						Display.getDefault().asyncExec(() -> MessageDialog.createDialog("Project lookup failed with status code " + lookupStatus + ".").open());
//					}
//
//				}
//				, new UIRequestErrorHandler("Failed to send project lookup request."));
//		
//		PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(lookupReq);
//	}
	
//	private void sendProjectDeleteRequest(long projectID) {
//		Request req = (new ProjectDeleteRequest(projectID)).getRequest(
//				new UIResponseHandler("Project delete"), 
//				new UIRequestErrorHandler("Failed to send project delete request."));
//		PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(req);
//	}
	
//	private void sendCreateFileRequests(String name, long projectID, List<IFile> files) throws CoreException, IOException {
//		
//		for (IFile f : files) {
//			String path = f.getProjectRelativePath().toString();
//			path = path.replace('\\', '/');
//			// remove filename from path
//			if (path.contains("/")) {
//				path = path.substring(0, path.lastIndexOf('/')); 
//			} else {
//				path = "";
//			}
//			
//			if (path.isEmpty()) {
//				path = name;
//			} else {
//				path = name + "/" + path;
//			}
//			Request req = (new FileCreateRequest(f.getName(), 
//					path, 
//					projectID,
//					inputStreamToByteArray(f.getContents()))).getRequest(
//							response -> {
//								int fileCreateStatusCode = response.getStatus();
//								
//								if (fileCreateStatusCode != 200) {
//									Display.getDefault().asyncExec(() -> MessageDialog.createDialog(DialogStrings.AddProjectDialog_FailedWithStatus + fileCreateStatusCode + ".").open()); //$NON-NLS-2$
//									return;
//								}
//							}, new UIRequestErrorHandler(DialogStrings.AddProjectDialog_FileCreateErr));
//			
//			PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(req);
//		}
//	}
	
//	private byte[] inputStreamToByteArray(InputStream is) throws IOException {
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		
//		byte curr;
//		while(true) {
//			curr = (byte) is.read();
//			if (curr == -1)
//				break;
//			out.write(curr);
//		}
//		
//		return out.toByteArray();
//	}

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
