package i5.las2peer.services.gamification.listener;

public class AchievementMapping {
	private String gameId;
	private String achievementId;
	private String listenTo;
	/**
	 * @return the gameId
	 */
	public String getGameId() {
		return gameId;
	}
	/**
	 * @param gameId the gameId to set
	 */
	public void setGameId(String gameId) {
		this.gameId = gameId;
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
	 * @return the listenTo
	 */
	public String getListenTo() {
		return listenTo;
	}
	/**
	 * @param listenTo the listenTo to set
	 */
	public void setListenTo(String listenTo) {
		this.listenTo = listenTo;
	}
}
