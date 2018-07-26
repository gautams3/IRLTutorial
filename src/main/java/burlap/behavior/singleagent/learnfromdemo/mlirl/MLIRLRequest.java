package burlap.behavior.singleagent.learnfromdemo.mlirl;

import burlap.behavior.singleagent.Episode;
import burlap.behavior.singleagent.learnfromdemo.IRLRequest;
import burlap.behavior.singleagent.learnfromdemo.mlirl.differentiableplanners.DifferentiableVI;
import burlap.behavior.singleagent.learnfromdemo.mlirl.support.DifferentiableRF;
import burlap.behavior.singleagent.learnfromdemo.mlirl.support.DifferentiableQFunction;
import burlap.behavior.singleagent.planning.Planner;
import burlap.mdp.singleagent.SADomain;
import burlap.statehashing.HashableStateFactory;

import java.util.List;

/**
 * A request object for Maximum-Likelihood Inverse Reinforcement Learning ({@link burlap.behavior.singleagent.learnfromdemo.mlirl.MLIRL}).
 * This request adds a set of optionally specified weights on the expert trajectories, the {@link burlap.behavior.singleagent.learnfromdemo.mlirl.support.DifferentiableRF}
 * to use, and the Boltzmann beta parameter used for Differentiable planning. The larger the beta value, the more
 * deterministic the expert trajectories are assumed to be.
 * <p>
 * If no expert trajectory weights are provided,
 * then they will all be assumed to have a weight of 1. Calls to the {@link #getEpisodeWeights()} method when weights have
 * not been specified will result in a new double array being created and returned with the value 1.0 everywhere, so changes
 * to the returned array will not change the weights actually used. Instead, modify the weights using the {@link #setEpisodeWeights(double[])}
 * method.
 * @author James MacGlashan.
 */
public class MLIRLRequest extends IRLRequest{

	/**
	 * The weight assigned to each episode. If null, all episodes will be assumed to have equal weight
	 */
	protected double [] episodeWeights = null;


	/**
	 * The parameter used in the boltzmann policy that affects how noisy the expert is assumed to be.
	 * The smaller the parameter, the more noisy the expert behavior is assumed; the larger the more correct.
	 */
	protected double boltzmannBeta = 0.5;


	/**
	 * The differentiable reward function model that will be estimated by MLRIL.
	 */
	protected DifferentiableRF rf;


	/**
	 * Initializes the request without any expert trajectory weights (which will be assumed to have a value 1).
	 * If the provided valueFunction is not null and does not implement the {@link DifferentiableQFunction}
	 * interface, an exception will be thrown.
	 * @param domain the domain in which trajectories are provided.
	 * @param planner a valueFunction that implements the {@link DifferentiableQFunction} interface.
	 * @param expertEpisodes the expert episodes/trajectories to use for training.
	 * @param rf the {@link burlap.behavior.singleagent.learnfromdemo.mlirl.support.DifferentiableRF} model to use.
	 */
	public MLIRLRequest(SADomain domain, Planner planner, List<Episode> expertEpisodes, DifferentiableRF rf){
		super(domain, planner, expertEpisodes);
		if(planner != null && !(planner instanceof DifferentiableQFunction)){
			throw new RuntimeException("Error: MLIRLRequest requires the valueFunction to be an instance of QGradientPlanner");
		}
		this.rf = rf;
	}


	/**
	 * Initializes without any expert trajectory weights (which will be assumed to have a value 1) and requests
	 * a default {@link DifferentiableQFunction} instance to be created using
	 * the {@link burlap.statehashing.HashableStateFactory} provided. The
	 * {@link DifferentiableQFunction} instance will be
	 * a {@link burlap.behavior.singleagent.learnfromdemo.mlirl.differentiableplanners.DifferentiableVI} that plans
	 * either until the maximum change is the value function is no greater than 0.01 or until 500 iterations have been performed.
	 * A default gamma (discount) value of 0.99 will be used for the valueFunction and no terminal states will be used.
	 * @param domain the domain in which trajectories are provided.
	 * @param expertEpisodes the expert episodes/trajectories to use for training.
	 * @param rf the {@link burlap.behavior.singleagent.learnfromdemo.mlirl.support.DifferentiableRF} model to use.
	 * @param hashingFactory the state hashing factory to use for the created valueFunction.
	 */
	public MLIRLRequest(SADomain domain, List<Episode> expertEpisodes, DifferentiableRF rf, HashableStateFactory hashingFactory){
		super(domain, null, expertEpisodes);
		this.rf = rf;
		this.planner = new DifferentiableVI(domain, rf, gamma, this.boltzmannBeta, hashingFactory, 0.01, 500);

	}





	@Override
	public boolean isValid(){

		if(!super.isValid()){
			return false;
		}

		if(this.episodeWeights != null && this.episodeWeights.length != this.expertEpisodes.size()){
			return false;
		}

		if(!(planner instanceof DifferentiableQFunction)){
			return false;
		}

		if(this.rf == null){
			return false;
		}

		return true;

	}


	@Override
	public void setPlanner(Planner p) {
		if(planner != null && !(p instanceof DifferentiableQFunction)){
			throw new RuntimeException("Error: MLIRLRequest requires the valueFunction to be an instance of QGradientPlanner");
		}
		this.planner = p;
	}


	/**
	 * Returns expert episodes weights. If no specific weights have been set, a new double array the same length
	 * as the number of expert episodes with a constant value of 1 will be created and returned.
	 * @return expert episodes weights
	 */
	public double[] getEpisodeWeights() {
		if(this.episodeWeights == null){
			double [] weights = new double[this.expertEpisodes.size()];
			for(int i = 0; i < weights.length; i++){
				weights[i] = 1.;
			}
			return weights;
		}
		return episodeWeights;
	}

	public double getBoltzmannBeta() {
		return boltzmannBeta;
	}

	public DifferentiableRF getRf() {
		return rf;
	}

	public void setEpisodeWeights(double[] episodeWeights) {
		this.episodeWeights = episodeWeights;
	}

	public void setBoltzmannBeta(double boltzmannBeta) {
		this.boltzmannBeta = boltzmannBeta;
	}

	public void setRf(DifferentiableRF rf) {
		this.rf = rf;
	}




}
