package clientcore.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import websocket.IResponseHandler;
import websocket.models.IRequestData;
import websocket.models.Response;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FileChangeRequest implements IRequestData {

	@JsonProperty("FileID")
	protected long fileID;

	@JsonProperty("Changes")
	protected List<String> changes;

	@JsonProperty("FileVersion")
	protected long fileVersion;

	public FileChangeRequest(long fileID, List<String> changes, long fileVersion) {
		this.fileID = fileID;
		this.changes = changes;
		this.fileVersion = fileVersion;
	}

	@JsonIgnore
	public NewRequest getRequest() {
		return new NewRequest("File", "Change", this, 
		(response) -> {
			System.out.println("Received file change response: " + response);
		} , 
		() -> {
			System.out.println("Failed to send file change to the server.");
		});
	}

}
