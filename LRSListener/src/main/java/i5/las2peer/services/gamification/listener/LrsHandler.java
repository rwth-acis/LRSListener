package i5.las2peer.services.gamification.listener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import i5.las2peer.api.Context;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.services.gamification.listener.LRSListener.HttpClient;
@SuppressWarnings("unused")
public class LrsHandler implements Runnable{
	private Map<String, List<? extends ElementMapping>> map = Collections.synchronizedMap(new HashMap<>());
	private String configId;
	
	@Override
	public void run() {
		//Initially retrieve mapping once from configuration. If mapping changes, it will be updated by another thread
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
		//keep this thread alive
		while (true) {
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
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
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
		WebTarget target = client.target("http://localhost:8080/gamification/configurator");
		Mapping response = target
				.path("/mapping/" + configId)
				.request()
				.header("access-token", "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJoZTJ6NVRzbEM1M3VPQXZxNmFWckplT2I0ZUx5TUxUam9IT3dIdTBiRmFJIn0.eyJleHAiOjE2MzUzNDYxMzgsImlhdCI6MTYzNTM0MjUzOCwiYXV0aF90aW1lIjoxNjM1MzQyNTE5LCJqdGkiOiI1YmMyNTViNi00YTQ5LTRlMzItODA0ZC02Yzc3ODRlNTNkN2IiLCJpc3MiOiJodHRwczovL2FwaS5sZWFybmluZy1sYXllcnMuZXUvYXV0aC9yZWFsbXMvbWFpbiIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiI5OTMzLTlkM2RhYjUxYWE5MCIsInR5cCI6IkJlYXJlciIsImF6cCI6ImJkZGE3Mzk2LTNmNmQtNGQ4My1hYzIxLTY1YjQwNjlkMGVhYiIsIm5vbmNlIjoiNjM2ZTdhZjVkZmJjNGM5MzlkMmUwYzA1MzAxMDIzNjUiLCJzZXNzaW9uX3N0YXRlIjoiYWUwNTBhYzYtZjg2Mi00Y2I2LWJkYTYtZDM1NDJlMjk0NThmIiwiYWNyIjoiMCIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vMTM3LjIyNi4yMzIuMTc1OjMyMDEwIiwiaHR0cDovL3RlY2g0Y29tcC5kYmlzLnJ3dGgtYWFjaGVuLmRlOjMxMDEwIiwiaHR0cDovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODAiLCJodHRwczovL2ZpbGVzLnRlY2g0Y29tcC5kYmlzLnJ3dGgtYWFjaGVuLmRlIiwiaHR0cDovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6OTA5OCIsImh0dHBzOi8vY2xvdWQxMC5kYmlzLnJ3dGgtYWFjaGVuLmRlOjgwODQiLCJodHRwczovL21vbml0b3IudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiLCJodHRwOi8vMTI3LjAuMC4xOjgwODEiLCJodHRwczovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODA4MCIsImh0dHBzOi8vZ2l0LnRlY2g0Y29tcC5kYmlzLnJ3dGgtYWFjaGVuLmRlIiwiaHR0cDovLzEyNy4wLjAuMTo4MCIsImh0dHA6Ly9sb2NhbGhvc3Q6ODAiLCJodHRwczovL2NhZS1kZXYudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiLCJodHRwOi8vMTI3LjAuMC4xOjgwODAiLCJodHRwOi8vbG9jYWxob3N0OjgwODAiLCJodHRwOi8vbGFzMnBlZXIuZGJpcy5yd3RoLWFhY2hlbi5kZSIsImh0dHBzOi8vbGFzMnBlZXIuZGJpcy5yd3RoLWFhY2hlbi5kZTo5MDk4IiwiaHR0cDovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODA4MCIsImh0dHA6Ly9sb2NhbGhvc3Q6ODA4MSIsImh0dHBzOi8vbGFzMnBlZXIudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiLCJodHRwczovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODAiLCJodHRwOi8vY2xvdWQxMC5kYmlzLnJ3dGgtYWFjaGVuLmRlOjgwODIiLCJodHRwczovL3NiZi1kZXYudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIiwic2lkIjoiYWUwNTBhYzYtZjg2Mi00Y2I2LWJkYTYtZDM1NDJlMjk0NThmIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJNYXJjIEJlbHNjaCIsInByZWZlcnJlZF91c2VybmFtZSI6Im1iZWxzY2giLCJnaXZlbl9uYW1lIjoiTWFyYyIsImZhbWlseV9uYW1lIjoiQmVsc2NoIiwiZW1haWwiOiJtYXJjLmJlbHNjaEByd3RoLWFhY2hlbi5kZSJ9.LMjOFlT-3JWqDPbSymEQKX9sROvosWIAPMRufTocRGy-0DQAuIJS41iSYPO3jyRC2i9HsyGdShoJy9ISocb4F3BiWUQQleuqXQ9zGAVP9i26j9fTH4xJRR8YWIIQp57-f8tx63dA85J9IAJZvaNkLGDcPzq2e5bbCOlCbRyB3KrirA3gtnwspwFF8yl4YHf93bQsugkAtdUACU-Ouh65_dDdTsX7nDmv2PsXC-qOWc52DPmPydOC_PEzP00hI5AkgUnmNLx6YqBT5Yif3jrMUkkzwlzBQDoQGGPIbNOGwosENlDbuaZ7jdqrpexXTWys7fGh6foU7zAApHKSmxqUFw")
				.header("Authorization", "Basic T0lEQ19TVUItOTkzMy05ZDNkYWI1MWFhOTA6OTkzMy05ZDNkYWI1MWFhOTA=")
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
	
	
	private String executeBadge(LrsStatement statement, String gameId, String badgeId) {
		// TODO Auto-generated method stub
		return null;
	}

	private String executeGame(LrsStatement statement, String gameId) {
		// TODO Auto-generated method stub
		return null;
	}

	private String executeAchievement(LrsStatement statement, String gameId, String achievementId) {
		// TODO Auto-generated method stub
		return null;
	}

	private String executeLevel(LrsStatement statement, String gameId, int levelNumber) {
		// TODO Auto-generated method stub
		return null;
	}

	private String executeAction(LrsStatement statement, String gameId, String actionId) {
		// TODO Auto-generated method stub
		return null;
	}

	private String executeQuest(LrsStatement statement, String gameId, String questId) {
		// TODO Auto-generated method stub
		return null;
	}

	private String executeStreak(LrsStatement statement, String gameId, String streakId) {
		try {
			String streak = retriveGameElement("/streak/" + getConfigId() + "/" + streakId);
			JSONObject jsonStreak = new JSONObject(streak);
			String boundary =  "--32532twtfaweafwsgfaegfawegf442365"; 
			
			HttpClient client = new HttpClient();
			client.setBaseURL("http://localhost:8080/gamification/streaks/");
			client.setToken("");
			HashMap<String, String> gameMap = new HashMap<>();
			gameMap.put("Authorization", "");
			gameMap.put("access-token", "");
			gameMap.put("Content-Type", "multipart/form-data; boundary="+boundary);
			switch (statement.getVerb()) {
			case "created":
				System.out.println("created");
				return null;
			case "submitted":
				System.out.println("submitted");
				return null;
			default:
				throw new IllegalArgumentException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalArgumentException e) {
			System.out.println("Verb " + statement.getVerb() + "is not suported for activity " + statement.getWhat());
			return null;
		}
	}

	private String retriveGameElement(String path) {
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget target = client.target("https://lrs.tech4comp.dbis.rwth-aachen.de/api/connection/statement");
		String response = target
				.path(path)
				.request()
				.header("Authorization", "Basic NjM2NmZiZDgzNDU5M2M3MDU5ODU3ZTg4ODQwYjMyZGRmMTY1NjQwMzo0MGUxZjRlZmRlNDFlM2JlZTFiMWJlOTIxNDQ1ODc5OWEwMWZhNDAy")
				.header("", "")
				.get(String.class)
				.toString();
		return response;
	}

	public Map<String, List<? extends ElementMapping>> getMap() {
		return map;
	}
	
	
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
}
