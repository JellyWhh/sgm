/**
 * 
 */
package edu.fudan.se.goalmodel;

/**
 * @author whh
 *
 */
public abstract class Element {
	
	protected String name;	//element的名字
	
	public Element(String name){
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	

}
