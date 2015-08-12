package np2015;

import gnu.trove.list.array.TDoubleArrayList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.Gson;

public class NPOsmose {

	public static GraphInfo ginfo;
	public static GlobalObserver o = new GlobalObserver();
	public static double epsilon;
	public static HashMap<Integer, TDoubleArrayList> result=new HashMap<Integer, TDoubleArrayList>();
	

	public static int workersActive = 0;

	public static Lock lock = new ReentrantLock();
	public static Condition condition = lock.newCondition();

	public static void main(String[] args) throws IOException,
			InterruptedException {
		Gson gson = new Gson();
		String json = "";
		// read data in
		if (args.length != 0) {
			Path path = Paths.get(args[0]);
			try {
				json = new String(Files.readAllBytes(path));
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.err
					.println("You must provide the serialized file as the first argument!");
		}
		GraphInfo ginfo = gson.fromJson(json, GraphInfo.class);
		NPOsmose.ginfo = ginfo;
		// Your implementation can now access ginfo to read out all important
		// values
		Entry<Integer, HashMap<Integer, Double>> e = ginfo.column2row2initialValue
				.entrySet().iterator().next();
		ColumnWorkerImpl worker = new ColumnWorkerImpl(e.getValue(),
				e.getKey(), ginfo, o, null, null);
		new Thread(worker).start();
		lock.lock();
		try {
			while (!o.allTerminated())
				condition.await();
		} finally {
			lock.unlock();
		}
		ImageConvertible graph = new ImageConvertibleImpl(); // <--- you should
																// implement
																// ImageConvertible
																// to write the
																// graph out
		ginfo.write2File("./result.txt", graph);
		//ginfo.write2File("/home/dennis/Schreibtisch/result.txt", graph);
	}

	public static synchronized void incrementWorkersActive() {
		workersActive++;
	}

	public static synchronized int getWorkersActive() {
		return workersActive;
	}

}
