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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.sql.Connection;
import java.sql.SQLException;
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
public class LRSListener extends RESTService {
	private LrsHandler handler;
	private LrsWorker worker;
	private Thread thread;
	private String gamificationUrl;
	private String lrsUrl;
	private String lrsFilter;
	private String configuratorUrl;
	private String configId;
	private String lrsAuth;
	private String l2pAuth;
	private String l2pAccessToken;

	@Override
	public void onStart() throws ServiceException {
		Properties properties =  new Properties();
		try {
			properties.load(new FileInputStream("./etc/i5.las2peer.services.gamification.listener.LRSListener.properties"));
			gamificationUrl = properties.getProperty("gamificationUrl");
			lrsUrl = properties.getProperty("lrsUrl");
			lrsFilter = properties.getProperty("lrsFilter");
			configuratorUrl = properties.getProperty("configuratorUrl");
			configId = properties.getProperty("configId");
			lrsAuth = properties.getProperty("lrsAuth");
			l2pAuth = properties.getProperty("l2pAuth");
			l2pAccessToken = properties.getProperty("l2pAccessToken");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		handler = new LrsHandler(gamificationUrl, lrsUrl, lrsFilter, configuratorUrl, configId, lrsAuth, l2pAuth, l2pAccessToken);
		worker = new LrsWorker(handler);
		thread = new Thread(worker);
		thread.setDaemon(true);
		//thread.start();
	}	
	
	@Override
	public void onStop() {
		thread.interrupt();
	}

	@POST
	@Path("/notify")
	@Consumes(MediaType.APPLICATION_JSON)
	public void notifed(@ApiParam(value = "Mapping detail in JSON", required = true) Mapping mapping) {
		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_10,
					"Unauthorized mapping notification with user  " + name);
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11,
					"Because of unauthorized access could not set mapping " + mapping.toString());
			return;
		}
		try {
			handler.setMap(mapping);
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
					"Could change mapping" + mapping.toString());
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15,
					"Mapping changed by user " + name);
		} catch (Exception e) {
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_10,
					"Could not set mapping. The follwoing error ocurred " + e.getMessage());
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11,
					"Mapping change started from user" + name);
		}
	}

	// Method to test Statement retrieval locally
	@POST
	@Path("/testStatements")
	public Response retriveStatements() {
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget target = client.target("https://lrs.tech4comp.dbis.rwth-aachen.de/api/connection/statement");
		String response = target.path(
				"?filter=%7B%22%24and%22%3A%5B%7B%22%24comment%22%3A%22%7B%5C%22criterionLabel%5C%22%3A%5C%22A%5C%22%2C%5C%22criteriaPath%5C%22%3A%5B%5C%22person%5C%22%5D%7D%22%2C%22person._id%22%3A%7B%22%24in%22%3A%5B%7B%22%24oid%22%3A%225db02311c5dcd9003d904ba4%22%7D%5D%7D%7D%5D%7D&sort=%7B%22timestamp%22%3A-1%2C%22_id%22%3A1%7D")
				.request()
				.header("Authorization",
						"Basic NjM2NmZiZDgzNDU5M2M3MDU5ODU3ZTg4ODQwYjMyZGRmMTY1NjQwMzo0MGUxZjRlZmRlNDFlM2JlZTFiMWJlOTIxNDQ1ODc5OWEwMWZhNDAy")
				.get(String.class).toString();
		return Response.status(HttpURLConnection.HTTP_OK).entity(response).build();
	}

	// Method to test Mapping retrieval locally
	@POST
	@Path("/testMapping")
	public Response retriveMapping() {
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget target = client.target("http://localhost:8080/gamification/configurator");
		Mapping response = target.path("/mapping/testConfig").request().header("access-token",
				"eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJoZTJ6NVRzbEM1M3VPQXZxNmFWckplT2I0ZUx5TUxUam9IT3dIdTBiRmFJIn0.eyJleHAiOjE2MzUzNDYxMzgsImlhdCI6MTYzNTM0MjUzOCwiYXV0aF90aW1lIjoxNjM1MzQyNTE5LCJqdGkiOiI1YmMyNTViNi00YTQ5LTRlMzItODA0ZC02Yzc3ODRlNTNkN2IiLCJpc3MiOiJodHRwczovL2FwaS5sZWFybmluZy1sYXllcnMuZXUvYXV0aC9yZWFsbXMvbWFpbiIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiI5OTMzLTlkM2RhYjUxYWE5MCIsInR5cCI6IkJlYXJlciIsImF6cCI6ImJkZGE3Mzk2LTNmNmQtNGQ4My1hYzIxLTY1YjQwNjlkMGVhYiIsIm5vbmNlIjoiNjM2ZTdhZjVkZmJjNGM5MzlkMmUwYzA1MzAxMDIzNjUiLCJzZXNzaW9uX3N0YXRlIjoiYWUwNTBhYzYtZjg2Mi00Y2I2LWJkYTYtZDM1NDJlMjk0NThmIiwiYWNyIjoiMCIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vMTM3LjIyNi4yMzIuMTc1OjMyMDEwIiwiaHR0cDovL3RlY2g0Y29tcC5kYmlzLnJ3dGgtYWFjaGVuLmRlOjMxMDEwIiwiaHR0cDovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODAiLCJodHRwczovL2ZpbGVzLnRlY2g0Y29tcC5kYmlzLnJ3dGgtYWFjaGVuLmRlIiwiaHR0cDovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6OTA5OCIsImh0dHBzOi8vY2xvdWQxMC5kYmlzLnJ3dGgtYWFjaGVuLmRlOjgwODQiLCJodHRwczovL21vbml0b3IudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiLCJodHRwOi8vMTI3LjAuMC4xOjgwODEiLCJodHRwczovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODA4MCIsImh0dHBzOi8vZ2l0LnRlY2g0Y29tcC5kYmlzLnJ3dGgtYWFjaGVuLmRlIiwiaHR0cDovLzEyNy4wLjAuMTo4MCIsImh0dHA6Ly9sb2NhbGhvc3Q6ODAiLCJodHRwczovL2NhZS1kZXYudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiLCJodHRwOi8vMTI3LjAuMC4xOjgwODAiLCJodHRwOi8vbG9jYWxob3N0OjgwODAiLCJodHRwOi8vbGFzMnBlZXIuZGJpcy5yd3RoLWFhY2hlbi5kZSIsImh0dHBzOi8vbGFzMnBlZXIuZGJpcy5yd3RoLWFhY2hlbi5kZTo5MDk4IiwiaHR0cDovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODA4MCIsImh0dHA6Ly9sb2NhbGhvc3Q6ODA4MSIsImh0dHBzOi8vbGFzMnBlZXIudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiLCJodHRwczovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODAiLCJodHRwOi8vY2xvdWQxMC5kYmlzLnJ3dGgtYWFjaGVuLmRlOjgwODIiLCJodHRwczovL3NiZi1kZXYudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIiwic2lkIjoiYWUwNTBhYzYtZjg2Mi00Y2I2LWJkYTYtZDM1NDJlMjk0NThmIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJNYXJjIEJlbHNjaCIsInByZWZlcnJlZF91c2VybmFtZSI6Im1iZWxzY2giLCJnaXZlbl9uYW1lIjoiTWFyYyIsImZhbWlseV9uYW1lIjoiQmVsc2NoIiwiZW1haWwiOiJtYXJjLmJlbHNjaEByd3RoLWFhY2hlbi5kZSJ9.LMjOFlT-3JWqDPbSymEQKX9sROvosWIAPMRufTocRGy-0DQAuIJS41iSYPO3jyRC2i9HsyGdShoJy9ISocb4F3BiWUQQleuqXQ9zGAVP9i26j9fTH4xJRR8YWIIQp57-f8tx63dA85J9IAJZvaNkLGDcPzq2e5bbCOlCbRyB3KrirA3gtnwspwFF8yl4YHf93bQsugkAtdUACU-Ouh65_dDdTsX7nDmv2PsXC-qOWc52DPmPydOC_PEzP00hI5AkgUnmNLx6YqBT5Yif3jrMUkkzwlzBQDoQGGPIbNOGwosENlDbuaZ7jdqrpexXTWys7fGh6foU7zAApHKSmxqUFw")
				.header("Authorization", "Basic T0lEQ19TVUItOTkzMy05ZDNkYWI1MWFhOTA6OTkzMy05ZDNkYWI1MWFhOTA=")
				.get(Mapping.class);
		return Response.status(HttpURLConnection.HTTP_OK).entity(response).build();
	}

	//Method to for testint multipart form-data
	@POST
	@Path("/data")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "createGame", notes = "Method to create a new game")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Cannot connect to database"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Database Error"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Error in parsing form data"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Game ID already exist"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Game ID cannot be empty"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Error checking app ID exist"),
			@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Unauthorized"),
			@ApiResponse(code = HttpURLConnection.HTTP_CREATED, message = "New game created") })
	public Response createGame(
			@ApiParam(value = "Game detail in multiple/form-data type", required = true) @HeaderParam(value = HttpHeaders.CONTENT_TYPE) String contentType,
			@ApiParam(value = "Content of form data", required = true) byte[] formData) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"POST " + "gamification/games/data", true);
		long randomLong = new Random().nextLong(); // To be able to match

		JSONObject objResponse = new JSONObject();
		String name = null;
		String gameid = null;
		String gamedesc = null;
		String commtype = null;
		Connection conn = null;


		Map<String, FormDataPart> parts;

		try {
			parts = MultipartHelper.getParts(formData, contentType);

			FormDataPart partGameID = parts.get("gameid");
			if (partGameID != null) {
				// these data belong to the (optional) file id text input form element
				gameid = partGameID.getContent();
				// gameid must be unique
				System.out.println(gameid);

				FormDataPart partGameDesc = parts.get("gamedesc");
				if (partGameDesc != null) {
					gamedesc = partGameDesc.getContent();
				} else {
					gamedesc = "";
				}
				FormDataPart partCommType = parts.get("commtype");
				if (partGameDesc != null) {
					commtype = partCommType.getContent();
				} else {
					commtype = "def_type";
				}
				objResponse.put("gameIIIIId", gameid);
				objResponse.put("dessssc", gamedesc);
				objResponse.put("commmmmmmmmmmmmmmtype", commtype);
				return Response.status(200).entity(objResponse.toString()).build();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Response.status(200).build();
	}
}
