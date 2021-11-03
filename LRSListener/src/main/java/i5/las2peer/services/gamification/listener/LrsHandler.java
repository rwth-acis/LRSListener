package i5.las2peer.services.gamification.listener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
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
	
	public LrsHandler(String gamificationUrl, String lrsUrl, String lrsFilter, String configuratorUrl, String configId, String lrsAuth, String l2pAuth, String l2pAccessToken) {
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
		Mapping response = target
				.path("/mapping/" + configId)
				.request()
				.header("access-token", getL2pAccessToken())
				.header("Authorization", getL2pAuth())
				.get(Mapping.class);
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
		String result = null;
		try {
			String streak = retriveGameElement("/gamification/configurator/streak/" + getConfigId() + "/" + streakId);
			JSONObject jsonStreak = new JSONObject(streak);
			String boundary =  "--32532twtfaweafwsgfaegfawegf442365"; 
			
//			gameMap.put("Content-Type", "multipart/form-data; boundary="+boundary);
			switch (statement.getVerb()) {
			case "created":
				System.out.println("created");
				//result = gamificationPostRequest();
				return result;
			case "submitted":
				System.out.println("submitted");
				return result;
			default:
				throw new IllegalArgumentException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalArgumentException e) {
			System.out.println("Verb " + statement.getVerb() + "is not suported for activity " + statement.getWhat());
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
			
			
			switch (statement.getVerb()) {
			case "created":
				System.out.println("created");
				return result;
			case "updated":
				System.out.println("updated");
				return result;
			case "deleted":
				System.out.println("deleted");
				return result;
			default:
				throw new IllegalArgumentException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalArgumentException e) {
			System.out.println("Verb " + statement.getVerb() + "is not suported for activity " + statement.getWhat());
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
			

			
			switch (statement.getVerb()) {
			case "created":
				System.out.println("created");
				result = gamificationPostRequest("path", out.toString());
				return result;
			case "updated":
				System.out.println("updated");
				result = gamificationPutRequest("path", out.toString());
				return result;
			case "deleted":
				System.out.println("deleted");
				result = gamificationDeleteRequest("path");
				return result;
			default:
				throw new IllegalArgumentException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalArgumentException e) {
			System.out.println("Verb " + statement.getVerb() + "is not suported for activity " + statement.getWhat());
			return result;
		} catch (IOException e) {
			e.printStackTrace();
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
			
			
			switch (statement.getVerb()) {
			case "created":
				System.out.println("created");
				return result;
			case "updated":
				System.out.println("updated");
				return result;
			case "deleted":
				System.out.println("deleted");
				return result;
			default:
				throw new IllegalArgumentException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalArgumentException e) {
			System.out.println("Verb " + statement.getVerb() + "is not suported for activity " + statement.getWhat());
			return result;
		} catch (IOException e) {
			e.printStackTrace();
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
			
			
			switch (statement.getVerb()) {
			case "created":
				System.out.println("created");
				return result;
			case "updated":
				System.out.println("updated");
				return result;
			case "deleted":
				System.out.println("deleted");
				return result;
			default:
				throw new IllegalArgumentException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalArgumentException e) {
			System.out.println("Verb " + statement.getVerb() + "is not suported for activity " + statement.getWhat());
			return result;
		} catch (IOException e) {
			e.printStackTrace();
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
			
		
			String memberId = null;
			switch (statement.getVerb()) {
			case "created":
				System.out.println("created");
				return result;
			case "updated":
				System.out.println("updated");
				return result;
			case "deleted":
				System.out.println("deleted");
				return result;
			default:
				throw new IllegalArgumentException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalArgumentException e) {
			System.out.println("Verb " + statement.getVerb() + "is not suported for activity " + statement.getWhat());
			return result;
		} catch (IOException e) {
			e.printStackTrace();
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
			String badge = retriveGameElement("/gamification/configurator/badge/" + getConfigId() + "/" + badgeId);
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
//			FileDataBodyPart filepart = new FileDataBodyPart("badgeimageinput", new File("logo.png"));
//			FormDataMultiPart mulitpart = new FormDataMultiPart()
//					.field("badgeid", jsonBadge.getString("badgeId"))
//					.field("badgename", jsonBadge.getString("name"))
//					.field("badgedesc", jsonBadge.getString("description"))
//					.field("dev" ,"yes")
//					.field("badgenotificationcheck", String.valueOf(jsonBadge.getBoolean("useNotification")))
//					.field("badgenotificationmessage", jsonBadge.getString("notificationMessage"));
//					//.bodyPart(filepart);
//			
			
			switch (statement.getVerb()) {
			case "created":
				System.out.println("created");
				return result;
			case "updated":
				System.out.println("updated");
				return result;
			case "deleted":
				System.out.println("deleted");
				return result;
			default:
				throw new IllegalArgumentException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalArgumentException e) {
			System.out.println("Verb " + statement.getVerb() + "is not suported for activity " + statement.getWhat());
			return result;
		} catch (IOException e) {
			e.printStackTrace();
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
	 */
	private String gamificationPostRequest(String path, String body) {
		Client client = ClientBuilder.newClient();
		WebTarget target = client.target(getGamificationUrl());
		Response response = target
				.path(path)
				.request()
				.post(Entity.entity(body, MediaType.MULTIPART_FORM_DATA_TYPE));
				
		return response.readEntity(String.class);
	}
	
	/**
	 * 
	 * @param path The path to trigger the corresponding gamification function. Path will be appended to gamificationUrl
	 * @param body The body to be send. Usually Gamification Element
	 * @return Response in String format
	 */
	private String gamificationPutRequest(String path, String body) {
		Client client = ClientBuilder.newClient();
		WebTarget target = client.target(getGamificationUrl());
		Response response = target
				.path(path)
				.request()
				.put(Entity.entity(body, MediaType.MULTIPART_FORM_DATA_TYPE));
				
		return response.readEntity(String.class);
	}
	
	/**
	 * 
	 * @param path The path to trigger the corresponding gamification function. Path will be appended to gamificationUrl
	 * @return Response in String format
	 */
	private String gamificationDeleteRequest(String path) {
		Client client = ClientBuilder.newClient();
		WebTarget target = client.target(getGamificationUrl());
		String result = target
				.path(path)
				.request()
				.delete(String.class);
		return result;
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
		return l2pAccessToken;
	}

	/**
	 * @param l2pAccessToken the l2pAccessToken to set
	 */
	public void setL2pAccessToken(String l2pAccessToken) {
		this.l2pAccessToken = l2pAccessToken;
	}
}
