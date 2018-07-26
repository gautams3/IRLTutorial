package burlap.behavior.singleagent.learning.lspi;

import burlap.behavior.functionapproximation.dense.DenseStateActionFeatures;
import burlap.behavior.functionapproximation.dense.DenseStateActionLinearVFA;
import burlap.behavior.policy.EpsilonGreedy;
import burlap.behavior.policy.GreedyQPolicy;
import burlap.behavior.policy.Policy;
import burlap.behavior.policy.PolicyUtils;
import burlap.behavior.singleagent.Episode;
import burlap.behavior.singleagent.MDPSolver;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.lspi.SARSCollector.UniformRandomSARSCollector;
import burlap.behavior.singleagent.learning.lspi.SARSData.SARS;
import burlap.behavior.singleagent.planning.Planner;
import burlap.behavior.valuefunction.QProvider;
import burlap.behavior.valuefunction.QValue;
import burlap.debugtools.DPrint;
import burlap.mdp.auxiliary.common.ConstantStateGenerator;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.SADomain;
import burlap.mdp.singleagent.environment.Environment;
import burlap.mdp.singleagent.model.RewardFunction;
import org.ejml.simple.SimpleMatrix;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * This class implements the optimized version of last squares policy iteration [1] (runs in quadratic time of the number of state features). Unlike other planning and learning algorithms,
 * it is recommended that you use this class differently than the conventional ways. That is, rather than using the {@link #planFromState(State)} or {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment)}
 * methods, you should instead use a {@link SARSCollector} object to gather a bunch of example state-action-reward-state tuples that are then used for policy iteration. You can
 * set the dataset to use using the {@link #setDataset(SARSData)} method and then you can run LSPI on it using the {@link #runPolicyIteration(int, double)} method. LSPI requires
 * initialize a matrix to an identity matrix multiplied by some large positive constant (see the reference for more information).
 * By default this constant is 100, but you can change it with the {@link #setIdentityScalar(double)}
 * method.
 * <p>
 * If you do use the {@link #planFromState(State)} method, you should first initialize the parameters for it using the
 * {@link #initializeForPlanning(int, SARSCollector)} or
 * {@link #initializeForPlanning(int)} method.
 * If you do not set a {@link burlap.behavior.singleagent.learning.lspi.SARSCollector} to use for planning
 * a {@link UniformRandomSARSCollector} will be automatically created. After collecting data, it will call
 * the {@link #runPolicyIteration(int, double)} method using a maximum of 30 policy iterations. You can change the {@link SARSCollector} this method uses, the number of samples it acquires, the maximum weight change for PI termination,
 * and the maximum number of policy iterations by using the {@link #setPlanningCollector(SARSCollector)}, {@link #setNumSamplesForPlanning(int)}, {@link #setMaxChange(double)}, and
 * {@link #setMaxNumPlanningIterations(int)} methods respectively.
 * <p>
 * If you use the {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment)} method (or the {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment, int)}  method),
 * it will work by following a learning policy for the episode and adding its observations to its dataset for its
 * policy iteration. After enough new data has been acquired, policy iteration will be rereun.
 * You can adjust the learning policy, the maximum number of allowed learning steps in an
 * episode, and the minimum number of new observations until LSPI is rerun using the {@link #setLearningPolicy(Policy)}, {@link #setMaxLearningSteps(int)}, {@link #setMinNewStepsForLearningPI(int)}
 * methods respectively. The LSPI  termination parameters are set using the same methods that you use for adjusting the results from the {@link #planFromState(State)} method discussed above.
 * <p>
 * This data gathering and replanning behavior from learning episodes is not expected to be an especially good choice.
 * Therefore, if you want a better online data acquisition, you should consider subclassing this class
 * and overriding the methods {@link #updateDatasetWithLearningEpisode(Episode)} and {@link #shouldRereunPolicyIteration(Episode)}, or
 * the {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment, int)} method
 * itself.
 * <p>
 * Note that LSPI is not well defined for domains with terminal states. Therefore, you need to make sure
 * your reward function returns a value for terminal transitions that offsets the effect of the state not being terminal.
 * For example, for goal states, it should return a large enough value to offset any costs incurred from continuing.
 * For failure states, it should return a negative reward large enough to offset any gains incurred from continuing.
 * <p>
 * 1. Lagoudakis, Michail G., and Ronald Parr. "Least-squares policy iteration." The Journal of Machine Learning Research 4 (2003): 1107-1149.
 * 
 * @author James MacGlashan
 *
 */
public class LSPI extends MDPSolver implements QProvider, LearningAgent, Planner {

	/**
	 * The object that performs value function approximation given the weights that are estimated
	 */
	protected DenseStateActionLinearVFA 		vfa;
	
	/**
	 * The SARS dataset on which LSPI is performed
	 */
	protected SARSData												dataset;
	
	/**
	 * The state feature database on which the linear VFA is performed
	 */
	protected DenseStateActionFeatures saFeatures;
	
	
	/**
	 * The initial LSPI identity matrix scalar; default is 100.
	 */
	protected double												identityScalar = 100.;
	
	/**
	 * The last weight values set from LSTDQ
	 */
	protected SimpleMatrix											lastWeights;
	
	/**
	 * the number of samples that are acquired for this object's dataset when the {@link #planFromState(State)} method is called.
	 */
	protected int													numSamplesForPlanning = 10000;
	
	/**
	 * The maximum change in weights permitted to terminate LSPI. Default is 1e-6.
	 */
	protected double												maxChange = 1e-6;
	
	/**
	 * The data collector used by the {@link #planFromState(State)} method.
	 */
	protected SARSCollector											planningCollector;
	
	/**
	 * The maximum number of policy iterations permitted when LSPI is run from the {@link #planFromState(State)} or {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment)} methods.
	 */
	protected int													maxNumPlanningIterations = 30;
	
	
	/**
	 * The learning policy followed in {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment)} method calls. Default is 0.1 epsilon greedy.
	 */
	protected Policy												learningPolicy;
	
	/**
	 * The maximum number of learning steps in an episode when the {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment)} method is called. Default is INT_MAX.
	 */
	protected int													maxLearningSteps = Integer.MAX_VALUE;
	
	/**
	 * Number of new observations received from learning episodes since LSPI was run
	 */
	protected int													numStepsSinceLastLearningPI = 0;
	
	/**
	 * The minimum number of new observations received from learning episodes before LSPI will be run again.
	 */
	protected int													minNewStepsForLearningPI = 100;
	
	
	/**
	 * the saved previous learning episodes
	 */
	protected LinkedList<Episode>							episodeHistory = new LinkedList<Episode>();
	
	/**
	 * The number of the most recent learning episodes to store.
	 */
	protected int													numEpisodesToStore;
	
	
	
	
	
	
	/**
	 * Initializes.
	 * @param domain the problem domain
	 * @param gamma the discount factor
	 * @param saFeatures the state-action features to use
	 */
	public LSPI(SADomain domain, double gamma, DenseStateActionFeatures saFeatures){
		this.solverInit(domain, gamma, null);
		this.saFeatures = saFeatures;
		this.vfa = new DenseStateActionLinearVFA(saFeatures, 0.);
		this.learningPolicy = new EpsilonGreedy(this, 0.1);
	}

	/**
	 * Initializes.
	 * @param domain the problem domain
	 * @param gamma the discount factor
	 * @param saFeatures the state-action features
	 * @param dataset the dataset of transitions to use
	 */
	public LSPI(SADomain domain, double gamma, DenseStateActionFeatures saFeatures, SARSData dataset){
		this.solverInit(domain, gamma, null);
		this.saFeatures = saFeatures;
		this.vfa = new DenseStateActionLinearVFA(saFeatures, 0.);
		this.learningPolicy = new EpsilonGreedy(this, 0.1);
		this.dataset = dataset;
	}


	/**
	 * Sets the number of {@link burlap.behavior.singleagent.learning.lspi.SARSData.SARS} samples to use for planning when
	 * the {@link #planFromState(State)} method is called. If the
	 * {@link RewardFunction} and {@link burlap.mdp.core.TerminalFunction}
	 * are not set, the {@link #planFromState(State)} method will throw a runtime exception.
	 * @param numSamplesForPlanning the number of SARS samples to collect for planning.
	 */
	public void initializeForPlanning(int numSamplesForPlanning){
		this.numSamplesForPlanning = numSamplesForPlanning;
	}

	/**
	 * Sets the number of {@link burlap.behavior.singleagent.learning.lspi.SARSData.SARS} samples, and the {@link burlap.behavior.singleagent.learning.lspi.SARSCollector} to use
	 * to collect samples for planning when
	 * the {@link #planFromState(State)} method is called. If the
	 * {@link RewardFunction} and {@link burlap.mdp.core.TerminalFunction}
	 * are not set, the {@link #planFromState(State)} method will throw a runtime exception.
	 * @param numSamplesForPlanning the number of SARS samples to collect for planning.
	 * @param planningCollector the dataset collector to use for planning
	 */
	public void initializeForPlanning(int numSamplesForPlanning, SARSCollector planningCollector){
		this.numSamplesForPlanning = numSamplesForPlanning;
		this.planningCollector = planningCollector;
	}

	
	/**
	 * Sets the SARS dataset this object will use for LSPI
	 * @param dataset the SARSA dataset
	 */
	public void setDataset(SARSData dataset){
		this.dataset = dataset;
	}
	
	/**
	 * Returns the dataset this object uses for LSPI
	 * @return the dataset this object uses for LSPI
	 */
	public SARSData getDataset(){
		return this.dataset;
	}
	
	
	/**
	 * Returns the state-action features used
	 * @return the state-action features used
	 */
	public DenseStateActionFeatures getSaFeatures() {
		return saFeatures;
	}

	/**
	 * Sets the state-action features to used
	 * @param saFeatures the state-action feature to use
	 */
	public void setSaFeatures(DenseStateActionFeatures saFeatures) {
		this.saFeatures = saFeatures;
	}

	
	/**
	 * Returns the initial LSPI identity matrix scalar used
	 * @return the initial LSPI identity matrix scalar used
	 */
	public double getIdentityScalar() {
		return identityScalar;
	}

	/**
	 * Sets the initial LSPI identity matrix scalar used.
	 * @param identityScalar the initial LSPI identity matrix scalar used.
	 */
	public void setIdentityScalar(double identityScalar) {
		this.identityScalar = identityScalar;
	}

	
	/**
	 * Gets the number of SARS samples that will be gathered by the {@link #planFromState(State)} method.
	 * @return the number of SARS samples that will be gathered by the {@link #planFromState(State)} method.
	 */
	public int getNumSamplesForPlanning() {
		return numSamplesForPlanning;
	}

	/**
	 * Sets the number of SARS samples that will be gathered by the {@link #planFromState(State)} method.
	 * @param numSamplesForPlanning the number of SARS samples that will be gathered by the {@link #planFromState(State)} method.
	 */
	public void setNumSamplesForPlanning(int numSamplesForPlanning) {
		this.numSamplesForPlanning = numSamplesForPlanning;
	}

	/**
	 * Gets the {@link SARSCollector} used by the {@link #planFromState(State)} method for collecting data.
	 * @return the {@link SARSCollector} used by the {@link #planFromState(State)} method for collecting data.
	 */
	public SARSCollector getPlanningCollector() {
		return planningCollector;
	}

	
	/**
	 * Sets the {@link SARSCollector} used by the {@link #planFromState(State)} method for collecting data.
	 * @param planningCollector the {@link SARSCollector} used by the {@link #planFromState(State)} method for collecting data.
	 */
	public void setPlanningCollector(SARSCollector planningCollector) {
		this.planningCollector = planningCollector;
	}

	
	/**
	 * The maximum number of policy iterations that will be used by the {@link #planFromState(State)} method.
	 * @return the maximum number of policy iterations that will be used by the {@link #planFromState(State)} method.
	 */
	public int getMaxNumPlanningIterations() {
		return maxNumPlanningIterations;
	}

	/**
	 * Sets the maximum number of policy iterations that will be used by the {@link #planFromState(State)} method.
	 * @param maxNumPlanningIterations the maximum number of policy iterations that will be used by the {@link #planFromState(State)} method.
	 */
	public void setMaxNumPlanningIterations(int maxNumPlanningIterations) {
		this.maxNumPlanningIterations = maxNumPlanningIterations;
	}

	
	/**
	 * The learning policy followed by the {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment)} and {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment, int)} methods.
	 * @return learning policy followed by the {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment)} and {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment, int)} methods.
	 */
	public Policy getLearningPolicy() {
		return learningPolicy;
	}

	/**
	 * Sets the learning policy followed by the {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment)} and {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment, int)} methods.
	 * @param learningPolicy the learning policy followed by the {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment)} and {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment, int)} methods.
	 */
	public void setLearningPolicy(Policy learningPolicy) {
		this.learningPolicy = learningPolicy;
	}

	
	/**
	 * The maximum number of learning steps permitted by the {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment)} method.
	 * @return maximum number of learning steps permitted by the {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment)} method.
	 */
	public int getMaxLearningSteps() {
		return maxLearningSteps;
	}

	/**
	 * Sets the maximum number of learning steps permitted by the {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment)} method.
	 * @param maxLearningSteps the maximum number of learning steps permitted by the {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment)} method.
	 */
	public void setMaxLearningSteps(int maxLearningSteps) {
		this.maxLearningSteps = maxLearningSteps;
	}

	/**
	 * The minimum number of new learning observations before policy iteration is run again.
	 * @return the minimum number of new learning observations before policy iteration is run again.
	 */
	public int getMinNewStepsForLearningPI() {
		return minNewStepsForLearningPI;
	}

	/**
	 * Sets the minimum number of new learning observations before policy iteration is run again.
	 * @param minNewStepsForLearningPI the minimum number of new learning observations before policy iteration is run again.
	 */
	public void setMinNewStepsForLearningPI(int minNewStepsForLearningPI) {
		this.minNewStepsForLearningPI = minNewStepsForLearningPI;
	}
	
	

	/**
	 * The maximum change in weights required to terminate policy iteration when called from the {@link #planFromState(State)}, {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment)} or {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment, int)} methods.
	 * @return the maximum change in weights required to terminate policy iteration when called from the {@link #planFromState(State)}, {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment)} or {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment, int)} methods.
	 */
	public double getMaxChange() {
		return maxChange;
	}

	/**
	 * Sets the maximum change in weights required to terminate policy iteration when called from the {@link #planFromState(State)}, {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment)} or {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment, int)} methods.
	 * @param maxChange the maximum change in weights required to terminate policy iteration when called from the {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment)} or {@link #runLearningEpisode(burlap.mdp.singleagent.environment.Environment, int)} methods.
	 */
	public void setMaxChange(double maxChange) {
		this.maxChange = maxChange;
	}

	
	/**
	 * Runs LSTDQ on this object's current {@link SARSData} dataset.
	 * @return the new weight matrix as a {@link SimpleMatrix} object.
	 */
	public SimpleMatrix LSTDQ(){
		
		//set our policy
		Policy p = new GreedyQPolicy(this);
		
		//first we want to get all the features for all of our states in our data set; this is important if our feature database generates new features on the fly
		List<SSFeatures> features = new ArrayList<LSPI.SSFeatures>(this.dataset.size());
		int nf = 0;
		for(SARS sars : this.dataset.dataset){
			SSFeatures transitionFeatures = new SSFeatures(this.saFeatures.features(sars.s, sars.a), this.saFeatures.features(sars.sp, p.action(sars.sp)));
			features.add(transitionFeatures);
			nf = Math.max(nf, transitionFeatures.sActionFeatures.length);
		}

		SimpleMatrix B = SimpleMatrix.identity(nf).scale(this.identityScalar);
		SimpleMatrix b = new SimpleMatrix(nf, 1);
		
		
		
		for(int i = 0; i < features.size(); i++){

			SimpleMatrix phi = this.phiConstructor(features.get(i).sActionFeatures, nf);
			SimpleMatrix phiPrime = this.phiConstructor(features.get(i).sPrimeActionFeatures, nf);
			double r = this.dataset.get(i).r;
			

			SimpleMatrix numerator = B.mult(phi).mult(phi.minus(phiPrime.scale(gamma)).transpose()).mult(B);
			SimpleMatrix denomenatorM = phi.minus(phiPrime.scale(this.gamma)).transpose().mult(B).mult(phi);
			double denomenator = denomenatorM.get(0) + 1;
			
			B = B.minus(numerator.scale(1./denomenator));
			b = b.plus(phi.scale(r));
			
			//DPrint.cl(0, "updated matrix for row " + i + "/" + features.size());
			
		}
		
		
		SimpleMatrix w = B.mult(b);
		
		this.vfa = this.vfa.copy();
		for(int i = 0; i < nf; i++){
			this.vfa.setParameter(i, w.get(i, 0));
		}
		
		return w;
		
		
	}
	
	/**
	 * Runs LSPI for either numIterations or until the change in the weight matrix is no greater than maxChange.
	 * @param numIterations the maximum number of policy iterations.
	 * @param maxChange when the weight change is smaller than this value, LSPI terminates.
	 * @return a {@link burlap.behavior.policy.GreedyQPolicy} using this object as the {@link QProvider} source.
	 */
	public GreedyQPolicy runPolicyIteration(int numIterations, double maxChange){
		
		boolean converged = false;
		for(int i = 0; i < numIterations && !converged; i++){
			SimpleMatrix nw = this.LSTDQ();
			double change = Double.POSITIVE_INFINITY;
			if(this.lastWeights != null){
				change = this.lastWeights.minus(nw).normF();
				if(change <= maxChange){
					converged = true;
				}
			}
			this.lastWeights = nw;
			
			DPrint.cl(0, "Finished iteration: " + i + ". Weight change: " + change);
			
		}
		DPrint.cl(0, "Finished Policy Iteration.");
		return new GreedyQPolicy(this);
	}
	
	
	/**
	 * Constructs the state-action feature vector as a {@link SimpleMatrix}.
	 * @param features the state-action features
	 * @param nf the total number of state-action features.
	 * @return the state-action feature vector as a {@link SimpleMatrix}.
	 */
	protected SimpleMatrix phiConstructor(double [] features, int nf){
		SimpleMatrix phi = new SimpleMatrix(nf, 1, true, features);

		return phi;
	}
	
	
	@Override
	public List<QValue> qValues(State s) {
		
		List<Action> gas = this.applicableActions(s);
		List <QValue> qs = new ArrayList<QValue>(gas.size());


		for(Action ga : gas){
			double q = this.vfa.evaluate(s, ga);
			qs.add(new QValue(s, ga, q));
		}
		
		return qs;
		
		
	}

	@Override
	public double qValue(State s, Action a) {
		return this.vfa.evaluate(s, a);
	}

	@Override
	public double value(State s) {
		return Helper.maxQ(this, s);
	}

	/**
	 * Plans from the input state and then returns a {@link burlap.behavior.policy.GreedyQPolicy} that greedily
	 * selects the action with the highest Q-value and breaks ties uniformly randomly.
	 * @param initialState the initial state of the planning problem
	 * @return a {@link burlap.behavior.policy.GreedyQPolicy}.
	 */
	@Override
	public GreedyQPolicy planFromState(State initialState) {

		if(this.model == null){
			throw new RuntimeException("LSPI cannot execute planFromState because the reward function and/or terminal function for planning have not been set. Use the initializeForPlanning method to set them.");
		}

		if(planningCollector == null){
			this.planningCollector = new SARSCollector.UniformRandomSARSCollector(this.actionTypes);
		}
		this.dataset = this.planningCollector.collectNInstances(new ConstantStateGenerator(initialState), this.model, this.numSamplesForPlanning, Integer.MAX_VALUE, this.dataset);
		return this.runPolicyIteration(this.maxNumPlanningIterations, this.maxChange);


	}

	@Override
	public void resetSolver() {
		this.dataset.clear();
		this.vfa.resetParameters();
	}
	
	
	
	
	/**
	 * Pair of the the state-action features and the next state-action features.
	 * @author James MacGlashan
	 *
	 */
	protected class SSFeatures{
		
		/**
		 * State-action features
		 */
		public double[] sActionFeatures;
		
		/**
		 * Next state-action features.
		 */
		public double[] sPrimeActionFeatures;
		
		
		/**
		 * Initializes.
		 * @param sActionFeatures state-action features
		 * @param sPrimeActionFeatures next state-action features
		 */
		public SSFeatures(double[] sActionFeatures, double[] sPrimeActionFeatures){
			this.sActionFeatures = sActionFeatures;
			this.sPrimeActionFeatures = sPrimeActionFeatures;
		}
		
	}


	@Override
	public Episode runLearningEpisode(Environment env) {
		return this.runLearningEpisode(env, -1);
	}

	@Override
	public Episode runLearningEpisode(Environment env, int maxSteps) {

		Episode ea = maxSteps != -1 ? PolicyUtils.rollout(this.learningPolicy, env, maxSteps) : PolicyUtils.rollout(this.learningPolicy, env);

		this.updateDatasetWithLearningEpisode(ea);

		if(this.shouldRereunPolicyIteration(ea)){
			this.runPolicyIteration(this.maxNumPlanningIterations, this.maxChange);
			this.numStepsSinceLastLearningPI = 0;
		}
		else{
			this.numStepsSinceLastLearningPI += ea.numTimeSteps()-1;
		}

		if(episodeHistory.size() >= numEpisodesToStore){
			episodeHistory.poll();
		}
		episodeHistory.offer(ea);

		return ea;
	}

	
	
	
	/**
	 * Updates this object's {@link SARSData} to include the results of a learning episode.
	 * @param ea the learning episode as an {@link Episode} object.
	 */
	protected void updateDatasetWithLearningEpisode(Episode ea){
		if(this.dataset == null){
			this.dataset = new SARSData(ea.numTimeSteps()-1);
		}
		for(int i = 0; i < ea.numTimeSteps()-1; i++){
			this.dataset.add(ea.state(i), ea.action(i), ea.reward(i+1), ea.state(i+1));
		}
	}
	
	
	/**
	 * Returns whether LSPI should be rereun given the latest learning episode results. Default behavior is to return true
	 * if the number of leanring episode steps plus the number of steps since the last run is greater than the {@link #numStepsSinceLastLearningPI} threshold.
	 * @param ea the most recent learning episode
	 * @return true if LSPI should be rerun; false otherwise.
	 */
	protected boolean shouldRereunPolicyIteration(Episode ea){
		if(this.numStepsSinceLastLearningPI+ea.numTimeSteps()-1 > this.minNewStepsForLearningPI){
			return true;
		}
		return false;
	}

	public Episode getLastLearningEpisode() {
		return this.episodeHistory.getLast();
	}

	public void setNumEpisodesToStore(int numEps) {
		this.numEpisodesToStore = numEps;
	}

	public List<Episode> getAllStoredLearningEpisodes() {
		return this.episodeHistory;
	}


}
