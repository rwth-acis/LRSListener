package i5.las2peer.services.gamification.listener;

public class LevelMapping {
	private String gameId;
	private int levelNumber;
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
