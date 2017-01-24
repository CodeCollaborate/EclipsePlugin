package cceclipseplugin.editor.listeners;

import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import cceclipseplugin.core.CCIgnore;
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
	
	private final Logger logger = LogManager.getLogger("directoryListener");

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
					logger.debug(String.format("sent project rename request: renamed to \"%s\"; path changed to : \"%s\"", newName, newPath));
					return false;
				}
			} else {
				if (pm.isProjectInWarnList(p.getName(), ProjectDeleteNotification.class)) {
					pm.removeProjectFromWarnList(p.getName(), ProjectDeleteNotification.class);
					return true;
				} else {
					logger.debug("Deleting project");
					// Project was deleted from disk
					logger.debug("Unsubscribed from project due to removal from disk");
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
		
		logger.debug(String.format("Filename: %s File flag: %d", f.getName(), delta.getFlags()));
		
		if (delta.getKind() == IResourceDelta.CHANGED) {
			if (fileMeta == null) {
			    // rather than deleting the document if metadata doesn't exist, we want to
				//		a) check it's not in the .ccignore
				//		b) if it's not, add it to the server
				logger.warn("No metadata found for file change event, resolving");
				createFile(f, pm, mm);
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
						logger.debug(String.format("Sent file move request; moving from %s to %s",
										f.getProjectRelativePath().toString(), relativeMovedToPath));
					} else {
						// send File.Rename request
						String newName = relativeMovedToPath.lastSegment();
						if (pm.isFileInWarnList(workspaceRelativePath, FileRenameNotification.class)) {
							pm.removeFileFromWarnList(workspaceRelativePath, FileRenameNotification.class);
						} else {
							rm.renameFile(fileMeta.getFileID(), newName);
						}
						logger.debug(String.format("Sent file rename request; changing to %s", newName));
					}
					
				}
				
			} else if ((delta.getFlags() & IResourceDelta.CONTENT) != 0) {
				// don't diff this if this is the actively open file
				String currFile = pm.getDocumentManager().getCurrFile();
				if (currFile != null) {					
					if (currFile.equals(f.getLocation().toString())) {
						logger.debug("Save did not trigger diffing for active document.");
						return;
					}
				}
				
				if ((delta.getFlags() & IResourceDelta.REPLACED) != 0) {
					logger.debug(String.format("File contents were replaced for %s", workspaceRelativePath));
					return;
				}

				if (pm.isFileInWarnList(workspaceRelativePath, FileChangeRequest.class)) {
					pm.removeFileFromWarnList(workspaceRelativePath, FileChangeRequest.class);
				} else {
					if (fileMeta == null) {
						// file should have metadata but doesn't, so send a request to make the file
						createFile(f, pm, mm);
					} else {
						EclipseRequestManager rm = pm.getRequestManager();
						// I'm really at a loss as to how to make sure this isn't triggering directly after the file
						// is pulled w/out causing side effects. I thought about putting in at EclipseRequestManager:100, 
                        // but I'm not sure it's a good idea because it may never get removed. 
                        //
                        // The problem stems from the fact that this case of IResource flags isn't specific enough to 
                        // differentiate when the file is being written to after closing the editor vs from the plugin 
                        // itself.
						//
						// I tried looking at IResourceDelta codes and trying to find the more specific case where a file 
                        // is replaced, but we're already checking IResourceDelta.REPLACED, so I'm quite sure why that's
                        // not being flipped.
						//
						// If you have an idea, please let me know - Joel (jshap70)
						rm.pullDiffSendChanges(fileMeta);
					}
				}
			}
			
		} else if (delta.getKind() == IResourceDelta.REMOVED) {
			if ((delta.getFlags() & IResourceDelta.MOVED_TO) == 0) {
				// File was deleted from disk
				if (pm.isFileInWarnList(workspaceRelativePath, FileDeleteNotification.class)) {
					pm.removeFileFromWarnList(workspaceRelativePath, FileDeleteNotification.class);
				} else {
					if (fileMeta == null) {
						logger.debug("No metadata found, ignoring file");
						return;
					}
					pm.getRequestManager().deleteFile(fileMeta.getFileID());
					logger.debug("Sent file delete request");
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
								logger.warn("No metadata found, ignoring file");
								return;
							}
							
							logger.debug(String.format("Getting metadata from file : %s", movedFromPathString));
							rm.moveFile(fileMeta.getFileID(), movedFromPathString, 
									f.getProjectRelativePath().removeLastSegments(1).toString());
							logger.debug(String.format("Sent file move request; moving from %s to %s",
											f.getFullPath().toString(), movedFromPathString));
						}					
					} else {
						// send File.Rename request
						String newName = f.getProjectRelativePath().lastSegment();
						
						fileMeta = mm.getFileMetadata(fullMovedFromPath.toString().replace("\\", "/"));
						if (fileMeta == null) {
							logger.debug("No metadata found, ignoring file");
							return;
						}
						
						if (pm.isFileInWarnList(workspaceRelativePath, FileRenameNotification.class)) {
							pm.removeFileFromWarnList(workspaceRelativePath, FileRenameNotification.class);
						} else {
							rm.renameFile(fileMeta.getFileID(), newName);
							logger.debug(String.format("Sent file rename request; changing to %s", newName));
						}
					}
					
				}
			} else {
				logger.debug(String.format("File added - %s", f.getName()));

				if (pm.isFileInWarnList(workspaceRelativePath, FileCreateNotification.class)) {
					pm.removeFileFromWarnList(workspaceRelativePath, FileCreateNotification.class);
				} else if (pm.isFileInWarnList(workspaceRelativePath, FileCreateResponse.class)) {
					pm.removeFileFromWarnList(workspaceRelativePath, FileCreateResponse.class);
				} else {
					createFile(f, pm, mm);
				}
			}
		}
	}

	private void createFile(IFile f, PluginManager pm, MetadataManager mm) {
		RequestManager rm = pm.getRequestManager();

		ProjectMetadata pMeta = mm.getProjectMetadata(f.getProject().getLocation().toString());
		IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(pMeta.getName());
		CCIgnore ignoreFile = CCIgnore.createForProject(p);

		if (ignoreFile.containsEntry(f.getFullPath().toString())) {
			logger.debug(String.format("file ignored by .ccignore: %s", f.getFullPath().toString()));
			return;
		} else {
			try(InputStream in = f.getContents()) {
				byte[] fileBytes = EclipseRequestManager.inputStreamToByteArray(in);
				rm.createFile(f.getName(), f.getFullPath().toString(), f.getProjectRelativePath().removeLastSegments(1).toString(), pMeta.getProjectID(), fileBytes);
				logger.debug(String.format("Sent file create request: %s", f.getName()));
			} catch (IOException | CoreException e) {
				e.printStackTrace();
			}
		}
	}
	
}
