package burlap.mdp.core.oo.propositional;

import burlap.mdp.core.oo.state.OOState;
import burlap.mdp.core.oo.state.OOStateUtilities;
import burlap.mdp.core.state.State;

import java.util.ArrayList;
import java.util.List;


/**
 * The propositional function class defines evaluations of object instances in an OO-MDP state and are part of the definition for an OO-MDP domain.
 * Propositional functions have an name to identify them and a set of typed parameters that they can take. Implementing
 * this class requires implementing the abstract method {@link #isTrue(OOState, String...)}, which takes an input state,
 * identifiers to one or more objects in the state, and returns true or false depending on whether the relationship
 * of the object arguments this propositional function evaluates are satisfied.
 * <br><br>
 * If you want to determine all possible parameter bindings on which a PropositionalFunction's {@link #isTrue(OOState, String...)}
 * method can be evaluated, use the {@link #allGroundings(OOState)} method, which takes as input an {@link OOState}, and detemrines
 * which combinations of objects in the state can be applied to this object's typed parameters. The returned list will be a list of
 * {@link GroundedProp} objects, where each {@link GroundedProp} object contains a reference to this {@link PropositionalFunction}
 * and a list of the object reference on which to evaluate it. Similarly, if you want all possible groundings for a list of different
 * {@link PropositionalFunction} objects, you can use the static {@link #allGroundingsFromList(List, OOState)} method, which
 * calls the {@link #allGroundings(OOState)} method on each of the {@link PropositionalFunction} objects in the list, and concatenates
 * their results into a single List of {@link GroundedProp} objects.
 * <br><br>
 * Optionally, the parameters of a propositional function may also be assigned to order groups that indicate any symmetry in the
 * evaluation of the propositional function. For example, suppose we were to define a "touching" propositional function that specified whether
 * two objects of a class representing blocks (called BLOCK) were touching in the world or not. Consider then, evaluating
 * touching(block0, block1). Note that there is a symmetry in this evaluation; that is, touching(block0, block1) = touching(block1, block0) in which
 * case there is no point to evaluate both argument orders, because evaluating one tells you the value of hte other.
 * Specifying parameter order groups allows the designer to indicate these
 * kinds of symmetry. If two parameters are set to the same order group, then it indicates that the evaluation for the propositional function
 * will be the same for any order of parameters within that order group. If the order group between two parameters is different, then it indicates
 * that the order of the parameters can change the result of the evaluation. So, in the previous touching example, if the parameter order groups of the two parameters
 * were both "p0", then it indicates that touching(block0, block1) = touching(block1, block0); if one parameter was assigned to order group "p0" and the other
 * to "p1", then it is possible that touching(block0, block1) != touching(block1, block0). If the parameter order groups of parameters
 * are not specified, then they are all assumed to be different. Any specified parameter order groups will be checked in the
 * {@link #allGroundings(OOState)} method, so that different groundings that necessarily have the same value will only have
 * one of the groundings included in the returned list.
 * @author James MacGlashan
 *
 */
public abstract class PropositionalFunction {

	/*
	 * name of the propositional function
	 */
	protected String					name;

	
	/*
	 * list of class names for each parameter of the function
	 */
	protected String []					parameterClasses;
	
	/*
	 * Defines symmetry between parameters. For example, setting two or more parameters to the same order group 
	 * indicates that the function evaluate the same regardless of which specific object is set to each parameter
	 */
	protected String []					parameterOrderGroup;

	
	

	
	/**
	 * Initializes a propositional function with the given name and parameter object classes. Unique parameter order groups
	 * for each parameter are assumed; that is, the order of parameters always matters.
	 * @param name the name of the propositional function
	 * @param parameterClasses an array of strings specifying the name of object classes that the parameters must satisfy.
	 */
	public PropositionalFunction(String name, String [] parameterClasses){
		
		String [] rcn = new String[parameterClasses.length];
		for(int i = 0; i < rcn.length; i++){
			rcn[i] = name + ".P" + i;
		}
		
		this.init(name, parameterClasses, rcn);
		
	}

	
	/**
	 * Initializes a propositional function with the given name, parameter object classes, and the parameter order groups of the parameters.
	 * @param name the name of the propositional function
	 * @param parameterClasses an array of strings specifying the name of object classes that the parameters must satisfy.
	 * @param parameterOrderGroup an array of strings specifying order group names for the parameters
	 */
	public PropositionalFunction(String name, String [] parameterClasses, String [] parameterOrderGroup){
		this.init(name, parameterClasses, parameterOrderGroup);
	}

	
	
	protected final void init(String name, String [] parameterClasses, String [] parameterOrderGroup){
		this.name = name;
		this.parameterClasses = parameterClasses;
		this.parameterOrderGroup = parameterOrderGroup;
	}
	
	
	/**
	 * Returns the name of this propositional function.
	 * @return the name of this propositional function.
	 */
	public final String getName(){
		return name;
	}
	
	/**
	 * Returns the object classes of the parameters for this propositional function
	 * @return the object classes of the parameters for this propositional function
	 */
	public final String[] getParameterClasses(){
		return parameterClasses;
	}
	
	/**
	 * Returns the parameter order group names for the parameters of this propositional function.
	 * @return the parameter order group names for the parameters of this propositional function.
	 */
	public final String[] getParameterOrderGroups(){
		return parameterOrderGroup;
	}

	
	/**
	 * Returns whether the propositional function is true for the given state with the given parameters
	 * This version is preferred to the comma delimited version.
	 * @param s the state that is being checked
	 * @param params the parameters being passed in to the propositional function
	 * @return whether the propositional function is true
	 */
	public abstract boolean isTrue(OOState s, String... params);

	
	
	/**
	 * Returns all possible groundings of this {@link PropositionalFunction} for the given {@link State}
	 * @param s the {@link State} in which all groundings will be returned
	 * @return a {@link List} of all possible groundings of this {@link PropositionalFunction} for the given {@link State}
	 */
	public List<GroundedProp> allGroundings(OOState s){
		
		List <GroundedProp> res = new ArrayList<GroundedProp>();
		
		if(this.getParameterClasses().length == 0){
			res.add(new GroundedProp(this, new String[]{}));
			return res; //no parameters so just the single gp without params
		}

		if(!(s instanceof OOState)){
			throw new RuntimeException("Cannot generate all GroundedProp objects for state " + s.getClass().getName() + " because it does not implement OOState");
		}

		List <List <String>> bindings = OOStateUtilities.getPossibleBindingsGivenParamOrderGroups((OOState)s, this.getParameterClasses(), this.getParameterOrderGroups());
		
		for(List <String> params : bindings){
			String [] aprams = params.toArray(new String[params.size()]);
			GroundedProp gp = new GroundedProp(this, aprams);
			res.add(gp);
		}
		
		return res;

	}
	
	/**
	 * Returns true if there existing a {@link GroundedProp} for the provided {@link State} that is in true in the {@link State}.
	 * @param s the {@link State} to test.
	 * @return true if there existing a {@link GroundedProp} for the provided {@link State} that is in true in the {@link State}; false otherwise.
	 */
	public boolean someGroundingIsTrue(OOState s){
		List<GroundedProp> gps = this.allGroundings(s);
		for(GroundedProp gp : gps){
			if(gp.isTrue(s)){
				return true;
			}
		}
		
		return false;
	}
	
	
	
	@Override
	public boolean equals(Object obj){
		PropositionalFunction op = (PropositionalFunction)obj;
		if(op.name.equals(name))
			return true;
		return false;
	}
	
	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public int hashCode(){
		return name.hashCode();
	}



	/**
	 * Returns all possible groundings for all of the {@link PropositionalFunction}s in the provided list for the given {@link State}.
	 * @param pfs The list of {@link PropositionalFunction}s for which all groundings will be returned.
	 * @param s the {@link State} in which the groundings should be produced.
	 * @return a {@link List} of all possible groundings for all of the {@link PropositionalFunction}s in the provided list for the given {@link State}
	 */
	public static List<GroundedProp> allGroundingsFromList(List<PropositionalFunction> pfs, OOState s){
		List<GroundedProp> res = new ArrayList<GroundedProp>();
		for(PropositionalFunction pf : pfs){
			List<GroundedProp> gps = pf.allGroundings(s);
			res.addAll(gps);
		}
		return res;
	}


	/**
	 * Used to retrieve a {@link PropositionalFunction} with a specified name from a list of them.
	 * @param pfs The list of {@link PropositionalFunction} objects to search.
	 * @param pfName The name of the {@link PropositionalFunction}
	 * @return the {@link PropositionalFunction} with the name or null if it does not exist
	 */
	public static PropositionalFunction findPF(List<PropositionalFunction> pfs, String pfName){
		for(PropositionalFunction pf : pfs){
			if(pf.getName().equals(pfName)){
				return pf;
			}
		}
		return null;
	}

	
}
