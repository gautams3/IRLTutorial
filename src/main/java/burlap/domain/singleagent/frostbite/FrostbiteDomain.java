package burlap.domain.singleagent.frostbite;

import burlap.domain.singleagent.frostbite.state.FrostbiteAgent;
import burlap.domain.singleagent.frostbite.state.FrostbiteIgloo;
import burlap.domain.singleagent.frostbite.state.FrostbitePlatform;
import burlap.domain.singleagent.frostbite.state.FrostbiteState;
import burlap.mdp.auxiliary.DomainGenerator;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.oo.propositional.PropositionalFunction;
import burlap.mdp.core.oo.state.OOState;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.model.RewardFunction;
import burlap.mdp.singleagent.SADomain;
import burlap.mdp.core.action.UniversalActionType;
import burlap.shell.visual.VisualExplorer;
import burlap.mdp.singleagent.model.FactoredModel;
import burlap.mdp.singleagent.oo.OOSADomain;
import burlap.visualizer.Visualizer;

import java.util.Arrays;
import java.util.List;

/**
 * A simplified version of the classic Atari Frostbite domain. In this game, the agent must jump between different
 * ice blocks. Each time the agent jumps on an ice block, it adds a layer to an igloo that is being built and "activates"
 * all ice blocks on the same row. Jumping on an activated ice block does not ad a layer to the igloo. Once all rows
 * of ice blocks are activated, they reset and can be activated by jumping on them again. Once the igloo is built,
 * the agent can go to it to win the game. If the agent jumps or walks into the water, the game is over.
 * <p>
 * If you run the main method of this class, it will launch of a visual explorer that you can play. They keys
 * w,s,a,d,x correspond to the actions jump north, jump south, move west, move east, do nothing.
 *
 * @author Phillipe Morere
 */
public class FrostbiteDomain implements DomainGenerator{

	/**
	 * Constant for the name of the x position attribute.
	 */
	public static final String VAR_X = "x";
	/**
	 * Constant for the name of the y position attribute.
	 */
	public static final String VAR_Y = "y";

	/**
	 * Attribute name for height
	 */
	public static final String VAR_HEIGHT = "height";

	/**
	 * Constant for the name of the size of a frozen platform
	 */
	public static final String VAR_SIZE = "size";
	/**
	 * Constant for the name of the building step of the igloo
	 */
	public static final String VAR_BUILDING = "building";
	/**
	 * Constant for the name of the activated status of a platform
	 */
	public static final String VAR_ACTIVATED = "activated";

	/**
	 * Constant for the name of the agent OO-MDP class
	 */
	public static final String CLASS_AGENT = "agent";
	/**
	 * Constant for the name of the igloo OO-MDP class
	 */
	public static final String CLASS_IGLOO = "igloo";
	/**
	 * Constant for the name of the obstacle OO-MDP class
	 */
	public static final String CLASS_PLATFORM = "platform";

	/**
	 * Constant for the name of the north action
	 */
	public static final String ACTION_NORTH = "north";
	/**
	 * Constant for the name of the south action
	 */
	public static final String ACTION_SOUTH = "south";
	/**
	 * Constant for the name of the east action
	 */
	public static final String ACTION_EAST = "east";
	/**
	 * Constant for the name of the west action
	 */
	public static final String ACTION_WEST = "west";
	/**
	 * Constant for the name of the west action
	 */
	public static final String ACTION_IDLE = "idle";

	/**
	 * Constant for the name of the propositional function "agent is on platform"
	 */
	public static final String PF_ON_PLATFORM = "pfOnPlatform";
	/**
	 * Constant for the name of the propositional function "platform is active"
	 */
	public static final String PF_PLATFORM_ACTIVE = "pfPlatformActive";
	/**
	 * Constant for the name of the propositional function "agent is on ice"
	 */
	public static final String PF_ON_ICE = "pfOnIce";
	/**
	 * Constant for the name of the propositional function "igloo is built"
	 */
	public static final String PF_IGLOO_BUILT = "pfIglooBuilt";
	/**
	 * Constant for the name of the propositional function "agent is in water"
	 */
	public static final String PF_IN_WATER = "pfInWater";


	/**
	 * Constant to adjust the scale of the game
	 */
	protected int scale = 5;

	protected RewardFunction rf;
	protected TerminalFunction tf;



	/**
	 * Main function to test the domain.
	 * Note: The termination conditions are not checked when testing the domain this way, which means it is
	 * impossible to win or die and might trigger bugs. To enable them, uncomment the code in the "update" function.
	 *
	 * @param args command line args
	 */
	public static void main(String[] args) {
		FrostbiteDomain fd = new FrostbiteDomain();
		SADomain d = fd.generateDomain();
		State s = new FrostbiteState();

		Visualizer vis = FrostbiteVisualizer.getVisualizer();
		VisualExplorer exp = new VisualExplorer(d, vis, s);

		exp.addKeyAction("a", ACTION_WEST, "");
		exp.addKeyAction("d", ACTION_EAST, "");
		exp.addKeyAction("w", ACTION_NORTH, "");
		exp.addKeyAction("s", ACTION_SOUTH, "");
		exp.addKeyAction("x", ACTION_IDLE, "");

		exp.initGUI();
	}

	public TerminalFunction getTf() {
		return tf;
	}

	public void setTf(TerminalFunction tf) {
		this.tf = tf;
	}

	public RewardFunction getRf() {
		return rf;
	}

	public void setRf(RewardFunction rf) {
		this.rf = rf;
	}

	public int getScale() {
		return scale;
	}

	public void setScale(int scale) {
		this.scale = scale;
	}


	public List<PropositionalFunction> generatePFs(){

		int numberPlatformCol = 4;

		int gameHeight = 130 * scale;
		int gameWidth = 160 * scale;
		int platformSpeed = 1 * scale;
		int agentSize = 8 * scale;
		int gameIceHeight = gameHeight / 4;

		List<PropositionalFunction> pfs = Arrays.asList(
				new PlatformActivePF(PF_PLATFORM_ACTIVE),
				new OnPlatformPF(PF_ON_PLATFORM, gameWidth, agentSize),
				new InWaterPF(PF_IN_WATER, gameWidth, agentSize, gameHeight, numberPlatformCol, platformSpeed),
				new OnIcePF(PF_ON_ICE, agentSize, gameIceHeight),
				new IglooBuiltPF(PF_IGLOO_BUILT));


		return pfs;
	}

	/**
	 * Creates a new frostbite domain.
	 *
	 * @return the generated domain object
	 */
	@Override
	public OOSADomain generateDomain() {

		OOSADomain domain = new OOSADomain();

		domain.addStateClass(CLASS_AGENT, FrostbiteAgent.class)
				.addStateClass(CLASS_IGLOO, FrostbiteIgloo.class)
				.addStateClass(CLASS_PLATFORM, FrostbitePlatform.class);

		//add actions
		domain.addActionType(new UniversalActionType(ACTION_NORTH))
				.addActionType(new UniversalActionType(ACTION_SOUTH))
				.addActionType(new UniversalActionType(ACTION_EAST))
				.addActionType(new UniversalActionType(ACTION_WEST))
				.addActionType(new UniversalActionType(ACTION_IDLE));



		//add pfs
		List<PropositionalFunction> pfs = this.generatePFs();
		for(PropositionalFunction pf : pfs){
			domain.addPropFunction(pf);
		}


		FrostbiteModel smodel = new FrostbiteModel(scale);
		RewardFunction rf = this.rf;
		TerminalFunction tf = this.tf;
		if(rf == null){
			rf = new FrostbiteRF(domain);
		}
		if(tf == null){
			tf = new FrostbiteTF(domain);
		}


		FactoredModel model = new FactoredModel(smodel, rf, tf);
		domain.setModel(model);

		return domain;
	}



	public class OnPlatformPF extends PropositionalFunction {

		int width;
		int agentSize;



		public OnPlatformPF(String name, int width, int agentSize) {
			super(name, new String[]{CLASS_AGENT, CLASS_PLATFORM});
			this.width = width;
			this.agentSize = agentSize;
		}


		@Override
		public boolean isTrue(OOState st, String... params) {

			FrostbiteAgent agent = (FrostbiteAgent)st.object(params[0]);
			FrostbitePlatform platform = (FrostbitePlatform)st.object(params[1]);

			int x = platform.x;
			int y = platform.y;
			int s = platform.size;

			int ax = agent.x + agentSize / 2;
			int ay = agent.y + agentSize / 2;
			int ah = agent.height;

			if(ah != 0){
				return false;
			}

			return pointInPlatform(ax, ay, x, y, s);
		}

		/**
		 * Collision check between a point (player center) and a platform, including wrapping around edges
		 * @param px point X coordinate
		 * @param py point Y coordinate
		 * @param x platform top left corner X coordinate
		 * @param y platform top left corner Y coordinate
		 * @param s platform size
		 * @return true if the point is in the platform, false otherwise
		 */
		private boolean pointInPlatform(int px, int py, int x, int y, int s) {
			if (pointInPlatformHelper(px, py, x, y, s))
				return true;
			if (x + s > width && pointInPlatformHelper(px, py, x - width, y, s))
				return true;
			else if (x < 0 && pointInPlatformHelper(px, py, x + width, y, s))
				return true;
			return false;
		}

		/**
		 * Collision check between a point (player center) and a platform.
		 * @param px point X coordinate
		 * @param py point Y coordinate
		 * @param x platform top left corner X coordinate
		 * @param y platform top left corner Y coordinate
		 * @param s platform size
		 * @return true if the point is in the platform, false otherwise
		 */
		private boolean pointInPlatformHelper(int px, int py, int x, int y, int s) {
			return px > x && px < x + s && py > y && py < y + s;
		}
	}

	public class PlatformActivePF extends PropositionalFunction {
		/**
		 * Initializes to be evaluated on an agent object and platform object.
		 *
		 * @param name   the name of the propositional function
		 */
		public PlatformActivePF(String name) {
			super(name, new String[]{CLASS_PLATFORM});
		}

		@Override
		public boolean isTrue(OOState st, String... params) {
			FrostbitePlatform platform = (FrostbitePlatform)st.object(params[0]);
			return platform.activated;
		}
	}

	public class InWaterPF extends PropositionalFunction {

		int width;
		int agentSize;
		int gameIceHeight;
		int numberPlatformCol;
		int platformSpeed;


		public InWaterPF(String name, int width, int agentSize, int gameIceHeight, int numberPlatformCol, int platformSpeed) {
			super(name, new String[]{CLASS_AGENT});
			this.width = width;
			this.agentSize = agentSize;
			this.gameIceHeight = gameIceHeight;
			this.numberPlatformCol = numberPlatformCol;
			this.platformSpeed = platformSpeed;
		}


		@Override
		public boolean isTrue(OOState st, String... params) {

			FrostbiteAgent agent = (FrostbiteAgent)st.object(params[0]);
			int ah = agent.height;

			if (ah != 0)
				return false;

			// Agent is on a platform
			if (getLandedPlatformSpeed((FrostbiteState)st) != 0)
				return false;

			int ay = agent.y + agentSize / 2;
			return ay >= gameIceHeight;
		}

		/**
		 * Checks whether the player is on a platform and return its platform speed if so.
		 * @param s State on which the check is made
		 * @return 0 if the player is not on a platform. Otherwise returns the platform speed of the platform the player is on.
		 */
		private int getLandedPlatformSpeed(FrostbiteState s) {
			FrostbiteAgent agent = s.agent;
			int ax = agent.x + agentSize / 2;
			int ay = agent.y + agentSize / 2;
			List<FrostbitePlatform> platforms = s.platforms;
			for (int i = 0; i < platforms.size(); i++) {
				FrostbitePlatform platform = platforms.get(i);
				if (pointInPlatform(ax, ay, platform.x, platform.y, platform.size))
					return ((i / numberPlatformCol) % 2 == 0) ? platformSpeed : -platformSpeed;
			}
			return 0;
		}

		/**
		 * Collision check between a point (player center) and a platform, including wrapping around edges
		 * @param px point X coordinate
		 * @param py point Y coordinate
		 * @param x platform top left corner X coordinate
		 * @param y platform top left corner Y coordinate
		 * @param s platform size
		 * @return true if the point is in the platform, false otherwise
		 */
		private boolean pointInPlatform(int px, int py, int x, int y, int s) {
			if (pointInPlatformHelper(px, py, x, y, s))
				return true;
			if (x + s > width && pointInPlatformHelper(px, py, x - width, y, s))
				return true;
			else if (x < 0 && pointInPlatformHelper(px, py, x + width, y, s))
				return true;
			return false;
		}

		/**
		 * Collision check between a point (player center) and a platform.
		 * @param px point X coordinate
		 * @param py point Y coordinate
		 * @param x platform top left corner X coordinate
		 * @param y platform top left corner Y coordinate
		 * @param s platform size
		 * @return true if the point is in the platform, false otherwise
		 */
		private boolean pointInPlatformHelper(int px, int py, int x, int y, int s) {
			return px > x && px < x + s && py > y && py < y + s;
		}
	}

	public class OnIcePF extends PropositionalFunction {

		int agentSize;
		int gameIceHeight;

		public OnIcePF(String name, int agentSize, int gameIceHeight) {
			super(name, new String[]{CLASS_AGENT});
			this.agentSize = agentSize;
			this.gameIceHeight = gameIceHeight;
		}


		@Override
		public boolean isTrue(OOState st, String... params) {
			FrostbiteAgent agent = (FrostbiteAgent)st.object(params[0]);

			int ay = agent.x + agentSize / 2;
			return ay < gameIceHeight;
		}
	}

	public class IglooBuiltPF extends PropositionalFunction {
		/**
		 * Initializes to be evaluated on an agent object.
		 *
		 * @param name   the name of the propositional function
		 */
		public IglooBuiltPF(String name) {
			super(name, new String[]{CLASS_IGLOO});
		}

		@Override
		public boolean isTrue(OOState st, String... params) {
			FrostbiteIgloo igloo = (FrostbiteIgloo)st.object(params[0]);

			int building = igloo.height;
			return building >= 16;
		}
	}

}
