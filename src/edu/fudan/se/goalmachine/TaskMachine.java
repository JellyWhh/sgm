/**
 * 
 */
package edu.fudan.se.goalmachine;

import java.util.Date;

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

	@Override
	public void run() {

		this.setCurrentState(State.Initial); // 刚开始是目标状态是initial状态
		this.setStartTime(new Date()); // 设置目标状态机开始运行时间为当前时间

		Log.logDebug(this.name, "run()", "TaskMachine start! Start time is: "
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
			if (!isActivatedEntryDone) { // 刚进入激活状态，尝试把自己状态转换为激活

				activatedEntry();
				isActivatedEntryDone = true; // 设置为true表示这个entry动作做完了，以后不会再执行了

			} else { // 自己激活后就等待父目标的Start指令
				this.activateDo_waitingParentNotify();
			}

			break;
		case Executing: // executing
			// TODO 要进行commitment condition和invariant condition的检查
			// 开始具体的taks执行，是抽象方法，在实例时实现
			executingDo();

			break;
		case Waiting:// waiting
			break;
		case Suspended: // suspened
			break;
		case Repairing: // repairing
			break;
		case ProgressChecking: // progressChecking
			break;
		case Failed:// failed
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

	/**
	 * activated状态中entry所做的action：尝试把自己状态转换为激活
	 */
	private void activatedEntry() {
		Log.logDebug(this.getName(), "activatedEntry()", "init.");

		this.setCurrentState(this.transition(State.Initial,
				this.getContextCondition()));
	}

	/**
	 * executing状态中do所做的action：这个需要根据具体的task有不同的具体执行行为，所以这个是抽象方法，在实例化时具体实现
	 */
	public abstract void executingDo();

}
