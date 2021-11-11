package i5.las2peer.services.gamification.listener;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

public class ConfigDAO {

	PreparedStatement stmt;

	public boolean isConfigIdExist(Connection conn, String configId) throws SQLException {
		stmt = conn.prepareStatement("SELECT config_id FROM model.config_data WHERE config_id=?");
		stmt.setString(1, configId);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			return true;
		}
		return false;
	}

	public boolean isGameIdExist(Connection conn, String gameId) throws SQLException {
		stmt = conn.prepareStatement("SELECT game_id FROM model.game_data WHERE game_id=?");
		stmt.setString(1, gameId);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			return true;
		}
		return false;
	}

	public boolean isQuestIdExist(Connection conn, String questId) throws SQLException {
		stmt = conn.prepareStatement("SELECT quest_id FROM model.quest_data WHERE quest_id=?");
		stmt.setString(1, questId);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			return true;
		}
		return false;
	}

	public boolean isAchievementIdExist(Connection conn, String achievementId) throws SQLException {
		stmt = conn.prepareStatement("SELECT achievement_id FROM model.achievement_data WHERE achievement_id=?");
		stmt.setString(1, achievementId);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			return true;
		}
		return false;
	}

	public boolean isBadgeIdExist(Connection conn, String badgeId) throws SQLException {
		stmt = conn.prepareStatement("SELECT badge_id FROM model.badge_data WHERE badge_id=?");
		stmt.setString(1, badgeId);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			return true;
		}
		return false;
	}

	public boolean isActionIdExist(Connection conn, String actionId) throws SQLException {
		stmt = conn.prepareStatement("SELECT action_id FROM model.action_data WHERE action_id=?");
		stmt.setString(1, actionId);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			return true;
		}
		return false;
	}

	public boolean isLevelIdExist(Connection conn, String levelId) throws SQLException {
		stmt = conn.prepareStatement("SELECT level_num FROM model.level_data WHERE level_num=?");
		int levelNumber = Integer.parseInt(levelId);
		stmt.setInt(1, levelNumber);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			return true;
		}
		return false;
	}
	
	public boolean isStreakIdExist(Connection conn, String streakId) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public void createConfig(Connection conn, ConfigModel config) throws SQLException {
		stmt = conn.prepareStatement("INSERT INTO model.config_data (config_id, name, description) VALUES (?, ?, ?)");
		stmt.setString(1, config.getConfigId());
		stmt.setString(2, config.getName());
		stmt.setString(3, config.getDescription());
		stmt.executeUpdate();
	}

	public void createGame(Connection conn, GameModel game) throws SQLException {
		stmt = conn
				.prepareStatement("INSERT INTO model.game_data(game_id, description, community_type) VALUES(?, ?, ?)");
		stmt.setString(1, game.getGameId());
		stmt.setString(2, game.getDescription());
		stmt.setString(3, game.getCommunityType());
		stmt.executeUpdate();
	}

	public void createQuest(Connection conn, QuestModel quest) throws SQLException {
		stmt = conn.prepareStatement(
				"INSERT INTO model.quest_data (quest_id, name, description, status, achievement_id, quest_flag, quest_id_completed, point_flag, point_value, use_notification , notif_message)  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		stmt.setString(1, quest.getQuestId());
		stmt.setString(2, quest.getName());
		stmt.setString(3, quest.getDescription());
		if (quest.getStatus() != null) {
			stmt.setString(4, quest.getStatus().toString());
		}
		stmt.setString(5, quest.getAchievementId());
		stmt.setBoolean(6, quest.isQuestFlag());
		stmt.setString(7, quest.getQuestIdCompleted());
		stmt.setBoolean(8, quest.isPointFlag());
		stmt.setInt(9, quest.getPointValue());
		stmt.setBoolean(10, quest.isUseNotification());
		stmt.setString(11, quest.getNotificationMessage());
		stmt.executeUpdate();
		if (quest.getActionIds() != null) {
			for (String entry : quest.getActionIds()) {
				stmt = conn.prepareStatement("Insert INTO model.quest_action_data (quest_id, action_id) VALUES (?,?)");
				stmt.setString(1, quest.getQuestId());
				stmt.setString(2, entry);
				stmt.executeUpdate();
			}
		}
	}

	public void createAchhievement(Connection conn, AchievementModel achievement) throws SQLException {
		stmt = conn.prepareStatement(
				"INSERT INTO model.achievement_data (achievement_id, name, description, point_value, badge_id, use_notification, notif_message) VALUES (?, ?, ?, ?, ?, ?, ?)");
		stmt.setString(1, achievement.getAchievementId());
		stmt.setString(2, achievement.getName());
		stmt.setString(3, achievement.getDescription());
		stmt.setInt(4, achievement.getPointValue());
		stmt.setString(5, achievement.getBadgeId());
		stmt.setBoolean(6, achievement.isUseNotification());
		stmt.setString(7, achievement.getNotificationMessage());
		stmt.executeUpdate();

	}

	public void createBadge(Connection conn, BadgeModel badge) throws SQLException {
		stmt = conn.prepareStatement(
				"INSERT INTO model.badge_data (badge_id, name, description, use_notification, notif_message) VALUES (?, ?, ?, ?, ?)");
		stmt.setString(1, badge.getBadgeId());
		stmt.setString(2, badge.getName());
		stmt.setString(3, badge.getDescription());
		stmt.setBoolean(4, badge.isUseNotification());
		stmt.setString(5, badge.getNotificationMessage());
		stmt.executeUpdate();
	}

	public void createAction(Connection conn, ActionModel action) throws SQLException {
		stmt = conn.prepareStatement(
				"INSERT INTO model.action_data (action_id, name, description, point_value, use_notification, notif_message) VALUES (?, ?, ?, ?, ?, ?)");
		stmt.setString(1, action.getActionId());
		stmt.setString(2, action.getName());
		stmt.setString(3, action.getDescription());
		stmt.setInt(4, action.getPointValue());
		stmt.setBoolean(5, action.isUseNotification());
		stmt.setString(6, action.getNotificationMessage());
		stmt.executeUpdate();
	}

	public void createLevel(Connection conn, LevelModel level) throws SQLException {
		stmt = conn.prepareStatement(
				"INSERT INTO model.level_data (level_num, name, point_value, use_notification, notif_message ) VALUES (?, ?, ?, ?, ?)");
		stmt.setInt(1, level.getLevelNumber());
		stmt.setString(2, level.getName());
		stmt.setInt(3, level.getPointValue());
		stmt.setBoolean(4, level.isUseNotification());
		stmt.setString(5, level.getNotificationMessage());
		stmt.executeUpdate();
	}
	
	public void createStreak(Connection conn, StreakModel streakModel) {
		// TODO Auto-generated method stub
		
	}
	
	public ConfigModel getConfigModelWithId(Connection conn, String configId) throws SQLException {
		stmt = conn.prepareStatement("SELECT * FROM model.config_data WHERE config_id = ?");
		stmt.setString(1, configId);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			ConfigModel config = new ConfigModel();
			config.setConfigId(configId);
			config.setName(rs.getString("name"));
			config.setDescription(rs.getString("description"));
			return config;
		}
		return null;
	}

	public GameModel getGameWithId(Connection conn, String gameId) {
		try {
			stmt = conn.prepareStatement("SELECT community_type,description FROM model.game_data WHERE game_id = ?");
			stmt.setString(1, gameId);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				GameModel game = new GameModel();
				game.setGameId(gameId);
				game.setDescription(rs.getString("description"));
				game.setCommunityType(rs.getString("community_type"));
				return game;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public QuestModel getQuestWithId(Connection conn, String questId) throws SQLException, IOException {
		stmt = conn.prepareStatement("SELECT * FROM model.quest_data WHERE quest_id = ?");
		stmt.setString(1, questId);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			QuestModel quest = new QuestModel();
			quest.setQuestId(questId);
			quest.setName(rs.getString("name"));
			quest.setDescription(rs.getString("description"));
			quest.setStatus(
					i5.las2peer.services.gamification.listener.QuestModel.QuestStatus.valueOf(rs.getString("status")));
			quest.setAchievementId(rs.getString("achievement_id"));
			quest.setQuestFlag(rs.getBoolean("quest_flag"));
			quest.setQuestIdCompleted(rs.getString("quest_id_completed"));
			quest.setPointFlag(rs.getBoolean("point_flag"));
			quest.setPointValue(rs.getInt("point_value"));
			quest.setUseNotification(rs.getBoolean("use_notification"));
			quest.setNotificationMessage(rs.getString("notif_message"));

			List<String> actionList = new ArrayList<>();
			stmt = conn.prepareStatement("SELECT action_id FROM model.quest_action_data WHERE quest_id=?");
			stmt.setString(1, quest.getQuestId());
			ResultSet rs2 = stmt.executeQuery();
			while (rs2.next()) {
				if (rs2.getString("action_id") != "") {
					actionList.add(rs2.getString("action_id"));
				}
			}
			if (!actionList.isEmpty()) {
				quest.setActionIds(actionList);
			}
			return quest;
		}
		return null;
	}

	public AchievementModel getAchievementWithId(Connection conn, String achievementId) throws SQLException {
		stmt = conn.prepareStatement("SELECT * FROM model.achievement_data WHERE achievement_id = ?");
		stmt.setString(1, achievementId);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			AchievementModel achievement = new AchievementModel();
			achievement.setAchievementId(achievementId);
			achievement.setName(rs.getString("name"));
			achievement.setDescription(rs.getString("description"));
			achievement.setPointValue(rs.getInt("point_value"));
			achievement.setBadgeId(rs.getString("badge_id"));
			achievement.setUseNotification(rs.getBoolean("use_notification"));
			achievement.setNotificationMessage(rs.getString("notif_message"));
			return achievement;
		}
		return null;
	}

	public BadgeModel getBadgeWithId(Connection conn, String badgeId) throws SQLException {
		stmt = conn.prepareStatement("SELECT * FROM model.badge_data WHERE badge_id = ?");
		stmt.setString(1, badgeId);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			BadgeModel badge = new BadgeModel();
			badge.setBadgeId(badgeId);
			badge.setName(rs.getString("name"));
			badge.setDescription(rs.getString("description"));
			badge.setUseNotification(rs.getBoolean("use_notification"));
			badge.setNotificationMessage(rs.getString("notif_message"));
			return badge;
		}
		return null;
	}

	public ActionModel getActionWithId(Connection conn, String actionId) throws SQLException {
		stmt = conn.prepareStatement("SELECT * FROM model.action_data WHERE action_id = ?");
		stmt.setString(1, actionId);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			ActionModel action = new ActionModel();
			action.setActionId(actionId);
			action.setName(rs.getString("name"));
			action.setDescription(rs.getString("description"));
			action.setPointValue(rs.getInt("point_value"));
			action.setUseNotification(rs.getBoolean("use_notification"));
			action.setNotificationMessage(rs.getString("notif_message"));
			return action;
		}
		return null;
	}

	public LevelModel getLevelWithId(Connection conn, String levelId) throws SQLException {
		stmt = conn.prepareStatement("SELECT * FROM model.level_data WHERE level_num = ?");
		int levelNumber = Integer.parseInt(levelId);
		stmt.setInt(1, levelNumber);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			LevelModel level = new LevelModel();
			level.setLevelNumber(levelNumber);
			level.setName(rs.getString("name"));
			level.setPointValue(rs.getInt("point_value"));
			level.setUseNotification(rs.getBoolean("use_notification"));
			level.setNotificationMessage(rs.getString("notif_message"));
			return level;
		}
		return null;
	}
	
	public StreakModel getStreakWithId(Connection conn, String streakId) throws SQLException{
		// TODO Auto-generated method stub
		return null;
	}

	public void updateGame(Connection conn, GameModel game) throws SQLException {
		stmt = conn
				.prepareStatement("UPDATE model.game_data SET description = ?, community_type = ? WHERE game_id = ?");
		stmt.setString(1, game.getDescription());
		stmt.setString(2, game.getCommunityType());
		stmt.setString(3, game.getGameId());
		stmt.executeUpdate();
	}

	public void updateConfig(Connection conn, ConfigModel config) throws SQLException {
		stmt = conn.prepareStatement("UPDATE model.config_data SET config_id =?, name = ?, description = ?");
		stmt.setString(1, config.getConfigId());
		stmt.setString(2, config.getName());
		stmt.setString(3, config.getDescription());
		stmt.executeUpdate();

	}
	
	public void updateQuest(Connection conn, QuestModel quest) throws SQLException {
		stmt = conn.prepareStatement(
				"UPDATE model.quest_data SET name = ?, description = ?, status = ?, achievement_id = ?, quest_flag = ?, quest_id_completed = ?, point_flag = ?, point_value = ?, use_notification = ?, notif_message = ? WHERE quest_id = ?");
		stmt.setString(1, quest.getName());
		stmt.setString(2, quest.getDescription());
		if (quest.getStatus() != null) {
			stmt.setString(3, quest.getStatus().toString());
		}
		stmt.setString(4, quest.getAchievementId());
		stmt.setBoolean(5, quest.isQuestFlag());
		stmt.setString(6, quest.getQuestIdCompleted());
		stmt.setBoolean(7, quest.isPointFlag());
		stmt.setInt(8, quest.getPointValue());
		stmt.setBoolean(9, quest.isUseNotification());
		stmt.setString(10, quest.getNotificationMessage());
		stmt.setString(11, quest.getQuestId());
		stmt.executeUpdate();

		stmt = conn.prepareStatement("DELETE FROM model.quest_action_data WHERE quest_id=?");
		stmt.setString(1, quest.getQuestId());
		stmt.executeUpdate();
		
		if (quest.getActionIds() != null) {
			for (String entry : quest.getActionIds()) {
				stmt = conn
				.prepareStatement("INSERT INTO model.quest_action_data (quest_id, action_id) VALUES ( ?, ?)");
				stmt.setString(1, quest.getQuestId());
				stmt.setString(2, entry);
				stmt.executeUpdate();
			}
		}
	}

	public void updateAchievement(Connection conn, AchievementModel achievement) throws SQLException {
		stmt = conn.prepareStatement(
				"UPDATE model.achievement_data SET name = ?, description = ?, point_value = ?, badge_id = ?, use_notification = ?, notif_message = ? WHERE achievement_id = ?");
		stmt.setString(1, achievement.getName());
		stmt.setString(2, achievement.getDescription());
		stmt.setInt(3, achievement.getPointValue());
		stmt.setString(4, achievement.getBadgeId());
		stmt.setBoolean(5, achievement.isUseNotification());
		stmt.setString(6, achievement.getNotificationMessage());
		stmt.setString(7, achievement.getAchievementId());
		stmt.executeUpdate();
	}

	public void updateBadge(Connection conn, BadgeModel badge) throws SQLException {
		stmt = conn.prepareStatement(
				"UPDATE model.badge_data SET name = ?, description = ?, use_notification = ?, notif_message = ? WHERE badge_id = ?");
		stmt.setString(1, badge.getName());
		stmt.setString(2, badge.getDescription());
		stmt.setBoolean(3, badge.isUseNotification());
		stmt.setString(4, badge.getNotificationMessage());
		stmt.setString(5, badge.getBadgeId());
		stmt.executeUpdate();
	}

	public void updateAction(Connection conn, ActionModel action) throws SQLException {
		stmt = conn.prepareStatement(
				"UPDATE model.action_data SET name = ?, description = ?, point_value = ?, use_notification = ?, notif_message = ? WHERE action_id = ?");
		stmt.setString(1, action.getName());
		stmt.setString(2, action.getDescription());
		stmt.setInt(3, action.getPointValue());
		stmt.setBoolean(4, action.isUseNotification());
		stmt.setString(5, action.getNotificationMessage());
		stmt.setString(6, action.getActionId());
		stmt.executeUpdate();
	}

	public void updateLevel(Connection conn, LevelModel level) throws SQLException {
		stmt = conn.prepareStatement(
				"UPDATE model.level_data SET name = ?, point_value = ?, use_notification = ?, notif_message = ? WHERE level_num = ?");
		stmt.setString(1, level.getName());
		stmt.setInt(2, level.getPointValue());
		stmt.setBoolean(3, level.isUseNotification());
		stmt.setString(4, level.getNotificationMessage());
		stmt.setInt(5, level.getLevelNumber());
		stmt.executeUpdate();
	}
	
	public void updateStreak(Connection conn, StreakModel streakModel) throws SQLException{
		// TODO Auto-generated method stub
		
	}
	
	public void deleteConfig(Connection conn, String configId) throws SQLException {
		stmt = conn.prepareStatement("DELETE FROM model.config_data WHERE config_id = ?");
		stmt.setString(1, configId);
		stmt.executeUpdate();
	}

	public void deleteGame(Connection conn, String gameId) throws SQLException {
		stmt = conn.prepareStatement("DELETE FROM model.game_data WHERE game_id = ?");
		stmt.setString(1, gameId);
		stmt.executeUpdate();
	}

	public void deleteQuest(Connection conn, String questId) throws SQLException {
		stmt = conn.prepareStatement("DELETE FROM model.quest_data WHERE quest_id = ?");
		stmt.setString(1, questId);
		stmt.executeUpdate();

		stmt = conn.prepareStatement("DELETE FROM model.quest_action_data WHERE quest_id = ?");
		stmt.setString(1, questId);
		stmt.executeUpdate();
	}

	public void deleteAchievement(Connection conn, String achievementId) throws SQLException {
		stmt = conn.prepareStatement("DELETE FROM model.achievement_data WHERE achievement_id = ?");
		stmt.setString(1, achievementId);
		stmt.executeUpdate();
	}

	public void deleteBadge(Connection conn, String badgeId) throws SQLException {
		stmt = conn.prepareStatement("DELETE FROM model.badge_data WHERE badge_id = ?");
		stmt.setString(1, badgeId);
		stmt.executeUpdate();
	}

	public void deleteAction(Connection conn, String actionId) throws SQLException {
		stmt = conn.prepareStatement("DELETE FROM model.action_data WHERE action_id = ?");
		stmt.setString(1, actionId);
		stmt.executeUpdate();
	}

	public void deleteLevel(Connection conn, String levelId) throws SQLException {
		stmt = conn.prepareStatement("DELETE FROM model.level_data WHERE level_num = ?");
		int levelNumber = Integer.parseInt(levelId);
		stmt.setInt(1, levelNumber);
		stmt.executeUpdate();
	}
	
	public void deleteStreak(Connection conn, String streakId) throws SQLException{
		// TODO Auto-generated method stub
		
	}

	public void addElementToMapping(Connection conn, String configId, String gameId, String elementId, String listenTo,
			String type) throws SQLException {
		if (type.equals("game")) {
			stmt = conn
					.prepareStatement("INSERT INTO listen.game_info (config_id, game_id, listen_to) VALUES (?, ?, ?)");
			stmt.setString(1, configId);
			stmt.setString(2, elementId);
			stmt.setString(3, listenTo);
			stmt.executeUpdate();
		} else if (type.equals("level")) {
			stmt = conn.prepareStatement(
					"INSERT INTO listen.level_info (config_id, game_id, level_num, listen_to) VALUES (?, ?, ?, ?)");
			int levelNumber = Integer.parseInt(elementId);
			stmt.setString(1, configId);
			stmt.setString(2, gameId);
			stmt.setInt(3, levelNumber);
			stmt.setString(4, listenTo);
			stmt.executeUpdate();
		} else {
			switch (type) {
			case "quest": {
				stmt = conn.prepareStatement(
						"INSERT INTO listen.quest_info (config_id, game_id, quest_id, listen_to) VALUES (?, ?, ?, ?)");
				break;
			}
			case "achievement": {
				stmt = conn.prepareStatement(
						"INSERT INTO listen.achievement_info (config_id, game_id, achievement_id, listen_to) VALUES (?, ?, ?, ?)");
				break;
			}
			case "badge": {
				stmt = conn.prepareStatement(
						"INSERT INTO listen.badge_info (config_id, game_id, badge_id, listen_to) VALUES (?, ?, ?, ?)");
				break;
			}
			case "action": {
				stmt = conn.prepareStatement(
						"INSERT INTO listen.action_info (config_id, game_id, action_id, listen_to) VALUES (?, ?, ?, ?)");
				break;
			}
			case "streak": {
				stmt = conn.prepareStatement(
						"INSERT INTO listen.streak_info (config_id, game_id, streak_id, listen_to) VALUES (?, ?, ?, ?)");
				break;
			}
			default:
				throw new IllegalArgumentException("Unexpected value: " + type);
			}
			stmt.setString(1, configId);
			stmt.setString(2, gameId);
			stmt.setString(3, elementId);
			stmt.setString(4, listenTo);
			stmt.executeUpdate();
		}
	}

	public void removeElementFromMapping(Connection conn, String configId, String elementId, String type)
			throws SQLException {
		if (type.equals("level")) {
			stmt = conn.prepareStatement("DELETE FROM listen.level_info WHERE config_id = ? AND level_num = ?");
			int levelNumber = Integer.parseInt(elementId);
			stmt.setString(1, configId);
			stmt.setInt(2, levelNumber);
			stmt.executeUpdate();
		} else {
			switch (type) {
			case "game": {
				stmt = conn.prepareStatement("DELETE FROM listen.game_info WHERE config_id = ? AND game_id = ?");
				break;
			}
			case "quest": {
				stmt = conn.prepareStatement("DELETE FROM listen.quest_info WHERE config_id = ? AND quest_id = ?");
				break;
			}
			case "achievement": {
				stmt = conn.prepareStatement(
						"DELETE FROM listen.achievement_info WHERE config_id = ? AND achievement_id = ?");
				break;
			}
			case "badge": {
				stmt = conn.prepareStatement("DELETE FROM listen.badge_info WHERE config_id = ? AND badge_id = ?");
				break;
			}
			case "action": {
				stmt = conn.prepareStatement("DELETE FROM listen.action_info WHERE config_id = ? AND action_id = ?");
				break;
			}
			case "streak": {
				stmt = conn.prepareStatement("DELETE FROM listen.streak_info WHERE config_id = ? AND streak_id = ?");
				break;
			}
			default:
				throw new IllegalArgumentException("Unexpected value: " + type);
			}
			stmt.setString(1, configId);
			stmt.setString(2, elementId);
			stmt.executeUpdate();
		}
	}

	public Mapping getMapping(Connection conn, String configId) throws SQLException {
		Mapping mapping = new Mapping();
		mapping.setGameMapping(getGameMapping(conn, configId));
		mapping.setQuestMapping(getQuestMapping(conn,configId));
		mapping.setAchievementMapping(getAchievementMapping(conn, configId));
		mapping.setBadgeMapping(getBadgeMapping(conn, configId));
		mapping.setActionMapping(getActionMapping(conn, configId));
		mapping.setLevelMapping(getLevelMapping(conn, configId));
		mapping.setStreakMapping(getStreakMapping(conn,configId));
		return mapping;
	}

	
	

	private List<GameMapping> getGameMapping(Connection conn, String configId) throws SQLException{
		List<GameMapping> result = new ArrayList<GameMapping>();
		stmt= conn.prepareStatement("SELECT game_id, listen_to FROM listen.game_info WHERE config_id = ?");
		stmt.setString(1, configId);
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			GameMapping gm = new GameMapping();
			gm.setGameId(rs.getString("game_id"));
			gm.setListenTo(rs.getString("listen_to"));
			result.add(gm);
		}
		return result;
	}
	
	private List<QuestMapping> getQuestMapping(Connection conn, String configId) throws SQLException {
		List<QuestMapping> result = new ArrayList<>();
		stmt= conn.prepareStatement("SELECT game_id, quest_id, listen_to FROM listen.quest_info WHERE config_id = ?");
		stmt.setString(1, configId);
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			QuestMapping qm = new QuestMapping();
			qm.setGameId(rs.getString("game_id"));
			qm.setQuestId(rs.getString("quest_id"));
			qm.setListenTo(rs.getString("listen_to"));
			result.add(qm);
		}
		return result;
	}
	
	private List<AchievementMapping> getAchievementMapping(Connection conn, String configId) throws SQLException{
		List<AchievementMapping> result = new ArrayList<>();
		stmt= conn.prepareStatement("SELECT game_id, achievement_id, listen_to FROM listen.achievement_info WHERE config_id = ?");
		stmt.setString(1, configId);
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			AchievementMapping am = new AchievementMapping();
			am.setGameId(rs.getString("game_id"));
			am.setAchievementId(rs.getString("achievement_id"));
			am.setListenTo(rs.getString("listen_to"));
			result.add(am);
		}
		return result;
	}
	
	private List<BadgeMapping> getBadgeMapping(Connection conn, String configId) throws SQLException{
		List<BadgeMapping> result = new ArrayList<>();
		stmt= conn.prepareStatement("SELECT game_id, badge_id, listen_to FROM listen.badge_info WHERE config_id = ?");
		stmt.setString(1, configId);
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			BadgeMapping bm = new BadgeMapping();
			bm.setGameId(rs.getString("game_id"));
			bm.setBadgeId(rs.getString("badge_id"));
			bm.setListenTo(rs.getString("listen_to"));
			result.add(bm);
		}
		return result;
	}
	
	private List<ActionMapping> getActionMapping(Connection conn, String configId) throws SQLException{
		List<ActionMapping> result = new ArrayList<>();
		stmt= conn.prepareStatement("SELECT game_id, action_id, listen_to FROM listen.action_info WHERE config_id = ?");
		stmt.setString(1, configId);
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			ActionMapping am = new ActionMapping();
			am.setGameId(rs.getString("game_id"));
			am.setActionId(rs.getString("action_id"));
			am.setListenTo(rs.getString("listen_to"));
			result.add(am);
		}
		return result;
	}
	
	private List<LevelMapping> getLevelMapping(Connection conn, String configId) throws SQLException{
		List<LevelMapping> result = new ArrayList<>();
		stmt= conn.prepareStatement("SELECT game_id, level_num, listen_to FROM listen.level_info WHERE config_id = ?");
		stmt.setString(1, configId);
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			LevelMapping lm = new LevelMapping();
			lm.setGameId(rs.getString("game_id"));
			lm.setLevelNumber(rs.getInt("level_num"));
			lm.setListenTo(rs.getString("listen_to"));
			result.add(lm);
		}
		return result;
	}
	
	private List<StreakMapping> getStreakMapping(Connection conn, String configId) throws SQLException{
		List<StreakMapping> result = new ArrayList<>();
		stmt= conn.prepareStatement("SELECT game_id, streak_id, listen_to FROM listen.streak_info WHERE config_id = ?");
		stmt.setString(1, configId);
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			StreakMapping sm = new StreakMapping();
			sm.setGameId(rs.getString("game_id"));
			sm.setStreakId(rs.getString("streak_id"));
			sm.setListenTo(rs.getString("listen_to"));
			result.add(sm);
		}
		return result;
	}

	public JSONObject getTimeWithId(Connection conn, String configId) throws SQLException {
		String timesTamp = null;
		String lastStatementTimeStamp = null;
		stmt= conn.prepareStatement("SELECT times, times2 FROM listener.times WHERE config_id = ?");
		stmt.setString(1, configId);
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			timesTamp = rs.getString("times");
			lastStatementTimeStamp = rs.getString("times2");
		}
		JSONObject timeStamps = new JSONObject();
		timeStamps.put("timestamp", timesTamp);
		timeStamps.put("laststatement", lastStatementTimeStamp);
		return timeStamps;
	}

	public void setTime(Connection conn, String configId, String timestamp, String lastSatementTimeStamp) throws SQLException {
		stmt = conn.prepareStatement("DELETE FROM listener.times WHERE config_id = ?");
		stmt.setString(1, configId);
		stmt.executeUpdate();
		
		stmt = conn
				.prepareStatement("INSERT INTO listener.times (config_id, times, times2) VALUES (?, ?, ?)");
		stmt.setString(1, configId);
		stmt.setString(2, timestamp);
		stmt.setString(3, lastSatementTimeStamp);
		stmt.executeUpdate();
	}

	public void registerObserver(Connection conn, String configId, URL url) throws SQLException {
		//check if url is already registered to this configuration
		String urlString = url.toString();
		stmt = conn.prepareStatement("SELECT * FROM listener.observers WHERE config_id = ? AND observers = ?");
		stmt.setString(1, configId);
		stmt.setString(2, urlString);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			return;
		}
		stmt = conn
				.prepareStatement("INSERT INTO listener.observers (config_id, observers) VALUES (?, ?)");
		stmt.setString(1, configId);
		stmt.setString(2, urlString);
		stmt.executeUpdate();
	}

	public List<String> getObservers(Connection conn, String configId) throws SQLException{
		List<String> result = new ArrayList<String>();
		stmt= conn.prepareStatement("SELECT observers FROM listener.observers WHERE config_id = ?");
		stmt.setString(1, configId);
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			String observer = rs.getString("observers");
			result.add(observer);
		}
		return result;
	}
}
