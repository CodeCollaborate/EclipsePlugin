package cceclipseplugin.editor.listeners;

import java.nio.file.Paths;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IPath;

import cceclipseplugin.core.EclipseRequestManager;
import cceclipseplugin.core.PluginManager;
import dataMgmt.MetadataManager;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;
import requestMgmt.RequestManager;
import websocket.models.notifications.FileDeleteNotification;
import websocket.models.notifications.FileMoveNotification;
import websocket.models.notifications.FileRenameNotification;

public class PostChangeDirectoryListener extends AbstractDirectoryListener {
	
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
		FileMetadata fileMeta = mm.getFileMetadata(f.getFullPath().removeLastSegments(1).toString());
		String path = f.getProjectRelativePath().toString();
		
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
							rm.moveFile(fileMeta.getFileID(), 
									fullMovedToPath.removeLastSegments(1).toString(), 
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
				
				rm.pullDiffSendChanges(fileMeta);
				
			}
			
		} else if (delta.getKind() == IResourceDelta.REMOVED) {
			// File was deleted from disk
			if (pm.isFileInWarnList(path, FileDeleteNotification.class)) {
				pm.removeFileFromWarnList(path, FileDeleteNotification.class);
			} else {
				pm.getRequestManager().deleteFile(fileMeta.getFileID());
				System.out.println("sent file delete request");
			}
		}
	}
	
}
