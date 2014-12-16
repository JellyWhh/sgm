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

		GoalMachine root = new GoalMachine("root", 0, 1, null) {

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

		GoalMachine alice = new GoalMachine("alice", 0, 0, root) {

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
		GoalMachine bob = new GoalMachine("bob", 1, -1, root) {

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

		TaskMachine aliceChild_1 = new TaskMachine("aliceChild_1", alice) {

			@Override
			public State doRepairing(Condition condition) {
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
						.println("aliceChild_1 is doing his executingDoAction...");
				this.executingDo_waitingEnd();
			}
		};

		TaskMachine aliceChild_2 = new TaskMachine("aliceChild_2", alice) {

			@Override
			public State doRepairing(Condition condition) {
				if (condition.getType().equals("CONTEXT")) {
					this.getContextCondition().setSatisfied(false);
					return State.Failed; // 修复不成功，直接进入failed状态
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
						.println("aliceChild_2 is doing his executingDoAction...");
				this.executingDo_waitingEnd();

			}
		};

		TaskMachine bobChild_1 = new TaskMachine("bobChild_1", bob) {

			@Override
			public State doRepairing(Condition condition) {
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
						.println("bobChild_1 is doing his executingDoAction...");
				this.executingDo_waitingEnd();

			}
		};

		TaskMachine bobChild_2 = new TaskMachine("bobChild_2", bob) {

			@Override
			public State doRepairing(Condition condition) {
				if (condition.getType().equals("CONTEXT")) {
					this.getContextCondition().setSatisfied(true);
					return State.Activated;
				} else {
					return null;
				}
			}

			@Override
			public void checkPreCondition() {
				if (true) {
					this.getPreCondition().setSatisfied(false);
				}

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
						.println("bobChild_2 is doing his executingDoAction...");
				this.executingDo_waitingEnd();

			}
		};
		TaskMachine bobChild_3 = new TaskMachine("bobChild_3", bob) {

			@Override
			public State doRepairing(Condition condition) {
				if (condition.getType().equals("CONTEXT")) {
					this.getContextCondition().setSatisfied(true);
					return State.Activated; // 修复不成功，直接进入failed状态
				} else if (condition.getType().equals("POST")) {
					// post condition不满足，并且修复失败
					this.getPostCondition().setSatisfied(false);
					return State.Failed;
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
				if (true) {
					this.getPostCondition().setSatisfied(false);
				}

			}

			@Override
			public void checkInvariantCondition() {
				// TODO Auto-generated method stub

			}

			@Override
			public void checkContextCondition() {
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
						.println("bobChild_3 is doing his executingDoAction...");
				this.executingDo_waitingEnd();
			}
		};
		aliceChild_1.setContextCondition(new Condition("CONTEXT"));
		// aliceChild_2.setContextCondition(new Condition("CONTEXT"));
		bobChild_2.setPreCondition(new Condition("PRE"));
		bobChild_3.setContextCondition(new Condition("CONTEXT"));
		bobChild_3.setPostCondition(new Condition("POST"));

		root.addSubElement(alice, 1);
		root.addSubElement(bob, 1);

		alice.addSubElement(aliceChild_1, 1);
		alice.addSubElement(aliceChild_2, 1);

		bob.addSubElement(bobChild_1, 1);
		bob.addSubElement(bobChild_2, 2);
		bob.addSubElement(bobChild_3, 3);

		Thread rootThread = new Thread(root);
		Thread aliceThread = new Thread(alice);
		Thread bobThread = new Thread(bob);
		Thread aliceChild_1Thread = new Thread(aliceChild_1);
		Thread aliceChild_2Thread = new Thread(aliceChild_2);
		Thread bobChild_1Thread = new Thread(bobChild_1);
		Thread bobChild_2Thread = new Thread(bobChild_2);
		Thread bobChild_3Thread = new Thread(bobChild_3);

		// for(ElementMachine element:bob.getSubElements()){
		// System.out.println(element.getName() + ": " +
		// element.getPriorityLevel());
		// }

		//
		// ExecutorService service = Executors.newCachedThreadPool();
		// service.execute(parentAlice);
		// service.execute(childBob);
		rootThread.start();
		aliceThread.start();
		bobThread.start();
		aliceChild_1Thread.start();
		aliceChild_2Thread.start();
		bobChild_1Thread.start();
		bobChild_2Thread.start();
		bobChild_3Thread.start();

		Thread.sleep(5 * 1000);
		SGMMessage msg = new SGMMessage("TOROOT", null, root.getName(),
				"ACTIVATE");
		if (root.getMsgPool().offer(msg)) {
			System.out
					.println("Main thread sends an ACTIVATE msg to parentAlice.");
		}

		Thread.sleep(30 * 1000);

		// SGMMessage msg3 = new SGMMessage("TOROOT", null, root.getName(),
		// "SUSPEND");
		// if (root.getMsgPool().offer(msg3)) {
		// System.out
		// .println("Main thread sends an SUSPEND msg to parentAlice.");
		// }
		//
		// Thread.sleep(5 * 1000);
		//
		// SGMMessage msg4 = new SGMMessage("TOROOT", null, root.getName(),
		// "RESUME");
		// if (root.getMsgPool().offer(msg4)) {
		// System.out
		// .println("Main thread sends an RESUME msg to parentAlice.");
		// }
		//
		// Thread.sleep(15 * 1000);

		SGMMessage aliceChild1End = new SGMMessage("TOROOT", null,
				aliceChild_1.getName(), "END");
		if (aliceChild_1.getMsgPool().offer(aliceChild1End)) {
			System.out.println("Main thread sends a END msg to aliceChild_1.");
		}

		Thread.sleep(5 * 1000);

		SGMMessage aliceChild2End = new SGMMessage("TOROOT", null,
				aliceChild_2.getName(), "END");
		if (aliceChild_2.getMsgPool().offer(aliceChild2End)) {
			System.out.println("Main thread sends a END msg to aliceChild_2.");
		}

		Thread.sleep(30 * 1000);
		
		SGMMessage bobChild3End = new SGMMessage("TOROOT", null,
				bobChild_3.getName(), "END");
		if (bobChild_3.getMsgPool().offer(bobChild3End)) {
			System.out.println("Main thread sends a END msg to bobChild_3.");
		}
		
		Thread.sleep(20 * 1000);
		
		SGMMessage bobChild2End = new SGMMessage("TOROOT", null,
				bobChild_2.getName(), "END");
		if (bobChild_2.getMsgPool().offer(bobChild2End)) {
			System.out.println("Main thread sends a END msg to bobChild_2.");
		}
		
		Thread.sleep(10 * 1000);
		
		SGMMessage bobChild1End = new SGMMessage("TOROOT", null,
				bobChild_1.getName(), "END");
		if (bobChild_1.getMsgPool().offer(bobChild1End)) {
			System.out.println("Main thread sends a END msg to bobChild_1.");
		}

	}

}
