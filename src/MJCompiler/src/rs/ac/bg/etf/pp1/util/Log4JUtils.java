package rs.ac.bg.etf.pp1.util;

import java.io.File;
import java.net.URL;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;

public class Log4JUtils
{
	private static Log4JUtils logs = new Log4JUtils();
	
	public static Log4JUtils instance() {
		return logs;
	}
	
	public URL findLoggerConfigFile() { return Thread.currentThread().getContextClassLoader().getResource("log4j.xml"); }
	public URL findLoggerConfigProdFile() { return Thread.currentThread().getContextClassLoader().getResource("log4j-prod.xml"); }
	
	public void prepareLogFile(Logger root)
	{
		Appender appender = root.getAppender("file");
		
		if (!(appender instanceof FileAppender)) return;
		FileAppender fAppender = (FileAppender) appender;
		
		String logFileName = fAppender.getFile();
		File logFile = new File(logFileName.substring(0, logFileName.lastIndexOf('.')) + "-test." + System.currentTimeMillis() + ".log");
		
		fAppender.setFile(logFile.getAbsolutePath());
		fAppender.activateOptions();
	}
}
