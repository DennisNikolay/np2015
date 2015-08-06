package impl;

import java.util.ArrayList;
import java.util.Observable;
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
public abstract class ColumnWorkerAbstractImpl extends Observable  implements ColumnWorker{

	private TDoubleArrayList vertexValue;
	private int columnIndex;
	private TDoubleArrayList leftAccValues=new ArrayList<Double>();
	private TDoubleArrayList rightAccValues=new ArrayList<Double>();
	private double valueSum=0;
	private AtomicBoolean terminate=new AtomicBoolean(false);
	private Exchanger<ArrayList<Double>> leftExchanger;
	private Exchanger<ArrayList<Double>> rightExchanger;
	
	/**
	 * Initialize fields with initial values
	 * @param initialVertexValues
	 * @param column
	 */
	public ColumnWorkerAbstractImpl(ArrayList<Double> initialVertexValues, int column){
		vertexValue=initialVertexValues;
		columnIndex=column;
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
	
	
	//Synchronized
	
	synchronized public double getValueSum(){
		return valueSum;
	}
	synchronized public void terminate(){
		terminate=new AtomicBoolean(true);
	}
}
