package i5.las2peer.services.gamification.listener;


import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import javax.ws.rs.core.MediaType;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.restMapper.annotations.ServicePath;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;

import net.minidev.json.JSONObject;

import i5.las2peer.restMapper.RESTService;
import i5.las2peer.api.security.UserAgent;
import i5.las2peer.api.ManualDeployment;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.Context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * ListenerConfigurator Service
 * 
 * This service is used to configure the LRSListener Service. It uses the
 * LAS2peer Web-Connector for RESTful access to it.
 * 
 * Note: If you plan on using Swagger you should adapt the information below in
 * the ApiInfo annotation to suit your project. If you do not intend to provide
 * a Swagger documentation of your service API, the entire ApiInfo annotation
 * should be removed.
 * 
 */
@Api(value = "/gamification/configurator", authorizations = { @Authorization(value = "configurator_auth") })
@SwaggerDefinition(info = @Info(title = "ListenerConfigurator Service", version = "0.1", description = "Configurator for Listener Service", termsOfService = "http://your-terms-of-service-url.com", contact = @Contact(name = "Marc Belsch", url = "dbis.rwth-aachen.de", email = "marc.belsch.rwth-aachen.de"), license = @License(name = "your software license name", url = "http://your-software-license-url.com")))

@ManualDeployment
@ServicePath("/gamification/configurator")
public class ListenerConfigurator extends RESTService {

	private final L2pLogger logger = L2pLogger.getInstance(ListenerConfigurator.class.getName());
	private String jdbcDriverClassName;
	private String jdbcUrl;
	private String jdbcSchema;
	private String jdbcLogin;
	private String jdbcPass;
	private DatabaseManager dbm;
	private ConfigDAO configAccess;
	private Map<String,URL> observers;

	public ListenerConfigurator() {
		setFieldValues();
		dbm = new DatabaseManager(jdbcDriverClassName, jdbcLogin, jdbcPass, jdbcUrl, jdbcSchema);
		this.configAccess = new ConfigDAO();
		this.observers = new HashMap<String,URL>();
	}

	/**
	 * Function to return http unauthorized message
	 * 
	 * @return HTTP response unauthorized
	 */
	private Response unauthorizedMessage() {
		JSONObject objResponse = new JSONObject();
		objResponse.put("message", "You are not authorized");
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
		return Response.status(HttpURLConnection.HTTP_UNAUTHORIZED).entity(objResponse.toJSONString())
				.type(MediaType.APPLICATION_JSON).build();
	}

	/**
	 * Create a new configuration.
	 * 
	 * @param configId    Config ID obtained from LMS
	 * @param configModel  Config in JSON
	 * @param contentType Content type (implicitly sent in header)
	 * @return HTTP Response returned as JSON object
	 */
	@POST
	@Path("/{configId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_CREATED, message = "{message:Configuration upload success}"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "{message:Cannot create configuration. Configuration already exist!}"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "{message:Cannot create configuration. Configuration cannot be null!}"),

			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "{message:Cannot create configuration. Failed to upload configuration."),
			@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "{message:You are not authorized") })
	@ApiOperation(value = "createNewConfig", notes = "Create configuration")
	public Response createNewConfig(
			@ApiParam(value = "Config ID to store a new config", required = true) @PathParam("configId") String configId,
			@ApiParam(value = "Content-type in header", required = true) @HeaderParam(value = HttpHeaders.CONTENT_TYPE) String contentType,
			@ApiParam(value = "Configuration detail in JSON", required = true) ConfigModel configModel) {

		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"POST " + "gamification/configurator/" + configId, true);
		long randomLong = new Random().nextLong();

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		JSONObject objResponse = new JSONObject();
		Connection conn = null;

		try {
			conn = dbm.getConnection();

			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);

			// Check the existence of configuration ID
			if (configAccess.isConfigIdExist(conn, configId)) {
				objResponse.put("message", "Cannot create configuration.Cufigration ID already exist!");
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}

			try {
				configAccess.createConfig(conn, configModel);
				objResponse.put("message", "Configuration upload success (" + configId + ")");

				// Mobsos Logger
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15, "" + randomLong,
						true);
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_24, "" + name, true);
				return Response.status(HttpURLConnection.HTTP_CREATED).entity(objResponse.toJSONString()).build();

			} catch (SQLException e) {
				e.printStackTrace();
				objResponse.put("message",
						"Cannot create configuration. Failed to upload " + configId + ". " + e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.type(MediaType.APPLICATION_JSON).build();
			}

		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message",
					"Cannot create configuration. Failed to upload " + configId + ". " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.type(MediaType.APPLICATION_JSON).build();

		} catch (NullPointerException e) {
			e.printStackTrace();
			objResponse.put("message",
					"Cannot create configuration. Failed to upload " + configId + ". " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.type(MediaType.APPLICATION_JSON).build();
		}
		// always close connections
		finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Get a configuration with specific ID
	 * 
	 * @param configId Config ID obtained from LMS
	 * @return HTTP Response returned Config Model {@link ConfigModel} as JSON
	 *         object
	 */
	@GET
	@Path("/{configId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = ""),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "{message:Cannot get config detail. JSON processing error}"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "{message:Cannot get config. Failed to fetch config}"),

			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "{message:Cannot get config detail. Config not found}"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "{message:Config Null, Cannot find config with (configId)"),
			@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "{message:You are not authorized") })
	@ApiOperation(value = "getConfigWithId", notes = "Get config data with specified ID", response = ConfigModel.class)
	public Response getConfigWithId(@ApiParam(value = "Config ID") @PathParam("configId") String configId) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"GET " + "gamification/configurator/" + configId, true);
		long randomLong = new Random().nextLong(); // To be able to match
		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		JSONObject objResponse = new JSONObject();
		ConfigModel configModel = null;
		Connection conn = null;

		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				if (!configAccess.isConfigIdExist(conn, configId)) {
					objResponse.put("message", "Cannot get configuration detail. Configuration not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
			} catch (SQLException e1) {
				e1.printStackTrace();
				objResponse.put("message",
						"Cannot get configuration detail. Cannot check whether config ID exist or not. Database error. "
								+ e1.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
			configModel = configAccess.getConfigModelWithId(conn, configId);
			if (configModel == null) {
				objResponse.put("message", "Configuration Null, Cannot find configuration with " + configId);
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString()).build();
			}
			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			String configString = mapper.writeValueAsString(configModel);

			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_17, "" + randomLong, true);
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_26, "" + name, true);
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_27, "" + configId, true);
			return Response.status(HttpURLConnection.HTTP_OK).entity(configString).build();
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot get configuration detail. DB Error. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString()).build();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot get configuration detail. JSON processing error. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (Exception e2) {
				logger.printStackTrace(e2);
			}
		}
	}

	/**
	 * Update an configuration.
	 * 
	 * @param configId    Config ID obtained from LMS
	 * @param contentType Content type (implicitly sent in header)
	 * @param configModel Config Model in JSON
	 * @return HTTP Response returned as JSON object
	 */
	@PUT
	@Path("/{configId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Config Updated"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Error occured"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad request"),
			@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Unauthorized") })
	@ApiOperation(value = "updateConfiguration", notes = "Update a configuration")
	public Response updateConfiguration(
			@ApiParam(value = "Config ID to update a configuration", required = true) @PathParam("configId") String configId,
			@ApiParam(value = "Configuration data in multiple/form-data type", required = true) @HeaderParam(value = HttpHeaders.CONTENT_TYPE) String contentType,
			@ApiParam(value = "Configuration detail in JSON", required = true) ConfigModel configModel) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"UPDATE " + "gamification/configurator/" + configId, true);
		long randomLong = new Random().nextLong(); // To be able to match
		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		JSONObject objResponse = new JSONObject();
		Connection conn = null;

		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				if (!configAccess.isConfigIdExist(conn, configId)) {
					objResponse.put("message", "Cannot update configuration detail. Configuration not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
			} catch (SQLException e1) {
				e1.printStackTrace();
				objResponse.put("message",
						"Cannot update configuration detail. Cannot check whether config ID exist or not. Database error. "
								+ e1.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
			try {
				configAccess.updateConfig(conn, configModel);
				objResponse.put("message", "Configuration updated");
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_19, "" + randomLong,
						true);
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_28, "" + name, true);
				return Response.status(HttpURLConnection.HTTP_OK).entity(objResponse.toJSONString()).build();
			} catch (SQLException e) {
				objResponse.put("message", "Configuration could not be updated. DB error");
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot update configuration. DB Error. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString()).build();
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (Exception e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Delete an configuration data with specified ID
	 * 
	 * @param configId Config ID obtained from LMS
	 * @return HTTP Response returned as JSON object
	 */
	@DELETE
	@Path("/{configId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Config Delete Success"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "deleteConfiguration", notes = "Delete an configuration")
	public Response deleteConfiguration(@PathParam("configId") String configId) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"DELETE " + "gamification/configurator/" + configId, true);
		long randomLong = new Random().nextLong();

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);

			try {
				if (!configAccess.isConfigIdExist(conn, configId)) {
					objResponse.put("message", "Cannot get configuration detail. Configuration not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
			} catch (SQLException e1) {
				e1.printStackTrace();
				objResponse.put("message",
						"Cannot get configuration detail. Cannot check whether configuration ID exist or not. Database error. "
								+ e1.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
			configAccess.deleteConfig(conn, configId);

			objResponse.put("message", "Configuration deleted");
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_21, "" + randomLong, true);
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_30, "" + name, true);

			return Response.status(HttpURLConnection.HTTP_OK).entity(objResponse.toJSONString())
					.type(MediaType.APPLICATION_JSON).build();

		} catch (SQLException e) {

			e.printStackTrace();
			objResponse.put("message", "Cannot delete configuration. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.type(MediaType.APPLICATION_JSON).build();
		}
		// always close connections
		finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Acquire a Mapping to Listen to mapping changes
	 * 
	 * @param configId Config ID obtained from LMS
	 * @return HTTP Response returned as JSON object
	 */
	@GET
	@Path("/mapping/{configId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Config Delete Success"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "deleteConfiguration", notes = "Delete an configuration")
	public Response getMapping(@PathParam("configId") String configId) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"GET " + "gamification/configurator/mapping/" + configId, true);
		long randomLong = new Random().nextLong(); // To be able to match

//		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
//		String name = userAgent.getLoginName();
//		if (name.equals("anonymous")) {
//			return unauthorizedMessage();
//		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				if (!configAccess.isConfigIdExist(conn, configId)) {
					objResponse.put("message", "Cannot get configuration mapping. Configuration not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
			} catch (SQLException e1) {
				e1.printStackTrace();
				objResponse.put("message",
						"Cannot get configuration mapping. Cannot check whether configuration ID exist or not. Database error. "
								+ e1.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
			JSONObject mapping = configAccess.getMapping(conn, configId);
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_17, "" + randomLong, true);
//			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_26, "" + name, true);
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_27, "" + configId, true);
			return Response.status(HttpURLConnection.HTTP_OK).entity(mapping).build();
		} catch (SQLException e) {

		} finally {
			try {

			} catch (Exception e) {
				logger.printStackTrace(e);
			}
		}
		return null;
	}

	/**
	 * Register a game for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param lsitenTo String the Listener should listen to
	 * @param gameModel Game in JSON
	 * @return HTTP Response returned as JSON object
	 */
	@POST
	@Path("/game/{configId}/{listenTo}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Created game successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "registerGame", notes = "Create and register a game to a configuration")
	public Response registerGame(
			@PathParam("configId") String configId,
			@PathParam("listenTo") String listenTo,
			@ApiParam(value = "Game detail in JSON", required = true) GameModel gameModel) {

		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"POST " + "gamification/configurator/game/" + configId + "/" + listenTo, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				if (!configAccess.isConfigIdExist(conn, configId)) {
					objResponse.put("message", "Cannot register game. Configuration not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}

				try {
					configAccess.createGame(conn,gameModel);
					configAccess.addElementToMapping(conn, configId, gameModel.getGameId(), listenTo, "game");
					notifyObservers(configId);
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot register and create game. Failed to upload " + gameModel.getGameId() + ". " + e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString()).build();
				}
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15, "" + randomLong, true);
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_24, "" + name, true);
				return Response.status(HttpURLConnection.HTTP_CREATED).entity(objResponse.toJSONString()).build();
			} catch (SQLException e1) {
				e1.printStackTrace();
				objResponse.put("message",
						"Cannot register game. Cannot check whether game ID exist or not. Database error. "
								+ e1.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot register game. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.type(MediaType.APPLICATION_JSON).build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}

	}

	/**
	 * Get a registered game for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param gameId   to be worked on
	 * @return HTTP Response returned as JSON object
	 */
	@GET
	@Path("/game/{configId}/{gameId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Retrieved game successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "getGame", notes = "Get a game")
	public Response getGame(@PathParam("configId") String configId, @PathParam("gameId") String gameId) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"GET " + "gamification/configurator/game/" + configId + gameId, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				try {
					if (!configAccess.isConfigIdExist(conn, configId)) {
						objResponse.put("message", "Cannot get game. Configuration not found");
						Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
								(String) objResponse.get("message"));
						return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
								.build();
					}
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot register game. Cannot check whether config ID exist or not. Database error. "
									+ e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
							.build();
				}
				if (!configAccess.isGameIdExist(conn, gameId)) {
					objResponse.put("message", "Cannot get game. Game not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				GameModel game = configAccess.getGameWithId(conn, gameId);
				if(game == null){
					objResponse.put("message", "Game Null, Cannot find game with " + gameId);
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString()).type(MediaType.APPLICATION_JSON).build();
				}
				//TODO parse to JSON
				ObjectMapper mapper = new ObjectMapper();
				mapper.enable(SerializationFeature.INDENT_OUTPUT);
				String gameString = mapper.writeValueAsString(game);
				//
				Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_17, ""+randomLong, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_26, ""+name, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_27, ""+gameId, true);
				return Response.status(HttpURLConnection.HTTP_OK).entity(gameString).type(MediaType.APPLICATION_JSON).build();
			} catch (SQLException e) {
				e.printStackTrace();
				objResponse.put("message",
						"Cannot get game. Cannot check whether game ID exist or not. Database error. "
								+ e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				objResponse.put("message", "Cannot get game detail. JSON processing error. " + e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(e.getMessage())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot get game. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Update a registered game for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param gameId   to be worked on
	 * @param gameModel Game in JSON
	 * @return HTTP Response returned as JSON object
	 */
	@PUT
	@Path("/game/{configId}/{gameId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Updated game successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "updateGame", notes = "Update game")
	public Response updateGame(
			@PathParam("configId") String configId,
			@PathParam("gameId") String gameId,
			@ApiParam(value = "Game detail in JSON", required = true) GameModel gameModel) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"PUT " + "gamification/configurator/game/" + configId + "/" + gameId, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				try {
					if (!configAccess.isConfigIdExist(conn, configId)) {
						objResponse.put("message", "Cannot update game. Configuration not found");
						Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
								(String) objResponse.get("message"));
						return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
								.build();
					}
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot update game. Cannot check whether config ID exist or not. Database error. "
									+ e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
							.build();
				}
				if (!configAccess.isGameIdExist(conn, gameId)) {
					objResponse.put("message", "Cannot update game. Game not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				configAccess.updateGame(conn, gameModel);
				objResponse.put("message", "Game updated");
				Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_17, ""+randomLong, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_26, ""+name, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_27, ""+gameId, true);
				return Response.status(HttpURLConnection.HTTP_OK).entity(objResponse).build();
			} catch (SQLException e) {
				e.printStackTrace();
				objResponse.put("message",
						"Cannot update game. Cannot check whether game ID exist or not. Database error. "
								+ e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot update game. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Update a registered game for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param gameId   to be worked on
	 * @return HTTP Response returned as JSON object
	 */
	@DELETE
	@Path("/game/{configId}/{gameId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Deleted game successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "deleteGame", notes = "Delete and unregister a game from a configuration")
	public Response deleteGame(@PathParam("configId") String configId, @PathParam("gameId") String gameId) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"DELETE " + "gamification/configurator/game/" + configId + "/" + gameId, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				try {
					if (!configAccess.isConfigIdExist(conn, configId)) {
						objResponse.put("message", "Cannot delete game. Configuration not found");
						Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
								(String) objResponse.get("message"));
						return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
								.build();
					}
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot delete game. Cannot check whether config ID exist or not. Database error. "
									+ e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
							.build();
				}
				if (!configAccess.isGameIdExist(conn, gameId)) {
					objResponse.put("message", "Cannot delete game. Game not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				
				configAccess.deleteGame(conn, gameId);
				configAccess.removeElementFromMapping(conn, configId, gameId, "game");
				notifyObservers(configId);
				objResponse.put("message", "Game deleted");
				Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_17, ""+randomLong, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_26, ""+name, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_27, ""+gameId, true);
				return Response.status(HttpURLConnection.HTTP_OK).entity(objResponse).build();
			} catch (SQLException e) {
				e.printStackTrace();
				objResponse.put("message",
						"Cannot register game. Cannot check whether game ID exist or not. Database error. "
								+ e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot register game. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Register a quest for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param lsitenTo String the Listener should listen to
	 * @param questModel Quest in JSON
	 * @return HTTP Response returned as JSON object
	 */
	@POST
	@Path("/quest/{configId}/{listenTo}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Created quest successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "registerQuest", notes = "Create and register a quest to a configuration")
	public Response registerQuest(
			@PathParam("configId") String configId,
			@PathParam("listenTo") String listenTo,
			@ApiParam(value = "Quest detail in JSON", required = true) QuestModel questModel) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"POST " + "gamification/configurator/quest/" + configId + "/" + listenTo, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				if (!configAccess.isConfigIdExist(conn, configId)) {
					objResponse.put("message", "Cannot register quest. Configuration not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				try {
					configAccess.createQuest(conn, questModel);
					configAccess.addElementToMapping(conn, configId, questModel.getQuestId(), listenTo, "quest");
					notifyObservers(configId);
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot register and create quest. Failed to upload " + questModel.getQuestId() + ". " + e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString()).build();
				}
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15, "" + randomLong, true);
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_24, "" + name, true);
				return Response.status(HttpURLConnection.HTTP_CREATED).entity(objResponse.toJSONString()).build();
			} catch (SQLException e1) {
				e1.printStackTrace();
				objResponse.put("message",
						"Cannot register quest. Cannot check whether quest ID exist or not. Database error. "
								+ e1.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot register quest. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.type(MediaType.APPLICATION_JSON).build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Get a registered quest for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param questId  to be worked on
	 * @return HTTP Response returned as JSON object
	 */
	@GET
	@Path("/quest/{configId}/{questId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Retrieved quest successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "getQuest", notes = "Get a quest")
	public Response getQuest(@PathParam("configId") String configId, @PathParam("questId") String questId) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"GET " + "gamification/configurator/quest/" + configId + "/" + questId, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				try {
					if (!configAccess.isConfigIdExist(conn, configId)) {
						objResponse.put("message", "Cannot get quest. Configuration not found");
						Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
								(String) objResponse.get("message"));
						return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
								.build();
					}
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot get quest. Cannot check whether quest ID exist or not. Database error. "
									+ e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
							.build();
				}
				if (!configAccess.isQuestIdExist(conn, questId)) {
					objResponse.put("message", "Cannot get quest. Quest not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				QuestModel quest = configAccess.getQuestWithId(conn, questId);
				if(quest == null){
					objResponse.put("message", "Quest Null, Cannot find quest with " + questId);
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString()).type(MediaType.APPLICATION_JSON).build();
				}
				//TODO parse to JSON
				ObjectMapper mapper = new ObjectMapper();
				mapper.enable(SerializationFeature.INDENT_OUTPUT);
				String questString = mapper.writeValueAsString(quest);
				//
				Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_17, ""+randomLong, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_26, ""+name, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_27, ""+questId, true);
				return Response.status(HttpURLConnection.HTTP_OK).entity(questString).type(MediaType.APPLICATION_JSON).build();
			} catch (SQLException e) {
				e.printStackTrace();
				objResponse.put("message",
						"Cannot get quest. Cannot check whether quest ID exist or not. Database error. "
								+ e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				objResponse.put("message", "Cannot get quest detail. JSON processing error. " + e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(e.getMessage())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot get quest. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Update a registered quest for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param questId  to be worked on
	 * @param questModel Quest in JSON
	 * @return HTTP Response returned as JSON object
	 */
	@PUT
	@Path("/quest/{configId}/{questId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Updated quest successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "updateQuest", notes = "Update a quest")
	public Response updateQuest(
			@PathParam("configId") String configId,
			@PathParam("questId") String questId,
			@ApiParam(value = "Quest detail in JSON", required = true) QuestModel questModel) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"PUT " + "gamification/configurator/quest/" + configId + "/" + questId, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				try {
					if (!configAccess.isConfigIdExist(conn, configId)) {
						objResponse.put("message", "Cannot update quest. Configuration not found");
						Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
								(String) objResponse.get("message"));
						return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
								.build();
					}
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot update quest. Cannot check whether quest ID exist or not. Database error. "
									+ e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
							.build();
				}
				if (!configAccess.isQuestIdExist(conn, questId)) {
					objResponse.put("message", "Cannot update quest. Quest not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				configAccess.updateQuest(conn, questModel);
				objResponse.put("message", "Quest updated");
				Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_17, ""+randomLong, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_26, ""+name, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_27, ""+questId, true);
				return Response.status(HttpURLConnection.HTTP_OK).entity(objResponse).build();
			} catch (SQLException e) {
				e.printStackTrace();
				objResponse.put("message",
						"Cannot update quest. Cannot check whether quest ID exist or not. Database error. "
								+ e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot update quest. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Update a registered quest for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param questId  to be worked on
	 * @return HTTP Response returned as JSON object
	 */
	@DELETE
	@Path("/quest/{configId}/{questId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Deleted quest successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "deleteQuest", notes = "Delete and unregister a quest from a configuration")
	public Response deleteQuest(@PathParam("configId") String configId, @PathParam("questId") String questId) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"DELETE " + "gamification/configurator/quest/" + configId + "/" + questId, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				try {
					if (!configAccess.isConfigIdExist(conn, configId)) {
						objResponse.put("message", "Cannot delete quest. Configuration not found");
						Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
								(String) objResponse.get("message"));
						return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
								.build();
					}
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot delete quest. Cannot check whether quest ID exist or not. Database error. "
									+ e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
							.build();
				}
				if (!configAccess.isQuestIdExist(conn, questId)) {
					objResponse.put("message", "Cannot delete quest. Quest not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				configAccess.deleteQuest(conn, questId);
				configAccess.removeElementFromMapping(conn, configId, questId, "quest");
				notifyObservers(configId);
				objResponse.put("message", "Quest deleted");
				Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_17, ""+randomLong, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_26, ""+name, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_27, ""+questId, true);
				return Response.status(HttpURLConnection.HTTP_OK).entity(objResponse).build();
			} catch (SQLException e) {
				e.printStackTrace();
				objResponse.put("message",
						"Cannot delete quest. Cannot check whether quest ID exist or not. Database error. "
								+ e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot delete quest. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Register a achievement for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param lsitenTo String the Listener should listen to
	 * @param achievementModel Achievement in JSON
	 * @return HTTP Response returned as JSON object
	 */
	@POST
	@Path("/achievement/{configId}/{listenTo}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Created achievement successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "registerAchievement", notes = "Create and register an achievement to a configuration")
	public Response registerAchievement(
			@PathParam("configId") String configId,
			@PathParam("listenTo") String listenTo,
			@ApiParam(value = "Achievement detail in JSON", required = true) AchievementModel achievementModel) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"POST " + "gamification/configurator/achievement/" + configId + "/" + listenTo, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				if (!configAccess.isConfigIdExist(conn, configId)) {
					objResponse.put("message", "Cannot register achievement. Configuration not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				try {
					configAccess.createAchhievement(conn, achievementModel);
					configAccess.addElementToMapping(conn, configId, achievementModel.getAchievementId(), listenTo, "achievement");
					notifyObservers(configId);
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot register and create achievement. Failed to upload " + achievementModel.getAchievementId() + ". " + e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString()).build();
				}
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15, "" + randomLong, true);
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_24, "" + name, true);
				return Response.status(HttpURLConnection.HTTP_CREATED).entity(objResponse.toJSONString()).build();
			} catch (SQLException e1) {
				e1.printStackTrace();
				objResponse.put("message",
						"Cannot register achievement. Cannot check whether achievement ID exist or not. Database error. "
								+ e1.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot register achievement. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.type(MediaType.APPLICATION_JSON).build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Get a registered achievement for a given configuration
	 * 
	 * @param configId      Config ID obtained from LMS
	 * @param achievementId to be worked on
	 * @return HTTP Response returned as JSON object
	 */
	@GET
	@Path("/achievement/{configId}/{achievementId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Retrieved achievement successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "getAchievement", notes = "Get an achievement")
	public Response getAchievement(@PathParam("configId") String configId,
			@PathParam("achievementId") String achievementId) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"GET " + "gamification/configurator/achievement/" + configId + "/" + achievementId, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				try {
					if (!configAccess.isConfigIdExist(conn, configId)) {
						objResponse.put("message", "Cannot get achievement. Configuration not found");
						Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
								(String) objResponse.get("message"));
						return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
								.build();
					}
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot get achievement. Cannot check whether config ID exist or not. Database error. "
									+ e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
							.build();
				}
				if (!configAccess.isAchievementIdExist(conn, achievementId)) {
					objResponse.put("message", "Cannot get achievement. Achievement not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				AchievementModel achievement = configAccess.getAchievementWithId(conn, achievementId);
				if(achievement == null){
					objResponse.put("message", "Achievement Null, Cannot find achievement with " + achievementId);
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString()).type(MediaType.APPLICATION_JSON).build();
				}
				//TODO parse to JSON
				ObjectMapper mapper = new ObjectMapper();
				mapper.enable(SerializationFeature.INDENT_OUTPUT);
				String achievementString = mapper.writeValueAsString(achievement);
				//
				Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_17, ""+randomLong, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_26, ""+name, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_27, ""+achievementId, true);
				return Response.status(HttpURLConnection.HTTP_OK).entity(achievementString).type(MediaType.APPLICATION_JSON).build();
			} catch (SQLException e) {
				e.printStackTrace();
				objResponse.put("message",
						"Cannot get achievement. Cannot check whether achievement ID exist or not. Database error. "
								+ e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				objResponse.put("message", "Cannot get achievement detail. JSON processing error. " + e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(e.getMessage())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot get achievement. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Update a registered achievement for a given configuration
	 * 
	 * @param configId      Config ID obtained from LMS
	 * @param achievementId to be worked on
	 * @param achievementModel Achievement in JSON
	 * @return HTTP Response returned as JSON object
	 */
	@PUT
	@Path("/achievement/{configId}/{achievementId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Updated achievement successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "updateAchievement", notes = "Update an achievement")
	public Response updateAchievement(
			@PathParam("configId") String configId,
			@PathParam("achievementId") String achievementId,
			@ApiParam(value = "Achievement detail in JSON", required = true) AchievementModel achievementModel) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"PUT " + "gamification/configurator/achievement/" + configId + "/" + achievementId, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				try {
					if (!configAccess.isConfigIdExist(conn, configId)) {
						objResponse.put("message", "Cannot update achievement. Configuration not found");
						Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
								(String) objResponse.get("message"));
						return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
								.build();
					}
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot update achievement. Cannot check whether config ID exist or not. Database error. "
									+ e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
							.build();
				}
				if (!configAccess.isAchievementIdExist(conn, achievementId)) {
					objResponse.put("message", "Cannot update achievement. Achievement not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				configAccess.updateAchievement(conn, achievementModel);
				objResponse.put("message", "Achievement updated");
				Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_17, ""+randomLong, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_26, ""+name, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_27, ""+achievementId, true);
				return Response.status(HttpURLConnection.HTTP_OK).entity(objResponse).build();
			} catch (SQLException e) {
				e.printStackTrace();
				objResponse.put("message",
						"Cannot update achievement. Cannot check whether achievement ID exist or not. Database error. "
								+ e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot update achievement. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Update a registered achievement for a given configuration
	 * 
	 * @param configId      Config ID obtained from LMS
	 * @param achievementId to be worked on
	 * @return HTTP Response returned as JSON object
	 */
	@DELETE
	@Path("/achievement/{configId}/{achievementId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Deleted achievement successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "deleteAchievement", notes = "Delete and unregister an achievement from a configuration")
	public Response deleteAchievement(@PathParam("configId") String configId,
			@PathParam("achievementId") String achievementId) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"DELETE " + "gamification/configurator/achievement/" + configId + "/" + achievementId, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				try {
					if (!configAccess.isConfigIdExist(conn, configId)) {
						objResponse.put("message", "Cannot delete achievement. Configuration not found");
						Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
								(String) objResponse.get("message"));
						return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
								.build();
					}
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot delete achievement. Cannot check whether config ID exist or not. Database error. "
									+ e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
							.build();
				}
				if (!configAccess.isAchievementIdExist(conn, achievementId)) {
					objResponse.put("message", "Cannot delete achievement. Achievement not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				configAccess.deleteAchievement(conn, achievementId);
				configAccess.removeElementFromMapping(conn, configId, achievementId, "achievement");
				notifyObservers(configId);
				objResponse.put("message", "Achievement deleted");
				Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_17, ""+randomLong, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_26, ""+name, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_27, ""+achievementId, true);
				return Response.status(HttpURLConnection.HTTP_OK).entity(objResponse).build();
			} catch (SQLException e) {
				e.printStackTrace();
				objResponse.put("message",
						"Cannot delete achievement. Cannot check whether achievement ID exist or not. Database error. "
								+ e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot delete achievement. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Register a badge for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param lsitenTo String the Listener should listen to
	 * @param badgeModel Badge in JSON
	 * @return HTTP Response returned as JSON object
	 */
	@POST
	@Path("/badge/{configId}/{listenTo}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Created badge successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "registerBadge", notes = "Create and register a badge to a configuration")
	public Response registerBadge(
			@PathParam("configId") String configId,
			@PathParam("listenTo") String listenTo,
			@ApiParam(value = "Badge detail in JSON", required = true) BadgeModel badgeModel) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"POST " + "gamification/configurator/badge/" + configId + "/" + listenTo, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				if (!configAccess.isConfigIdExist(conn, configId)) {
					objResponse.put("message", "Cannot register badge. Configuration not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				try {
					configAccess.createBadge(conn, badgeModel);
					configAccess.addElementToMapping(conn,configId,badgeModel.getBadgeId(), listenTo, "badge");
					notifyObservers(configId);
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot register and create badge. Failed to upload " + badgeModel.getBadgeId() + ". " + e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString()).build();
				}
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15, "" + randomLong, true);
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_24, "" + name, true);
				return Response.status(HttpURLConnection.HTTP_CREATED).entity(objResponse.toJSONString()).build();
			} catch (SQLException e1) {
				e1.printStackTrace();
				objResponse.put("message",
						"Cannot register badge. Cannot check whether badge ID exist or not. Database error. "
								+ e1.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot register badge. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.type(MediaType.APPLICATION_JSON).build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Get a registered badge for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param badgeId  to be worked on
	 * @return HTTP Response returned as JSON object
	 */
	@GET
	@Path("/badge/{configId}/{badgeId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Retrieved badge successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "getBadge", notes = "Get a badge")
	public Response getBadge(@PathParam("configId") String configId, @PathParam("badgeId") String badgeId) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"GET " + "gamification/configurator/badge/" + configId + "/" + badgeId, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				try {
					if (!configAccess.isConfigIdExist(conn, configId)) {
						objResponse.put("message", "Cannot get badge. Configuration not found");
						Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
								(String) objResponse.get("message"));
						return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
								.build();
					}
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot get badge. Cannot check whether config ID exist or not. Database error. "
									+ e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
							.build();
				}
				if (!configAccess.isBadgeIdExist(conn, badgeId)) {
					objResponse.put("message", "Cannot get badge. Badge not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				BadgeModel badge = configAccess.getBadgeWithId(conn, badgeId);
				if(badge == null){
					objResponse.put("message", "Badge Null, Cannot find badge with " + badgeId);
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString()).type(MediaType.APPLICATION_JSON).build();
				}
				//TODO parse to JSON
				ObjectMapper mapper = new ObjectMapper();
				mapper.enable(SerializationFeature.INDENT_OUTPUT);
				String badgeString = mapper.writeValueAsString(badge);
				//
				Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_17, ""+randomLong, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_26, ""+name, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_27, ""+badgeId, true);
				return Response.status(HttpURLConnection.HTTP_OK).entity(badgeString).type(MediaType.APPLICATION_JSON).build();
			} catch (SQLException e) {
				e.printStackTrace();
				objResponse.put("message",
						"Cannot get badge. Cannot check whether badge ID exist or not. Database error. "
								+ e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				objResponse.put("message", "Cannot get badge detail. JSON processing error. " + e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(e.getMessage())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot get badge. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Update a registered badge for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param badgeId  to be worked on
	 * @param badgeModel Badge in JSON
	 * @return HTTP Response returned as JSON object
	 */
	@PUT
	@Path("/badge/{configId}/{badgeId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Updated badge successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "updateBadge", notes = "Update a badge")
	public Response updateBadge(
			@PathParam("configId") String configId,
			@PathParam("badgeId") String badgeId,
			@ApiParam(value = "Badge detail in JSON", required = true) BadgeModel badgeModel) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"PUT " + "gamification/configurator/badge/" + configId + "/" + badgeId, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				try {
					if (!configAccess.isConfigIdExist(conn, configId)) {
						objResponse.put("message", "Cannot update badge. Configuration not found");
						Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
								(String) objResponse.get("message"));
						return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
								.build();
					}
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot update badge. Cannot check whether config ID exist or not. Database error. "
									+ e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
							.build();
				}
				if (!configAccess.isBadgeIdExist(conn, badgeId)) {
					objResponse.put("message", "Cannot update badge. Badge not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				configAccess.updateBadge(conn, badgeModel);
				objResponse.put("message", "Badge updated");
				Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_17, ""+randomLong, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_26, ""+name, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_27, ""+badgeId, true);
				return Response.status(HttpURLConnection.HTTP_OK).entity(objResponse).build();
			} catch (SQLException e) {
				e.printStackTrace();
				objResponse.put("message",
						"Cannot update badge. Cannot check whether badge ID exist or not. Database error. "
								+ e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot update badge. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Update a registered badge for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param badgeId  to be worked on
	 * @return HTTP Response returned as JSON object
	 */
	@DELETE
	@Path("/badge/{configId}/{badgeId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Deleted badge successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "deleteBadge", notes = "Delete and unregister a badge from a configuration")
	public Response deleteBadge(@PathParam("configId") String configId, @PathParam("badgeId") String badgeId) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"DELETE " + "gamification/configurator/badge/" + configId + "/" + badgeId, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				try {
					if (!configAccess.isConfigIdExist(conn, configId)) {
						objResponse.put("message", "Cannot delete badge. Configuration not found");
						Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
								(String) objResponse.get("message"));
						return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
								.build();
					}
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot delete badge. Cannot check whether config ID exist or not. Database error. "
									+ e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
							.build();
				}
				if (!configAccess.isBadgeIdExist(conn, badgeId)) {
					objResponse.put("message", "Cannot delete badge. Badge not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				configAccess.deleteBadge(conn, badgeId);
				configAccess.removeElementFromMapping(conn, configId, badgeId, "badge");
				notifyObservers(configId);
				objResponse.put("message", "Badge deleted");
				Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_17, ""+randomLong, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_26, ""+name, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_27, ""+badgeId, true);
				return Response.status(HttpURLConnection.HTTP_OK).entity(objResponse).build();
			} catch (SQLException e) {
				e.printStackTrace();
				objResponse.put("message",
						"Cannot delete badge. Cannot check whether badge ID exist or not. Database error. "
								+ e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot delete badge. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Register a action for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param lsitenTo String the Listener should listen to
	 * @param actionModel Action in JSON
	 * @return HTTP Response returned as JSON object
	 */
	@POST
	@Path("/action/{configId}/{listenTo}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Created action successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "registerAction", notes = "Create and register an action to a configuration")
	public Response registerAction(
			@PathParam("configId") String configId,
			@PathParam("listenTo") String listenTo,
			@ApiParam(value = "Action detail in JSON", required = true) ActionModel actionModel) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"POST " + "gamification/configurator/action/" + configId + "/" + listenTo, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				if (!configAccess.isConfigIdExist(conn, configId)) {
					objResponse.put("message", "Cannot register action. Configuration not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				try {
					configAccess.createAction(conn, actionModel);
					configAccess.addElementToMapping(conn, configId, actionModel.getActionId(), listenTo, "action");
					notifyObservers(configId);
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot register and create action. Failed to upload " + actionModel.getActionId() + ". " + e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString()).build();
				}
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15, "" + randomLong, true);
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_24, "" + name, true);
				return Response.status(HttpURLConnection.HTTP_CREATED).entity(objResponse.toJSONString()).build();
			} catch (SQLException e1) {
				e1.printStackTrace();
				objResponse.put("message",
						"Cannot register action. Cannot check whether action ID exist or not. Database error. "
								+ e1.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot register action. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.type(MediaType.APPLICATION_JSON).build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Get a registered action for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param actionId to be worked on
	 * @return HTTP Response returned as JSON object
	 */
	@GET
	@Path("/action/{configId}/{actionId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Retrieved action successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "getAction", notes = "Get an action")
	public Response getAction(@PathParam("configId") String configId, @PathParam("actionId") String actionId) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"GET " + "gamification/configurator/action/" + configId + "/" + actionId, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				try {
					if (!configAccess.isConfigIdExist(conn, configId)) {
						objResponse.put("message", "Cannot get action. Configuration not found");
						Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
								(String) objResponse.get("message"));
						return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
								.build();
					}
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot get action. Cannot check whether config ID exist or not. Database error. "
									+ e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
							.build();
				}
				if (!configAccess.isActionIdExist(conn, actionId)) {
					objResponse.put("message", "Cannot get action. Action not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				ActionModel action = configAccess.getActionWithId(conn, actionId);
				if(action == null){
					objResponse.put("message", "Action Null, Cannot find action with " + actionId);
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString()).type(MediaType.APPLICATION_JSON).build();
				}
				//TODO parse to JSON
				ObjectMapper mapper = new ObjectMapper();
				mapper.enable(SerializationFeature.INDENT_OUTPUT);
				String actionString = mapper.writeValueAsString(action);
				//
				Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_17, ""+randomLong, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_26, ""+name, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_27, ""+actionId, true);
				return Response.status(HttpURLConnection.HTTP_OK).entity(actionString).type(MediaType.APPLICATION_JSON).build();
			} catch (SQLException e) {
				e.printStackTrace();
				objResponse.put("message",
						"Cannot get action. Cannot check whether action ID exist or not. Database error. "
								+ e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				objResponse.put("message", "Cannot get action detail. JSON processing error. " + e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(e.getMessage())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot register action. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Update a registered action for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param actionId to be worked on
	 * @param actionModel Action in JSON
	 * @return HTTP Response returned as JSON object
	 */
	@PUT
	@Path("/action/{configId}/{actionId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Updated action successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "updateAction", notes = "Update an action")
	public Response updateAction(
			@PathParam("configId") String configId,
			@PathParam("actionId") String actionId,
			@ApiParam(value = "Action detail in JSON", required = true) ActionModel actionModel) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"PUT " + "gamification/configurator/action/" + configId + "/" + actionId, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				try {
					if (!configAccess.isConfigIdExist(conn, configId)) {
						objResponse.put("message", "Cannot update action. Configuration not found");
						Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
								(String) objResponse.get("message"));
						return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
								.build();
					}
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot update action. Cannot check whether config ID exist or not. Database error. "
									+ e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
							.build();
				}
				if (!configAccess.isActionIdExist(conn, actionId)) {
					objResponse.put("message", "Cannot update action. Action not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				configAccess.updateAction(conn, actionModel);
				objResponse.put("message", "Action updated");
				Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_17, ""+randomLong, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_26, ""+name, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_27, ""+actionId, true);
				return Response.status(HttpURLConnection.HTTP_OK).entity(objResponse).build();
			} catch (SQLException e) {
				e.printStackTrace();
				objResponse.put("message",
						"Cannot update action. Cannot check whether action ID exist or not. Database error. "
								+ e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot update action. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Update a registered action for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param actionId to be worked on
	 * @return HTTP Response returned as JSON object
	 */
	@DELETE
	@Path("/action/{configId}/{actionId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Deleted action successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "deleteAction", notes = "Delete and unregister an action from a configuration")
	public Response deleteAction(@PathParam("configId") String configId, @PathParam("actionId") String actionId) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"DELETE " + "gamification/configurator/action/" + configId + "/" + actionId, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				try {
					if (!configAccess.isConfigIdExist(conn, configId)) {
						objResponse.put("message", "Cannot delete action. Configuration not found");
						Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
								(String) objResponse.get("message"));
						return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
								.build();
					}
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot delete action. Cannot check whether config ID exist or not. Database error. "
									+ e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
							.build();
				}
				if (!configAccess.isActionIdExist(conn, actionId)) {
					objResponse.put("message", "Cannot delete action. Action not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				configAccess.deleteAction(conn, actionId);
				configAccess.removeElementFromMapping(conn, configId, actionId, "action");
				notifyObservers(configId);
				objResponse.put("message", "Action deleted");
				Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_17, ""+randomLong, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_26, ""+name, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_27, ""+actionId, true);
				return Response.status(HttpURLConnection.HTTP_OK).entity(objResponse).build();
			} catch (SQLException e) {
				e.printStackTrace();
				objResponse.put("message",
						"Cannot delete action. Cannot check whether action ID exist or not. Database error. "
								+ e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot delete action. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Register a level for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param lsitenTo String the Listener should listen to
	 * @param levelModel Level in JSON
	 * @return HTTP Response returned as JSON object
	 */
	@POST
	@Path("/level/{configId}/{listenTo}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Created level successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "registerLevel", notes = "Create and register a level to a configuration")
	public Response registerLevel(
			@PathParam("configId") String configId,
			@PathParam("listenTo") String listenTo,
			@ApiParam(value = "Level detail in JSON", required = true) LevelModel levelModel) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"POST " + "gamification/configurator/level/" + configId + "/" + listenTo, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				if (!configAccess.isConfigIdExist(conn, configId)) {
					objResponse.put("message", "Cannot register level. Configuration not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				try {
					configAccess.createLevel(conn, levelModel);
					Integer levelId = levelModel.getLevelNumber();
					configAccess.addElementToMapping(conn, configId, levelId.toString(), listenTo, "level");
					notifyObservers(configId);
					
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot register and create level. Failed to upload " + levelModel + ". " + e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString()).build();
				}
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15, "" + randomLong, true);
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_24, "" + name, true);
				return Response.status(HttpURLConnection.HTTP_CREATED).entity(objResponse.toJSONString()).build();
			} catch (SQLException e1) {
				e1.printStackTrace();
				objResponse.put("message",
						"Cannot register level. Cannot check whether level ID exist or not. Database error. "
								+ e1.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot register level. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.type(MediaType.APPLICATION_JSON).build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Get a registered level for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param levelId  to be worked on
	 * @return HTTP Response returned as JSON object
	 */
	@GET
	@Path("/level/{configId}/{levelId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Retrieved level successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "getLevel", notes = "Get a level")
	public Response getLevel(@PathParam("configId") String configId, @PathParam("levelId") String levelId) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"GET " + "gamification/configurator/level/" + configId + "/" + levelId, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				try {
					if (!configAccess.isConfigIdExist(conn, configId)) {
						objResponse.put("message", "Cannot get level. Configuration not found");
						Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
								(String) objResponse.get("message"));
						return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
								.build();
					}
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot get level. Cannot check whether config ID exist or not. Database error. "
									+ e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
							.build();
				}
				if (!configAccess.isLevelIdExist(conn, levelId)) {
					objResponse.put("message", "Cannot get level. Level not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				LevelModel level = configAccess.getLevelWithId(conn, Integer.parseInt(levelId));
				if(level == null){
					objResponse.put("message", "Level Null, Cannot find level with " + levelId);
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString()).type(MediaType.APPLICATION_JSON).build();
				}
				//TODO parse to JSON
				ObjectMapper mapper = new ObjectMapper();
				mapper.enable(SerializationFeature.INDENT_OUTPUT);
				String levelString = mapper.writeValueAsString(level);
			
				Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_17, ""+randomLong, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_26, ""+name, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_27, ""+levelId, true);
				return Response.status(HttpURLConnection.HTTP_OK).entity(levelString).type(MediaType.APPLICATION_JSON).build();
			} catch (SQLException e) {
				e.printStackTrace();
				objResponse.put("message",
						"Cannot get level. Cannot check whether level ID exist or not. Database error. "
								+ e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				objResponse.put("message", "Cannot get level detail. JSON processing error. " + e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(e.getMessage())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot get level. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Update a registered level for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param levelId  to be worked on
	 * @param levelModel Level in JSON
	 * @return HTTP Response returned as JSON object
	 */
	@PUT
	@Path("/level/{configId}/{levelId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Updated level successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "updateLevel", notes = "Update a level")
	public Response updateLevel(
			@PathParam("configId") String configId,
			@PathParam("levelId") String levelId,
			@ApiParam(value = "Level detail in JSON", required = true) LevelModel levelModel) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"PUT " + "gamification/configurator/level/" + configId + "/" + levelId, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				try {
					if (!configAccess.isConfigIdExist(conn, configId)) {
						objResponse.put("message", "Cannot update level. Configuration not found");
						Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
								(String) objResponse.get("message"));
						return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
								.build();
					}
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot update level. Cannot check whether config ID exist or not. Database error. "
									+ e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
							.build();
				}
				if (!configAccess.isLevelIdExist(conn, levelId)) {
					objResponse.put("message", "Cannot update level. Level not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				configAccess.updateLevel(conn, levelModel);
				objResponse.put("message", "Level updated");
				Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_17, ""+randomLong, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_26, ""+name, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_27, ""+levelId, true);
				return Response.status(HttpURLConnection.HTTP_OK).entity(objResponse).build();
			} catch (SQLException e) {
				e.printStackTrace();
				objResponse.put("message",
						"Cannot update level. Cannot check whether level ID exist or not. Database error. "
								+ e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot update level. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Update a registered level for a given configuration
	 * 
	 * @param configId Config ID obtained from LMS
	 * @param levelId  to be worked on
	 * @return HTTP Response returned as JSON object
	 */
	@DELETE
	@Path("/level/{configId}/{levelId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Deleted level successfully"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Config not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"), })
	@ApiOperation(value = "deleteLevel", notes = "Delete and unregister a level from a configuration")
	public Response deleteLevel(@PathParam("configId") String configId, @PathParam("levelId") String levelId) {

		// Request log
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"DELETE " + "gamification/configurator/level/" + configId + "/" + levelId, true);
		long randomLong = new Random().nextLong(); // To be able to match

		UserAgent userAgent = (UserAgent) Context.getCurrent().getMainAgent();
		String name = userAgent.getLoginName();
		if (name.equals("anonymous")) {
			return unauthorizedMessage();
		}
		Connection conn = null;
		JSONObject objResponse = new JSONObject();
		try {
			conn = dbm.getConnection();
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				try {
					if (!configAccess.isConfigIdExist(conn, configId)) {
						objResponse.put("message", "Cannot delete level. Configuration not found");
						Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
								(String) objResponse.get("message"));
						return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
								.build();
					}
				} catch (SQLException e) {
					e.printStackTrace();
					objResponse.put("message",
							"Cannot delete level. Cannot check whether config ID exist or not. Database error. "
									+ e.getMessage());
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
							.build();
				}
				if (!configAccess.isLevelIdExist(conn, levelId)) {
					objResponse.put("message", "Cannot delete level. Level not found");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString())
							.build();
				}
				configAccess.deleteLevel(conn, levelId);
				configAccess.removeElementFromMapping(conn, configId, levelId, "level");
				notifyObservers(configId);
				objResponse.put("message", "Level deleted");
				Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_17, ""+randomLong, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_26, ""+name, true);
		    	Context.getCurrent().monitorEvent(this,MonitoringEvent.SERVICE_CUSTOM_MESSAGE_27, ""+levelId, true);
				return Response.status(HttpURLConnection.HTTP_OK).entity(objResponse).build();
			} catch (SQLException e) {
				e.printStackTrace();
				objResponse.put("message",
						"Cannot delete level. Cannot check whether level ID exist or not. Database error. "
								+ e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message", "Cannot delete level. Failed to get connection. " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}


	private void notifyObservers(String configId){
		for(Map.Entry<String, URL> entry : observers.entrySet()){
			if (entry.getKey().equals(configId)) {
				HttpURLConnection connection = null;
				try {
					URL url = entry.getValue();
					connection = (HttpURLConnection) url.openConnection();
					connection.setDoInput(true);
					connection.setDoOutput(true);
					connection.setRequestMethod("POST");
					connection.setRequestProperty("Content-Type", "application/json; utf-8");
					String jsonInputString = "{\"changedmapping\":\"true\"";
					try (OutputStream os = connection.getOutputStream()) {
						byte[] input = jsonInputString.getBytes("utf-8");
						os.write(input, 0, input.length);
					}
					catch (IOException e){
						e.printStackTrace();
						Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
								"Failed to notify observers of mapping changes" + e.getMessage());
					}
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_25,
							"Notified observer" + url + "with status" + connection.getResponseCode());
				}catch (Exception e) {
					e.printStackTrace();
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							"Failed to notify observers of mapping changes" + e.getMessage());
				}finally {	
					if (connection != null) {
					connection.disconnect();
					}
				}
			}
		}
	}


	/**
	 * Notifies registers observers in case mapping changed
	 * 
	 * @param configId config
	 * @param jsonObject where key should be "url" and value the url address to be notified on changes
	 * @return Response
	 */
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("register/{configId}")
	@ApiOperation(value = "registerObserver", notes = "Register an observer to a configuration")
	public Response registerObserver(@PathParam("configId") String configId,
			@ApiParam(value = "Observer url in JSON", required = true) JSONObject jsonObject) {
		
		long randomLong = new Random().nextLong(); // To be able to match
		Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_99,
				"POST " + "gamification/configurator/register/" + configId, true);
		JSONObject objResponse = new JSONObject();
		URL url = null;
		Connection conn = null;
		try {
			conn = dbm.getConnection();

			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, "" + randomLong, true);
			try {
				if (!configAccess.isConfigIdExist(conn, configId)) {
					objResponse.put("message",
							"Cannot register observer to confiId " + configId + ". Cufigration ID already exist!");
					Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
							(String) objResponse.get("message"));
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
							.build();
				}
			} catch (SQLException e) {
				e.printStackTrace();
				objResponse.put("message", "Cannot register observer to configId " + configId + ". " + e.getMessage());
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
						.type(MediaType.APPLICATION_JSON).build();
			}
			if (jsonObject == null) {
				objResponse.put("message", "Cannot register observer. No data received");
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString()).build();
			}
			String receivedURL = jsonObject.get("url").toString();
			if (receivedURL == null) {
				objResponse.put("message", "Cannot register observer. Url is empty");
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString()).build();
			}
			try {
				url = new URL(receivedURL);
			} catch (MalformedURLException e) {
				e.printStackTrace();
				objResponse.put("message", "Cannot register observer. Url has wrong format");
				Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR,
						(String) objResponse.get("message"));
				return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(objResponse.toJSONString()).build();
			}
			observers.put(configId, url);
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_15, "" + randomLong, true);
			return Response.status(HttpURLConnection.HTTP_OK).build();
		} catch (SQLException e) {
			e.printStackTrace();
			objResponse.put("message",
					"Cannot register observer. Failed to check " + configId + ". " + e.getMessage());
			Context.getCurrent().monitorEvent(this, MonitoringEvent.SERVICE_ERROR, (String) objResponse.get("message"));
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(objResponse.toJSONString())
					.type(MediaType.APPLICATION_JSON).build();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}
}
