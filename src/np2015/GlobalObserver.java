package np2015;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;

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
	private HashMap<SimpleColumnWorker, Boolean> workers = new HashMap<SimpleColumnWorker, Boolean>();
	//private HashMap<SimpleColumnWorker, Double> workersOld = new HashMap<SimpleColumnWorker, Double>();

	
	public GlobalObserver() {
		super();
	}
	
	/**
	 * To be called by every column worker once.
	 * 
	 * @param worker
	 */
	public synchronized void addWorker(SimpleColumnWorker worker) {
		workers.put(worker, false);
		NPOsmose.incrementWorkersActive();
	}

	/**
	 * Triggered by notifying thread suspecting local convergence.
	 */
	@Override
	public synchronized void update(Observable o, Object arg) {
		if(Thread.currentThread().isInterrupted()){
			return;
		}
		SimpleColumnWorker scw=((SimpleColumnWorker)o);
		if (!workers.containsKey(scw)) {
			throw new UnsupportedOperationException();
		}else{
			workers.put(scw, true);
		}
		boolean b=true;
		for(Entry<SimpleColumnWorker, Boolean> e: workers.entrySet()){
			if(e.getValue()==false){
				System.out.println(e.getKey().getColumnIndex()+" : numLeft -"+e.getKey().getNumLeft()+" ; numRight - "+e.getKey().getNumRight());
				b=false;
			}
		}
		if (b) {
			System.out.println(b);
			// all threads have converged locally
			Set<SimpleColumnWorker> l = workers.keySet();
			//if (checkGlobalConvergence(l)) {
				// terminate threads
				for (SimpleColumnWorker columnWorker : l) {
					columnWorker.terminate();
					
					
				}
				for(Thread t: NPOsmose.threads){
					t.interrupt();
				}
				
				allTerminated=true;
				//System.out.println("All terminated");
				NPOsmose.lock.lock();
				NPOsmose.condition.signal();
				NPOsmose.lock.unlock();

			//}
		}

	}

	private boolean checkGlobalConvergence(Set<SimpleColumnWorker> l) {
		// Compare old with current value considering epsilon.
		for (SimpleColumnWorker columnWorker : l) {
			System.out.println(Math.abs(workersOld.get(columnWorker) - columnWorker.getValueSum())+" > "+NPOsmose.epsilon);
			if (Math.abs(workersOld.get(columnWorker) - columnWorker.getValueSum()) > NPOsmose.epsilon) {
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
	
	public synchronized void addThread(Thread t){
		NPOsmose.threads.add(t);
	}
	
}