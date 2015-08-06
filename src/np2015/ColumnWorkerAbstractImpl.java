package np2015;


import impl.ColumnWorker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicBoolean;


import  gnu.trove.list.array.TDoubleArrayList;
/**
 * Extend this to implement local ColumnWorker
 * No Implementation of actual run method in this class
 * No implementation of exchangeLeft/exchangeRight - YET -
 * No implementation of addLeftAcc/addRightAcc - YET -
 * @author dennis
 *
 */
public abstract class ColumnWorkerAbstractImpl extends Observable  implements ColumnWorker, GuardedCommand{

	private TDoubleArrayList vertexValue=new TDoubleArrayList();
	private int columnIndex;
	private TDoubleArrayList leftAccValues=new TDoubleArrayList();
	private TDoubleArrayList rightAccValues=new TDoubleArrayList();
	private double valueSum=0;
	private AtomicBoolean terminate=new AtomicBoolean(false);
	private Exchanger<ArrayList<Double>> leftExchanger;
	private Exchanger<ArrayList<Double>> rightExchanger;
	/**
	 * Edges encoded by position, 4 per vertex, 0 mod 4 is left, 1 mod 4 is right, 2 mod 4 is top, 3 mod 4 is bottom.
	 */
	private TDoubleArrayList edges=new TDoubleArrayList();

	
	/**
	 * Initialize fields with initial values
	 * @param initialVertexValues - make sure keys inserted in order
	 * @param column
	 */
	public ColumnWorkerAbstractImpl(HashMap<Integer, Double> initialVertexValues, int column, GraphInfo ginfo, Observer globalChecker){
		//Initialize with Values
		for(Entry<Integer,Double> e:initialVertexValues.entrySet()){
			vertexValue.add(10*e.getKey()+e.getValue());
		}
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
	}
	
	public ArrayList<Double> exchangeLeftAccValues(){
		//TODO: Implement this!
		return null;
	}
	public ArrayList<Double> exchangeRightAccValues(){
		//TODO: Implement this!
		return null;
	}
	
	public void addLeftAcc(int y, double value){
		//TODO: Implement this!
	}
	public void addRightAcc(int y, double value){
		//TODO: Implement this!
	}

	//Getters
	
	public ArrayList<Double> getVertexValue(){
		return vertexValue;
	}
	
	public int getColumnIndex(){
		return columnIndex;
	}
	
	public ArrayList<Double> getLeftAccValues(){
		return leftAccValues;
	}
	
	public ArrayList<Double> getRightAccValues(){
		return rightAccValues;
	}
	
	public boolean shouldTerminate(){
		return terminate.get();
	}
	
	//Setters
	
	public void setLeftExchanger(Exchanger<ArrayList<Double>> left){
		leftExchanger=left;
	}
	public void setRightExchanger(Exchanger<ArrayList<Double>> right){
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

}
