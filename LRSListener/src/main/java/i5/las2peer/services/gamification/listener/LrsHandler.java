package i5.las2peer.services.gamification.listener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import i5.las2peer.api.Context;
import i5.las2peer.api.logging.MonitoringEvent;

public class LrsHandler implements Runnable{
	private Map<String,String> map = Collections.synchronizedMap(new HashMap<String,String>());
	@Override
	public void run() {
		Mapping mapping = getMappingFromConfigurator();
		
		Statement[] statements = retriveStatements();
		for (Statement statement : statements) {
			String result = executeGamification(statement, map);
			Context.get().monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_MESSAGE_14, result);
		}
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Mapping getMappingFromConfigurator() {
		// TODO Auto-generated method stub
		return null;
	}

	

	private Statement[] retriveStatements() {
		// TODO Auto-generated method stub
		return null;
	}

	public void add(Map<String,String> map) {
		this.map = Collections.synchronizedMap(map);
	}
	
	private String executeGamification(Statement statement, Map<String, String> map2) {
		// TODO Auto-generated method stub
		return null;
	}
}
