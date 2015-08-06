package impl;

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
	public ArrayList<Double> exchangeLeftAccValues();
	public ArrayList<Double> exchangeRightAccValues();
	public void addLeftAcc(int y, double value);
	public void addRightAcc(int y, double value);

		//Standard Getters
	public ArrayList<Double> getVertexValue();
	public int getColumnIndex();
	public ArrayList<Double> getLeftAccValues();
	public ArrayList<Double> getRightAccValues();
	public boolean shouldTerminate();
		//Standard Setters - all other fields are only set in constructor
	public void setLeftExchanger(Exchanger<ArrayList<Double>> left);
	public void setRightExchanger(Exchanger<ArrayList<Double>> right);
	public void setValueSum(double sum);
	
}
