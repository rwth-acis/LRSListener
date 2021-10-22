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
	private String notificationMessage;
	
	/**
	 * @return the badgeId
	 */
	public String getBadgeId() {
		return badgeId;
	}
	/**
	 * @param badgeId the badgeId to set
	 */
	public void setBadgeId(String badgeId) {
		this.badgeId = badgeId;
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
	/**
	 * @return the useNotification
	 */
	public boolean isUseNotification() {
		return useNotification;
	}
	/**
	 * @param useNotification the useNotification to set
	 */
	public void setUseNotification(boolean useNotification) {
		this.useNotification = useNotification;
	}
	/**
	 * @return the notificationMessage
	 */
	public String getNotificationMessage() {
		return notificationMessage;
	}
	/**
	 * @param notificationMessage the notificationMessage to set
	 */
	public void setNotificationMessage(String notificationMessage) {
		this.notificationMessage = notificationMessage;
	}
	
	
	
}
