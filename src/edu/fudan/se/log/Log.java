/**
 * 
 */
package edu.fudan.se.log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.fudan.se.goalmachine.SGMMessage;

/**
 * 把程序运行中的各种日志写入到日志文件，包括Debug log和Error log等
 * 
 * @author whh
 *
 */
public class Log {

	/**
	 * 记录正常运行的debug日志
	 * 
	 * @param goalName
	 *            goal name
	 * @param methodName
	 *            记录日志时调用的方法名字
	 * @param content
	 *            日志内容
	 */
	public static void logDebug(String goalName, String methodName,
			String content) {

		String debugFile = "/debug.txt";
		try {
			String con = goalName + ", " + methodName + ", ---: " + content;
			write(debugFile, con);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 记录错误日志
	 * 
	 * @param goalName
	 *            goal name
	 * @param methodName
	 *            记录日志时调用的方法名字
	 * @param content
	 *            日志内容
	 */
	public static void logError(String goalName, String methodName,
			String content) {

		String errorFile = "/error.txt";
		try {
			String con = goalName + ", " + methodName + ", ---: " + content;
			write(errorFile, con);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 记录消息发送日志
	 * 
	 * @param msg
	 *            记录的发送的消息
	 * @param success
	 *            消息是否发送成功
	 */
	public static void logMessage(SGMMessage msg, boolean success) {
		String messageFile = "/message.txt";

		String result = "";
		if (success) {
			result = "succeed!";
		} else {
			result = "failed!";
		}

		String content = msg.getHeader() + ", [" + msg.getSender()
				+ "l send to [" + msg.getReceiver() + "], body is: ["
				+ msg.getBody() + "]. " + result;
		try {
			write(messageFile, content);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 把日志内容写入文件
	 * 
	 * @param filePath
	 *            文件路径
	 * @param goalName
	 *            goal name
	 * @param methodName
	 *            记录日志时调用的方法名字
	 * @param content
	 *            日志内容
	 * @throws IOException
	 *             IO异常
	 */
	public static void write(String filePath, String content)
			throws IOException {

		File file = new File(filePath);

		if (!file.exists()) {
			file.createNewFile();
			System.out.println("Create new log file: " + filePath);

		}

		FileWriter writer = new FileWriter(file, true);

		Date nowDate = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		String time = dateFormat.format(nowDate);

		writer.write(time + ": " + content + ".\n");
		writer.close();

	}

}
