package uk.ac.rhul.cs.dice.vacuumworld.agents.minds;

import uk.ac.rhul.cs.dice.vacuumworld.actions.CleanAction;
import uk.ac.rhul.cs.dice.vacuumworld.actions.MoveAction;
import uk.ac.rhul.cs.dice.vacuumworld.actions.PerceiveAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import uk.ac.rhul.cs.dice.gawl.interfaces.actions.EnvironmentalAction;
import uk.ac.rhul.cs.dice.vacuumworld.actions.TurnLeftAction;
import uk.ac.rhul.cs.dice.vacuumworld.actions.TurnRightAction;
import uk.ac.rhul.cs.dice.vacuumworld.actions.VacuumWorldSpeechPayload;
import uk.ac.rhul.cs.dice.vacuumworld.agents.ActorFacingDirection;
import uk.ac.rhul.cs.dice.vacuumworld.agents.VacuumWorldCleaningAgent;
import uk.ac.rhul.cs.dice.vacuumworld.agents.user.User;
import uk.ac.rhul.cs.dice.vacuumworld.common.VWPerception;
import uk.ac.rhul.cs.dice.vacuumworld.environment.VacuumWorldCoordinates;
import uk.ac.rhul.cs.dice.vacuumworld.environment.VacuumWorldLocation;
import uk.ac.rhul.cs.dice.vacuumworld.utils.Pair;


public class VacuumWorldExplorerMind extends VacuumWorldDefaultMind{

String[][] gridArray;
int n = 0;
ArrayList<VacuumWorldCoordinates> locationsFound = new ArrayList<VacuumWorldCoordinates>();
ArrayList<VacuumWorldCoordinates> targetPoints;
int planNumber = 0;
VacuumWorldExplorerPlan explorePlan = new VacuumWorldExplorerPlan();	 
	public VacuumWorldExplorerMind(String bodyId) {
		super(bodyId);
	}

	@Override
	public EnvironmentalAction decide(Object... parameters) {
		VWPerception perception = getPerception();
		
	    
		if (perception != null) {
			updateAvailableActions(perception);
			return planSelector(perception);		    
		} else {
			return buildPhysicalAction(PerceiveAction.class);
		}
		
		
		
	}
	
	public EnvironmentalAction planSelector(VWPerception perception) {
		// If there is dirt, clean it without doing anything else
		User user = perception.getUserInPerceptionIfPresent();
		VacuumWorldCoordinates actCoordinates  = perception.getActorCoordinates();
	    ActorFacingDirection direction = perception.getActorCurrentFacingDirection();
	    int x = actCoordinates.getX();
	    int y = actCoordinates.getY();
	    System.out.println("!!!!!! PLAN NUMBER is : " + this.planNumber);
		if(perception.canCurrentActorCleanOnHisCurrentLocation()) {
	    	return buildPhysicalAction(CleanAction.class);
	    } else if (user != null && isUserBlocking(perception, user)) { // This will tell the user to move if they are in the way.
	    	return tellUserToMoveIfInFront(perception, user);
	    } else if ((this.n==0)&&(this.planNumber==0)) {	// This will find N if N hasn't been found.
	    	
	    	if((perception.doesCurrentActorHaveWallInFront()) && ((x>0) || (y>0)) && 
					((direction == ActorFacingDirection.EAST)||(direction == ActorFacingDirection.SOUTH))) {
				
				// Operator used to determine which value is n.
	    		this.n = (x > y) ? x : y ;
		    	this.gridArray = new String[this.n+1][this.n+1];
		    	System.out.println("!!!!!! N is : " + this.n);
		    	this.planNumber++;
		    	buildPlan1(perception, this.n);	// This will move the agent to a location on the 
				// exploration grid so it can start exploring.
			}
		    // if n hasn't been found, then try to find it.
		    return decideDirection(x, y, direction.toString());
	    	
	    	
	    	
	    } else if (this.planNumber == 1) {
	    	updateMap(perception);	// This updates the agents map on dirt, agents and user location.	    	
	    	if(!this.explorePlan.getActionsToPerform().isEmpty()) {
	    		this.explorePlan.setLastAction(this.explorePlan.pullActionToPerform());
				return buildPhysicalAction(this.explorePlan.getLastAction());
	    	} else {
	    		this.planNumber++;
	    		this.explorePlan.clearActionsToPerform();
	    	}
	    	
	    } else if (this.planNumber == 2) {
	    	
	    	if(this.explorePlan.getActionsToPerform().isEmpty()) {
	    		if(!this.targetPoints.isEmpty()) {
	    			buildPlan2(perception);
		    	} else {
		    		this.explorePlan.clearActionsToPerform();
		    		this.planNumber++;
		    	}
	    	} else {
	    		this.explorePlan.setLastAction(this.explorePlan.pullActionToPerform());
				return buildPhysicalAction(this.explorePlan.getLastAction());
	    	} 
	    } else if ((this.planNumber == 3) &&  (this.n > 5)) {
	    	if(this.explorePlan.getActionsToPerform().isEmpty()) {
	    		System.out.println("!!!!!!!!!!!!!!!!!!BUILDING PLAN 3!!!");
	    		buildPlan3(perception);
	    	} else {
	    		System.out.println("!!!!!!!!!!!!!!!!!!EXECUTING PLAN 3!!!");
	    		this.explorePlan.setLastAction(this.explorePlan.pullActionToPerform());
				return buildPhysicalAction(this.explorePlan.getLastAction());
	    	}
	    }
		return null;
		
	}
	
	public void buildPlan1(VWPerception perception, int n) {
		
		this.targetPoints = new ArrayList<VacuumWorldCoordinates>();
		
		// Generates the path for the robot to follow, it's a spiral
		// pattern determined by the grid around the walls of the wall.
		
		for(int index=1; index < n; index++) {
			
			VacuumWorldCoordinates top = new VacuumWorldCoordinates(index,1);
			VacuumWorldCoordinates bottom = new VacuumWorldCoordinates(index,n-1);
			VacuumWorldCoordinates left = new VacuumWorldCoordinates(1,index);
			VacuumWorldCoordinates right = new VacuumWorldCoordinates(n-1,index);
			
			if(!this.targetPoints.contains(top)) {
				this.targetPoints.add(top);
			}
			if(!this.targetPoints.contains(bottom)) {
				this.targetPoints.add(bottom);
			}
			if(!this.targetPoints.contains(left)) {
				this.targetPoints.add(left);
			}
			if(!this.targetPoints.contains(right)) {
				this.targetPoints.add(right);
			}
			
		}
		System.out.println("!!!!!! TARGET POINTS CONTAIN: : " + this.targetPoints.toString());
		VacuumWorldCoordinates closestCoordinate = closest(this.targetPoints, perception.getActorCoordinates());
		System.out.println("!!!!!! CLOSESTCOORDINATES CONTAIN: : " + closestCoordinate.toString());
		int xDifference = closestCoordinate.getX() - perception.getActorCoordinates().getX();
		int yDifference = closestCoordinate.getY() - perception.getActorCoordinates().getY();
		
		String planCode = explorePlan.getPlanCodes().getPlanCodes().get(new Pair<>(Integer.signum(xDifference), Integer.signum(yDifference))).get(perception.getActorCurrentFacingDirection());
		System.out.println("!!!!!! PLANCODE CONTAIN: : " + planCode);
		planCode.chars().mapToObj(character -> (char) character).forEach(character -> addActionsToPlan(character, xDifference, yDifference, perception.getActorCurrentFacingDirection()));	
	}
	
	public void buildPlan2(VWPerception perception) {
			VacuumWorldCoordinates closestCoordinate = closest(this.targetPoints, perception.getActorCoordinates());
			this.targetPoints.remove(closestCoordinate);
			int xDifference = closestCoordinate.getX() - perception.getActorCoordinates().getX();
			int yDifference = closestCoordinate.getY() - perception.getActorCoordinates().getY();
			String planCode = explorePlan.getPlanCodes().getPlanCodes().get(new Pair<>(Integer.signum(xDifference), Integer.signum(yDifference))).get(perception.getActorCurrentFacingDirection());
			planCode.chars().mapToObj(character -> (char) character).forEach(character -> addActionsToPlan(character, xDifference, yDifference, perception.getActorCurrentFacingDirection()));
	}
	
	public void buildPlan3(VWPerception perception) {
		
		VacuumWorldCoordinates target = new VacuumWorldCoordinates(this.n/2, this.n/2);
		System.out.println("!!!!!! 333 TARGET POINT CONTAIN: : " + target.toString());
		int xDifference = target.getX() - perception.getActorCoordinates().getX();
		int yDifference = target.getY() - perception.getActorCoordinates().getY();
		String planCode = explorePlan.getPlanCodes().getPlanCodes().get(new Pair<>(Integer.signum(xDifference), Integer.signum(yDifference))).get(perception.getActorCurrentFacingDirection());
		planCode.chars().mapToObj(character -> (char) character).forEach(character -> addActionsToPlan(character, xDifference, yDifference, perception.getActorCurrentFacingDirection()));
}
	
	private boolean isUserBlocking(VWPerception perception, User user) {
		VacuumWorldCoordinates currentCoordinates = perception.getActorCoordinates();
    	if(perception.getActorCurrentFacingDirection() == ActorFacingDirection.NORTH) {
    		if(user.getCurrentLocation().equals(new VacuumWorldCoordinates(currentCoordinates.getX(), currentCoordinates.getY()-1))) {
    			return true;
    		}
    	} else if (perception.getActorCurrentFacingDirection() == ActorFacingDirection.SOUTH) {
    		if(user.getCurrentLocation().equals(new VacuumWorldCoordinates(currentCoordinates.getX(), currentCoordinates.getY()+1))) {
    			return true;
    		}
    	} else if (perception.getActorCurrentFacingDirection() == ActorFacingDirection.WEST) {
    		if(user.getCurrentLocation().equals(new VacuumWorldCoordinates(currentCoordinates.getX()-1, currentCoordinates.getY()))) {
    			return true;
    		}
    	} else if (perception.getActorCurrentFacingDirection() == ActorFacingDirection.EAST) {
    		if(user.getCurrentLocation().equals(new VacuumWorldCoordinates(currentCoordinates.getX()+1, currentCoordinates.getY()-1))) {
    			return true;
    		}
    	}
    	return false;
	}
	
	private EnvironmentalAction tellUserToMoveIfInFront(VWPerception perception, User user) {
		// This checks whether the user is immediately in front of the user, it tells the user to move
    	// based on its location.
		VacuumWorldCoordinates currentCoordinates = perception.getActorCoordinates();
		ArrayList<String> communications = new ArrayList<String>();
    	if(perception.getActorCurrentFacingDirection() == ActorFacingDirection.NORTH) {
    		if(user.getCurrentLocation().equals(new VacuumWorldCoordinates(currentCoordinates.getX(), currentCoordinates.getY()-1))) {
    			if(perception.getActorCoordinates().getX() == 0) {
	    			return buildSpeechAction(getBodyId(), communications, new VacuumWorldSpeechPayload("moveE", true));
	    		} else {
	    			return buildSpeechAction(getBodyId(), communications, new VacuumWorldSpeechPayload("moveW", true));
	    		}
    		}
    	} else if (perception.getActorCurrentFacingDirection() == ActorFacingDirection.SOUTH) {
    		if(user.getCurrentLocation().equals(new VacuumWorldCoordinates(currentCoordinates.getX(), currentCoordinates.getY()+1))) {
    			if(perception.getActorCoordinates().getX() == 0) {
	    			return buildSpeechAction(getBodyId(), communications, new VacuumWorldSpeechPayload("moveE", true));
	    		} else {
	    			return buildSpeechAction(getBodyId(), communications, new VacuumWorldSpeechPayload("moveW", true));
	    		}
    		}
    	} else if (perception.getActorCurrentFacingDirection() == ActorFacingDirection.WEST) {
    		if(user.getCurrentLocation().equals(new VacuumWorldCoordinates(currentCoordinates.getX()-1, currentCoordinates.getY()))) {
    			if(perception.getActorCoordinates().getY() == 0) {
	    			return buildSpeechAction(getBodyId(), communications, new VacuumWorldSpeechPayload("moveN", true));
	    		} else {
	    			return buildSpeechAction(getBodyId(), communications, new VacuumWorldSpeechPayload("moveS", true));
	    		}
    		}
    	} else if (perception.getActorCurrentFacingDirection() == ActorFacingDirection.EAST) {
    		if(user.getCurrentLocation().equals(new VacuumWorldCoordinates(currentCoordinates.getX()+1, currentCoordinates.getY()-1))) {
    			if(perception.getActorCoordinates().getY() == 0) {
	    			return buildSpeechAction(getBodyId(), communications, new VacuumWorldSpeechPayload("moveN", true));
	    		} else {
	    			return buildSpeechAction(getBodyId(), communications, new VacuumWorldSpeechPayload("moveS", true));
	    		}
    		}
    	}
    	return null;
	}
	
	// The three following methods have been adapted from the VacuumWorldManhattanMind class
	private void addActionsToPlan(char character, int xDifference, int yDifference, ActorFacingDirection facingDirection) {
		switch (character) {
		case 'L':
			this.explorePlan.pushActionToPerform(TurnLeftAction.class);
			break;
		case 'R':
			this.explorePlan.pushActionToPerform(TurnRightAction.class);
			break;
		case 'M':
			getAllNecessaryMoveActions(xDifference, yDifference, facingDirection).stream().forEach(action -> this.explorePlan.pushActionToPerform(action));
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
	
	
	public VacuumWorldCoordinates closest(List<VacuumWorldCoordinates> coordList, VacuumWorldCoordinates coord) {
		VacuumWorldCoordinates closestCoord = null;
		double lowestDist = 10.0;
		for (VacuumWorldCoordinates coordx : coordList) {
			if(!coordx.equals(coord)) {
				System.out.println("!!!!I'm in the loop, here are the two coordinates : " + coord +","+coordx);
				double dist = findDistanceBetweenCoordinates(coord, coordx);
				System.out.println("!!!!I'm in the loop, PRINTING DISTANCE: " + dist);
				if(dist  < lowestDist) {
					System.out.println("!!!!I SHOULD BE ASSIGNED HERE! : " + coord +","+coordx);
					lowestDist = findDistanceBetweenCoordinates(coord, coordx);
					closestCoord = new VacuumWorldCoordinates(coordx.getX(), coordx.getY());
				}
			} else {
				System.out.println("COORDINATES ARE THE SAME!");
			}
			
		}
		System.out.println("!!!!I'm in the loop, here is the closest coordinate : " + closestCoord);
		return closestCoord;
	}
	
	public double findDistanceBetweenCoordinates(VacuumWorldCoordinates coord1, VacuumWorldCoordinates coord2) {
		double a = Math.pow((coord2.getX()-coord1.getX()), 2.0);
		double b = Math.pow((coord2.getY()-coord1.getY()), 2.0);
		return Math.sqrt(a + b);
	}
		
		
	
	public List<VacuumWorldCoordinates> generateExplorePoints() {
		return null;
		
	}
	
	public EnvironmentalAction exploreMove() {
		
		
		
		return null;
		
	}
	
	public void updateMap(VWPerception perception) {
		
		for (VacuumWorldLocation i : perception.getLocationsWithDirtCompatibleWithCurrentActor()) {
			int dirtX = i.getCoordinates().getX();
			int dirtY = i.getCoordinates().getY();
			locationsFound.add(i.getCoordinates());
			if (i.getDirt().getExternalAppearance().getDirtType().toString() == "GREEN") {
				this.gridArray[dirtX][dirtY] = "GREEN_DIRT";
			} else {
				this.gridArray[dirtX][dirtY] = "ORANGE_DIRT";
			}
			
		}
		
		User user = perception.getUserInPerceptionIfPresent();
		
		if (user != null) {
			locationsFound.add(user.getCurrentLocation());
			int userX = user.getCurrentLocation().getX();
			int userY = user.getCurrentLocation().getY();
			this.gridArray[userX][userY] = "User";
			
		}
		
		 List<VacuumWorldCleaningAgent> whiteAgents = perception.getWhiteAgentsInPerception();
		 
		 for (VacuumWorldCleaningAgent wAgent : whiteAgents) {
			 locationsFound.add(wAgent.getCurrentLocation());
			 this.gridArray[wAgent.getCurrentLocation().getX()][wAgent.getCurrentLocation().getY()] = "WHITE_AGENT";
		 }
		 
		 List<VacuumWorldCleaningAgent> orangeAgents = perception.getOrangeAgentsInPerception();
		 
		 for (VacuumWorldCleaningAgent oAgent : orangeAgents) {
			 locationsFound.add(oAgent.getCurrentLocation());
			 this.gridArray[oAgent.getCurrentLocation().getX()][oAgent.getCurrentLocation().getY()] = "ORANGE_AGENT";
		 }
		 
		 List<VacuumWorldCleaningAgent> greenAgents = perception.getGreenAgentsInPerception();
		 
		 for (VacuumWorldCleaningAgent gAgent : greenAgents) {
			 locationsFound.add(gAgent.getCurrentLocation());
			 this.gridArray[gAgent.getCurrentLocation().getX()][gAgent.getCurrentLocation().getY()] = "GREEN_AGENT";
		 }
		 
		 for(VacuumWorldLocation loc : perception.getLocationsInPerceptionList()) {
			 
			 if(!locationsFound.contains(loc.getCoordinates())) {
				 this.gridArray[loc.getCoordinates().getX()][loc.getCoordinates().getY()] = "EXPLORED";
			 }
		 }
		
	}
	
	/**
	 * This method is used to determine whether the agents should move east or south in order
	 * to find N.
	 * @param x, the x-coordinate
	 * @param y, the y-coordinate of the agent.
	 * @param direction_string, the facing direction of the agent.
	 * @return an EnvironmentalAction that will move the robot.
	 */
	public EnvironmentalAction decideDirection(int x, int y, String direction_string) {
		
		// if mister robot is closer to south end than the east end, we're going to move south
		if (x < y) {
			
			return decideAction(direction_string, "SOUTH");
			
		} else {	// Otherwise just move east until we find the wall
			
			return decideAction(direction_string, "EAST");
		}	
	}
	
	/**
	 * This function is used to determine the next physical action for the agent, its generalised so can be
	 * used for any action required
	 * @param direction_string, the current facing direction of the agent
	 * @param desired_direction, the desired direction, if the direction is the same as the
	 * 							 current direction it will move forward.
	 * @return an EnviromentalAction object in which physical action will be done.
	 */
	
	public EnvironmentalAction decideAction(String direction_string, String desired_direction) {
		switch (direction_string.charAt(0)) {
		case 'N':
			
		    switch (desired_direction.charAt(0)) {
		    case 'N':
		    	return buildPhysicalAction(MoveAction.class);
		    case 'S':
		    	return buildPhysicalAction(TurnRightAction.class);
		    case 'E':
		    	return buildPhysicalAction(TurnRightAction.class);
		    case 'W':
		    	return buildPhysicalAction(TurnLeftAction.class);	
		    default:
			    throw new IllegalArgumentException();
		    } 
		case 'S':
			switch (desired_direction.charAt(0)) {
		    case 'N':
		    	return buildPhysicalAction(TurnRightAction.class);
		    case 'S':
		    	return buildPhysicalAction(MoveAction.class);
		    case 'E':
		    	return buildPhysicalAction(TurnLeftAction.class);
		    case 'W':
		    	return buildPhysicalAction(TurnRightAction.class);	
		    default:
			    throw new IllegalArgumentException();
		    } 
		case 'E':
			switch (desired_direction.charAt(0)) {
		    case 'N':
		    	return buildPhysicalAction(TurnLeftAction.class);
		    case 'S':
		    	return buildPhysicalAction(TurnRightAction.class);
		    case 'E':
		    	return buildPhysicalAction(MoveAction.class);
		    case 'W':
		    	return buildPhysicalAction(TurnRightAction.class);	
		    default:
			    throw new IllegalArgumentException();
		    } 
		case 'W':
			switch (desired_direction.charAt(0)) {
		    case 'N':
		    	return buildPhysicalAction(TurnRightAction.class);
		    case 'S':
		    	return buildPhysicalAction(TurnLeftAction.class);
		    case 'E':
		    	return buildPhysicalAction(TurnRightAction.class);
		    case 'W':
		    	return buildPhysicalAction(MoveAction.class);	
		    default:
			    throw new IllegalArgumentException();
		    } 
		default:
		    throw new IllegalArgumentException();
		}
		
	}

}
