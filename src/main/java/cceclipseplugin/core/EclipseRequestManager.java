package cceclipseplugin.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		Project p = PluginManager.getInstance().getDataManager().getSessionStorage().getProjectById(id);
		IProject eclipseProject = root.getProject(p.getName());
		ProjectMetadata meta =  new ProjectMetadata();
		meta.setName(p.getName());
		meta.setProjectID(id);
		PluginManager.getInstance().getMetadataManager().putProjectMetadata(eclipseProject.getFullPath().toString(), meta);
		try {
			NullProgressMonitor progressMonitor = new NullProgressMonitor();
			if (eclipseProject.exists()) {
				eclipseProject.delete(true, true, progressMonitor);
			}
			eclipseProject.create(progressMonitor);
			eclipseProject.open(progressMonitor);
			for (File f : files) {
				pullFileAndCreate(eclipseProject, p, f, progressMonitor, false);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	public void pullFileAndCreate(IProject p, Project ccp, File file, IProgressMonitor progressMonitor, boolean unsubscribeOnFailure) {
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
					System.out.println("Making file " + relPath.toString());
					IFile newFile = p.getFile(relPath);
					try {
						if (newFile.exists()) {
							ByteArrayInputStream in = new ByteArrayInputStream(fileBytes);
							newFile.setContents(in, true, false, progressMonitor);
							in.close();
						} else {
							ByteArrayInputStream in = new ByteArrayInputStream(fileBytes);
							newFile.create(in, true, progressMonitor);
							in.close();
						}
						FileMetadata meta = new FileMetadata();
						meta.setFileID(file.getFileID());
						meta.setFilename(file.getFilename());
						meta.setRelativePath(file.getRelativePath());
						meta.setVersion(file.getFileVersion());
						PluginManager.getInstance().getMetadataManager().putFileMetadata(newFile.getFullPath().removeLastSegments(1).toString(), 
								ccp.getProjectID(), meta);
					} catch (Exception e) {
						e.printStackTrace();
						if (unsubscribeOnFailure) {
							showErrorAndUnsubscribe(ccp.getProjectID());
						}
						return;
					}
				} else {
					MessageDialog.createDialog("Failed to pull file" + file.getFilename() + " with status code " + response.getStatus()).open();
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
		PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(req);
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
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		MetadataManager mm = PluginManager.getInstance().getMetadataManager();
		ProjectMetadata pMeta = mm.getProjectMetadata(mm.getProjectIDForFileID(fMeta.getFileID()));
		IPath filePath = new Path(pMeta.getName());
		filePath = filePath.append(fMeta.getFilePath());
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(filePath);
		
		Request req = new FilePullRequest(fMeta.getFileID()).getRequest(response -> {
			if (response.getStatus() == 200) {
				try {
					byte[] oldContents = ((FilePullResponse) response.getData()).getFileBytes();
					InputStream in = file.getContents();
					byte[] newContents = inputStreamToByteArray(in);
					in.close();
					
					List<Diff> diffs = generateStringDiffs(new String(oldContents), new String(newContents));
					
					if (diffs != null && !diffs.isEmpty()) {
						this.sendFileChanges(fMeta.getFileID(), new String[] { new Patch((int) fMeta.getVersion(), diffs).toString() }, 0);
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
		PluginManager.getInstance().getMetadataManager().putProjectMetadata(iproject.getFullPath().toString(), meta);
		List<IFile> files = recursivelyGetFiles(iproject);
		for (IFile f : files) {
			String path = f.getProjectRelativePath().removeLastSegments(1).toString(); // remove filename from path
			try {
				Request req = (new FileCreateRequest(f.getName(), 
						path, 
						project.getProjectID(),
						inputStreamToByteArray(f.getContents()))).getRequest(
								response -> {
									int fileCreateStatusCode = response.getStatus();
									if (fileCreateStatusCode == 200) {
										FileCreateResponse r = ((FileCreateResponse) response.getData());
										FileMetadata fmeta = new FileMetadata();
										fmeta.setFileID(r.getFileID());
										fmeta.setFilename(f.getName());
										fmeta.setRelativePath(path);
										// TODO: make file version be sent with file create request
										fmeta.setVersion(0);
										PluginManager.getInstance().getMetadataManager().putFileMetadata(f.getFullPath().removeLastSegments(1).toString(), project.getProjectID(), fmeta);
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
}
