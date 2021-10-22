package i5.las2peer.services.gamification.listener;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


/**
* Action Model
* 
* This is the class to store the Badge model
* 
*/

@ApiModel( value = "ActionModel", description = "Action resource representation" )
public class ActionModel{
	

	@ApiModelProperty( value = "Action ID", required = true ) 
	private String actionId;
	@ApiModelProperty( value = "Action name", required = true ) 
	private String name;
	@ApiModelProperty( value = "Action description") 
	private String description;
	@ApiModelProperty( value = "Action point value") 
	private int pointValue = 0;
	@ApiModelProperty( value = "Use notification status", required = true ) 
	private boolean useNotification;
	@ApiModelProperty( value = "Notification Message") 
	private String notificationMessage;
	
	/**
	 * @return the actionId
	 */
	public String getActionId() {
		return actionId;
	}
	/**
	 * @param actionId the actionId to set
	 */
	public void setActionId(String actionId) {
		this.actionId = actionId;
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
