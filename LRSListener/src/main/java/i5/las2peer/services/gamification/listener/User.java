package i5.las2peer.services.gamification.listener;

public class User {
	private String name;
	private String mail;
	private String token;
	private String l2pAuth;
	
	public User(String name, String mail, String token, String l2pAuth) {
		this.name = name;
		this.mail= mail;
		this.token= token;
		this.l2pAuth = l2pAuth;
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
	 * @return the mail
	 */
	public String getMail() {
		return mail;
	}
	/**
	 * @param mail the mail to set
	 */
	public void setMail(String mail) {
		this.mail = mail;
	}
	/**
	 * @return the token
	 */
	public String getToken() {
		return token;
	}
	/**
	 * @param token the token to set
	 */
	public void setToken(String token) {
		this.token = token;
	}

	/**
	 * @return the l2pAuth
	 */
	public String getL2pAuth() {
		return l2pAuth;
	}

	/**
	 * @param l2pAuth the l2pAuth to set
	 */
	public void setL2pAuth(String l2pAuth) {
		this.l2pAuth = l2pAuth;
	}
}
