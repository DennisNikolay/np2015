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

	private TIntDoubleHashMap vertex=new TIntDoubleHashMap();
	private TIntDoubleHashMap leftAcc=new TIntDoubleHashMap();
	private TIntDoubleHashMap rightAcc=new TIntDoubleHashMap();
	private Exchanger<TIntDoubleHashMap> exchangeLeft;
	private Exchanger<TIntDoubleHashMap> exchangeRight;
	private double valueSum;
	private int columnIndex;
	private TDoubleArrayList edges = new TDoubleArrayList();

	private int totalIterCounter=0;
	private int leftIterCounter=0;
	private int rightIterCounter=0;
	
	private int numLeft=100;
	private int numRight=100;
	
	private AtomicBoolean terminate=new AtomicBoolean(true);
	
	public SimpleColumnWorker(int column, GraphInfo ginfo, GlobalObserver o,
			Exchanger<TIntDoubleHashMap> el, Exchanger<TIntDoubleHashMap> er) {
		columnIndex=column;
		for (int i = 0; i < ginfo.height * 4; i++) {
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
			edges.add(ginfo.getRateForTarget(columnIndex, i / 4, n));
			addObserver(o);
     		o.addWorker(this);
			exchangeLeft = el;
			exchangeRight = er;
		}
	}

	
	public SimpleColumnWorker(HashMap<Integer, Double> initialVertexValues,
			int column, GraphInfo ginfo, GlobalObserver o,
			Exchanger<TIntDoubleHashMap> el, Exchanger<TIntDoubleHashMap> er) {		
		
		this(column, ginfo, o, el, er);
		for (Entry<Integer, Double> e : initialVertexValues.entrySet()) {
			vertex.put(e.getKey(), e.getValue());
		}
	}

	public SimpleColumnWorker(TIntDoubleHashMap initialVertexValues, int column,
			GraphInfo ginfo, GlobalObserver o, Exchanger<TIntDoubleHashMap> el,
			Exchanger<TIntDoubleHashMap> er) {
		
		this(column, ginfo, o, el, er);
		vertex=initialVertexValues;
		
	}
	
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

	public void run() {
		int last=0;
		while (!shouldTerminate() && totalIterCounter != Integer.MAX_VALUE) {
			double propLastBottom = 0;
			TIntDoubleHashMap gotLeft=null;
			TIntDoubleHashMap gotRight=null;
			double sum=0;
			for(TIntDoubleIterator iter=vertex.iterator(); iter.hasNext();){
				iter.advance();
				sum+=iter.value();
				if(gotLeft!=null && !gotLeft.isEmpty()){
					if(gotLeft.containsKey(iter.key())){
						iter.setValue((iter.value()+gotLeft.get(iter.key())));
						gotLeft.remove(iter.key());
					}
				}
				if(gotRight!=null && !gotRight.isEmpty()){
					if(gotRight.containsKey(iter.key())){
						iter.setValue((iter.value()+gotRight.get(iter.key())));
						gotRight.remove(iter.key());
					}
				}
				
				double propagateTop=iter.value()*getEdge(iter.key(), Neighbor.Top);
				double propagateBottom=iter.value()*getEdge(iter.key(), Neighbor.Bottom);
				double propagateLeft=iter.value()*getEdge(iter.key(), Neighbor.Left);
				double propagateRight=iter.value()*getEdge(iter.key(), Neighbor.Right);
				iter.setValue(iter.value()+propLastBottom-propagateTop-propagateBottom-propagateLeft-propagateRight);
				
				propLastBottom=propagateBottom;
				addLeftAcc(iter.key(), propagateLeft, totalIterCounter);
				addRightAcc(iter.key(), propagateRight, totalIterCounter);
				//vertex.put(iter.key()-1, propagateTop);
				
				if(leftIterCounter==numLeft){
					gotLeft=exchangeLeftAccValues();
					leftIterCounter=-1;
				}
				if(rightIterCounter==numRight){
					gotRight=exchangeRightAccValues();
					rightIterCounter=-1;
				}
				last=iter.key();
			}
			
			if(propLastBottom!=0){
				if(vertex.containsKey(last+1)){
					vertex.put(last+1, vertex.get(last+1)+propLastBottom);
				}else{
					vertex.put(last+1, propLastBottom);
				}
			}
			rightIterCounter++;
			leftIterCounter++;
			totalIterCounter++;
			setValueSum(sum);
			
		}
		NPOsmose.result.put(getColumnIndex(), getVertexValues());


	}
	

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
					r = new SimpleColumnWorker(acc, columnIndex-1,
							NPOsmose.ginfo, NPOsmose.o, null, ex);
				}else{
					this.exchangeRight=ex;
					r = new SimpleColumnWorker(acc, columnIndex+1,
							NPOsmose.ginfo, NPOsmose.o, ex, null);
				}
				new Thread(r).start();
				return new TIntDoubleHashMap();
			}
		}else{
				try {
					TIntDoubleHashMap got=ex.exchange(acc);
					calculateIter(acc, got, left);
					return got;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}

		return null;
		
		
	}
	
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
					setChanged();
					notifyObservers();
				}
				return;
			}
			numLeft=(int) Math.min(Math.floor(result / NPOsmose.epsilon), 100);
		}else{
			result = Math.abs(result);
			if (result <= NPOsmose.epsilon) {
				numRight=1;
				if(numLeft==1 && numRight==1){
					setChanged();
					notifyObservers();
				}
				return;
			}
			numRight=(int) Math.min(Math.floor(result / NPOsmose.epsilon), 100);
		}
	}
	
	public double getValueSum() {
		/*double result=0;
		for(TIntDoubleIterator i=vertex.iterator(); i.hasNext(); i.advance()){
			result+=i.value();
		}
		return result;*/
		return valueSum;
	}

	public void terminate() {
		terminate.set(true);
	}

	public TIntDoubleHashMap exchangeLeftAccValues() {
		return exchangeAcc(true);
	}

	
	public TIntDoubleHashMap exchangeRightAccValues() {
		return exchangeAcc(false);
	}

	private void addAcc(int y, double value, int numIter, boolean left){
		TIntDoubleHashMap acc;
		if(left){
			acc=leftAcc;
		}else{
			acc=rightAcc;
		}
		if(acc.containsKey(y)){
			//TODO:
			acc.put(y, (acc.get(y)+value)/2);
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
		return vertex;
	}

	
	public int getColumnIndex() {
		return columnIndex;
	}

	
	public TIntDoubleHashMap getLeftAccValues() {
		return leftAcc;
	}

	
	public TIntDoubleHashMap getRightAccValues() {
		return rightAcc;
	}

	
	public boolean shouldTerminate() {
		return !terminate.get();
	}

	
	public void setLeftExchanger(Exchanger<TIntDoubleHashMap> left) {
		this.exchangeLeft=left;

	}

	
	public void setRightExchanger(Exchanger<TIntDoubleHashMap> right) {
		this.exchangeRight=right;
	}

	
	public void setValueSum(double sum) {
		this.valueSum=sum;
	}



}