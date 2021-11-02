package i5.las2peer.services.gamification.listener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.Reader;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.glassfish.jersey.client.ClientConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import i5.las2peer.api.Context;
import i5.las2peer.api.Service;
import i5.las2peer.api.ServiceException;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.security.UserAgent;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import io.swagger.annotations.ApiParam;

@ServicePath("/gamification/listener")
@SuppressWarnings("unused")
public class LRSListener extends RESTService implements Runnable{
	private LrsHandler handler;
	private Thread thread;

	@Override
	public void onStart() throws ServiceException {
		handler = new LrsHandler();
		thread = new Thread(handler);
		thread.setDaemon(true);
		thread.start();
	}
	
	@Override
	public void onStop() {
		thread.interrupt();
	}
	
	
	@Override
	public void run() {
		
	}
	@POST
	@Path("/notify")
	@Consumes(MediaType.APPLICATION_JSON)
	public void notifed(
			@ApiParam(value = "Mapping detail in JSON", required = true) Mapping mapping) {
		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_10, "Unauthorized mapping notification with user  " + name);
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11, "Because of unauthorized access could not set mapping " + mapping.toString());
			return;
		}
		try {
			handler.setMap(mapping);
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "Could change mapping" + mapping.toString());
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15, "Mapping changed by user " + name);
		} catch (Exception e) {
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_10, "Could not set mapping. The follwoing error ocurred " + e.getMessage());
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11, "Mapping change started from user" + name);
		}
	}
	
	@POST
	@Path("/testStatements")
	public Response retriveStatements() {
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget target = client.target("https://lrs.tech4comp.dbis.rwth-aachen.de/api/connection/statement");
		String response = target
				.path("?filter=%7B%22%24and%22%3A%5B%7B%22%24comment%22%3A%22%7B%5C%22criterionLabel%5C%22%3A%5C%22A%5C%22%2C%5C%22criteriaPath%5C%22%3A%5B%5C%22person%5C%22%5D%7D%22%2C%22person._id%22%3A%7B%22%24in%22%3A%5B%7B%22%24oid%22%3A%225db02311c5dcd9003d904ba4%22%7D%5D%7D%7D%5D%7D&sort=%7B%22timestamp%22%3A-1%2C%22_id%22%3A1%7D")
				.request()
				.header("Authorization", "Basic NjM2NmZiZDgzNDU5M2M3MDU5ODU3ZTg4ODQwYjMyZGRmMTY1NjQwMzo0MGUxZjRlZmRlNDFlM2JlZTFiMWJlOTIxNDQ1ODc5OWEwMWZhNDAy")
				.get(String.class)
				.toString();
		List<LrsStatement> statements = null;
		if (response!= null) {
			statements = parseToList(response);
		}
		return Response.status(HttpURLConnection.HTTP_OK).entity(response).build();
	}
	
	@POST
	@Path("/testMapping")
	public Response retriveMapping() {
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget target = client.target("http://localhost:8080/gamification/configurator");
		Mapping response = target
				.path("/mapping/testConfig")
				.request()
				.header("access-token", "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJoZTJ6NVRzbEM1M3VPQXZxNmFWckplT2I0ZUx5TUxUam9IT3dIdTBiRmFJIn0.eyJleHAiOjE2MzUzNDYxMzgsImlhdCI6MTYzNTM0MjUzOCwiYXV0aF90aW1lIjoxNjM1MzQyNTE5LCJqdGkiOiI1YmMyNTViNi00YTQ5LTRlMzItODA0ZC02Yzc3ODRlNTNkN2IiLCJpc3MiOiJodHRwczovL2FwaS5sZWFybmluZy1sYXllcnMuZXUvYXV0aC9yZWFsbXMvbWFpbiIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiI5OTMzLTlkM2RhYjUxYWE5MCIsInR5cCI6IkJlYXJlciIsImF6cCI6ImJkZGE3Mzk2LTNmNmQtNGQ4My1hYzIxLTY1YjQwNjlkMGVhYiIsIm5vbmNlIjoiNjM2ZTdhZjVkZmJjNGM5MzlkMmUwYzA1MzAxMDIzNjUiLCJzZXNzaW9uX3N0YXRlIjoiYWUwNTBhYzYtZjg2Mi00Y2I2LWJkYTYtZDM1NDJlMjk0NThmIiwiYWNyIjoiMCIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vMTM3LjIyNi4yMzIuMTc1OjMyMDEwIiwiaHR0cDovL3RlY2g0Y29tcC5kYmlzLnJ3dGgtYWFjaGVuLmRlOjMxMDEwIiwiaHR0cDovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODAiLCJodHRwczovL2ZpbGVzLnRlY2g0Y29tcC5kYmlzLnJ3dGgtYWFjaGVuLmRlIiwiaHR0cDovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6OTA5OCIsImh0dHBzOi8vY2xvdWQxMC5kYmlzLnJ3dGgtYWFjaGVuLmRlOjgwODQiLCJodHRwczovL21vbml0b3IudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiLCJodHRwOi8vMTI3LjAuMC4xOjgwODEiLCJodHRwczovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODA4MCIsImh0dHBzOi8vZ2l0LnRlY2g0Y29tcC5kYmlzLnJ3dGgtYWFjaGVuLmRlIiwiaHR0cDovLzEyNy4wLjAuMTo4MCIsImh0dHA6Ly9sb2NhbGhvc3Q6ODAiLCJodHRwczovL2NhZS1kZXYudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiLCJodHRwOi8vMTI3LjAuMC4xOjgwODAiLCJodHRwOi8vbG9jYWxob3N0OjgwODAiLCJodHRwOi8vbGFzMnBlZXIuZGJpcy5yd3RoLWFhY2hlbi5kZSIsImh0dHBzOi8vbGFzMnBlZXIuZGJpcy5yd3RoLWFhY2hlbi5kZTo5MDk4IiwiaHR0cDovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODA4MCIsImh0dHA6Ly9sb2NhbGhvc3Q6ODA4MSIsImh0dHBzOi8vbGFzMnBlZXIudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiLCJodHRwczovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODAiLCJodHRwOi8vY2xvdWQxMC5kYmlzLnJ3dGgtYWFjaGVuLmRlOjgwODIiLCJodHRwczovL3NiZi1kZXYudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIiwic2lkIjoiYWUwNTBhYzYtZjg2Mi00Y2I2LWJkYTYtZDM1NDJlMjk0NThmIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJNYXJjIEJlbHNjaCIsInByZWZlcnJlZF91c2VybmFtZSI6Im1iZWxzY2giLCJnaXZlbl9uYW1lIjoiTWFyYyIsImZhbWlseV9uYW1lIjoiQmVsc2NoIiwiZW1haWwiOiJtYXJjLmJlbHNjaEByd3RoLWFhY2hlbi5kZSJ9.LMjOFlT-3JWqDPbSymEQKX9sROvosWIAPMRufTocRGy-0DQAuIJS41iSYPO3jyRC2i9HsyGdShoJy9ISocb4F3BiWUQQleuqXQ9zGAVP9i26j9fTH4xJRR8YWIIQp57-f8tx63dA85J9IAJZvaNkLGDcPzq2e5bbCOlCbRyB3KrirA3gtnwspwFF8yl4YHf93bQsugkAtdUACU-Ouh65_dDdTsX7nDmv2PsXC-qOWc52DPmPydOC_PEzP00hI5AkgUnmNLx6YqBT5Yif3jrMUkkzwlzBQDoQGGPIbNOGwosENlDbuaZ7jdqrpexXTWys7fGh6foU7zAApHKSmxqUFw")
				.header("Authorization", "Basic T0lEQ19TVUItOTkzMy05ZDNkYWI1MWFhOTA6OTkzMy05ZDNkYWI1MWFhOTA=")
				.get(Mapping.class);		
		return Response.status(HttpURLConnection.HTTP_OK).entity(response).build();
	}

	private List<LrsStatement> parseToList(String response) {
		List<LrsStatement> statements = new ArrayList<>();
		JSONObject object = new JSONObject(response);
		JSONArray array = object.getJSONArray("edges");
		for (int i = 0; i < array.length(); i++) {
			JSONObject statement = array.getJSONObject(i).getJSONObject("node").getJSONObject("statement");
			LrsStatement stmt = new LrsStatement();
			stmt.setActor(statement.getJSONObject("actor").getString("name"));
			stmt.setVerb(statement.getJSONObject("verb").getJSONObject("display").getString("en-US"));
			stmt.setWhat(statement.getJSONObject("object").getJSONObject("definition").getJSONObject("name")
				.getString("en-US"));
			//stmt.setTimeStamp(statement.getString("timestamp"));
			stmt.setTimeStamp(null);
			statements.add(stmt);
		}
		return statements;
	}
	
	
	private static String retriveGameElement(String path) throws IOException {
		HttpClient gamingClient = new HttpClient();
		gamingClient.setBaseURL("http://localhost/gamification/configurator");
		HashMap<String, String> initHeaders = new HashMap<>();
		initHeaders.put("access-token","");
		initHeaders.put("Authorization", "");
		gamingClient.setToken("");
		initHeaders.put("Content-Type", "text/plain");
		String gameElement = gamingClient.executeGet(path, initHeaders);
		return gameElement;
	}

	

	private static void executeStreak(String who, String did, String what, String configId, String streakId) {
		try {
			String streak = retriveGameElement("/streak/" +configId + "/" + streakId);
			JSONObject jsonStreak = new JSONObject(streak);
			String boundary =  "--32532twtfaweafwsgfaegfawegf442365"; 
			
			HttpClient client = new HttpClient();
			client.setBaseURL("http://localhost:8080/gamification/streaks/");
			client.setToken("");
			HashMap<String, String> gameMap = new HashMap<>();
			gameMap.put("Authorization", "");
			gameMap.put("access-token", "");
			gameMap.put("Content-Type", "multipart/form-data; boundary="+boundary);
			String body = null;
			switch (did) {
			case "created":
				System.out.println("created");
				client.executePost("data", gameMap, body);
				break;
			case "submitted":
				System.out.println("submitted");
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + what);
			}
		} catch (IllegalArgumentException e) {
			System.out.println("Verb " + did + "is not suported for activity " + what);
		}catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	private static void executeGame(String who, String did, String what, String configId, String gameId) {
		try {
			String game = retriveGameElement("/game/"+ configId + "/" + gameId);
			JSONObject jsonGame = new JSONObject(game);
			
			String boundary =  "--32532twtfaweafwsgfaegfawegf4"; 
			
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			builder.setBoundary(boundary);
			

			builder.addPart("gameid", new StringBody(jsonGame.getString("gameId"), ContentType.TEXT_PLAIN));
			builder.addPart("gamedesc", new StringBody(jsonGame.getString("description"), ContentType.TEXT_PLAIN));
			builder.addPart("commtype", new StringBody(jsonGame.getString("communityType"), ContentType.TEXT_PLAIN));
			
			HttpEntity formData = builder.build();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			formData.writeTo(out);
			
			HttpClient client = new HttpClient();
			client.setBaseURL("http://localhost:8080/gamification/games");
			client.setToken("");
			HashMap<String, String> gameMap = new HashMap<>();
			gameMap.put("Authorization", "");
			gameMap.put("access-token", "");
			gameMap.put("Content-Type", "multipart/form-data; boundary="+boundary);
			
			String result = null;
			String memberId = null;
			switch (did) {
			case "created":
				result = client.executePost("/data", gameMap, out);
				System.out.println(result);
				break;
			case "deleted":
				System.out.println("deleted");
				result = client.executeDelete("/data/" + gameId);
				System.out.println(result);
				break;
			case "addedUser":
				System.out.println("addedUser");
				result = client.executePost("/data/" + gameId + "/" + memberId);
				System.out.println(result);
				break;
			case "removedUser":
				System.out.println("removedUser");
				result = client.executeDelete("/data/" + gameId + "/" + memberId);
				System.out.println(result);
				break;
			case "vaildatedUser":
				System.out.println("vaildatedUser");
				result = client.executePost("/validation");
				System.out.println(result);
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + what);
			}
		} catch (IllegalArgumentException e) {
			System.out.println("Verb " + did + "is not suported for activity " + what);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	private static void executeQuest(String who, String did, String what, String configId, String questId) {
		try {
			String quest = retriveGameElement("/quest/" +configId + "/" + questId);
			JSONObject jsonQuest = new JSONObject(quest);
			
			
			JSONObject obj = new JSONObject();
			obj.put("questid", jsonQuest.getString("questId"));
			obj.put("questname", jsonQuest.getString("name"));
			obj.put("questdescription", jsonQuest.getString("description"));
			obj.put("queststatus", jsonQuest.getString("status"));
			obj.put("questpointflag", String.valueOf(jsonQuest.getBoolean("pointFlag")));
			obj.put("questpointvalue", String.valueOf(jsonQuest.getInt("pointValue")));
			obj.put("questquestflag", String.valueOf(jsonQuest.getBoolean("questFlag")));
			obj.put("questidcompleted", jsonQuest.getString("questIdCompleted"));
			obj.put("questactionids", jsonQuest.getJSONArray("actionIds"));
			obj.put("questachievementid", jsonQuest.getString("achievementId"));
			obj.put("questnotificationcheck", String.valueOf(jsonQuest.getBoolean("useNotification")));
			obj.put("questnotificationmessage", jsonQuest.getString("notificationMessage"));
			
			HttpClient client = new HttpClient();
			client.setBaseURL("http://localhost:8080/gamification/quests/");
			client.setToken("");
			HashMap<String, String> gameMap = new HashMap<>();
			gameMap.put("Authorization", "");
			gameMap.put("access-token", "");
			gameMap.put("Content-Type", "application/json");
			
			String result = null;
			//TODO get gameId from?
			String gameId = null;
			switch (did) {
			case "created":
				System.out.println("created");
				result = client.executePost(gameId, gameMap, obj);
				System.out.println(result);
				break;
			case "updated":
				System.out.println("updated");
				result = client.executePut(gameId + "/" + questId, gameMap, obj);
				System.out.println(result);
				break;
			case "deleted":
				System.out.println("deleted");
				result = client.executeDelete(gameId + "/" + questId);
				System.out.println(result);
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + what);
			}
		} catch (IllegalArgumentException e) {
			System.out.println("Verb " + did + "is not suported for activity " + what);
		} catch (IOException e) {
			e.getMessage();
			e.printStackTrace();
		}
	}

	private static void executeAchievement(String who, String did, String what, String configId, String achievementId) {
		try {
			String achievement = retriveGameElement("/achievement/" +configId + "/" + achievementId);
			JSONObject jsonAchievement = new JSONObject(achievement);
			
			String boundary =  "--32532twtfaweafwsgfaegfawegf4"; 
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			builder.setBoundary(boundary);
			
			builder.addPart("achievementid", new StringBody(jsonAchievement.getString("achievementId"), ContentType.TEXT_PLAIN));
			builder.addPart("achievementname", new StringBody(jsonAchievement.getString("name"), ContentType.TEXT_PLAIN));
			builder.addPart("achievementdesc", new StringBody(jsonAchievement.getString("description"), ContentType.TEXT_PLAIN));
			builder.addPart("achievementpointvalue", new StringBody(String.valueOf(jsonAchievement.getInt("pointValue")), ContentType.TEXT_PLAIN));
			builder.addPart("achievementbadgeid", new StringBody(jsonAchievement.getString("badgeId"), ContentType.TEXT_PLAIN));

			builder.addPart("achievementnotificationcheck", new StringBody(String.valueOf(jsonAchievement.getBoolean("useNotification")), ContentType.TEXT_PLAIN));
			builder.addPart("achievementnotificationmessage", new StringBody(jsonAchievement.getString("notificationMessage"), ContentType.TEXT_PLAIN));
			HttpEntity formData = builder.build();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			formData.writeTo(out);
			
			HttpClient client = new HttpClient();
			client.setBaseURL("http://localhost:8080/gamification/achievements/");
			client.setToken("");
			HashMap<String, String> gameMap = new HashMap<>();
			gameMap.put("Authorization", "");
			gameMap.put("access-token", "");
			gameMap.put("Content-Type", "multipart/form-data; boundary="+boundary);
			
			String result = null;
			//TODO get gameId from?
			String gameId = null;
			switch (did) {
			case "created":
				System.out.println("created");
				result = client.executePost(gameId, gameMap, out);
				System.out.println(result);
				break;
			case "updated":
				System.out.println("updated");
				result = client.executePut(gameId + "/" + achievementId, gameMap, out);
				System.out.println(result);
				break;
			case "deleted":
				System.out.println("deleted");
				result = client.executeDelete(gameId + "/" + achievementId);
				System.out.println(result);
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + what);
			}
		} catch (IllegalArgumentException e) {
			System.out.println("Verb " + did + "is not suported for activity " + what);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	private static void executeBadge(String who, String did, String what, String configId, String badgeId) {
		try {
			String badge = retriveGameElement("/badge/" +configId + "/" + badgeId);
			JSONObject jsonBadge = new JSONObject(badge);
			
			File badgeImage = new File("./files/logo.png");
			String boundary =  "----WebKitFormBoundaryuK41JdjQK2kdEBDn"; 
			
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			builder.setBoundary(boundary);
			
			builder.addPart("badgeid", new StringBody(jsonBadge.getString("badgeId"), ContentType.TEXT_PLAIN));
			builder.addPart("badgename", new StringBody(jsonBadge.getString("name"), ContentType.TEXT_PLAIN));
			builder.addPart("badgedesc", new StringBody(jsonBadge.getString("description"), ContentType.TEXT_PLAIN));
			builder.addPart("badgeimageinput", new FileBody(badgeImage, ContentType.create("image/png"), "logo.png"));
			builder.addPart("dev", new StringBody("yes", ContentType.TEXT_PLAIN));

			builder.addPart("badgenotificationcheck", new StringBody(String.valueOf(jsonBadge.getBoolean("useNotification")), ContentType.TEXT_PLAIN));
			builder.addPart("badgenotificationmessage", new StringBody(jsonBadge.getString("notificationMessage"), ContentType.TEXT_PLAIN));
		
			HttpEntity formData = builder.build();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			formData.writeTo(out);
			
			HttpClient client = new HttpClient();
			client.setBaseURL("http://localhost:8080/gamification/badges/");
			client.setToken("");
			HashMap<String, String> gameMap = new HashMap<>();
			gameMap.put("Authorization", "");
			gameMap.put("access-token", "");
			gameMap.put("Content-Type", "multipart/form-data; boundary="+boundary);
			
			//TODO get gameId from?
			String gameId = null;
			String result = null;
			switch (did) {
			case "created":
				System.out.println("created");
				result = client.executePost( gameId, gameMap, out);
				System.out.println(result);
				break;
			case "updated":
				System.out.println("updated");
				result = client.executePut(gameId + "/" + badgeId, gameMap, out);
				System.out.println(result);
				break;
			case "deleted":
				System.out.println("deleted");
				result = client.executeDelete(gameId + "/" + badgeId);
				System.out.println(result);
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + what);
			}
		} catch (IllegalArgumentException e) {
			System.out.println("Verb " + did + "is not suported for activity " + what);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	private static void executeAction(String who, String did, String what, String configId, String actionId) {
		try {
			String action = retriveGameElement("/action/" +configId + "/" + actionId);
			JSONObject jsonAction = new JSONObject(action);
			
			String boundary =  "--32532twtfaweafwsgfaegfawegf4"; 
			
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			builder.setBoundary(boundary);
			
			builder.addPart("actionid", new StringBody(jsonAction.getString("actionId"), ContentType.TEXT_PLAIN));
			builder.addPart("actionname", new StringBody(jsonAction.getString("name") , ContentType.TEXT_PLAIN));
			builder.addPart("actiondesc", new StringBody(jsonAction.getString("description"), ContentType.TEXT_PLAIN));
			builder.addPart("actionpointvalue", new StringBody(String.valueOf(jsonAction.getInt("pointValue")), ContentType.TEXT_PLAIN));
			
			builder.addPart("actionnotificationcheck", new StringBody(String.valueOf(jsonAction.getBoolean("useNotification")), ContentType.TEXT_PLAIN));
			builder.addPart("actionnotificationmessage", new StringBody(jsonAction.getString("notificationMessage"), ContentType.TEXT_PLAIN));
			
			HttpEntity formData = builder.build();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			formData.writeTo(out);
			
			HttpClient client = new HttpClient();
			client.setBaseURL("http://localhost:8080/gamification/actions/");
			client.setToken("");
			HashMap<String, String> gameMap = new HashMap<>();
			gameMap.put("Authorization", "");
			gameMap.put("access-token", "");
			gameMap.put("Content-Type", "multipart/form-data; boundary="+boundary);
			
			//TODO get gameId from?
			String gameId = null;
			String result = null;
			switch (did) {
			case "created":
				System.out.println("created");
				result = client.executePost(gameId, gameMap, out);
				System.out.println(result);
				break;
			case "updated":
				System.out.println("updated");
				result = client.executePut(gameId + "/" + actionId, gameMap, out);
				System.out.println(result);
				break;
			case "deleted":
				System.out.println("deleted");
				result = client.executeDelete( gameId + "/" + actionId);
				System.out.println(result);
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + what);
			}
		} catch (IllegalArgumentException e) {
			System.out.println("Verb " + did + "is not suported for activity " + what);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	private static void executeLevel(String who, String did, String what, String configId, String levelNumber) {
		try {
			String level = retriveGameElement("/level/" +configId + "/" + levelNumber);
			JSONObject jsonLevel = new JSONObject(level);
			
			String boundary =  "--32532twtfaweafwsgfaegfawegf4"; 
			
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			builder.setBoundary(boundary);
			
			builder.addPart("levelnum", new StringBody(Integer.toString(jsonLevel.getInt("levelNumber")), ContentType.TEXT_PLAIN));
			builder.addPart("levelname", new StringBody(jsonLevel.getString("name"), ContentType.TEXT_PLAIN));
			builder.addPart("levelpointvalue", new StringBody(String.valueOf(jsonLevel.getInt("pointValue")), ContentType.TEXT_PLAIN));
			

			builder.addPart("levelnotificationcheck", new StringBody(String.valueOf(jsonLevel.getBoolean("useNotification")), ContentType.TEXT_PLAIN));
			builder.addPart("levelnotificationmessage", new StringBody(jsonLevel.getString("notificationMessage"), ContentType.TEXT_PLAIN));
			
			HttpEntity formData = builder.build();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			formData.writeTo(out);
			
			HttpClient client = new HttpClient();
			client.setBaseURL("http://localhost:8080/gamification/levels/");
			client.setToken("");
			HashMap<String, String> gameMap = new HashMap<>();
			gameMap.put("Authorization", "");
			gameMap.put("access-token", "");
			gameMap.put("Content-Type", "multipart/form-data; boundary="+boundary);
			
			//TODO get gameId from?
			String gameId = null;
			String result = null;
			switch (did) {
			case "created":
				System.out.println("created");
				result = client.executePost(gameId, gameMap, out);
				System.out.println(result);
				break;
			case "updated":
				System.out.println("updated");
				result = client.executePut(gameId + "/" + levelNumber, gameMap, out);
				System.out.println(result);
				break;
			case "deleted":
				System.out.println("deleted");
				result = client.executeDelete(gameId + "/" + levelNumber);
				System.out.println(result);
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + what);
			}
		} catch (IllegalArgumentException e) {
			System.out.println("Verb " + did + "is not suported for activity " + what);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public static class HttpClient {
		private HttpURLConnection connection;
		private String token;
		private String baseURL;

		private void setHeaders(Map<String, String> headers) {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				if (entry.getKey().equals("Authorization")) {
					connection.setRequestProperty(entry.getKey(), getToken());
				} else {
					connection.setRequestProperty(entry.getKey(), entry.getValue());
				}
			}
		}

		private void setBody(Object body) {
			try {
				OutputStream output = connection.getOutputStream();
				output.write(body.toString().getBytes("utf-8"));
				output.flush();
			} catch (Exception e) {
				e.getMessage();
				e.printStackTrace();
			}
		}

		public void disconnect() {
			if (connection != null) {
				connection.disconnect();
			}
		}

		public String getToken() {
			return token;
		}

		public void setToken(String token) {
			this.token = token;
		}

		public String getBaseURL() {
			return baseURL;
		}

		public void setBaseURL(String baseURL) {
			this.baseURL = baseURL;
		}

		public String executePost(String path) throws IOException {
			return executePost(path, null, null);
		}

		public String executePost(String path, Map<String, String> headers, Object body) throws IOException {
			return execute("POST", path, headers, body);
		}

		public String executeGet(String path, Map<String, String> headers) throws IOException {
			return execute("GET", path, headers, null);
		}

		public String executePut(String path) throws IOException {
			return executePut(path, null, null);
		}

		public String executePut(String path, Map<String, String> headers, Object body) throws IOException {
			return execute("PUT", path, headers, body);
		}

		public String executeDelete(String path) throws IOException {
			return execute("DELETE", path, null, null);
		}

		public String execute(String method, String path, Map<String, String> headers, Object body) throws IOException {
			URL url = new URL(getBaseURL() + path);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestMethod(method);
			if (headers != null) {
				setHeaders(headers);
			}
			if (method.equals("POST") && body != null) {
				setBody(body);
			}
			StringBuilder sb = new StringBuilder();
			BufferedReader br = null;
			if (!(connection.getResponseCode() <= 299 && connection.getResponseCode() >= 200)) {
				br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
			} else {
				br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			}
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			return sb.toString();
		}

	}

	
}
