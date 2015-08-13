package np2015;

import gnu.trove.list.array.TDoubleArrayList;

import java.util.HashMap;
import java.util.concurrent.Exchanger;

public class ColumnWorkerImpl extends ColumnWorkerAbstractImpl{

	static final int INITIAL_NUM_ITERATIONS=100;
	private int totalIterCounter=0;
	private int leftIterCounter=0;
	private int rightIterCounter=0;
	private int exchangeLeft=INITIAL_NUM_ITERATIONS;
	private int exchangeRight=INITIAL_NUM_ITERATIONS;
	
	public ColumnWorkerImpl(HashMap<Integer, Double> initialVertexValues,
			int column, GraphInfo ginfo, GlobalObserver o, Exchanger<TDoubleArrayList> el, Exchanger<TDoubleArrayList> er) {
		super(initialVertexValues, column, ginfo, o, el, er);
	}

	public ColumnWorkerImpl(TDoubleArrayList initialVertexValues,
			int column, GraphInfo ginfo, GlobalObserver o, Exchanger<TDoubleArrayList> el, Exchanger<TDoubleArrayList> er) {
		super(initialVertexValues, column, ginfo, o, el, er);
	}
	
	@Override
	public void run() {
		TDoubleArrayList left=new TDoubleArrayList();
		TDoubleArrayList right=new TDoubleArrayList();
		//Terminate when signaled or Integer.MAX_VALUE iterations done
		while(!shouldTerminate() && totalIterCounter!=Integer.MAX_VALUE){
			TDoubleArrayList vertex=this.getVertexValues();
			double propLastBottom=0;
			for(int j=0; j<vertex.size(); j++){
				//Get current Vertex
				double vertexValue=vertex.get(j);
				int y=getEncodedRowCoordinate(vertexValue);
				vertexValue=getActualValue(vertexValue, y);
				
				//If exchanged last iteration, add this to your value
				if(left.size()!=0){
					int leftY=getEncodedRowCoordinate(left.get(0)); //TODO j statt 0?
					if(leftY==y){
						vertexValue=(vertexValue+left.get(0))/2;
						left.remove(0,1);
					}
					if(leftY<y){ //if node propagated to from acc was 0 earlier
						double[] toAdd={getActualValue(left.get(0), leftY)+leftY*10}; // add new node
						vertex.add(toAdd, j-1, 1);
						left.remove(0,1);
					}
					//TODO leftY>y ?
				}
				//Same for right
				if(right.size()!=0){
					int rightY=getEncodedRowCoordinate(right.get(0));
					if(rightY==y){
						vertexValue=(vertexValue+right.get(0))/2;
						right.remove(0,1);
					}
					if(rightY<y){ //if node propagated to from acc was 0 earlier
						double[] toAdd={getActualValue(right.get(0), rightY)+rightY*10}; // add new node
						vertex.add(toAdd, j-1, 1);
						right.remove(0,1);
					}
				}
				
				//Calculate Values to be propagated
				double propagateTop=(vertexValue*getRateForTarget(getColumnIndex(),y,Neighbor.Top));
				double propagateBottom=(vertexValue*getRateForTarget(getColumnIndex(),y,Neighbor.Bottom));
				double propagateLeft=(vertexValue*getRateForTarget(getColumnIndex(),y,Neighbor.Left));		
				double propagateRight=(vertexValue*getRateForTarget(getColumnIndex(),y,Neighbor.Right));
					
				//Propagate Top
				if(j!=0 && propagateTop!=0){
					double nodeBack=vertex.get(j-1);
					int backY=getEncodedRowCoordinate(nodeBack);
					if(backY==y-1){ //Check if previous node in array list is wanted vertex
						vertex.set(j-1, getActualValue(nodeBack, backY)+propagateTop+backY*10); //if so set correct value
					}else{
						double[] toAdd={propagateTop+(y-1)*10}; //else add new node
						vertex.insert(j-1, toAdd);
					}
					
				}else if(j==0 && propagateTop!=0){ //if added node is at beginning of list
					double[] toAdd={propagateTop+(y-1)*10}; // add new node
					vertex.insert(0, toAdd);
				}
					
				//Propagate Left&Right Accumulators
				addLeftAcc(y, propagateLeft, leftIterCounter);
				leftIterCounter++;
				addRightAcc(y, propagateRight, rightIterCounter);
				rightIterCounter++;	
				
				//Calculate new own Value
				vertex.set(j, vertexValue+propLastBottom-propagateTop-propagateBottom-propagateLeft-propagateRight+y*10);
					
				//Propagate Down
				propLastBottom=propagateBottom;
				if(j==vertex.size()-1 && propagateBottom!=0){ //check if reached end of array list and have to prop bottom
					double[] toAdd={propagateBottom+(y+1)*10}; //if so add new node at the end
					vertex.add(toAdd);
				}else if(propagateBottom!=0){ //have to prop bottom?
					double nodeForward=vertex.get(j+1); 
					int forwardY=getEncodedRowCoordinate(nodeForward);
					if(forwardY!=y+1){ //is next node in list wanted vertex?
						double[] toAdd={propagateBottom+(y+1)*10}; //if not add new node
						vertex.insert(j+1,toAdd);
					}else{
						vertex.set(j+1, getActualValue(nodeForward, forwardY)+propagateBottom+forwardY*10); //if so set correct value
					}
				}
					
			}
			//Left Accumulator work
			boolean addedLeft=false; //needed for the right at the end adding
			if(left.size()>0){ //All exchanged Acc values should done by now, if not, then they are at the end of vertex list and 0 till now
				for(double d: left.toArray()){
					vertex.add(d);
				}
				left.remove(0,1);
				addedLeft=true;
			}
			if(leftIterCounter==exchangeLeft){ //should exchange?
				TDoubleArrayList inflow=exchangeLeftAccValues();
				double result=0;
				for(double d: inflow.toArray()){
					result+=d;
				}
				for(double d: getLeftAccValues().toArray()){
					result-=d;
				}
				exchangeLeft=calculateIterations(result);
				left=inflow;
				leftIterCounter=0;
			}
			
			//Right Accumulator work
			//if addedLeft in this turn, better add right next turn
			if(right.size()>0 && !addedLeft){ //All exchanged Acc values should done by now, if not, then they are at the end of vertex list and 0 till now
				for(double d: left.toArray()){
					vertex.add(d);
				}
				right.remove(0,1);
			}
			if(rightIterCounter==exchangeRight){ //same for right
				TDoubleArrayList inflow=exchangeRightAccValues();
				double result=0;
				for(double d: inflow.toArray()){
					result+=d;
				}
				for(double d: getLeftAccValues().toArray()){
					result-=d;
				}
				exchangeRight=calculateIterations(result);
				right=inflow;
				rightIterCounter=0;
			}
			//Finished this iteration
			totalIterCounter++;
		}
		NPOsmose.result.put(getColumnIndex(), getVertexValues());
		
			
	}
	
	
	private int calculateIterations(double inOut){
		inOut=Math.abs(inOut);
		if(inOut<=NPOsmose.epsilon){
			setChanged();
			notifyObservers();
			return 1;
		}
		return (int) Math.min(Math.floor(inOut/NPOsmose.epsilon), 100);
	}
	

}
