package burlap.behavior.singleagent.planning.deterministic.informed.astar;

import burlap.behavior.singleagent.options.EnvironmentOptionOutcome;
import burlap.behavior.singleagent.options.Option;
import burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.informed.Heuristic;
import burlap.behavior.singleagent.planning.deterministic.informed.PrioritizedSearchNode;
import burlap.datastructures.HashIndexedHeap;
import burlap.debugtools.DPrint;
import burlap.mdp.auxiliary.stateconditiontest.StateConditionTest;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.core.action.ActionType;
import burlap.mdp.singleagent.SADomain;
import burlap.mdp.singleagent.environment.EnvironmentOutcome;
import burlap.statehashing.HashableState;
import burlap.statehashing.HashableStateFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamic Weighted A* [1] uses a dynamic heuristic weight that is based on depth of the current search tree and based on an expected depth of the search. Specifically,
 * f(n) = g(n) + (1 + \epsilon * w(n))*h(n),
 * 
 * where epsilon is a parameter &gt; 1 indicating greediness (the larger the more greedy) and
 * 
 * w(n) = {  1 - d(n)/N      if d(n) &lt;= N
 *        {  0               if d(n) &gt; N,
 *        
 * where d(n) is the depth of the search and N is the expected depth of the search. This algorithm has the effect of becoming less
 * greedy as the search continues, which allows it to find a decent solution quickly but avoid returning extremely sub-optimal solutions.
 * 
 * <p>
 * If a terminal function is provided via the setter method defined for OO-MDPs, then the BestFirst search algorithm will not expand any nodes
 * that are terminal states, as if there were no actions that could be executed from that state. Note that terminal states
 * are not necessarily the same as goal states, since there could be a fail condition from which the agent cannot act, but
 * that is not explicitly represented in the transition dynamics.
 * 
 * 1. Pohl, Ira (August, 1973). "The avoidance of (relative) catastrophe, heuristic competence, genuine dynamic weighting and computational issues in heuristic problem solving". 
 * Proceedings of the Third International Joint Conference on Artificial Intelligence (IJCAI-73) 3. California, USA. pp. 11-17.
 * 
 * 
 * @author James MacGlashan
 *
 */
public class DynamicWeightedAStar extends AStar {

	/**
	 * parameter &gt; 1 indicating the maximum amount of greediness; the larger the more greedy.
	 */
	protected double										epsilon;
	
	/**
	 * The expected depth required for a plan
	 */
	protected int											expectedDepth;
	
	/**
	 * Data structure for storing the depth of explored states
	 */
	protected Map <HashableState, Integer>					depthMap;
	
	/**
	 * maintains the depth of the last explored node
	 */
	protected int											lastComputedDepth;
	
	
	/**
	 * Initializes
	 * @param domain the domain in which to plan
	 * @param gc should evaluate to true for goal states; false otherwise
	 * @param hashingFactory the state hashing factory to use
	 * @param heuristic the planning heuristic. Should return non-positive values.
	 * @param epsilon parameter &gt; 1 indicating greediness; the larger the value the more greedy.
	 * @param expectedDepth the expected depth of the plan
	 */
	public DynamicWeightedAStar(SADomain domain, StateConditionTest gc, HashableStateFactory hashingFactory, Heuristic heuristic, double epsilon, int expectedDepth) {
		super(domain, gc, hashingFactory, heuristic);
		this.epsilon = epsilon;
		this.expectedDepth = expectedDepth;
	}
	
	@Override
	public void prePlanPrep(){
		super.prePlanPrep();
		depthMap = new HashMap<HashableState, Integer>();
	}
	
	@Override
	public void postPlanPrep(){
		super.postPlanPrep();
		depthMap = null; //clear out to reclaim memory
	}
	
	@Override
	public void insertIntoOpen(HashIndexedHeap<PrioritizedSearchNode> openQueue, PrioritizedSearchNode psn){
		super.insertIntoOpen(openQueue, psn);
		depthMap.put(psn.s, lastComputedDepth);
	}
	
	@Override
	public void updateOpen(HashIndexedHeap<PrioritizedSearchNode> openQueue, PrioritizedSearchNode openPSN, PrioritizedSearchNode npsn){
		super.updateOpen(openQueue, openPSN, npsn);
		depthMap.put(npsn.s, lastComputedDepth);
	}



	/**
	 * Plans and returns a {@link burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy}. If
	 * a {@link State} is not in the solution path of this planner, then
	 * the {@link burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy} will throw
	 * a runtime exception. If you want a policy that will dynamically replan for unknown states,
	 * you should create your own {@link burlap.behavior.singleagent.planning.deterministic.DDPlannerPolicy}.
	 * <p>
	 * This method overrides AStar's implementation so that it avoids reopening closed states that are not actually better due to the dynamic
	 * h weight, the reopen check needs to be based on the g score, note the f score
	 * @param initialState the initial state of the planning problem
	 * @return a {@link burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy}.
	 */

	@Override
	public SDPlannerPolicy planFromState(State initialState) {
		
		//first determine if there is even a need to plan
		HashableState sih = this.stateHash(initialState);
		
		if(internalPolicy.containsKey(sih)){
			return new SDPlannerPolicy(this); //no need to plan since this is already solved
		}
		
		
		//a plan is not cached so being planning process
		this.prePlanPrep();

		HashIndexedHeap<PrioritizedSearchNode> openQueue = new HashIndexedHeap<PrioritizedSearchNode>(new PrioritizedSearchNode.PSNComparator());
		Map<PrioritizedSearchNode, PrioritizedSearchNode> closedSet = new HashMap<PrioritizedSearchNode,PrioritizedSearchNode>();
		
		PrioritizedSearchNode ipsn = new PrioritizedSearchNode(sih, this.computeF(null, null, sih, new EnvironmentOutcome(null, null, sih.s(), 0., false)));
		this.insertIntoOpen(openQueue, ipsn);
		
		int nexpanded = 0;
		PrioritizedSearchNode lastVistedNode = null;
		double minF = ipsn.priority;
		while(openQueue.size() > 0){
			
			PrioritizedSearchNode node = openQueue.poll();
			closedSet.put(node, node);
			
			nexpanded++;
			if(node.priority < minF){
				minF = node.priority;
				DPrint.cl(debugCode, "Min F Expanded: " + minF + "; Nodes expanded so far: " + nexpanded + "; Open size: " + openQueue.size());
			}
			
			
			State s = node.s.s();
			if(gc.satisfies(s)){
				lastVistedNode = node;
				break;
			}
			
			if(this.model.terminal(s)){
				continue; //do not expand terminal state
			}
		
			//generate successors
			for(ActionType a : actionTypes){
				//List<GroundedAction> gas = s.getAllGroundedActionsFor(a);
				List<Action> gas = a.allApplicableActions(s);
				for(Action ga : gas){
					EnvironmentOutcome eo = this.model.sample(s, ga);
					State ns = eo.op;
					HashableState nsh = this.stateHash(ns);
					
					double F = this.computeF(node, ga, nsh, eo.r);
					PrioritizedSearchNode npsn = new PrioritizedSearchNode(nsh, ga, node, F);
					
					//check closed
					PrioritizedSearchNode closedPSN = closedSet.get(npsn);
					if(closedPSN != null && lastComputedCumR <= cumulatedRewardMap.get(closedPSN.s)){
					    continue; //no need to reopen because this is a worse path to an already explored node
					}
					
					
					//check open
					PrioritizedSearchNode openPSN = openQueue.containsInstance(npsn);
					if(openPSN == null){
						this.insertIntoOpen(openQueue, npsn);
					}
					else if(lastComputedCumR > cumulatedRewardMap.get(openPSN.s)){
						this.updateOpen(openQueue, openPSN, npsn);
					}
					
					
				}
				
				
			}
			
			
			
			
		}
		
		
		
		//search to goal complete. Now follow back pointers to set policy
		this.encodePlanIntoPolicy(lastVistedNode);
		
		DPrint.cl(debugCode, "Num Expanded: " + nexpanded);
		
		this.postPlanPrep();


		return new SDPlannerPolicy(this);
		
	}
	
	
	

	public double computeF(PrioritizedSearchNode parentNode, Action generatingAction, HashableState successorState, EnvironmentOutcome eo) {
		double cumR = 0.;
		int d = 0;
		if(parentNode != null){
			double pCumR = cumulatedRewardMap.get(parentNode.s);
			cumR = pCumR + eo.r;
			
			int pD = depthMap.get(parentNode.s);
			if(!(generatingAction instanceof Option)){
				d = pD + 1;
			}
			else{
				d = pD + ((EnvironmentOptionOutcome)eo).numSteps();
			}
		}
		
		double H  = heuristic.h(successorState.s());
		lastComputedCumR = cumR;
		lastComputedDepth = d;
		double weightedE = this.epsilon * this.epsilonWeight(d);
		double F = cumR + ((1. + weightedE)*H);
		
		return F;
	}
	
	
	/**
	 * Returns the weighted epsilon value at the given search depth
	 * @param depth the search depth
	 * @return the weighted epsilon value at the given search depth
	 */
	protected double epsilonWeight(int depth){
		
		double ratio = ((double)depth)/((double)expectedDepth);
		return Math.max(1.-ratio, 0.0);
		//return 1.;
		
	}
	
}
