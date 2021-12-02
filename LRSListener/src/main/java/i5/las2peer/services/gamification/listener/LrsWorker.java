package i5.las2peer.services.gamification.listener;

import java.time.LocalDateTime;

public class LrsWorker implements Runnable{

	private LrsHandler handler;
	private final LocalDateTime expirationDate;

	public LrsWorker(LrsHandler handler, LocalDateTime expirationDate) {
		this.expirationDate = expirationDate;
		this.setHandler(handler);
	}
	
	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted() && !expired()) {
			getHandler().handle();
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean expired() {
		LocalDateTime now = LocalDateTime.now();
		if (getExpirationDate().isBefore(now)) {
			return true;
		}
		return false;
	}

	/**
	 * @return the handler
	 */
	public LrsHandler getHandler() {
		return handler;
	}

	/**
	 * @param handler the handler to set
	 */
	public void setHandler(LrsHandler handler) {
		this.handler = handler;
	}

	/**
	 * @return the expirationDate
	 */
	public LocalDateTime getExpirationDate() {
		return expirationDate;
	}
}
