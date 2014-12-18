/**
 * 
 */
package edu.fudan.se.goalmachine.support;

/**
 * 导致跳转到repairing状态的原因类型
 * 
 * @author whh
 *
 */
public enum CauseToRepairing {
	InvViolated, CcViolated, SubFail, PreCondition, PostCondition,DefaultPreCondition
}
