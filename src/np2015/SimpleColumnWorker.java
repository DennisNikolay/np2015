package np2015;

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleColumnWorker extends Observable implements Runnable{

	/**
	 * A hash map (row -> dobule) containing all node values of the column the thread is working at.
	 */
	private TIntDoubleHashMap vertex=new TIntDoubleHashMap();
	
	/**
	 * A second hash map to compare the old with the new value considering global convergence.
	 */
	private TIntDoubleHashMap oldVertex=new TIntDoubleHashMap();
	
	/**
	 * The accumulator lists. For each neighbor one list.
	 */
	private TIntDoubleHashMap leftAcc=new TIntDoubleHashMap();
	private TIntDoubleHashMap rightAcc=new TIntDoubleHashMap();
	
	/**
	 * The exchanger to exchange the accumulator lists between two adjacent threads/colum worker.
	 */
	private Exchanger<TIntDoubleHashMap> exchangeLeft;
	private Exchanger<TIntDoubleHashMap> exchangeRight;
	
	/**
	 * The sum of all node values within the same column. Again two fields in order to check global convergence.
	 */
	private double oldValueSum;
	private double valueSum;
	
	/**
	 * The column's index.
	 */
	private int columnIndex;
	
	/**
	 * The propagation rates of one column.
	 */
	private TDoubleArrayList edges = new TDoubleArrayList();
	
	/**
	 * The total amount if local iterations.
	 */
	private int totalIterCounter=0;
	
	/**
	 * The current amount of iterations concerning the left/right accumulators.
	 */
	private int leftIterCounter=0;
	private int rightIterCounter=0;
	
	/**
	 * The amount of iterations that have to be made before exchanging with the right/left neighbor.
	 */
	private int numLeft=100;
	private int numRight=100;
	
	/**
	 * An concurrently safe bool flag that is used to terminate the thread when global convergence is detected by the observer.
	 */
	private AtomicBoolean terminate=new AtomicBoolean(false);

	/**
	 * The column worker constructor. One instance is representing a thread that calculates the node values of one single column.
	 * 
	 * @param column - The thread's column index.
	 * @param el	- The thread's left exchanger.
	 * @param er	- The threas's right exchanger.
	 */
	public SimpleColumnWorker(int column, Exchanger<TIntDoubleHashMap> el, Exchanger<TIntDoubleHashMap> er) {
		columnIndex=column;
		
		// Calculating the propagation rates of each node, given the @GraphInfo information.
		for (int i = 0; i < NPOsmose.ginfo.height * 4; i++) {
			Neighbor n = Neighbor.Left;
			switch (i % 4) {
			case 0:
				n = Neighbor.Left;
				break;
			case 1:
				n = Neighbor.Right;
				break;
			case 2:
				n = Neighbor.Top;
				break;
			case 3:
				n = Neighbor.Bottom;
				break;
			}
			edges.add(NPOsmose.ginfo.getRateForTarget(columnIndex, i / 4, n));
		}
		addObserver(NPOsmose.o);
		NPOsmose.o.addWorker(this);
		exchangeLeft = el;
		exchangeRight = er;
		
	}

	/**
	 * The column worker constructor. One instance is representing a thread that calculates the node values of one single column.
	 * 
	 * @param initialVertexValues	- The initial vertex values of the start node given by the @GraphInfo
	 * @param column	- The thread's column index.
	 * @param el	- The thread's left exchanger.
	 * @param er	- The thread's right exchanger..
	 */
	public SimpleColumnWorker(HashMap<Integer, Double> initialVertexValues,
			int column, Exchanger<TIntDoubleHashMap> el, Exchanger<TIntDoubleHashMap> er) {		
		
		this(column, el, er);
		for (Entry<Integer, Double> e : initialVertexValues.entrySet()) {
			vertex.put(e.getKey(), e.getValue());
		}
	}

	/**
	 * The column worker constructor. One instance is representing a thread that calculates the node values of one single column.
	 * 
	 * @param initialVertexValues	- The initial vertex values of the start node given by the @GraphInfo
	 * @param column	- The thread's column index.
	 * @param el	- The thread's left exchanger.
	 * @param er	- The thread's right exchanger..
	 */
	public SimpleColumnWorker(TIntDoubleHashMap initialVertexValues, int column, Exchanger<TIntDoubleHashMap> el,
			Exchanger<TIntDoubleHashMap> er) {
		
		this(column, el, er);
		vertex=initialVertexValues;
		
	}
	
	/**
	 * Gets the propagation rate of one node to a specific direction.
	 * 
	 * @param y	- The row of the node.
	 * @param dir - The direction to propagate at.
	 * @return
	 */
	private double getEdge(int y, Neighbor dir){
		switch(dir){
		case Left:
			return edges.get(4*y+0);
		case Right:
			return edges.get(4*y+1);
		case Top:
			return edges.get(4*y+2);
		case Bottom:
			return edges.get(4*y+3);
		default:
			return 0;		
		}
	}

	/**
	 * The main method of the column worker. While not terminated or interrupted by the observer the column worker/thread iterates
	 * over its nodes and propagates the values to its neighbors.
	 */
	public void run() {
		NPOsmose.o.addThread(Thread.currentThread());
		TIntDoubleHashMap gotLeft=null;
		TIntDoubleHashMap gotRight=null;
		while (!shouldTerminate() && !Thread.interrupted() && totalIterCounter != Integer.MAX_VALUE) {
			double sum=0;
			TIntDoubleHashMap tmpMap=new TIntDoubleHashMap();
			
			// Iterate over all nodes within the column.
			for(TIntDoubleIterator iter=vertex.iterator(); iter.hasNext(); ){
				iter.advance();
				sum+=iter.value();
				// Propagated left in previous iteration?
				if(gotLeft!=null && !gotLeft.isEmpty()){
					if(gotLeft.containsKey(iter.key())){
						iter.setValue((iter.value()+gotLeft.get(iter.key())));
						gotLeft.remove(iter.key());
					}
				}
				// Propagated right in previous iteration?
				if(gotRight!=null && !gotRight.isEmpty()){
					if(gotRight.containsKey(iter.key())){
						iter.setValue((iter.value()+gotRight.get(iter.key())));
						gotRight.remove(iter.key());
					}
				}
				
				// Calculating propagation values.
				double propagateTop=iter.value()*getEdge(iter.key(), Neighbor.Top);
				double propagateBottom=iter.value()*getEdge(iter.key(), Neighbor.Bottom);
				double propagateLeft=iter.value()*getEdge(iter.key(), Neighbor.Left);
				double propagateRight=iter.value()*getEdge(iter.key(), Neighbor.Right);
				
				// Update own value.
				tmpMap.adjustOrPutValue(iter.key(),iter.value()-propagateTop-propagateBottom-propagateLeft-propagateRight,iter.value()-propagateTop-propagateBottom-propagateLeft-propagateRight);
				
				// Set accumulators left.
				if(columnIndex>0){
					addLeftAcc(iter.key(), propagateLeft, totalIterCounter);
				}
				// Set accumulators right.
				if(columnIndex!=NPOsmose.ginfo.width-1){
					addRightAcc(iter.key(), propagateRight, totalIterCounter);
				}
				
				// Propagate to vertical neighbors.
				if(propagateTop!=0 && iter.key()!=0){
					tmpMap.adjustOrPutValue(iter.key()-1, propagateTop, propagateTop);
				}
				if(propagateBottom!=0 && iter.key()!=NPOsmose.ginfo.height-1){
					tmpMap.adjustOrPutValue(iter.key()+1, propagateBottom, propagateBottom);
				}
			}
			// Iteration finished. Update vertexValues.
			setVertex(tmpMap);
			
			rightIterCounter++;
			leftIterCounter++;
			// Check if horizontal iteration is necessary. If yes, do so.
			if(leftIterCounter==numLeft && !shouldTerminate() && !Thread.interrupted()){
				gotLeft=exchangeLeftAccValues();
				leftIterCounter=0;
				leftAcc=new TIntDoubleHashMap();
			}
			if(rightIterCounter==numRight && !shouldTerminate() && !Thread.interrupted()){
				gotRight=exchangeRightAccValues();
				rightIterCounter=0;
				rightAcc=new TIntDoubleHashMap();
			}
			
			totalIterCounter++;
			// Update value some.
			setValueSum(sum);
			
		}
		// Read out the calculated vertex values of the processed column before terminating.
		synchronized (NPOsmose.class) { NPOsmose.result.put(getColumnIndex(), getVertexValues()); }

	}
	
	/**
	 * Exchange the accumulator to the given neighbor.
	 * 
	 * @param left	- The given neighbor to exchange with.
	 * @return	- The received values from the neighbor. If it's empty, the neighbor did not propagate anything.
	 */
	private TIntDoubleHashMap exchangeAcc(boolean left){

		Exchanger<TIntDoubleHashMap> ex;
		TIntDoubleHashMap acc;
		if(left){
			ex=exchangeLeft;
			acc=leftAcc;
		}else{
			ex=exchangeRight;
			acc=rightAcc;
		}
		if(ex==null){
			if(acc!=null && !acc.isEmpty()){
				ex=new Exchanger<TIntDoubleHashMap>();
				Runnable r;
				if(left){
					this.exchangeLeft=ex;
					TIntDoubleHashMap accCopy=new TIntDoubleHashMap();
					accCopy.putAll(acc);
					acc=new TIntDoubleHashMap();
					r = new SimpleColumnWorker(accCopy, columnIndex-1, null, ex);
				}else{
					this.exchangeRight=ex;
					TIntDoubleHashMap accCopy=new TIntDoubleHashMap();
					accCopy.putAll(acc);
					acc=new TIntDoubleHashMap();
					r = new SimpleColumnWorker(accCopy, columnIndex+1, ex, null);
				}
				new Thread(r).start();
				return new TIntDoubleHashMap();
			}else{
				if(left){
					numLeft=1;
				}else{
					numRight=1;
				}
			}
		}else{
				try {
					TIntDoubleHashMap accCopy=new TIntDoubleHashMap();
					accCopy.putAll(acc);
					if(shouldTerminate() || Thread.interrupted()){
						return new TIntDoubleHashMap();
					}else{
						TIntDoubleHashMap got=ex.exchange(accCopy);
						calculateIter(acc, got, left);
						acc=new TIntDoubleHashMap();
						return got;
					}
				} catch (InterruptedException e) {
					return new TIntDoubleHashMap();
				}
		}

		return null;
		
		
	}
	
	/**
	 * The new amount of iterations which have to be made before the next horizontal propagation.
	 * Returns 1 if the thread assums local convergence, >1 otherwise.
	 * 
	 * @param own
	 * @param other
	 * @param left
	 */
	private void calculateIter(TIntDoubleHashMap own, TIntDoubleHashMap other, boolean left){
		double result=0;
		for(TIntDoubleIterator iter=own.iterator(); iter.hasNext();){
			iter.advance();
			result+=iter.value();
		}
		for(TIntDoubleIterator iter=other.iterator(); iter.hasNext();){
			iter.advance();
			result-=iter.value();
		}
		if(left){
			result = Math.abs(result);
			if (result <= NPOsmose.epsilon) {
				numLeft=1;
				if(numLeft==1 && numRight==1){
					if(shouldTerminate() || Thread.interrupted()){
						return;
					}
					setChanged();
					notifyObservers(columnIndex);
				}
				return;
			}
			numLeft=(int) Math.min(Math.floor(result / NPOsmose.epsilon), 100);
		}else{
			result = Math.abs(result);
			if (result <= NPOsmose.epsilon) {
				numRight=1;
				if(numLeft==1 && numRight==1){
					if(shouldTerminate() || Thread.interrupted()){
						return;
					}
					setChanged();
					notifyObservers(columnIndex);
				}
				return;
			}
			numRight=(int) Math.min(Math.floor(result / NPOsmose.epsilon), 100);
		}
	}
	
	public double getValueSum() {
		return valueSum;
	}

	/**
	 * Terminates the thread.
	 */
	public void terminate() {
		terminate.set(true);
	}

	public TIntDoubleHashMap exchangeLeftAccValues() {
		return exchangeAcc(true);
	}

	public TIntDoubleHashMap exchangeRightAccValues() {
		return exchangeAcc(false);
	}

	/**
	 * Put the propagation value to the accumulator list.
	 * @param y	- The row index of the node.
	 * @param value	- The propagation value.
	 * @param numIter
	 * @param left	- The neighbor to propagate to.
	 */
	private void addAcc(int y, double value, int numIter, boolean left){
		TIntDoubleHashMap acc;
		if(left){
			acc=leftAcc;
		}else{
			acc=rightAcc;
		}
		if(acc.containsKey(y)){
			acc.put(y, acc.get(y)+value);
		}else{
			if(value!=0){
				acc.put(y, value);
			}
		}
	}
	
	
	public void addLeftAcc(int y, double value, int numIter) {
		addAcc(y, value, numIter, true);
	}

	
	public void addRightAcc(int y, double value, int numIter) {
		addAcc(y, value, numIter, false);

	}

	
	public TIntDoubleHashMap getVertexValues() {
		return new TIntDoubleHashMap(vertex);
	}

	public TIntDoubleHashMap getOldVertexValues() {
		return new TIntDoubleHashMap(oldVertex);
	}

	
	public int getColumnIndex() {
		return columnIndex;
	}

	public boolean shouldTerminate() {
		return terminate.get();
	}
	
	synchronized public void setValueSum(double sum) {
		this.oldValueSum=valueSum;
		this.valueSum=sum;
	}
	
	public double getOldValueSum(){
		return oldValueSum;
	}

	public int getNumLeft(){
		return numLeft;
	}
	public int getNumRight(){
		return numRight;
	}
	
	private void setVertex(TIntDoubleHashMap v) {
		oldVertex = vertex;
		vertex = v;
	}

}
