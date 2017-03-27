package onlinejudge.judge.process;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MyJudge {
	Log logger = LogFactory.getLog(getClass());
	public static String NEW_LINE = "\n";
	boolean isError = false;
	String errorMessage = "";
	boolean isRunSuccess = false;
	boolean isTimeOut = false;
	boolean isCorrect = false;
	boolean isIncorrect = false;
	long timeExecuted = 0;
	StringBuilder outputFromProcess;

	String pathFileJava;
	String pathFileData;
	String fileClassRun;
	String inputDataFile;
	String outputDataFile;
	byte testCaseNo = 0;
	int maxTime;
	
	List<Callback> listCallback;
	public MyJudge(String pathFileJava, String pathFileData, String fileClassRun, String inputDataFile,
			String outputDataFile, int maxTime) {
		super();
		this.pathFileJava = pathFileJava;
		this.pathFileData = pathFileData;
		this.fileClassRun = fileClassRun;
		this.inputDataFile = inputDataFile;
		this.outputDataFile = outputDataFile;
		this.maxTime = maxTime;
		this.listCallback = new ArrayList<>();
	}
	public MyJudge(String pathFileJava, String pathFileData, String fileClassRun, String inputDataFile,
			String outputDataFile, int maxTime, Callback callback) {
		this(pathFileJava, pathFileData, fileClassRun, inputDataFile, outputDataFile, maxTime);
		addCallback(callback);
	}
	public void addCallback(Callback callback){
		listCallback.add(callback);
	}
	public void run() throws IOException, InterruptedException {
		logger.debug("MyJudge is running....");
		List<String> commands = new ArrayList<String>();
		commands.add("java");
		commands.add(fileClassRun);

		ProcessBuilder pb = new ProcessBuilder(commands);
		pb.directory(new File(pathFileJava + File.separator));
		pb.redirectInput(Redirect.from(new File(pathFileData + File.separator + inputDataFile)));
		final long st = System.currentTimeMillis();
		final Process process = pb.start();
		//
		final InputStream inputStreamFromProcess = process.getInputStream();
		final InputStream errorStreamFromProcess = process.getErrorStream();
		final ExecutorService pool = Executors.newFixedThreadPool(4);

		pool.execute(new Runnable() { // #1 read input
			@Override
			public void run() {
				String s = "";
				BufferedReader br = new BufferedReader(new InputStreamReader(inputStreamFromProcess));
				outputFromProcess = new StringBuilder();
				try {
					boolean first = true;
					while (!isError && !isRunSuccess && (s = br.readLine()) != null) {
						// System.out.println(s);
						if (!first) {
							outputFromProcess.append(NEW_LINE);
						} else {
							first = !first;
						}
						outputFromProcess.append(s);
					}
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

		pool.execute(new Runnable() { // #2 check exception
			@Override
			public void run() {
				String s = "";
				BufferedReader br = new BufferedReader(new InputStreamReader(errorStreamFromProcess));
				try {
					while (!isError && !isRunSuccess && (s = br.readLine()) != null) {
						System.err.println(s);
						errorMessage = s;
						isError = true;
					}
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
		pool.execute(new Runnable() {// #3 check timeout
			private int time = 0;
			private int sleepTime = 1;

			public void run() {
				try {
					while (!isError && !isRunSuccess && time < maxTime) {
						Thread.sleep(sleepTime);
						time += sleepTime;
					}
					if (!isError && !isRunSuccess) {
						isTimeOut = true;
						process.destroy();
					} 
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			};

		});

		final int exitValue = process.waitFor();
		long en = System.currentTimeMillis();
		timeExecuted = en - st;
		logger.debug("MyJudge run code is complete with exit value is: " + exitValue);
		isRunSuccess = !isTimeOut && !isError;

		pool.execute(new Runnable() { // #4 compare output process with output
										// data
			public void run() {
				if (isError || isTimeOut) {
					completeProcess(exitValue);
					return;
				}
				BufferedReader brOutputData = null;
				BufferedReader brOutputProcess = null;
				try {
					brOutputData = new BufferedReader(
							new InputStreamReader(new FileInputStream(pathFileData + File.separator + outputDataFile)));
					FileUtils.writeStringToFile(new File(pathFileJava + File.separator +"anwsert.txt"),outputFromProcess.toString(), true);
					brOutputProcess = new BufferedReader(
							new InputStreamReader(new ByteArrayInputStream(outputFromProcess.toString().getBytes())));
					String outputData = "";
					String outputProcess = "";

					while ((outputData = brOutputData.readLine()) != null
							& (outputProcess = brOutputProcess.readLine()) != null) {
						if (!outputData.equals(outputProcess)) {
							isIncorrect = true;
							break;
						}
					}
					if (!isIncorrect && outputData == null && outputProcess == null) {
						isCorrect = true;
					} else {
						isIncorrect = true;
					}
					completeProcess(exitValue);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (brOutputData != null)
						try {
							brOutputData.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					if (brOutputProcess != null)
						try {
							brOutputProcess.close();
						} catch (IOException e) {
							e.printStackTrace();
						}

				}
			}
		});
		pool.shutdown();
	}

	public void completeProcess(int exitValue) {
//		System.out.println("timeExecuted " + timeExecuted);
//		System.out.println("exitValue " + exitValue);
//		System.out.println("complete: error=" + isError + ", isRunSuccess=" + isRunSuccess + ", isTimeOut=" + isTimeOut
//				+ ", isCorrect=" + isCorrect + ", isIncorrect=" + isIncorrect);
		for (Callback callback : listCallback) {
			callback.complete();
		}
	}

	public String getInformation() {
		String pattern = "error=%s, isRunSuccess=%s, isTimeOut=%s, isCorrect=%s, isIncorrect=%s";
		return String.format(pattern, isError, isRunSuccess, isTimeOut, isCorrect, isIncorrect);
	}
	
	public boolean isError() {
		return isError;
	}
	public void setError(boolean isError) {
		this.isError = isError;
	}
	public boolean isTimeOut() {
		return isTimeOut;
	}
	public void setTimeOut(boolean isTimeOut) {
		this.isTimeOut = isTimeOut;
	}
	public boolean isCorrect() {
		return isCorrect;
	}
	public void setCorrect(boolean isCorrect) {
		this.isCorrect = isCorrect;
	}
	public boolean isIncorrect() {
		return isIncorrect;
	}
	public void setIncorrect(boolean isIncorrect) {
		this.isIncorrect = isIncorrect;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public long getTimeExecuted() {
		return timeExecuted;
	}
	public void setTimeExecuted(long timeExecuted) {
		this.timeExecuted = timeExecuted;
	}
	
//	public static void main(String[] args) throws IOException, InterruptedException {
//		String pathFileJava = "D:" + File.separator + "hle56" + File.separator + "test" + File.separator + "12130010";
//		String pathRootDirectory = "D:" + File.separator + "hle56" + File.separator + "test" + File.separator + "P001";
//		String fileClassRun = "MyRunableClass";
//		String inputDataFile = "input.txt";
//		String outputDataFile = "output.txt";
//		int maxTime = 3000;
//		final MyJudge myJudge = new MyJudge(pathFileJava, pathRootDirectory, fileClassRun, inputDataFile, outputDataFile, maxTime);
//		myJudge.addCallback(new Callback() {
//			
//			@Override
//			public void complete() {
//				System.out.println(myJudge.getInformation());
//				
//			}
//		});
//		myJudge.run();
//	}
}
