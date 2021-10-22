package i5.las2peer.services.gamification.listener;

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
	private String gameDesc;
	@ApiModelProperty( value = "Community type of game", required = true ) 
	private String communityType;
	
	
	public String getGameId() {
		return gameId;
	}
	public void setGameId(String gameId) {
		this.gameId = gameId;
	}
	public String getGameDesc() {
		return gameDesc;
	}
	public void setGameDesc(String gameDesc) {
		this.gameDesc = gameDesc;
	}
	public String getCommunityType() {
		return communityType;
	}
	public void setCommunityType(String communityType) {
		this.communityType = communityType;
	}
}
