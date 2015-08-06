package np2015;

import java.util.HashMap;
import java.util.Observer;

public class ColumnWorkerImpl extends ColumnWorkerAbstractImpl{

	public ColumnWorkerImpl(HashMap<Integer, Double> initialVertexValues,
			int column, GraphInfo ginfo, Observer o) {
		super(initialVertexValues, column, ginfo, o);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}


}
