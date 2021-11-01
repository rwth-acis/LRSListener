package i5.las2peer.services.gamification.listener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
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
import org.json.JSONArray;
import org.json.JSONObject;

import i5.las2peer.api.Service;
import i5.las2peer.api.ServiceException;
import i5.las2peer.restMapper.RESTService;


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
	
	@POST
	@Path("/notify")
	@Consumes(MediaType.APPLICATION_JSON)
	public void notifed() {
		Map<String,String> map = new HashMap<>();
		handler.add(map);
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	@Override
	public void run() {
		//TODO Thread management
		while (!Thread.currentThread().isInterrupted()) {
			try {
				startListen();
			} catch (Exception e) {
				System.out.println("Exception handeled in run method");
				e.printStackTrace();
			}
		}
	}
	/**
	 * Starts the application and contains the listening loop.
	 * 
	 * @throws SQLException in case of sql error
	 */
	public static void startListen() throws SQLException {
		
		//the url of this listener
		String thisURL = "";
		//Get config first
		HttpClient initClient = new HttpClient();
		initClient.setBaseURL("http://localhost/gamification/configurator");
		// For Authorization header
		initClient.setToken("");
		HashMap<String, String> initHeaders = new HashMap<>();
		//las2peer applications need access token
		initHeaders.put("access-token","");
		String configId ="";
		String mapping= null;
		try {
			mapping = initClient.executeGet("/mapping" + configId, initHeaders);
		} catch (IOException e2) {
			e2.printStackTrace();
			System.out.println("Failed to retrieve mapping");
		}
		
		
		//subscribe to configuration
		try {
			initHeaders.put("Content-Type", "application/json; charset=utf-8");
			JSONObject object = new JSONObject();
			object.put("url", thisURL);
			initClient.executePost("/register/" + configId, initHeaders, object);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.out.println("Unable to register as observer for configId" + configId);
		}
		
		//create boolean to reload config
		boolean newConfig = false;
			try {
				//listen to changes ??? how
				newConfig = false;
			}catch (Exception e) {
				e.printStackTrace();
				System.out.println("Failed to check mapping status");
			}
			if(newConfig) {
				//when Config has been updated, reload configuration ->time check necessary??
				try {
					mapping = initClient.executeGet("/mapping" + configId, initHeaders);
				} catch (IOException e2) {
					e2.printStackTrace();
					System.out.println("Failed to retrieve mapping");
				}
			}
			
			//proceed with normal listening with received configuration
			HttpClient lrsClient = new HttpClient();
			lrsClient.setToken(
					"Basic NjM2NmZiZDgzNDU5M2M3MDU5ODU3ZTg4ODQwYjMyZGRmMTY1NjQwMzo0MGUxZjRlZmRlNDFlM2JlZTFiMWJlOTIxNDQ1ODc5OWEwMWZhNDAy\"");
			lrsClient.setBaseURL("https://lrs.tech4comp.dbis.rwth-aachen.de/api/connection/statement");
			while (!newConfig) {
				try {
					// Get xAPI statements
					HashMap<String, String> lrsHeaders = new HashMap<>();
					lrsHeaders.put("Content-Type", "application/json; charset=utf-8");
					lrsHeaders.put("Authorization", "");
					String result = lrsClient.executeGet(
							"?filter=%7B%22%24and%22%3A%5B%7B%22%24comment%22%3A%22%7B%5C%22criterionLabel%5C%22%3A%5C%22A%5C%22%2C%5C%22criteriaPath%5C%22%3A%5B%5C%22person%5C%22%5D%7D%22%2C%22person._id%22%3A%7B%22%24in%22%3A%5B%7B%22%24oid%22%3A%225db02311c5dcd9003d904ba4%22%7D%5D%7D%7D%5D%7D&sort=%7B%22timestamp%22%3A-1%2C%22_id%22%3A1%7D",
							lrsHeaders);
					lrsClient.disconnect();
					if (result != null) {
						JSONObject[] relevantStatements = parseToJson(result);
						// This is the correct usage, but for testing, we only use one statement
//						for (JSONObject jsonObject : relevantStatements) {
						// Test code begins here
						JSONObject jsonObject = relevantStatements[0];
						// Test Code ends here
						String[] gameDetails = getGameDetails(jsonObject);
						System.out.println(gameDetails[0] + " " + gameDetails[1] + " " + gameDetails[2]);
						executeGamification(gameDetails, mapping);
//						}
						try {
							Thread.sleep(10000);
						} catch (InterruptedException e) {
							System.out.println("Interrupted sleep. How rude!");
						}
					}
					System.out.println("Still running");
				} catch (Exception e) {
					System.out.println("Uppsi got an error");
					System.err.println(e.getMessage());
					System.err.println(e.getStackTrace());
				} finally {
					lrsClient.disconnect();
					initClient.disconnect();
			}
		}
	}

	/**
	 * 
	 * @param jsonObject
	 * @return values to node.statement.actor/verb/object
	 */
	private static String[] getGameDetails(JSONObject jsonObject) {
		String[] result = new String[3];
		JSONObject statement = jsonObject.getJSONObject("node").getJSONObject("statement");
		result[0] = statement.getJSONObject("actor").getString("name");
		result[1] = statement.getJSONObject("verb").getJSONObject("display").getString("en-US");
		result[2] = statement.getJSONObject("object").getJSONObject("definition").getJSONObject("name")
				.getString("en-US");
		return result;
	}

	/**
	 * 
	 * @param line
	 * @return JSONObject Array, containing JSONObjects, each starting with
	 *         {"cursor":...
	 */
	private static JSONObject[] parseToJson(String line) {
		JSONObject object = new JSONObject(line);
		JSONArray array = object.getJSONArray("edges");
		JSONObject[] objectArray = new JSONObject[array.length()];
		for (int i = 0; i < array.length(); i++) {
			objectArray[i] = array.getJSONObject(i);
		}
		return objectArray;
	}
	
	private static Map<String, ?> extractGamificationElement(String mapping, String gamificationElement) {
		JSONArray array = new JSONArray(mapping);
		Map<String, ?> result = new HashMap<>();
		switch (gamificationElement) {
		case "game":
			result = array.getJSONObject(0).getJSONObject("game").toMap();
			break;
		case "quest":
			result = array.getJSONObject(1).getJSONObject("quest").toMap();
			break;
		case "achievement":
			result = array.getJSONObject(2).getJSONObject("achievement").toMap();
			break;
		case "badge":
			result = array.getJSONObject(3).getJSONObject("badge").toMap();
			break;
		case "action":
			result = array.getJSONObject(4).getJSONObject("action").toMap();
			break;
		case "level":
			result = array.getJSONObject(5).getJSONObject("level").toMap();
			break;
		case "streak":
			result = array.getJSONObject(6).getJSONObject("streak").toMap();
			break;
		}
		return result;
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

	/**
	 * @param gamificationDetails contains actors, verb and activity of xAPI statement
	 * @param mapping contains activities to listen for
	 * @return URL address as String to fetch all xAPI statements related to
	 *         Gamification Framework activities
	 */

	private static void executeGamification(String[] gamificationDetails, String mapping) {
		String who = gamificationDetails[0];
		String did = gamificationDetails[1];
		String what = gamificationDetails[2];
		Map<String, ?> listenGame = null;
		Map<String, ?> listenQuest = null;
		Map<String, ?> listenAchievement = null;
		Map<String, ?> listenBadge =null;
		Map<String, ?> listenAction = null;
		Map<String, ?> listenLevel = null;
		Map<String, ?> listenStreak = null;
		if (mapping != null) {
			listenGame = extractGamificationElement(mapping, "game");
			listenQuest = extractGamificationElement(mapping, "quest");
			listenAchievement = extractGamificationElement(mapping, "achievement");
			listenBadge = extractGamificationElement(mapping, "badge");
			listenAction = extractGamificationElement(mapping, "action");
			listenLevel = extractGamificationElement(mapping, "level");
			listenStreak = extractGamificationElement(mapping, "streak");
		}
		if (who != null && did != null && what != null) {
			try {
				if (listenGame.containsValue(what)) {
					executeGame(who, did, what, "configId", "elementId");
				} 
				else if(listenQuest.containsValue(what)){
					executeQuest(who, did, what, "configId", "elementId");
				}
				else if(listenAchievement.containsValue(what)){
					executeAchievement(who, did, what, "configId", "elementId");
				}
				else if(listenBadge.containsValue(what)){
					executeBadge(who, did, what, "configId", "elementId");
				}
				else if(listenAction.containsValue(what)){
					executeAction(who, did, what, "configId", "elementId");
				}
				else if(listenLevel.containsValue(what)){
					executeLevel(who, did, what, "configId", "elementId");
				}
				else if(listenStreak.containsValue(what)){
					executeStreak(who, did, what, "configId", "elementId");
				}
				else {
					throw new IllegalArgumentException("Unexpected value: " + what);
				}
			}
			catch (IllegalArgumentException e) {
				e.printStackTrace();
				System.out.println("Activity " + what + " is not supported");
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
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
