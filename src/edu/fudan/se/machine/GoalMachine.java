/**
 * 
 */
package edu.fudan.se.machine;

import java.util.ArrayList;

import edu.fudan.se.goalmodel.Goal;
import edu.fudan.se.message.SGMMessage;
import edu.fudan.se.message.SGMMessagePool;

/**
 * 目标状态机
 * 
 * @author whh
 *
 */
public abstract class GoalMachine extends Goal implements Runnable {

	private SGMMessagePool msgPool; // 消息队列，即消息池

	/**
	 * 目标状态机
	 * 
	 * @param name
	 *            目标状态机的名称
	 * @param msgPool
	 *            目标相关联的消息队列，所有目标共享一个消息队列，即消息池
	 */
	public GoalMachine(String name, SGMMessagePool msgPool) {
		super(name);
		this.msgPool = msgPool;
	}

	@Override
	public void run() {
		System.out.println("---DEBUG--- GoalMachine: " + this.name
				+ " started!");

		this.setState(0);// 刚开始是目标状态是0
		if (!isBehaviourDone()) {
			doInitialBehaviour();
		}

		this.setFinished(false);
		while (!this.isFinished()) {
			// if (!isBehaviourDone()) {
			if (getContextCondition() == null) { // 如果contextCondition为空，表示没有设置上下文条件，这个goal是有意义的，可以检出消息，执行行为
				doMainRunningBehaviour();
			} else { // 否则，就要一直检查contextCondition，直到它为true，这个goal才有执行的意义
				checkContextCondition();
				if (getContextCondition() == true) {
					doMainRunningBehaviour();
				}
			}
			// }
		}
	}

	/**
	 * 主体持续运行的行为，包括从消息池里检出消息，以及在对应的状态执行相应的行为
	 */
	private void doMainRunningBehaviour() {
		try {
			ArrayList<SGMMessage> msgs = getMsgPool().getMessage(this);
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

			Thread.sleep(1 * 1000); // 每隔1秒循环一次

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}

		// System.out.println(this.getName()+ "---DEBUG-------index: " +
		// index + "; state is "
		// + state + "; isBehaviourDone: " + isBehaviourDone());
		switch (this.getState()) {
		case 0:
			if (!isBehaviourDone()) {
				doInitialBehaviour();
			}

			break;
		case 1:
			if (!isBehaviourDone()) {
				if (getActivatedCondition() == null) { // activatedCondition为空，表示父目标一旦发消息让子目标激活，子目标就可以被激活，直接执行激活状态下的行为
					doActivatedBehaviour();
				} else { // 否则，一直检查激活条件，知道它为true
					checkActivatedCondition();
					if (getActivatedCondition() == true) {
						doActivatedBehaviour();
					}
				}
			}
			break;
		case 2:
			if (!isBehaviourDone()) {

				if (getPreCondition() == null) { // preCondition为空，直接进入下一个检查
					if (getTriggerCondition() == null) { // triggerCondition为空，表示父目标一旦发消息让子目标执行，子目标就可以直接开始执行
						doExecutingBehaviour();
					} else { // 不然的话，只有triggerCondition为true的时候才可以执行
						checkTriggerCondition();
						if (getTriggerCondition() == true) { // triggerCondition为true
							doExecutingBehaviour();
						}
					}
				} else {
					checkPreCondition();
					if (getPreCondition() == true) { // preCondition满足了，表示goal可以被执行，但仍需要检查triggerCondition
						if (getTriggerCondition() == null) { // triggerCondition为空，表示父目标一旦发消息让子目标执行，子目标就可以直接开始执行
							doExecutingBehaviour();
						} else { // 不然的话，只有triggerCondition为true的时候才可以执行
							checkTriggerCondition();
							if (getTriggerCondition() == true) { // triggerCondition为true
								doExecutingBehaviour();
							}
						}
					}
				}

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
				if (getPostCondition() == null) { // postCondition为空，直接执行doFinishedBehaviour
					doFinishedBehaviour();
				} else { // 否则，一直检查postCondition，直到它为true
					checkPostCondition();
					if (getPostCondition() == true) {
						doFinishedBehaviour();
					}
				}

			}
			break;
		}
	}

	public void stop() {
		this.setFinished(true);
	}

	public SGMMessagePool getMsgPool() {
		return msgPool;
	}

	// *********************************************
	// 下面的方法都是需要新建一个GoalMachine实例时根据具体要求实现的
	// 主要做的是在各个状态上需要执行的行为，以及各个状态相关的条件检查
	// *********************************************

	/**
	 * 在initial状态要执行的行为
	 */
	public abstract void doInitialBehaviour();

	/**
	 * 在activated状态要执行的行为
	 */
	public abstract void doActivatedBehaviour();

	/**
	 * 在executing状态要执行的行为
	 */
	public abstract void doExecutingBehaviour();

	/**
	 * 在achieved状态要执行的行为
	 */
	public abstract void doAchievedBehaviour();

	/**
	 * 在failed状态要执行的行为
	 */
	public abstract void doFailedBehaviour();

	/**
	 * 在repairing状态要执行的行为
	 */
	public abstract void doRepairingBehaviour();

	/**
	 * 在finished状态要执行的行为
	 */
	public abstract void doFinishedBehaviour();

	/**
	 * 检查contextCondition是否被满足，如果满足了，就调用set方法将之设置为true
	 */
	public abstract void checkContextCondition();

	/**
	 * 检查activatedCondition是否被满足，如果满足了，就调用set方法将之设置为true
	 */
	public abstract void checkActivatedCondition();

	/**
	 * 检查preCondition是否被满足，如果满足了，就调用set方法将之设置为true
	 */
	public abstract void checkPreCondition();

	/**
	 * 检查triggerCondition是否被满足，如果满足了，就调用set方法将之设置为true
	 */
	public abstract void checkTriggerCondition();

	/**
	 * 检查postCondition是否被满足，如果满足了，就调用set方法将之设置为true
	 */
	public abstract void checkPostCondition();

}
