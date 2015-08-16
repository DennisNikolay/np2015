package np2015;

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Observing all running threads according to convergence.
 * 
 * @author Mo
 */
public class GlobalObserver extends Thread implements Observer {

	private boolean allTerminated=false;
	
	private Lock observerLock=new ReentrantLock();
	private Condition notified=observerLock.newCondition();
	/**
	 * Map from worker to boolean. The boolean 
	 */
	private LinkedList<SimpleColumnWorker> workers = new LinkedList<SimpleColumnWorker>();
	private LinkedList<Integer> threadsColumns=new LinkedList<Integer>();
	
	public GlobalObserver() {
		super();
	}
	
	/**
	 * To be called by every column worker once.
	 * 
	 * @param worker
	 */
	public synchronized void addWorker(SimpleColumnWorker worker) {
		try{
			observerLock.lock();
			workers.add(worker);
			threadsColumns.add(worker.getColumnIndex());
		}finally{
			observerLock.unlock();
		}
		NPOsmose.incrementWorkersActive();
	}

	@Override
	public void run(){
		observerLock.lock();
		while (!checkGlobalConvergence(workers)) {
			try {
				notified.await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		observerLock.unlock();
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
		//System.out.println("All terminated");
		NPOsmose.lock.lock();
		NPOsmose.condition.signal();
		NPOsmose.lock.unlock();


	}
	
	/**
	 * Triggered by notifying thread suspecting local convergence.
	 */
	@Override
	public synchronized void update(Observable o, Object arg) {
		threadsColumns.remove(arg);
		if(threadsColumns.isEmpty()){
			observerLock.lock();
			notified.signal();
			observerLock.unlock();
		}
	}

	private boolean checkGlobalConvergence(LinkedList<SimpleColumnWorker> l) {
		for(SimpleColumnWorker w : workers){
			if(w.getNumLeft()!=1 || w.getNumRight()!=1){
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
