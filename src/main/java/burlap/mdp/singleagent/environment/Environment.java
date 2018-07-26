package burlap.mdp.singleagent.environment;

import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;


/**
 * Environments define a current observation represetned with a {@link State} and manage state and reward transitions when an action is executed in the environment through
 * the {@link #executeAction(Action)} method. {@link burlap.mdp.singleagent.environment.Environment}
 * instances are what learning algorithms implementing {@link burlap.behavior.singleagent.learning.LearningAgent} interact with.
 * Maintaining an Environment ensures that transitions are protected from an agent manipulating the state
 * and are also useful when a BURLAP agent is interacting with external or real time systems such as robotics. Environments
 * also make it easy to use a planning algorithm to compute a {@link burlap.behavior.policy.Policy} using some model of the world
 * and then have that policy executed in an {@link burlap.mdp.singleagent.environment.Environment} that may behave differently
 * than the model (e.g., robotics operating in the real world). {@link burlap.mdp.singleagent.environment.Environment} implementations
 * also make it easy to train a {@link burlap.behavior.singleagent.learning.LearningAgent} in one {@link burlap.mdp.singleagent.environment.Environment}
 * and then use them in a new {@link burlap.mdp.singleagent.environment.Environment} after learning.
 * <p>
 * If you wish to use a simulated BURLAP {@link burlap.mdp.core.Domain} to manage the transitions and reward function, you should
 * consider using the {@link burlap.mdp.singleagent.environment.SimulatedEnvironment} implementation.
 * 
 * @author James MacGlashan
 *
 */
public interface Environment {

	
	/**
	 * Returns the current observation of the environment as a {@link State}.
	 * @return the current observation of the environment as a {@link State}.
	 */
	State currentObservation();


	/**
	 * Executes the specified action in this environment
	 * @param a the Action that is to be performed in this environment.
	 * @return the resulting observation and reward transition from applying the given GroundedAction in this environment.
	 */
	EnvironmentOutcome executeAction(Action a);
	

	
	/**
	 * Returns the last reward returned by the environment
	 * @return  the last reward returned by the environment
	 */
	double lastReward();
	
	/**
	 * Returns whether the environment is in a terminal state that prevents further action by the agent.
	 * @return true if the current environment is in a terminal state; false otherwise.
	 */
	boolean isInTerminalState();


	/**
	 * Resets this environment to some initial state, if the functionality exists.
	 */
	void resetEnvironment();
	
}
