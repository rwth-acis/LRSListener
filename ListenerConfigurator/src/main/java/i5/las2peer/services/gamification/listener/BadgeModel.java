package i5.las2peer.services.gamification.listener;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
* Badge Model
* 
* This is the class to store the Badge model
* 
*/

@ApiModel( value = "BadgeModel", description = "Badge resource representation" )
public class BadgeModel{
	@ApiModelProperty( value = "Badge ID", required = true ) 
	private String badgeId;
	@ApiModelProperty( value = "Badge name", required = true ) 
	private String name;
	@ApiModelProperty( value = "Badge description") 
	private String description;
	@ApiModelProperty( value = "Use notification status", required = true ) 
	private boolean useNotification;
	@ApiModelProperty( value = "Notification Message") 
	private String notifMessage;
	
	
	/**
	 * Getter for variable id
	 * 
	 * @return id of a badge
	 */
	public String getId(){
		return badgeId;
	}
	
	/**
	 * Setter for variable id
	 * 
	 * @param id id of a badge
	 */
	public void setId(String id){
		this.badgeId = id;
	}
	
	/**
	 * Getter for variable name
	 * 
	 * @return name of a badge
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * Setter for variable name
	 * 
	 * @param name name of a badge
	 */
	public void setName(String name){
		this.name = name;
	}
	
	/**
	 * Getter for description
	 * 
	 * @return description of a badge
	 */
	public String getDescription(){
		return this.description;
	}
	
	/**
	 * Setter for description
	 * 
	 * @param description description of a badge
	 */
	public void setDescription(String description){
		this.description = description;
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
