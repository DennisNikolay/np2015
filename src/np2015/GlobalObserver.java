package np2015;

import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

/**
 * Observing all running threads according to convergence.
 * 
 * @author Mo
 */
public class GlobalObserver implements Observer {

	/**
	 * The counter which represents the threads which are already terminated
	 * because of local convergence.
	 */
	private int alreadyFinished = 0;

	/**
	 * Mapping from worker to pair of two double values representing old and current valueSum of one worker.
	 */
	private HashMap<ColumnWorker, Double> workers = new HashMap<ColumnWorker, Double>();

	/**
	 * To be called by every column worker once.
	 * 
	 * @param worker
	 */
	public void addWorker(ColumnWorker worker) {
		workers.put(worker, worker.getValueSum());
		NPOsmose.incrementWorkersActive();
	}

	/**
	 * Triggered by notifying thread suspecting local convergence.
	 */
	@Override
	public void update(Observable o, Object arg) {
		// TODO check for data race if multiple threads notify concurrently??
		// TODO 
		increment();
		Set<ColumnWorker> l = workers.keySet();
		if (checkGlobalConvergence(l)) {
			
			// terminate threads
			for (ColumnWorker columnWorker : l) {
				columnWorker.terminate();
			}
		}

	}

	private boolean checkGlobalConvergence(Set<ColumnWorker> l) {
		// all threads have converged locally
		if (NPOsmose.getWorkersActive() == alreadyFinished) {
			// Compare old with current value considering epsilon.
			for (ColumnWorker columnWorker : l) {
				double oldValue = workers.get(columnWorker).doubleValue();
				if (oldValue - columnWorker.getValueSum() > NPOsmose.epsilon) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	private synchronized void increment(){
		alreadyFinished++;
	}

}
