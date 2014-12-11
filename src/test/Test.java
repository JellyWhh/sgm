/**
 * 
 */
package test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.fudan.se.goalmodel.Element;
import edu.fudan.se.machine.GoalMachine;
import edu.fudan.se.message.SGMMessage;
import edu.fudan.se.message.SGMMessagePool;

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
		// TODO Auto-generated method stub
		SGMMessagePool msgPool = new SGMMessagePool();

		GoalMachine parent = new GoalMachine("parentBob", msgPool) {

			@Override
			public void doInitialBehaviour() {
				System.out
						.println("Bob is doing initial behaviour! And then change state to activated!");
				setState(1);
			}

			@Override
			public void doActivatedBehaviour() {
				System.out
						.println("Bob is doing activated behaviour! And then change state to executing!");
				setState(2);
			}

			@Override
			public void doExecutingBehaviour() {
				System.out
						.println("Bob is doing executing behaviour! He will send a message to all his children!");
				if (getSubElements() != null) {
					for (Element element : getSubElements()) {
						SGMMessage msg = new SGMMessage("TOSUBM",
								this.getName(), element.getName(),
								"STATECHANGETO:1");
						if (!msgPool.offer(msg)) {
							System.err.println("send msg failed!");
						}
					}
				}

				setBehaviourDone(true);

			}

			@Override
			public void doAchievedBehaviour() {
				System.out
						.println("Bob is doing achieved behaviour! And then change state to finished!");
				 setState(6);

				
			}

			@Override
			public void doFailedBehaviour() {
				// TODO Auto-generated method stub

			}

			@Override
			public void doRepairingBehaviour() {
				// TODO Auto-generated method stub

			}

			@Override
			public void doFinishedBehaviour() {
				System.out.println("Bob is doing finished behaviour!");
				this.stop();

				setBehaviourDone(true);
			}

			@Override
			public void checkTriggerCondition() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void checkContextCondition() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void checkActivatedCondition() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void checkPreCondition() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void checkPostCondition() {
				// TODO Auto-generated method stub
				
			}
		};
		GoalMachine child = new GoalMachine("childAlice", msgPool) {

			@Override
			public void doInitialBehaviour() {
				System.out.println("Alice is doing initial behaviour!");

				setBehaviourDone(true);
			}

			@Override
			public void doActivatedBehaviour() {
				System.out
						.println("Alice is doing activated behaviour! And then change state to executing!");
				setState(2);
			}

			@Override
			public void doExecutingBehaviour() {
				System.out
						.println("Alice is doing executing behaviour! And then change state to achieved!");
				setState(3);
			}

			@Override
			public void doAchievedBehaviour() {
				System.out
						.println("Alice is doing achieved behaviour! And then change state to finished!");
				setState(6);
			}

			@Override
			public void doFailedBehaviour() {
				// TODO Auto-generated method stub

			}

			@Override
			public void doRepairingBehaviour() {
				// TODO Auto-generated method stub

			}

			@Override
			public void doFinishedBehaviour() {
				System.out
						.println("Alice has finished her behaviours! And she send a message to her parent!");

				if (getParentGoal() != null) {
					SGMMessage msg = new SGMMessage("TOPARENTM",
							this.getName(), getParentGoal().getName(),
							"STATECHANGETO:3");
					if (!msgPool.offer(msg)) {
						System.err.println("send msg failed!");
					}
				}
				this.stop();

				setBehaviourDone(true);
			}

			@Override
			public void checkTriggerCondition() {
				System.out.println("Alice is checking triggerCondition...");
				setTriggerCondition(true);
			}

			@Override
			public void checkContextCondition() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void checkActivatedCondition() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void checkPreCondition() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void checkPostCondition() {
				System.out.println("Alice is checking postCondition...");
				setPostCondition(true);
			}

		};
		child.setTriggerCondition(false);
		child.setPostCondition(false);
		
		parent.addSubElement(child);
		child.setParentGoal(parent);

		ExecutorService service = Executors.newCachedThreadPool();
		service.execute(parent);
		service.execute(child);

	}

}
