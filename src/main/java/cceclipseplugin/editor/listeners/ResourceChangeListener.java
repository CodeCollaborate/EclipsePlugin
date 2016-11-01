package cceclipseplugin.editor.listeners;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IPath;

import cceclipseplugin.core.PluginManager;
import dataMgmt.MetadataManager;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;

public class ResourceChangeListener implements IResourceChangeListener {

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta rootDelta = event.getDelta();
		if (rootDelta == null) {
			return;
		}
		
		System.out.println("resource change detected");
		recursivelyHandleChange(rootDelta);
	}
	
	/**
	 * Iterates through the children of the given IResourceDelta and, if a registered CodeCollaborate
	 * project with metadata, sends the resource to its corresponding handler. 
	 * 
	 * @param delta
	 */
	private void recursivelyHandleChange(IResourceDelta delta) {
		IResource res = delta.getResource();
		
		if (res instanceof IFile) {
			System.out.println("type: file; kind: " + delta.getKind());
			handleFile(delta);
			
		}	else if (res instanceof IProject) {
			// stop handling if the project doesn't have CodeCollaborate metadata
			System.out.println("type: project; kind: " + delta.getKind());
			MetadataManager meta = PluginManager.getInstance().getMetadataManager();
			if (meta.getProjectMetadata(res.getFullPath().toString()) == null) {
				return;
			}
			
			handleProject(delta);
			
		}
		
		for (IResourceDelta childDelta : delta.getAffectedChildren()) {
			recursivelyHandleChange(childDelta);
		}
	}
	
	private void handleProject(IResourceDelta delta) {
		IProject p = (IProject) delta.getResource();
		ProjectMetadata projectMeta = PluginManager.getInstance().getMetadataManager().getProjectMetadata(p.getFullPath().toString());
		if (delta.getKind() == IResourceDelta.REMOVED) {
			System.out.println("flag: " + delta.getFlags());
			// Project was renamed
			if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
				String newName = delta.getMovedToPath().lastSegment();
				// TODO (loganga): send project rename request
				System.out.println("sent project rename request: renamed to \"" + newName + "\"");
			}
			// Project was deleted from disk
			else {
				System.out.println("unsubscribed from project due to removal from disk");
				PluginManager.getInstance().getRequestManager().unsubscribeFromProject(projectMeta.getProjectID());
			}
		}
	}
	
	private void handleFile(IResourceDelta delta) {
		IFile f = (IFile) delta.getResource();
		FileMetadata fileMeta = PluginManager.getInstance().getMetadataManager().getFileMetadata(f.getProjectRelativePath().toString());

		if (delta.getKind() == IResourceDelta.REMOVED || delta.getKind() == IResourceDelta.CHANGED) {
			System.out.println("flag: " + delta.getFlags());
			// File was renamed
			if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
				IPath relativeMovedToPath = delta.getMovedToPath().removeFirstSegments(1);
				if (!relativeMovedToPath.toString().equals(f.getProjectRelativePath().toString())) {
					if (f.getName().equals(relativeMovedToPath.lastSegment())) {
						System.out.println("sent file move request; moving from " +
							f.getProjectRelativePath().toString() + " to " + relativeMovedToPath);
					} else {
						String newName = relativeMovedToPath.lastSegment();
						System.out.println("sent file rename request; changing to " + newName);
					}
				}
			} else if (delta.getKind() == IResourceDelta.REMOVED) {
				// File was deleted from disk
				// TODO (loganga): send file delete request
				System.out.println("sent file delete request");
			}
		}
	}

}
