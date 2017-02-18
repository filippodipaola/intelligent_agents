package uk.ac.rhul.cs.dice.vacuumworld.agents.minds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.rhul.cs.dice.gawl.interfaces.actions.EnvironmentalAction;
import uk.ac.rhul.cs.dice.vacuumworld.actions.CleanAction;
import uk.ac.rhul.cs.dice.vacuumworld.actions.MoveAction;
import uk.ac.rhul.cs.dice.vacuumworld.actions.PerceiveAction;
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

public class VacuumWorldWhiteMind extends VacuumWorldDefaultMind{
	
	private List<VacuumWorldCleaningAgent> greenAgents;
	private List<VacuumWorldCleaningAgent> orangeAgents;
	private List<VacuumWorldCoordinates> greenDirt;
	private List<VacuumWorldCoordinates> orangeDirt;
	private List<VacuumWorldCoordinates> lastGreenDirt;
	private List<VacuumWorldCoordinates> lastOrangeDirt;
	String[][] gridArray;
	int n = 0;
	int planNumber = 0;
	VacuumWorldExplorerPlan explorePlan = new VacuumWorldExplorerPlan();	
	ArrayList<VacuumWorldCoordinates> targetPoints;
	
	public VacuumWorldWhiteMind(String bodyId) {
		super(bodyId);
		this.greenAgents = new ArrayList<VacuumWorldCleaningAgent>();
		this.orangeAgents = new ArrayList<VacuumWorldCleaningAgent>();
		this.orangeDirt = new ArrayList<VacuumWorldCoordinates>();
		this.greenDirt = new ArrayList<VacuumWorldCoordinates>();
		this.lastOrangeDirt = new ArrayList<VacuumWorldCoordinates>();
		this.lastGreenDirt = new ArrayList<VacuumWorldCoordinates>();
		// TODO Auto-generated constructor stub
	}

	@Override
	public EnvironmentalAction decide(Object... parameters) {
		VWPerception perception = getPerception();
		
	    
		if (perception != null) {
			updateAvailableActions(perception);
			updateMap(perception);
			checkForDirt(perception);
			checkForOtherAgents(perception);
			
			return planSelector(perception);		    
		} else {
			return buildPhysicalAction(PerceiveAction.class);
		}
	}
	/**
	 * This will chose a plan for the agent, mattering on its belief of the environment.
	 * @param perception
	 * @return an action for the agent to perform/
	 */
	public EnvironmentalAction planSelector(VWPerception perception) {
		// If there is dirt, clean it without doing anything else
		User user = perception.getUserInPerceptionIfPresent();
		VacuumWorldCoordinates actCoordinates  = perception.getActorCoordinates();
	    ActorFacingDirection direction = perception.getActorCurrentFacingDirection();
	    int x = actCoordinates.getX();
	    int y = actCoordinates.getY();
	    if(perception.canCurrentActorCleanOnHisCurrentLocation()) {
	    	return buildPhysicalAction(CleanAction.class);
	    } else if (user != null && isUserBlocking(perception, user)) { // This will tell the user to move if they are in the way.
	    	return tellUserToMoveIfInFront(perception, user);
	    } else if (!this.explorePlan.getActionsToPerform().isEmpty()) {
	    	return buildPhysicalAction(this.explorePlan.pullActionToPerform());	    
		} else if (this.n==0) {	// This will find N if N hasn't been found.

	    	if((perception.doesCurrentActorHaveWallInFront()) && ((x>0) || (y>0)) && 
					((direction == ActorFacingDirection.EAST)||(direction == ActorFacingDirection.SOUTH))) {
				
				// Operator used to determine which value is n.
	    		this.n = (x > y) ? x : y ;
		    	this.gridArray = new String[this.n+1][this.n+1];
		    	this.planNumber++;
		    	buildPlan1(perception, this.n);	// This will move the agent to a location on the 
				// exploration grid so it can start exploring.
			}
		    // if n hasn't been found, then try to find it.
		    return decideDirection(x, y, direction.toString());
	    } else if ((!greenAgents.isEmpty()) && (!this.lastGreenDirt.equals(this.greenDirt))) { // This will send the perceived dirt locations to the green agents.
	    	this.lastGreenDirt = this.greenDirt;
	    	this.greenDirt = new ArrayList<>();
	    	return sendGreenDirt();
	    	
	    } else if ((!orangeAgents.isEmpty()) && (!this.lastOrangeDirt.equals(this.orangeDirt))) { // This will send the perceived dirt locations to the orange agents.
	    	this.lastOrangeDirt = this.orangeDirt;
	    	this.lastOrangeDirt = this.orangeDirt;
	    	this.orangeDirt = new ArrayList<>();
	    	return sendOrangeDirt();
	    } else if (this.targetPoints.isEmpty()) {
	    	buildPlan1(perception, this.n);
	    } else if (this.planNumber == 2) {
	    	if(!this.targetPoints.isEmpty()) {
	    		buildPlan2(perception);
		    } else {
		    	this.explorePlan.clearActionsToPerform();
		    	generatorTargetList();
		    }
	    }
		return decideActionRandomly();
	} 
	 
	/**
	 * This builds the first plan which gets the agent to the starting position of the exploring
	 * path.	
	 * @param perception
	 * @param n
	 */
		
	public void buildPlan1(VWPerception perception, int n) {
		generatorTargetList();
		VacuumWorldCoordinates closestCoordinate = closest(this.targetPoints, perception.getActorCoordinates());
		int xDifference = closestCoordinate.getX() - perception.getActorCoordinates().getX();
		int yDifference = closestCoordinate.getY() - perception.getActorCoordinates().getY();
		String planCode = explorePlan.getPlanCodes().getPlanCodes().get(new Pair<>(Integer.signum(xDifference), Integer.signum(yDifference))).get(perception.getActorCurrentFacingDirection());
		planCode.chars().mapToObj(character -> (char) character).forEach(character -> addActionsToPlan(character, xDifference, yDifference, perception.getActorCurrentFacingDirection()));
	}
	/**
	 * Sends a message to all the green agents the white agent has found if it see any dirt.
	 * @return a speech action
	 */
	public EnvironmentalAction sendGreenDirt() {
		this.explorePlan.clearActionsToPerform();
		List<String> greenAgentIDs = new ArrayList<String>();
		for (VacuumWorldCleaningAgent agent : this.greenAgents) {
			greenAgentIDs.add(agent.getId());
		}
		String header = "dirt_location";
		String stringToSend = formatStringTransmission(header, this.greenDirt);
		return buildSpeechAction(getBodyId(), greenAgentIDs, new VacuumWorldSpeechPayload(stringToSend, false));
	}
	/**
	 * Sends a message to all the orange agents the white agent has found if it see any dirt.
	 * @return a speech action
	 */
	public EnvironmentalAction sendOrangeDirt() {
		this.explorePlan.clearActionsToPerform();
		List<String> orangeAgentIDs = new ArrayList<String>();
		for (VacuumWorldCleaningAgent agent : this.orangeAgents) {
			orangeAgentIDs.add(agent.getId());
		}
		String header = "dirt_location";
		String stringToSend = formatStringTransmission(header, this.greenDirt);
		return buildSpeechAction(getBodyId(), orangeAgentIDs, new VacuumWorldSpeechPayload(stringToSend, false));
	}
	/**
	 * This formats the string used in the payload of the speech action
	 * @param header
	 * @param coordinates
	 * @return
	 */
	public String formatStringTransmission(String header, List<VacuumWorldCoordinates> coordinates) {
		String returnString = "";
		returnString += header;
		returnString += ":";
		String coordinateString = coordinates.stream().map(VacuumWorldCoordinates::toString).collect(Collectors.joining("|"));
		returnString += coordinateString;
		System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!! RETURN STRING!!! : " + returnString);
		return returnString;
	}
	/**
	 * This builds the plan to move the agent to the next location on the exploration grid.
	 * @param perception
	 */
	public void buildPlan2(VWPerception perception) {
		VacuumWorldCoordinates closestCoordinate = closest(this.targetPoints, perception.getActorCoordinates());
		this.targetPoints.remove(closestCoordinate);
		int xDifference = closestCoordinate.getX() - perception.getActorCoordinates().getX();
		int yDifference = closestCoordinate.getY() - perception.getActorCoordinates().getY();
		String planCode = explorePlan.getPlanCodes().getPlanCodes().get(new Pair<>(Integer.signum(xDifference), Integer.signum(yDifference))).get(perception.getActorCurrentFacingDirection());
		planCode.chars().mapToObj(character -> (char) character).forEach(character -> addActionsToPlan(character, xDifference, yDifference, perception.getActorCurrentFacingDirection()));
	}
	
	/**
	 * When the exploration grid has been completely searched, this function
	 * is called to allow for the grid to be re-explored.
	 */
	public void generatorTargetList() {
		this.targetPoints = new ArrayList<VacuumWorldCoordinates>();
		
		// Generates the path for the robot to follow, it's a spiral
		// pattern determined by the grid around the walls of the wall.
		
		for(int index=1; index < this.n; index++) {
			
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
	}
	/**
	 * This decides the direction that the agent will go in order to find N.
	 * @param x
	 * @param y
	 * @param direction_string
	 * @return
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

	/**
	 * This method is used to check if the user is blocking the agents, it used to
	 * tell the agent to request the user to move out of the way.
	 * @param perception
	 * @param user
	 * @return
	 */
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
	/**
	 * if the user is in the way, this will return the speech action to ask the user to move.
	 * @param perception
	 * @param user
	 * @return
	 */
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
	/**
	 * This will check and order the agents it finds.
	 * @param perception
	 */
	public void checkForOtherAgents(VWPerception perception) {
		for(VacuumWorldCleaningAgent agent : perception.getGreenAgentsInPerception()) {
			if(!this.greenAgents.contains(agent)) {
				this.greenAgents.add(agent);
			}
		}
		for(VacuumWorldCleaningAgent agent : perception.getOrangeAgentsInPerception()) {
			if(!this.orangeAgents.contains(agent)) {
				this.orangeAgents.add(agent);
			}
		}
		 
	}
	/**
	 * This will check if there is any dirt in the perception.
	 * @param perception
	 */
	public void checkForDirt(VWPerception perception) {
		for (VacuumWorldLocation i : perception.getLocationsWithDirtCompatibleWithCurrentActor()) {
			if (i.getDirt().getExternalAppearance().getDirtType().toString() == "GREEN") {
				if(!greenDirt.contains(i.getCoordinates())) {
					greenDirt.add(i.getCoordinates());
				}
			} else {
				if(!orangeDirt.contains(i.getCoordinates())) {
					orangeDirt.add(i.getCoordinates());
				}
			}
			
		}
	}
	/**
	 * This will update the list of dirt if the agents finds a new piece of dirt.
	 * @param perception
	 */
	public void updateMap(VWPerception perception) {
		
		for (VacuumWorldLocation i : perception.getLocationsWithDirtCompatibleWithCurrentActor()) {
			int dirtX = i.getCoordinates().getX();
			int dirtY = i.getCoordinates().getY();
			//locationsFound.add(i.getCoordinates());
			if (i.getDirt().getExternalAppearance().getDirtType().toString() == "GREEN") {
				this.gridArray[dirtX][dirtY] = "GREEN_DIRT";
			} else {
				this.gridArray[dirtX][dirtY] = "ORANGE_DIRT";
			}
			
		}
		
		User user = perception.getUserInPerceptionIfPresent();
		
		if (user != null) {
			//locationsFound.add(user.getCurrentLocation());
			int userX = user.getCurrentLocation().getX();
			int userY = user.getCurrentLocation().getY();
			this.gridArray[userX][userY] = "User";
			
		}
		
		 List<VacuumWorldCleaningAgent> whiteAgents = perception.getWhiteAgentsInPerception();
		 
		 for (VacuumWorldCleaningAgent wAgent : whiteAgents) {
			 //locationsFound.add(wAgent.getCurrentLocation());
			 this.gridArray[wAgent.getCurrentLocation().getX()][wAgent.getCurrentLocation().getY()] = "WHITE_AGENT";
		 }
		 
		 List<VacuumWorldCleaningAgent> orangeAgents = perception.getOrangeAgentsInPerception();
		 
		 for (VacuumWorldCleaningAgent oAgent : orangeAgents) {
			 //locationsFound.add(oAgent.getCurrentLocation());
			 this.gridArray[oAgent.getCurrentLocation().getX()][oAgent.getCurrentLocation().getY()] = "ORANGE_AGENT";
		 }
		 
		 List<VacuumWorldCleaningAgent> greenAgents = perception.getGreenAgentsInPerception();
		 
		 for (VacuumWorldCleaningAgent gAgent : greenAgents) {
			 //locationsFound.add(gAgent.getCurrentLocation());
			 this.gridArray[gAgent.getCurrentLocation().getX()][gAgent.getCurrentLocation().getY()] = "GREEN_AGENT";
		 }
		 		
	}

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
				double dist = findDistanceBetweenCoordinates(coord, coordx);
				if(dist  < lowestDist) {
					lowestDist = findDistanceBetweenCoordinates(coord, coordx);
					closestCoord = new VacuumWorldCoordinates(coordx.getX(), coordx.getY());
				}
			} else {
			}
			
		}
		return closestCoord;
	}
	
	public double findDistanceBetweenCoordinates(VacuumWorldCoordinates coord1, VacuumWorldCoordinates coord2) {
		double a = Math.pow((coord2.getX()-coord1.getX()), 2.0);
		double b = Math.pow((coord2.getY()-coord1.getY()), 2.0);
		return Math.sqrt(a + b);
	}

}
