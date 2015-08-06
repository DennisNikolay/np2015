package np2015;

import impl.GlobalObserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.Gson;

public class NPOsmose {

	public static GraphInfo ginfo;
	public static GlobalObserver o;
	public static double epsilon;
	
	public static int workersActive = 0;
	
	public static void main(String[] args) throws IOException, InterruptedException {
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
			System.err.println("You must provide the serialized file as the first argument!");
		}
		GraphInfo ginfo = gson.fromJson(json, GraphInfo.class);
		NPOsmose.ginfo=ginfo;
		// Your implementation can now access ginfo to read out all important values
		ImageConvertible graph = null; // <--- you should implement ImageConvertible to write the graph out
		ginfo.write2File("./result.txt", graph);
	}

	public static synchronized void incrementWorkersActive() {
		workersActive++;
	}
	
	public static synchronized int getWorkersActive() {
		return workersActive;
	}
	
}
