package np2015;


import gnu.trove.list.array.TDoubleArrayList;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * Extend this to implement local ColumnWorker
 * No Implementation of actual run method in this class
 * No implementation of exchangeLeft/exchangeRight - YET -
 * No implementation of addLeftAcc/addRightAcc - YET -
 * @author dennis
 *
 */
public abstract class ColumnWorkerAbstractImpl extends Observable  implements ColumnWorker, GuardedCommand{

	private TDoubleArrayList vertexValues=new TDoubleArrayList();
	private final int columnIndex;
	private TDoubleArrayList leftAccValues=new TDoubleArrayList();
	private TDoubleArrayList rightAccValues=new TDoubleArrayList();
	private double valueSum=0;
	private AtomicBoolean terminate=new AtomicBoolean(false);
	private Exchanger<TDoubleArrayList> leftExchanger;
	private Exchanger<TDoubleArrayList> rightExchanger;

	/**
	 * Edges encoded by position, 4 per vertex, 0 mod 4 is left, 1 mod 4 is right, 2 mod 4 is top, 3 mod 4 is bottom.
	 */
	private TDoubleArrayList edges=new TDoubleArrayList();

	
	/**
	 * Initialize fields with initial values
	 * @param initialVertexValues - make sure keys inserted in order
	 * @param column
	 */
	public ColumnWorkerAbstractImpl(HashMap<Integer, Double> initialVertexValues, int column, GraphInfo ginfo, GlobalObserver globalChecker, Exchanger<TDoubleArrayList> el, Exchanger<TDoubleArrayList> er){
		//Initialize with Values
		this(column, ginfo, globalChecker, el, er);
		for(Entry<Integer,Double> e:initialVertexValues.entrySet()){
			vertexValues.add(10*e.getKey()+e.getValue());
		}
	}

	public ColumnWorkerAbstractImpl(TDoubleArrayList initialVertexValues, int column, GraphInfo ginfo, GlobalObserver globalChecker, Exchanger<TDoubleArrayList> el, Exchanger<TDoubleArrayList> er){
		this(column, ginfo, globalChecker, el, er);
		vertexValues=initialVertexValues;
	}
	
	public ColumnWorkerAbstractImpl(int column, GraphInfo ginfo, GlobalObserver globalChecker, Exchanger<TDoubleArrayList> el, Exchanger<TDoubleArrayList> er){
		columnIndex=column;
		//Save Rates (Edges)
		for(int i=0; i<ginfo.height*4; i++){
			Neighbor n=Neighbor.Left;
			switch(i%4){
			case 0:
				n=Neighbor.Left;
				break;
			case 1:
				n=Neighbor.Right;
				break;
			case 2:
				n=Neighbor.Top;
				break;
			case 3:
				n=Neighbor.Bottom;
				break;
			}
			edges.add(ginfo.getRateForTarget(columnIndex, i/4, n));
		}
		addObserver(globalChecker);
		globalChecker.addWorker(this);
		leftExchanger=el;
		rightExchanger=er;
	}
	

	public TDoubleArrayList exchangeLeftAccValues(){
		if(getColumnIndex() != 0 && leftExchanger==null && !leftAccValues.isEmpty()){
			leftExchanger=new Exchanger<TDoubleArrayList>();
			Runnable r=new ColumnWorkerImpl(leftAccValues, columnIndex-1, NPOsmose.ginfo, NPOsmose.o, null, leftExchanger);
			new Thread(r).start();
			return new TDoubleArrayList();
		}else if (leftExchanger!=null){
			try {
				return leftExchanger.exchange(leftAccValues);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
	public TDoubleArrayList exchangeRightAccValues(){
		if(rightExchanger==null && !rightAccValues.isEmpty()){
			rightExchanger=new Exchanger<TDoubleArrayList>();
			Runnable r=new ColumnWorkerImpl(rightAccValues, columnIndex+1, NPOsmose.ginfo, NPOsmose.o, rightExchanger, null);
			new Thread(r).start();
			return new TDoubleArrayList();
		}else if (rightExchanger!=null){
			try {
				return rightExchanger.exchange(rightAccValues);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return new TDoubleArrayList();
	}
	
	public void addLeftAcc(int y, double value, int numIter){
		addAcc(y, value, numIter, true);
	}
	public void addRightAcc(int y, double value, int numIter){
		addAcc(y, value, numIter, false);
	}
	/**
	 * TODO: Bad Implementation here
	 * @param y
	 * @param value
	 * @param numIter
	 * @param left
	 */
	private void addAcc(int y, double value, int numIter, boolean left){
		TDoubleArrayList acc;
		if(left){
			acc=leftAccValues;
		}else{
			acc=rightAccValues;
		}
		for(int i=0; i<acc.size(); i++){
			double d=acc.get(i);
			if(getEncodedRowCoordinate(d)==y){
				acc.set(i, (d*(numIter-1)+value)/numIter+y*10);
				break;
			}else if(getEncodedRowCoordinate(d)>y){
				double[] toAdd={value};
				acc.insert(i-1, toAdd);
				break;
			}
		}
		if(y>=acc.size()){
			acc.add(value);
		}
		
	}

	//Getters
	
	public TDoubleArrayList getVertexValues(){
		return vertexValues;
	}
	
	public int getColumnIndex(){
		return columnIndex;
	}
	
	public TDoubleArrayList getLeftAccValues(){
		return leftAccValues;
	}
	
	public TDoubleArrayList getRightAccValues(){
		return rightAccValues;
	}
	
	public boolean shouldTerminate(){
		return terminate.get();
	}
	
	//Setters
	public void setVertexValue(TDoubleArrayList newValues){
		vertexValues=newValues;
	}
	
	public void setLeftExchanger(Exchanger<TDoubleArrayList> left){
		leftExchanger=left;
	}
	
	public void setRightExchanger(Exchanger<TDoubleArrayList> right){
		rightExchanger=right;
	}
	/**
	 * Only to be called by this thread itself
	 */
	public void setValueSum(double sum){
		valueSum=sum;
	}
	
	@Override
	public double getRateForTarget(int x, int y, Neighbor where) {
		if(x!=columnIndex){
			throw new UnsupportedOperationException("Trying to getRate for Column, that is not the Column worked by this Thread");
		}else{
			int n=0;
			switch(where){
			case Left:
				n=0;
				break;
			case Right:
				n=1;
				break;
			case Top:
				n=2;
				break;
			case Bottom:
				n=3;
				break;
			}
			return edges.get(y*4+n);
		}
	}

	
	
	//Synchronized
	
	synchronized public double getValueSum(){
		return valueSum;
	}
	//TODO: Check this?
	public void terminate(){
		terminate=new AtomicBoolean(true);
	}

	public int getEncodedRowCoordinate(double vertex){
		return (int) Math.floor(vertex/10);
	}
	
	public double getActualValue(double vertex, int row){
		return vertex-row*10;
	}
}
