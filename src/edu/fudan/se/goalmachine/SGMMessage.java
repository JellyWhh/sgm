/**
 * 
 */
package edu.fudan.se.goalmachine;

import java.io.Serializable;


/**
 * 实现线程间的通信的消息格式，是可序列化对象
 * 
 * @author whh
 *
 */
public class SGMMessage implements Serializable {

	private static final long serialVersionUID = 1L;

	private String header; // 消息头部
	private String sender; // 消息发送者
	private String receiver; // 消息接收者，只有一个，如果一个Goal要发消息给多个接收者，就发多条信息，每条信息只有一个接收者
	private String body; // 消息主体

	public SGMMessage(String header, String sender, String receiver, String body) {
		this.header = header;
		this.sender = sender;
		this.receiver = receiver;
		this.body = body;
	}

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getReceiver() {
		return receiver;
	}

	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

}
