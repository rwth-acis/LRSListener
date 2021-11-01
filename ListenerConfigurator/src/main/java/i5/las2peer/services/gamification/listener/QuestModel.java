package i5.las2peer.services.gamification.listener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
* Quest Model
* 
* This is the class to store the Quest model
* 
*/

@ApiModel( value = "QuestModel", description = "Quest resource representation" )
public class QuestModel{

	public static enum QuestStatus {
		COMPLETED,
		REVEALED,
		HIDDEN
	}
	
	// Constraint for the quest to be revealed
	public static enum QuestConstraint{
		QUEST,
		POINT,
		BOTH,
		NONE
	}

	@ApiModelProperty( value = "Quest ID", required = true ) 
	private String questId;
	@ApiModelProperty( value = "Quest name", required = true ) 
	private String name;
	@ApiModelProperty( value = "Quest description") 
	private String description;
	@ApiModelProperty( value = "Quest status", required = true ) 
	private QuestStatus status;
	@ApiModelProperty( value = "Achievement ID", required = true ) 
	private String achievementId;
	@ApiModelProperty( value = "Quest flag", required = true ) 
	private boolean questFlag = false;
	@ApiModelProperty( value = "Quest ID Completed") 
	private String questIdCompleted;
	@ApiModelProperty( value = "Point flag", required = true ) 
	private boolean pointFlag = false;
	@ApiModelProperty( value = "Point value") 
	private int pointValue = 0;
	@ApiModelProperty( value = "Action IDs", required = true ) 
	private List<String> actionIds;
	@ApiModelProperty( value = "Use notification status", required = true ) 
	private boolean useNotification;
	@ApiModelProperty( value = "Notification Message") 
	private String notificationMessage;
	/**
	 * @return the questId
	 */
	public String getQuestId() {
		return questId;
	}
	/**
	 * @param questId the questId to set
	 */
	public void setQuestId(String questId) {
		this.questId = questId;
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
	 * @return the status
	 */
	public QuestStatus getStatus() {
		return status;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(QuestStatus status) {
		this.status = status;
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
	 * @return the questFlag
	 */
	public boolean isQuestFlag() {
		return questFlag;
	}
	/**
	 * @param questFlag the questFlag to set
	 */
	public void setQuestFlag(boolean questFlag) {
		this.questFlag = questFlag;
	}
	/**
	 * @return the questIdCompleted
	 */
	public String getQuestIdCompleted() {
		return questIdCompleted;
	}
	/**
	 * @param questIdCompleted the questIdCompleted to set
	 */
	public void setQuestIdCompleted(String questIdCompleted) {
		this.questIdCompleted = questIdCompleted;
	}
	/**
	 * @return the pointFlag
	 */
	public boolean isPointFlag() {
		return pointFlag;
	}
	/**
	 * @param pointFlag the pointFlag to set
	 */
	public void setPointFlag(boolean pointFlag) {
		this.pointFlag = pointFlag;
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
	 * @return the actionIds
	 */
	public List<String> getActionIds() {
		return actionIds;
	}
	/**
	 * @param actionIds the actionIds to set
	 * @throws IOException IOExeption
	 */
	public void setActionIds(List<String> actionIds) throws IOException {
		this.actionIds = new ArrayList<>();
		
		if(actionIds.isEmpty()){
			throw new IOException("List cannot be empty.");
		}
		for(String p: actionIds){
			this.actionIds.add(p);
		}
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
