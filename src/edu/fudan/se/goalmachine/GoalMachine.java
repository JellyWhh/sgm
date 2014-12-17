/**
 * 
 */
package edu.fudan.se.goalmachine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import edu.fudan.se.log.Log;

/**
 * Goal Machine， 继承自<code>ElementMachine</code><br>
 * 它与<code>TaskMachine</code>的区别是， <code>TaskMachine</code>不能够再有subElement
 * 
 * @author whh
 *
 */
public abstract class GoalMachine extends ElementMachine {

	private ArrayList<ElementMachine> subElements = new ArrayList<>(); // subElements

	private int decomposition; // 分解，0表示AND分解，1表示OR分解
	private int schedulerMethod; // AND分解情况下的子目标执行顺序，0表示并行处理，1表示串行

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

	// ***********************************************
	// 下面的方法都是在各个状态下entry和do部分做的action
	// ***********************************************

	/**
	 * activated状态中entry所做的action：给所有subElement发ACTIVATE消息，让其激活
	 */
	@Override
	public void activatedEntry() {
		Log.logDebug(this.getName(), "activatedEntry()", "init.");

		if (this.getSubElements() != null) {

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
		} else {
			Log.logError(this.getName(), "activatedEntry()",
					"getSubElements() == null");
		}

	}

	boolean isActivatedDo_waitingSubReplyDone = false;

	/**
	 * 重写了<code>ElementMachine</code>中的activateDo()方法。<br>
	 * 做的action为：等待subElements反馈ACTIVATEDDONE消息中，如果都反馈已激活，进入等待父目标的START指令中。
	 * 如果是root goal，则在activatedDo_waitingSubReply()方法中已发生状态跳转
	 */
	@Override
	public void activateDo() {
		Log.logDebug(this.getName(), "activateDo()", "init.");
		if (isActivatedDo_waitingSubReplyDone) { // subElements都反馈已激活，进入等待父目标的START指令中。如果是root
													// goal，则在activatedDo_waitingSubReply()方法中已发生状态跳转
			// activateDo_waitingParentNotify();
			SGMMessage msg = this.getMsgPool().poll(); // 每次拿出一条消息
			if (msg != null) {
				Log.logDebug(this.getName(), "activateDo()",
						"get a message from " + msg.getSender() + "; body is: "
								+ msg.getBody());

				// 消息内容是START，表示父目标让当前目标开始状态转换
				if (msg.getBody().equals("START")) {
					this.setCurrentState(this.transition(State.Activated,
							this.getPreCondition()));
				}

			}

		} else { // 等待subElements反馈中
			activatedDo_waitingSubReply();
		}
	}

	boolean isSendMesToOneSubDone = false;

	/**
	 * 重写了<code>ElementMachine</code>中的executingEntry()方法。<br>
	 * 做的action为：按照是否是AND分解以及是否是并行来决定给哪些subElements发START消息<br>
	 * 如果是AND、并行：给所有subElements发START消息；<br>
	 * 如果是AND、串行： 给一个不是achieved状态的subElement发START消息；<br>
	 * 如果是OR：按照优先级给其中已激活的subElements发送START消息
	 */
	@Override
	public void executingEntry() {
		Log.logDebug(this.getName(), "executingEntry()", "init.");

		if (this.getDecomposition() == 0) { // AND分解
			if (this.getSchedulerMethod() == 0) { // 并行
				executingEntry_sendMesToAllSub_AND_PARALLERL();

			} else { // 串行
				executingEntryDo_sendMesToOneSub_AND_SERIAL();
			}
		} else { // OR分解
			executingEntryDo_sendMesToOneSub_OR();
		}
	}

	/**
	 * 重写了<code>ElementMachine</code>中的executingDo()方法。<br>
	 * 做的action为：按照是否是AND分解以及是否是并行来决定等待哪些subElements的反馈<br>
	 * 如果是AND、并行：等待所有subElements反馈消息ACHIEVED，必须是所有的子目标都反馈完成，
	 * 如果所有子目标都完成了，自己可以尝试发生跳转到achieved；<br>
	 * 如果是AND、串行：等待subElement反馈完成的消息，得到消息后，把它标记为achieved，
	 * 然后重新进入SendMesToOneSub_AND_SERIAL，给下个未完成状态的subElement发消息
	 * ，如此循环，直到最后一个subElement完成，可以尝试发生跳转；<br>
	 * 如果是OR：
	 */
	@Override
	public void executingDo() {
		Log.logDebug(this.getName(), "executingDo()", "init.");
		if (this.getDecomposition() == 0) { // AND分解
			if (this.getSchedulerMethod() == 0) { // 并行
				executingDo_waitingSubReply_AND_PARALLERL();
			} else { // 串行
				if (isSendMesToOneSubDone) { // 已经给其中一个sub发过消息了，要进入等待反馈状态中
					executingDo_waitingSubReply_AND_SERIAL();
				} else { // 给下一个sub发消息
					executingEntryDo_sendMesToOneSub_AND_SERIAL();
				}
			}
		} else { // OR分解
			if (isSendMesToOneSubDone) { // 已经给其中一个已激活状态的sub发过消息了，进入等待反馈中
				executingDo_waitingSubReply_OR();
			} else {
				executingEntryDo_sendMesToOneSub_OR();
			}
		}

	}

	/**
	 * progressChecking状态中do所做的action：只要收到subElement发来的ACHIEVEDDONE消息就进入这个状态，
	 * 然后检查是不是符合自身进入achieved状态的条件，如果符合，跳转到achieved，如果不符合，跳回到executing状态。<br>
	 * AND分解检查条件：所有的subElements都achieved<br>
	 * OR分解检查条件：只要有一个subElement进入achieved
	 */
	@Override
	public void progressCheckingDo() {
		Log.logDebug(this.getName(), "progressCheckingDo()", "init.");

		if (this.getDecomposition() == 0) { // AND分解
			// 检查是否全部已完成
			int count = 0;
			for (ElementMachine element : this.getSubElements()) {
				if (element.getRecordedState() == State.Achieved) { // achieved
					count++;
				}
			}

			if (this.getSchedulerMethod() == 0) { // 并行
				if (count == this.getSubElements().size()) { // 全部激活,自己可以尝试发生跳转到achieved
					this.setCurrentState(this.transition(
							State.ProgressChecking, this.getPostCondition()));
				} else {
					this.setCurrentState(State.Executing); // 没有全部激活，继续跳回到executing
				}

			} else { // 串行
				if (count == this.getSubElements().size()) { // 全部激活,自己可以尝试发生跳转到achieved
					this.setCurrentState(this.transition(
							State.ProgressChecking, this.getPostCondition()));
				} else {
					isSendMesToOneSubDone = false; // 这样下次循环的时候就会再次去执行executingEntry_AND_SERIAL()
					this.setCurrentState(State.Executing); // 没有全部激活，继续跳回到executing
				}
			}

		} else { // OR分解，因为是收到了ACHIEVEDDONE消息才会进入progressChecking，所以肯定有一个subElement状态是achieved了
			this.setCurrentState(this.transition(State.ProgressChecking,
					this.getPostCondition()));
		}
	}

	/**
	 * suspended状态中entry所做的action：给所有subElements发送SUSPEND消息
	 */
	@Override
	public void suspendedEntry() {
		Log.logDebug(this.getName(), "suspendedEntry()", "init.");
		if (this.getSubElements() != null) {
			for (ElementMachine element : this.getSubElements()) {

				if (this.sendMessageToSub(element, "SUSPEND")) {
					Log.logDebug(this.getName(), "suspendedEntry()",
							"send SUSPEND msg to " + element.getName()
									+ " succeed!");
				} else {
					Log.logError(this.getName(), "suspendedEntry()",
							"send SUSPEND msg to " + element.getName()
									+ " error!");
				}
			}
		}
	}

	/**
	 * suspended状态中do所做的action：目标处于挂起状态时，只需要不断检查是否有RESUME到来即可，如果收到了，
	 * 给所有subElements发送RESUME消息，然后把自己状态转换为executing状态
	 */
	@Override
	public void suspendedDo() {
		Log.logDebug(this.getName(), "suspendedDo()", "init.");
		SGMMessage msg = this.getMsgPool().poll(); // 每次拿出一条消息
		if (msg != null) {
			Log.logDebug(this.getName(), "suspendedDo()", "get a message from "
					+ msg.getSender() + "; body is: " + msg.getBody());
			if (msg.getBody().equals("RESUME")) {
				// 给所有subElements发送RESUME消息
				if (this.getSubElements() != null) {
					for (ElementMachine element : this.getSubElements()) {

						if (this.sendMessageToSub(element, "RESUME")) {
							Log.logDebug(this.getName(), "suspendedEntry()",
									"send RESUME msg to " + element.getName()
											+ " succeed!");
						} else {
							Log.logError(this.getName(), "suspendedEntry()",
									"send RESUME msg to " + element.getName()
											+ " error!");
						}
					}
				}
				// 把自己状态设置为executing
				this.setCurrentState(State.Executing);
			}
		}
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

				if (this.getDecomposition() == 0) { // AND分解，检查是否所有子目标都已激活
					int count = 0;
					for (ElementMachine element : this.getSubElements()) {
						if (element.getRecordedState() == State.Activated) { // 激活
							count++;
						}
					}
					if (count == this.getSubElements().size()) { // 全部激活
						// 如果subElement已激活，表示自己的激活全部完成，可告诉parent开始下一步行动了；这时，目标仍处于activated状态，等待父目标发送START指令。
						if (this.getParentGoal() != null) { // 不是root goal，
															// 有parent
															// goal
							if (this.sendMessageToParent("ACTIVATEDDONE")) {
								Log.logDebug(this.getName(),
										"activatedDo_waitingSubReply()",
										"send ACTIVATEDDONE msg to parent.");
								isActivatedDo_waitingSubReplyDone = true;
							} else {
								Log.logError(this.getName(),
										"activatedDo_waitingSubReply()",
										"send ACTIVATEDDONE msg to parent error!");
							}

						} else { // 自己本身是root goal，无需等待父目标发送START指令，直接发生跳转
							this.setCurrentState(this.transition(
									State.Activated, this.getPreCondition()));
						}
					}
				} else { // OR分解，只要所有的subElements都不是Initial就表示自己的激活完成，可告诉parent开始下一步行动了；这时，目标仍处于activated状态，等待父目标发送START指令。
					int count = 0;
					for (ElementMachine element : this.getSubElements()) {
						if (element.getRecordedState() != State.Initial) { // 不是initial
							count++;
						}
					}
					if (count == this.getSubElements().size()) {// 所有的subElements都不是Initial
						if (this.getParentGoal() != null) { // 不是root
															// goal，有parent
							// goal
							if (this.sendMessageToParent("ACTIVATEDDONE")) {
								Log.logDebug(this.getName(),
										"activatedDo_waitingSubReply()",
										"send ACTIVATEDDONE msg to parent.");
								isActivatedDo_waitingSubReplyDone = true;
							} else {
								Log.logError(this.getName(),
										"activatedDo_waitingSubReply()",
										"send ACTIVATEDDONE msg to parent error!");
							}

						} else { // 自己本身是root goal，无需等待父目标发送START指令，直接发生跳转
							this.setCurrentState(this.transition(
									State.Activated, this.getPreCondition()));
						}
					}

				}
			} else if (msg.getBody().equals("FAILED")) { // 子目标反馈的是FAILED消息
				setSubElementRecordedState(msg.getSender(), msg.getBody());
				if (this.getDecomposition() == 0) { // AND分解，向父目标反馈FAILED，同时自己failed
					// 进入failed状态，在failedEntry()里会向父目标反馈failed消息的
					this.setCurrentState(State.Failed);

				} else { // OR分解
					// 先检查是不是所有的都failed
					int failedCount = 0, noinitialCount = 0;
					for (ElementMachine element : this.getSubElements()) {
						if (element.getRecordedState() != State.Initial) { // 不是initial
							noinitialCount++;
							if (element.getRecordedState() == State.Failed) { // 失败
								failedCount++;
							}
						}

					}
					if (noinitialCount == this.getSubElements().size()) {// 所有的subElements都不是Initial，也就是收到了所有subElements的回复
						if (failedCount == this.getSubElements().size()) { // 全部失败
							// 进入failed状态
							this.setCurrentState(State.Failed);
						} else { // 不是全部失败，表示只要有一个被成功激活，就可以表示自身激活了
							if (this.getParentGoal() != null) { // 不是root
								// goal，有parent
								// goal
								if (this.sendMessageToParent("ACTIVATEDDONE")) {
									Log.logDebug(this.getName(),
											"activatedDo_waitingSubReply()",
											"send ACTIVATEDDONE msg to parent.");
									isActivatedDo_waitingSubReplyDone = true;
								} else {
									Log.logError(this.getName(),
											"activatedDo_waitingSubReply()",
											"send ACTIVATEDDONE msg to parent error!");
								}

							} else { // 自己本身是root goal，无需等待父目标发送START指令，直接发生跳转
								this.setCurrentState(this.transition(
										State.Activated, this.getPreCondition()));
							}
						}
					}

				}
			}
		}
	}

	/**
	 * AND分解而且是并行<br>
	 * executing状态中entry所做的action：给所有subElement发START消息，让其开始进入执行Executing状态
	 */
	private void executingEntry_sendMesToAllSub_AND_PARALLERL() {
		Log.logDebug(this.getName(),
				"executingEntry_sendMesToAllSub_AND_PARALLERL()", "init.");

		if (getSubElements() != null) {
			for (ElementMachine element : getSubElements()) {

				if (sendMessageToSub(element, "START")) {
					Log.logDebug(this.getName(),
							"executingEntry_sendMesToAllSub_AND_PARALLERL()",
							"send START msg to " + element.getName()
									+ " succeed!");
				} else {
					Log.logError(this.getName(),
							"executingEntry_sendMesToAllSub_AND_PARALLERL()",
							"send START msg to " + element.getName()
									+ " error!");
				}

			}
		} else {
			Log.logError(this.getName(),
					"executingEntry_sendMesToAllSub_AND_PARALLERL()",
					"getSubElements() == null.");

		}
	}

	/**
	 * AND分解而且是串行<br>
	 * executing状态中entry所做的action：给subElements中一个状态不是已完成的element发消息
	 */
	private void executingEntryDo_sendMesToOneSub_AND_SERIAL() {
		Log.logDebug(this.getName(),
				"executingEntryDo_sendMesToOneSub_AND_SERIAL()", "init.");

		if (getSubElements() != null) {

			for (ElementMachine element : getSubElements()) {
				// 找到下一个还不是已完成状态的subElement，给其发送start消息，然后break，跳出循环
				if (element.getRecordedState() != State.Achieved) { // 5表示是完成状态

					if (sendMessageToSub(element, "START")) {
						Log.logDebug(
								this.getName(),
								"executingEntryDo_sendMesToOneSub_AND_SERIAL()",
								"send START msg to " + element.getName()
										+ " succeed!");
						break;
					} else {
						Log.logError(
								this.getName(),
								"executingEntryDo_sendMesToOneSub_AND_SERIAL()",
								"send START msg to " + element.getName()
										+ " error!");
					}

				}
			}
		} else {
			Log.logError(this.getName(),
					"executingEntryDo_sendMesToOneSub_AND_SERIAL()",
					"getSubElements() == null.");
		}

		isSendMesToOneSubDone = true;
	}

	/**
	 * OR分解<br>
	 * executing状态中entry所做的action：按照优先级给其中已激活的subElements发送START消息
	 */
	private void executingEntryDo_sendMesToOneSub_OR() {
		Log.logDebug(this.getName(), "executingEntryDo_sendMesToOneSub_OR()",
				"init.");
		if (getSubElements() != null) {
			for (ElementMachine element : getSubElements()) {
				// 找到下一个已激活状态的subElement，给其发送start消息，然后break，跳出循环
				if (element.getRecordedState() == State.Activated) {

					if (sendMessageToSub(element, "START")) {
						Log.logDebug(this.getName(),
								"executingEntryDo_sendMesToOneSub_OR()",
								"send START msg to " + element.getName()
										+ " succeed!");
						break;
					} else {
						Log.logError(this.getName(),
								"executingEntryDo_sendMesToOneSub_OR()",
								"send START msg to " + element.getName()
										+ " error!");
					}
				}
			}
		} else {
			Log.logError(this.getName(),
					"executingEntryDo_sendMesToOneSub_OR()",
					"getSubElements() == null.");
		}
		isSendMesToOneSubDone = true;
	}

	/**
	 * AND分解而且是并行<br>
	 * executing状态中do所做的action：等待所有subElements反馈消息ACHIEVED，必须是所有的子目标都反馈完成，
	 * 如果所有子目标都完成了，自己可以尝试发生跳转到achieved；<br>
	 * 如果得到的反馈消息是FAILED，直接进入failed状态
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
				// 检查是否全部已完成
				this.setCurrentState(State.ProgressChecking);

			} else if (msg.getBody().equals("FAILED")) {
				setSubElementRecordedState(msg.getSender(), msg.getBody());
				this.setCurrentState(State.Failed);
			}

		}

	}

	/**
	 * AND分解而且是串行<br>
	 * executing状态中do所做的action：等待subElement反馈完成的消息，得到消息后，把它标记为achieved，
	 * 然后重新进入SendMesToOneSub_AND_SERIAL，给下个未完成状态的subElement发消息
	 * ，如此循环，直到最后一个subElement完成，可以尝试发生跳转。<br>
	 * 如果得到的反馈消息是FAILED，直接进入failed状态
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
				// 检查是不是所有的都已完成
				this.setCurrentState(State.ProgressChecking);

			} else if (msg.getBody().equals("FAILED")) {
				setSubElementRecordedState(msg.getSender(), msg.getBody());
				this.setCurrentState(State.Failed);
			}

		}

	}

	/**
	 * OR分解<br>
	 * executing状态中do所做的action：已经给一个激活状态的subElement发过START消息了，等待反馈中。<br>
	 * 如果收到的是ACHIEVEDDONE,表示子目标完成了，那么自己也就完成了，尝试发生跳转到achieved；<br>
	 * 如果收到的是WAITING
	 */
	private void executingDo_waitingSubReply_OR() {
		Log.logDebug(this.getName(), "executingDo_waitingSubReply_OR()",
				"init.");

		SGMMessage msg = this.getMsgPool().poll(); // 拿出一条消息
		if (msg != null) { // 收到了一条消息
			Log.logDebug(this.getName(), "executingDo_waitingSubReply_OR()",
					"get a message from " + msg.getSender() + "; body is: "
							+ msg.getBody());

			if (msg.getBody().equals("ACHIEVEDDONE")) { // 如果子目标反馈的是ACHIEVED
				setSubElementRecordedState(msg.getSender(), msg.getBody());
				this.setCurrentState(State.ProgressChecking);
			} else if (msg.getBody().equals("FAILED")) {
				// 收到failed消息后，先检查是否所有的subElements都是failed状态，如果是那么自身直接进入failed状态
				setSubElementRecordedState(msg.getSender(), msg.getBody());
				int count = 0;
				for (ElementMachine element : this.getSubElements()) {
					if (element.getRecordedState() == State.Failed) {
						count++;
					}
				}
				if (count == this.getSubElements().size()) { // 全部是Failed状态
					isSendMesToOneSubDone = true;
					this.setCurrentState(State.Failed); // 进入failed状态
				} else {
					// 重新开始执行executingEntryDo_sendMesToOneSub_OR()，给下个处于激活状态的subElement发送START消息
					isSendMesToOneSubDone = false;
				}
			}

		}
	}

	/**
	 * 停止运行当前machine：另外，要给所有subElements里面不是Failed或者Achieved状态的子目标发送STOP消息
	 */
	public void stopMachine() {
		this.setFinish(true);

		if (this.getSubElements() != null) {
			// 给所有subElements里面不是Failed状态或者Achieved状态，或者是没有记录RecordedState的目标发送STOP消息
			for (ElementMachine element : this.getSubElements()) {
				if ((element.getRecordedState() != State.Failed && element
						.getRecordedState() != State.Achieved)
						|| element.getRecordedState() == null) {
					if (sendMessageToSub(element, "STOP")) {
						Log.logDebug(this.getName(), "stopMachine()",
								"send STOP msg to " + element.getName()
										+ " succeed!");
					} else {
						Log.logError(this.getName(), "stopMachine()",
								"send STOP msg to " + element.getName()
										+ " error!");
					}
				}
			}
		} else {
			Log.logError(this.getName(), "stopMachine()",
					"getSubElements() == null!");
		}
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
					sub.setRecordedState(State.Activated); // 子目标已激活
					break;
				case "ACHIEVEDDONE":
					sub.setRecordedState(State.Achieved); // 子目标已完成
					break;
				case "FAILED":
					sub.setRecordedState(State.Failed); // 子目标失败
					break;
				case "STARTEXECUTING":
					sub.setRecordedState(State.Executing); // 子目标开始执行
					break;
				case "WAITING":
					sub.setRecordedState(State.Waiting); // 子目标waiting中
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

	/**
	 * 为当前目标添加一个subElement
	 * 
	 * @param element
	 *            要添加的subElement
	 * @param priorityLevel
	 *            这个subElement在所有subElements中的优先级，数值越大优先级越高，优先级主要在OR分解中用到
	 */
	public void addSubElement(ElementMachine element, int priorityLevel) {

		element.setPriorityLevel(priorityLevel);
		this.subElements.add(element);
		// 根据优先级对subElements排序，按照优先级从大到小排序
		Collections.sort(this.subElements, new Comparator<ElementMachine>() {

			@Override
			public int compare(ElementMachine e1, ElementMachine e2) {
				if (e1.getPriorityLevel() < e2.getPriorityLevel()) {
					return 1;
				} else if (e1.getPriorityLevel() > e2.getPriorityLevel()) {
					return -1;
				} else {
					return 0;
				}
			}
		});
	}

	public ArrayList<ElementMachine> getSubElements() {
		return this.subElements;
	}

}
