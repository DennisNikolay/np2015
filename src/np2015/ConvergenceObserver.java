package np2015;

import gnu.trove.list.array.TIntArrayList;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConvergenceObserver implements Observer, Runnable {

	private LinkedList<DoubleColumnWorker> workers=new LinkedList<DoubleColumnWorker>();
	private TIntArrayList localConvergence=new TIntArrayList();
	private Lock lock=new ReentrantLock();
	private Condition allLocalConvergent=lock.newCondition();

	private boolean allTerm=false;;
	
	synchronized public void addWorker(DoubleColumnWorker w){
		localConvergence.add(w.getColumnIndex());
		workers.add(w);
	}
	
	@Override
	public void update(Observable arg0, Object arg1) {
		localConvergence.remove((int)arg1);
		if(localConvergence.isEmpty()){
			this.run();
		}
		
	}

	@Override
	synchronized public void run() {
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
	
	private boolean globalConvergent(){
		double sum=0;
		int iterCounter=0;
		for(DoubleColumnWorker w:workers){;
			sum+=(w.getOldValueSum()-w.getValueSum())*(w.getOldValueSum()-w.getValueSum());
			iterCounter=w.iterCounter;
		}
		if(Math.sqrt(sum)<NPOsmose.epsilon){
			return true;
		}
		if(iterCounter % 10000==0)
			System.out.println(Math.sqrt(sum)+" epsilon: "+NPOsmose.epsilon);
		return false;
	}

	
}
