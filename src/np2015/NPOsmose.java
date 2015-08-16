package np2015;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.google.gson.Gson;

public class NPOsmose {

	public static GraphInfo ginfo;
	public static final GlobalObserver o = new GlobalObserver();
	public static double epsilon = 0.1;
	public static final HashMap<Integer, TIntDoubleHashMap> result=new HashMap<Integer, TIntDoubleHashMap>();	

	public static int workersActive = 0;

	public static final  Lock lock = new ReentrantLock();
	public static final Condition condition = lock.newCondition();
	public static final LinkedList<Thread> threads=new LinkedList<Thread>();

	public static void main(String[] args) throws IOException,
			InterruptedException {
		long startTime = System.currentTimeMillis();		
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
		NPOsmose.epsilon = ginfo.epsilon;
		// Your implementation can now access ginfo to read out all important values
		Entry<Integer, HashMap<Integer, Double>> e = ginfo.column2row2initialValue
				.entrySet().iterator().next();
		SimpleColumnWorker worker = new SimpleColumnWorker(e.getValue(),
				e.getKey(), null, null);
		new Thread(worker).start();
		lock.lock();
		NPOsmose.o.start();
		try {
			while (!o.allTerminated())
				condition.await();
		} finally {
			lock.unlock();
		}
		for(Thread t: threads){
			t.join();
		}
		ImageConvertible graph = new ImageConvertibleImpl();
		
		ginfo.write2File("./result.txt", graph);
		long stopTime=System.currentTimeMillis();
		long millis=stopTime-startTime;
		String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
	            TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
	            TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
		System.out.println("Written Result in "+hms);
		//ginfo.write2File("/home/dennis/Schreibtisch/result.txt", graph);
	}

	public static synchronized void incrementWorkersActive() {
		workersActive++;
	}

	public static synchronized int getWorkersActive() {
		return workersActive;
	}

}
