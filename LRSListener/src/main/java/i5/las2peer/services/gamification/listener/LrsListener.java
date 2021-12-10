package i5.las2peer.services.gamification.listener;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.glassfish.jersey.client.ClientConfig;
import org.json.JSONObject;

import i5.las2peer.api.Context;
import i5.las2peer.api.ManualDeployment;
import i5.las2peer.api.ServiceException;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.AnonymousAgent;
import i5.las2peer.api.security.UserAgent;
import i5.las2peer.connectors.webConnector.WebConnector;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@SuppressWarnings("unused")
@ServicePath("/gamification/listener")
@ManualDeployment
public class LRSListener extends RESTService implements Runnable{
	private Thread thread;
	private static String gamificationUrl;
	private static String lrsUrl;
	private static String listenerUrl;
	private static String configuratorUrl;
	private static String configId;
	private static String lrsAuth;
	private static String l2pAccessToken;
	private static List<LrsWorker> workers;
	

	@Override
	public void onStart() throws ServiceException {
		Properties properties =  new Properties();
		try {
			properties.load(new FileInputStream("./etc/i5.las2peer.services.gamification.listener.LRSListener.properties"));
			gamificationUrl = properties.getProperty("gamificationUrl");
			lrsUrl = properties.getProperty("lrsUrl");
			listenerUrl = properties.getProperty("listenerUrl");
			configuratorUrl = properties.getProperty("configuratorUrl");
			configId = properties.getProperty("configId");
			lrsAuth = properties.getProperty("lrsAuth");
			workers = Collections.synchronizedList(new ArrayList<LrsWorker>());
			thread = new Thread(this);
			thread.setDaemon(true);
			thread.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		try {
			Thread.sleep(60000);
			monitorWorkers();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * method for workerMonitorThread, kill worker as soon they expire
	 */
	private void monitorWorkers() {
		List<LrsWorker> workerCopy = new ArrayList<>(workers);
		for (LrsWorker lrsWorker : workerCopy ) {
			if (lrsWorker!= null) {
				if (lrsWorker.expired()) {
					workers.remove(lrsWorker);
				}
			}
		}
	}
	
	/**
	 * Function to return http unauthorized message
	 * @return HTTP response unauthorized
	 */
	private Response unauthorizedMessage(){
		JSONObject objResponse = new JSONObject();
		objResponse.put("message", "You are not authorized");
		i5.las2peer.api.Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
		return Response.status(HttpURLConnection.HTTP_UNAUTHORIZED).entity(objResponse.toString()).type(MediaType.APPLICATION_JSON).build();
	}
	
	/**
	 * 
	 * @param content json string in json in format of {"token":"[valueHere]"}
	 * @return http response
	 */
	@POST
	@Path("/start")
	public Response startListener(
			@ApiParam(value = "Mapping detail in JSON", required = true) byte[] content) {
		String name = "";
		String email = "";
		String l2pAuth = "";
		String userToken ="";
		Agent agent = i5.las2peer.api.Context.getCurrent().getMainAgent();
		if (agent instanceof AnonymousAgent) {
			return unauthorizedMessage();
		}
		else if (agent instanceof UserAgent) {
			UserAgent userAgent = (UserAgent) i5.las2peer.api.Context.getCurrent().getMainAgent();
			name = userAgent.getLoginName();
			if (userAgent.hasEmail()) {
				email = userAgent.getEmail();
			}
		}
		else {
			name = agent.getIdentifier();
		}
		try {
			if (content == null) {
				i5.las2peer.api.Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_10,
						"Could not start listener. Missing required payload");
				return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity("Could not start listener. Missing required payload").build();
			}
			JSONObject obj = new JSONObject(new String(content));
			String token = obj.getString("token");
			l2pAuth = getUserAuth(token);
			userToken = token;
			if (l2pAuth == null) {
				i5.las2peer.api.Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_10,
						"Could not start listener for user "+name +". Error creating authorization header from token");
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity("Could not parse received data for user  " + name).build();
			}
			try {
				if (!isHandlerExist(name)) {
					User user = new User(name, email, userToken, l2pAuth);
					LrsHandler handler = new LrsHandler(gamificationUrl, lrsUrl, listenerUrl, configuratorUrl, configId, lrsAuth, user);
					//estimated las2peer session duration
					LocalDateTime expirationDate = LocalDateTime.now().plusMinutes(60);
					LrsWorker worker = new LrsWorker(handler, expirationDate);
					Thread workerThread = new Thread(worker);
					workerThread.setDaemon(true);
					workers.add(worker);
					workerThread.start();
					i5.las2peer.api.Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_10,
							"Started listener for user "+name);
					return Response.status(HttpURLConnection.HTTP_OK).entity("Started LRSListener for user "+ name).build();
				}
				return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity("Listener for user " + name + " is already existing and running").build();
			} catch (Exception e) {
				e.printStackTrace();
				i5.las2peer.api.Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_10,
						"Could not start listener for user "+name +". The follwoing error ocurred " + e.getMessage());
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity("Could not start LRSListener for user " + name).build();
			}
		} catch (Exception e) {
			e.printStackTrace();
			i5.las2peer.api.Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_10,
					"Could not start listener for user "+name +". The follwoing error ocurred " + e.getMessage());
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity("Could not parse received data for user  " + name).build();
		}
	}
	
	/**
	 * checks if there exist a worker with a handler for given user
	 * @param name UserAgent name to check
	 * @return true if handler already exist
	 */
	private boolean isHandlerExist(String name) {
		if (workers != null) {
			for (LrsWorker lrsWorker : workers) {
				if (lrsWorker!=null) {
					if (lrsWorker.getHandler().getUser().getName().equals(name)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private String getUserAuth(String token) {
		String auth = null;
		try {
			String oidcSub = retrieveSub(token);
			oidcSub = "OIDC_SUB-"+oidcSub+":"+oidcSub;
			auth = Base64.getEncoder().encodeToString(oidcSub.getBytes());
			auth = "Basic " + auth;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return auth;
	}

	private String retrieveSub(String token) throws IOException {
		URL url = new URL("https://api.learning-layers.eu/auth/realms/main/protocol/openid-connect/userinfo");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Authorization", "Bearer " + token);
		int code = conn.getResponseCode();
		BufferedReader br = null;
		if (code >= 100 && code < 400) {
			br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
		} else {
			br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
		}

		StringBuilder sb = new StringBuilder();
		String responseLine = null;
		while ((responseLine = br.readLine()) != null) {
			sb.append(responseLine.trim());
		}
		String response = sb.toString();
		JSONObject obj = new JSONObject(response);
		response = obj.getString("sub");
		return response;
	}

	/**
	 * 
	 * @param mapping to set 
	 * @return htppresponse
	 */
	@POST
	@Path("/notify")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response notifed(@ApiParam(value = "Mapping detail in JSON", required = true) Mapping mapping) {
		String name = null;
		Agent agent = i5.las2peer.api.Context.getCurrent().getMainAgent();
		if (agent instanceof AnonymousAgent) {
			return unauthorizedMessage();
		}
		else if (agent instanceof UserAgent) {
			UserAgent userAgent = (UserAgent) i5.las2peer.api.Context.getCurrent().getMainAgent();
			name = userAgent.getLoginName();
		}
		else {
			name = agent.getIdentifier();
		}
		try {
			setMapping(mapping);
			i5.las2peer.api.Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
					"Could change mapping" + mapping.toString());
			i5.las2peer.api.Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15,
					"Mapping changed by user " + name);
			return Response.status(HttpURLConnection.HTTP_OK).entity("Mapping set successfully").build();
		} catch (Exception e) {
			e.printStackTrace();
			i5.las2peer.api.Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_10,
					"Could not set mapping. The follwoing error ocurred " + e.getMessage());
			i5.las2peer.api.Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11,
					"Mapping change started from user" + name);
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity("Mapping could not be set").build();
		}
	}

	private void setMapping(Mapping mapping) {
		for (LrsWorker lrsWorker : workers) {
			if (lrsWorker!= null) {
				lrsWorker.getHandler().setMap(mapping);
			}
		}
	}
}
