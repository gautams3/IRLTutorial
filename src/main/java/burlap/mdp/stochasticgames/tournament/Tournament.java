package burlap.mdp.stochasticgames.tournament;

import burlap.debugtools.DPrint;
import burlap.debugtools.RandomFactory;
import burlap.mdp.stochasticgames.agent.AgentFactory;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.mdp.stochasticgames.world.World;
import burlap.mdp.stochasticgames.world.WorldGenerator;

import java.util.*;
import java.util.Map.Entry;


/**
 * This class is designed to run tournaments of sets of users. It is particularly useful when there are a large number of agents
 * whose performance is going to be compared in games in which only a subset of the agents can participate at a time, for instance
 * running a tournament of many agents in 2-player games like iterated prisoner's dilemma. The Tournament class requires
 * a list of agents, a world generator and a match selector. The MatchSelector determines which agents will be matched up
 * in a game, which {@link SGAgentType} role they will play as and how many matches there will be.
 * @author James MacGlashan
 *
 */
public class Tournament {

	protected List<AgentFactory>			agents;
	protected List <Double>					tournamentCumulatedReward;
	
	protected int							maxStages;
	protected int							numGames;
	
	protected MatchSelector					selector;
	protected WorldGenerator				worldGenerator;
	
	
	
	protected int							debugId = 25633;
	
	
	
	/**
	 * Initializes the Tournament
	 * @param maxStages the maximum number of stages to be played in any single game of the tournament.
	 * @param selector a match selector that dictates which agents will play who and in which games
	 * @param worldGenerator a world generator to create new worlds for each match
	 */
	public Tournament(int maxStages, MatchSelector selector, WorldGenerator worldGenerator){
		agents = new ArrayList<AgentFactory>();
		tournamentCumulatedReward = new ArrayList<Double>();
		this.maxStages = maxStages;
		this.selector = selector;
		this.worldGenerator = worldGenerator;
		
		this.numGames = 0;
		
	}
	
	/**
	 * Initializes the Tournament
	 * @param maxStages the maximum number of stages to be played in any single game of the tournament.
	 * @param numGames the number of games that will be played for each agent matching
	 * @param selector a match selector that dictates which agents will play who and in which games
	 * @param worldGenerator a world generator to create new worlds for each match
	 */
	public Tournament(int maxStages, int numGames, MatchSelector selector, WorldGenerator worldGenerator){
		agents = new ArrayList<AgentFactory>();
		tournamentCumulatedReward = new ArrayList<Double>();
		this.maxStages = maxStages;
		this.selector = selector;
		this.worldGenerator = worldGenerator;
		
		this.numGames = numGames;
		
	}
	
	
	/**
	 * Initializes the Tournament
	 * @param agents the list of agents that will participate in the tournament
	 * @param maxStages the maximum number of stages to be played in any single game of the tournament.
	 * @param selector a match selector that dictates which agents will play who and in which games
	 * @param worldGenerator a world generator to create new worlds for each match
	 */
	public Tournament(List <AgentFactory> agents, int maxStages, MatchSelector selector, WorldGenerator worldGenerator){
		this.agents = agents;
		tournamentCumulatedReward = new ArrayList<Double>(this.agents.size());
		for(int i = 0; i < agents.size(); i++){
			tournamentCumulatedReward.add(0.);
		}
		
		this.maxStages = maxStages;
		this.selector = selector;
		this.worldGenerator = worldGenerator;
		
		this.numGames = 0;
	}
	
	
	/**
	 * Initializes the Tournament
	 * @param agents the list of agents that will participate in the tournament
	 * @param maxStages the maximum number of stages to be played in any single game of the tournament.
	 * @param numGames the number of games that will be played for each agent matching
	 * @param selector a match selector that dictates which agents will play who and in which games
	 * @param worldGenerator a world generator to create new worlds for each match
	 */
	public Tournament(List <AgentFactory> agents, int maxStages, int numGames, MatchSelector selector, WorldGenerator worldGenerator){
		this.agents = agents;
		tournamentCumulatedReward = new ArrayList<Double>(this.agents.size());
		for(int i = 0; i < agents.size(); i++){
			tournamentCumulatedReward.add(0.);
		}
		
		this.maxStages = maxStages;
		this.selector = selector;
		this.worldGenerator = worldGenerator;
		
		this.numGames = numGames;;
	}
	
	
	/**
	 * Adds an agent to the tournament
	 * @param agent the agent to add to the tournament
	 * @return the index of the agent in this tournament
	 */
	public int addAgent(AgentFactory agent){
		this.agents.add(agent);
		this.tournamentCumulatedReward.add(0.);
		return this.agents.size()-1;
	}
	
	
	/**
	 * Returns the number of agents who are playing in this tournament
	 * @return the number of agents who are playing in this tournament
	 */
	public int getNumAgents(){
		return agents.size();
	}
	
	
	/**
	 * Returns the cumulative reward received by the agent who is indexed at position i
	 * @param i the index of the agent
	 * @return the cumulative reward received by the agent who is indexed at position i
	 */
	public double getCumulativeRewardFor(int i){
		return tournamentCumulatedReward.get(i);
	}
	
	
	/**
	 * Reset the cumulative reward received by each agent in this tournament.
	 */
	public void resetTournamentReward(){
		for(int i = 0; i < tournamentCumulatedReward.size(); i++){
			tournamentCumulatedReward.set(i, 0.);
		}
	}
	
	
	/**
	 * Prints the tournament results by agent index and their cumulative reward received in the tournament.
	 */
	public void printOutResults(){
		for(int i = 0; i < agents.size(); i++){
			System.out.println(i + ": " + tournamentCumulatedReward.get(i));
		}
	}
	
	
	/**
	 * Runs the tournament
	 */
	public void runTournament(){
		
		selector.resetMatchSelections();
		
		List<MatchEntry> match;
		while((match = selector.getNextMatch()) != null){
			
			World w = worldGenerator.generateWorld();
			
			//shuffle entrants
			Collections.shuffle(match, RandomFactory.getMapped(0));
			
			Map<String, Integer> agentNameToId = new HashMap<String, Integer>();
			
			//have the matched agents join the world
			for(MatchEntry me : match){
				SGAgent a = agents.get(me.agentId).generateAgent("agent" + me.agentId, me.agentType);
				w.join(a);
				agentNameToId.put(a.agentName(), me.agentId);
				DPrint.c(debugId, me.agentId + " ");
			}
			DPrint.cl(debugId, "");
			
			//run the game
			for(int i = 0; i < this.numGames; i++){
				w.runGame(maxStages);
			}
			
			//record results
			for(Entry<String, Integer> an : agentNameToId.entrySet()){
				int aId = an.getValue();
				double gameCumR = w.getCumulativeRewardForAgent(an.getKey());
				double tournCumR = tournamentCumulatedReward.get(aId);
				tournamentCumulatedReward.set(aId, gameCumR+tournCumR);
			}
			
		}
		
	}

}
