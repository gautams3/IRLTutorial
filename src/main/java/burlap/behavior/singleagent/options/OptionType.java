package burlap.behavior.singleagent.options;

import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.core.action.ActionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An {@link ActionType} for generating a specific unparameterized option. The {@link #allApplicableActions(State)}
 * method checks the option initiation set via its {@link Option#inInitiationSet(State)} method
 * and returns a list containing the option only if it is satisfied.
 * @author James MacGlashan.
 */
public class OptionType implements ActionType {

	Option o;

	public OptionType(Option o) {
		this.o = o;
	}

	@Override
	public String typeName() {
		return o.actionName();
	}

	@Override
	public Action associatedAction(String strRep) {
		return o;
	}

	@Override
	public List<Action> allApplicableActions(State s) {
		if(o.inInitiationSet(s)){
			return Arrays.<Action>asList(o);
		}
		return new ArrayList<Action>();
	}
}
