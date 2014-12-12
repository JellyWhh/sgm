/**
 * 
 */
package edu.fudan.se.message;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

import edu.fudan.se.goalmachine.Element;
import edu.fudan.se.goalmachine.SGMMessage;

/**
 * 继承了<code>LinkedBlockingQueue<E></code>
 * ，新增了方法，从消息队列中检索出属于某一个receiver的所有消息，并把它们从队列中删除
 * 
 * @author whh
 *
 */
public class SGMMessagePool extends LinkedBlockingQueue<SGMMessage> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 从整个消息队列中拿出发给当前receiver的所有消息
	 * 
	 * @param receiver
	 *            当前receiver
	 * @return ArrayList<SGMMessage>发给当前receiver的所有消息
	 * @throws InterruptedException
	 *             异常
	 */
	public ArrayList<SGMMessage> getMessage(Element receiver)
			throws InterruptedException {

		ArrayList<SGMMessage> ret = new ArrayList<>();

		Iterator<SGMMessage> allMessages = this.iterator(); // 拿到队列中所有消息

		while (allMessages.hasNext()) {
			SGMMessage msg = allMessages.next();

			// 如果这条消息是发给当前receiver的，就把它拿出来，然后从消息队列里remove
			if (msg.getReceiver().equals(receiver.getName())) {
				ret.add(msg);
				this.remove(msg);
			}
		}

		return ret;
	}

}
