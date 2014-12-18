/**
 * 
 */
package edu.fudan.se.goalmachine;

import edu.fudan.se.log.Log;

/**
 * 抽象类<br>
 * Task Machine，继承自<code>ElementMachine</code>
 * 
 * @author whh
 *
 */
public abstract class TaskMachine extends ElementMachine {

	/* 标记各种状态的entry动作是否完成 */
	boolean isInitialEntryDone = false;
	boolean isActivatedEntryDone = false;
	boolean isAchievedEntryDone = false;

	/**
	 * 构造方法
	 * 
	 * @param name
	 *            task machine名字
	 * @param parentGoal
	 *            父目标
	 */
	public TaskMachine(String name, ElementMachine parentGoal) {
		super(name, parentGoal);
	}

	/**
	 * activated状态中entry所做的action：在initialDo中已经尝试把自己状态转换为Activated了，进入这个状态后，
	 * 说明已经被激活了，于是向父目标发送ACTIVATEDDONE消息，然后进入activatedDo()方法中，等待父进程的START消息
	 */
	@Override
	public void activatedEntry() {
		Log.logDebug(this.getName(), "activatedEntry()", "init.");

		if (this.sendMessageToParent("ACTIVATEDDONE")) {
			Log.logDebug(this.getName(), "activatedEntry()",
					"send ACTIVATEDDONE msg to "
							+ this.getParentGoal().getName() + " succeed!");
		} else {
			Log.logError(this.getName(), "activatedEntry()",
					"send ACTIVATEDDONE msg to "
							+ this.getParentGoal().getName() + " error!");
		}

	}

	/**
	 * activated状态中do所做的action：自身不是root goal，所以要一直等待父目标的START指令，收到后才可以发生状态转换<br>
	 * <code>GoalMachine中需要重写</code>
	 */
	@Override
	public void activateDo(SGMMessage msg) {
		Log.logDebug(this.getName(), "activateDo()", "init.");

		//SGMMessage msg = this.getMsgPool().poll(); // 每次拿出一条消息
		if (msg != null) {
			Log.logDebug(this.getName(), "activateDo()",
					"get a message from " + msg.getSender() + "; body is: "
							+ msg.getBody());

			// 消息内容是START，表示父目标让当前目标开始状态转换
			if (msg.getBody().equals("START")) {
				this.getMsgPool().poll();
				this.setCurrentState(this.transition(State.Activated,
						this.getPreCondition()));
			}

		}
	}

	

	/**
	 * executing状态中do所做的action：这个需要根据具体的task有不同的具体执行行为，所以这个是抽象方法，在实例化时具体实现
	 */
	@Override
	public abstract void executingDo(SGMMessage msg);

	/**
	 * executing状态中do所做的action：等待外部进程（比如UI）发来的END消息，这个方法在executingDo()方法的具体实现中调用<br>
	 * 接到END消息后，尝试跳转到Achieved
	 */
	public void executingDo_waitingEnd(SGMMessage msg) {
		Log.logDebug(this.getName(), "executingDo_waitingEnd()", "init.");

		//SGMMessage msg = this.getMsgPool().poll(); // 拿出一条消息
		if (msg != null) {
			Log.logDebug(this.getName(), "executingDo_waitingEnd()",
					"get a message from " + msg.getSender() + "; body is: "
							+ msg.getBody());

			if (msg.getBody().equals("END")) {	//收到外部UI的END消息
				this.getMsgPool().poll();
				this.setCurrentState(this.transition(State.Executing,
						this.getPostCondition()));
			}else if (msg.getBody().equals("SUSPEND")) {	//收到父目标的SUSPEND消息
				this.getMsgPool().poll();
				this.setCurrentState(State.Suspended);
			}
		}
	}

	/**
	 * suspended状态中do所做的action：目标处于挂起状态时，只需要不断检查是否有RESUME到来即可，如果收到了，
	 * 把目标状态转换为executing状态
	 */
	@Override
	public void suspendedDo(SGMMessage msg) {
		Log.logDebug(this.getName(), "suspendedDo()", "init.");
	//	SGMMessage msg = this.getMsgPool().poll(); // 每次拿出一条消息
		if (msg != null) {
			Log.logDebug(this.getName(), "suspendedDo()", "get a message from "
					+ msg.getSender() + "; body is: " + msg.getBody());
			if (msg.getBody().equals("RESUME")) {
				this.getMsgPool().poll();
				// 把自己状态设置为executing
				this.setCurrentState(State.Executing);
			}
		}
	}

}
