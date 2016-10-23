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
import dataMgmt.DataManager;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;
import requestMgmt.IInvalidResponseHandler;
import requestMgmt.RequestManager;
import websocket.IRequestSendErrorHandler;
import websocket.WSManager;
import websocket.models.File;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.requests.FilePullRequest;
import websocket.models.responses.FilePullResponse;

public class EclipseRequestManager extends RequestManager {

	public EclipseRequestManager(DataManager dataManager, WSManager wsManager,
			IRequestSendErrorHandler requestSendErrorHandler, IInvalidResponseHandler invalidResponseHandler) {
		super(dataManager, wsManager, requestSendErrorHandler, invalidResponseHandler);
	}
	
	@Override
	public void finishSubscribeToProject(long id, File[] files) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		Project p = PluginManager.getInstance().getDataManager().getSessionStorage().getProjectById(id);
		IProject eclipseProject = root.getProject(p.getName());
		ProjectMetadata meta =  new ProjectMetadata();
		meta.setName(p.getName());
		meta.setProjectID(id);
		PluginManager.getInstance().getMetadataManager().putProjectMetadata(eclipseProject.getFullPath().toString(), meta);
		try {
			if (eclipseProject.exists()) {
				eclipseProject.delete(true, true, new NullProgressMonitor());
			}
			eclipseProject.create(new NullProgressMonitor());
			eclipseProject.open(new NullProgressMonitor());
			for (File f : files) {
				pullFileAndCreate(eclipseProject, p, f, new NullProgressMonitor());
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	private byte[] fileBytes;
	
	public void pullFileAndCreate(IProject p, Project ccp, File file, IProgressMonitor progressMonitor) {
		Request req = (new FilePullRequest(file.getFileID())).getRequest(response -> {
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
						FileMetadata meta = new FileMetadata();
						meta.setFileID(file.getFileID());
						meta.setFilename(file.getFilename());
						meta.setRelativePath(file.getRelativePath());
						meta.setVersion(file.getFileVersion());
						PluginManager.getInstance().getMetadataManager().putFileMetadata(path, ccp.getProjectID(), meta);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					MessageDialog.createDialog("Failed to pull file" + file.getFilename() + " with status code " + response.getStatus()).open();
				}
		}, new UIRequestErrorHandler("Couldn't send file pull request."));
		
		PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(req);
	}

	@Override
	public void finishCreateProject(Project project) {
		
	}
}
