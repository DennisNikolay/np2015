package impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import np2015.NPOsmose;

/**
 * Observing all running threads according to convergence.
 * @author Mo
 */
public class GlobalObserver implements Observer {

	/**
	 * The counter which represents the threads which are already terminated because of local convergence.
	 */
	int alreadyFinished = 0;
	
	private List<ColumnWorker> workers = new ArrayList<ColumnWorker>();
	

	/**
	 * To be called by every column worker once.
	 * @param worker
	 */
	public void addWorker(ColumnWorker worker) {
		workers.add(worker);
		NPOsmose.incrementWorkersActive();
	}
	
	
	/**
	 * Triggered by notifying thread suspecting local convergence.
	 */
	@Override
	public void update(Observable o, Object arg) {
		
		alreadyFinished++;
		if (NPOsmose.getWorkersActive() == alreadyFinished) {
			if (checkGlobalConvergence()) {
				for (Iterator<ColumnWorker> iterator = workers.iterator(); iterator.hasNext();) {
					ColumnWorker columnWorker = (ColumnWorker) iterator.next();
					columnWorker.terminate();
				}
			}
			
		}
	}
	
	

	private boolean checkGlobalConvergence() {
		return true;
	}
	
}
