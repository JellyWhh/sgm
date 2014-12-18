/**
 * 
 */
package edu.fudan.se.goalmachine;

import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.fudan.se.goalmachine.support.CauseToRepairing;
import edu.fudan.se.goalmachine.support.RecordedState;
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

	private RecordedState recordedState = RecordedState.Initial; // 让父目标用来记录当前element的状态
	private int priorityLevel; // 让父目标用来记录当前element的优先级

	private Date startTime; // 当前element machine开始执行时的时间
	private int timeLimit; // 完成这个任务的时间限制，单位为分钟minute

	private Date startWaitingTime; // 开始等待的时间
	private int waitingTimeLimit; // 等待时间限制

	private boolean finish; // 标识当前machine是否运行结束，结束后run()里面的while循环将停止

	// element machine相关的各种条件，目前设定是都可以为null
	private Condition contextCondition;
	private Condition preCondition; // 可以为null
	private Condition postCondition; // 可以为null
	private Condition commitmentCondition; // 整个shouldDo符合状态里都有可能不满足的，要一直检查
	private Condition invariantCondition; // 整个shouldDo符合状态里都有可能不满足的，要一直检查

	private Condition defaultPreCondition; // default pre
											// condition，GoalMachine不为空，用来检查是不是所有的子目标都激活成功了。
	private CauseToRepairing causeToRepairing;

	/* 标记各种状态的entry动作是否完成 */
	boolean isInitialEntryDone = false;
	boolean isActivatedEntryDone = false;
	boolean isExecutingEntryDone = false;
	boolean isFailedEntryDone = false;
	boolean isAchievedEntryDone = false;
	boolean isWaitingEntryDone = false;
	boolean isSuspendedEntryDone = false;
	boolean isRepairingEntryDone = false;

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

	@Override
	public void run() {

		this.setCurrentState(State.Initial); // 刚开始是目标状态是initial状态
		// this.setStartTime(new Date()); // 设置目标状态机开始运行时间为当前时间

		Log.logDebug(this.name, "run()", "ElementMachine start!");

		this.setFinish(false);
		while (!this.isFinish()) {
			// 如果contextCondition为空，表示没有设置上下文条件，这个goal是有意义的，可以检出消息，执行行为
			doMainRunningBehaviour();
			try {
				Thread.sleep(2 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Log.logDebug(this.getName(), "run()",
				"ElementMachine stop while cycling!");
	}

	/**
	 * 主体持续运行的行为，包括从消息池里检出消息，以及在对应的状态执行相应的行为
	 */
	private void doMainRunningBehaviour() {

		// 每次循环拿出一条消息，然后后面有不同的方法来处理这个消息，如果消息被处理了，再从消息队列中把这条消息移除，这样可以保证每条消息都经过了处理，不会漏掉
		SGMMessage msg = this.getMsgPool().peek();

		// 每次开始前都先检查是否有STOP消息到来
		if (checkIfStop(msg)) { // 有STOP消息
			this.stopMachine();
		} else {

			switch (this.getCurrentState()) {
			case Initial: // initial
				if (isInitialEntryDone) { // 如果完成了entry动作，循环执行do动作
					initialDo(msg);
				} else {
					initialEntry(); // entry动作只会执行一次
					isInitialEntryDone = true; // 设置为true表示这个entry动作做完了，以后不会再执行了
				}

				break;
			case Activated: // activated
				if (doCCandInvCChecking()) { // 检测到违反
					break;
				} else {

					if (isActivatedEntryDone) { // 自己激活后就等待父目标的Start指令
						activateDo(msg);
					} else { // 刚进入激活状态，尝试把自己状态转换为激活
						activatedEntry();
						isActivatedEntryDone = true; // 设置为true表示这个entry动作做完了，以后不会再执行了
					}
				}

				break;
			case Waiting:// waiting
				if (doCCandInvCChecking()) { // 检测到违反
					break;
				} else {

					if (isWaitingEntryDone) {
						waitingDo();
					} else {
						waitingEntry();
						isWaitingEntryDone = true;
					}
				}
				break;
			case Executing: // executing
				if (doCCandInvCChecking()) { // 检测到违反
					break;
				} else {

					if (isExecutingEntryDone) {
						// 先检查一下是否有SUSPEND消息到达
						if (checkIfSuspend(msg)) { // 需要挂起
							break;
						} else { // 不需要挂起
							executingDo(msg);
						}

					} else {
						executingEntry(msg);
						isExecutingEntryDone = true;
					}
				}

				break;
			case Suspended: // suspened
				if (doCCandInvCChecking()) { // 检测到违反
					break;
				} else {

					if (isSuspendedEntryDone) {
						suspendedDo(msg);
					} else {
						suspendedEntry();
						isSuspendedEntryDone = true;
					}
				}
				break;
			case Repairing: // repairing
				if (isRepairingEntryDone) {
					repairingDo();
				} else {
					repairingEntry();
					isRepairingEntryDone = true;
				}
				break;
			case ProgressChecking: // progressChecking
				progressCheckingDo();
				break;

			case Failed:// failed
				if (isFailedEntryDone) {
					failedDo();
				} else {
					failedEntry();
					isFailedEntryDone = true;
				}
				break;
			case Achieved:// achieved
				if (isAchievedEntryDone) {
					achievedDo();
				} else {
					achievedEntry();
					isAchievedEntryDone = true; // 设置为true表示这个entry动作做完了，以后不会再执行了
				}

				break;

			}
		}
	}

	// ***********************************************
	// 下面的方法都是在各个状态下entry和do部分做的action
	// ***********************************************

	/**
	 * initial状态中entry所做的action：<br>
	 * 只会执行一次
	 */
	public void initialEntry() {
		Log.logDebug(this.getName(), "initialEntry()", "init.");
		// this.setStartTime(new Date()); // setTimer
	}

	/**
	 * initial状态的do所做的action：监听消息池，看是否有ACTIVATE消息到达
	 */
	public void initialDo(SGMMessage msg) {
		Log.logDebug(this.getName(), "initialDo()", "init.");
		// SGMMessage msg = this.getMsgPool().poll(); // 每次拿出一条消息
		if (msg != null) {
			Log.logDebug(
					this.name,
					"initialDo()",
					"get a msg from " + msg.getSender() + ", body is: "
							+ msg.getBody());

			// 收到消息后的行为处理
			if (msg.getBody().equals("ACTIVATE")) {
				this.getMsgPool().poll();
				this.setCurrentState(transition(State.Initial,
						this.getContextCondition()));
				// 如果激活后发现状态仍然回到了Intial状态，说明上下文不满足，激活失败，告诉父目标激活失败
				if (this.getCurrentState() == State.Initial) {
					if (sendMessageToParent("ACTIVATEDFAILED")) {
						Log.logDebug(this.getName(), "initialDo()",
								"send ACTIVATEDFAILED msg to parent succeed!");
					} else {
						Log.logError(this.getName(), "initialDo()",
								"send ACTIVATEDFAILED msg to parent error!");
					}
				}
			}
		}
	}

	/**
	 * activated状态中entry所做的action，子类需要重写
	 */
	public void activatedEntry() {

	}

	/**
	 * activated状态中do所做的action：自身不是root goal，所以要一直等待父目标的START指令，收到后才可以发生状态转换<br>
	 * <code>GoalMachine中需要重写</code>
	 */
	public void activateDo(SGMMessage msg) {

	}

	/**
	 * waiting状态中entry所做的action：告诉父目标自己进入了Waiting状态
	 */
	public void waitingEntry() {
		Log.logDebug(this.getName(), "waitingEntry()", "init.");
		// 设置waiting开始的时间
		this.setStartWaitingTime(new Date());
		// // 先告诉父目标自己进入executing状态了
		// if (this.getParentGoal() != null) {
		// if (sendMessageToParent("WAITING")) {
		// Log.logDebug(this.getName(), "waitingEntry()",
		// "send WAITING msg to parent succeed!");
		// } else {
		// Log.logError(this.getName(), "waitingEntry()",
		// "send WAITING msg to parent error!");
		// }
		// }
	}

	/**
	 * waiting状态中do所做的action：等待父目标的EXITWAITING消息，并且一直做checkPreCondition
	 */
	public void waitingDo() {
		Log.logDebug(this.getName(), "waitingDo()", "init.");

		// 先判断等待时间是否超时
		Date nowTime = new Date();
		long waitTime = nowTime.getTime()
				- this.getStartWaitingTime().getTime(); // 得到的差值单位是毫秒
		// TODO 这里记得可能要在*1000前面加上*60，因为现在设的等待时间限制单位为秒，实际运行时可能需要设置为分钟
		if (waitTime <= (this.getWaitingTimeLimit() * 1000)) { // 没有超时
			// 然后再做条件检查，判断是否能够跳出waiting状态
			checkPreCondition();
			if (this.getPreCondition().isSatisfied()) {
				this.setCurrentState(State.Executing);
			}
		} else { // 超时了
			Log.logDebug(this.getName(), "waitingDo()", "Waiting Timeout!!!!!!");
			this.setCurrentState(State.Failed);
		}

	}

	/**
	 * executing状态中entry所做的action：<code>GoalMachine</code>需要重写
	 */
	public void executingEntry(SGMMessage msg) {
		Log.logDebug(this.getName(), "executingEntry()", "init.");

	}

	/**
	 * executing状态中do所做的action：需要重写
	 */
	public void executingDo(SGMMessage msg) {

	}

	/**
	 * 在executingDo()方法前面先调用的方法，用来检测是否有收到SUSPEND消息，如果收到了，就把状态跳转到Suspended
	 * 
	 * @return true 表示需要挂起，直接进入挂起状态，不能再执行下面的代码；false 不需要挂起
	 */
	private boolean checkIfSuspend(SGMMessage msg) {
		Log.logDebug(this.getName(), "checkIfSuspend()", "init.");

		// 这里用peek()方法取消息，Retrieves, but does not
		// remove，这样就不会影响接下来的executingDo()方法中取消息了
		// SGMMessage msg = this.getMsgPool().peek(); // 每次拿出一条消息
		if (msg != null) {

			// 消息内容是SUSPEND，表示父目标让当前目标进入挂起
			if (msg.getBody().equals("SUSPEND")) {
				this.getMsgPool().poll(); // 如果消息真的是SUSPEND，那么就把它拿出来
				Log.logDebug(this.getName(), "checkIfSuspend()",
						"get a message from " + msg.getSender() + "; body is: "
								+ msg.getBody());
				this.setCurrentState(State.Suspended);
				return true;
			}

		}
		return false;
	}

	/**
	 * suspended状态中entry所做的action：需要重写
	 */
	public void suspendedEntry() {

	}

	/**
	 * suspended状态中do所做的action：需要重写
	 */
	public void suspendedDo(SGMMessage msg) {

	}

	/**
	 * repairing状态中entry所做的action：
	 */
	public void repairingEntry() {

	}

	/**
	 * repairing状态中do所做的action：尝试从Repairing状态跳转到修复后的状态，
	 * 修复后的状态在transiton中会通过调用doRepairing(condition)得到
	 */
	public void repairingDo() {
		Log.logDebug(this.getName(), "repairingDo()", "init.");
		this.setCurrentState(this.transition(State.Repairing, null));
	}

	/**
	 * progressChecking状态中do所做的action：<code>GoalMachine</code>需要重写，
	 * <code>TaskMachine</code>不需要进行进度检查，不会进入到此状态
	 */
	public void progressCheckingDo() {

	}

	/**
	 * failed状态中entry所做的action：告诉父目标自己FAILED
	 */
	public void failedEntry() {
		Log.logDebug(this.getName(), "failedEntry()", "init.");
		// 先告诉父目标自己进入executing状态了
		if (this.getParentGoal() != null) {
			if (sendMessageToParent("FAILED")) {
				Log.logDebug(this.getName(), "failedEntry()",
						"send FAILED msg to parent succeed!");
			} else {
				Log.logError(this.getName(), "failedEntry()",
						"send FAILED msg to parent error!");
			}
		}
	}

	/**
	 * failed状态中do所做的action：停止自己的状态机，<code>GoalMachine</code>需要重写
	 */
	public void failedDo() {
		Log.logDebug(this.getName(), "failedDo()", "init.");
		this.stopMachine();
		Log.logDebug(this.getName(), "failedDo()",
				"It failed to achieved its goal and stopped its machine!");
	}

	/**
	 * achieved状态中entry所做的action：给父目标发送ACHIEVEDDONE消息，然后标记自己完成；如果本身是root
	 * goal，就不用发送了，直接标记整个goal model完成
	 */
	public void achievedEntry() {
		Log.logDebug(this.getName(), "achievedEntry()", "init.");

		if (this.getParentGoal() != null) { // 不是root goal
			if (this.sendMessageToParent("ACHIEVEDDONE")) {
				Log.logDebug(this.getName(), "achievedEntry()",
						"send ACHIEVEDDONE msg to parent succeed!");
			} else {
				Log.logError(this.getName(), "achievedEntry()",
						"send ACHIEVEDDONE msg to parent error!");
			}
		}
	}

	/**
	 * achieved状态中do所做的action：在<code>GoalMachine</code>中需要重写
	 */
	public void achievedDo() {
		Log.logDebug(this.getName(), "achievedDo()", "init.");
		this.stopMachine(); // 本身已完成
		Log.logDebug(this.getName(), "achievedDo()",
				"It has achieved its goal and stopped its machine!");

	}

	// ***********************************************
	// 结束各个状态下entry和do部分做的action声明
	// ***********************************************

	// *************一些辅助方法***************************

	/**
	 * 检查是否有STOP消息到来
	 * 
	 * @return true 有STOP消息到来；false 没有STOP消息到来
	 */
	private boolean checkIfStop(SGMMessage msg) {
		if (msg != null) {

			// 消息内容是SUSPEND，表示父目标让当前目标进入挂起
			if (msg.getBody().equals("STOP")) {
				this.getMsgPool().poll(); // 如果消息真的是STOP，那么就把它拿出来
				Log.logDebug(this.getName(), "checkIfStop()",
						"get a message from " + msg.getSender() + "; body is: "
								+ msg.getBody());
				return true;
			}
		}
		return false;
	}

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

		State ret = currentState;

		switch (currentState) {
		case Initial: // (initial)
			if (condition == null) { // context condition为空
				ret = State.Activated;
			} else {
				// 先判断条件是不是context condition，是的话执行检查然后根据结果进行跳转；如果不是，无意义，返回-1，
				if (condition.getType().equals("CONTEXT")) {
					// 先检查一下context condition，在方法里面通过判断是否满足然后对conditon赋值
					checkContextCondition();
					if (condition.isSatisfied()) {
						ret = State.Activated; // context
												// condition满足，跳转到Activated
					} else {
						ret = State.Initial; // context
												// condition不满足，跳回到initial状态，同时告诉父目标自己激活失败
						// this.setConditionCauseToRepairing(condition);
					}
				}
			}
			break;

		case Activated: // 1(activated)

			// 先检查隐含的default pre conditon，基本只针对goal machine，task
			// machine的defaultPreConditon为空
			if (this.getDefaultPreCondition() != null) {
				checkDefaultPreConditon();
				// default pre condition不满足
				if (!this.getDefaultPreCondition().isSatisfied()) {
					ret = State.Repairing;
					this.setCauseToRepairing(CauseToRepairing.DefaultPreCondition);

					break; // 直接跳到修复，不用再检查下面的pre condition了
				}
			}

			// 再检查pre condition
			if (condition == null) { // pre condition为空
				ret = State.Executing; // 直接跳转到Executing
			} else {
				// 先判断条件是不是pre condition，是的话执行检查然后根据结果进行跳转；如果不是，无意义，返回-1
				if (condition.getType().equals("PRE")) {

					checkPreCondition();
					if (condition.isSatisfied()) {
						ret = State.Executing; // pre condition满足，跳转到Executing
					} else {
						if (condition.isCanRepairing()) {
							ret = State.Repairing; // pre
													// condition不满足，但是能够被修复，跳转到Repairing
							this.setCauseToRepairing(CauseToRepairing.PreCondition);
						} else {
							ret = State.Waiting; // pre
													// condition不满足，而且不能够被修复，跳转到Waiting
						}
					}
				}
			}
			break;

		case Executing: // (executing)
		case ProgressChecking: // progressChecking

			if (condition == null) { // post condition为空
				ret = State.Achieved; // 直接跳转到Achieved
			} else {
				// 先判断条件是不是post condition，是的话执行检查然后根据结果进行跳转；如果不是，无意义，返回-1
				if (condition.getType().equals("POST")) {
					checkPostCondition();
					if (condition.isSatisfied()) {
						ret = State.Achieved; // post condition满足，跳转到Achieved
					} else {
						ret = State.Repairing; // post condition不满足，跳转到Repairing
						this.setCauseToRepairing(CauseToRepairing.PostCondition);
					}
				}
			}
			break;

		case Repairing:
			ret = doRepairing(this.getCauseToRepairing());
			break;

		default:
			break;

		}
		return ret;
	}

	/**
	 * 根据不同的策略进行修复，修复成功后仍然是根据不同的策略跳转到不同的状态<br>
	 * 要有一张修复策略表对应
	 * 
	 * @param cause
	 *            导致进入修复的原因，可能是Condition违反，也可能是subFail
	 * @return 修复结束后要跳转到的状态
	 */
	private State doRepairing(CauseToRepairing cause) {
		// TODO
		State retState = State.Repairing; // 默认返回repairing状态，以防出现异常修复失败
		switch (cause) {
		case InvViolated:
			retState = State.Failed;
			break;
		case CcViolated:
			retState = State.Failed;
			break;
		case SubFail:
			retState = subFailRepairing();
			break;
		case PreCondition:
			retState = State.Failed;
			break;
		case PostCondition:
			retState = State.Failed;
			break;
		case DefaultPreCondition:
			// default pre condition不满足，也就是有子目标激活失败，直接failed（只针对GoalMachine）
			retState = State.Failed;
			break;

		default:
			break;
		}

		return retState;
	}

	/**
	 * 对SubFail情况进行修复，<code>GoalMachine</code>需要重写；<code>TaskMachine</code>
	 * 不需要，因为Task不会收到SubFail消息
	 * 
	 * @return 默认返回repairing状态
	 */
	public State subFailRepairing() {
		return State.Repairing;
	}

	/**
	 * 在ShouldDo复合状态中队commitment condition和invariant
	 * condition进行检查，如果违反，跳到repairing状态，并设置ConditionCauseToRepairing
	 * 
	 * @return true 检测到违反；false 没有检测到违反
	 */
	public boolean doCCandInvCChecking() {

		// 先判断commitment condition是否为null
		if (this.getCommitmentCondition() != null) {
			checkCommitmentCondition();
			if (!this.getCommitmentCondition().isSatisfied()) { // 检测到违反
				Log.logDebug(this.getName(), "doCCandInvCChecking()",
						"commitment condition violation is detected!!!");
				this.setCurrentState(State.Repairing);
				this.setCauseToRepairing(CauseToRepairing.CcViolated);
				return true;
			}
		}

		// 先判断invariant condition是否为null
		if (this.getInvariantCondition() != null) {
			checkInvariantCondition();
			if (!this.getInvariantCondition().isSatisfied()) { // 检测到违反
				Log.logDebug(this.getName(), "doCCandInvCChecking()",
						"invariant condition violation is detected!!!");
				this.setCurrentState(State.Repairing);
				this.setCauseToRepairing(CauseToRepairing.InvViolated);
				return true;
			}
		}
		return false;
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
	 * 停止运行当前machine：<code>GoalMachine需要重写</code>
	 */
	public void stopMachine() {
		this.setFinish(true);

		Log.logDebug(this.getName(), "stopMachine()",
				"It begins to stop its machine");
	}

	/**
	 * 检查default pre condition，<code>GoalMachine</code>需要重写，
	 * <code>TaskMachine</code>不需要重写
	 */
	public void checkDefaultPreConditon() {

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

	public RecordedState getRecordedState() {
		return recordedState;
	}

	public void setRecordedState(RecordedState recordedState) {
		this.recordedState = recordedState;
	}

	public int getPriorityLevel() {
		return priorityLevel;
	}

	public void setPriorityLevel(int priorityLevel) {
		this.priorityLevel = priorityLevel;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public int getTimeLimit() {
		return timeLimit;
	}

	public void setTimeLimit(int timeLimit) {
		this.timeLimit = timeLimit;
	}

	public Date getStartWaitingTime() {
		return startWaitingTime;
	}

	public void setStartWaitingTime(Date startWaitingTime) {
		this.startWaitingTime = startWaitingTime;
	}

	public int getWaitingTimeLimit() {
		return waitingTimeLimit;
	}

	public void setWaitingTimeLimit(int waitingTimeLimit) {
		this.waitingTimeLimit = waitingTimeLimit;
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

	public Condition getDefaultPreCondition() {
		return defaultPreCondition;
	}

	public void setDefaultPreCondition(Condition defaultPreCondition) {
		this.defaultPreCondition = defaultPreCondition;
	}

	public CauseToRepairing getCauseToRepairing() {
		return causeToRepairing;
	}

	public void setCauseToRepairing(CauseToRepairing causeToRepairing) {
		this.causeToRepairing = causeToRepairing;
	}

}
