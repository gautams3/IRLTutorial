package burlap.behavior.stochasticgames.agents.naiveq.history;

import burlap.behavior.policy.EpsilonGreedy;
import burlap.behavior.valuefunction.QFunction;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.statehashing.HashableStateFactory;
import burlap.mdp.stochasticgames.agent.AgentFactory;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.mdp.stochasticgames.SGDomain;


/**
 * An agent factory for Q-learning with history agents.
 * @author James MacGlashan
 *
 */
public class SGQWActionHistoryFactory implements AgentFactory {

	/**
	 * The stochastic games domain in which the agent will act
	 */
	protected SGDomain													domain;
	
	/**
	 * The discount rate the Q-learning algorithm will use
	 */
	protected double													discount;
	
	/**
	 * The learning rate the Q-learning algorithm will use
	 */
	protected double													learningRate;
	
	
	/**
	 * The state hashing factory the Q-learning algorithm will use
	 */
	protected HashableStateFactory stateHash;
	
	/**
	 * How much history the agent should remember
	 */
	protected int														historySize;
	
	/**
	 * The maximum number of players that can be in the game
	 */
	protected int														maxPlayers;

	/**
	 * A default Q-value initializer
	 */
	protected QFunction													qinit = null;
	
	/**
	 * The epislon value for epislon greedy policy. If negative, then the policy of the created agent
	 * will not be different than its default.
	 */
	protected double													epsilon = -1.;
	
	

	/**
	 * Initializes the factory
	 * @param d the stochastic games domain in which the agent will act
	 * @param discount The discount rate the Q-learning algorithm will use
	 * @param learningRate The learning rate the Q-learning algorithm will use
	 * @param stateHash The state hashing factory the Q-learning algorithm will use
	 * @param historySize How much history the agent should remember
	 */
	public SGQWActionHistoryFactory(SGDomain d, double discount, double learningRate, HashableStateFactory stateHash, int historySize) {
		this.domain = d;
		this.learningRate = learningRate;
		this.stateHash = stateHash;
		this.historySize = historySize;
	}
	
	/**
	 * Sets the Q-value initialization function that will be used by the agent.
	 * @param qinit the Q-value initialization function.
	 */
	public void setQValueInitializer(QFunction qinit){
		this.qinit = qinit;
	}
	
	/**
	 * Sets the epislon parmaeter (for epsilon greedy policy). If set to a negative, then the default policy of the create agent will be used.
	 * @param epsilon the epsilon value to use
	 */
	public void setEpsilon(double epsilon){
		this.epsilon = epsilon;
	}

	@Override
	public SGAgent generateAgent(String agentName, SGAgentType type) {
		SGQWActionHistory agent = new SGQWActionHistory(domain, discount, learningRate, stateHash, historySize)
				.setAgentDetails(agentName, type);

		if(this.qinit != null){
			agent.setQValueInitializer(qinit);
		}
		if(this.epsilon >= 0.){
			EpsilonGreedy egreedy = new EpsilonGreedy(agent, this.epsilon);
			agent.setStrategy(egreedy);
		}
		
		return agent;
		
	}

}
