package impl;

import java.util.Observable;
import java.util.Observer;


public class GlobalObserver implements Observer {

	/**
	 * The counter which represents the threads which are already terminated because of local convergence.
	 */
	int alreadyTerminated = 0;
	
	@Override
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		
		
	}
	
	private checkGlobalConvergence() {
		
	}
	
}
