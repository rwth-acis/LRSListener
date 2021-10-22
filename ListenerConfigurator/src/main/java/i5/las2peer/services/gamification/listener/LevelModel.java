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
	private int levelNumber;
	@ApiModelProperty( value = "Level name", required = true ) 
	private String name;
	@ApiModelProperty( value = "Level point value threshold") 
	private int pointValue = 0;
	@ApiModelProperty( value = "Use notification status", required = true ) 
	private boolean useNotification;
	@ApiModelProperty( value = "Notification Message") 
	private String notificationMessage;
	/**
	 * @return the levelNumber
	 */
	public int getLevelNumber() {
		return levelNumber;
	}
	/**
	 * @param levelNumber the levelNumber to set
	 */
	public void setLevelNumber(int levelNumber) {
		this.levelNumber = levelNumber;
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
