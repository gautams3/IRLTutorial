package burlap.behavior.singleagent.learnfromdemo.mlirl.differentiableplanners;

import burlap.behavior.policy.BoltzmannQPolicy;
import burlap.behavior.singleagent.learnfromdemo.mlirl.differentiableplanners.dpoperator.DifferentiableSoftmaxOperator;
import burlap.behavior.singleagent.learnfromdemo.mlirl.support.DifferentiableRF;
import burlap.behavior.singleagent.planning.Planner;
import burlap.debugtools.DPrint;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.SADomain;
import burlap.mdp.singleagent.model.FullModel;
import burlap.mdp.singleagent.model.TransitionProb;
import burlap.statehashing.HashableState;
import burlap.statehashing.HashableStateFactory;

import java.util.*;

/**
 * Performs Differentiable Value Iteration using the Boltzmann backup operator and a
 * {@link burlap.behavior.singleagent.learnfromdemo.mlirl.support.DifferentiableRF}. This class
 * behaves the same as the normal {@link burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration}
 * valueFunction except for being in the differentiable value function case.
 * @author James MacGlashan.
 */
public class DifferentiableVI extends DifferentiableDP implements Planner {

	/**
	 * When the maximum change in the value function is smaller than this value, VI will terminate.
	 */
	protected double												maxDelta;

	/**
	 * When the number of VI iterations exceeds this value, VI will terminate.
	 */
	protected int													maxIterations;


	/**
	 * Indicates whether the reachable states has been computed yet.
	 */
	protected boolean												foundReachableStates = false;


	/**
	 * When the reachability analysis to find the state space is performed, a breadth first search-like pass
	 * (spreading over all stochastic transitions) is performed. It can optionally be set so that the
	 * search is pruned at terminal states by setting this value to true. By default, it is false and the full
	 * reachable state space is found
	 */
	protected boolean												stopReachabilityFromTerminalStates = false;


	/**
	 * Indicates whether VI has been run or not
	 */
	protected boolean												hasRunVI = false;


	protected double												boltzBeta;

	/**
	 * Initializes the valueFunction.
	 * @param domain the domain in which to plan
	 * @param rf the differentiable reward function that will be used
	 * @param gamma the discount factor
	 * @param boltzBeta the scaling factor in the boltzmann distribution used for the state value function. The larger the value, the more deterministic.
	 * @param hashingFactory the state hashing factor to use
	 * @param maxDelta when the maximum change in the value function is smaller than this value, VI will terminate.
	 * @param maxIterations when the number of VI iterations exceeds this value, VI will terminate.
	 */
	public DifferentiableVI(SADomain domain, DifferentiableRF rf, double gamma, double boltzBeta, HashableStateFactory hashingFactory, double maxDelta, int maxIterations){

		this.DPPInit(domain, gamma, hashingFactory);

		this.rf = rf;
		this.maxDelta = maxDelta;
		this.maxIterations = maxIterations;
		this.operator = new DifferentiableSoftmaxOperator(boltzBeta);
		this.boltzBeta = boltzBeta;

	}


	/**
	 * Calling this method will force the valueFunction to recompute the reachable states when the {@link #planFromState(State)} method is called next.
	 * This may be useful if the transition dynamics from the last planning call have changed and if planning needs to be restarted as a result.
	 */
	public void recomputeReachableStates(){
		this.foundReachableStates = false;
	}


	/**
	 * Sets whether the state reachability search to generate the state space will be prune the search from terminal states.
	 * The default is not to prune.
	 * @param toggle true if the search should prune the search at terminal states; false if the search should find all reachable states regardless of terminal states.
	 */
	public void toggleReachabiltiyTerminalStatePruning(boolean toggle){
		this.stopReachabilityFromTerminalStates = toggle;
	}



	/**
	 * Plans from the input state and returns a {@link burlap.behavior.policy.BoltzmannQPolicy} following the
	 * Boltzmann parameter used for value Botlzmann value backups in this planner.
	 * @param initialState the initial state of the planning problem
	 * @return a {@link burlap.behavior.policy.BoltzmannQPolicy}
	 */
	@Override
	public BoltzmannQPolicy planFromState(State initialState){
		if(!this.valueFunction.containsKey(this.hashingFactory.hashState(initialState))){
			this.performReachabilityFrom(initialState);
			this.runVI();
		}

		return new BoltzmannQPolicy(this, 1./this.boltzBeta);

	}

	@Override
	public void resetSolver(){
		super.resetSolver();
		this.foundReachableStates = false;
		this.hasRunVI = false;
	}

	/**
	 * Runs VI until the specified termination conditions are met. In general, this method should only be called indirectly through the {@link #planFromState(State)} method.
	 * The {@link #performReachabilityFrom(State)} must have been performed at least once
	 * in the past or a runtime exception will be thrown. The {@link #planFromState(State)} method will automatically call the {@link #performReachabilityFrom(State)}
	 * method first and then this if it hasn't been run.
	 */
	public void runVI(){

		if(!this.foundReachableStates){
			throw new RuntimeException("Cannot run VI until the reachable states have been found. Use the planFromState, performReachabilityFrom, addStateToStateSpace or addStatesToStateSpace methods at least once before calling runVI.");
		}

		Set<HashableState> states = valueFunction.keySet();

		int i;
		for(i = 0; i < this.maxIterations; i++){

			double delta = 0.;
			for(HashableState sh : states){

				double v = this.value(sh);
				double newV = this.performBellmanUpdateOn(sh);
				this.performDPValueGradientUpdateOn(sh);
				delta = Math.max(Math.abs(newV - v), delta);

			}

			if(delta < this.maxDelta){
				break; //approximated well enough; stop iterating
			}

		}

		DPrint.cl(this.debugCode, "Passes: " + i);

		this.hasRunVI = true;

	}


	/**
	 * Adds the given state to the state space over which VI iterates.
	 * @param s the state to add
	 */
	public void addStateToStateSpace(State s){
		HashableState sh = this.hashingFactory.hashState(s);
		this.valueFunction.put(sh, valueInitializer.value(s));
		this.foundReachableStates = true;
	}


	/**
	 * Adds a {@link java.util.Collection} of states over which VI will iterate.
	 * @param states the collection of states.
	 */
	public void addStatesToStateSpace(Collection<State> states){
		for(State s : states){
			this.addStateToStateSpace(s);
		}
	}

	/**
	 * This method will find all reachable states that will be used by the {@link #runVI()} method and will cache all the transition dynamics.
	 * This method will not do anything if all reachable states from the input state have been discovered from previous calls to this method.
	 * @param si the source state from which all reachable states will be found
	 * @return true if a reachability analysis had never been performed from this state; false otherwise.
	 */
	public boolean performReachabilityFrom(State si){



		HashableState sih = this.stateHash(si);

		DPrint.cl(this.debugCode, "Starting reachability analysis");

		//add to the open list
		LinkedList<HashableState> openList = new LinkedList<HashableState>();
		Set <HashableState> openedSet = new HashSet<HashableState>();
		openList.offer(sih);
		openedSet.add(sih);


		while(!openList.isEmpty()){
			HashableState sh = openList.poll();

			//skip this if it's already been expanded
			if(valueFunction.containsKey(sh)){
				continue;
			}

			//do not need to expand from terminal states if set to prune
			if(model.terminal(sh.s()) && stopReachabilityFromTerminalStates){
				continue;
			}

			valueFunction.put(sh, valueInitializer.value(sh.s()));

			List<Action> actions = this.applicableActions(sh.s());
			for(Action a : actions){
				List<TransitionProb> tps = ((FullModel)model).transitions(sh.s(), a);
				for(TransitionProb tp : tps){
					HashableState tsh = this.stateHash(tp.eo.op);
					if(!openedSet.contains(tsh) && !valueFunction.containsKey(tsh)){
						openedSet.add(tsh);
						openList.offer(tsh);
					}
				}
			}


		}

		DPrint.cl(this.debugCode, "Finished reachability analysis; # states: " + valueFunction.size());

		this.foundReachableStates = true;
		this.hasRunVI = false;

		return true;

	}


}
