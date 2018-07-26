package burlap.mdp.core.oo.state;

import burlap.mdp.core.state.MutableState;

/**
 * A {@link MutableState} extension {@link OOState}.
 * Allows adding new objects ({@link ObjectInstance}), removing them, and renaming them.
 * @author James MacGlashan.
 */
public interface MutableOOState extends OOState, MutableState{

	/**
	 * Adds object instance o to this state.
	 * @param o the object instance to be added to this state.
	 * @return the modified state
	 */
	MutableOOState addObject(ObjectInstance o);


	/**
	 * Removes the object instance with the name oname from this state.
	 * @param oname the name of the object instance to remove.
	 * @return the modified state
	 */
	MutableOOState removeObject(String oname);


	/**
	 * Renames the identifier for object instance o in this state to newName.
	 * @param objectName the current object name
	 * @param newName the new name of the object instance
	 * @return the modified state
	 */
	MutableOOState renameObject(String objectName, String newName);

}
