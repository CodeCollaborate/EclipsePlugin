package cceclipseplugin.editor.listeners;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import cceclipseplugin.core.EclipseRequestManager;
import cceclipseplugin.core.PluginManager;
import dataMgmt.MetadataManager;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;

public class PreChangeDirectoryListener extends AbstractDirectoryListener {

	private byte[] oldFileBytes;
	
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
		System.out.println("File flag: " + delta.getFlags());
		IFile f = (IFile) delta.getResource();
		MetadataManager mm = PluginManager.getInstance().getMetadataManager();
		FileMetadata fileMeta = mm.getFileMetadata(f.getFullPath().removeLastSegments(1).toString());
		
		// File was added
		if (delta.getKind() == IResourceDelta.ADDED) {
			System.out.println("PRE-CHANGE: file added - " + f.getName());
			if (fileMeta == null && !f.getName().equals(".project")) {
				ProjectMetadata pMeta = mm.getProjectMetadata(f.getProject().getFullPath().toString());
				
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
		if ((delta.getFlags() & IResourceDelta.MOVED_FROM) != 0 && (delta.getFlags() & IResourceDelta.CONTENT) != 0) {
			
			System.out.println("PRE-CHANGE: file moved or renamed - " + f.getName());
//			IPath relativeMovedFromPath = delta.getMovedFromPath().removeFirstSegments(1);
			IFile oldFile = f.getProject().getFile(f.getProjectRelativePath().toString());
			
			try {
				this.oldFileBytes = EclipseRequestManager.inputStreamToByteArray(oldFile.getContents());
			} catch (IOException | CoreException e) {
				System.out.println("Failed to store old file bytes.");
				e.printStackTrace();
			}
			
		}
	}
	
	public byte[] getOldFileBytes() {
		return this.oldFileBytes;
	}

}
