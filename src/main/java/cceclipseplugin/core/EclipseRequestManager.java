package cceclipseplugin.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
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
import patching.Diff;
import patching.Patch;
import requestMgmt.IInvalidResponseHandler;
import requestMgmt.RequestManager;
import websocket.IRequestSendErrorHandler;
import websocket.WSManager;
import websocket.models.File;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.notifications.ProjectDeleteNotification;
import websocket.models.requests.FileCreateRequest;
import websocket.models.requests.FilePullRequest;
import websocket.models.requests.ProjectCreateRequest;
import websocket.models.responses.FileChangeResponse;
import websocket.models.responses.FileCreateResponse;
import websocket.models.requests.ProjectGetFilesRequest;
import websocket.models.responses.FilePullResponse;
import websocket.models.responses.ProjectGetFilesResponse;

public class EclipseRequestManager extends RequestManager {

	public EclipseRequestManager(DataManager dataManager, WSManager wsManager,
			IRequestSendErrorHandler requestSendErrorHandler, IInvalidResponseHandler invalidResponseHandler) {
		super(dataManager, wsManager, requestSendErrorHandler, invalidResponseHandler);
	}
	
	@Override
	public void finishSubscribeToProject(long id, File[] files) {
		PluginManager pm = PluginManager.getInstance();
		MetadataManager metaMgr = pm.getMetadataManager();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		Project p = pm.getDataManager().getSessionStorage().getProjectById(id);
		IProject eclipseProject = root.getProject(p.getName());
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		
		// create & open a new project, deleting the old one if it exists
		try {
			pm.putProjectInWarnList(p.getName(), ProjectDeleteNotification.class);
			if (eclipseProject.exists()) {
				eclipseProject.delete(false, true, progressMonitor);
			}
			pm.putProjectInWarnList(p.getName(), ProjectCreateRequest.class);
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
			fileMetadatas.add(new FileMetadata(f));
			pullFileAndCreate(eclipseProject, p, f, progressMonitor, false);
		}
		pmeta.setFiles(fileMetadatas);
		metaMgr.putProjectMetadata(eclipseProject.getLocation().toString(), pmeta);
		metaMgr.writeProjectMetadataToFile(pmeta, eclipseProject.getLocation().toString(), CoreStringConstants.CONFIG_FILE_NAME);
	}
	
	public void pullFileAndCreate(IProject p, Project ccp, File file, IProgressMonitor progressMonitor, boolean unsubscribeOnFailure) {
		PluginManager pm = PluginManager.getInstance();
		Request req = (new FilePullRequest(file.getFileID())).getRequest(response -> {
				if (response.getStatus() == 200) {
					byte[] fileBytes = ((FilePullResponse) response.getData()).getFileBytes();
					
					Path relPath = new Path(file.getRelativePath());
					System.out.println("Processing path " + relPath.toString());
					if (!relPath.toString().equals("") && !relPath.toString().equals(".")) {
						
						Path currentFolder = Path.EMPTY;
						for (int i = 0; i < relPath.segmentCount(); i++) {
							// iterate through path segments and create if they don't exist
							currentFolder = (Path) currentFolder.append(relPath.segment(i));
							System.out.println("Making folder " + currentFolder.toString());
							
							IFolder newFolder = p.getFolder(currentFolder);
							try {
								if (!newFolder.exists()) {
									newFolder.create(true, true, progressMonitor);
								}
							} catch (Exception e1) {
								System.out.println("Could not create folder for " + currentFolder.toString());
								e1.printStackTrace();
								if (unsubscribeOnFailure) {
									showErrorAndUnsubscribe(ccp.getProjectID());
								}
								return;
							}
							
						}
						
					}
					
					relPath = (Path) relPath.append(file.getFilename());
					IPath workspaceRelativePath = p.getFullPath().append(relPath);
					System.out.println("Making file " + relPath.toString());
					IFile newFile = p.getFile(relPath);
					try {
						String fileContents = new String(fileBytes);
						List<Patch> patches = new ArrayList<>();
						for (String stringPatch : ((FilePullResponse) response.getData()).getChanges()) {
							patches.add(new Patch(stringPatch));
						}
						fileContents = pm.getDataManager().getPatchManager().applyPatch(fileContents, patches);
						if (newFile.exists()) {
							pm.putFileInWarnList(workspaceRelativePath.toString(), FileChangeResponse.class);
							ByteArrayInputStream in = new ByteArrayInputStream(fileContents.getBytes());
							newFile.setContents(in, false, false, progressMonitor);
							
							in.close();
						} else {
							// warn directory watching before creating the file
							pm.putFileInWarnList(workspaceRelativePath.toString(), FileCreateResponse.class);
							ByteArrayInputStream in = new ByteArrayInputStream(fileContents.getBytes());
							newFile.create(in, false, progressMonitor);
							in.close();
						}
						FileMetadata meta = new FileMetadata(file);
						pm.getMetadataManager().putFileMetadata(workspaceRelativePath.toString(), 
								ccp.getProjectID(), meta);
					} catch (Exception e) {
						e.printStackTrace();
						if (unsubscribeOnFailure) {
							showErrorAndUnsubscribe(ccp.getProjectID());
						}
						return;
					}
				} else {
					Display.getDefault().asyncExec(() -> MessageDialog.createDialog("Failed to pull file" + file.getFilename() + " with status code " + response.getStatus()).open());
					if (unsubscribeOnFailure) {
						showErrorAndUnsubscribe(ccp.getProjectID());
					}
				}
		}, () -> {
			if (unsubscribeOnFailure) {
				showErrorAndUnsubscribe(ccp.getProjectID());
			} else {
				new UIRequestErrorHandler("Couldn't send file pull request.").handleRequestSendError();
			}
		});
		pm.getWSManager().sendAuthenticatedRequest(req);
	}
	
	@Override
	public void finishRenameFile(FileMetadata fMeta) {
		pullDiffSendChanges(fMeta);
	}
	
	@Override
	public void finishMoveFile(FileMetadata fMeta) {
		pullDiffSendChanges(fMeta);
	}
	
	public void pullDiffSendChanges(FileMetadata fMeta) {
		MetadataManager mm = PluginManager.getInstance().getMetadataManager();
		long fileID = fMeta.getFileID();
		long projectID = mm.getProjectIDForFileID(fileID);
		ProjectMetadata pMeta = mm.getProjectMetadata(projectID);
		IPath filePath = new Path(fMeta.getFilePath());
		System.out.println("pulldiffsendchanges for " + filePath);
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(pMeta.getName());
		IFile file = project.getFile(filePath);
		
		Request req = new FilePullRequest(fMeta.getFileID()).getRequest(response -> {
			if (response.getStatus() == 200) {
				try {
					byte[] oldContents = ((FilePullResponse) response.getData()).getFileBytes();
					InputStream in = file.getContents();
					byte[] newContents = inputStreamToByteArray(in);
					String newStringContents = new String(newContents);
					newStringContents = newStringContents.replace("\r\n", "\n");
					in.close();
					// applying patches
					String oldStringContents = new String(oldContents);
					List<Patch> patches = new ArrayList<>();
					for (String stringPatch : ((FilePullResponse) response.getData()).getChanges()) {
						patches.add(new Patch(stringPatch));
					}
					oldStringContents = PluginManager.getInstance().getDataManager().getPatchManager().applyPatch(oldStringContents, patches);
					
					List<Diff> diffs = generateStringDiffs(oldStringContents, newStringContents);
					
					if (diffs != null && !diffs.isEmpty()) {
						this.sendFileChanges(fMeta.getFileID(), new Patch[] { new Patch((int) fMeta.getVersion(), diffs)});
					} else {
						System.out.println("File either failed to pull or no diffs were found.");
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, new UIRequestErrorHandler("Couldn't send file pull request"));
		
		PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(req);
	}
	
	public List<Diff> generateStringDiffs(String oldContents, String newContents) {
		DiffMatchPatch dmp = new DiffMatchPatch();
		List<org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Patch> patches = dmp.patchMake(oldContents, newContents);
		List<Diff> ccDiffs = new ArrayList<>();

		for (org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Patch p : patches) {
			int index = p.start1;
			
			for (org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Diff d : p.diffs) {
				Diff ccDiff = null;
				
				if (d.operation == DiffMatchPatch.Operation.INSERT) {
					ccDiff = new Diff(true, index, d.text);
					index += d.text.length();
				} else if (d.operation == DiffMatchPatch.Operation.DELETE) {
					ccDiff = new Diff(false, index, d.text);
				} else if (d.operation == DiffMatchPatch.Operation.EQUAL) {
					index += d.text.length();
				}
				
				if (ccDiff != null) {
					ccDiff.convertToLF(oldContents);
					ccDiffs.add(ccDiff);
				}
			}
			
		}
		
		return ccDiffs;
	}

	@Override
	public void finishCreateProject(Project project) {
		IProject iproject = ResourcesPlugin.getWorkspace().getRoot().getProject(project.getName());
		ProjectMetadata meta = new ProjectMetadata();
		meta.setName(project.getName());
		meta.setProjectID(project.getProjectID());
		
		CCIgnore ignoreFile = CCIgnore.createForProject(iproject);
		
		List<IFile> ifiles = recursivelyGetFiles(iproject, ignoreFile);
		Semaphore waiter = new Semaphore(0);
		for (IFile f : ifiles) {
			String path = f.getProjectRelativePath().removeLastSegments(1).toString(); // remove filename from path
			try (InputStream in = f.getContents();) {
				Request req = (new FileCreateRequest(f.getName(), 
						path, 
						project.getProjectID(),
						inputStreamToByteArray(in))).getRequest(
								response -> {
									int fileCreateStatusCode = response.getStatus();
									if (fileCreateStatusCode == 200) {
										waiter.release();
									} else {
										Display.getDefault().asyncExec(() -> MessageDialog.createDialog(DialogStrings.AddProjectDialog_FailedWithStatus + fileCreateStatusCode + ".").open()); //$NON-NLS-2$
										return;
									}
								}, new UIRequestErrorHandler(DialogStrings.AddProjectDialog_FileCreateErr));
				PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(req);
			} catch (IOException | CoreException e) {
				Display.getDefault().asyncExec(() -> MessageDialog.createDialog(DialogStrings.AddProjectDialog_ReadFileErr).open());
				e.printStackTrace();
				PluginManager.getInstance().getRequestManager().deleteProject(project.getProjectID());
				return;
			}			
		}
		new Thread(() -> {
			try {
				waiter.tryAcquire(ifiles.size(), 20 * ifiles.size(), TimeUnit.SECONDS);
				lookupProjectFilesAndPutMetadata(iproject.getLocation().toString(), meta);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}
	
	private void lookupProjectFilesAndPutMetadata(String path, ProjectMetadata meta) {
		Request requestForFiles = (new ProjectGetFilesRequest(meta.getProjectID())).getRequest(response -> {
            int status = response.getStatus();
            if (status == 200) {
                ProjectGetFilesResponse r = (ProjectGetFilesResponse) response.getData();
                if (r.files != null) {
                    List<FileMetadata> fmetas = new ArrayList<>();
                    for (int i = 0; i < r.files.length; i++) {
                    	fmetas.add(new FileMetadata(r.files[i]));
                    }
                    meta.setFiles(fmetas);
                }
                MetadataManager mm = PluginManager.getInstance().getMetadataManager();
                mm.putProjectMetadata(path, meta);
                mm.writeProjectMetadataToFile(meta, path, CoreStringConstants.CONFIG_FILE_NAME);
            } else {
                Display.getDefault().asyncExec(() -> MessageDialog.createDialog("Failed to retrieve project file objects after project creation"));
                return;
            }
        }, new UIRequestErrorHandler("Failure sending request for project file objects after project creation"));
        PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(requestForFiles);
	}
	
	private List<IFile> recursivelyGetFiles(IContainer f, CCIgnore ignoreFile) {
		ArrayList<IFile> files = new ArrayList<>();
		IResource[] members = null;
		
		try {
			members = f.members();
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
		
		for(IResource m : members) {
			
			if (m instanceof IFile) {
				String path = ((IFile) m).getProjectRelativePath().toString();
				if (ignoreFile.containsEntry(path)) {
					System.out.println(String.format("File %s was ignored when scanning for files.", path));
				} else {
					files.add((IFile) m);
				}
			} else if (m instanceof IFolder) {
				String path = ((IFolder) m).getProjectRelativePath().toString();
				if (ignoreFile.containsEntry(path)) {
					System.out.println(String.format("Folder %s was ignored when scanning for files.", path));
				} else {
					files.addAll(recursivelyGetFiles((IFolder) m, ignoreFile));
				}
			}
		}
		return files;
	}
	
	private void showErrorAndUnsubscribe(long projectId) {
		Display.getDefault().asyncExec(() -> {
			PluginManager pm = PluginManager.getInstance();
			String projName = pm.getDataManager().getSessionStorage().getProjectById(projectId).getName();
			unsubscribeFromProject(projectId);
			MessageDialog.createDialog("An error occured. Please re-subscribe to the project " + projName).open();
		});
	}
	
	public static byte[] inputStreamToByteArray(InputStream is) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		byte curr;
		while(true) {
			curr = (byte) is.read();
			if (curr == -1)
				break;
			out.write(curr);
		}
		byte[] result = out.toByteArray();
		out.close();
		return result;
	}

	@Override
	public void finishDeleteProject(Project project) {
		IProject iproject = ResourcesPlugin.getWorkspace().getRoot().getProject(project.getName());
		IFile metaFile = iproject.getFile(CoreStringConstants.CONFIG_FILE_NAME);
		if (metaFile.exists()) {
			try {
				metaFile.delete(true, true, new NullProgressMonitor());
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}
}
