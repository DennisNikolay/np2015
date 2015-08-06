package impl;

import java.util.Observable;
import java.util.Observer;

/**
 * Observing all running threads according to convergence.
 * @author Mo
 */
public class GlobalObserver implements Observer {

	/**
	 * The counter which represents the threads which are already terminated because of local convergence.
	 */
	int alreadyTerminated = 0;
	
	/**
	 * As soons as the difference between the current valueSum (@ColumnWorker) and
	 * the former after one iteration is lower than this value we assume convergence.
	 */
	private final double epsilon;
	
	
	
	public GlobalObserver(double e) {
		epsilon = e;
	}
	
	
	/**
	 * Triggered by notifying thread suspecting local convergence.
	 */
	@Override
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		if(checkLocalConvergence((ColumnWorker) o )) {
			checkGlobalConvergence();
		}
		
		
	}
	
	private boolean checkLocalConvergence(ColumnWorker o) {
		// TODO Auto-generated method stubo.terminate()
		o.terminate();
		alreadyTerminated++;		
		return true;
	}

	private void checkGlobalConvergence() {
		
	}
	
}
