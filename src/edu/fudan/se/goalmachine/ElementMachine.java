/**
 * 
 */
package edu.fudan.se.goalmachine;

import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.fudan.se.log.Log;

/**
 * 抽象类<br>
 * 元素状态机，GoalMachine和TaskMachine继承此抽象类
 * 
 * @author whh
 *
 */
public abstract class ElementMachine implements Runnable {

	protected String name; // element的名字
	private ElementMachine parentGoal; // 当前element的父目标，除root
										// goal外每个element都有parentGoal

	private State currentState; // element目前所处的状态

	private BlockingQueue<SGMMessage> msgPool; // 消息队列，即消息池，可以直观理解为当前element的个人信箱

	private int recordedState; // 让父目标用来记录当前element的状态

	private Date startTime; // 当前element machine开始执行时的时间

	private boolean finish; // 标识当前machine是否运行结束，结束后run()里面的while循环将停止

	// element machine相关的各种条件
	private Condition contextCondition = new Condition("CONTEXT");
	private Condition preCondition; // 可以为null
	private Condition postCondition; // 可以为null
	private Condition commitmentCondition = new Condition("COMMITMENT"); // 整个shouldDo符合状态里都有可能不满足的，要一直检查
	private Condition invariantCondition = new Condition("INVARIANT"); // 整个shouldDo符合状态里都有可能不满足的，要一直检查

	/**
	 * 构造方法
	 * 
	 * @param name
	 *            当前element的名字
	 * @param parentGoal
	 *            当前element的父目标，如果当前目标是root goal，这个值可以设置为null
	 */
	public ElementMachine(String name, ElementMachine parentGoal) {
		this.name = name;
		this.parentGoal = parentGoal;
		this.msgPool = new LinkedBlockingQueue<SGMMessage>();
	}

	// ***********************************************
	// 下面的方法都是在各个状态下entry和do部分做的action
	// ***********************************************

	/**
	 * initial状态中entry所做的action：<br>
	 * 只会执行一次
	 */
	public void initialEntry() {
		// TODO
		// isInitialEntryDone = true; // 设置为true表示这个entry动作做完了，以后不会再执行了
	}

	/**
	 * initial状态的do所做的action：监听消息池，看是否有ACTIVATE消息到达
	 */
	public void initialDo() {
		SGMMessage msg = this.getMsgPool().poll(); // 每次拿出一条消息
		if (msg != null) {
			Log.logDebug(
					this.name,
					"initialDo()",
					"get a msg from" + msg.getSender() + ", body is: "
							+ msg.getBody());

			// 收到消息后的行为处理
			if (msg.getBody().equals("ACTIVATE")) {
				// TODO
				this.setCurrentState(transition(State.Initial,
						getContextCondition()));
			}
		}
	}

	/**
	 * activated状态中do所做的action：自身不是root goal，所以要一直等待父目标的START指令，收到后才可以发生状态转换
	 */
	public void activateDo_waitingParentNotify() {
		Log.logDebug(this.getName(), "activateDo_waitingParentNotify()",
				"init.");

		SGMMessage msg = this.getMsgPool().poll(); // 每次拿出一条消息
		if (msg != null) {
			Log.logDebug(this.getName(), "activateDo_waitingParentNotify()",
					"get a message from " + msg.getSender() + "; body is: "
							+ msg.getBody());

			// 消息内容是START，表示父目标让当前目标开始状态转换
			if (msg.getBody().equals("START")) {
				this.setCurrentState(this.transition(State.Activated,
						this.getPreCondition()));
			}

		}
	}

	/**
	 * achieved状态中entry所做的action：给父目标发送ACHIEVEDDONE消息，然后标记自己完成；如果本身是root
	 * goal，就不用发送了，直接标记整个goal model完成
	 */
	public void achievedEntry() {
		// TODO
		Log.logDebug(this.getName(), "achievedEntry()", "init.");

		if (this.getParentGoal() != null) { // 不是root goal
			if (this.sendMessageToParent("ACHIEVEDDONE")) {
				this.stopMachine(); // 本身已完成
				// isAchievedEntryDone = true; // 设置为true表示这个entry动作做完了，以后不会再执行了
				Log.logDebug(this.getName(), "achievedEntry()",
						"send msg to parent succeed!");
				Log.logDebug(this.getName(), "achievedEntry()",
						"It has achieved its goal and begins to stop its machine");
			} else {
				Log.logError(this.getName(), "achievedEntry()",
						"send msg to parent error!");
			}
		} else {
			this.stopMachine(); // 本身已完成
			// isAchievedEntryDone = true; // 设置为true表示这个entry动作做完了，以后不会再执行了
			Log.logDebug(this.getName(), "achievedEntry()",
					"It has achieved its goal and begins to stop its machine");
		}

	}

	// ***********************************************
	// 结束各个状态下entry和do部分做的action声明
	// ***********************************************

	// *************一些辅助方法***************************

	/**
	 * 状态转换
	 * 
	 * @param currentState
	 *            目前所处状态
	 * @param condition
	 *            转换时要检查的条件
	 * @return 转换后的状态
	 */
	public State transition(State currentState, Condition condition) {
		// TODO
		State ret = currentState;

		switch (currentState) {
		case Initial: // (initial)
			// 先判断条件是不是context condition，是的话执行检查然后根据结果进行跳转；如果不是，无意义，返回-1，
			if (condition.getType().equals("CONTEXT")) {
				if (condition.isSatisfied()) {
					ret = State.Activated; // context
											// condition满足，跳转到1(activated)
				} else {
					ret = State.Waiting; // context condition不满足，跳转到3(waiting)
				}
			}
			break;
		case Activated: // 1(activated)
			// 先判断条件是不是pre condition，是的话执行检查然后根据结果进行跳转；如果不是，无意义，返回-1
			if (condition.getType().equals("PRE")) {
				if (condition.isSatisfied()) {
					ret = State.Executing; // pre condition满足，跳转到2(executing)
				} else {
					ret = State.Waiting; // pre condition不满足，跳转到3(waiting)
				}
			}
			break;

		case Executing: // (executing)
			// 先判断条件是不是post condition，是的话执行检查然后根据结果进行跳转；如果不是，无意义，返回-1
			if (condition.getType().equals("POST")) {
				if (condition.isSatisfied()) {
					ret = State.Achieved; // post condition满足，跳转到5(achieved)
				} else {
					ret = State.Waiting; // post condition不满足，跳转到3(waiting)
				}
			}
			break;
		case Repairing: // 3(repairing)
			// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			// TODO
			break;

		default:
			ret = null;
			break;
		}

		return ret;
	}

	/**
	 * 发送消息给parentGoal
	 * 
	 * @param body
	 *            消息的body部分
	 * @return true 发送成功, false 发送失败
	 */
	public boolean sendMessageToParent(String body) {
		SGMMessage msg = new SGMMessage("TOPARENT", this.getName(), this
				.getParentGoal().getName(), body);
		if (this.getParentGoal().getMsgPool().offer(msg)) {
			// 发送成功
			Log.logMessage(msg, true);
			return true;
		} else {
			Log.logMessage(msg, false);
			return false;
		}

	}

	/**
	 * 停止运行当前machine
	 */
	public void stopMachine() {
		this.setFinish(true);
	}

	// *************结束一些辅助方法************************

	// ***********************************************
	// 下面的方法都是需要新建一个GoalMachine实例时根据具体要求实现的
	// 主要做的各个状态相关的条件检查
	// ***********************************************

	// *************checkCondition抽象方法**************

	public abstract void checkContextCondition();

	public abstract void checkPreCondition();

	public abstract void checkPostCondition();

	public abstract void checkCommitmentCondition();

	public abstract void checkInvariantCondition();

	// *************结束checkCondition抽象方法**********

	// *********************************************
	// 结束抽象方法声明
	// *********************************************

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public State getCurrentState() {
		return currentState;
	}

	public void setCurrentState(State currentState) {
		this.currentState = currentState;
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

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public boolean isFinish() {
		return finish;
	}

	public void setFinish(boolean finish) {
		this.finish = finish;
	}

	public ElementMachine getParentGoal() {
		return parentGoal;
	}

	public void setParentGoal(ElementMachine parentGoal) {
		this.parentGoal = parentGoal;
	}

	public Condition getContextCondition() {
		return contextCondition;
	}

	public void setContextCondition(Condition contextCondition) {
		this.contextCondition = contextCondition;
	}

	public Condition getPreCondition() {
		return preCondition;
	}

	public void setPreCondition(Condition preCondition) {
		this.preCondition = preCondition;
	}

	public Condition getPostCondition() {
		return postCondition;
	}

	public void setPostCondition(Condition postCondition) {
		this.postCondition = postCondition;
	}

	public Condition getCommitmentCondition() {
		return commitmentCondition;
	}

	public void setCommitmentCondition(Condition commitmentCondition) {
		this.commitmentCondition = commitmentCondition;
	}

	public Condition getInvariantCondition() {
		return invariantCondition;
	}

	public void setInvariantCondition(Condition invariantCondition) {
		this.invariantCondition = invariantCondition;
	}

}
