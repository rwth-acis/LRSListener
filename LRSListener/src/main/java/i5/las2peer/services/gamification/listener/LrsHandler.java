package i5.las2peer.services.gamification.listener;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.glassfish.jersey.client.ClientConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import i5.las2peer.api.Context;
import i5.las2peer.api.logging.MonitoringEvent;

@SuppressWarnings("unused")
public class LrsHandler {
	private Map<String, List<? extends ElementMapping>> map = Collections.synchronizedMap(new HashMap<>());
	private String gamificationUrl;
	private String lrsUrl;
	private String lrsFilter;
	private String listenerUrl;
	private String configuratorUrl;
	private String configId;
	private String lrsAuth;
	private String l2pAuth;
	private String l2pAccessToken;
	private String lastCheckedDay;
	private String lastCheckedTime;

	public LrsHandler(String gamificationUrl, String lrsUrl, String listenerUrl, String configuratorUrl,
			String configId, String lrsAuth, String l2pAuth, String l2pAccessToken) {
		this.gamificationUrl = gamificationUrl;
		this.lrsUrl = lrsUrl;
		this.listenerUrl = listenerUrl;
		this.configuratorUrl = configuratorUrl;
		this.configId = configId;
		this.lrsAuth = lrsAuth;
		this.l2pAuth = l2pAuth;
		this.l2pAccessToken = l2pAccessToken;
		initTimeStamps();
		initFilter();
		Mapping mapping = null;
		try {
			mapping = getMappingFromConfigurator(getConfigId());
			setMap(mapping);
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
			// "Received and set mapping correctly");
		} catch (Exception e) {
			e.printStackTrace();
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_16,
			// "Error when setting mapping");
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_17,
			// e.getMessage());
		}
		try {
			registerAsObserver(getConfigId());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * initializes both timestamps, by retrieving from DB In case of failure the
	 * current time is taken
	 */
	private void initTimeStamps() {
		try {
			JSONObject timeStamps = new JSONObject(getTimeStampFromDB(this.configId));
			String day = timeStamps.getString("timestamp");
			String time = timeStamps.getString("laststatement");
			setLastCheckedDay(day);
			setLastCheckedTime(time);
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
			// "Received and set timestamp correctly");
		} catch (Exception e) {
			String currentDay = getCurrentDate();
			setLastCheckedDay(currentDay);
			setLastCheckedTime("00:00");
			e.printStackTrace();
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_16,
			// "Error when setting timestamp. Using current time " + timeStamp);
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_17,
			// e.getMessage());
		}
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
		Invocation invocation = target.path("/timestamp/" + configId).request()
				.header("access-token", getL2pAccessToken()).header("Authorization", getL2pAuth()).buildGet();
		String response = invocation.invoke(String.class);
		return response;
	}

	/**
	 * 
	 * @param configId for which the timestamp has tp be set
	 */
	private void writeTimeStampToDB(String configId) {
		JSONObject timeStamps = new JSONObject();
		timeStamps.put("timestamp", getLastCheckedDay());
		timeStamps.put("laststatement", getLastCheckedTime());
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget target = client.target(getConfiguratorUrl());
		Invocation invocation = target.path("/timestamp/" + configId).request()
				.header("access-token", getL2pAccessToken()).header("Authorization", getL2pAuth())
				.buildPost(Entity.entity(timeStamps, MediaType.TEXT_PLAIN));
		String result = invocation.invoke(String.class);
	}

	/**
	 * 
	 * @return get the current day in parses it into correct format
	 */
	private String getCurrentDate() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String current = formatter.format(date);
		return current;
	}

	/**
	 * filtering just by timestamp, sorting is time decsending by LRS default
	 */
	private void initFilter() {
		adjustFilterWithTimeStamp();
	}

	private void adjustFilterWithTimeStamp() {
		setLrsFilter(encodeTimeStamp());
	}

	/**
	 * Filter to get all statements from that day
	 * 
	 * @return URL encoded LRS Filter query
	 */
	private String encodeTimeStamp() {
		String filter = "?filter=%7B%22%24and%22%3A%5B%7B%22timestamp%22%3A%7B%22%24gte%22%3A%7B%22%24dte%22%3A%22"
				+ getLastCheckedDay()
				+ "T00%3A00%2B01%3A00%22%7D%7D%2C%22%24comment%22%3A%22%7B%5C%22criterionLabel%5C%22%3A%5C%22A%5C%22%2C%5C%22criteriaPath%5C%22%3A%5B%5C%22timestamp%5C%22%5D%7D%22%7D%5D%7D&sort=%7B%22timestamp%22%3A-1%2C%22_id%22%3A1%7D";
		return filter;
	}

	/**
	 * Key in JSON object must be "url", value is the url to be notified on
	 * 
	 * @param configId the config to register to
	 * @throws IOException
	 */
	private void registerAsObserver(String configId) throws IOException {
		JSONObject body = new JSONObject();
		body.put("url", getListenerUrl() + "/gamification/listener/notify");
		String output = body.toString();
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget target = client.target(getConfiguratorUrl());
		Invocation invocation = target.path("/register/" + configId).request()
				.header("access-token", getL2pAccessToken()).header("Authorization", getL2pAuth())
				.buildPost(Entity.entity(output, MediaType.TEXT_PLAIN));
		String response = invocation.invoke(String.class);
	}

	/**
	 * Method,that solves the main use,case It retirives xAPI statements from a LRS
	 * and executes Gamification Framework functions, based on a configuration
	 * mapping
	 */
	public void handle() {
		// Listener can only work with present mapping, else sleep and wait for new
		// mapping
		if (getMap() != null) {
			List<LrsStatement> statements = null;
			try {
				statements = retriveStatements();
				for (LrsStatement statement : statements) {
					String result = null;
					try {
						if (compareStatementTimestamp(statement.getTimeStamp())) {
							result = executeGamification(statement);
							updateTimes(statement);
							adjustFilterWithTimeStamp();
							// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
							// "Statement executed successfully " + statement.toString());
							// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15,
							// "Result of executed statement " + result);
						}
					} catch (Exception e) {
						e.printStackTrace();
						// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_10,
						// "Statement executed unsuccessfully " + statement.toString());
						// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_10,
						// "The following error occured" + e.getMessage());
						// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_12,
						// "Result of executed statement " + result);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_16,
				// "Error during operation");
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_17,
				// e.getMessage());
			} finally {
				writeTimeStampToDB(getConfigId());
			}
		}
	}

	private void updateTimes(LrsStatement statement) {
		String statementTime = statement.getTimeStamp();
		String[] times = statementTime.split("T", 2);
		setLastCheckedDay(times[0]);
		setLastCheckedTime(times[1]);
	}

	/**
	 * @param configId Configuration Id for Mapping to be retrieved
	 * @return Mapping obtained from ListenerConfigurator for Configuration
	 */
	private Mapping getMappingFromConfigurator(String configId) {
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget target = client.target(getConfiguratorUrl());
		Invocation invocation = target.path("/mapping/" + configId).request()
				.header("access-token", getL2pAccessToken()).header("Authorization", getL2pAuth()).buildGet();
		Mapping response = invocation.invoke(Mapping.class);
		return response;
	}

	/**
	 * 
	 * @return List of LrsStatements for preset filter set in target.path
	 * @throws IOException
	 */
	private List<LrsStatement> retriveStatements() throws IOException {
//		ClientConfig clientConfig = new ClientConfig();
//		Client client = ClientBuilder.newClient(clientConfig);
//		WebTarget target = client.target(getLrsUrl());
//		String response = target
//				.path(getLrsFilter())
//				.request()
//				.header("Authorization", getLrsAuth())
//				.get(String.class)
//				.toString();
		URL url = new URL(getLrsUrl() + getLrsFilter());
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Authorization", getLrsAuth());
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
		List<LrsStatement> statements = null;
		if (response != null) {
			statements = parseToList(response);
		}
		return statements;
	}

	/**
	 * 
	 * @param response JSONString of LRS Statements applicable to filter set in
	 *                 target.path
	 * @return
	 */
	private List<LrsStatement> parseToList(String response) {
		List<LrsStatement> statements = new ArrayList<>();
		JSONObject object = new JSONObject(response);
		JSONArray array = object.getJSONArray("edges");
		// also sort from eldest to newest
		for (int i = array.length() - 1; i >= 0; i--) {
			try {
				JSONObject statement = array.getJSONObject(i).getJSONObject("node").getJSONObject("statement");
				LrsStatement stmt = new LrsStatement();
				stmt.setActor(statement.getJSONObject("actor").getJSONObject("account").getString("name"));
				stmt.setVerb(statement.getJSONObject("verb").getJSONObject("display").getString("en-US"));
				stmt.setWhat(statement.getJSONObject("object").getJSONObject("definition").getJSONObject("name")
						.getString("en-US"));
				String stamp = statement.getString("timestamp");
				String subString = stamp.substring(0, 16);
				stmt.setTimeStamp(statement.getString("timestamp").substring(0, 16));
				statements.add(stmt);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return statements;
	}

	/**
	 * 
	 * @param statementTimeStamp
	 * @return true, when statement.timestamp is newer, than lastTimeStamp
	 * @throws ParseException
	 */
	private boolean compareStatementTimestamp(String statementTimeStamp) throws ParseException {
		String before = getLastCheckedDay() + " " + getLastCheckedTime();
		String[] times = statementTimeStamp.split("T", 2);
		String now = times[0] + " " + times[1];
		try {
			LocalDateTime statementTime = LocalDateTime.parse(now, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
			LocalDateTime lastChecked = LocalDateTime.parse(before, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
			if (lastChecked.isBefore(statementTime) || lastChecked.isEqual(statementTime)) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 
	 * @param statement LrsStatement to execute Gamification Framework Functions
	 *                  with
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
		// check to which GamificationElementType the Statement activity bonds to
		// then trigger the according Gamification Framework Function
		Pattern p = null;
		String toMatch = "";
		String pattern ="";
		for (StreakMapping streak : streaks) {
			pattern = streak.getListenTo();
			p = Pattern.compile(".*"+pattern+".*");
			toMatch = statement.getWhat();
			Matcher m = p.matcher(toMatch);
			if (m.matches()) {
				return executeStreak(statement, streak.getGameId(), streak.getStreakId());
			}
		}
		for (QuestMapping quest : quests) {
			pattern = quest.getListenTo();
			p = Pattern.compile(".*"+pattern+".*");
			toMatch = statement.getWhat();
			Matcher m = p.matcher(toMatch);
			if (m.matches()) {
				return executeQuest(statement, quest.getGameId(), quest.getQuestId());
			}
		}
		for (ActionMapping action : actions) {
			pattern = action.getListenTo();
			p = Pattern.compile(".*"+pattern+".*");
			toMatch = statement.getWhat();
			Matcher m = p.matcher(toMatch);
			if (m.matches()) {
				return executeAction(statement, action.getGameId(), action.getActionId());
			}
		}
		for (LevelMapping level : levels) {
			pattern = level.getListenTo();
			p = Pattern.compile(".*"+pattern+".*");
			toMatch = statement.getWhat();
			Matcher m = p.matcher(toMatch);
			if (m.matches()) {
				return executeLevel(statement, level.getGameId(), level.getLevelNumber());
			}
		}
		for (AchievementMapping achivement : achievements) {
			pattern = achivement.getListenTo();
			p = Pattern.compile(".*"+pattern+".*");
			toMatch = statement.getWhat();
			Matcher m = p.matcher(toMatch);
			if (m.matches()) {
				return executeAchievement(statement, achivement.getGameId(), achivement.getAchievementId());
			}
		}
		for (GameMapping game : games) {
			pattern = game.getListenTo();
			p = Pattern.compile(".*"+pattern+".*");
			toMatch = statement.getWhat();
			Matcher m = p.matcher(toMatch);
			if (m.matches()) {
				return executeGame(statement, game.getGameId());
			}
		}
		for (BadgeMapping badge : badges) {
			pattern = badge.getListenTo();
			p = Pattern.compile(".*"+pattern+".*");
			toMatch = statement.getWhat();
			Matcher m = p.matcher(toMatch);
			if (m.matches()) {
				return executeBadge(statement, badge.getGameId(), badge.getBadgeId());
			}
		}
		return "Unsupported";
	}

	/**
	 * 
	 * @param statement The statement to be executed
	 * @param gameId    The gameId the streak is assigned on
	 * @param streakId  The id of the streak
	 * @return Result of the executed Gamification request
	 */
	private String executeStreak(LrsStatement statement, String gameId, String streakId) {
		// TODO
		String result = null;
		try {
			String streak = retriveGameElement("/gamification/configurator/streak/" + getConfigId() + "/" + streakId);
			JSONObject jsonStreak = new JSONObject(streak);
			String boundary = "--32532twtfaweafwsgfaegfawegf442365";
			byte[] output = new byte[0];
			switch (statement.getVerb()) {
			case "created":
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationPostRequest("/streaks/" + gameId, output,
						"multipart/form-data; boundary=" + boundary);
				return result;
			case "updated":
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationPutRequest("/streaks/" + gameId + "/" + streakId, output,
						"multipart/form-data; boundary=" + boundary);
				return result;
			case "deleted":
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_16,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationDeleteRequest("/streaks/" + gameId + "/" + streakId);
				return result;
			default:
				throw new IllegalStateException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_18,
			// e.getMessage());
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_19,
			// "Verb " + statement.getVerb() + "is not suported for activity " +
			// statement.getWhat());
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11,
			// "Error when triggering Gamification Framework funtions");
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_12,
			// e.getMessage());
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
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationPostRequest("/quests/" + gameId, output, "application/json");
				return result;
			case "updated":
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationPutRequest("/quests/" + gameId + "/" + questId, output, "application/json");
				return result;
			case "deleted":
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_16,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationDeleteRequest("/quests/" + gameId + "/" + questId);
				return result;
			default:
				throw new IllegalStateException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_18,
			// e.getMessage());
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_19,
			// "Verb " + statement.getVerb() + "is not suported for activity " +
			// statement.getWhat());
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11,
			// "Error when triggering Gamification Framework funtions");
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_12,
			// e.getMessage());
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
		byte[] output = new byte[0];
		String boundary = "";
		try {
			String action = retriveGameElement("/action/" + getConfigId() + "/" + actionId);
			JSONObject jsonAction = new JSONObject(action);

			boundary = "--32532twtfaweafwsgfaegfawegf4";

			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			builder.setBoundary(boundary);

			builder.addPart("actionid", new StringBody(jsonAction.getString("actionId"), ContentType.TEXT_PLAIN));
			builder.addPart("actionname", new StringBody(jsonAction.getString("name"), ContentType.TEXT_PLAIN));
			builder.addPart("actiondesc", new StringBody(jsonAction.getString("description"), ContentType.TEXT_PLAIN));
			builder.addPart("actionpointvalue",
					new StringBody(String.valueOf(jsonAction.getInt("pointValue")), ContentType.TEXT_PLAIN));

			builder.addPart("actionnotificationcheck",
					new StringBody(String.valueOf(jsonAction.getBoolean("useNotification")), ContentType.TEXT_PLAIN));
			builder.addPart("actionnotificationmessage",
					new StringBody(jsonAction.getString("notificationMessage"), ContentType.TEXT_PLAIN));

			HttpEntity formData = builder.build();
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			formData.writeTo(out);
			output = out.toString().getBytes("UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {

			switch (statement.getVerb()) {
			case "replied":
				result = gamificationPostRequest(
						"/visualization/actions/" + gameId + "/" + actionId + "/" + statement.getActor(), new byte[0],
						"application/json");
				return result;
			case "created":
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationPostRequest("/actions/" + gameId, output,
						"multipart/form-data; boundary=" + boundary);
				return result;
			case "updated":
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationPutRequest("/actions/" + gameId + "/" + actionId, output,
						"multipart/form-data; boundary=" + boundary);
				return result;
			case "deleted":
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_16,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationDeleteRequest("/actions/" + gameId + "/" + actionId);
				return result;
			default:
				throw new IllegalStateException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_18,
			// e.getMessage());
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_19,
			// "Verb " + statement.getVerb() + "is not suported for activity " +
			// statement.getWhat());
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11,
			// "Error when triggering Gamification Framework funtions");
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_12,
			// e.getMessage());
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

			String boundary = "--32532twtfaweafwsgfaegfawegf4";

			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			builder.setBoundary(boundary);

			builder.addPart("levelnum",
					new StringBody(Integer.toString(jsonLevel.getInt("levelNumber")), ContentType.TEXT_PLAIN));
			builder.addPart("levelname", new StringBody(jsonLevel.getString("name"), ContentType.TEXT_PLAIN));
			builder.addPart("levelpointvalue",
					new StringBody(String.valueOf(jsonLevel.getInt("pointValue")), ContentType.TEXT_PLAIN));

			builder.addPart("levelnotificationcheck",
					new StringBody(String.valueOf(jsonLevel.getBoolean("useNotification")), ContentType.TEXT_PLAIN));
			builder.addPart("levelnotificationmessage",
					new StringBody(jsonLevel.getString("notificationMessage"), ContentType.TEXT_PLAIN));

			HttpEntity formData = builder.build();
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			formData.writeTo(out);
			byte[] output = out.toString().getBytes("UTF-8");

			switch (statement.getVerb()) {
			case "created":
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationPostRequest("/levels/" + gameId, output,
						"multipart/form-data; boundary=" + boundary);
				return result;
			case "updated":
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationPutRequest("/levels/" + gameId + "/" + levelNumber, output,
						"multipart/form-data; boundary=" + boundary);
				return result;
			case "deleted":
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_16,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationDeleteRequest("/levels/" + gameId + "/" + levelNumber);
				return result;
			default:
				throw new IllegalStateException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_18,
			// e.getMessage());
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_19,
			// "Verb " + statement.getVerb() + "is not suported for activity " +
			// statement.getWhat());
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11,
			// "Error when triggering Gamification Framework funtions");
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_12,
			// e.getMessage());
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
			String achievement = retriveGameElement("/achievement/" + getConfigId() + "/" + achievementId);
			JSONObject jsonAchievement = new JSONObject(achievement);

			String boundary = "--32532twtfaweafwsgfaegfawegf4";
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			builder.setBoundary(boundary);

			builder.addPart("achievementid",
					new StringBody(jsonAchievement.getString("achievementId"), ContentType.TEXT_PLAIN));
			builder.addPart("achievementname",
					new StringBody(jsonAchievement.getString("name"), ContentType.TEXT_PLAIN));
			builder.addPart("achievementdesc",
					new StringBody(jsonAchievement.getString("description"), ContentType.TEXT_PLAIN));
			builder.addPart("achievementpointvalue",
					new StringBody(String.valueOf(jsonAchievement.getInt("pointValue")), ContentType.TEXT_PLAIN));
			builder.addPart("achievementbadgeid",
					new StringBody(jsonAchievement.getString("badgeId"), ContentType.TEXT_PLAIN));

			builder.addPart("achievementnotificationcheck", new StringBody(
					String.valueOf(jsonAchievement.getBoolean("useNotification")), ContentType.TEXT_PLAIN));
			builder.addPart("achievementnotificationmessage",
					new StringBody(jsonAchievement.getString("notificationMessage"), ContentType.TEXT_PLAIN));
			HttpEntity formData = builder.build();
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			formData.writeTo(out);
			byte[] output = out.toString().getBytes("UTF-8");

			switch (statement.getVerb()) {
			case "created":
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationPostRequest("/achievements/" + gameId, output,
						"multipart/form-data; boundary=" + boundary);
				return result;
			case "updated":
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationPutRequest("/achievements/" + gameId + "/" + achievementId, output,
						"multipart/form-data; boundary=" + boundary);
				return result;
			case "deleted":
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_16,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationDeleteRequest("/achievements/" + gameId + "/" + achievementId);
				return result;
			default:
				throw new IllegalStateException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_18,
			// e.getMessage());
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_19,
			// "Verb " + statement.getVerb() + "is not suported for activity " +
			// statement.getWhat());
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11,
			// "Error when triggering Gamification Framework funtions");
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_12,
			// e.getMessage());
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
			String game = retriveGameElement("/gamification/configurator/game/" + getConfigId() + "/" + gameId);
			JSONObject jsonGame = new JSONObject(game);

			String boundary = "--32532twtfaweafwsgfaegfawegf4";

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
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationPostRequest("/games/data", output, "multipart/form-data; boundary=" + boundary);
				return result;
			case "updated":
				// This should never happen
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationPutRequest("/games/data/" + gameId, output,
						"multipart/form-data; boundary=" + boundary);
				return result;
			case "deleted":
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_16,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationDeleteRequest("/games/data/" + gameId);
				return result;
			default:
				throw new IllegalStateException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_18,
			// e.getMessage());
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_19,
			// "Verb " + statement.getVerb() + "is not suported for activity " +
			// statement.getWhat());
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11,
			// "Error when triggering Gamification Framework funtions");
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_12,
			// e.getMessage());
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
			String boundary = "----WebKitFormBoundaryuK41JdjQK2kdEBDn";

			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			builder.setBoundary(boundary);

			builder.addPart("badgeid", new StringBody(jsonBadge.getString("badgeId"), ContentType.TEXT_PLAIN));
			builder.addPart("badgename", new StringBody(jsonBadge.getString("name"), ContentType.TEXT_PLAIN));
			builder.addPart("badgedesc", new StringBody(jsonBadge.getString("description"), ContentType.TEXT_PLAIN));
			builder.addPart("badgeimageinput", new FileBody(badgeImage, ContentType.create("image/png"), "logo.png"));
			builder.addPart("dev", new StringBody("yes", ContentType.TEXT_PLAIN));

			builder.addPart("badgenotificationcheck",
					new StringBody(String.valueOf(jsonBadge.getBoolean("useNotification")), ContentType.TEXT_PLAIN));
			builder.addPart("badgenotificationmessage",
					new StringBody(jsonBadge.getString("notificationMessage"), ContentType.TEXT_PLAIN));

			HttpEntity formData = builder.build();
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			formData.writeTo(out);
			byte[] output = out.toString().getBytes("UTF-8");

			switch (statement.getVerb()) {
			case "created":
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationPostRequest("/badges/" + gameId, output,
						"multipart/form-data; boundary=" + boundary);
				return result;
			case "updated":
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationPutRequest("/badges/" + gameId + "/" + badgeId, output,
						"multipart/form-data; boundary=" + boundary);
				return result;
			case "deleted":
				// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_16,
				// statement.getActor() + " " + statement.getVerb() + " " +
				// statement.getWhat());
				result = gamificationDeleteRequest("/badges/" + gameId + "/" + badgeId);
				return result;
			default:
				throw new IllegalStateException("Unexpected value: " + statement.getVerb());
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_18,
			// e.getMessage());
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_19,
			// "Verb " + statement.getVerb() + "is not suported for activity " +
			// statement.getWhat());
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11,
			// "Error when triggering Gamification Framework funtions");
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_12,
			// e.getMessage());
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
		String result = target.path(path).request().header("access-token", getL2pAccessToken())
				.header("Authorization", getL2pAuth()).get(String.class);
		return result;
	}

	/**
	 * 
	 * @param path The path to trigger the corresponding gamification function. Path
	 *             will be appended to gamificationUrl
	 * @param body The body to be send. Usually Gamification Element
	 * @return Response in String format
	 * @throws IOException
	 */
	private String gamificationPostRequest(String path, byte[] body, String contentType) throws IOException {
		URL url = null;
		try {
			url = new URL(getGamificationUrl() + path);
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
			// "Sending to " + url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11,
			// "Could not send, because of malformed url " + url);
		}
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", contentType);
		connection.setRequestProperty("Authorization", getL2pAuth());
		connection.setRequestProperty("access-token", getL2pAccessToken());
		connection.setRequestMethod("POST");
		OutputStream wr = connection.getOutputStream();
		wr.write(body, 0, body.length);
		// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
		// "Send POST request to " +url);
		BufferedReader br = null;
		if (connection.getResponseCode() >= 100 && connection.getResponseCode() < 400) {
			br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
		} else {
			br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"));
		}
		// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
		// "Reading response from POST request to endpoint " + url);
		StringBuilder response = new StringBuilder();
		String responseLine = null;
		while ((responseLine = br.readLine()) != null) {
			response.append(responseLine.trim());
		}
		// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
		// "Response code is " + connection.getResponseCode());
		// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
		// "Response is " + response.toString());
		return response.toString();
	}

	/**
	 * 
	 * @param path   The path to trigger the corresponding gamification function.
	 *               Path will be appended to gamificationUrl
	 * @param output
	 * @param body   The body to be send. Usually Gamification Element
	 * @return Response in String format
	 * @throws IOException
	 */
	private String gamificationPutRequest(String path, byte[] body, String boundary) throws IOException {
		URL url = null;
		try {
			url = new URL(getGamificationUrl() + path);
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
			// "Sending to " + url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11,
			// "Could not send, because of malformed url " + url);
		}
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
		connection.setRequestProperty("Authorization", getL2pAuth());
		connection.setRequestProperty("access-token", getL2pAccessToken());
		connection.setRequestMethod("PUT");
		OutputStream wr = connection.getOutputStream();
		wr.write(body, 0, body.length);
		// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
		// "Send PUT request to " +url);
		BufferedReader br = null;
		if (connection.getResponseCode() >= 100 && connection.getResponseCode() < 400) {
			br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
		} else {
			br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"));
		}
		// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
		// "Reading response from PUT request to endpoint " + url);
		StringBuilder response = new StringBuilder();
		String responseLine = null;
		while ((responseLine = br.readLine()) != null) {
			response.append(responseLine.trim());
		}
		// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
		// "Response code is " + connection.getResponseCode());
		// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
		// "Response is " + response.toString());
		return response.toString();
	}

	/**
	 * 
	 * @param path The path to trigger the corresponding gamification function. Path
	 *             will be appended to gamificationUrl
	 * @return Response in String format
	 * @throws IOException
	 */
	private String gamificationDeleteRequest(String path) throws IOException {
		URL url = null;
		try {
			url = new URL(getGamificationUrl() + path);
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
			// "Sending to " + url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_11,
			// "Could not send, because of malformed url " + url);
		}
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setRequestProperty("Authorization", getL2pAuth());
		connection.setRequestProperty("access-token", getL2pAccessToken());
		connection.setRequestMethod("DELETE");
		OutputStream wr = connection.getOutputStream();
		// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
		// "Send DELETE request to " +url);
		BufferedReader br = null;
		if (connection.getResponseCode() >= 100 && connection.getResponseCode() < 400) {
			br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
		} else {
			br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"));
		}
		// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
		// "Reading response from DELETE request to endpoint " + url);
		StringBuilder response = new StringBuilder();
		String responseLine = null;
		while ((responseLine = br.readLine()) != null) {
			response.append(responseLine.trim());
		}
		// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
		// "Response code is " + connection.getResponseCode());
		// Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14,
		// "Response is " + response.toString());
		return response.toString();
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
		Map<String, List<? extends ElementMapping>> map = new HashMap<>();
		map.put("games", mapping.getGameMapping());
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
	 * @return the listenerUrl
	 */
	public String getListenerUrl() {
		return listenerUrl;
	}

	/**
	 * @param listenerUrl the listenerUrl to set
	 */
	public void setListenerUrl(String listenerUrl) {
		this.listenerUrl = listenerUrl;
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

	/**
	 * @return the day
	 */
	public String getLastCheckedDay() {
		return lastCheckedDay;
	}

	/**
	 * @param day the day to set
	 */
	public void setLastCheckedDay(String day) {
		this.lastCheckedDay = day;
	}

	/**
	 * @return the time
	 */
	public String getLastCheckedTime() {
		return lastCheckedTime;
	}

	/**
	 * @param time the lastStatementTimeStamp to set
	 */
	public void setLastCheckedTime(String time) {
		this.lastCheckedTime = time;
	}

}
