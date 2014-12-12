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
	boolean satisfied; // 条件是否被满足，true为被满足
	
	public Condition(String type){
		this.type = type;
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
}
