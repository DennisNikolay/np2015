package np2015;

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.concurrent.BrokenBarrierException;
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

	private GraphInfo info;
	
	private int totalIterCounter=0;
	private int leftIterCounter=0;
	private int rightIterCounter=0;
	
	private int numLeft=100;
	private int numRight=100;
	
	private AtomicBoolean terminate=new AtomicBoolean(false);
	private double oldValueSum;

	
	public SimpleColumnWorker(int column, GraphInfo ginfo, GlobalObserver o,
			Exchanger<TIntDoubleHashMap> el, Exchanger<TIntDoubleHashMap> er) {
		columnIndex=column;
		info=ginfo;
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
		}
		addObserver(o);
 		o.addWorker(this);
		exchangeLeft = el;
		exchangeRight = er;
		
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
 		synchronized(NPOsmose.class){NPOsmose.o.addThread(Thread.currentThread());}
		TIntDoubleHashMap gotLeft=null;
		TIntDoubleHashMap gotRight=null;
		while (!shouldTerminate() && !Thread.currentThread().isInterrupted() && totalIterCounter != Integer.MAX_VALUE) {
			/*if (totalIterCounter % 10000000 == 0) {
				synchronized(NPOsmose.class){NPOsmose.result.put(getColumnIndex(), getVertexValues());}
				ImageConvertible graph = new ImageConvertibleImpl();
				NPOsmose.ginfo.write2File("./test.txt", graph);
				System.out.println("new pic");
			}*/
			double sum=0;
			TIntDoubleHashMap tmpMap=new TIntDoubleHashMap();
			for(TIntDoubleIterator iter=vertex.iterator(); iter.hasNext(); ){
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
				
				double newValue = iter.value() - propagateTop - propagateBottom - propagateLeft - propagateRight;
				tmpMap.adjustOrPutValue(iter.key(), newValue, newValue);
				
				if(columnIndex>0){
					addLeftAcc(iter.key(), propagateLeft, totalIterCounter);
				}
				if(columnIndex!=info.width-1){
					addRightAcc(iter.key(), propagateRight, totalIterCounter);
				}
				if(propagateTop!=0 && iter.key()!=0){
					tmpMap.adjustOrPutValue(iter.key()-1, propagateTop, propagateTop);
				}
				if(propagateBottom!=0 && iter.key()!=info.height-1){
					tmpMap.adjustOrPutValue(iter.key()+1, propagateBottom, propagateBottom);
				}
			}
			vertex=tmpMap;
			rightIterCounter++;
			leftIterCounter++;
			if(leftIterCounter==numLeft && !shouldTerminate() && !Thread.currentThread().isInterrupted()){
				gotLeft=exchangeAccLeft();
				leftIterCounter=0;
				leftAcc=new TIntDoubleHashMap();
			}
			if(rightIterCounter==numRight && !shouldTerminate() && !Thread.currentThread().isInterrupted()){
				gotRight=exchangeAccRight();
				rightIterCounter=0;
				rightAcc=new TIntDoubleHashMap();
			}
			totalIterCounter++;
			//System.out.println(NPOsmose.total);
			setValueSum(sum);
			
		}
		synchronized(NPOsmose.class){NPOsmose.result.put(getColumnIndex(), getVertexValues());}


	}
	
	private TIntDoubleHashMap exchangeAcc(Exchanger exchanger, TIntDoubleHashMap accumulators, int iterations, int neighborColumnIndex, boolean left) {
		Exchanger<TIntDoubleHashMap> ex;
		TIntDoubleHashMap acc;
		int num;
		
		ex=exchangeLeft;
		acc=leftAcc;
		num=numLeft;
		
		if(ex==null){
			if(acc!=null && !acc.isEmpty()){
				for(TIntDoubleIterator iter=acc.iterator(); iter.hasNext();){
					iter.advance();
					iter.setValue(iter.value()/num);
				}
				ex=new Exchanger<TIntDoubleHashMap>();
				Runnable r;
				
				exchanger=ex;
				TIntDoubleHashMap accCopy=new TIntDoubleHashMap();
				accCopy.putAll(acc);
				acc=new TIntDoubleHashMap();
				r = new SimpleColumnWorker(accCopy, neighborColumnIndex,
						NPOsmose.ginfo, NPOsmose.o, null, ex);
				
				new Thread(r).start();
				return new TIntDoubleHashMap();
				
			}else{
				if(left){
					numLeft=1;
				}else{
					numRight=1;
				}
			}
		} else {
			try {
				for(TIntDoubleIterator iter=acc.iterator(); iter.hasNext();){
					iter.advance();
					iter.setValue(iter.value()/num);
				}
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
				// TODO Auto-generated catch block
				//e.printStackTrace();
				return new TIntDoubleHashMap();
			}
		}

	return null;
		
	}
	
	private TIntDoubleHashMap exchangeAccLeft() {
		return exchangeAcc(exchangeLeft, leftAcc, numLeft, columnIndex-1, true);
	}
	
	private TIntDoubleHashMap exchangeAccRight() {
		return exchangeAcc(exchangeRight, rightAcc, numRight, columnIndex+1, false);
	}
	
	
/*	
	private TIntDoubleHashMap exchangeAcc(boolean left){

		Exchanger<TIntDoubleHashMap> ex;
		TIntDoubleHashMap acc;
		int num;
		if(left){
			ex=exchangeLeft;
			acc=leftAcc;
			num=numLeft;
		}else{
			ex=exchangeRight;
			acc=rightAcc;
			num=numRight;
		}
		if(ex==null){
			if(acc!=null && !acc.isEmpty()){
				for(TIntDoubleIterator iter=acc.iterator(); iter.hasNext();){
					iter.advance();
					iter.setValue(iter.value()/num);
				}
				ex=new Exchanger<TIntDoubleHashMap>();
				Runnable r;
				if(left){
					this.exchangeLeft=ex;
					TIntDoubleHashMap accCopy=new TIntDoubleHashMap();
					accCopy.putAll(acc);
					acc=new TIntDoubleHashMap();
					r = new SimpleColumnWorker(accCopy, columnIndex-1,
							NPOsmose.ginfo, NPOsmose.o, null, ex);
				}else{
					this.exchangeRight=ex;
					TIntDoubleHashMap accCopy=new TIntDoubleHashMap();
					accCopy.putAll(acc);
					acc=new TIntDoubleHashMap();
					r = new SimpleColumnWorker(accCopy, columnIndex+1,
							NPOsmose.ginfo, NPOsmose.o, ex, null);
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
		} else {
				try {
					for(TIntDoubleIterator iter=acc.iterator(); iter.hasNext();){
						iter.advance();
						iter.setValue(iter.value()/num);
					}
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
					// TODO Auto-generated catch block
					//e.printStackTrace();
					return new TIntDoubleHashMap();
				}
		}

		return null;
	}
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
				//if(columnIndex==1 || columnIndex==2)
				//	System.out.println("Thread "+columnIndex+": numLeft="+numLeft);
				if(numLeft==1 && numRight==1){
					if(shouldTerminate() || Thread.interrupted()){
						return;
					}
					setChanged();
					notifyObservers();
				}
				return;
			}
			numLeft=(int) Math.min(Math.floor(result / NPOsmose.epsilon), 100);
			//if(columnIndex==1 || columnIndex==2)
			//	System.out.println("Thread "+columnIndex+": numLeft="+numLeft);
		}else{
			result = Math.abs(result);
			if (result <= NPOsmose.epsilon) {
				numRight=1;
				//if(columnIndex==1 || columnIndex==2)
					//System.out.println("Thread "+columnIndex+": numRight="+numRight);
				if(numLeft==1 && numRight==1){
					if(shouldTerminate() || Thread.interrupted()){
						return;
					}
					setChanged();
					notifyObservers();
				}
				return;
			}
			numRight=(int) Math.min(Math.floor(result / NPOsmose.epsilon), 100);
			//if(columnIndex==1 || columnIndex==2)
			//	System.out.println("Thread "+columnIndex+": numRight="+numRight);
		}
	}
	
	synchronized public double getValueSum() {
		double result=0;
		for(TIntDoubleIterator i=vertex.iterator(); i.hasNext(); ){
			i.advance();
			result+=i.value();
		}
		return result;
		//return valueSum;
	}

	public void terminate() {
		terminate.set(true);
	}

	/*
	public TIntDoubleHashMap exchangeLeftAccValues() {
		return exchangeAccLeft(true);
	}

	
	public TIntDoubleHashMap exchangeRightAccValues() {
		return exchangeAcc(false);
	}
*/
	private void addAcc(int y, double value, int numIter, boolean left){
		TIntDoubleHashMap acc;
		if(left){
			acc=leftAcc;
		}else{
			acc=rightAcc;
		}
		if(acc.containsKey(y)){
			//TODO:
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
		return terminate.get();
	}

	
	public void setLeftExchanger(Exchanger<TIntDoubleHashMap> left) {
		this.exchangeLeft=left;

	}

	
	public void setRightExchanger(Exchanger<TIntDoubleHashMap> right) {
		this.exchangeRight=right;
	}

	synchronized public double getOldValueSum(){
		return oldValueSum;
	}
	
	synchronized public void setValueSum(double sum) {
		this.oldValueSum=this.getValueSum();
		this.valueSum=sum;
	}



}
