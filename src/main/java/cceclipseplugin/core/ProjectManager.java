package cceclipseplugin.core;

import java.io.ByteArrayInputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import cceclipseplugin.ui.UIRequestErrorHandler;
import cceclipseplugin.ui.dialogs.MessageDialog;
import websocket.models.File;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.requests.FilePullRequest;
import websocket.models.responses.FilePullResponse;

public class ProjectManager {
	public void createEclipseProject(Project p, File[] files) throws CoreException {
		IProgressMonitor progressMonitor = new NullProgressMonitor();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(p.getName());
		project.create(progressMonitor);
		project.open(progressMonitor);
		
		for (int i = 0; i < files.length; i++) {
			pullFileAndCreate(project, files[i], progressMonitor);
		}
	}
	
	public void deleteEclipseProject(Project p) {
		IProgressMonitor progressMonitor = new NullProgressMonitor();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(p.getName());
		try {
			project.delete(true, true, progressMonitor);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	private byte[] fileBytes;
	
	public void pullFileAndCreate(IProject p, File file, IProgressMonitor progressMonitor) {
		Request req = (new FilePullRequest(file.getFileID())).getRequest(response -> {
				System.out.println("Got some stuff");
				if (response.getStatus() == 200) {
					fileBytes = ((FilePullResponse) response.getData()).getFileBytes();
					
					String path = file.getRelativePath();
					if (path.contains("/")) {
						path = path.substring(path.indexOf("/") + 1, path.length()); // get rid of project name from dir
					} else {
						path = "";
					}
					Path relPath = new Path(path);
					if (!path.equals("")) {
						String currentFolder = "";
						for (int i = 0; i < relPath.segmentCount(); i++) {
							// iterate through path segments and create if they don't exist
							currentFolder += "/" + relPath.segment(i);
							Path currentPath = new Path(currentFolder);
							System.out.println("Making folder " + currentPath.toString());
							IFolder newFolder = p.getFolder(currentPath);
							try {
								if (!newFolder.exists()) {
									newFolder.create(true, true, progressMonitor);
								}
							} catch (Exception e1) {
								System.out.println("Could not create folder for " + currentPath.toString());
								e1.printStackTrace();
							}
						}
					}
					path += "/" + file.getFilename();
					System.out.println("Making file " + path);
					IFile newFile = p.getFile(new Path(path));
					try {
						if (newFile.exists()) {
							newFile.setContents(new ByteArrayInputStream(fileBytes), true, false, progressMonitor);
						} else {
							newFile.create(new ByteArrayInputStream(fileBytes), true, progressMonitor);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					MessageDialog.createDialog("Failed to pull file" + file.getFilename() + " with status code " + response.getStatus()).open();
				}
		}, new UIRequestErrorHandler("Couldn't send file pull request."));
		
		PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(req);
	}
}
