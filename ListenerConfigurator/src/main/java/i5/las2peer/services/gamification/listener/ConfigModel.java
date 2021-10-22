package i5.las2peer.services.gamification.listener;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Config Model
 * 
 * This is the class to store the Config model
 * 
 */
@ApiModel(value = "ConfigModel", description = "Config resource representation")
public class ConfigModel {

	@ApiModelProperty(value = "Config ID", required = true)
	private String configId;
	@ApiModelProperty(value = "Config name", required = true)
	private String name;
	@ApiModelProperty(value = "Config description")
	private String description;
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
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}


}
