package burlap.behavior.singleagent.learnfromdemo.mlirl.differentiableplanners.diffvinit;

import burlap.behavior.functionapproximation.FunctionGradient;
import burlap.behavior.functionapproximation.ParametricFunction;
import burlap.behavior.singleagent.learnfromdemo.mlirl.support.DifferentiableRF;
import burlap.behavior.valuefunction.ValueFunction;
import burlap.mdp.core.state.State;

/**
 * A class for the default condition when a value function initialization returns an unparameterized  value
 * for each state, but must be differentiable
 * with respect to the reward function parameters for use with a differentiable finite horizon valueFunction.
 * @author James MacGlashan.
 */
public class VanillaDiffVinit implements DifferentiableVInit {


	/**
	 * The source value function initialization.
	 */
	protected ValueFunction vinit;

	/**
	 * The differentiable reward function that defines the parameter space over which this value function
	 * initialization must differentiate.
	 */
	protected DifferentiableRF rf;


	/**
	 * Initializes.
	 * @param vinit The vanilla unparameterized value function initialization
	 * @param rf the differentiable reward function that defines the total parameter space
	 */
	public VanillaDiffVinit(ValueFunction vinit, DifferentiableRF rf) {
		this.vinit = vinit;
		this.rf = rf;
	}

	@Override
	public int numParameters() {
		return this.rf.numParameters();
	}

	@Override
	public double getParameter(int i) {
		return this.rf.getParameter(i);
	}

	@Override
	public void setParameter(int i, double p) {
		this.rf.setParameter(i, p);
	}

	@Override
	public void resetParameters() {
		this.rf.resetParameters();
	}

	@Override
	public ParametricFunction copy() {
		return new VanillaDiffVinit(vinit, rf);
	}

	@Override
	public FunctionGradient valueGradient(State s) {
		return new FunctionGradient.SparseGradient();
	}

	@Override
	public double value(State s) {
		return this.vinit.value(s);
	}


}
