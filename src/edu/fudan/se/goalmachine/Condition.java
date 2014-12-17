/**
 * 
 */
package edu.fudan.se.goalmachine;

/**
 * 状态机中需要检查的条件Condition,具体类型有：CONTEXT,PRE,POST,COMMITMENT,INVARIANT
 * 
 * @author whh
 *
 */
public class Condition {

	private String type; // 条件类型，具体有CONTEXT,PRE,POST,COMMITMENT,INVARIANT
	private boolean satisfied = true; // 条件是否被满足，true为被满足
	private boolean canRepairing; // 标志这个条件是否可通过主动做一些事来修复，使之满足，只针对PreCondition

	public Condition(String type) {
		this.type = type;
	}
	public Condition(String type, boolean canPrepairing) {
		this.type = type;
		this.canRepairing = canPrepairing;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isSatisfied() {
		return satisfied;
	}

	public void setSatisfied(boolean satisfied) {
		this.satisfied = satisfied;
	}

	public boolean isCanRepairing() {
		return canRepairing;
	}

	public void setCanRepairing(boolean canRepairing) {
		this.canRepairing = canRepairing;
	}
}
