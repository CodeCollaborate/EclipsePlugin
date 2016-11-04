package cceclipseplugin.editor.listeners;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

import cceclipseplugin.core.EclipseRequestManager;
import cceclipseplugin.core.PluginManager;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;

public class PreChangeDirectoryListener extends AbstractDirectoryListener {

	@Override
	protected boolean handleProject(IResourceDelta delta) {
		
		if (delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.REMOVED) {
			return true;
		}
		
		return false;
	}
	
	@Override
	protected void handleFile(IResourceDelta delta) {
//		System.out.println("///PreChange Handler///");
		IFile f = (IFile) delta.getResource();
		FileMetadata fileMeta = PluginManager.getInstance().getMetadataManager().getFileMetadata(f.getFullPath().removeLastSegments(1).toString());
		
		if (delta.getKind() == IResourceDelta.ADDED) {
			if (fileMeta == null && !f.getName().equals(".project")) {
				ProjectMetadata pMeta = PluginManager.getInstance().getMetadataManager().getProjectMetadata(f.getProject().getFullPath().toString());
				byte[] fileBytes;
				try {
					fileBytes = EclipseRequestManager.inputStreamToByteArray(f.getContents());
					PluginManager.getInstance().getRequestManager().createFile(f.getName(), f.getFullPath().removeLastSegments(1).toString(), 
							f.getProjectRelativePath().removeLastSegments(1).toString(), pMeta.getProjectID(), fileBytes);
				} catch (IOException | CoreException e) {
					e.printStackTrace();
				}
				System.out.println("sent file create request: " + f.getName());
			} else {
				System.out.println("metadata found for " + fileMeta.getFilename() + "; create request not sent");
			}
		}
	}

}
