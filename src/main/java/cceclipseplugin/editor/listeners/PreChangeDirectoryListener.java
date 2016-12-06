package cceclipseplugin.editor.listeners;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

import cceclipseplugin.core.EclipseRequestManager;
import cceclipseplugin.core.PluginManager;
import dataMgmt.MetadataManager;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;
import websocket.models.notifications.FileCreateNotification;

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
		IFile f = (IFile) delta.getResource();
		System.out.println("PRE-CHANGE; Filename: " + f.getName() + " File flag: " + delta.getFlags());
		PluginManager pm = PluginManager.getInstance();
		MetadataManager mm = pm.getMetadataManager();
		FileMetadata fileMeta = mm.getFileMetadata(f.getFullPath().removeLastSegments(1).toString());
		String path = Paths.get(fileMeta.getRelativePath(), fileMeta.getFilename()).toString();
		
		// File was added
		if (delta.getKind() == IResourceDelta.ADDED) {
			System.out.println("file added - " + f.getName());
			if (!f.getName().equals(".project")) {
				ProjectMetadata pMeta = mm.getProjectMetadata(f.getProject().getFullPath().toString());

				byte[] fileBytes;
				try {
					if (pm.isFileInWarnList(path, FileCreateNotification.class)) {
						pm.removeFileFromWarnList(path, FileCreateNotification.class);
					} else {
						InputStream in = f.getContents();
						fileBytes = EclipseRequestManager.inputStreamToByteArray(in);
						in.close();

						EclipseRequestManager rm = pm.getRequestManager();
						
						rm.createFile(f.getName(), f.getFullPath().removeLastSegments(1).toString(),
								f.getProjectRelativePath().removeLastSegments(1).toString(), pMeta.getProjectID(), fileBytes);						
					}
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
