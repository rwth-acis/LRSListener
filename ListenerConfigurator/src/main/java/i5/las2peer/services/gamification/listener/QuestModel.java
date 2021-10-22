package i5.las2peer.services.gamification.listener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

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
	private List<Pair<String, Integer>> actionIds;
	@ApiModelProperty( value = "Use notification status", required = true ) 
	private boolean useNotification;
	@ApiModelProperty( value = "Notification Message") 
	private String notifMessage;

	/**
	 * Getter for quest flag
	 * 
	 * @return quest flag
	 */
	public boolean getQuestFlag(){
		return this.questFlag;
	}
	
	/**
	 * Setter for quest flag
	 * 
	 * @param quest_flag quest flag
	 */
	public void setQuestFlag(boolean quest_flag){
		this.questFlag = quest_flag;
	}

	
	/**
	 * Getter for completed quest ID
	 * 
	 * @return completed quest ID
	 */
	public String getQuestIdCompleted(){
		return this.questIdCompleted;
	}
	
	/**
	 * Setter for completed quest ID
	 * 
	 * @param quest_id_completed completed quest ID
	 */
	public void setQuestIdCompleted(String quest_id_completed){
		this.questIdCompleted =  quest_id_completed;
	}
	
	/**
	 * Getter for point flag
	 * 
	 * @return point flag
	 */
	public boolean getPointFlag(){
		return this.pointFlag;
	}
	
	/**
	 * Setter for point flag
	 * 
	 * @param point_flag point flag
	 */
	public void setPointFlag(boolean point_flag){
		this.pointFlag = point_flag;
	}
	
	/**
	 * Getter for point value
	 * 
	 * @return point value
	 */
	public int getPointValue(){
		return this.pointValue;
	}
	
	/**
	 * Setter for point value
	 * 
	 * @param point_value point value
	 */
	public void setPointValue(int point_value){
		this.pointValue =  point_value;
	}
	
	/**
	 * Getter for action ids used by quest
	 * 
	 * @return action ids
	 */
	public List<Pair<String, Integer>> getActionIds(){
		return this.actionIds;
	}
	
	/**
	 * Setter for action ids used by quest
	 * 
	 * @param action_ids list of action id with times
	 * @throws IOException io exception
	 */
	public void setActionIds(List<Pair<String, Integer>> action_ids) throws IOException{
		this.actionIds = new ArrayList<Pair<String,Integer>>();
		
		if(action_ids.isEmpty()){
			throw new IOException("List cannot be empty.");
		}
		for(Pair<String,Integer> p: action_ids){
			this.actionIds.add(Pair.of(p.getLeft(), p.getRight()));
		}
	}
	
	/**
	 * Getter for variable quest id
	 * 
	 * @return id of a quest
	 */
	public String getId(){
		return this.questId;
	}
	
	/**
	 * Setter for variable quest id
	 * 
	 * @param quest_id id of a quest
	 */
	public void setId(String quest_id){
		this.questId = quest_id;
	}
	
	/**
	 * Getter for variable name
	 * 
	 * @return name of a quest
	 */
	public String getName(){
		return this.name;
	}
	
	/**
	 * Setter for variable name
	 * 
	 * @param name name of a quest
	 */
	public void setName(String name){
		this.name = name;
	}
	
	/**
	 * Getter for description
	 * 
	 * @return description of a quest
	 */
	public String getDescription(){
		return this.description;
	}
	
	/**
	 * Setter for description
	 * 
	 * @param description description of a quest
	 */
	public void setDescription(String description){
		this.description = description;
	}
	
	/**
	 * Getter for quest status
	 * 
	 * @return quest status
	 */
	public QuestStatus getStatus(){
		return this.status;
	}
	
	/**
	 * Setter for quest status
	 * 
	 * @param status quest status
	 */
	public void setStatus(QuestStatus status){
		this.status = status;
	}
	
	/**
	 * Getter for achievement id
	 * 
	 * @return id of achievement
	 */
	public String getAchievementId(){
		return this.achievementId;
	}
	
	/**
	 * Setter for variable id
	 * 
	 * @param achievement_id id of achievement
	 */
	public void setAchievementId(String achievement_id){
		this.achievementId = achievement_id;
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
