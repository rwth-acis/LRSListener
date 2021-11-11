package i5.las2peer.services.gamification.listener;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.json.JSONObject;


import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.LocalNodeManager;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.services.gamification.listener.QuestModel.QuestStatus;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.connectors.webConnector.WebConnector;
import i5.las2peer.connectors.webConnector.client.ClientResponse;
import i5.las2peer.connectors.webConnector.client.MiniClient;

import org.junit.After;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;

import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConfiguratorTest {
	
	private static final int HTTP_PORT = 8081;
	
	
	private static LocalNode node;
	private static WebConnector connector;
	private static ByteArrayOutputStream logStream;
	
	private static MiniClient c1, c2, c3;
	
	private static UserAgentImpl user1, user2, user3;
	
	
	private static String configId = "testConfig";
	private static String gameId = "testGame";
	private static String questId = "testQuest";
	private static String achievementId = "testAchievement";
	private static String badgeId = "testBadge";
	private static String actionId = "testAction1";
	private static int levelNumber = 1;
	private static String streakId = "testString";
	private static final String mainPath = "gamification/configurator";
	private static Map<String, String> headers;
	private static ConfigModel config;
	private static GameModel game;
	private static ActionModel action;
	private static AchievementModel achievement;
	private static BadgeModel badge;
	private static QuestModel quest;
	private static LevelModel level;
	private static StreakModel streak;
	//private static List<Pair<String,Integer>> actionList;
	
	// to fetch data per batch
	int currentPage = 1;
	int windowSize = 10;
	String searchParam = "";
	
	String unitName = "dollar";
	/**
	 * Called before the tests start.
	 * 
	 * Sets up the node and initializes connector and users that can be used throughout the tests.
	 * 
	 * @throws Exception
	 */
	@Before
	public void startServer() throws Exception {
		
		node = new LocalNodeManager().newNode();
		node.launch();
		
		user1 = MockAgentFactory.getAdam();
		user2 = MockAgentFactory.getAbel();
		user3 = MockAgentFactory.getEve();
		
		// agent must be unlocked in order to be stored 
		user1.unlock("adamspass");
		user2.unlock("abelspass");
		user3.unlock("evespass");
		
		node.storeAgent(user1);
		node.storeAgent(user2);
		node.storeAgent(user3);
	
		node.startService(new ServiceNameVersion(ListenerConfigurator.class.getName(), "0.1"), "a pass");
		
		logStream = new ByteArrayOutputStream();
	
		connector = new WebConnector(true, HTTP_PORT, false, 1000);
		connector.setLogStream(new PrintStream(logStream));
		connector.start(node);
		// wait a second for the connector to become ready
		Thread.sleep(1000);
		
		c1 = new MiniClient();
		c1.setConnectorEndpoint(connector.getHttpEndpoint());
		c1.setLogin(user1.getIdentifier(), "adamspass");
		
		c2 = new MiniClient();
		c2.setConnectorEndpoint(connector.getHttpEndpoint());
		c2.setLogin(user2.getIdentifier(), "abelspass");
	
		c3 = new MiniClient();
		c3.setConnectorEndpoint(connector.getHttpEndpoint());
		c3.setLogin(user3.getIdentifier(), "evespass");
		
		config = new ConfigModel();
		config.setConfigId(configId);
		config.setDescription("testDesc");
		config.setName("TestConfigName");
		
		game = new GameModel();
		game.setGameId(gameId);
		game.setDescription("testDesc");
		game.setCommunityType("testCommunity");
		
		action = new ActionModel();
		action.setActionId(actionId);
		action.setName("TestActionName");
		action.setDescription("testDesc");
		action.setUseNotification(true);
		action.setNotificationMessage("This is some test message");
		
		badge = new BadgeModel();
		badge.setBadgeId(badgeId);
		badge.setName("testBadgeName");
		badge.setDescription("testDesc");
		badge.setUseNotification(true);
		badge.setNotificationMessage("This is some test message");
		
		achievement = new AchievementModel();
		achievement.setAchievementId(achievementId);
		achievement.setBadgeId(badgeId);
		achievement.setName("testAchievementName");
		achievement.setDescription("testDesc");
		achievement.setPointValue(50);
		achievement.setUseNotification(true);
		achievement.setNotificationMessage("This is some test message");
		
		level = new LevelModel();
		level.setLevelNumber(levelNumber);
		level.setName("testLevelName");
		level.setPointValue(50);
		level.setUseNotification(true);
		level.setNotificationMessage("This is some test message");
		
		quest = new QuestModel();
		quest.setQuestId(questId);
		quest.setName("testQuestName");
		quest.setDescription("testDesc");
		quest.setQuestFlag(false);
		quest.setPointFlag(false);
		quest.setPointValue(50);
		quest.setQuestIdCompleted(questId);
		quest.setStatus(QuestStatus.REVEALED);
		quest.setAchievementId(achievementId);
		List<String> actionList = new ArrayList<>();
		actionList.add("testAction1");
		actionList.add("testAction2");
		actionList.add("testAction3");
		quest.setActionIds(actionList);
		quest.setUseNotification(true);
		quest.setNotificationMessage("This is some test message");
		
		streak = new StreakModel();
		
		headers = new HashMap<>();
		headers.put("Accept-Encoding","gzip, deflate");
		headers.put("Accept-Language","en-GB,en-US;q=0.8,en;q=0.6");
	}
	
	/**
	 * Called after the test has finished. Shuts down the server and prints out the connector log file for reference.
	 * 
	 * @throws Exception
	 */
	@After
	public void shutDownServer() throws Exception {
		if (connector != null) {
			connector.stop();
			connector = null;
		}
		if (node != null) {
			node.shutDown();
			node = null;
		}
		if (logStream != null) {
			System.out.println("Connector-Log:");
			System.out.println("--------------");
			System.out.println(logStream.toString());
			logStream = null;
		}
	}
	
	@Test
	public void testA1_createNewConfiguration(){
		System.out.println("Test --- Create New Configuration");
		try
		{
			JSONObject jsonObject = new JSONObject(config);
			ClientResponse result = c1.sendRequest("POST", mainPath + "/"+ configId, jsonObject.toString(), "application/json", "*/*", headers);
			System.out.println(result.getResponse());
			if(result.getHttpCode()==HttpURLConnection.HTTP_OK){
				assertEquals(HttpURLConnection.HTTP_OK,result.getHttpCode());
			}
			else{
				assertEquals(HttpURLConnection.HTTP_CREATED,result.getHttpCode());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testA2_createNewGame(){
		System.out.println("Test --- Create New Game");
		try
		{
			JSONObject jsonObject = new JSONObject(game);
			
			ClientResponse result = c1.sendRequest("POST", mainPath + "/game/"+ configId + "/" + "listenTo", jsonObject.toString(), "application/json", "*/*", headers);
			System.out.println(result.getResponse());
			if(result.getHttpCode()==HttpURLConnection.HTTP_OK){
				assertEquals(HttpURLConnection.HTTP_OK,result.getHttpCode());
			}
			else{
				assertEquals(HttpURLConnection.HTTP_CREATED,result.getHttpCode());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testA3_createNewAction(){
		System.out.println("Test --- Create New Action");
		try
		{
			JSONObject jsonObject = new JSONObject(action);
			ClientResponse result = c1.sendRequest("POST", mainPath + "/action/"+ configId + "/" + gameId + "/" + "listenTo", jsonObject.toString(), "application/json", "*/*", headers);
			System.out.println(result.getResponse());
			
			
			action.setActionId("testAction2");
			jsonObject = new JSONObject(action);
			result = c1.sendRequest("POST", mainPath + "/action/"+ configId + "/" + gameId + "/" + "listenTo", jsonObject.toString(), "application/json", "*/*", headers);
			System.out.println(result.getResponse());
			
			action.setActionId("testAction3");
			jsonObject = new JSONObject(action);
			result = c1.sendRequest("POST", mainPath + "/action/"+ configId + "/" + gameId + "/" + "listenTo", jsonObject.toString(), "application/json", "*/*", headers);
			System.out.println(result.getResponse());
			
			if(result.getHttpCode()==HttpURLConnection.HTTP_OK){
				assertEquals(HttpURLConnection.HTTP_OK,result.getHttpCode());
			}
			else{
				assertEquals(HttpURLConnection.HTTP_CREATED,result.getHttpCode());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testA4_createNewBadge(){
		System.out.println("Test --- Create New Badge");
		try
		{
			JSONObject jsonObject = new JSONObject(badge);

			ClientResponse result = c1.sendRequest("POST", mainPath + "/badge/"+ configId + "/" + gameId + "/" + "listenTo", jsonObject.toString(), "application/json", "*/*", headers);
			System.out.println(result.getResponse());
			if(result.getHttpCode()==HttpURLConnection.HTTP_OK){
				assertEquals(HttpURLConnection.HTTP_OK,result.getHttpCode());
			}
			else{
				assertEquals(HttpURLConnection.HTTP_CREATED,result.getHttpCode());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testA5_createNewAchievement(){
		System.out.println("Test --- Create New Achievement");
		try
		{
			JSONObject jsonObject = new JSONObject(achievement);

			ClientResponse result = c1.sendRequest("POST", mainPath + "/achievement/"+ configId + "/" + gameId + "/" + "listenTo", jsonObject.toString(), "application/json", "*/*", headers);
			System.out.println(result.getResponse());
			if(result.getHttpCode()==HttpURLConnection.HTTP_OK){
				assertEquals(HttpURLConnection.HTTP_OK,result.getHttpCode());
			}
			else{
				assertEquals(HttpURLConnection.HTTP_CREATED,result.getHttpCode());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testA6_createNewLevel(){
		System.out.println("Test --- Create New Level");
		try
		{
			JSONObject jsonObject = new JSONObject(level);

			ClientResponse result = c1.sendRequest("POST", mainPath + "/level/"+ configId + "/" + gameId + "/" + "listenTo", jsonObject.toString(), "application/json", "*/*", headers);
			System.out.println(result.getResponse());
			if(result.getHttpCode()==HttpURLConnection.HTTP_OK){
				assertEquals(HttpURLConnection.HTTP_OK,result.getHttpCode());
			}
			else{
				assertEquals(HttpURLConnection.HTTP_CREATED,result.getHttpCode());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testA7_createNewQuest(){
		System.out.println("Test --- Create New Quest");
		try
		{
			JSONObject jsonObject = new JSONObject(quest);

			ClientResponse result = c1.sendRequest("POST", mainPath + "/quest/"+ configId + "/" + gameId + "/" + "listenTo", jsonObject.toString(), "application/json", "*/*", headers);
			System.out.println(result.getResponse());
			if(result.getHttpCode()==HttpURLConnection.HTTP_OK){
				assertEquals(HttpURLConnection.HTTP_OK,result.getHttpCode());
			}
			else{
				assertEquals(HttpURLConnection.HTTP_CREATED,result.getHttpCode());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	
	public void testA8_createNewStreak(){
		System.out.println("Test --- Create New Streak");
		try
		{
			JSONObject jsonObject = new JSONObject(streak);

			ClientResponse result = c1.sendRequest("POST", mainPath + "/streak/"+ configId + "/" + gameId + "/" + "listenTo", jsonObject.toString(), "application/json", "*/*", headers);
			System.out.println(result.getResponse());
			if(result.getHttpCode()==HttpURLConnection.HTTP_OK){
				assertEquals(HttpURLConnection.HTTP_OK,result.getHttpCode());
			}
			else{
				assertEquals(HttpURLConnection.HTTP_CREATED,result.getHttpCode());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testB1_getConfigWithId(){

		System.out.println("Test --- Get Config With Id");
		try
		{
			ClientResponse result = c1.sendRequest("GET",  mainPath + "/" +configId, "");
			System.out.println(result.getResponse());
	        assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testB2_getGameWithId(){

		System.out.println("Test --- Get Game With Id");
		try
		{
			ClientResponse result = c1.sendRequest("GET",  mainPath + "/game/" +configId + "/"+ gameId, "");
			System.out.println(result.getResponse());
	        assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testB3_getActionWithId(){

		System.out.println("Test --- Get Action With Id");
		try
		{
			ClientResponse result = c1.sendRequest("GET",  mainPath + "/action/" +configId +"/"+ actionId, "");
			System.out.println(result.getResponse());
	        assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testB4_getBadgeWithId(){

		System.out.println("Test --- Get Badge With Id");
		try
		{
			ClientResponse result = c1.sendRequest("GET",  mainPath + "/badge/" +configId +"/"+  badgeId, "");
			System.out.println(result.getResponse());
	        assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testB5_getAchievementWithId(){

		System.out.println("Test --- Get Achievement With Id");
		try
		{
			ClientResponse result = c1.sendRequest("GET",  mainPath + "/achievement/" +configId +"/"+  achievementId, "");
			System.out.println(result.getResponse());
	        assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testB6_getQuestWithId(){

		System.out.println("Test --- Get Quest With Id");
		try
		{
			ClientResponse result = c1.sendRequest("GET",  mainPath + "/quest/" +configId +"/"+  questId, "");
			System.out.println(result.getResponse());
	        assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testB7_getLevelWithId(){

		System.out.println("Test --- Get Level With Id");
		try
		{
			ClientResponse result = c1.sendRequest("GET",  mainPath + "/level/" +configId +"/"+  levelNumber, "");
			System.out.println(result.getResponse());
	        assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	
	public void testB8_getStreakWithId(){

		System.out.println("Test --- Get Streak With Id");
		try
		{
			ClientResponse result = c1.sendRequest("GET",  mainPath + "/streak/" +configId +"/"+ streakId, "");
			System.out.println(result.getResponse());
	        assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testC1_getConfigurationMapping(){

		System.out.println("Test --- Get Mapping With configId");
		try
		{
			ClientResponse result = c1.sendRequest("GET",  mainPath + "/mapping/" +configId , "");
			System.out.println(result.getResponse());
	        assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testC2_setTimeStamps(){

		System.out.println("Test --- Set Timestamps");
		try
		{	JSONObject jsonObject = new JSONObject();
			jsonObject.put("timestamp", "2021-10-05");
			jsonObject.put("laststatement", "12:13:14.123");
			ClientResponse result = c1.sendRequest("POST",  mainPath + "/timestamp/" +configId , jsonObject.toString(), "application/json", "*/*", headers);
			System.out.println(result.getResponse());
	        assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testC3_getTimeStamps(){

		System.out.println("Test --- Get Timestamps");
		try
		{
			ClientResponse result = c1.sendRequest("GET",  mainPath + "/timestamp/" +configId , "");
			System.out.println(result.getResponse());
	        assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testD1_updateAction() {
		try
		{
			action.setName("UpdatedName");
			JSONObject jsonObject = new JSONObject(action);
			ClientResponse result = c1.sendRequest("PUT", mainPath + "/action/" +configId + "/"+ actionId, jsonObject.toString() ,"application/json", "*/*", headers);
	
			System.out.println(result.getResponse());
			assertEquals(HttpURLConnection.HTTP_OK,result.getHttpCode());
			
	} catch (Exception e)
		{
		e.printStackTrace();
		System.out.println(e.getMessage());
		
		fail("Exception: " + e);
		System.exit(0);
		}
	}
	
	@Test
	public void testD2_updateBadge() {
		try
		{
			badge.setName("UpdatedName");
			JSONObject jsonObject = new JSONObject(badge);
			ClientResponse result = c1.sendRequest("PUT", mainPath + "/badge/" +configId + "/"+ badgeId, jsonObject.toString() ,"application/json", "*/*", headers);
	
			System.out.println(result.getResponse());
			assertEquals(HttpURLConnection.HTTP_OK,result.getHttpCode());
			
	} catch (Exception e)
		{
		e.printStackTrace();
		System.out.println(e.getMessage());
		
		fail("Exception: " + e);
		System.exit(0);
		}
	}
	
	@Test
	public void testD3_updateAchievement() {
		try
		{
			achievement.setName("UpdatedName");
			JSONObject jsonObject = new JSONObject(achievement);
			ClientResponse result = c1.sendRequest("PUT", mainPath + "/achievement/" +configId + "/"+ achievementId, jsonObject.toString() ,"application/json", "*/*", headers);
	
			System.out.println(result.getResponse());
			assertEquals(HttpURLConnection.HTTP_OK,result.getHttpCode());
			
	} catch (Exception e)
		{
		e.printStackTrace();
		System.out.println(e.getMessage());
		
		fail("Exception: " + e);
		System.exit(0);
		}
	}
	
	@Test
	public void testD4_updateQuest() {
		try
		{
			quest.setName("UpdatedName");
			JSONObject jsonObject = new JSONObject(quest);
			ClientResponse result = c1.sendRequest("PUT", mainPath + "/quest/" +configId + "/"+ questId, jsonObject.toString() ,"application/json", "*/*", headers);
	
			System.out.println(result.getResponse());
			assertEquals(HttpURLConnection.HTTP_OK,result.getHttpCode());
			
	} catch (Exception e)
		{
		e.printStackTrace();
		System.out.println(e.getMessage());
		
		fail("Exception: " + e);
		System.exit(0);
		}
	}
	
	@Test
	public void testD5_updateLevel() {
		try
		{
			level.setName("UpdatedName");
			JSONObject jsonObject = new JSONObject(level);
			ClientResponse result = c1.sendRequest("PUT", mainPath + "/level/" +configId + "/"+ levelNumber, jsonObject.toString() ,"application/json", "*/*", headers);
	
			System.out.println(result.getResponse());
			assertEquals(HttpURLConnection.HTTP_OK,result.getHttpCode());
			
	} catch (Exception e)
		{
		e.printStackTrace();
		System.out.println(e.getMessage());
		
		fail("Exception: " + e);
		System.exit(0);
		}
	}
	
	
	public void testD6_updateStreak() {
		try
		{
			streak.setName("UpdatedName");
			JSONObject jsonObject = new JSONObject(streak);
			ClientResponse result = c1.sendRequest("PUT", mainPath + "/streak/" +configId + "/"+ streakId, jsonObject.toString() ,"application/json", "*/*", headers);
	
			System.out.println(result.getResponse());
			assertEquals(HttpURLConnection.HTTP_OK,result.getHttpCode());
			
	} catch (Exception e)
		{
		e.printStackTrace();
		System.out.println(e.getMessage());
		
		fail("Exception: " + e);
		System.exit(0);
		}
	}
	
	@Test
	public void testE1_deleteGame(){
		System.out.println("Test --- Delete Game");
		try
		{
			ClientResponse result = c1.sendRequest("DELETE",  mainPath + "/game/" +configId + "/"+ gameId , "");
	        assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testE2_deleteQuest(){
		System.out.println("Test --- Delete Quest");
		try
		{
			ClientResponse result = c1.sendRequest("DELETE",  mainPath + "/quest/" +configId + "/"+ questId, "");
	        assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	
	public void testE3_deleteStreak(){
		System.out.println("Test --- Delete Streak");
		try
		{
			ClientResponse result = c1.sendRequest("DELETE",  mainPath + "/streak/" +configId + "/"+ streakId, "");
	        assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testE4_deleteLevel(){
		System.out.println("Test --- Delete Level");
		try
		{
			ClientResponse result = c1.sendRequest("DELETE",  mainPath +  "/level/" +configId + "/"+ levelNumber, "");
	        assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testE5_deleteAchievement(){
		System.out.println("Test --- Delete Achievement");
		try
		{
			ClientResponse result = c1.sendRequest("DELETE",  mainPath + "/achievement/" +configId + "/"+ achievementId, "");
	        assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testE6_deleteBadge(){
		System.out.println("Test --- Delete Badge");
		try
		{
			ClientResponse result = c1.sendRequest("DELETE",  mainPath + "/badge/" +configId + "/"+ badgeId, "");
	        assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testE7_deleteAction(){
		System.out.println("Test --- Delete Action");
		try
		{
			ClientResponse result = c1.sendRequest("DELETE",  mainPath + "/action/" +configId + "/"+ "testAction1", "");
			result = c1.sendRequest("DELETE",  mainPath + "/action/" +configId + "/"+ "testAction2", "");
			result = c1.sendRequest("DELETE",  mainPath + "/action/" +configId + "/"+ "testAction3", "");
	        assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	
	@Test
	public void testE8_deleteConfiguration(){
		System.out.println("Test --- Delete Configuration");
		try
		{
			ClientResponse result = c1.sendRequest("DELETE",  mainPath  + "/" + configId, "");
	        assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
}