package burlap.behavior.singleagent.learnfromdemo.apprenticeship;

import burlap.behavior.functionapproximation.dense.DenseStateFeatures;
import burlap.behavior.singleagent.Episode;
import burlap.behavior.singleagent.learnfromdemo.IRLRequest;
import burlap.behavior.singleagent.planning.Planner;
import burlap.mdp.auxiliary.StateGenerator;
import burlap.mdp.singleagent.SADomain;

import java.util.ArrayList;
import java.util.List;


/**
 * A data structure for setting all the parameters of Max Margin Apprenticeship learning.
 * 
 * 
 * @author Stephen Brawner and Mark Ho
 *
 */
public class ApprenticeshipLearningRequest extends IRLRequest{

	
	/**
	 * The state feature generator that turns a state into a feature vector on which the reward function is assumed to be modeled
	 */
	protected DenseStateFeatures featureGenerator;
	
	/**
	 * The initial state generator that models the initial states from which the expert trajectories were drawn
	 */
	protected StateGenerator 						startStateGenerator;

	
	/**
	 * The maximum feature score to cause termination of Apprenticeship learning
	 */
	protected double 								epsilon;
	
	/**
	 * The maximum number of iterations of apprenticeship learning
	 */
	protected int 									maxIterations;
	
	/**
	 * The maximum number of times a policy is rolled out and evaluated
	 */
	protected int 									policyCount;
	
	/**
	 * the history of scores across each reward function improvement
	 */
	protected double[] 								tHistory;
	
	/**
	 * If true, use the full max margin method (expensive); if false, use the cheaper projection method 
	 */
	protected boolean 								useMaxMargin;


	public static final double 			DEFAULT_EPSILON = 0.01;
	public static final int 			DEFAULT_MAXITERATIONS = 100;
	public static final int 			DEFAULT_POLICYCOUNT = 5;
	public static final boolean 		DEFAULT_USEMAXMARGIN = false;

	public ApprenticeshipLearningRequest() {
		super();
		this.initDefaults();
	}

	public ApprenticeshipLearningRequest(SADomain domain, Planner planner, DenseStateFeatures featureGenerator, List<Episode> expertEpisodes, StateGenerator startStateGenerator) {
		super(domain, planner, expertEpisodes);
		this.initDefaults();
		this.setFeatureGenerator(featureGenerator);
		this.setStartStateGenerator(startStateGenerator);
	}

	protected void initDefaults() {
		this.epsilon = ApprenticeshipLearningRequest.DEFAULT_EPSILON;
		this.maxIterations = ApprenticeshipLearningRequest.DEFAULT_MAXITERATIONS;
		this.policyCount = ApprenticeshipLearningRequest.DEFAULT_POLICYCOUNT;
		this.useMaxMargin = ApprenticeshipLearningRequest.DEFAULT_USEMAXMARGIN;
	}

	@Override
	public boolean isValid() {

		if(!super.isValid()){
			return false;
		}

		if (this.featureGenerator == null) {
			return false;
		}
		if (this.startStateGenerator == null) {
			return false;
		}
		if (this.epsilon < 0 || Double.isNaN(this.epsilon)) {
			return false;
		}
		if (this.maxIterations <= 0) {
			return false;
		}
		if (this.policyCount <= 0) {
			return false;
		}
		return true;
	}




	public void setFeatureGenerator(DenseStateFeatures stateFeaturesGenerator) {
		this.featureGenerator = stateFeaturesGenerator;
	}

	public void setExpertEpisodes(List<Episode> episodeList) {
		this.expertEpisodes = new ArrayList<Episode>(episodeList);
	}

	public void setStartStateGenerator(StateGenerator startStateGenerator) { this.startStateGenerator = startStateGenerator;}


	public void setEpsilon(double epsilon) {this.epsilon = epsilon;}

	public void setMaxIterations(int maxIterations) {this.maxIterations = maxIterations;}

	public void setPolicyCount(int policyCount) {this.policyCount = policyCount;}

	public void setTHistory(double[] tHistory) {this.tHistory = tHistory.clone();}

	public void setUsingMaxMargin(boolean useMaxMargin) {this.useMaxMargin = useMaxMargin;}


	public DenseStateFeatures getFeatureGenerator() {return this.featureGenerator;}

	public List<Episode> getExpertEpisodes() { return new ArrayList<Episode>(this.expertEpisodes);}

	public StateGenerator getStartStateGenerator() {return this.startStateGenerator;}


	public double getEpsilon() {return this.epsilon;}

	public int getMaxIterations() {return this.maxIterations;}

	public int getPolicyCount() {return this.policyCount;}

	public double[] getTHistory() {return this.tHistory.clone();}

	public boolean getUsingMaxMargin() {return this.useMaxMargin;}
}
