package i5.las2peer.services.gamification.listener;

public class LrsWorker implements Runnable{

	private LrsHandler handler;

	public LrsWorker(LrsHandler handler) {
		this.handler = handler;
	}
	
	@Override
	public void run() {
		//keep this thread alive
		while (!Thread.currentThread().isInterrupted()) {
			handler.handle();
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
