package np2015;

import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.concurrent.Exchanger;

/**
 * If allowed use gnu.trove.list.array.TDoubleArrayList.java
 * instead of ArrayList<Double>
 * @author dennis
 *
 */
public interface ColumnWorker extends Runnable {

	
	@Override
	public void run();
	
	//Synchronized
	public double getValueSum();
	public void terminate();
	
	//Not Synchronized
	public TDoubleArrayList exchangeLeftAccValues();
	public TDoubleArrayList exchangeRightAccValues();
	public void addLeftAcc(int y, double value, int numIter);
	public void addRightAcc(int y, double value, int numIter);

		//Standard Getters
	public TDoubleArrayList getVertexValues();
	public int getColumnIndex();
	public TDoubleArrayList getLeftAccValues();
	public TDoubleArrayList getRightAccValues();
	public boolean shouldTerminate();
		//Standard Setters - all other fields are only set in constructor
	public void setLeftExchanger(Exchanger<TDoubleArrayList> left);
	public void setRightExchanger(Exchanger<TDoubleArrayList> right);
	public void setValueSum(double sum);

}
