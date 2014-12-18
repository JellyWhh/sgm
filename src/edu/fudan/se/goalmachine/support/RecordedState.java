/**
 * 
 */
package edu.fudan.se.goalmachine.support;

/**
 * 让父目标用来记录子目标状态的枚举类型
 * 
 * @author whh
 *
 */
public enum RecordedState {
	Initial, ActivatedFailed, Activated, Executing, Failed, Achieved;
}
