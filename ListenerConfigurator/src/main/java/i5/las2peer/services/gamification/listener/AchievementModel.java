package i5.las2peer.services.gamification.listener;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
* Achievement Model
* 
* This is the class to store the Achievement model
* 
*/


@ApiModel( value = "AchievementModel", description = "Achievement resource representation" )
public class AchievementModel{

	@ApiModelProperty( value = "Achievement ID", required = true ) 
	private String achievementId;
	@ApiModelProperty( value = "Achievement name", required = true ) 
	private String name;
	@ApiModelProperty( value = "Achievement description") 
	private String description;
	@ApiModelProperty( value = "Achievement point value") 
	private int pointValue;
	@ApiModelProperty( value = "Achievement badge") 
	private String badgeId;
	@ApiModelProperty( value = "Use notification status", required = true ) 
	private boolean useNotification;
	@ApiModelProperty( value = "Notification Message") 
	private String notifMessage;
	
	
	
	/**
	 * Getter for variable id
	 * 
	 * @return id of an achievement
	 */
	public String getId(){
		return achievementId;
	}
	
	/**
	 * Setter for variable id
	 * 
	 * @param achievement_id id of an achievement
	 */
	public void setId(String achievement_id){
		this.achievementId = achievement_id;
	}
	
	/**
	 * Getter for variable name
	 * 
	 * @return name of an achievement
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * Setter for variable name
	 * 
	 * @param name name of an achievement
	 */
	public void setName(String name){
		this.name = name;
	}
	
	/**
	 * Getter for description
	 * 
	 * @return description of an achievement
	 */
	public String getDescription(){
		return this.description;
	}
	
	/**
	 * Setter for description
	 * 
	 * @param description description of an achievement
	 */
	public void setDescription(String description){
		this.description = description;
	}
	
	/**
	 * Getter for point value
	 * 
	 * @return point value of an achievement
	 */
	public int getPointValue(){
		return this.pointValue;
	}
	
	/**
	 * Setter for point value
	 * 
	 * @param point_value point value of an achievement
	 */
	public void setPointValue(int point_value){
		this.pointValue = point_value;
	}
	
	
	/**
	 * Getter for badge id of an achievement
	 * 
	 * @return badge id of an achievement
	 */
	public String getBadgeId(){
		return this.badgeId;
	}
	
	/**
	 * Setter for badge id of an achievement
	 * 
	 * @param badge_id badge id of an achievement
	 */
	public void setBadgeId(String badge_id){
		this.badgeId = badge_id;
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
