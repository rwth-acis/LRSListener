package i5.las2peer.services.gamification.listener;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.json.JSONArray;
import org.json.JSONObject;

import i5.las2peer.api.Context;
import i5.las2peer.api.logging.MonitoringEvent;

@SuppressWarnings("unused")
public class LrsHandler{
	private Map<String, List<? extends ElementMapping>> map = Collections.synchronizedMap(new HashMap<>());
	private String gamificationUrl;
	private String lrsUrl;
	private String lrsFilter;
	private String configuratorUrl;
	private String configId;
	private String lrsAuth;
	private String l2pAuth;
	private String l2pAccessToken;
	private String timeStamp;


	
	public LrsHandler(String gamificationUrl, String lrsUrl, String lrsFilter, String configuratorUrl, String configId, String lrsAuth, String l2pAuth, String l2pAccessToken) {
		this.gamificationUrl = gamificationUrl;
		this.lrsUrl = lrsUrl;
		this.lrsFilter = lrsFilter;
		this.configuratorUrl = configuratorUrl;
		this.configId = configId;
		this.lrsAuth = lrsAuth;
		this.l2pAuth = l2pAuth;
		this.l2pAccessToken = l2pAccessToken;
		Mapping mapping = null;
		try {
			mapping = getMappingFromConfigurator(getConfigId());
			setMap(mapping);
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "Received and set mapping correctly");
		} catch (Exception e) {
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_16, "Error when setting  mapping");
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_17, e.getMessage());
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_18, mapping.toString());
		}
		try {
			timeStamp = getTimeStampFromDB(this.configId);
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "Received and set timestamp correctly");
		} catch (Exception e) {
			timeStamp = getCurrentTime();
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_16, "Error when setting  timestamp");
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_17, e.getMessage());
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_18, mapping.toString());
		}
	}
	
	public void handle() {
		//Listener can only work with present mapping, else sleep and wait for new mapping
		if(getMap()!= null) {
			List<LrsStatement>statements = null;
			try {
				statements = retriveStatements();
				for (LrsStatement statement : statements) {
					String result = null;
					try {
						result = executeGamification(statement);
						setTimeStamp(getCurrentTime());
						adjustFilterWithTimeStamp();
						Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "Statement executed successfully " + statement.toString());
						Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15, "Result of executed statement " + result);
					}
					catch (Exception e) {
						e.printStackTrace();
						Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_10, "Statement executed unsuccessfully " + statement.toString());
						Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_10, "The following error occured" + e.getMessage());
						Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_12, "Result of executed statement " + result);
					}
				}
			}
			catch (Exception e) {
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_16, "Error during operation");
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_17, e.getMessage());
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_18, statements.toString());
			}
			finally {
				writeTimeStampToDB(getConfigId());
			}
		}
	}

	/**
	 * @param configId Configuration Id for Mapping to be retrieved
	 * @return Mapping obtained from ListenerConfigurator for Configuration
	 */
	private Mapping getMappingFromConfigurator(String configId) {
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget target = client.target(getConfiguratorUrl());
		Invocation invocation = target
				.path("/mapping/" + configId)
				.request()
				.header("access-token", getL2pAccessToken())
				.header("Authorization", getL2pAuth())
				.buildGet();
		Mapping response = invocation.invoke(Mapping.class);
		return response;
	}

	/**
	 * 
	 * @return List of LrsStatements for preset filter set in target.path
	 */
	private List<LrsStatement> retriveStatements() {
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget target = client.target(getLrsUrl());
		String response = target
				.path(getLrsFilter())
				.request()
				.header("Authorization", getLrsAuth())
				.get(String.class)
				.toString();
		List<LrsStatement> statements = null;
		if (response!= null) {
			statements = parseToList(response);
		}
		return statements;
	}
	
	/**
	 * 
	 * @param response JSONString of LRS Statements applicable to filter set in target.path
	 * @return
	 */
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
	
	/**
	 * 
	 * @param statement LrsStatement to execute Gamification Framework Functions with
	 * @return Result of the specialized execute Methods
	 */
	@SuppressWarnings("unchecked")
	private String executeGamification(LrsStatement statement) {
		List<GameMapping> games = (List<GameMapping>) getMap().get("games");
		List<QuestMapping> quests = (List<QuestMapping>) getMap().get("quests");
		List<AchievementMapping> achievements = (List<AchievementMapping>) getMap().get("achievements");
		List<BadgeMapping> badges = (List<BadgeMapping>) getMap().get("badges");
		List<ActionMapping> actions = (List<ActionMapping>) getMap().get("actions");
		List<LevelMapping> levels = (List<LevelMapping>) getMap().get("levels");
		List<StreakMapping> streaks = (List<StreakMapping>) getMap().get("streaks");
		//check to which GamificationElementType the Statement activity bonds to
		//then trigger the according Gamification Framework Function
		for (StreakMapping streak : streaks) {
			if (streak.getListenTo().equals(statement.getWhat())) {
				return executeStreak(statement, streak.getGameId(), streak.getStreakId());
			}
		}
		for (QuestMapping quest : quests) {
			if (quest.getListenTo().equals(statement.getWhat())) {
				return executeQuest(statement, quest.getGameId(), quest.getQuestId());
			}
		}
		for (ActionMapping action : actions) {
			if (action.getListenTo().equals(statement.getWhat())) {
				return executeAction(statement, action.getGameId(), action.getActionId());
			}
		}
		for (LevelMapping level : levels) {
			if (level.getListenTo().equals(statement.getWhat())) {
				return executeLevel(statement, level.getGameId(), level.getLevelNumber());
			}
		}
		for (AchievementMapping achivement : achievements) {
			if (achivement.getListenTo().equals(statement.getWhat())) {
				return executeAchievement(statement, achivement.getGameId(), achivement.getAchievementId());
			}
		}
		for (GameMapping game : games) {
			if (game.getListenTo().equals(statement.getWhat())) {
				return executeGame(statement, game.getGameId());
			}
		}
		for (BadgeMapping badge : badges) {
			if (badge.getListenTo().equals(statement.getWhat())) {
				return executeBadge(statement, badge.getGameId(), badge.getBadgeId());
			}
		}
		throw new IllegalStateException("Could not execute " + statement.getWhat() + ". No such activity in configuration " + getConfigId());
	}
	
	/**
	 * 
	 * @param statement The statement to be executed
	 * @param gameId The gameId the streak is assigned on
	 * @param streakId The id of the streak
	 * @return Result of the executed Gamification request
	 */
	private String executeStreak(LrsStatement statement, String gameId, String streakId) {
		//TODO
		String result = null;
		try {
			String streak = retriveGameElement("/gamification/configurator/streak/" + getConfigId() + "/" + streakId);
			JSONObject jsonStreak = new JSONObject(streak);
			String boundary =  "--32532twtfaweafwsgfaegfawegf442365"; 
			byte[] output = new byte[0];
			switch (statement.getVerb()) {
			case "created":
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationPostRequest("/streaks/" + gameId, output, "multipart/form-data; boundary="+boundary);
				return result;
			case "updated":
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationPutRequest("/streaks/" + gameId + "/" + streakId, output, "multipart/form-data; boundary="+boundary);
				return result;
			case "deleted":
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_16, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationDeleteRequest("/streaks/" + gameId + "/" + streakId);
				return result;
			default:
				throw new IllegalStateException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_18, e.getMessage());
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_19, "Verb " + statement.getVerb() + "is not suported for activity " + statement.getWhat());
			return result;
		} 
		catch (IOException e) {
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11, "Error when triggering Gamification Framework funtions");
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_12, e.getMessage());
			return result;
		}
	}
	
	/**
	 * 
	 * @param statement
	 * @param gameId
	 * @param questId
	 * @return
	 */
	private String executeQuest(LrsStatement statement, String gameId, String questId) {
		String result = null;
		try {
			String quest = retriveGameElement("/gamification/configurator/quest/" + getConfigId() + "/" + questId);
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
			
			byte[] output = obj.toString().getBytes("UTF-8");
			
			switch (statement.getVerb()) {
			case "created":
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationPostRequest("/quests/" + gameId, output, "application/json");
				return result;
			case "updated":
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationPutRequest("/quests/" + gameId + "/" + questId, output, "application/json");
				return result;
			case "deleted":
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_16, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationDeleteRequest("/quests/" + gameId + "/" + questId);
				return result;
			default:
				throw new IllegalStateException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_18, e.getMessage());
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_19, "Verb " + statement.getVerb() + "is not suported for activity " + statement.getWhat());
			return result;
		} 
		catch (IOException e) {
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11, "Error when triggering Gamification Framework funtions");
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_12, e.getMessage());
			return result;
		}
	}
	
	/**
	 * 
	 * @param statement
	 * @param gameId
	 * @param actionId
	 * @return
	 */
	private String executeAction(LrsStatement statement, String gameId, String actionId) {
		String result = null;
		try {
			String action = retriveGameElement("/gamification/configurator/action/" + getConfigId() + "/" + actionId);
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
			byte[] output = out.toString().getBytes("UTF-8");

			
			switch (statement.getVerb()) {
			case "created":
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationPostRequest("/actions/" + gameId, output, "multipart/form-data; boundary="+boundary);
				return result;
			case "updated":
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationPutRequest("/actions/" + gameId + "/" + actionId, output, "multipart/form-data; boundary="+boundary);
				return result;
			case "deleted":
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_16, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationDeleteRequest("/actions/" + gameId + "/" + actionId);
				return result;
			default:
				throw new IllegalStateException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_18, e.getMessage());
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_19, "Verb " + statement.getVerb() + "is not suported for activity " + statement.getWhat());
			return result;
		} 
		catch (IOException e) {
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11, "Error when triggering Gamification Framework funtions");
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_12, e.getMessage());
			return result;
		}
	}

	/**
	 * 
	 * @param statement
	 * @param gameId
	 * @param levelNumber
	 * @return
	 */
	private String executeLevel(LrsStatement statement, String gameId, int levelNumber) {
		String result = null;
		try {
			String level = retriveGameElement("/gamification/configurator/level/" + getConfigId() + "/" + levelNumber);
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
			byte[] output = out.toString().getBytes("UTF-8");
			
			
			switch (statement.getVerb()) {
			case "created":
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationPostRequest("/levels/" + gameId, output, "multipart/form-data; boundary="+boundary);
				return result;
			case "updated":
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationPutRequest("/levels/" + gameId + "/" + levelNumber, output, "multipart/form-data; boundary="+boundary);
				return result;
			case "deleted":
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_16, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationDeleteRequest("/levels/" + gameId + "/" + levelNumber);
				return result;
			default:
				throw new IllegalStateException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_18, e.getMessage());
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_19, "Verb " + statement.getVerb() + "is not suported for activity " + statement.getWhat());
			return result;
		} 
		catch (IOException e) {
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11, "Error when triggering Gamification Framework funtions");
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_12, e.getMessage());
			return result;
		}
	}
	
	/**
	 * 
	 * @param statement
	 * @param gameId
	 * @param achievementId
	 * @return
	 */
	private String executeAchievement(LrsStatement statement, String gameId, String achievementId) {
		String result = null;
		try {
			String achievement = retriveGameElement("/gamification/configurator/achievement/" + getConfigId() + "/" + achievementId);
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
			byte[] output = out.toString().getBytes("UTF-8");
			
			switch (statement.getVerb()) {
			case "created":
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationPostRequest("/achievements/" + gameId, output, "multipart/form-data; boundary="+boundary);
				return result;
			case "updated":
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationPutRequest("/achievements/" + gameId + "/" + achievementId, output, "multipart/form-data; boundary="+boundary);
				return result;
			case "deleted":
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_16, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationDeleteRequest("/achievements/" + gameId + "/" + achievementId);
				return result;
			default:
				throw new IllegalStateException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_18, e.getMessage());
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_19, "Verb " + statement.getVerb() + "is not suported for activity " + statement.getWhat());
			return result;
		} 
		catch (IOException e) {
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11, "Error when triggering Gamification Framework funtions");
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_12, e.getMessage());
			return result;
		}
	}
	
	/**
	 * 
	 * @param statement
	 * @param gameId
	 * @return
	 */
	private String executeGame(LrsStatement statement, String gameId) {
		String result = null;
		try {
			String game = retriveGameElement("/gamification/configurator/game/"+ getConfigId() + "/" + gameId);
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
			byte[] output = out.toString().getBytes("UTF-8");

			switch (statement.getVerb()) {
			case "created":
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationPostRequest("/games/data", output, "multipart/form-data; boundary="+boundary);
				return result;
			case "updated":
				//This should never happen
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationPutRequest("/games/data/" + gameId, output, "multipart/form-data; boundary="+boundary);
				return result;
			case "deleted":
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_16, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationDeleteRequest("/games/data/" + gameId);
				return result;
			default:
				throw new IllegalStateException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_18, e.getMessage());
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_19, "Verb " + statement.getVerb() + "is not suported for activity " + statement.getWhat());
			return result;
		} 
		catch (IOException e) {
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11, "Error when triggering Gamification Framework funtions");
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_12, e.getMessage());
			return result;
		}
	}
	
	/**
	 * 
	 * @param statement
	 * @param gameId
	 * @param badgeId
	 * @return
	 */
	private String executeBadge(LrsStatement statement, String gameId, String badgeId) {
		String result = null;
		try {
			String badge = retriveGameElement("/badge/" + getConfigId() + "/" + badgeId);
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
			byte[] output = out.toString().getBytes("UTF-8");
			
			switch (statement.getVerb()) {
			case "created":
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationPostRequest("/badges/" + gameId, output, "multipart/form-data; boundary="+boundary);
				return result;
			case "updated":
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationPutRequest("/badges/" + gameId + "/" + badgeId, output, "multipart/form-data; boundary="+boundary);
				return result;
			case "deleted":
				Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_16, statement.getActor() + " " + statement.getVerb() + " " + statement.getWhat());
				result = gamificationDeleteRequest("/badges/" + gameId + "/" + badgeId);
				return result;
			default:
				throw new IllegalStateException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_18, e.getMessage());
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_19, "Verb " + statement.getVerb() + "is not suported for activity " + statement.getWhat());
			return result;
		} 
		catch (IOException e) {
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11, "Error when triggering Gamification Framework funtions");
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_12, e.getMessage());
			return result;
		}
	}
	
	/**
	 * 
	 * @param path path to retrieve the GamificationElement from the configurator
	 * @return The GamificationElement as JSONString
	 */
	private String retriveGameElement(String path) {
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget target = client.target(getConfiguratorUrl());
		String result = target
				.path(path)
				.request()
				.header("access-token", getL2pAccessToken())
				.header("Authorization", getL2pAuth())
				.get(String.class);
		return result;
	}
	
	/**
	 * 
	 * @param path The path to trigger the corresponding gamification function. Path will be appended to gamificationUrl
	 * @param body The body to be send. Usually Gamification Element
	 * @return Response in String format
	 * @throws IOException 
	 */
	private String gamificationPostRequest(String path, byte[] body, String contentType) throws IOException {
		URL url = null;
		try {
			url = new URL(getGamificationUrl() + path);
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "Sending to " + url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11, "Could not send, because of malformed url " + url);
		}
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", contentType);
		connection.setRequestProperty("Authroization", getL2pAuth());
		connection.setRequestProperty("access-token", getL2pAccessToken());
		connection.setRequestMethod("POST");
		OutputStream wr = connection.getOutputStream();
		wr.write(body, 0, body.length);
		Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "Send POST request to " +url);
		BufferedReader br = null;
		if (connection.getResponseCode() >= 100 && connection.getResponseCode() < 400) {
			br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
		} else {
			br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"));
		}
		Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "Reading response from POST request to endpoint " + url);
		StringBuilder response = new StringBuilder();
		String responseLine = null;
		while ((responseLine = br.readLine()) != null) {
			response.append(responseLine.trim());
		}
		Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "Response code is " + connection.getResponseCode());
		Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "Response is " + response.toString());
		return response.toString();
	}
	
	/**
	 * 
	 * @param path The path to trigger the corresponding gamification function. Path will be appended to gamificationUrl
	 * @param output 
	 * @param body The body to be send. Usually Gamification Element
	 * @return Response in String format
	 * @throws IOException 
	 */
	private String gamificationPutRequest(String path, byte[] body, String boundary) throws IOException {
		URL url = null;
		try {
			url = new URL(getGamificationUrl() + path);
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "Sending to " + url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11, "Could not send, because of malformed url " + url);
		}
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
		connection.setRequestProperty("Authroization", getL2pAuth());
		connection.setRequestProperty("access-token", getL2pAccessToken());
		connection.setRequestMethod("PUT");
		OutputStream wr = connection.getOutputStream();
		wr.write(body, 0, body.length);
		Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "Send PUT request to " +url);
		BufferedReader br = null;
		if (connection.getResponseCode() >= 100 && connection.getResponseCode() < 400) {
			br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
		} else {
			br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"));
		}
		Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "Reading response from PUT request to endpoint " + url);
		StringBuilder response = new StringBuilder();
		String responseLine = null;
		while ((responseLine = br.readLine()) != null) {
			response.append(responseLine.trim());
		}
		Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "Response code is " + connection.getResponseCode());
		Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "Response is " + response.toString());
		return response.toString();
	}
	
	/**
	 * 
	 * @param path The path to trigger the corresponding gamification function. Path will be appended to gamificationUrl
	 * @return Response in String format
	 * @throws IOException 
	 */
	private String gamificationDeleteRequest(String path) throws IOException {
		URL url = null;
		try {
			url = new URL(getGamificationUrl() + path);
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "Sending to " + url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11, "Could not send, because of malformed url " + url);
		}
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setRequestProperty("Authroization", getL2pAuth());
		connection.setRequestProperty("access-token", getL2pAccessToken());
		connection.setRequestMethod("DELETE");
		OutputStream wr = connection.getOutputStream();
		Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "Send DELETE request to " +url);
		BufferedReader br = null;
		if (connection.getResponseCode() >= 100 && connection.getResponseCode() < 400) {
			br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
		} else {
			br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"));
		}
		Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "Reading response from DELETE request to endpoint " + url);
		StringBuilder response = new StringBuilder();
		String responseLine = null;
		while ((responseLine = br.readLine()) != null) {
			response.append(responseLine.trim());
		}
		Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "Response code is " + connection.getResponseCode());
		Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "Response is " + response.toString());
		return response.toString();
	}
	
	/**
	 * 
	 * @return get the current Time in parses it into correct format
	 */
	private String getCurrentTime() {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void adjustFilterWithTimeStamp() {
		setLrsFilter(encodeTimeStamp());
	}
	
	private String encodeTimeStamp() {
		// TODO Auto-generated method stub
		//encode timestamp like in lrs request required
		//replace timestamp with regex
		//url encode and return it
		return null;
	}

	/**
	 * 
	 * @param configId associated with this Listener
	 * @return timestamp for the configId
	 */
	private String getTimeStampFromDB(String configId) {
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget target = client.target(getConfiguratorUrl());
		Invocation invocation = target
				.path("/timestamp/" + configId)
				.request()
				.header("access-token", getL2pAccessToken())
				.header("Authorization", getL2pAuth())
				.buildGet();
		String response = invocation.invoke(String.class);
		return response;
	}
	
	/**
	 * 
	 * @param configId for which the timestamp has tp be set
	 */
	private void writeTimeStampToDB(String configId) {
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget target = client.target(getConfiguratorUrl());
		Invocation invocation = target
				.path("/timestamp/" + configId)
				.request()
				.header("access-token", getL2pAccessToken())
				.header("Authorization", getL2pAuth())
				.buildPost(Entity.entity(getTimeStamp(), MediaType.TEXT_PLAIN));
	}

	/**
	 * @return the map
	 */
	public Map<String, List<? extends ElementMapping>> getMap() {
		return map;
	}
	
	/**
	 * @param mapping the Mapping to set
	 */
	public void setMap(Mapping mapping) {
		Map<String,List<? extends ElementMapping>> map = new HashMap<>();
		map.put("games",mapping.getGameMapping());
		map.put("quests", mapping.getQuestMapping());
		map.put("achievements", mapping.getAchievementMapping());
		map.put("badges", mapping.getBadgeMapping());
		map.put("actions", mapping.getActionMapping());
		map.put("levels", mapping.getLevelMapping());
		map.put("streaks", mapping.getStreakMapping());
		this.map = Collections.synchronizedMap(map);
	}

	/**
	 * @return the gamificationUrl
	 */
	public String getGamificationUrl() {
		return gamificationUrl;
	}

	/**
	 * @param gamificationUrl the gamificationUrl to set
	 */
	public void setGamificationUrl(String gamificationUrl) {
		this.gamificationUrl = gamificationUrl;
	}

	/**
	 * @return the lrsUrl
	 */
	public String getLrsUrl() {
		return lrsUrl;
	}

	/**
	 * @param lrsUrl the lrsUrl to set
	 */
	public void setLrsUrl(String lrsUrl) {
		this.lrsUrl = lrsUrl;
	}

	/**
	 * @return the lrsFilter
	 */
	public String getLrsFilter() {
		return lrsFilter;
	}

	/**
	 * @param lrsFilter the lrsFilter to set
	 */
	public void setLrsFilter(String lrsFilter) {
		this.lrsFilter = lrsFilter;
	}

	/**
	 * @return the configuratorUrl
	 */
	public String getConfiguratorUrl() {
		return configuratorUrl;
	}

	/**
	 * @param configuratorUrl the configuratorUrl to set
	 */
	public void setConfiguratorUrl(String configuratorUrl) {
		this.configuratorUrl = configuratorUrl;
	}

	/**
	 * @return the configId
	 */
	public String getConfigId() {
		return configId;
	}

	/**
	 * @param configId the configId to set
	 */
	public void setConfigId(String configId) {
		this.configId = configId;
	}

	/**
	 * @return the lrsAuth
	 */
	public String getLrsAuth() {
		return lrsAuth;
	}

	/**
	 * @param lrsAuth the lrsAuth to set
	 */
	public void setLrsAuth(String lrsAuth) {
		this.lrsAuth = lrsAuth;
	}

	/**
	 * @return the l2pAuth
	 */
	public String getL2pAuth() {
		return l2pAuth;
	}

	/**
	 * @param l2pAuth the l2pAuth to set
	 */
	public void setL2pAuth(String l2pAuth) {
		this.l2pAuth = l2pAuth;
	}

	/**
	 * @return the l2pAccessToken
	 */
	public String getL2pAccessToken() {
		//TODO
		return "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJoZTJ6NVRzbEM1M3VPQXZxNmFWckplT2I0ZUx5TUxUam9IT3dIdTBiRmFJIn0.eyJleHAiOjE2MzUzNDYxMzgsImlhdCI6MTYzNTM0MjUzOCwiYXV0aF90aW1lIjoxNjM1MzQyNTE5LCJqdGkiOiI1YmMyNTViNi00YTQ5LTRlMzItODA0ZC02Yzc3ODRlNTNkN2IiLCJpc3MiOiJodHRwczovL2FwaS5sZWFybmluZy1sYXllcnMuZXUvYXV0aC9yZWFsbXMvbWFpbiIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiI5OTMzLTlkM2RhYjUxYWE5MCIsInR5cCI6IkJlYXJlciIsImF6cCI6ImJkZGE3Mzk2LTNmNmQtNGQ4My1hYzIxLTY1YjQwNjlkMGVhYiIsIm5vbmNlIjoiNjM2ZTdhZjVkZmJjNGM5MzlkMmUwYzA1MzAxMDIzNjUiLCJzZXNzaW9uX3N0YXRlIjoiYWUwNTBhYzYtZjg2Mi00Y2I2LWJkYTYtZDM1NDJlMjk0NThmIiwiYWNyIjoiMCIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vMTM3LjIyNi4yMzIuMTc1OjMyMDEwIiwiaHR0cDovL3RlY2g0Y29tcC5kYmlzLnJ3dGgtYWFjaGVuLmRlOjMxMDEwIiwiaHR0cDovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODAiLCJodHRwczovL2ZpbGVzLnRlY2g0Y29tcC5kYmlzLnJ3dGgtYWFjaGVuLmRlIiwiaHR0cDovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6OTA5OCIsImh0dHBzOi8vY2xvdWQxMC5kYmlzLnJ3dGgtYWFjaGVuLmRlOjgwODQiLCJodHRwczovL21vbml0b3IudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiLCJodHRwOi8vMTI3LjAuMC4xOjgwODEiLCJodHRwczovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODA4MCIsImh0dHBzOi8vZ2l0LnRlY2g0Y29tcC5kYmlzLnJ3dGgtYWFjaGVuLmRlIiwiaHR0cDovLzEyNy4wLjAuMTo4MCIsImh0dHA6Ly9sb2NhbGhvc3Q6ODAiLCJodHRwczovL2NhZS1kZXYudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiLCJodHRwOi8vMTI3LjAuMC4xOjgwODAiLCJodHRwOi8vbG9jYWxob3N0OjgwODAiLCJodHRwOi8vbGFzMnBlZXIuZGJpcy5yd3RoLWFhY2hlbi5kZSIsImh0dHBzOi8vbGFzMnBlZXIuZGJpcy5yd3RoLWFhY2hlbi5kZTo5MDk4IiwiaHR0cDovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODA4MCIsImh0dHA6Ly9sb2NhbGhvc3Q6ODA4MSIsImh0dHBzOi8vbGFzMnBlZXIudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiLCJodHRwczovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODAiLCJodHRwOi8vY2xvdWQxMC5kYmlzLnJ3dGgtYWFjaGVuLmRlOjgwODIiLCJodHRwczovL3NiZi1kZXYudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIiwic2lkIjoiYWUwNTBhYzYtZjg2Mi00Y2I2LWJkYTYtZDM1NDJlMjk0NThmIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJNYXJjIEJlbHNjaCIsInByZWZlcnJlZF91c2VybmFtZSI6Im1iZWxzY2giLCJnaXZlbl9uYW1lIjoiTWFyYyIsImZhbWlseV9uYW1lIjoiQmVsc2NoIiwiZW1haWwiOiJtYXJjLmJlbHNjaEByd3RoLWFhY2hlbi5kZSJ9.LMjOFlT-3JWqDPbSymEQKX9sROvosWIAPMRufTocRGy-0DQAuIJS41iSYPO3jyRC2i9HsyGdShoJy9ISocb4F3BiWUQQleuqXQ9zGAVP9i26j9fTH4xJRR8YWIIQp57-f8tx63dA85J9IAJZvaNkLGDcPzq2e5bbCOlCbRyB3KrirA3gtnwspwFF8yl4YHf93bQsugkAtdUACU-Ouh65_dDdTsX7nDmv2PsXC-qOWc52DPmPydOC_PEzP00hI5AkgUnmNLx6YqBT5Yif3jrMUkkzwlzBQDoQGGPIbNOGwosENlDbuaZ7jdqrpexXTWys7fGh6foU7zAApHKSmxqUFw";
	}

	/**
	 * @param l2pAccessToken the l2pAccessToken to set
	 */
	public void setL2pAccessToken(String l2pAccessToken) {
		this.l2pAccessToken = l2pAccessToken;
	}

	/**
	 * @return the timeStamp
	 */
	public String getTimeStamp() {
		return timeStamp;
	}

	/**
	 * @param timeStamp the timeStamp to set
	 */
	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}
}
