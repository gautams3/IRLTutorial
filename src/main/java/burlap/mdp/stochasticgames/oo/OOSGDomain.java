package burlap.mdp.stochasticgames.oo;

import burlap.mdp.core.oo.OODomain;
import burlap.mdp.core.oo.propositional.PropositionalFunction;
import burlap.mdp.stochasticgames.SGDomain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class OOSGDomain extends SGDomain implements OODomain {

	protected Map<String, Class<?>> stateClassesMap = new HashMap<String, Class<?>>();

	protected Map<String, PropositionalFunction> propFunctionMap = new HashMap<String, PropositionalFunction>();

	@Override
	public List<Class<?>> stateClasses() {
		return new ArrayList<Class<?>>(stateClassesMap.values());
	}

	@Override
	public Class<?> stateClass(String className) {
		return stateClassesMap.get(className);
	}

	@Override
	public OOSGDomain addStateClass(String className, Class<?> stateClass) {
		this.stateClassesMap.put(className, stateClass);
		return this;
	}

	@Override
	public List<PropositionalFunction> propFunctions() {
		return new ArrayList<PropositionalFunction>(this.propFunctionMap.values());
	}

	@Override
	public PropositionalFunction propFunction(String name) {
		return this.propFunctionMap.get(name);
	}

	@Override
	public OOSGDomain addPropFunction(PropositionalFunction prop) {
		this.propFunctionMap.put(prop.getName(), prop);
		return this;
	}
}
