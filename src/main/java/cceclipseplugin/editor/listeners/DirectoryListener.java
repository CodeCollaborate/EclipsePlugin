package cceclipseplugin.editor.listeners;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

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
import websocket.models.responses.FileCreateResponse;

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
		ProjectMetadata projectMeta = PluginManager.getInstance().getMetadataManager().getProjectMetadata(p.getFullPath().toString());
		RequestManager rm = PluginManager.getInstance().getRequestManager();
		
		if (delta.getKind() == IResourceDelta.REMOVED) {
			System.out.println("flag: " + delta.getFlags());
			
			// Project was renamed
			if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
				String newName = delta.getMovedToPath().lastSegment();
				String newPath = delta.getMovedToPath().toString();
				
				rm.renameProject(projectMeta.getProjectID(), newName, newPath);
				System.out.println("sent project rename request: renamed to \"" + newName + "\"; path changed to : " + newPath);
				return false;
			}
			
			// Project was deleted from disk
			System.out.println("unsubscribed from project due to removal from disk");
			PluginManager.getInstance().getRequestManager().unsubscribeFromProject(projectMeta.getProjectID());
			return true;
			
		} else if (delta.getKind() == IResourceDelta.ADDED) {
			return true;
		}
		
		return false;
	}
	
	@Override
	protected void handleFile(IResourceDelta delta) {
		IFile f = (IFile) delta.getResource();
		PluginManager pm = PluginManager.getInstance();
		MetadataManager mm = pm.getMetadataManager();
		FileMetadata fileMeta = mm.getFileMetadata(f.getLocation().toString());
		String path = f.getProjectRelativePath().toString();
		
		System.out.println( "	Filename: " + f.getName() + "	File flag: " + delta.getFlags());
		
		if (delta.getKind() == IResourceDelta.CHANGED) {
			
			if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
				
				IPath relativeMovedToPath = delta.getMovedToPath().removeFirstSegments(1);
				IPath fullMovedToPath = f.getProject().getFullPath().append(relativeMovedToPath);
				
				if (!relativeMovedToPath.toString().equals(f.getProjectRelativePath().toString())) {
					
					RequestManager rm = pm.getRequestManager();
					
					if (f.getName().equals(relativeMovedToPath.lastSegment())) {
						// send File.Move request
						if (pm.isFileInWarnList(path, FileMoveNotification.class)) {
							pm.removeFileFromWarnList(path, FileMoveNotification.class);
						} else {
							// get metadata again but with the old path because old one should be null 
							// if the new path was used to find it
							String absPath = Paths.get(f.getProject().getLocation().toString(), 
									delta.getProjectRelativePath().toString()).normalize().toString();
							fileMeta = mm.getFileMetadata(absPath);
							rm.moveFile(fileMeta.getFileID(), 
									f.getLocation().toString(), 
									relativeMovedToPath.removeLastSegments(1).toString());
						}
						System.out.println("sent file move request; moving from " +
							f.getProjectRelativePath().toString() + " to " + relativeMovedToPath);						
					} else {
						// send File.Rename request
						String newName = relativeMovedToPath.lastSegment();
						if (pm.isFileInWarnList(path, FileRenameNotification.class)) {
							pm.removeFileFromWarnList(path, FileRenameNotification.class);
						} else {
							rm.renameFile(fileMeta.getFileID(), newName);
						}
						System.out.println("sent file rename request; changing to " + newName);
					}
					
				}
				
			} else if ((delta.getFlags() & IResourceDelta.CONTENT) != 0) {
				EclipseRequestManager rm = pm.getRequestManager();
				if (fileMeta != null) {
					rm.pullDiffSendChanges(fileMeta);
				}
			}
			
		} else if (delta.getKind() == IResourceDelta.REMOVED) {
			if ((delta.getFlags() & IResourceDelta.MOVED_TO) == 0) {
				// File was deleted from disk
				if (pm.isFileInWarnList(path, FileDeleteNotification.class)) {
					pm.removeFileFromWarnList(path, FileDeleteNotification.class);
				} else {
					pm.getRequestManager().deleteFile(fileMeta.getFileID());
					System.out.println("sent file delete request");
				}
			}
			
		} else if (delta.getKind() == IResourceDelta.ADDED) {
			if ((delta.getFlags() & IResourceDelta.MOVED_FROM) != 0) {
				// do same as rename stuff
				IPath relativeMovedFromPath = delta.getMovedFromPath().removeFirstSegments(1);
				
				if (!relativeMovedFromPath.toString().equals(f.getProjectRelativePath().toString())) {
					
					RequestManager rm = pm.getRequestManager();
					
					if (f.getName().equals(relativeMovedFromPath.lastSegment())) {
						// send File.Move request
						if (pm.isFileInWarnList(path, FileMoveNotification.class)) {
							pm.removeFileFromWarnList(path, FileMoveNotification.class);
						} else {
							String absPath = Paths.get(f.getProject().getLocation().toString(), 
									relativeMovedFromPath.toString()).normalize().toString().replace("\\", "/");
							fileMeta = mm.getFileMetadata(absPath);
							System.out.println("Getting metadata from file : " + absPath);
							rm.moveFile(fileMeta.getFileID(), 
									f.getLocation().toString(), 
									f.getProjectRelativePath().removeLastSegments(1).toString());
						}
						System.out.println("sent file move request; moving from " +
							f.getProjectRelativePath().toString() + " to " + relativeMovedFromPath);						
					} else {
						// send File.Rename request
						String newName = f.getProjectRelativePath().lastSegment();
						String absPath = Paths.get(f.getProject().getLocation().toString(), 
								relativeMovedFromPath.toString()).normalize().toString().replace("\\", "/");
						fileMeta = mm.getFileMetadata(absPath);
						if (pm.isFileInWarnList(path, FileRenameNotification.class)) {
							pm.removeFileFromWarnList(path, FileRenameNotification.class);
						} else {
							rm.renameFile(fileMeta.getFileID(), newName);
						}
						System.out.println("sent file rename request; changing to " + newName);
					}
					
				}
			} else {
				System.out.println("file added - " + f.getName());
				if (!f.getName().equals(".project")) {
					ProjectMetadata pMeta = mm.getProjectMetadata(f.getProject().getLocation().toString());
	
					byte[] fileBytes;
					try {
						if (pm.isFileInWarnList(path, FileCreateNotification.class)) {
							pm.removeFileFromWarnList(path, FileCreateNotification.class);
						} else if (pm.isFileInWarnList(path, FileCreateResponse.class)) {
							pm.removeFileFromWarnList(path, FileCreateResponse.class);
						} else {
							InputStream in = f.getContents();
							fileBytes = EclipseRequestManager.inputStreamToByteArray(in);
							in.close();
	
							EclipseRequestManager rm = pm.getRequestManager();
							
							rm.createFile(f.getName(), f.getFullPath().removeLastSegments(1).toString(),
									f.getProjectRelativePath().removeLastSegments(1).toString(), pMeta.getProjectID(), fileBytes);						
							System.out.println("sent file create request: " + f.getName());
						}
					} catch (IOException | CoreException e) {
						e.printStackTrace();
					}
	
				} else {
					System.out.println("metadata found for " + f.getName() + "; create request not sent");
				}
			}

		}
	}
	
}
