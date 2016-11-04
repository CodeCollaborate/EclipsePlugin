package cceclipseplugin.editor.listeners;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IPath;

import cceclipseplugin.core.PluginManager;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;

public class PostChangeDirectoryListener extends AbstractDirectoryListener {

	/**
	 * Handles a resource delta in the case that the resource is and IProject.
	 * 
	 * @param delta
	 * @return true if the recursion down the delta's children stops after this handler
	 */
	@Override
	protected boolean handleProject(IResourceDelta delta) {
		IProject p = (IProject) delta.getResource();
		ProjectMetadata projectMeta = PluginManager.getInstance().getMetadataManager().getProjectMetadata(p.getFullPath().toString());
		
		if (delta.getKind() == IResourceDelta.REMOVED) {
			System.out.println("flag: " + delta.getFlags());
			// Project was renamed
			if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
				String newName = delta.getMovedToPath().lastSegment();
				String newPath = delta.getMovedToPath().toString();
				PluginManager.getInstance().getRequestManager().renameProject(projectMeta.getProjectID(), newName, 
						newPath);
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
		FileMetadata fileMeta = PluginManager.getInstance().getMetadataManager().getFileMetadata(f.getFullPath().removeLastSegments(1).toString());

//		if (delta.getKind() == IResourceDelta.REMOVED || delta.getKind() == IResourceDelta.CHANGED) {
//			System.out.println("flag: " + delta.getFlags());
//			// File was renamed
			if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
				IPath relativeMovedToPath = delta.getMovedToPath().removeFirstSegments(1);
				IPath fullMovedToPath = f.getProject().getFullPath().append(relativeMovedToPath);
				if (!relativeMovedToPath.toString().equals(f.getProjectRelativePath().toString())) {
					if (f.getName().equals(relativeMovedToPath.lastSegment())) {
						PluginManager.getInstance().getRequestManager().moveFile(fileMeta.getFileID(), 
								fullMovedToPath.removeLastSegments(1).toString(), relativeMovedToPath.removeLastSegments(1).toString());
						System.out.println("sent file move request; moving from " +
							f.getProjectRelativePath().toString() + " to " + relativeMovedToPath);
						// TODO (loganga): process file changes and send as File.Change 
					} else {
						String newName = relativeMovedToPath.lastSegment();
						PluginManager.getInstance().getRequestManager().renameFile(fileMeta.getFileID(), newName);
						System.out.println("sent file rename request; changing to " + newName);
						// TODO (loganga): process file changes and send as File.Change 
					}
				}
			} else if (delta.getKind() == IResourceDelta.REMOVED) {
				// File was deleted from disk
				if (fileMeta != null) {
					PluginManager.getInstance().getRequestManager().deleteFile(fileMeta.getFileID());
					System.out.println("sent file delete request");
				}
			} 
//			else if (delta.getKind() == IResourceDelta.CHANGED) {
//				if ((delta.getFlags() & (IResourceDelta.CONTENT)) != 0) {
//					System.out.println("content");
//				} else if ((delta.getFlags() & IResourceDelta.LOCAL_CHANGED) != 0) {
//					System.out.println("local changed");
//				} else if ((delta.getFlags() & IResourceDelta.DERIVED_CHANGED) != 0) {
//					System.out.println("derived changed");
//				}
//			}
//		}
	}

}
