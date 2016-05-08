package clientcore.models;

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
	protected String[] changes;

	@JsonProperty("FileVersion")
	protected long fileVersion;

	public FileChangeRequest(long fileID, String[] changes, long fileVersion) {
		this.fileID = fileID;
		this.changes = changes;
		this.fileVersion = fileVersion;
	}

	public Request getRequest() {
		return new Request("File", "Change", this, 
		response -> {
			System.out.println(response);
		} , 
		() -> {
			System.out.println("Failed to send file change to the server.");
		});
	}

}
