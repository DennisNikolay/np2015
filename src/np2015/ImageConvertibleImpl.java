package np2015;

import java.util.Set;

public class ImageConvertibleImpl implements ImageConvertible {

	private ColumnWorker[] workers;
	
	public ImageConvertibleImpl() {
		// TODO check that all workers have finished their work to avoid data races. Maybe lock.
		Set<ColumnWorker> w = NPOsmose.o.getWorkers();
		workers = new ColumnWorker[w.size()];
		for (ColumnWorker columnWorker : workers) {
			workers[columnWorker.getColumnIndex()] = columnWorker;
		}
	}
	
	@Override
	public double getValueAt(int column, int row) {
		ColumnWorker worker = workers[column];
		double value = worker.getVertexValue().get(row);
		return value;
	}

}
