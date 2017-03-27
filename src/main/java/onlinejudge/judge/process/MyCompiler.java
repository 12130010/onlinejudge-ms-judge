package onlinejudge.judge.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MyCompiler {
	String rootPath;
	String javaFileName;
	boolean isCompileSuccess = true;
	List<Callback> listCallback;

	Log logger = LogFactory.getLog(getClass());
	
	public MyCompiler(String rootPath, String javaFileName) {
		super();
		this.rootPath = rootPath;
		this.javaFileName = javaFileName;
		this.listCallback = new ArrayList<>();
	}
	public MyCompiler(String rootPath, String javaFileName,Callback callback) {
		this(rootPath, javaFileName);
		addCallback(callback);
	}

	public void addCallback(Callback callback) {
		listCallback.add(callback);
	}

	public void completeProcess() {
		for (Callback callback : listCallback) {
			callback.complete();
		}
	}

	public void run() throws IOException {
		logger.debug("MyCompiler is running...");
		List<String> commands = new ArrayList<String>();
		commands.add("javac");
		commands.add(javaFileName);
		ProcessBuilder pb = new ProcessBuilder(commands);
		pb.directory(new File(rootPath + File.separator));

		final Process process = pb.start();
		//
		final InputStream errorStreamFromProcess = process.getErrorStream();
		final ExecutorService pool = Executors.newFixedThreadPool(4);

		pool.execute(new Runnable() { // #2 check exception
			@Override
			public void run() {
				String s = "";
				BufferedReader br = new BufferedReader(new InputStreamReader(errorStreamFromProcess));
				try {
					while ((s = br.readLine()) != null) {
						isCompileSuccess = false;
					}
					completeProcess();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
		pool.shutdown();
	}

	public boolean isCompileSuccess() {
		return isCompileSuccess;
	}

	public void setCompileSuccess(boolean isCompileSuccess) {
		this.isCompileSuccess = isCompileSuccess;
	}

//	public static void main(String[] args) throws IOException {
//		String rootPath = "D:" + File.separator + "hle56" + File.separator + "test" + File.separator + "12130010";
//		String javaFileName = "MyRunableClass.java";
//		MyCompiler myCompiler = new MyCompiler(rootPath, javaFileName);
//		myCompiler.run();
//	}
}