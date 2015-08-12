package np2015;

import gnu.trove.list.array.TDoubleArrayList;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

public class ImageConvertibleImpl implements ImageConvertible {

	private HashMap<Integer, HashMap<Integer, Double>> map=new HashMap<Integer, HashMap<Integer, Double>>();
	public ImageConvertibleImpl() {
		// TODO check that all workers have finished their work to avoid data races. Maybe lock.
		/*Set<ColumnWorker> w = NPOsmose.o.getWorkers();
		workers = new ColumnWorker[w.size()];
		for (ColumnWorker columnWorker : workers) {
			workers[columnWorker.getColumnIndex()] = columnWorker;
		}*/
		for(Entry<Integer, TDoubleArrayList> e:NPOsmose.result.entrySet()){
			HashMap<Integer,Double> tmpMap=new HashMap<Integer, Double>();
			for(double d: e.getValue().toArray()){
				int y=(int) Math.floor(d/10);
				d=d-y*10;
				tmpMap.put(y,d);
			}
			map.put(e.getKey(), tmpMap);
		}
	}
	
	@Override
	public double getValueAt(int column, int row) {
		/*ColumnWorker worker = workers[column];
		double value = worker.getVertexValue().get(row);
		return value;*/
		HashMap<Integer, Double> m=map.get(column);
		if(m==null){
			return 0;
		}else{
			Double val=m.get(row);
			if(val==null){
				return 0;
			}else{
				return val;
			}
		}
	}

}
