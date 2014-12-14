/**
 * 
 */
package edu.fudan.se.goalmachine;

/**
 * <code>ElementMachine中用到的状态</code>
 * 
 * @author whh
 *
 */
public enum State {
	Initial(0), Activated(1), Executing(2), Waiting(3), Suspended(4), Repairing(
			5), ProgressChecking(6), Failed(7), Achieved(8);

	private int id;

	State(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

}
