package np2015;

import gnu.trove.list.array.TIntArrayList;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Observing all running threads according to convergence.
 */
public class ConvergenceObserver implements Observer {
	
	/**
	 * A list of all active threads/column workers.
	 */
	private LinkedList<DoubleColumnWorker> workers=new LinkedList<DoubleColumnWorker>();
	
	/**
	 * A list of all active threads/column workers which are not converged locally yet.
	 */
	private TIntArrayList localConvergence=new TIntArrayList();


	/**
	 * Are all threads terminated/terminating yet?
	 */
	private boolean allTerm=false;;
	
	
	/**
	 * To be called by every column worker once. Adds the column worker to the worker lists.
	 * 
	 * @param worker	- The thread which has just started working.
	 */
	synchronized public void addWorker(DoubleColumnWorker w){
		localConvergence.add(w.getColumnIndex());
		workers.add(w);
	}
	
	/**
	 * Triggered by notifying thread suspecting local convergence.
	 */
	@Override
	synchronized public void update(Observable arg0, Object arg1) {
		localConvergence.remove((int)arg1);
		if(localConvergence.isEmpty()){
			checkConvergence();
		}
		
	}

	/**
	 * Checks global convergence.
	 */
	private void checkConvergence() {
		if(globalConvergent()){
			for(DoubleColumnWorker w:workers){
				NPOsmose.result.put(w.getColumnIndex(), w.getVertexValues());
			}
			
			synchronized(NPOsmose.class){
				for(Thread t: NPOsmose.threads){
					t.interrupt();
				}
			}

			allTerm=true;
			try{
				NPOsmose.lock.lock();
				NPOsmose.condition.signal();
			}finally{
				NPOsmose.lock.unlock();
			}
		}
	}

	public boolean allTerminated(){
		return allTerm;
	}
	
	/**
	 * Calculates the euclidean norm of the old and current value sums.
	 * @return true if it's global convergent.
	 */
	private boolean globalConvergent(){
		double sum=0;
		for(DoubleColumnWorker w:workers){;
			sum+=(w.getOldValueSum()-w.getValueSum())*(w.getOldValueSum()-w.getValueSum());
		}
		if(Math.sqrt(sum)<NPOsmose.epsilon){
			return true;
		}
		return false;
	}

	
}
