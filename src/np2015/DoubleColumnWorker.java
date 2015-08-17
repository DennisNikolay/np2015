package np2015;


import java.util.Observable;
import java.util.concurrent.Exchanger;

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.procedure.TIntDoubleProcedure;



public class DoubleColumnWorker extends Observable implements TIntDoubleProcedure, Runnable{
	/**
	 * A hash map (row -> dobule) containing all node values of the column the thread is working at.
	 */
	private TIntDoubleHashMap vertex=new TIntDoubleHashMap();
	
	/**
	 * The accumulator lists. For each neighbor one list.
	 */
	private TIntDoubleHashMap leftAcc=new TIntDoubleHashMap();
	private TIntDoubleHashMap rightAcc=new TIntDoubleHashMap();
	
	/**
	 * The exchanger to exchange the accumulator lists between two adjacent threads/colum worker.
	 */
	private Exchanger<TIntDoubleHashMap> leftExchanger;
	private Exchanger<TIntDoubleHashMap> rightExchanger;
	
	
	/**
	 * Contains the local propagations.
	 */
	private TIntDoubleHashMap add=new TIntDoubleHashMap();
	
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
	public int iterCounter=0;
	
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
	 * The sum of all node values within the same column. Three different versions in order to check global convergence.
	 */
	private double tmpSum=0;
	private double valueSum=0;
	private double oldValueSum=0;
	
	/**
	 * The column worker constructor. One instance is representing a thread that calculates the node values of one single column.
	 * 
	 * @param map 	- The vertex values.
	 * @param column - The thread's column index.
	 * @param el	- The thread's left exchanger.
	 * @param er	- The threas's right exchanger.
	 */
	public DoubleColumnWorker(TIntDoubleHashMap map, int column, Exchanger<TIntDoubleHashMap> el, Exchanger<TIntDoubleHashMap> er) {
		columnIndex=column;
		NPOsmose.o.addWorker(this);
		this.addObserver(NPOsmose.o);
		
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
		leftExchanger=el;
		rightExchanger=er;
		vertex=map;
	}

	private void setValueSum(double valueSum) {
		this.oldValueSum = this.valueSum;
		this.valueSum = valueSum;
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

	public double getValueSum() {
		return valueSum;
	}
	
	public double getOldValueSum() {
		return oldValueSum;
	}
	
	public int getColumnIndex(){
		return columnIndex;
	}
	
	public TIntDoubleHashMap getVertexValues(){
		return new TIntDoubleHashMap(vertex);
	}

	/**
	 * Exchanges accumulators
	 * @param ex - the exchanger to use
	 * @param accumulator - the accumulator to use
	 * @param left - flag indicating if the change was done to the left
	 */
	private void exchange(Exchanger<TIntDoubleHashMap> ex, TIntDoubleHashMap accumulator, boolean left){
		if(Thread.currentThread().isInterrupted()){
			return;
		}
		if(ex==null && accumulator.isEmpty()){
			if(left){
				numLeft=1;
			}else{
				numRight=1;
			}
			return;
		}
		if(ex==null){
			if(left){
				ex=new Exchanger<TIntDoubleHashMap>();
				leftExchanger=ex;
				DoubleColumnWorker dcw=new DoubleColumnWorker(new TIntDoubleHashMap(accumulator), columnIndex-1, null, ex);
				new Thread(dcw).start();
			}else{
				ex=new Exchanger<TIntDoubleHashMap>();
				rightExchanger=ex;
				DoubleColumnWorker dcw=new DoubleColumnWorker(new TIntDoubleHashMap(accumulator), columnIndex+1, ex, null);
				new Thread(dcw).start();
			}
		}else{
			try {
				TIntDoubleHashMap got=ex.exchange(new TIntDoubleHashMap(accumulator));
				double sum=0;
				for(TIntDoubleIterator iter=got.iterator(); iter.hasNext();){
					iter.advance();
					sum+=iter.value();
				}
				for(TIntDoubleIterator iter=accumulator.iterator(); iter.hasNext();){
					iter.advance();
					sum-=iter.value();
				}
				if(left){
					if(numLeft!=1)
						numLeft=Math.min(Math.max(((int)(Math.abs(sum)/NPOsmose.epsilon)), 100), 1);
					//if(valueSum<NPOsmose.epsilon)
					//	numLeft=100;
				}else{
					if(numRight!=1)
						numRight=Math.min(Math.max(((int)(Math.abs(sum)/NPOsmose.epsilon)), 100), 1);
					//if(valueSum<NPOsmose.epsilon)
					//	numRight=100;
				}
				for(TIntDoubleIterator iter=got.iterator(); iter.hasNext();){
					iter.advance();
					add.adjustOrPutValue(iter.key(), iter.value(), iter.value());
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				numLeft=1;
				numRight=1;
				Thread.currentThread().interrupt();
				return;
			}
		}
		if(left){
			leftAcc=new TIntDoubleHashMap();
		}else{
			rightAcc=new TIntDoubleHashMap();
		}
	}


	/**
	 * Called on each value in vertex during an iteration
	 */
	@Override
	public boolean execute(int y, double v) {

		tmpSum+=v;
		
		double propagateTop=v*getEdge(y, Neighbor.Top);
		double propagateLeft=v*getEdge(y, Neighbor.Left);
		double propagateRight=v*getEdge(y, Neighbor.Right);
		double propagateBottom=v*getEdge(y, Neighbor.Bottom);
		if(propagateTop!=0)
			add.adjustOrPutValue(y-1, propagateTop, propagateTop);
		if(propagateBottom!=0)
			add.adjustOrPutValue(y+1, propagateBottom, propagateBottom);
		if(propagateLeft!=0)
			leftAcc.adjustOrPutValue(y, propagateLeft, propagateLeft);
		if(propagateRight!=0)
			rightAcc.adjustOrPutValue(y, propagateRight, propagateRight);
		add.adjustOrPutValue(y, v-propagateTop-propagateBottom-propagateLeft-propagateRight, v-propagateTop-propagateBottom-propagateLeft-propagateRight);
		return true;
	}

	/**
	 * The main method of the column worker. While not terminated or interrupted by the observer the column worker/thread iterates
	 * over its nodes and propagates the values to its neighbors.
	 */
	@Override
	synchronized public void run() {
		synchronized(NPOsmose.class){NPOsmose.threads.add(Thread.currentThread());}
		while(!Thread.currentThread().isInterrupted()){
			
			vertex.forEachEntry(this);
			vertex=add;
			add=new TIntDoubleHashMap();


			if(leftIterCounter==numLeft){
				exchange(leftExchanger, leftAcc, true);
				leftIterCounter=0;
			}
			if(rightIterCounter==numRight){
				exchange(rightExchanger, rightAcc, false);
				rightIterCounter=0;
			}
			setValueSum(tmpSum);
			tmpSum=0;
			
			if(numLeft==1 && numRight==1){
				setChanged();
				notifyObservers(columnIndex);
			}

			iterCounter++;
			leftIterCounter++;
			rightIterCounter++;
		}
	}
	
}
