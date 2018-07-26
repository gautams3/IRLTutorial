package burlap.behavior.singleagent.learning.actorcritic.critics;

import burlap.behavior.learningrate.ConstantLR;
import burlap.behavior.learningrate.LearningRate;
import burlap.behavior.singleagent.MDPSolver;
import burlap.behavior.singleagent.learning.actorcritic.Critic;
import burlap.behavior.singleagent.options.EnvironmentOptionOutcome;
import burlap.behavior.singleagent.options.Option;
import burlap.behavior.valuefunction.ConstantValueFunction;
import burlap.behavior.valuefunction.ValueFunction;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.environment.EnvironmentOutcome;
import burlap.statehashing.HashableState;
import burlap.statehashing.HashableStateFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * An implementation of TDLambda that can be used as a critic for {@link burlap.behavior.singleagent.learning.actorcritic.ActorCritic} algorithms [1].
 * 
 * <p>
 * 1. Barto, Andrew G., Steven J. Bradtke, and Satinder P. Singh. "Learning to act using real-time dynamic programming." Artificial Intelligence 72.1 (1995): 81-138.
 * @author James MacGlashan
 *
 */
public class TDLambda extends MDPSolver implements Critic, ValueFunction {

	protected LearningRate							learningRate;
	
	/**
	 * Defines how the value function is initialized for unvisited states
	 */
	protected ValueFunction							vInitFunction;
	
	/**
	 * Indicates the strength of eligibility traces. Use 1 for Monte-carlo-like traces and 0 for single step backups
	 */
	protected double								lambda;
	
	
	/**
	 * The state value function.
	 */
	protected Map<HashableState, VValue>			vIndex;
	
	/**
	 * The eligibility traces for the current episode.
	 */
	protected LinkedList<StateEligibilityTrace>		traces;
	
	
	/**
	 * The total number of learning steps performed by this agent.
	 */
	protected int													totalNumberOfSteps = 0;
	
	
	/**
	 * Initializes the algorithm.
	 * @param gamma the discount factor
	 * @param hashingFactory the state hashing factory to use for hashing states and performing equality checks. 
	 * @param learningRate the learning rate that affects how quickly the estimated value function is adjusted.
	 * @param vinit a constant value function initialization value to use.
	 * @param lambda indicates the strength of eligibility traces. Use 1 for Monte-carlo-like traces and 0 for single step backups
	 */
	public TDLambda(double gamma, HashableStateFactory hashingFactory, double learningRate, double vinit, double lambda) {

		this.solverInit(null, gamma, hashingFactory);
		
		this.learningRate = new ConstantLR(learningRate);
		vInitFunction = new ConstantValueFunction(vinit);
		this.lambda = lambda;
		
		
		vIndex = new HashMap<HashableState, VValue>();
	}
	
	
	
	/**
	 * Initializes the algorithm.
	 * @param gamma the discount factor
	 * @param hashingFactory the state hashing factory to use for hashing states and performing equality checks. 
	 * @param learningRate the learning rate that affects how quickly the estimated value function is adjusted.
	 * @param vinit a method of initializing the value function for previously unvisited states.
	 * @param lambda indicates the strength of eligibility traces. Use 1 for Monte-carlo-like traces and 0 for single step backups
	 */
	public TDLambda(double gamma, HashableStateFactory hashingFactory, double learningRate, ValueFunction vinit, double lambda) {
		this.gamma = gamma;
		this.hashingFactory = hashingFactory;
		
		this.learningRate = new ConstantLR(learningRate);
		vInitFunction = vinit;
		this.lambda = lambda;
		
		
		vIndex = new HashMap<HashableState, VValue>();
	}




	@Override
	public void startEpisode(State s) {
		this.traces = new LinkedList<TDLambda.StateEligibilityTrace>();
	}

	@Override
	public void endEpisode() {
		this.traces.clear();
	}
	
	/**
	 * Sets the learning rate function to use.
	 * @param lr the learning rate function to use.
	 */
	public void setLearningRate(LearningRate lr){
		this.learningRate = lr;
	}
	
	@Override
	public double critique(EnvironmentOutcome eo) {
		
		HashableState sh = hashingFactory.hashState(eo.o);
		HashableState shprime = hashingFactory.hashState(eo.op);
		
		double r = eo.r;
		double discount = gamma;
		if(eo.a instanceof Option){
			discount = Math.pow(gamma, ((EnvironmentOptionOutcome)eo).numSteps());
		}
		
		VValue vs = this.getV(sh);
		double nextV = 0.;
		if(!eo.terminated){
			nextV = this.getV(shprime).v;
		}
		
		double delta = r + discount*nextV - vs.v;
		
		//update all traces
		boolean foundTrace = false;
		for(StateEligibilityTrace t : traces){
			
			if(t.sh.equals(sh)){
				foundTrace = true;
				t.eligibility = 1.;
			}
			
			double learningRate = this.learningRate.pollLearningRate(this.totalNumberOfSteps, t.sh.s(), null);
			t.v.v = t.v.v + learningRate * delta * t.eligibility;
			t.eligibility = t.eligibility * lambda * discount;
		}
		
		if(!foundTrace){
			//then add it
			double learningRate = this.learningRate.pollLearningRate(this.totalNumberOfSteps, sh.s(), null);
			vs.v = vs.v + learningRate * delta;
			StateEligibilityTrace t = new StateEligibilityTrace(sh, discount*this.lambda, vs);
			
			traces.add(t);
		}
		

		
		this.totalNumberOfSteps++;
		
		return delta;
	}


	@Override
	public double value(State s) {
		return this.getV(this.hashingFactory.hashState(s)).v;
	}

	@Override
	public void resetSolver() {
		this.reset();
	}

	@Override
	public void reset(){
		this.vIndex.clear();
		this.traces.clear();
		this.learningRate.resetDecay();
	}

	
	/**
	 * Returns the {@link TDLambda.VValue} object (storing the value) for a given hashed stated.
	 * @param sh the hased state for which the value should be returned.
	 * @return the {@link TDLambda.VValue} object (storing the value) for the given hashed stated.
	 */
	protected VValue getV(HashableState sh){
		VValue v = this.vIndex.get(sh);
		if(v == null){
			v = new VValue(this.vInitFunction.value(sh.s()));
			this.vIndex.put(sh, v);
		}
		return v;
	}
	
	
	
	/**
	 * A class for storing the value of a state. This is effectively a mutable double value wrapper.
	 * @author James MacGlashan
	 *
	 */
	class VValue{
		
		/**
		 * The value to store
		 */
		public double v;
		
		/**
		 * Initializes with a given value
		 * @param v the value to store
		 */
		public VValue(double v){
			this.v = v;
		}

	}
	
	
	/**
	 * A data structure for storing the elements of an eligibility trace.
	 * @author James MacGlashan
	 *
	 */
	public static class StateEligibilityTrace{
		
		/**
		 * The eligibility value
		 */
		public double			eligibility;
		
		/**
		 * The hashed state with which the eligibility value is associated.
		 */
		public HashableState sh;
		
		/**
		 * The value associated with the state.
		 */
		public VValue			v;

		
		/**
		 * Initializes with hashed state, eligibility value and the value function value associated with the state.
		 * @param sh the hashed input state for this eligibility
		 * @param eligibility the eligibility of the state
		 * @param v the value function value for the state.
		 */
		public StateEligibilityTrace(HashableState sh, double eligibility, VValue v){
			this.sh = sh;
			this.eligibility = eligibility;
			this.v = v;
		}
		
		
		
	}


	

}
