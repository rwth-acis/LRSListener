package i5.las2peer.services.gamification.listener;

import io.swagger.annotations.ApiModelProperty;

public class StreakModel {
	@ApiModelProperty( value = "Streak ID", required = true ) 
	private String streakId;
	@ApiModelProperty( value = "Streak name", required = true ) 
	private String name;
	@ApiModelProperty( value = "Streak description") 
	private String description;
	@ApiModelProperty( value = "Achievement ID", required = true ) 
	private String achievementId;
	@ApiModelProperty( value = "Use notification status", required = true ) 
	private boolean useNotification;
	@ApiModelProperty( value = "Notification Message") 
	private String notificationMessage;
	@ApiModelProperty( value = "Point value") 
	private int pointValue = 0;
	/**
	 * @return the streakId
	 */
	public String getStreakId() {
		return streakId;
	}
	/**
	 * @param streakId the streakId to set
	 */
	public void setStreakId(String streakId) {
		this.streakId = streakId;
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
}
