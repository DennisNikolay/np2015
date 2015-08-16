package np2015;

import gnu.trove.iterator.TIntDoubleIterator;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

/**
 * Observing all running threads according to convergence.
 * 
 * @author Mo
 */
public class GlobalObserver implements Observer {

	private boolean allTerminated=false;
	
	/**
	 * Map from worker to boolean. The boolean 
	 */
	private LinkedList<SimpleColumnWorker> workers = new LinkedList<SimpleColumnWorker>();
	
	public GlobalObserver() {
		super();
	}
	
	/**
	 * To be called by every column worker once.
	 * 
	 * @param worker
	 */
	public synchronized void addWorker(SimpleColumnWorker worker) {
		workers.add(worker);
		NPOsmose.incrementWorkersActive();
	}

	/**
	 * Triggered by notifying thread suspecting local convergence.
	 */
	@Override
	public synchronized void update(Observable o, Object arg) {
		if(Thread.interrupted()){
			return;
		}
		boolean b=true;
		for(SimpleColumnWorker w : workers){
			if(w.getNumLeft()!=1 || w.getNumRight()!=1){
				b=false;
			}
		}
		if (b) {
			// all threads have converged locally
			if (checkGlobalConvergence(workers)) {
				boolean ter = false;
				for (SimpleColumnWorker w : workers) {
					for(TIntDoubleIterator iter = w.getOldVertexValues().iterator(); iter.hasNext();) {
						iter.advance();
						if (Math.abs(iter.value() - w.getVertexValues().get(iter.key())) > NPOsmose.epsilon) {
							ter = true;
						}
						
					}
				}
				if (ter) {
					return;
				}	
				// terminate threads
				for (SimpleColumnWorker columnWorker : workers) {
					columnWorker.terminate();	
				}
				synchronized (NPOsmose.class) {
					for(Thread t: NPOsmose.threads){
						t.interrupt();
					}
				}
				
				allTerminated=true;
				NPOsmose.lock.lock();
				NPOsmose.condition.signal();
				NPOsmose.lock.unlock();
			}
		}

	}

	private boolean checkGlobalConvergence(LinkedList<SimpleColumnWorker> l) {
		// Compare old with current value considering epsilon.
		for (SimpleColumnWorker columnWorker : l) {
			double oldValue = columnWorker.getOldValueSum();
			if (Math.abs(oldValue - columnWorker.getValueSum()) > NPOsmose.epsilon) {
				return false;
			}
		}
		return true;
	}

	public synchronized boolean allTerminated(){
		return allTerminated;
	}
	
	public synchronized void addThread(Thread t){
		NPOsmose.threads.add(t);
	}
	
}