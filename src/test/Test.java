/**
 * 
 */
package test;

import edu.fudan.se.goalmachine.Condition;
import edu.fudan.se.goalmachine.GoalMachine;
import edu.fudan.se.goalmachine.SGMMessage;
import edu.fudan.se.goalmachine.State;
import edu.fudan.se.goalmachine.TaskMachine;

/**
 * @author whh
 *
 */
public class Test {

	/**
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException {

		GoalMachine parentAlice = new GoalMachine("parentAlice", 1, 0, null) {

			@Override
			public State doRepairing(Condition condition) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void checkPreCondition() {
				// TODO Auto-generated method stub

			}

			@Override
			public void checkPostCondition() {
				// TODO Auto-generated method stub

			}

			@Override
			public void checkInvariantCondition() {
				// TODO Auto-generated method stub

			}

			@Override
			public void checkContextCondition() {
				// TODO Auto-generated method stub

			}

			@Override
			public void checkCommitmentCondition() {
				// TODO Auto-generated method stub

			}
		};

		TaskMachine childBob = new TaskMachine("childBob", parentAlice) {

			@Override
			public State doRepairing(Condition condition) {
				// TODO Auto-generated method stub
				if (condition.getType().equals("CONTEXT")) {
					this.getContextCondition().setSatisfied(true);
					return State.Activated;
				} else {
					return null;
				}
			}

			@Override
			public void checkPreCondition() {
				// TODO Auto-generated method stub

			}

			@Override
			public void checkPostCondition() {
				// TODO Auto-generated method stub

			}

			@Override
			public void checkInvariantCondition() {
				// TODO Auto-generated method stub

			}

			@Override
			public void checkContextCondition() {
				// TODO Auto-generated method stub
				if (true) {
					this.getContextCondition().setSatisfied(false);
				}
			}

			@Override
			public void checkCommitmentCondition() {
				// TODO Auto-generated method stub

			}

			@Override
			public void executingDo() {
				System.out
						.println("childBod is doing his executingDoAction...");
				this.executingDo_waitingEnd();

			}
		};

		parentAlice.addSubElement(childBob, 1);

		//
		// ExecutorService service = Executors.newCachedThreadPool();
		// service.execute(parentAlice);
		// service.execute(childBob);

		Thread parent = new Thread(parentAlice);
		Thread child = new Thread(childBob);

		parent.start();
		child.start();

		Thread.sleep(10 * 1000);
		SGMMessage msg = new SGMMessage("TOROOT", null, parentAlice.getName(),
				"ACTIVATE");
		if (parentAlice.getMsgPool().offer(msg)) {
			System.out
					.println("Main thread sends an ACTIVATE msg to parentAlice.");
		}

		Thread.sleep(30 * 1000);

		SGMMessage msg2 = new SGMMessage("TOROOT", null, childBob.getName(),
				"END");
		if (childBob.getMsgPool().offer(msg2)) {
			System.out.println("Main thread sends a END msg to childBob.");
		}
	}

}
