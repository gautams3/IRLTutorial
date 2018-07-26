package burlap.mdp.auxiliary.stateconditiontest;

import burlap.mdp.core.state.State;


/**
 * And interface for defining classes that check for certain conditions in states. These are useful
 * for specifying binary goal conditions for classic search-based planners like A*
 * @author James MacGlashan
 *
 */
public interface StateConditionTest {

	public boolean satisfies(State s);
	
	
}
