package cceclipseplugin.editor.listeners;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;

import cceclipseplugin.core.EclipseRequestManager;
import cceclipseplugin.core.PluginManager;
import dataMgmt.MetadataManager;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;
import requestMgmt.RequestManager;
import websocket.models.notifications.FileCreateNotification;
import websocket.models.notifications.FileDeleteNotification;
import websocket.models.notifications.FileMoveNotification;
import websocket.models.notifications.FileRenameNotification;
import websocket.models.notifications.ProjectDeleteNotification;
import websocket.models.notifications.ProjectRenameNotification;
import websocket.models.requests.FileChangeRequest;
import websocket.models.responses.FileCreateResponse;
import websocket.models.responses.ProjectCreateResponse;

public class DirectoryListener extends AbstractDirectoryListener {
	
	/**
	 * Handles a resource delta in the case that the resource is and IProject.
	 * 
	 * @param delta
	 * @return 
	 * 		true if the recursion down the delta's children stops after this handler
	 */
	@Override
	protected boolean handleProject(IResourceDelta delta) {
		IProject p = (IProject) delta.getResource();
		ProjectMetadata projectMeta = PluginManager.getInstance().getMetadataManager().getProjectMetadata(p.getLocation().toString());
		PluginManager pm = PluginManager.getInstance();
		RequestManager rm = pm.getRequestManager();
		
		if (delta.getKind() == IResourceDelta.REMOVED) {			
			// Project was renamed
			if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
				if (pm.isProjectInWarnList(p.getName(), ProjectRenameNotification.class)) {
					pm.removeProjectFromWarnList(p.getName(), ProjectRenameNotification.class);
				} else {
					String newName = delta.getMovedToPath().lastSegment();
					String newPath = delta.getMovedToPath().toString();
					
					rm.renameProject(projectMeta.getProjectID(), newName, newPath);
					System.out.println("sent project rename request: renamed to \"" + newName + "\"; path changed to : " + newPath);
					return false;
				}
			} else {
				if (pm.isProjectInWarnList(p.getName(), ProjectDeleteNotification.class)) {
					pm.removeProjectFromWarnList(p.getName(), ProjectDeleteNotification.class);
					return true;
				} else {
					System.out.println("deleting project");
					// Project was deleted from disk
					System.out.println("unsubscribed from project due to removal from disk");
					PluginManager.getInstance().getRequestManager().unsubscribeFromProject(projectMeta.getProjectID());
					return true;
				}
			}
		} else if (delta.getKind() == IResourceDelta.ADDED) {
			if (pm.isProjectInWarnList(p.getName(), ProjectCreateResponse.class)) {
				pm.removeProjectFromWarnList(p.getName(), ProjectCreateResponse.class);
			}
			
			return true;
		}
		
		return false;
	}
	
	@Override
	protected void handleFile(IResourceDelta delta) {
		IFile f = (IFile) delta.getResource();
		PluginManager pm = PluginManager.getInstance();
		MetadataManager mm = pm.getMetadataManager();
		FileMetadata fileMeta = mm.getFileMetadata(f.getFullPath().toString());
		String workspaceRelativePath = f.getFullPath().toString();
		
		System.out.println( "	Filename: " + f.getName() + "	File flag: " + delta.getFlags());
		
		if (delta.getKind() == IResourceDelta.CHANGED) {
			if (fileMeta == null) {
				System.out.println("No metadata found for file change event, deleting file locally");
				try {
					f.delete(true, new NullProgressMonitor());
				} catch (CoreException e) {
					System.err.println("Error deleting untracked file from project.");
					e.printStackTrace();
				}
				return;
			}
			
			if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
				
				IPath relativeMovedToPath = delta.getMovedToPath().removeFirstSegments(1);
				if (!relativeMovedToPath.toString().equals(f.getProjectRelativePath().toString())) {
					
					RequestManager rm = pm.getRequestManager();
					
					if (f.getName().equals(relativeMovedToPath.lastSegment())) {
						// send File.Move request
						if (pm.isFileInWarnList(workspaceRelativePath, FileMoveNotification.class)) {
							pm.removeFileFromWarnList(workspaceRelativePath, FileMoveNotification.class);
						} else {
							// get metadata again but with the old path because old one should be null 
							// if the new path was used to find it
							String movedToPathString = delta.getMovedToPath().toString();
							fileMeta = mm.getFileMetadata(movedToPathString);
							rm.moveFile(fileMeta.getFileID(), 
									f.getFullPath().toString(), 
									relativeMovedToPath.removeLastSegments(1).toString());
						}
						System.out.println("sent file move request; moving from " +
							f.getProjectRelativePath().toString() + " to " + relativeMovedToPath);						
					} else {
						// send File.Rename request
						String newName = relativeMovedToPath.lastSegment();
						if (pm.isFileInWarnList(workspaceRelativePath, FileRenameNotification.class)) {
							pm.removeFileFromWarnList(workspaceRelativePath, FileRenameNotification.class);
						} else {
							rm.renameFile(fileMeta.getFileID(), f.getFullPath().makeAbsolute().toString(), newName);
						}
						System.out.println("sent file rename request; changing to " + newName);
					}
					
				}
				
			} else if ((delta.getFlags() & IResourceDelta.CONTENT) != 0) {
				
				// don't diff this if this is the actively open file
				String currFile = pm.getDocumentManager().getCurrFile();
				if (currFile != null) {					
					if (currFile.equals(f.getLocation().toString())) {
						System.out.println("Save did not trigger diffing for active document.");
						return;
					}
				}
				
				if ((delta.getFlags() & IResourceDelta.REPLACED) != 0) {
					System.out.println(String.format("File contents were replaced for %s", workspaceRelativePath));
					return;
				}
				
				EclipseRequestManager rm = pm.getRequestManager();
				if (pm.isFileInWarnList(workspaceRelativePath, FileChangeRequest.class)) {
					pm.removeFileFromWarnList(workspaceRelativePath, FileChangeRequest.class);
				} else {
					rm.pullDiffSendChanges(fileMeta);
				}
			}
			
		} else if (delta.getKind() == IResourceDelta.REMOVED) {
			if ((delta.getFlags() & IResourceDelta.MOVED_TO) == 0) {
				// File was deleted from disk
				if (pm.isFileInWarnList(workspaceRelativePath, FileDeleteNotification.class)) {
					pm.removeFileFromWarnList(workspaceRelativePath, FileDeleteNotification.class);
				} else {
					if (fileMeta == null) {
						System.out.println("No metadata found, ignoring file");
						return;
					}
					pm.getRequestManager().deleteFile(fileMeta.getFileID());
					System.out.println("sent file delete request");
				}
			}
			
		} else if (delta.getKind() == IResourceDelta.ADDED) {
			if ((delta.getFlags() & IResourceDelta.MOVED_FROM) != 0) {
				
				// do same as rename stuff
				IPath fullMovedFromPath = delta.getMovedFromPath();
				
				if (!fullMovedFromPath.toString().equals(f.getFullPath().toString())) {
					
					RequestManager rm = pm.getRequestManager();
					
					if (f.getName().equals(fullMovedFromPath.lastSegment())) {
						// send File.Move request
						if (pm.isFileInWarnList(workspaceRelativePath, FileMoveNotification.class)) {
							pm.removeFileFromWarnList(workspaceRelativePath, FileMoveNotification.class);
						} else {
							String movedFromPathString = fullMovedFromPath.toString().replace("\\", "/");
							
							fileMeta = mm.getFileMetadata(movedFromPathString);
							if (fileMeta == null) {
								System.out.println("No metadata found, ignoring file");
								return;
							}
							
							System.out.println("Getting metadata from file : " + movedFromPathString);
							rm.moveFile(fileMeta.getFileID(), movedFromPathString, 
									f.getProjectRelativePath().removeLastSegments(1).toString());
							System.out.println("sent file move request; moving from " +
									f.getFullPath().toString() + " to " + movedFromPathString);	
						}					
					} else {
						// send File.Rename request
						String newName = f.getProjectRelativePath().lastSegment();
						
						fileMeta = mm.getFileMetadata(fullMovedFromPath.toString().replace("\\", "/"));
						if (fileMeta == null) {
							System.out.println("No metadata found, ignoring file");
							return;
						}
						
						if (pm.isFileInWarnList(workspaceRelativePath, FileRenameNotification.class)) {
							pm.removeFileFromWarnList(workspaceRelativePath, FileRenameNotification.class);
						} else {
							rm.renameFile(fileMeta.getFileID(), f.getFullPath().makeAbsolute().toString(), newName);
							System.out.println("sent file rename request; changing to " + newName);
						}
					}
					
				}
			} else {
				System.out.println("file added - " + f.getName());
				System.out.println(pm.fileDirectoryWatchWarnList.keySet());
				ProjectMetadata pMeta = mm.getProjectMetadata(f.getProject().getLocation().toString());

				byte[] fileBytes;
				try {
					if (pm.isFileInWarnList(workspaceRelativePath, FileCreateNotification.class)) {
						pm.removeFileFromWarnList(workspaceRelativePath, FileCreateNotification.class);
					} else if (pm.isFileInWarnList(workspaceRelativePath, FileCreateResponse.class)) {
						pm.removeFileFromWarnList(workspaceRelativePath, FileCreateResponse.class);
					} else {
						InputStream in = f.getContents();
						fileBytes = EclipseRequestManager.inputStreamToByteArray(in);
						in.close();

						EclipseRequestManager rm = pm.getRequestManager();
						
						rm.createFile(f.getName(), f.getFullPath().toString(),
								f.getProjectRelativePath().removeLastSegments(1).toString(), pMeta.getProjectID(), fileBytes);						
						System.out.println("sent file create request: " + f.getName());
					}
				} catch (IOException | CoreException e) {
					e.printStackTrace();
				}
			}

		}
	}
	
}
