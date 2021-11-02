package i5.las2peer.services.gamification.listener;

public abstract class ElementMapping {
	private String gameId;
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
