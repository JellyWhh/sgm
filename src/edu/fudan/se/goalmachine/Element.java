/**
 * 
 */
package edu.fudan.se.goalmachine;

import java.util.concurrent.BlockingQueue;

/**
 * @author whh
 *
 */
public abstract class Element {

	protected String name; // element的名字

	private BlockingQueue<SGMMessage> msgPool; // 消息队列，即消息池
	
	private int recordedState;	//让父目标用来记录当前element的状态
	
//	private int hasDone;	//让父目标来

	public Element(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BlockingQueue<SGMMessage> getMsgPool() {
		return msgPool;
	}

	public void setMsgPool(BlockingQueue<SGMMessage> msgPool) {
		this.msgPool = msgPool;
	}

	public int getRecordedState() {
		return recordedState;
	}

	public void setRecordedState(int recordedState) {
		this.recordedState = recordedState;
	}

}
