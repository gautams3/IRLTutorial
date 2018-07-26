package burlap.mdp.singleagent.environment.extensions;

import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.environment.Environment;
import burlap.mdp.singleagent.environment.EnvironmentOutcome;

import java.util.LinkedList;
import java.util.List;

/**
 * A {@link EnvironmentServerInterface} implementation that delegates all {@link burlap.mdp.singleagent.environment.Environment} interactions and request
 * to a provided {@link burlap.mdp.singleagent.environment.Environment} delegate. This class will also
 * intercept all interactions through the {@link #executeAction(Action)} and
 * {@link #resetEnvironment()} methods
 * and tell all {@link burlap.mdp.singleagent.environment.EnvironmentOutcome} instances registered with this server
 * about the event.
 * @author James MacGlashan.
 */
public class EnvironmentServer implements EnvironmentServerInterface, EnvironmentDelegation {

	/**
	 * the {@link burlap.mdp.singleagent.environment.Environment} delegate that handles all primary {@link burlap.mdp.singleagent.environment.Environment}
	 * functionality.
	 */
	protected Environment delegate;

	/**
	 * The {@link EnvironmentObserver} objects that will be notified of {@link burlap.mdp.singleagent.environment.Environment}
	 * events.
	 */
	protected List<EnvironmentObserver> observers = new LinkedList<EnvironmentObserver>();


	/**
	 * If the input {@link burlap.mdp.singleagent.environment.Environment} is an instance {@link EnvironmentServerInterface},
	 * then all the input observers are added to it and it is returned. Otherwise, a new {@link EnvironmentServer}
	 * is created around it, with all of the observers added.
	 * @param env the {@link burlap.mdp.singleagent.environment.Environment} that will have observers added to it
	 * @param observers the {@link EnvironmentObserver} objects to add.
	 * @return the input {@link burlap.mdp.singleagent.environment.Environment} or an {@link EnvironmentServer}.
	 */
	public static EnvironmentServerInterface constructServerOrAddObservers(Environment env, EnvironmentObserver...observers){
		if(env instanceof EnvironmentServerInterface){
			((EnvironmentServerInterface)env).addObservers(observers);
			return (EnvironmentServerInterface)env;
		}
		else{
			return constructor(env, observers);
		}
	}

	/**
	 * Constructs an {@link EnvironmentServer} or {@link EnvironmentServer.StateSettableEnvironmentServer},
	 * based on whether the input delegate implements {@link StateSettableEnvironment}.
	 * @param delegate the delegate {@link burlap.mdp.singleagent.environment.Environment} for most environment interactions.
	 * @param observers the {@link EnvironmentObserver} objects notified of Environment events.
	 * @return an {@link EnvironmentServer} or {@link EnvironmentServer.StateSettableEnvironmentServer}.
	 */
	public static EnvironmentServer constructor(Environment delegate, EnvironmentObserver...observers){
		if(delegate instanceof StateSettableEnvironment){
			return new StateSettableEnvironmentServer((StateSettableEnvironment)delegate);
		}
		return new EnvironmentServer(delegate, observers);
	}

	public EnvironmentServer(Environment delegate, EnvironmentObserver...observers){
		this.delegate = delegate;
		for(EnvironmentObserver observer : observers){
			this.observers.add(observer);
		}
	}

	/**
	 * Returns the {@link burlap.mdp.singleagent.environment.Environment} delegate that handles all {@link burlap.mdp.singleagent.environment.Environment}
	 * functionality
	 * @return the {@link burlap.mdp.singleagent.environment.Environment} delegate
	 */
	public Environment getEnvironmentDelegate() {
		return delegate;
	}

	/**
	 * Sets the {@link burlap.mdp.singleagent.environment.Environment} delegate that handles all {@link burlap.mdp.singleagent.environment.Environment} functionality
	 * @param delegate  the {@link burlap.mdp.singleagent.environment.Environment} delegate
	 */
	public void setEnvironmentDelegate(Environment delegate) {
		this.delegate = delegate;
	}

	/**
	 * Adds one or more {@link EnvironmentObserver}s
	 * @param observers and {@link EnvironmentObserver}
	 */
	public void addObservers(EnvironmentObserver...observers){
		for(EnvironmentObserver observer : observers){
			this.observers.add(observer);
		}
	}

	/**
	 * Clears all {@link EnvironmentObserver}s from this server.
	 */
	public void clearAllObservers(){
		this.observers.clear();
	}

	/**
	 * Removes one or more {@link EnvironmentObserver}s from this server.
	 * @param observers the {@link EnvironmentObserver}s to remove.
	 */
	public void removeObservers(EnvironmentObserver...observers){
		for(EnvironmentObserver observer : observers){
			this.observers.remove(observer);
		}
	}


	/**
	 * Returns all {@link EnvironmentObserver}s registered with this server.
	 * @return all {@link EnvironmentObserver}s registered with this server.
	 */
	public List<EnvironmentObserver> observers(){
		return this.observers;
	}


	@Override
	public State currentObservation() {
		return this.delegate.currentObservation();
	}

	@Override
	public EnvironmentOutcome executeAction(Action ga) {
		for(EnvironmentObserver observer : this.observers){
			observer.observeEnvironmentActionInitiation(this.delegate.currentObservation(), ga);
		}
		EnvironmentOutcome eo = this.delegate.executeAction(ga);
		for(EnvironmentObserver observer : this.observers){
			observer.observeEnvironmentInteraction(eo);
		}
		return eo;
	}

	@Override
	public double lastReward() {
		return this.delegate.lastReward();
	}

	@Override
	public boolean isInTerminalState() {
		return this.delegate.isInTerminalState();
	}

	@Override
	public void resetEnvironment() {
		this.delegate.resetEnvironment();
		for(EnvironmentObserver observer : this.observers){
			observer.observeEnvironmentReset(this.delegate);
		}
	}


	public static class StateSettableEnvironmentServer extends EnvironmentServer implements StateSettableEnvironment{

		public StateSettableEnvironmentServer(StateSettableEnvironment delegate, EnvironmentObserver... observers) {
			super(delegate, observers);
		}

		@Override
		public void setCurStateTo(State s) {
			((StateSettableEnvironment)this.delegate).setCurStateTo(s);
		}
	}
}
