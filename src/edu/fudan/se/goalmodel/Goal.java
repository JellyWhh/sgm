/**
 * 
 */
package edu.fudan.se.goalmodel;

import java.util.ArrayList;

import edu.fudan.se.message.SGMMessage;
import edu.fudan.se.message.SGMMessagePool;

/**
 * 这个是“目标状态机”<br>
 * 它与<code>Task</code>的区别是，<code>Task</code>不能够再有subElement
 * 
 * @author whh
 *
 */
public abstract class Goal extends Element implements Runnable {

	private int state; // 取值0(initial),1(activated),2(executing),3(achieved),4(failed),5(repairing),6(finished)
	private Goal parentGoal;
	private ArrayList<Element> subElements = new ArrayList<>();

	private SGMMessagePool msgPool; // 消息队列，即消息池

	private boolean isFinished;
	private boolean isBehaviourDone = false; // 标识一个行为是否结束

	/**
	 * 构造方法
	 * 
	 * @param name
	 *            目标的名字
	 * @param msgQueue
	 *            目标相关联的消息队列，所有目标共享一个消息队列，即消息池
	 */
	public Goal(String name, SGMMessagePool msgPool) {
		super(name);
		this.msgPool = msgPool;
	}

	@Override
	public void run() {
		System.out.println("---DEBUG--- GoalMachine: " + this.name
				+ " started!");

		state = 0; // 刚开始是目标状态是0
		if (!isBehaviourDone()) {
			doInitialBehaviour();
		}

		isFinished = false;
		int index = 0;
		while (!isFinished) {
			try {
				ArrayList<SGMMessage> msgs = msgPool.getMessage(this);
				if (msgs != null) {
					for (SGMMessage msg : msgs) {
						System.out.println(msg.getReceiver()
								+ " get a message, from " + msg.getSender()
								+ "; body is: " + msg.getBody());

						// 收到消息后的行为处理
						if (msg.getBody().contains("STATECHANGETO")) {
							String[] bodys = msg.getBody().split(":");
							// state = Integer.parseInt(bodys[1]);
							setState(Integer.parseInt(bodys[1]));
						}

					}

				}
			//	Thread.sleep(2 * 1000);
				// Thread.sleep(2000);
				// // 发消息
				// // 给子目标发消息
				// if (subElements != null) {
				// for (Element element : subElements) {
				// SGMMessage msg = new SGMMessage("TOSUBM",
				// this.getName(), element.getName(), "hello, "
				// + element.getName() + "! " + index);
				// if (!msgPool.offer(msg)) {
				// System.err.println("send msg failed!");
				// }
				// }
				// }
				//
				// if (parentGoal != null) {
				// SGMMessage msg = new SGMMessage("TOPARENTM",
				// this.getName(), parentGoal.getName(), "hi, "
				// + parentGoal.getName() + "! " + index);
				// if (!msgPool.offer(msg)) {
				// System.err.println("send msg failed!");
				// }
				// }

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
			
			index++;
//			System.out.println(this.getName()+ "---DEBUG-------index: " + index + "; state is "
//					+ state + "; isBehaviourDone: " + isBehaviourDone());
			switch (state) {
			case 0:
				if (!isBehaviourDone()) {
					doInitialBehaviour();
				}

				break;
			case 1:
				if (!isBehaviourDone()) {
					doActivatedBehaviour();
				}
				break;
			case 2:
				if (!isBehaviourDone()) {
					doExecutingBehaviour();
				}
				break;
			case 3:
				if (!isBehaviourDone()) {
					doAchievedBehaviour();
				}
				break;
			case 4:
				if (!isBehaviourDone()) {
					doFailedBehaviour();
				}
				break;
			case 5:
				if (!isBehaviourDone()) {
					doRepairingBehaviour();
				}
				break;
			case 6:
				if (!isBehaviourDone()) {
					doFinishedBehaviour();
				}
				break;
			}
		}
	}

	public void stop() {
		isFinished = true;
	}

	public void setState(int state) {
		this.state = state;
		setBehaviourDone(false);
	}

	public void addSubElement(Element element) {
		this.subElements.add(element);
	}

	public ArrayList<Element> getSubElements() {
		return this.subElements;
	}

	public void setParentGoal(Goal goal) {
		this.parentGoal = goal;
	}

	public Goal getParentGoal() {
		return this.parentGoal;
	}

	public abstract void doInitialBehaviour();

	public abstract void doActivatedBehaviour();

	public abstract void doExecutingBehaviour();

	public abstract void doAchievedBehaviour();

	public abstract void doFailedBehaviour();

	public abstract void doRepairingBehaviour();

	public abstract void doFinishedBehaviour();

	public boolean isBehaviourDone() {
		return isBehaviourDone;
	}

	public void setBehaviourDone(boolean isBehaviourDone) {
		this.isBehaviourDone = isBehaviourDone;
	}

}
