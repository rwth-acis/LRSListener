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
	private String notificationMessage;
	/**
	 * @return the achievementId
	 */
	public String getAchievementId() {
		return achievementId;
	}
	/**
	 * @param achievementId the achievementId to set
	 */
	public void setAchievementId(String achievementId) {
		this.achievementId = achievementId;
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
	 * @return the pointValue
	 */
	public int getPointValue() {
		return pointValue;
	}
	/**
	 * @param pointValue the pointValue to set
	 */
	public void setPointValue(int pointValue) {
		this.pointValue = pointValue;
	}
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