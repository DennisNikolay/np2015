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

	private boolean allTerminated=false;
	
	/**
	 * The counter which represents the threads which already assume local convergence.
	 */
	private int alreadyFinished = 0;

	/**
	 * Mapping from worker to pair of two double values representing old and
	 * current valueSum of one worker.
	 */
	private HashMap<ColumnWorker, Double> workers = new HashMap<ColumnWorker, Double>();

	public GlobalObserver() {
		super();
	}
	
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

		if (NPOsmose.getWorkersActive() == alreadyFinished) {
			// all threads have converged locally
			Set<ColumnWorker> l = workers.keySet();
			if (checkGlobalConvergence(l)) {
				// terminate threads
				for (ColumnWorker columnWorker : l) {
					columnWorker.terminate();
				}
				allTerminated=true;
				NPOsmose.condition.notify();
			}
		}

	}

	private boolean checkGlobalConvergence(Set<ColumnWorker> l) {
		// Compare old with current value considering epsilon.
		for (ColumnWorker columnWorker : l) {
			double oldValue = workers.get(columnWorker).doubleValue();
			if (Math.abs(oldValue - columnWorker.getValueSum()) > NPOsmose.epsilon) {
				return false;
			}
		}
		return true;
	}

	private synchronized void increment() {
		alreadyFinished++;
	}

	public synchronized Set<ColumnWorker> getWorkers() {
		return workers.keySet();
	}

	public synchronized boolean allTerminated(){
		return allTerminated;
	}
	
}
