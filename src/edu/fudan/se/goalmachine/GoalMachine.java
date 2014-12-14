/**
 * 
 */
package edu.fudan.se.goalmachine;

import java.util.ArrayList;
import java.util.Date;

import edu.fudan.se.log.Log;

/**
 * 目标数据结构 它与<code>Task</code>的区别是，<code>Task</code>不能够再有subElement
 * 
 * @author whh
 *
 */
public abstract class GoalMachine extends ElementMachine {

	private ArrayList<ElementMachine> subElements = new ArrayList<>(); // subElements

	private int decomposition; // 分解，0表示AND分解，1表示OR分解
	private int schedulerMethod; // AND分解情况下的子目标执行顺序，0表示并行处理，1表示串行


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
			ElementMachine parentGoal) {
		super(name, parentGoal);
		this.decomposition = decomposition;
		this.schedulerMethod = schedulerMethod;
	}

	@Override
	public void run() {

		this.setCurrentState(State.Initial); // 刚开始是目标状态是initial状态
		this.setStartTime(new Date()); // 设置目标状态机开始运行时间为当前时间

		Log.logDebug(this.name, "run()", "GoalMachine start! Start time is: "
				+ this.getStartTime().toString());

		this.setFinish(false);
		while (!this.isFinish()) {
			// 如果contextCondition为空，表示没有设置上下文条件，这个goal是有意义的，可以检出消息，执行行为
			doMainRunningBehaviour();
			// TODO
		}
	}

	/**
	 * 主体持续运行的行为，包括从消息池里检出消息，以及在对应的状态执行相应的行为
	 */
	private void doMainRunningBehaviour() {

		switch (this.getCurrentState()) {
		case Initial: // initial
			if (isInitialEntryDone) { // 如果完成了entry动作，循环执行do动作
				this.initialDo();
			} else {
				this.initialEntry(); // entry动作只会执行一次
				isInitialEntryDone = true; // 设置为true表示这个entry动作做完了，以后不会再执行了
			}

			break;
		case Activated: // activated
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
		case Executing: // executing
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
		case Waiting:// waiting
			waitingDo();

			break;
		case Suspended: // suspened
			break;
		case Repairing: // repairing
			break;
		case ProgressChecking: // progressChecking
			break;
		case Failed:// failed
			if (isFailedEntryDone) {
				failedDo();
			} else {
				failedEntry();
			}

			break;
		case Achieved:// achieved
			if (isAchievedEntryDone) {

			} else {
				this.achievedEntry();
				isAchievedEntryDone = true; // 设置为true表示这个entry动作做完了，以后不会再执行了
			}

			break;

		}
	}


	
	// ***********************************************
	// 下面的方法都是在各个状态下entry和do部分做的action
	// ***********************************************



	/**
	 * activated状态中entry所做的action：给所有subElement发ACTIVATE消息，让其激活
	 */
	private void activatedEntry() {
		Log.logDebug(this.getName(), "activatedEntry()", "init.");

		if (this.getSubElements() != null) {

			if (this.getDecomposition() == 0) { // AND分解，需要激活所有subElements
				for (ElementMachine element : this.getSubElements()) {

					if (sendMessageToSub(element, "ACTIVATE")) { // 发送成功
						Log.logDebug(this.getName(), "activatedEntry()",
								"send \"ACTIVATE\" msg to " + element.getName()
										+ " succeed!");
					} else {
						Log.logError(this.getName(), "activatedEntry()",
								"send ACTIVATE msg to " + element.getName()
										+ " error!");
					}

				}
			} else { // OR分解，激活一个子目标？？？？？？？？？？？？？？？
				// TODO
			}
		} else {
			Log.logError(this.getName(), "activatedEntry()",
					"getSubElements() == null");
		}

		isActivatedEntryDone = true; // 设置为true表示这个entry动作做完了，以后不会再执行了
	}

	/**
	 * activated状态中do所做的action：检查消息队列，看subElement是否已激活，如果subElement已激活，
	 * 表示自己的激活全部完成，可告诉parent开始下一步行动了；这时，目标仍处于activated状态，等待父目标发送START指令。（root
	 * goal除外，root goal要直接发生状态跳转）
	 */
	private void activatedDo_waitingSubReply() {
		Log.logDebug(this.getName(), "activatedDo_waitingSubReply()", "init.");

		SGMMessage msg = this.getMsgPool().poll(); // 每次拿出一条消息
		if (msg != null) {
			Log.logDebug(this.getName(), "activatedDo_waitingSubReply()",
					"get a message from " + msg.getSender() + "; body is: "
							+ msg.getBody());

			// 消息内容是ACTIVATEDDONE，表示发送这条消息的子目标已激活
			if (msg.getBody().equals("ACTIVATEDDONE")) {
				setSubElementRecordedState(msg.getSender(), msg.getBody());
			}

			// 检查是否所有子目标都已激活
			if (this.getDecomposition() == 0) { // AND分解
				int count = 0;
				for (ElementMachine element : this.getSubElements()) {
					if (element.getRecordedState() == 1) { // 激活
						count++;
					}
				}
				if (count == this.getSubElements().size()) { // 全部激活
					// 如果subElement已激活，表示自己的激活全部完成，可告诉parent开始下一步行动了；这时，目标仍处于activated状态，等待父目标发送START指令。
					if (this.getParentGoal() != null) { // 不是root goal， 有parent
														// goal
						if (this.sendMessageToParent("ACTIVATEDDONE")) {
							Log.logDebug(this.getName(),
									"activatedDo_waitingSubReply()",
									"send ACTIVATEDDONE msg to parent.");
							isActivatedDo_waitingSubReplyDone = true;
						} else {
							Log.logError(this.getName(),
									"activatedDo_waitingSubReply()",
									"send msg to parent error!");
						}

					} else { // 自己本身是root goal，无需等待父目标发送START指令，直接发生跳转
						this.setCurrentState(this.transition(State.Activated,
								this.getPreCondition()));
					}
				}
			} else { // OR分解
				// TODO
			}
		}
	}

	

	/**
	 * AND分解而且是并行<br>
	 * executing状态中entry所做的action：给所有subElement发START消息，让其开始进入执行Executing状态
	 */
	private void executingEntry_AND_PARALLERL() {
		Log.logDebug(this.getName(), "executingEntry_AND_PARALLERL()", "init.");

		if (getSubElements() != null) {
			for (ElementMachine element : getSubElements()) {

				if (sendMessageToSub(element, "START")) {
					Log.logDebug(this.getName(),
							"executingEntry_AND_PARALLERL()",
							"send START msg to " + element.getName()
									+ " succeed!");
				} else {
					Log.logError(this.getName(),
							"executingEntry_AND_PARALLERL()",
							"send START msg to " + element.getName()
									+ " error!");
				}

			}
		} else {
			Log.logError(this.getName(), "executingEntry_AND_PARALLERL()",
					"getSubElements() == null.");

		}

		isExecutingEntryDone = true; // 设置为true表示这个entry动作做完了，以后不会再执行了
	}

	/**
	 * AND分解而且是并行<br>
	 * executing状态中do所做的action：等待所有subElements反馈消息ACHIEVED，必须是所有的子目标都反馈完成，
	 * 如果所有子目标都完成了，自己可以尝试发生跳转到achieved
	 */
	private void executingDo_waitingSubReply_AND_PARALLERL() {
		Log.logDebug(this.getName(),
				"executingDo_waitingSubReply_AND_PARALLERL()", "init.");

		SGMMessage msg = this.getMsgPool().poll(); // 拿出一条消息
		if (msg != null) {
			Log.logDebug(this.getName(),
					"executingDo_waitingSubReply_AND_PARALLERL()",
					"get a message from " + msg.getSender() + "; body is: "
							+ msg.getBody());
			// 如果子目标反馈的是ACHIEVED
			if (msg.getBody().equals("ACHIEVEDDONE")) {
				setSubElementRecordedState(msg.getSender(), msg.getBody());
			}

			// 检查是否全部已完成
			int count = 0;
			for (ElementMachine element : this.getSubElements()) {
				if (element.getRecordedState() == 5) { // achieved
					count++;
				}
			}
			if (count == this.getSubElements().size()) { // 全部激活,自己可以尝试发生跳转到achieved
				this.setCurrentState(this.transition(State.Executing,
						this.getPostCondition()));
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
		Log.logDebug(this.getName(), "executingEntry_AND_SERIAL()", "init.");

		if (getSubElements() != null) {

			for (ElementMachine element : getSubElements()) {
				// 找到下一个还不是已完成状态的subElement，给其发送start消息，然后break，跳出循环
				if (element.getRecordedState() != 5) { // 5表示是完成状态

					if (sendMessageToSub(element, "START")) {
						Log.logError(this.getName(),
								"executingEntry_AND_SERIAL()",
								"send START msg to " + element.getName()
										+ "succeed!");
						break;
					} else {
						Log.logError(this.getName(),
								"executingEntry_AND_SERIAL()",
								"send START msg to " + element.getName()
										+ "error!");
					}

				}
			}
		} else {
			Log.logError(this.getName(), "executingEntry_AND_SERIAL()",
					"getSubElements() == null.");
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
		Log.logDebug(this.getName(),
				"executingDo_waitingSubReply_AND_SERIAL()", "init.");

		SGMMessage msg = this.getMsgPool().poll(); // 拿出一条消息
		if (msg != null) { // 收到了一条消息
			Log.logDebug(this.getName(),
					"executingDo_waitingSubReply_AND_SERIAL()",
					"get a message from " + msg.getSender() + "; body is: "
							+ msg.getBody());
			// 如果子目标反馈的是ACHIEVED
			if (msg.getBody().equals("ACHIEVEDDONE")) {
				setSubElementRecordedState(msg.getSender(), msg.getBody());
			}

			// 检查是不是所有的都已完成
			int count = 0;
			for (ElementMachine element : this.getSubElements()) {
				if (element.getRecordedState() == 5) { // achieved
					count++;
				}
			}
			if (count == this.getSubElements().size()) { // 全部激活,自己可以尝试发生跳转到achieved
				this.setCurrentState(this.transition(State.Executing,
						this.getPostCondition()));
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


	// ***********************************************
	// 结束各个状态下entry和do部分做的action声明
	// ***********************************************

	// *************一些辅助方法***************************

	/**
	 * 发送消息给subElement
	 * 
	 * @param sub
	 *            subElement
	 * @param body
	 *            消息body部分
	 * @return true 发送成功, false 发送失败
	 */
	private boolean sendMessageToSub(ElementMachine sub, String body) {
		SGMMessage msg = new SGMMessage("TOSUB", this.getName(), sub.getName(),
				body);
		if (sub.getMsgPool().offer(msg)) {
			Log.logMessage(msg, true);
			return true;
		} else {
			Log.logMessage(msg, false);
			return false;
		}
	}

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
		for (ElementMachine sub : this.getSubElements()) {
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

	public int getDecomposition() {
		return decomposition;
	}

	public int getSchedulerMethod() {
		return schedulerMethod;
	}

	public void addSubElement(ElementMachine element) {
		this.subElements.add(element);
	}

	public ArrayList<ElementMachine> getSubElements() {
		return this.subElements;
	}

}
