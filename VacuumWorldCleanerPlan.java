package uk.ac.rhul.cs.dice.vacuumworld.agents.minds;

import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;

import uk.ac.rhul.cs.dice.gawl.interfaces.actions.EnvironmentalAction;
import uk.ac.rhul.cs.dice.vacuumworld.agents.minds.manhattan.PlanCodes;

public class VacuumWorldCleanerPlan {

	private Queue<Class<? extends EnvironmentalAction>> actionsToPerform;
	private Class<? extends EnvironmentalAction> lastAction;
	private int numberOfConsecutiveFailuresOfTheSameAction;
	private PlanCodes planCodes;

	
	public VacuumWorldCleanerPlan() {
		this.actionsToPerform = new LinkedTransferQueue<>();
		this.numberOfConsecutiveFailuresOfTheSameAction = 0;
		this.lastAction = null;
		this.planCodes = PlanCodes.getInstance();

	}
	
	public PlanCodes getPlanCodes() {
		return this.planCodes;
	}
	
	// Returns the set of actions that still need to be performed.
	
	public Queue<Class<? extends EnvironmentalAction>> getActionsToPerform() {
		return this.actionsToPerform;
	}
	
	// Set the list of actions that are required to be performed
	
	public void setActionsToPerform(Queue<Class<? extends EnvironmentalAction>> actionsToPerform) {
		this.actionsToPerform = actionsToPerform;
	}
	
	// clears the list of actions from all enviromental actions.
	
	public void clearActionsToPerform() {
		this.actionsToPerform.clear();
	}
	
	// Adds a new actions to perform at the end of the queue.
	
	public void pushActionToPerform(Class<? extends EnvironmentalAction> actionToPerform) {
		//VWUtils.logWithClass(this.getClass().getSimpleName(), VWUtils.ACTOR + agentId + ": adding " + actionToPerform.getSimpleName() + " to plan...");
		this.actionsToPerform.add(actionToPerform);
	}
	
	// Gets the next action to perform from the action queue.
	
	public Class<? extends EnvironmentalAction> pullActionToPerform() {
		Class<? extends EnvironmentalAction> selected = this.actionsToPerform.poll();
		//VWUtils.logWithClass(this.getClass().getSimpleName(), VWUtils.ACTOR + agentId + ": selecting " + selected.getSimpleName() + " from plan...");
		
		return selected;
	}
	
	public Class<? extends EnvironmentalAction> getLastAction() {
		return this.lastAction;
	}

	public void setLastAction(Class<? extends EnvironmentalAction> lastAction) {
		this.lastAction = lastAction;
	}
	

	public int getNumberOfConsecutiveFailuresOfTheSameAction() {
		return this.numberOfConsecutiveFailuresOfTheSameAction;
	}

	public void setNumberOfConsecutiveFailuresOfTheSameAction(int numberOfConsecutiveFailuresOfTheSameAction) {
		this.numberOfConsecutiveFailuresOfTheSameAction = numberOfConsecutiveFailuresOfTheSameAction;
	}
	
	public void incrementNumberOfConsecutiveFailuresOfTheSameAction() {
		this.numberOfConsecutiveFailuresOfTheSameAction++;
	}
	
}
