package burlap.behavior.valuefunction;

import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;


/**
 * This class is used to store Q-values.
 * @author James MacGlashan
 *
 */
public class QValue {
	
	/**
	 * The state with which this Q-value is associated.
	 */
	public State 						s;
	
	/**
	 * The action with which this Q-value is associated
	 */
	public Action a;
	
	/**
	 * The numeric Q-value
	 */
	public double						q;


	/**
	 * Default constructor for serialization purposes.
	 */
	public QValue() {
	}

	/**
	 * Creates a Q-value for the given state an action pair with the specified q-value
	 * @param s the state
	 * @param a the action
	 * @param q the initial Q-value
	 */
	public QValue(State s, Action a, double q){
		this.s = s;
		this.a = a;
		this.q = q;
	}
	
	
	/**
	 * Initialializes this Q-value by copying the information from another Q-value.
	 * @param src the source Q-value from which to copy.
	 */
	public QValue(QValue src){
		this.s = src.s.copy();
		this.a = src.a.copy();
		this.q = src.q;
	}
	
}
