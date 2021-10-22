package i5.las2peer.services.gamification.listener;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
* Level Model
* 
* This is the class to store the Level model
* 
*/

@ApiModel( value = "LevelModel", description = "Level resource representation" )
public class LevelModel{

	@ApiModelProperty( value = "Level number", required = true ) 
	private int levelId;
	@ApiModelProperty( value = "Level name", required = true ) 
	private String name;
	@ApiModelProperty( value = "Level point value threshold") 
	private int pointValue = 0;
	@ApiModelProperty( value = "Use notification status", required = true ) 
	private boolean useNotification;
	@ApiModelProperty( value = "Notification Message") 
	private String notifMessage;

	
	/**
	 * Getter for variable level number
	 * 
	 * @return level_num level number
	 */
	public int getNumber(){
		return levelId;
	}
	
	/**
	 * Setter for variable level number
	 * 
	 * @param level_num level number
	 */
	public void setNumber(int level_num){
		this.levelId = level_num;
	}
	
	/**
	 * Getter for variable name
	 * 
	 * @return name of a level
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * Setter for variable name
	 * 
	 * @param name name of a level
	 */
	public void setName(String name){
		this.name = name;
	}
	
	
	/**
	 * Getter for point value
	 * 
	 * @return point value of a level
	 */
	public int getPointValue(){
		return this.pointValue;
	}
	
	/**
	 * Setter for point value
	 * 
	 * @param point_value point value of a level
	 */
	public void setPointValue(int point_value){
		this.pointValue = point_value;
	}

	/**
	 * Getter for use notification status
	 * 
	 * @return use notification status
	 */
	public boolean isUseNotification(){
		return this.useNotification;
	}
	
	/**
	 * Setter for use notification status
	 * 
	 * @param use_notification use notification status
	 */
	public void useNotification(boolean use_notification){
		this.useNotification = use_notification;
	}
	
	/**
	 * Getter for notification message
	 * 
	 * @return notification message
	 */
	public String getNotificationMessage(){
		return this.notifMessage;
	}
	
	/**
	 * Setter for notification message
	 * 
	 * @param notif_message notification message of a badge
	 */
	public void setNotificationMessage(String notif_message){
		this.notifMessage = notif_message;
	}
}
