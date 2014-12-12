/**
 * 
 */
package edu.fudan.se.goalmachine;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 目标数据结构 它与<code>Task</code>的区别是，<code>Task</code>不能够再有subElement
 * 
 * @author whh
 *
 */
public abstract class GoalMachine extends Element implements Runnable {

	private int state; // 取值0(initial),1(activated),2(executing),3(waiting),4(failed),5(achieved),6(progressChecking),7(suspended)

	private GoalMachine parentGoal;
	private ArrayList<Element> subElements = new ArrayList<>(); // subElements

	private int decomposition; // 分解，0表示AND分解，1表示OR分解
	private int schedulerMethod; // AND分解情况下的子目标执行顺序，0表示并行处理，1表示串行

	private boolean isFinished; // 标识整个目标状态机是否完成
	private boolean isBehaviourDone = false; // 标识一个行为是否结束，true表示结束

	private Condition contextCondition = new Condition("CONTEXT");
	private Condition preCondition; // 可以为null
	private Condition postCondition; // 可以为null
	private Condition commitmentCondition = new Condition("COMMITMENT"); // 整个shouldDo符合状态里都有可能不满足的，要一直检查
	private Condition invariantCondition = new Condition("INVARIANT"); // 整个shouldDo符合状态里都有可能不满足的，要一直检查

	/* 标记各种状态的entry动作是否完成 */
	boolean isInitialEntryDone = false;
	boolean isActivatedEntryDone = false;
	boolean isActivatedDo_waitingSubReplyDone = false;
	boolean isExecutingEntryDone = false;
	boolean isFailedEntryDone = false;
	boolean isAchievedEntryDone = false;

	/**
	 * 目标状态机
	 * 
	 * @param name
	 *            目标状态机的名称
	 * @param decomposition
	 *            当前目标的分解方式，0表示AND分解，1表示OR分解
	 * @param schedulerMethod
	 *            AND分解情况下的子目标执行顺序，0表示并行处理，1表示串行，如果decomposition是OR分解，这个值无意义，
	 *            可以设置成-1
	 * @param parentGoal
	 *            当前目标的父目标，如果当前目标是root goal，这个值可以设置为null
	 */
	public GoalMachine(String name, int decomposition, int schedulerMethod,
			GoalMachine parentGoal) {
		super(name);
		this.decomposition = decomposition;
		this.schedulerMethod = schedulerMethod;
		this.parentGoal = parentGoal;
		this.setMsgPool(new LinkedBlockingQueue<SGMMessage>());
	}

	@Override
	public void run() {
		System.out.println("---DEBUG--- GoalMachine: " + this.name
				+ " started!");

		this.setState(0);// 刚开始是目标状态是0

		this.setFinished(false);
		while (!this.isFinished()) {
			// 如果contextCondition为空，表示没有设置上下文条件，这个goal是有意义的，可以检出消息，执行行为
			doMainRunningBehaviour();
			// TODO
		}
	}

	/**
	 * 主体持续运行的行为，包括从消息池里检出消息，以及在对应的状态执行相应的行为
	 */
	private void doMainRunningBehaviour() {

		switch (this.getState()) {
		case 0: // initial
			if (isInitialEntryDone) { // 如果完成了entry动作，循环执行do动作
				initialDo();
			} else {
				initialEntry(); // entry动作只会执行一次
			}

			break;
		case 1: // activated
			// TODO 要进行commitment condition和invariant condition的检查
			if (!isActivatedEntryDone) {
				// 刚进入激活状态，给subElements发消息
				activatedEntry();

			} else { // 激活消息已发完
				if (!isActivatedDo_waitingSubReplyDone) { // 等待subElements反馈中
					activatedDo_waitingSubReply();
				} else { // subElements都反馈已激活，进入等待父目标的START指令中。如果是root
							// goal，则在activatedDo_waitingSubReply()方法中已发生状态跳转
					activateDo_waitingParentNotify();
				}
			}

			break;
		case 2: // executing
			// TODO 要进行commitment condition和invariant condition的检查

			if (this.getDecomposition() == 0) { // AND分解
				if (this.getSchedulerMethod() == 0) { // 并行
					if (!isExecutingEntryDone) { // 刚进入executing状态，给所有子目标发送start
						executingEntry_AND_PARALLERL();
					} else { // 发送start消息完毕，进入等待子目标反馈中，必须是所有的子目标都反馈完成，如果所有子目标都完成了，自己可以尝试发生跳转到achieved
						executingDo_waitingSubReply_AND_PARALLERL();
					}
				} else { // 串行
					/*
					 * 依次给subElemtent发start消息，
					 * 然后进入executingDo_waitingSubReply_AND_SERIAL
					 * ，等待它反馈完成的消息，得到消息后重新进入executingEntry_AND_SERIAL
					 * ，给下个未完成状态的subElement发消息
					 * ，如此循环，直到最后一个subElement完成，可以尝试发生跳转。
					 */
					if (!isExecutingEntryDone) {
						executingEntry_AND_SERIAL();
					} else {
						executingDo_waitingSubReply_AND_SERIAL();
					}
				}
			} else { // OR分解
				// TODO
			}

			break;
		case 3:// waiting
			waitingDo();

			break;
		case 4:// failed
			if (isFailedEntryDone) {
				failedDo();
			} else {
				failedEntry();
			}

			break;
		case 5:// achieved
			if (isAchievedEntryDone) {

			} else {
				achievedEntry();
			}

			break;
		case 6:
			if (!isBehaviourDone()) {

			}
			break;
		}
	}

	/**
	 * 停止整个goal machine
	 */
	private void stop() {
		this.setFinished(true);
	}

	private int transition(int currentState, Condition condition) {
		//TODO
		int ret = -1;

		switch (currentState) {
		case 0: // 0(initial)
			// 先判断条件是不是context condition，是的话执行检查然后根据结果进行跳转；如果不是，无意义，返回-1，
			if (condition.getType().equals("CONTEXT")) {
				if (condition.isSatisfied()) {
					ret = 1; // context condition满足，跳转到1(activated)
				} else {
					ret = 3; // context condition不满足，跳转到3(waiting)
				}
			}
			break;
		case 1: // 1(activated)
			// 先判断条件是不是pre condition，是的话执行检查然后根据结果进行跳转；如果不是，无意义，返回-1
			if (condition.getType().equals("PRE")) {
				if (condition.isSatisfied()) {
					ret = 2; // pre condition满足，跳转到2(executing)
				} else {
					ret = 3; // pre condition不满足，跳转到3(waiting)
				}
			}
			break;

		case 2: // 2(executing)
			// 先判断条件是不是post condition，是的话执行检查然后根据结果进行跳转；如果不是，无意义，返回-1
			if (condition.getType().equals("POST")) {
				if (condition.isSatisfied()) {
					ret = 5; // post condition满足，跳转到5(achieved)
				} else {
					ret = 3; // post condition不满足，跳转到3(waiting)
				}
			}
			break;
		case 3: // 3(repairing)
			// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			// TODO
			break;

		default:
			ret = -1;
			break;
		}

		return ret;
	}

	// ***********************************************
	// 下面的方法都是在各个状态下entry和do部分做的action
	// ***********************************************

	/**
	 * initial状态中entry所做的action：<br>
	 * 只会执行一次
	 */
	private void initialEntry() {
		// TODO
		isInitialEntryDone = true; // 设置为true表示这个entry动作做完了，以后不会再执行了
	}

	/**
	 * initial状态的do所做的action：监听消息池，看是否有ACTIVATE消息到达
	 */
	private void initialDo() {
		SGMMessage msg = this.getMsgPool().poll(); // 每次拿出一条消息
		if (msg != null) {
			System.out.println(msg.getReceiver() + " get a message, from "
					+ msg.getSender() + "; body is: " + msg.getBody());

			// 收到消息后的行为处理
			if (msg.getBody().equals("ACTIVATE")) {
				// TODO
				this.setState(transition(0, getContextCondition()));
			}
		}
	}

	/**
	 * activated状态中entry所做的action：给所有subElement发ACTIVATE消息，让其激活
	 */
	private void activatedEntry() {
		System.out
				.println("---DEBUG--- "
						+ this.getName()
						+ " is doing activatedEntry! It will send a \"ACTIVATE\" event message to all its subElements!");
		if (this.getSubElements() != null) {

			if (this.getDecomposition() == 0) { // AND分解，需要激活所有subElements
				for (Element element : this.getSubElements()) {
					SGMMessage msg = new SGMMessage("TOSUB", this.getName(),
							element.getName(), "ACTIVATE");
					// 给element的消息池发消息
					if (!element.getMsgPool().offer(msg)) {
						System.err.println("---ERROR--- " + this.getName()
								+ " activatedEntry(): send msg error!");
					}
				}
			} else { // OR分解，激活一个子目标？？？？？？？？？？？？？？？
				// TODO
			}
		} else {
			System.err.println("---ERROR--- " + this.getName()
					+ " activatedEntry(): getSubElements() == null.");
		}

		isActivatedEntryDone = true; // 设置为true表示这个entry动作做完了，以后不会再执行了
	}

	/**
	 * activated状态中do所做的action：检查消息队列，看subElement是否已激活，如果subElement已激活，
	 * 表示自己的激活全部完成，可告诉parent开始下一步行动了；这是，目标仍处于activated状态，等待父目标发送START指令。（root
	 * goal除外，root goal要直接发生状态跳转）
	 */
	private void activatedDo_waitingSubReply() {
		System.out.println("---DEBUG--- " + this.getName()
				+ " is doing activatedDo_waitingSubReply!");
		SGMMessage msg = this.getMsgPool().poll(); // 每次拿出一条消息
		if (msg != null) {
			System.out.println(msg.getReceiver() + " get a message, from "
					+ msg.getSender() + "; body is: " + msg.getBody());
			// 消息内容是ACTIVATEDDONE，表示发送这条消息的子目标已激活
			if (msg.getBody().equals("ACTIVATEDDONE")) {
				setSubElementRecordedState(msg.getSender(), msg.getBody());
			}

		}

		// 检查是否所有子目标都已激活
		if (this.getDecomposition() == 0) { // AND分解
			int count = 0;
			for (Element element : this.getSubElements()) {
				if (element.getRecordedState() == 1) { // 激活
					count++;
				}
			}
			if (count == this.getSubElements().size()) { // 全部激活
				// 如果subElement已激活，表示自己的激活全部完成，可告诉parent开始下一步行动了；这是，目标仍处于activated状态，等待父目标发送START指令。
				if (this.getParentGoal() != null) { // 不是root goal， 有parent goal
					SGMMessage msgToParent = new SGMMessage("TOPARENT",
							this.getName(), this.getParentGoal().getName(),
							"ACTIVATEDDONE");
					if (this.getParentGoal().getMsgPool().offer(msgToParent)) {
						isActivatedDo_waitingSubReplyDone = true;
					} else {
						System.err
								.println("---ERROR--- "
										+ this.getName()
										+ " activatedDo_waitingSubReply(): send msg to parent error!");
					}
				} else { // 自己本身是root goal，无需等待父目标发送START指令，直接发生跳转
					this.setState(transition(1, preCondition));
				}
			}
		} else { // OR分解
			// TODO
		}

	}

	/**
	 * activated状态中do所做的action：自身不是root goal，所以要一直等待父目标的START指令，收到后才可以发生状态转换
	 */
	private void activateDo_waitingParentNotify() {
		System.out.println("---DEBUG--- " + this.getName()
				+ " is doing activateDo_waitingParentNotify!");
		SGMMessage msg = this.getMsgPool().poll(); // 每次拿出一条消息
		if (msg != null) {
			System.out.println(msg.getReceiver() + " get a message, from "
					+ msg.getSender() + "; body is: " + msg.getBody());
			// 消息内容是START，表示父目标让当前目标开始状态转换
			if (msg.getBody().equals("START")) {
				this.setState(transition(1, preCondition));
			}

		}
	}

	/**
	 * AND分解而且是并行<br>
	 * executing状态中entry所做的action：给所有subElement发START消息，让其开始进入执行Executing状态
	 */
	private void executingEntry_AND_PARALLERL() {
		System.out
				.println("---DEBUG--- "
						+ this.getName()
						+ " is doing executingEntry_AND_PARALLERL! It will send a \"START\" event message to all its subElements!");
		if (getSubElements() != null) {

			for (Element element : getSubElements()) {
				SGMMessage msg = new SGMMessage("TOSUB", this.getName(),
						element.getName(), "START");
				// 给element的消息池发消息
				if (!element.getMsgPool().offer(msg)) {
					System.err
							.println("---ERROR--- "
									+ this.getName()
									+ " executingEntry_AND_PARALLERL(): send msg error!");
				}
			}
		} else {
			System.err
					.println("---ERROR--- "
							+ this.getName()
							+ " executingEntry_AND_PARALLERL(): getSubElements() == null.");
		}

		isExecutingEntryDone = true; // 设置为true表示这个entry动作做完了，以后不会再执行了
	}

	/**
	 * AND分解而且是并行<br>
	 * executing状态中do所做的action：等待所有subElements反馈消息ACHIEVED，必须是所有的子目标都反馈完成，
	 * 如果所有子目标都完成了，自己可以尝试发生跳转到achieved
	 */
	private void executingDo_waitingSubReply_AND_PARALLERL() {
		System.out.println("---DEBUG--- " + this.getName()
				+ " is doing executingDo_waitingSubReply_AND_PARALLERL!");
		SGMMessage msg = this.getMsgPool().poll(); // 拿出一条消息
		if (msg != null) {
			System.out.println(msg.getReceiver() + " get a message, from "
					+ msg.getSender() + "; body is: " + msg.getBody());
			// 如果子目标反馈的是ACHIEVED
			if (msg.getBody().equals("ACHIEVEDDONE")) {
				setSubElementRecordedState(msg.getSender(), msg.getBody());
			}

			// 检查是否全部已完成
			int count = 0;
			for (Element element : this.getSubElements()) {
				if (element.getRecordedState() == 5) { // achieved
					count++;
				}
			}
			if (count == this.getSubElements().size()) { // 全部激活,自己可以尝试发生跳转到achieved
				this.setState(transition(2, postCondition));
			}
		}

	}

	/**
	 * AND分解而且是串行<br>
	 * executing状态中entry所做的action：依次给subElemtent发start消息，
	 * 然后进入executingDo_waitingSubReply_AND_SERIAL
	 * ，等待它反馈完成的消息，得到消息后重新进入executingEntry_AND_SERIAL，给下个未完成状态的subElement发消息
	 * ，如此循环，直到最后一个subElement完成，可以尝试发生跳转。
	 */
	private void executingEntry_AND_SERIAL() {
		System.out.println("---DEBUG--- " + this.getName()
				+ " is doing executingEntry_AND_SERIAL!");
		if (getSubElements() != null) {

			for (Element element : getSubElements()) {
				// 找到下一个还不是已完成状态的subElement，给其发送start消息，然后break，跳出循环
				if (element.getRecordedState() != 5) { // 5表示是完成状态
					SGMMessage msg = new SGMMessage("TOSUB", this.getName(),
							element.getName(), "START");
					// 给element的消息池发消息
					if (element.getMsgPool().offer(msg)) {
						break;
					} else {
						System.err
								.println("---ERROR--- "
										+ this.getName()
										+ " executingEntry_AND_SERIAL(): send msg error!");
					}
				}
			}
		} else {
			System.err
					.println("---ERROR--- "
							+ this.getName()
							+ " executingEntry_AND_SERIAL(): getSubElements() == null.");
		}

		isExecutingEntryDone = true;
	}

	/**
	 * AND分解而且是串行<br>
	 * executing状态中entry所做的action：等待subElement反馈完成的消息，得到消息后，把它标记为achieved，
	 * 然后重新进入executingEntry_AND_SERIAL，给下个未完成状态的subElement发消息
	 * ，如此循环，直到最后一个subElement完成，可以尝试发生跳转。
	 */
	private void executingDo_waitingSubReply_AND_SERIAL() {
		System.out.println("---DEBUG--- " + this.getName()
				+ " is doing executingDo_waitingSubReply_AND_SERIAL!");

		SGMMessage msg = this.getMsgPool().poll(); // 拿出一条消息
		if (msg != null) { // 收到了一条消息
			System.out.println(msg.getReceiver() + " get a message, from "
					+ msg.getSender() + "; body is: " + msg.getBody());
			// 如果子目标反馈的是ACHIEVED
			if (msg.getBody().equals("ACHIEVEDDONE")) {
				setSubElementRecordedState(msg.getSender(), msg.getBody());
			}

			// 检查是不是所有的都已完成
			int count = 0;
			for (Element element : this.getSubElements()) {
				if (element.getRecordedState() == 5) { // achieved
					count++;
				}
			}
			if (count == this.getSubElements().size()) { // 全部激活,自己可以尝试发生跳转到achieved
				this.setState(transition(2, postCondition));
			} else {
				isExecutingEntryDone = false; // 这样下次循环的时候就会再次去执行executingEntry_AND_SERIAL()
			}
		}

	}

	/**
	 * waiting状态中do所做的action：
	 */
	private void waitingDo() {
		// TODO
	}

	/**
	 * failed状态中entry所做的action：
	 */
	private void failedEntry() {
		// TODO
		isFailedEntryDone = true; // 设置为true表示这个entry动作做完了，以后不会再执行了
	}

	/**
	 * failed状态中do所做的action：
	 */
	private void failedDo() {
		// TODO
	}

	/**
	 * achieved状态中entry所做的action：给父目标发送ACHIEVEDDONE消息，然后标记自己完成；如果本身是root
	 * goal，就不用发送了，直接标记整个goal model完成
	 */
	private void achievedEntry() {
		// TODO
		System.out.println("---DEBUG--- " + this.getName()
				+ " is doing achievedEntry!");
		if (this.getParentGoal() != null) { // 不是root goal
			SGMMessage msgToParent = new SGMMessage("TOPARENT", this.getName(),
					this.getParentGoal().getName(), "ACHIEVEDDONE");
			if (this.getParentGoal().getMsgPool().offer(msgToParent)) {
				stop(); // 本身已完成
				isAchievedEntryDone = true; // 设置为true表示这个entry动作做完了，以后不会再执行了
			} else {
				System.err.println("---ERROR--- " + this.getName()
						+ " achievedEntry(): send msg to parent error!");
			}
		} else {
			stop(); // 本身已完成
			isAchievedEntryDone = true; // 设置为true表示这个entry动作做完了，以后不会再执行了
		}

	}

	// ***********************************************
	// 结束各个状态下entry和do部分做的action声明
	// ***********************************************

	// *************一些辅助方法***************************

	/**
	 * 根据收到的来自subElement的消息内容来设定父目标所记录的子目标的状态
	 * 
	 * @param subElementName
	 *            subElement的名称，即sender部分
	 * @param message
	 *            来自subElement的消息的内容，即body部分
	 */
	private void setSubElementRecordedState(String subElementName,
			String message) {
		for (Element sub : this.getSubElements()) {
			if (sub.getName().equals(subElementName)) {
				switch (message) {
				case "ACTIVATEDDONE":
					sub.setRecordedState(1); // 子目标已激活
					break;
				case "ACHIEVEDDONE":
					sub.setRecordedState(5); // 子目标已完成
					break;
				default:
					break;
				}
			}
		}

	}

	// *************结束一些辅助方法************************

	// ***********************************************
	// 下面的方法都是需要新建一个GoalMachine实例时根据具体要求实现的
	// 主要做的是在各个状态上需要执行的Action，以及各个状态相关的条件检查
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

	public int getDecomposition() {
		return decomposition;
	}

	public int getSchedulerMethod() {
		return schedulerMethod;
	}

	public boolean isFinished() {
		return isFinished;
	}

	public void setFinished(boolean isFinished) {
		this.isFinished = isFinished;
	}

	public int getState() {
		return this.state;
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

	public void setParentGoal(GoalMachine goal) {
		this.parentGoal = goal;
	}

	public GoalMachine getParentGoal() {
		return this.parentGoal;
	}

	public boolean isBehaviourDone() {
		return isBehaviourDone;
	}

	public void setBehaviourDone(boolean isBehaviourDone) {
		this.isBehaviourDone = isBehaviourDone;
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
