//import org.junit.Before;
//import org.junit.FixMethodOrder;
//import org.junit.Test;
//import org.junit.runners.MethodSorters;
//
//import i5.las2peer.p2p.LocalNode;
//import i5.las2peer.p2p.LocalNodeManager;
//import i5.las2peer.api.p2p.ServiceNameVersion;
//import i5.las2peer.security.UserAgentImpl;
//import i5.las2peer.services.gamification.listener.ListenerConfigurator;
//import i5.las2peer.testing.MockAgentFactory;
//import i5.las2peer.connectors.webConnector.WebConnector;
//import i5.las2peer.connectors.webConnector.client.ClientResponse;
//import i5.las2peer.connectors.webConnector.client.MiniClient;
//
//import org.junit.After;
//import org.junit.Assert;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.fail;
//
//import java.io.ByteArrayOutputStream;
//import java.io.PrintStream;
//
//import javax.ws.rs.client.Client;
//import javax.ws.rs.client.ClientBuilder;
//import javax.ws.rs.client.WebTarget;
//import javax.ws.rs.core.MediaType;
//import javax.ws.rs.core.Response;
//
//@SuppressWarnings("unused")
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//public class ConfiguratorTest {
//	
//private static final int HTTP_PORT = 8081;
//	
//	private static LocalNode node;
//	private static WebConnector connector;
//	private static ByteArrayOutputStream logStream;
//
//	private static MiniClient c1, c2, c3;
//
//	private static UserAgentImpl user1, user2, user3;
//	
//	private static String mainPath = "gamification/configurator";
//	private static String configId = "1";
//	
//	
//	@Before
//	public void startServer() throws Exception {
//		
//		node = new LocalNodeManager().newNode();
//		node.launch();
//		
//		user1 = MockAgentFactory.getAdam();
//		user2 = MockAgentFactory.getAbel();
//		user3 = MockAgentFactory.getEve();
//		
//		// agent must be unlocked in order to be stored 
//		user1.unlock("adamspass");
//		user2.unlock("abelspass");
//		user3.unlock("evespass");
//		
//		node.storeAgent(user1);
//		node.storeAgent(user2);
//		node.storeAgent(user3);
//
//		node.startService(new ServiceNameVersion(ListenerConfigurator.class.getName(), "0.1"), "a pass");
//		
//		logStream = new ByteArrayOutputStream();
//
//		connector = new WebConnector(true, HTTP_PORT, false, 1000);
//		connector.setLogStream(new PrintStream(logStream));
//		connector.start(node);
//		// wait a second for the connector to become ready
//		Thread.sleep(1000);
//		
//		c1 = new MiniClient();
//		c1.setConnectorEndpoint(connector.getHttpEndpoint());
//		c1.setLogin(user1.getIdentifier(), "adamspass");
//		
//		c2 = new MiniClient();
//		c2.setConnectorEndpoint(connector.getHttpEndpoint());
//		c2.setLogin(user2.getIdentifier(), "abelspass");
//
//		c3 = new MiniClient();
//		c3.setConnectorEndpoint(connector.getHttpEndpoint());
//		c3.setLogin(user3.getIdentifier(), "evespass");
//
//	}
//
//	/**
//	 * Called after the test has finished. Shuts down the server and prints out the connector log file for reference.
//	 * 
//	 * @throws Exception
//	 */
//	@After
//	public void shutDownServer() throws Exception {
//		if (connector != null) {
//			connector.stop();
//			connector = null;
//		}
//		if (node != null) {
//			node.shutDown();
//			node = null;
//		}
//		if (logStream != null) {
//			System.out.println("Connector-Log:");
//			System.out.println("--------------");
//			System.out.println(logStream.toString());
//			logStream = null;
//		}
//	}
//	
//	@Test
//	public void testA2_getConfigWithId() {
//		System.out.println("Test --- Get Config With Id");
//		try
//		{
//			ClientResponse result = c1.sendRequest("GET",  mainPath + "/" + configId, "");
//	        assertEquals(200, result.getHttpCode());
//		} catch (Exception e)
//		{
//			e.printStackTrace();
//			fail("Exception: " + e);
//			System.exit(0);
//		}
//	}
//}
