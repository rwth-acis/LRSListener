package i5.las2peer.services.gamification.listener;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.json.JSONObject;


import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.LocalNodeManager;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.security.UserAgentImpl;
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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
@SuppressWarnings("unused")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ListenerTest {
	
	private static final int HTTP_PORT = 8081;
	
	
	private static LocalNode node;
	private static WebConnector connector;
	private static ByteArrayOutputStream logStream;
	
	private static MiniClient c1;
	
	private static UserAgentImpl user1;

	private static String mainPath = "gamification/listener";
	private static Map<String, String> headers;
	
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

		
		// agent must be unlocked in order to be stored 
		user1.unlock("adamspass");

		
		node.storeAgent(user1);

	
		node.startService(new ServiceNameVersion(LRSListener.class.getName(), "0.1"), "a pass");
		
		logStream = new ByteArrayOutputStream();
	
		connector = new WebConnector(true, HTTP_PORT, false, 1000);
		connector.setLogStream(new PrintStream(logStream));
		connector.start(node);
		// wait a second for the connector to become ready
		Thread.sleep(1000);
		
		c1 = new MiniClient();
		c1.setConnectorEndpoint(connector.getHttpEndpoint());
		c1.setLogin(user1.getIdentifier(), "adamspass");
		
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
	
	
	public void testA1_testStatements(){
		System.out.println("Test --- Create New Configuration");
		try
		{
			ClientResponse result = c1.sendRequest("POST", mainPath+ "/testStatements", "", "text/plain", "*/*", headers);
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
	
	
	public void testA2_testMapping(){
		System.out.println("Test --- Create New Configuration");
		try
		{
			ClientResponse result = c1.sendRequest("POST", mainPath+ "/testMapping", "", "text/plain", "*/*", headers);
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
	
	
	public void testA3_testNotify(){
		System.out.println("Test --- Create New Configuration");
		try
		{
			ClientConfig clientConfig = new ClientConfig();
			Client client = ClientBuilder.newClient(clientConfig);
			WebTarget target = client.target("http://localhost:8080/gamification/configurator");
			Mapping response = target
					.path("/mapping/testConfig")
					.request()
					.header("access-token", "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJoZTJ6NVRzbEM1M3VPQXZxNmFWckplT2I0ZUx5TUxUam9IT3dIdTBiRmFJIn0.eyJleHAiOjE2MzUzNDYxMzgsImlhdCI6MTYzNTM0MjUzOCwiYXV0aF90aW1lIjoxNjM1MzQyNTE5LCJqdGkiOiI1YmMyNTViNi00YTQ5LTRlMzItODA0ZC02Yzc3ODRlNTNkN2IiLCJpc3MiOiJodHRwczovL2FwaS5sZWFybmluZy1sYXllcnMuZXUvYXV0aC9yZWFsbXMvbWFpbiIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiI5OTMzLTlkM2RhYjUxYWE5MCIsInR5cCI6IkJlYXJlciIsImF6cCI6ImJkZGE3Mzk2LTNmNmQtNGQ4My1hYzIxLTY1YjQwNjlkMGVhYiIsIm5vbmNlIjoiNjM2ZTdhZjVkZmJjNGM5MzlkMmUwYzA1MzAxMDIzNjUiLCJzZXNzaW9uX3N0YXRlIjoiYWUwNTBhYzYtZjg2Mi00Y2I2LWJkYTYtZDM1NDJlMjk0NThmIiwiYWNyIjoiMCIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vMTM3LjIyNi4yMzIuMTc1OjMyMDEwIiwiaHR0cDovL3RlY2g0Y29tcC5kYmlzLnJ3dGgtYWFjaGVuLmRlOjMxMDEwIiwiaHR0cDovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODAiLCJodHRwczovL2ZpbGVzLnRlY2g0Y29tcC5kYmlzLnJ3dGgtYWFjaGVuLmRlIiwiaHR0cDovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6OTA5OCIsImh0dHBzOi8vY2xvdWQxMC5kYmlzLnJ3dGgtYWFjaGVuLmRlOjgwODQiLCJodHRwczovL21vbml0b3IudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiLCJodHRwOi8vMTI3LjAuMC4xOjgwODEiLCJodHRwczovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODA4MCIsImh0dHBzOi8vZ2l0LnRlY2g0Y29tcC5kYmlzLnJ3dGgtYWFjaGVuLmRlIiwiaHR0cDovLzEyNy4wLjAuMTo4MCIsImh0dHA6Ly9sb2NhbGhvc3Q6ODAiLCJodHRwczovL2NhZS1kZXYudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiLCJodHRwOi8vMTI3LjAuMC4xOjgwODAiLCJodHRwOi8vbG9jYWxob3N0OjgwODAiLCJodHRwOi8vbGFzMnBlZXIuZGJpcy5yd3RoLWFhY2hlbi5kZSIsImh0dHBzOi8vbGFzMnBlZXIuZGJpcy5yd3RoLWFhY2hlbi5kZTo5MDk4IiwiaHR0cDovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODA4MCIsImh0dHA6Ly9sb2NhbGhvc3Q6ODA4MSIsImh0dHBzOi8vbGFzMnBlZXIudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiLCJodHRwczovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODAiLCJodHRwOi8vY2xvdWQxMC5kYmlzLnJ3dGgtYWFjaGVuLmRlOjgwODIiLCJodHRwczovL3NiZi1kZXYudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIiwic2lkIjoiYWUwNTBhYzYtZjg2Mi00Y2I2LWJkYTYtZDM1NDJlMjk0NThmIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJNYXJjIEJlbHNjaCIsInByZWZlcnJlZF91c2VybmFtZSI6Im1iZWxzY2giLCJnaXZlbl9uYW1lIjoiTWFyYyIsImZhbWlseV9uYW1lIjoiQmVsc2NoIiwiZW1haWwiOiJtYXJjLmJlbHNjaEByd3RoLWFhY2hlbi5kZSJ9.LMjOFlT-3JWqDPbSymEQKX9sROvosWIAPMRufTocRGy-0DQAuIJS41iSYPO3jyRC2i9HsyGdShoJy9ISocb4F3BiWUQQleuqXQ9zGAVP9i26j9fTH4xJRR8YWIIQp57-f8tx63dA85J9IAJZvaNkLGDcPzq2e5bbCOlCbRyB3KrirA3gtnwspwFF8yl4YHf93bQsugkAtdUACU-Ouh65_dDdTsX7nDmv2PsXC-qOWc52DPmPydOC_PEzP00hI5AkgUnmNLx6YqBT5Yif3jrMUkkzwlzBQDoQGGPIbNOGwosENlDbuaZ7jdqrpexXTWys7fGh6foU7zAApHKSmxqUFw")
					.header("Authorization", "Basic T0lEQ19TVUItOTkzMy05ZDNkYWI1MWFhOTA6OTkzMy05ZDNkYWI1MWFhOTA=")
					.get(Mapping.class);
			JSONObject mapping = new JSONObject(response);
			ClientResponse result = c1.sendRequest("POST", mainPath+ "/notify", mapping.toString(), "application/json", "*/*", headers);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
	
	@Test
	public void testA4_testGamification(){
		System.out.println("Test --- Create New Configuration");
			String result = null;
			String gameId = "testGame";
			try {
//				String game = retriveGameElement("/gamification/configurator/game/"+ "testConfig" + "/" + gameId);
//				JSONObject jsonGame = new JSONObject(game);
				Form form = new Form();
				form.param("gameid", "testGame76");
				form.param("gamedesc", "someDesc");
				form.param("commtype", "commType");
				
//				MultiPart multi = new FormDataMultiPart()
//						.bodyPart()
				String boundary =  "--32532twtfaweafwsgfaegfawegf4"; 
				
				MultipartEntityBuilder builder = MultipartEntityBuilder.create();
				builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
				builder.setBoundary(boundary);
				

//				builder.addPart("gameid", new StringBody(jsonGame.getString("gameId"), ContentType.TEXT_PLAIN));
//				builder.addPart("gamedesc", new StringBody(jsonGame.getString("description"), ContentType.TEXT_PLAIN));
//				builder.addPart("commtype", new StringBody(jsonGame.getString("communityType"), ContentType.TEXT_PLAIN));
				
				builder.addPart("gameid", new StringBody("testGame76", ContentType.TEXT_PLAIN));
				builder.addPart("gamedesc", new StringBody("description", ContentType.TEXT_PLAIN));
				builder.addPart("commtype", new StringBody("communityType", ContentType.TEXT_PLAIN));
				
				HttpEntity formData = builder.build();
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				formData.writeTo(out);
				String output = out.toString();
				
//				
//				Client client = ClientBuilder.newClient();
//				WebTarget target = client.target("http://localhost:8080/gamification/listener");
//				Invocation invocation = target
//						.path("/data")
//						.request()
//						.header("Content-Length", output.getBytes().length)
//						.header("Content-Type", "multipart/form-data; boundary="+boundary)
//						.header("access-token", "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJoZTJ6NVRzbEM1M3VPQXZxNmFWckplT2I0ZUx5TUxUam9IT3dIdTBiRmFJIn0.eyJleHAiOjE2MzUzNDYxMzgsImlhdCI6MTYzNTM0MjUzOCwiYXV0aF90aW1lIjoxNjM1MzQyNTE5LCJqdGkiOiI1YmMyNTViNi00YTQ5LTRlMzItODA0ZC02Yzc3ODRlNTNkN2IiLCJpc3MiOiJodHRwczovL2FwaS5sZWFybmluZy1sYXllcnMuZXUvYXV0aC9yZWFsbXMvbWFpbiIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiI5OTMzLTlkM2RhYjUxYWE5MCIsInR5cCI6IkJlYXJlciIsImF6cCI6ImJkZGE3Mzk2LTNmNmQtNGQ4My1hYzIxLTY1YjQwNjlkMGVhYiIsIm5vbmNlIjoiNjM2ZTdhZjVkZmJjNGM5MzlkMmUwYzA1MzAxMDIzNjUiLCJzZXNzaW9uX3N0YXRlIjoiYWUwNTBhYzYtZjg2Mi00Y2I2LWJkYTYtZDM1NDJlMjk0NThmIiwiYWNyIjoiMCIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vMTM3LjIyNi4yMzIuMTc1OjMyMDEwIiwiaHR0cDovL3RlY2g0Y29tcC5kYmlzLnJ3dGgtYWFjaGVuLmRlOjMxMDEwIiwiaHR0cDovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODAiLCJodHRwczovL2ZpbGVzLnRlY2g0Y29tcC5kYmlzLnJ3dGgtYWFjaGVuLmRlIiwiaHR0cDovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6OTA5OCIsImh0dHBzOi8vY2xvdWQxMC5kYmlzLnJ3dGgtYWFjaGVuLmRlOjgwODQiLCJodHRwczovL21vbml0b3IudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiLCJodHRwOi8vMTI3LjAuMC4xOjgwODEiLCJodHRwczovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODA4MCIsImh0dHBzOi8vZ2l0LnRlY2g0Y29tcC5kYmlzLnJ3dGgtYWFjaGVuLmRlIiwiaHR0cDovLzEyNy4wLjAuMTo4MCIsImh0dHA6Ly9sb2NhbGhvc3Q6ODAiLCJodHRwczovL2NhZS1kZXYudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiLCJodHRwOi8vMTI3LjAuMC4xOjgwODAiLCJodHRwOi8vbG9jYWxob3N0OjgwODAiLCJodHRwOi8vbGFzMnBlZXIuZGJpcy5yd3RoLWFhY2hlbi5kZSIsImh0dHBzOi8vbGFzMnBlZXIuZGJpcy5yd3RoLWFhY2hlbi5kZTo5MDk4IiwiaHR0cDovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODA4MCIsImh0dHA6Ly9sb2NhbGhvc3Q6ODA4MSIsImh0dHBzOi8vbGFzMnBlZXIudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiLCJodHRwczovL2xhczJwZWVyLmRiaXMucnd0aC1hYWNoZW4uZGU6ODAiLCJodHRwOi8vY2xvdWQxMC5kYmlzLnJ3dGgtYWFjaGVuLmRlOjgwODIiLCJodHRwczovL3NiZi1kZXYudGVjaDRjb21wLmRiaXMucnd0aC1hYWNoZW4uZGUiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIiwic2lkIjoiYWUwNTBhYzYtZjg2Mi00Y2I2LWJkYTYtZDM1NDJlMjk0NThmIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJNYXJjIEJlbHNjaCIsInByZWZlcnJlZF91c2VybmFtZSI6Im1iZWxzY2giLCJnaXZlbl9uYW1lIjoiTWFyYyIsImZhbWlseV9uYW1lIjoiQmVsc2NoIiwiZW1haWwiOiJtYXJjLmJlbHNjaEByd3RoLWFhY2hlbi5kZSJ9.LMjOFlT-3JWqDPbSymEQKX9sROvosWIAPMRufTocRGy-0DQAuIJS41iSYPO3jyRC2i9HsyGdShoJy9ISocb4F3BiWUQQleuqXQ9zGAVP9i26j9fTH4xJRR8YWIIQp57-f8tx63dA85J9IAJZvaNkLGDcPzq2e5bbCOlCbRyB3KrirA3gtnwspwFF8yl4YHf93bQsugkAtdUACU-Ouh65_dDdTsX7nDmv2PsXC-qOWc52DPmPydOC_PEzP00hI5AkgUnmNLx6YqBT5Yif3jrMUkkzwlzBQDoQGGPIbNOGwosENlDbuaZ7jdqrpexXTWys7fGh6foU7zAApHKSmxqUFw")
//						.header("Authorization", "Basic T0lEQ19TVUItOTkzMy05ZDNkYWI1MWFhOTA6OTkzMy05ZDNkYWI1MWFhOTA=")
//						.buildPost(Entity.entity(output.getBytes(), MediaType.MULTIPART_FORM_DATA));
//				Response response = invocation.invoke();		
			
			
			
			
			
			
			
			ClientResponse result2 = c1.sendRequest("GET", mainPath +  "/gami", "", "text/plain", "*/*", headers);
			System.out.println(result2.getResponse());
			if(result2.getHttpCode()==HttpURLConnection.HTTP_OK){
				assertEquals(HttpURLConnection.HTTP_OK,result2.getHttpCode());
			}
			else{
				assertEquals(HttpURLConnection.HTTP_CREATED,result2.getHttpCode());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
			System.exit(0);
		}
	}
}