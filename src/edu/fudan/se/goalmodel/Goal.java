/**
 * 
 */
package edu.fudan.se.goalmodel;

import java.util.ArrayList;

import edu.fudan.se.message.SGMMessagePool;

/**
 * 目标数据结构
 * 它与<code>Task</code>的区别是，<code>Task</code>不能够再有subElement
 * 
 * @author whh
 *
 */
public class Goal extends Element {

	private int state; // 取值0(initial),1(activated),2(executing),3(achieved),4(failed),5(repairing),6(finished)
	private Goal parentGoal;
	private ArrayList<Element> subElements = new ArrayList<>(); // subElements

	

	private boolean isFinished; // 标识整个目标状态机是否完成
	private boolean isBehaviourDone = false; // 标识一个行为是否结束，true表示结束

	private Boolean contextCondition = null; // 上下文条件，如果不满足，这个goal无意义
	private Boolean activatedCondition = null; // 激活条件，指一个承诺被满足，同时，激活条件一旦被满足，意味着一个承诺条件被激活
	private Boolean preCondition = null; // 决定目标什么时候可以被执行，但是preCondition被满足不一定意味着goal开始真的被执行，但preCondition不满足则意味着goal一定不能开始被执行
	private Boolean triggerCondition = null; // 判断triggerCondition是否被满足，true为被满足，为null表示不需要这个条件
	private Boolean postCondition = null; // 检测goal是否达成，满足这个条件，才表示goal成功达成

	/**
	 * 构造方法
	 * 
	 * @param name
	 *            目标的名字
	 * @param msgQueue
	 *            目标相关联的消息队列，所有目标共享一个消息队列，即消息池
	 */
	public Goal(String name) {
		super(name);
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

	public void setParentGoal(Goal goal) {
		this.parentGoal = goal;
	}

	public Goal getParentGoal() {
		return this.parentGoal;
	}

	public boolean isBehaviourDone() {
		return isBehaviourDone;
	}

	public void setBehaviourDone(boolean isBehaviourDone) {
		this.isBehaviourDone = isBehaviourDone;
	}

	public Boolean getTriggerCondition() {
		return triggerCondition;
	}

	public void setTriggerCondition(Boolean triggerCondition) {
		this.triggerCondition = triggerCondition;
	}

	public Boolean getContextCondition() {
		return contextCondition;
	}

	public void setContextCondition(Boolean contextCondition) {
		this.contextCondition = contextCondition;
	}

	public Boolean getActivatedCondition() {
		return activatedCondition;
	}

	public void setActivatedCondition(Boolean activatedCondition) {
		this.activatedCondition = activatedCondition;
	}

	public Boolean getPreCondition() {
		return preCondition;
	}

	public void setPreCondition(Boolean preCondition) {
		this.preCondition = preCondition;
	}

	public Boolean getPostCondition() {
		return postCondition;
	}

	public void setPostCondition(Boolean postCondition) {
		this.postCondition = postCondition;
	}

}
