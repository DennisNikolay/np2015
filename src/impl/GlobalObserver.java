package impl;

import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import javax.vecmath.Point2d;

import np2015.NPOsmose;

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
	int alreadyFinished = 0;

	/**
	 * Mapping from worker to pair of two double values representing old and current valueSum of one worker.
	 */
	private HashMap<ColumnWorker, Point2d> workers = new HashMap<ColumnWorker, Point2d>();

	/**
	 * To be called by every column worker once.
	 * 
	 * @param worker
	 */
	public void addWorker(ColumnWorker worker) {
		Point2d p = new Point2d(worker.getValueSum(), 0);
		workers.put(worker, p);
		NPOsmose.incrementWorkersActive();
	}

	/**
	 * Triggered by notifying thread suspecting local convergence.
	 */
	@Override
	public void update(Observable o, Object arg) {
		// TODO check for data race if multiple threads notify concurrently??
		// TODO 
		alreadyFinished++;
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
			//TODO compare old and new value. and update values.
			return true;
		}
		return false;
	}

}
