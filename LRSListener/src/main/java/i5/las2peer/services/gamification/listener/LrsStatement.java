package i5.las2peer.services.gamification.listener;

import java.util.Date;

public class LrsStatement {
	private String actor;
	private String verb;
	private String what;
	private Date timeStamp;
	/**
	 * @return the actor
	 */
	public String getActor() {
		return actor;
	}
	/**
	 * @param actor the actor to set
	 */
	public void setActor(String actor) {
		this.actor = actor;
	}
	/**
	 * @return the verb
	 */
	public String getVerb() {
		return verb;
	}
	/**
	 * @param verb the verb to set
	 */
	public void setVerb(String verb) {
		this.verb = verb;
	}
	/**
	 * @return the what
	 */
	public String getWhat() {
		return what;
	}
	/**
	 * @param what the what to set
	 */
	public void setWhat(String what) {
		this.what = what;
	}
	/**
	 * @return the timeStamp
	 */
	public Date getTimeStamp() {
		return timeStamp;
	}
	/**
	 * @param timeStamp the timeStamp to set
	 */
	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}
}
