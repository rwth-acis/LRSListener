package i5.las2peer.services.gamification.listener;

import java.sql.Connection;
import java.sql.SQLException;

import net.minidev.json.JSONObject;

public class ConfigDAO {

	public boolean isConfigIdExist(Connection conn, String configId) throws SQLException{
		// TODO Auto-generated method stub
		return false;
	}

	public ConfigModel getConfigModelWithId(Connection conn, String configId) throws SQLException{
		// TODO Auto-generated method stub
		return null;
	}

	public void createConfig(Connection conn, ConfigModel configModel) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	public void deleteConfig(Connection conn, String configId) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public void updateConfig(Connection conn, ConfigModel configModel) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public JSONObject getMapping(Connection conn, String configId) throws SQLException{
		// TODO Auto-generated method stub
		return null;
	}

	public void createGame(Connection conn, String configId, GameModel game) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public void createQuest(Connection conn, String configId, QuestModel quest) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public void createAchhievement(Connection conn, String configId, AchievementModel achievement) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public void createBadge(Connection conn, String configId, BadgeModel badge) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public void createAction(Connection conn, String configId, ActionModel action) throws SQLException{
		// TODO Auto-generated method stub
		
	}
	
	public void createLevel(Connection conn, String configId, LevelModel level) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public boolean isGameIdExist(Connection conn, String configId, String gameId) throws SQLException{
		// TODO Auto-generated method stub
		return false;
	}


	public GameModel getGameWithId(Connection conn, String configId, String gameId) {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateGame(Connection conn, String configId, GameModel game) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public void deleteGame(Connection conn, String configId, String gameId) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public boolean isQuestIdExist(Connection conn, String configId, String questId) throws SQLException{
		// TODO Auto-generated method stub
		return false;
	}

	public QuestModel getQuestWithId(Connection conn, String configId, String questId) throws SQLException{
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isAchievementIdExist(Connection conn, String configId, String achievementId) throws SQLException{
		// TODO Auto-generated method stub
		return false;
	}

	public AchievementModel getAchievementWithId(Connection conn, String configId, String achievementId) throws SQLException{
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isBadgeIdExist(Connection conn, String configId, String badgeId) throws SQLException{
		// TODO Auto-generated method stub
		return false;
	}

	public BadgeModel getBadgeWithId(Connection conn, String configId, String badgeId) throws SQLException{
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isActionIdExist(Connection conn, String configId, String actionId) throws SQLException{
		// TODO Auto-generated method stub
		return false;
	}

	public ActionModel getActionWithId(Connection conn, String configId, String actionId) throws SQLException{
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isLevelIdExist(Connection conn, String configId, String levelId) throws SQLException{
		// TODO Auto-generated method stub
		return false;
	}

	public LevelModel getLevelWithId(Connection conn, String configId, String levelId) throws SQLException{
		// TODO Auto-generated method stub
		return null;
	}

	public void deleteQuest(Connection conn, String configId, String questId) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public void deleteAchievement(Connection conn, String configId, String achievementId) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public void deleteBadge(Connection conn, String configId, String badgeId) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public void deleteAction(Connection conn, String configId, String actionId) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public void deleteLevel(Connection conn, String configId, String levelId) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public void updateQuest(Connection conn, String configId, QuestModel quest) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public void updateAchievement(Connection conn, String configId, AchievementModel achievement) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public void updateBadge(Connection conn, String configId, BadgeModel badge) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public void updateAction(Connection conn, String configId, ActionModel action) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public void updateLevel(Connection conn, String configId, LevelModel level) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public void addElementToMapping(Connection conn, String configId, String id) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public void removeElementFromMapping(Connection conn, String configId, String gameId) throws SQLException{
		// TODO Auto-generated method stub
		
	}
	
}
