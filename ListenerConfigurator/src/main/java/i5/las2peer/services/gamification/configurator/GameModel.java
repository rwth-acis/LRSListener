package i5.las2peer.services.gamification.configurator;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
* Game Model
* 
* This is the class to store the Game model
* 
*/

@ApiModel( value = "GameModel", description = "Game data resource representation" )
public class GameModel{
	@ApiModelProperty( value = "Game ID", required = true ) 
	private String gameId;
	@ApiModelProperty( value = "Game description", required = true ) 
	private String description;
	@ApiModelProperty( value = "Community type of game", required = true ) 
	private String communityType;
	/**
	 * @return the gameId
	 */
	public String getGameId() {
		return gameId;
	}
	/**
	 * @param gameId the gameId to set
	 */
	public void setGameId(String gameId) {
		this.gameId = gameId;
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
	/**
	 * @return the communityType
	 */
	public String getCommunityType() {
		return communityType;
	}
	/**
	 * @param communityType the communityType to set
	 */
	public void setCommunityType(String communityType) {
		this.communityType = communityType;
	}
	
	

}
