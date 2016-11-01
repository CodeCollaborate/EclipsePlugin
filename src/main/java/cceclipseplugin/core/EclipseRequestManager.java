package cceclipseplugin.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Display;

import cceclipseplugin.ui.UIRequestErrorHandler;
import cceclipseplugin.ui.dialogs.DialogStrings;
import cceclipseplugin.ui.dialogs.MessageDialog;
import constants.CoreStringConstants;
import dataMgmt.DataManager;
import dataMgmt.MetadataManager;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;
import patching.Patch;
import requestMgmt.IInvalidResponseHandler;
import requestMgmt.RequestManager;
import websocket.IRequestSendErrorHandler;
import websocket.WSManager;
import websocket.models.File;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.requests.FileCreateRequest;
import websocket.models.requests.FilePullRequest;
import websocket.models.responses.FileCreateResponse;
import websocket.models.responses.FilePullResponse;

public class EclipseRequestManager extends RequestManager {

	public EclipseRequestManager(DataManager dataManager, WSManager wsManager,
			IRequestSendErrorHandler requestSendErrorHandler, IInvalidResponseHandler invalidResponseHandler) {
		super(dataManager, wsManager, requestSendErrorHandler, invalidResponseHandler);
	}
	
	@Override
	public void finishSubscribeToProject(long id, File[] files) {
		MetadataManager metaMgr = PluginManager.getInstance().getMetadataManager();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		Project p = PluginManager.getInstance().getDataManager().getSessionStorage().getProjectById(id);
		IProject eclipseProject = root.getProject(p.getName());
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		
		// create & open a new project, deleting the old one if it exists
		try {
			if (eclipseProject.exists()) {
				eclipseProject.delete(true, true, progressMonitor);
			}
			eclipseProject.create(progressMonitor);
			eclipseProject.open(progressMonitor);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		// create project metadata and write the metadata to disk
		ProjectMetadata pmeta = new ProjectMetadata();
		pmeta.setName(p.getName());
		pmeta.setProjectID(id);
		List<FileMetadata> fileMetadatas = new ArrayList<>();
		for (File f : files) {
			pullFileAndCreate(eclipseProject, p, f, progressMonitor);
			fileMetadatas.add(new FileMetadata(f));
		}
		pmeta.setFiles(fileMetadatas.toArray(new FileMetadata[fileMetadatas.size()]));
		metaMgr.putProjectMetadata(eclipseProject.getLocation().toString(), pmeta);
		metaMgr.writeProjectMetadataToFile(pmeta, eclipseProject.getLocation().toString(), CoreStringConstants.CONFIG_FILE_NAME);
	}

	public void pullFileAndCreate(IProject p, Project ccp, File file, IProgressMonitor progressMonitor) {
		Request req = (new FilePullRequest(file.getFileID())).getRequest(response -> {
				if (response.getStatus() == 200) {
					byte[] fileBytes = ((FilePullResponse) response.getData()).getFileBytes();
					
					String path = file.getRelativePath();
					Path relPath = new Path(path);
					if (!path.equals("") && !path.equals(".")) {
						String currentFolder = "";
						for (int i = 0; i < relPath.segmentCount(); i++) {
							// iterate through path segments and create if they don't exist
							currentFolder = Paths.get(currentFolder, relPath.segment(i)).toString();
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
						String fileContents = new String(fileBytes);
						List<Patch> patches = new ArrayList<>();
						for (String stringPatch : ((FilePullResponse) response.getData()).getChanges()) {
							patches.add(new Patch(stringPatch));
						}
						fileContents = PluginManager.getInstance().getDataManager().getPatchManager().applyPatch(fileContents, patches);
						if (newFile.exists()) {
							newFile.setContents(new ByteArrayInputStream(fileContents.getBytes()), true, false, progressMonitor);
						} else {
							newFile.create(new ByteArrayInputStream(fileContents.getBytes()), true, progressMonitor);
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

	@Override
	public void finishCreateProject(Project project) {
//		IProject iproject = ResourcesPlugin.getWorkspace().getRoot().getProject(project.getName());
//		ProjectMetadata meta = new ProjectMetadata();
//		meta.setName(project.getName());
//		meta.setProjectID(project.getProjectID());
////		PluginManager.getInstance().getMetadataManager().putProjectMetadata(iproject.getFullPath().toString(), meta);
//		List<IFile> files = recursivelyGetFiles(iproject);
//		for (IFile f : files) {
//			String path = f.getProjectRelativePath().removeLastSegments(1).toString(); // remove filename from path
//			try {
//				Request req = (new FileCreateRequest(f.getName(), 
//						path, 
//						project.getProjectID(),
//						inputStreamToByteArray(f.getContents()))).getRequest(
//								response -> {
//									int fileCreateStatusCode = response.getStatus();
//									if (fileCreateStatusCode == 200) {
//										FileCreateResponse r = ((FileCreateResponse) response.getData());
//										FileMetadata fmeta = new FileMetadata();
//										fmeta.setFileID(r.getFileID());
//										fmeta.setFilename(f.getName());
//										fmeta.setRelativePath(path);
//										// TODO: make file version be sent with file create request
//										fmeta.setVersion(0);
//										PluginManager.getInstance().getMetadataManager().putFileMetadata(f.getFullPath().removeLastSegments(1).toString(), project.getProjectID(), fmeta);
//									} else {
//										Display.getDefault().asyncExec(() -> MessageDialog.createDialog(DialogStrings.AddProjectDialog_FailedWithStatus + fileCreateStatusCode + ".").open()); //$NON-NLS-2$
//										return;
//									}
//								}, new UIRequestErrorHandler(DialogStrings.AddProjectDialog_FileCreateErr));
//				PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(req);
//			} catch (IOException | CoreException e) {
//				Display.getDefault().asyncExec(() -> MessageDialog.createDialog(DialogStrings.AddProjectDialog_ReadFileErr).open());
//				e.printStackTrace();
//				PluginManager.getInstance().getRequestManager().deleteProject(project.getProjectID());
//				return;
//			}			
//		}
	}
	
	private List<IFile> recursivelyGetFiles(IContainer f) {
		ArrayList<IFile> files = new ArrayList<>();
		ArrayList<IFolder> folders = new ArrayList<>();
		IResource[] members = null;
		try {
			members = f.members();
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
		for(IResource m : members) {
			if (m instanceof IFile) {
				files.add((IFile) m);
			} else if (m instanceof IFolder) {
				folders.add((IFolder) m);
			}
		}
		
		for (IFolder folder : folders) {
			files.addAll(recursivelyGetFiles(folder));
		}
		return files;
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
}
