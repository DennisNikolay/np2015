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
	private HashMap<SimpleColumnWorker, Double> workers = new HashMap<SimpleColumnWorker, Double>();

	public GlobalObserver() {
		super();
	}
	
	/**
	 * To be called by every column worker once.
	 * 
	 * @param worker
	 */
	public synchronized void addWorker(SimpleColumnWorker worker) {
		workers.put(worker, worker.getValueSum());
		NPOsmose.incrementWorkersActive();
	}

	/**
	 * Triggered by notifying thread suspecting local convergence.
	 */
	@Override
	public void update(Observable o, Object arg) {
		
		if (!workers.containsKey(((SimpleColumnWorker)o).getColumnIndex() )) {
			increment();
			System.out.println("not in");
		}
		
		if (NPOsmose.getWorkersActive() == alreadyFinished) {
			// all threads have converged locally
			Set<SimpleColumnWorker> l = workers.keySet();
			if (checkGlobalConvergence(l)) {
				// terminate threads
				for (SimpleColumnWorker columnWorker : l) {
					columnWorker.terminate();
				}
				allTerminated=true;
				System.out.println("All terminated");
				NPOsmose.lock.lock();
				NPOsmose.condition.signal();
				NPOsmose.lock.unlock();

			}
		}

	}

	private boolean checkGlobalConvergence(Set<SimpleColumnWorker> l) {
		// Compare old with current value considering epsilon.
		for (SimpleColumnWorker columnWorker : l) {
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

	public synchronized Set<SimpleColumnWorker> getWorkers() {
		return workers.keySet();
	}

	public synchronized boolean allTerminated(){
		return allTerminated;
	}
	
}
