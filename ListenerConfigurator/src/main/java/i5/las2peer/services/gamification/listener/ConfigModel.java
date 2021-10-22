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

	public ConfigModel(String configId, String name, String desc) {
		this.configId = configId;
		this.name = name;
		this.description = desc;
	}

	public String getConfigId() {
		return configId;
	}

	public void setConfigId(String configId) {
		this.configId = configId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
