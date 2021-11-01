package i5.las2peer.services.gamification.listener;

public class StreakMapping {
	private String gameId;
	private String streakId;
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
