package clientcore.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FileChangeNotification {
	@JsonProperty("FileID")
	protected long fileID;

	@JsonProperty("Changes")
	protected List<String> changes;

	@JsonProperty("FileVersion")
	protected long fileVersion;

	public long getFileID() {
		return fileID;
	}

	public List<String> getChanges() {
		return changes;
	}

	public long getFileVersion() {
		return fileVersion;
	}
}
