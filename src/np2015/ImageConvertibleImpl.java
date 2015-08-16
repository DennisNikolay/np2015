package np2015;

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.HashMap;
import java.util.Map.Entry;

public class ImageConvertibleImpl implements ImageConvertible {

	private HashMap<Integer, HashMap<Integer, Double>> map=new HashMap<Integer, HashMap<Integer, Double>>();
	public ImageConvertibleImpl() {
		synchronized(NPOsmose.class){
			for(Entry<Integer, TIntDoubleHashMap> e: NPOsmose.result.entrySet()){
				HashMap<Integer,Double> tmpMap=new HashMap<Integer, Double>();
				for(TIntDoubleIterator iter=e.getValue().iterator(); iter.hasNext(); ){
					iter.advance();
					tmpMap.put(iter.key(), iter.value());
				}
				map.put(e.getKey(), tmpMap);
			}
		}
	}
	
	
	@Override
	public double getValueAt(int column, int row) {
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
