package cceclipseplugin.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

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

	private final Logger logger = LogManager.getLogger("eclipseRequestManager");
	
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
			if (eclipseProject.exists()) {
				// using false for the "deleteContent" flag so that this doesn't aggressively delete files out
				// from underneath the user upon subscribing (and file deletes were being propagated to the server)
				pm.putProjectInWarnList(p.getName(), ProjectDeleteNotification.class);
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
			if (f.getFileVersion() == 0) {
				System.err.println(String.format("File %s was pulled with version 0.", f.getFilename()));
			}
			fileMetadatas.add(new FileMetadata(f));
			pullFileAndCreate(eclipseProject, p, f, progressMonitor, true);
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
					logger.debug(String.format("Processing path %s", relPath.toString()));
					if (!relPath.toString().equals("") && !relPath.toString().equals(".")) {
						
						Path currentFolder = Path.EMPTY;
						for (int i = 0; i < relPath.segmentCount(); i++) {
							// iterate through path segments and create if they don't exist
							currentFolder = (Path) currentFolder.append(relPath.segment(i));
							logger.debug(String.format("Making folder %s", currentFolder.toString()));
							
							IFolder newFolder = p.getFolder(currentFolder);
							try {
								if (!newFolder.exists()) {
									newFolder.create(true, true, progressMonitor);
								}
							} catch (Exception e1) {
								logger.error(String.format("Could not create folder for %s", currentFolder.toString()), e1);
								if (unsubscribeOnFailure) {
									showErrorAndUnsubscribe(ccp.getProjectID());
								}
								return;
							}
							
						}
						
					}
					
					relPath = (Path) relPath.append(file.getFilename());
					IPath workspaceRelativePath = p.getFullPath().append(relPath);
					logger.debug(String.format("Making file %s", relPath.toString()));
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
						
						if (file.getFileVersion() == 0) {
							System.err.println(String.format("File %s was pulled with version 0.", file.getFilename()));
						}
						// was already happening in both places where this method was being called
//						FileMetadata meta = new FileMetadata(file);
//						pm.getMetadataManager().putFileMetadata(workspaceRelativePath.toString(), 
//								ccp.getProjectID(), meta);
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
		logger.debug(String.format("pulldiffsendchanges for %s", filePath));
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(pMeta.getName());
		IFile file = project.getFile(filePath);
		
		Request req = new FilePullRequest(fMeta.getFileID()).getRequest(response -> {
			if (response.getStatus() == 200) {
				try {
					byte[] serverContents = ((FilePullResponse) response.getData()).getFileBytes();
					InputStream in = file.getContents();
					byte[] localContents = inputStreamToByteArray(in);
					String localStringContents = new String(localContents);
					localStringContents = localStringContents.replace("\r\n", "\n");
					in.close();
					// applying patches
					String serverStringContents = new String(serverContents);
					List<Patch> patches = new ArrayList<>();
					for (String stringPatch : ((FilePullResponse) response.getData()).getChanges()) {
						patches.add(new Patch(stringPatch));
					}
					serverStringContents = PluginManager.getInstance().getDataManager().getPatchManager().applyPatch(serverStringContents, patches);
					
					List<Diff> diffs = generateStringDiffs(serverStringContents, localStringContents);
					
					if (diffs != null && !diffs.isEmpty()) {
						this.sendFileChanges(fMeta.getFileID(), new Patch[] { new Patch((int) fMeta.getVersion(), diffs)});
					} else {
						logger.debug("File either failed to pull or no diffs were found.");
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
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject iproject = workspace.getRoot().getProject(project.getName());
		ProjectMetadata meta = new ProjectMetadata();
		meta.setName(project.getName());
		meta.setProjectID(project.getProjectID());
		
		Display.getDefault().syncExec(() -> PlatformUI.getWorkbench().saveAllEditors(false));	
		CCIgnore ignoreFile = CCIgnore.createForProject(iproject);
		
		List<IFile> ifiles = recursivelyGetFiles(iproject, ignoreFile);
		Semaphore waiter = new Semaphore(0);
		for (IFile f : ifiles) {
			String path = f.getProjectRelativePath().removeLastSegments(1).toString(); // remove filename from path
			try (InputStream in = f.getContents();) {
				String contents = new String(inputStreamToByteArray(in));
				if (contents.contains("\r\n")) {
					contents = contents.replace("\r\n", "\n");
				}
				Request req = (new FileCreateRequest(f.getName(), 
						path, 
						project.getProjectID(),
						contents.getBytes())).getRequest(
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
                    	if (r.files[i].getFileVersion() == 0) {
                    		System.err.println(String.format("Version from file lookup for %s was 0.", r.files[i].getFilename()));
                    	}
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
					logger.debug(String.format("File %s was ignored when scanning for files.", path));
				} else {
					files.add((IFile) m);
				}
			} else if (m instanceof IFolder) {
				String path = ((IFolder) m).getProjectRelativePath().toString();
				if (ignoreFile.containsEntry(path)) {
					logger.debug(String.format("Folder %s was ignored when scanning for files.", path));
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
