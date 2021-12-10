package i5.las2peer.services.gamification.configurator;

public class ActionMapping {
	private String gameId;
	private String actionId;
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
}
