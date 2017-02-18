package uk.ac.rhul.cs.dice.vacuumworld.agents.minds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import uk.ac.rhul.cs.dice.gawl.interfaces.actions.EnvironmentalAction;
import uk.ac.rhul.cs.dice.gawl.interfaces.actions.Result;
import uk.ac.rhul.cs.dice.vacuumworld.actions.CleanAction;
import uk.ac.rhul.cs.dice.vacuumworld.actions.MoveAction;
import uk.ac.rhul.cs.dice.vacuumworld.actions.PerceiveAction;
import uk.ac.rhul.cs.dice.vacuumworld.actions.TurnLeftAction;
import uk.ac.rhul.cs.dice.vacuumworld.actions.TurnRightAction;
import uk.ac.rhul.cs.dice.vacuumworld.actions.result.VacuumWorldSpeechActionResult;
import uk.ac.rhul.cs.dice.vacuumworld.agents.ActorFacingDirection;
import uk.ac.rhul.cs.dice.vacuumworld.common.VWPerception;
import uk.ac.rhul.cs.dice.vacuumworld.environment.VacuumWorldCoordinates;
import uk.ac.rhul.cs.dice.vacuumworld.environment.VacuumWorldLocation;
import uk.ac.rhul.cs.dice.vacuumworld.utils.Pair;


public class VacuumWorldOrangeMind extends VacuumWorldDefaultMind{

private ArrayList<VacuumWorldCoordinates> uncleanedDirt = new ArrayList<>();	
private VacuumWorldCleanerPlan cleanerPlan = new VacuumWorldCleanerPlan();
private boolean cleanUsersDirt = true;
private int count = 0;

	public VacuumWorldOrangeMind(String bodyId) {
		super(bodyId);
	}

	@Override
	public EnvironmentalAction decide(Object... parameters) {
		VWPerception perception = getPerception();
		List<Result> receivedCommunications = getReceivedCommunications();
		
		if(receivedCommunications != null) {
			updateCommunications(receivedCommunications);
		}
		
		if (perception != null) {
			updateAvailableActions(perception);
			updateDirtInPercetion(perception);
			if (this.count == 0) {
				perceieveBehindAgent();
				System.out.println("!!!!!!!!!!!!!!!!!!!!!!!It should go back now!!!!");
				this.count++;
			}
			return planSelector(perception);		    
		} else {
			return buildPhysicalAction(PerceiveAction.class);
		}
	}
	/**
	 * This method is used to determine what plan the agent should execute, it
	 * is seperated into priority order, being if there is dirt, clean it immediately
	 * if there is a plan in motion, completely it
	 * if there is a user, follow them
	 * if there no user, but dirty location to clean, then clean
	 * otherwise just move randomly.
	 * @param perception
	 * @return returns the actions which should be performed.
	 */
	public EnvironmentalAction planSelector(VWPerception perception) {
		
		if(perception.canCurrentActorCleanOnHisCurrentLocation()) {
			this.uncleanedDirt.remove(perception.getCurrentActorLocation());	//removes dirt from the dirt list
	    	return buildPhysicalAction(CleanAction.class);
		} if(!this.cleanerPlan.getActionsToPerform().isEmpty()) {		// This agent will always finish a plan before starting a new one.
			return buildPhysicalAction(this.cleanerPlan.pullActionToPerform());
		} else if (isUserInPerception(perception) && this.cleanUsersDirt) {
			this.cleanUsersDirt = false;
			VacuumWorldCoordinates userLocation = ifUserGetCoordinates(perception);
			buildFollowPlan(perception, userLocation);
			
		} else if (!this.uncleanedDirt.isEmpty()) {
			VacuumWorldCoordinates closestDirt = findNearestCompatibleDirt(perception);
			this.cleanUsersDirt = true;
			buildCleanPlan(perception, closestDirt);
		}
		
		return decideActionRandomly();
		
	}
	
	/**
	 * 
	 * @param receivedCommunications
	 */
	
	public void updateCommunications(List<Result> receivedCommunications) {
			
			for(Result result : receivedCommunications) {
				try {
					VacuumWorldSpeechActionResult message = (VacuumWorldSpeechActionResult) result;
					String payload = message.getPayload().getPayload();
					if(payload.startsWith("dirt_coordinates")) {
						String[] splitPayload = payload.split(":");
						String[] locations = splitPayload[1].split("|");
						for(String tuple : locations) {
							String[] coord = tuple.split(",");
							
							VacuumWorldCoordinates newDirtCoordinate = new VacuumWorldCoordinates(Integer.parseInt(coord[0].replace("(", "")),Integer.parseInt(coord[1].replace(")", "")));
							if(!this.uncleanedDirt.contains(newDirtCoordinate)) {
								this.uncleanedDirt.add(newDirtCoordinate);
							}
						}
					}
				} catch (Exception e) {
					System.out.println("Cast did not work!");
				}
				
			}
		}
	
	/**
	 * Used to produce a plan to follow the user
	 * @param perception
	 * @param userLocation, the perceived location of the user.
	 */
	
	public void buildFollowPlan(VWPerception perception, VacuumWorldCoordinates userLocation) {
		VacuumWorldCoordinates target = userLocation;
		int xDifference = target.getX() - perception.getActorCoordinates().getX();
		int yDifference = target.getY() - perception.getActorCoordinates().getY();
		String planCode = this.cleanerPlan.getPlanCodes().getPlanCodes().get(new Pair<>(Integer.signum(xDifference), Integer.signum(yDifference))).get(perception.getActorCurrentFacingDirection());
		planCode.chars().mapToObj(character -> (char) character).forEach(character -> addActionsToPlan(character, xDifference, yDifference, perception.getActorCurrentFacingDirection()));
	}
	
	public void buildCleanPlan(VWPerception perception, VacuumWorldCoordinates closestDirt) {
		VacuumWorldCoordinates target = closestDirt;
		int xDifference = target.getX() - perception.getActorCoordinates().getX();
		int yDifference = target.getY() - perception.getActorCoordinates().getY();
		String planCode = this.cleanerPlan.getPlanCodes().getPlanCodes().get(new Pair<>(Integer.signum(xDifference), Integer.signum(yDifference))).get(perception.getActorCurrentFacingDirection());
		planCode.chars().mapToObj(character -> (char) character).forEach(character -> addActionsToPlan(character, xDifference, yDifference, perception.getActorCurrentFacingDirection()));
	}
	
	public void updateDirtInPercetion(VWPerception perception) {
		List<VacuumWorldLocation> perceptionList = perception.getLocationsWithDirtCompatibleWithCurrentActor();
		for (VacuumWorldLocation loc : perceptionList) {
			if(!this.uncleanedDirt.contains(loc.getCoordinates())) {
				this.uncleanedDirt.add(loc.getCoordinates());
			}
		}
	}
	/**
	 * Checks to see if there is a user in the perception range.
	 * @param perception
	 * @return a boolean value whether a user is in perception range.
	 */
	public boolean isUserInPerception(VWPerception perception) {
		return (perception.getUserInPerceptionIfPresent() != null);
		 
	}
	/**
	 * Finds the nearest combatible dirt that the agents can clean.
	 * @param perception
	 * @return the coordinates of the closest dirt in the dirt list.
	 */
	public VacuumWorldCoordinates findNearestCompatibleDirt(VWPerception perception) {
		VacuumWorldCoordinates dirtCoordinates;
		VacuumWorldCoordinates closestCoord = null;
		double lowestDist = 10.0;
		for (VacuumWorldCoordinates dirt : this.uncleanedDirt) {
			dirtCoordinates = dirt;
			double dist = findDistanceBetweenCoordinates(dirtCoordinates, perception.getActorCoordinates());
			if(dist  < lowestDist) {
				lowestDist = dist;
				closestCoord = new VacuumWorldCoordinates(dirtCoordinates.getX(), dirtCoordinates.getY());
			}
		}
		return closestCoord;
	}
	/**
	 * Used to determine the distance between two VacuumWorldCoordinates objects
	 * @param coord1
	 * @param coord2
	 * @return the pythagorean distance between two coordinates.
	 */
	public double findDistanceBetweenCoordinates(VacuumWorldCoordinates coord1, VacuumWorldCoordinates coord2) {
		double a = Math.pow((coord2.getX()-coord1.getX()), 2.0);
		double b = Math.pow((coord2.getY()-coord1.getY()), 2.0);
		return Math.sqrt(a + b);
	}
	/**
	 * A simple plan created to check behind agents when the agents is first placed.
	 */
	public void perceieveBehindAgent() {
		this.cleanerPlan.clearActionsToPerform();
		this.cleanerPlan.pushActionToPerform(TurnRightAction.class);
		this.cleanerPlan.pushActionToPerform(TurnRightAction.class);
		this.cleanerPlan.pushActionToPerform(PerceiveAction.class);
	}
	
	/**
	 * If there is user present, it will return the location of the user
	 * @param perception
	 * @return location of the user
	 */
	public VacuumWorldCoordinates ifUserGetCoordinates(VWPerception perception) {
		return perception.getUserInPerceptionIfPresent().getCurrentLocation();
	}
	
	
	// The three following methods have been adapted from the VacuumWorldManhattanMind class
		private void addActionsToPlan(char character, int xDifference, int yDifference, ActorFacingDirection facingDirection) {
			switch (character) {
			case 'L':
				this.cleanerPlan.pushActionToPerform(TurnLeftAction.class);
				break;
			case 'R':
				this.cleanerPlan.pushActionToPerform(TurnRightAction.class);
				break;
			case 'M':
				getAllNecessaryMoveActions(xDifference, yDifference, facingDirection).stream().forEach(action -> this.cleanerPlan.pushActionToPerform(action));
				break;
			default:
				throw new IllegalArgumentException();
			}
		}

		private Collection<? extends Class<? extends EnvironmentalAction>> getAllNecessaryMoveActions(int xDifference, int yDifference, ActorFacingDirection facingDirection) {
			switch (facingDirection) {
			case NORTH:
			case SOUTH:
				return addMoveActions(yDifference);
			case WEST:
			case EAST:
				return addMoveActions(xDifference);
			default:
				throw new IllegalArgumentException();
			}
		}

		private Collection<? extends Class<? extends EnvironmentalAction>> addMoveActions(int difference) {
			List<Class<? extends EnvironmentalAction>> actions = new ArrayList<>();

			for (int i = 0; i < Math.abs(difference); i++) {
				actions.add(MoveAction.class);
			}

			return actions;
		}
	
	

}
