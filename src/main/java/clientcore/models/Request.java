package clientcore.models;

import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.IRequestSendErrorHandler;
import websocket.IResponseHandler;
import websocket.models.IRequestData;

/**
 * Created by fahslaj on 4/14/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Request extends websocket.models.Request {
	
	private static AtomicLong tagGenerator = new AtomicLong(0);
	
    @JsonProperty("Tag")
    protected long tag;

    @JsonProperty("Resource")
    protected String resource;

    @JsonProperty("Method")
    protected String method;

    @JsonProperty("SenderID")
    protected String senderId;

    @JsonProperty("SenderToken")
    protected String senderToken;

    @JsonProperty("Timestamp")
    protected long timestamp;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    protected IRequestData data;

    @JsonIgnore
    private IResponseHandler responseHandler;

    @JsonIgnore
    private IRequestSendErrorHandler errorHandler;
    
    

    public Request(String resource, String method,
			IRequestData data, IResponseHandler responseHandler, IRequestSendErrorHandler errorHandler) {
		super();
		this.tag = tagGenerator.getAndIncrement();
		this.resource = resource;
		this.method = method;
		// TODO: get these from the client core based on user info
		this.senderId = "12345";
		this.senderToken = "12345";
		this.timestamp = System.currentTimeMillis();
		this.data = data;
		this.responseHandler = responseHandler;
		this.errorHandler = errorHandler;
	}

	public long getTag() {
        return tag;
    }

    public void setTag(long tag) {
        this.tag = tag;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderToken() {
        return senderToken;
    }

    public void setSenderToken(String senderToken) {
        this.senderToken = senderToken;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public IRequestData getData() {
        return data;
    }

    public void setData(IRequestData data) {
        this.data = data;
    }

    public IResponseHandler getResponseHandler() {
        return responseHandler;
    }

    public void setResponseHandler(IResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }

    public IRequestSendErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(IRequestSendErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }
}