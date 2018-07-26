package burlap.domain.singleagent.blockdude.state;

import burlap.mdp.core.oo.state.MutableOOState;
import burlap.mdp.core.oo.state.OOStateUtilities;
import burlap.mdp.core.oo.state.OOVariableKey;
import burlap.mdp.core.oo.state.ObjectInstance;
import burlap.mdp.core.state.MutableState;
import burlap.mdp.core.state.State;
import burlap.mdp.core.state.StateUtilities;
import burlap.mdp.core.state.annotations.ShallowCopyState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static burlap.domain.singleagent.blockdude.BlockDude.*;

/**
 * @author James MacGlashan.
 */
@ShallowCopyState
public class BlockDudeState implements MutableOOState {

	public BlockDudeAgent agent;
	public BlockDudeMap map;
	public BlockDudeCell exit;
	public List<BlockDudeCell> blocks;

	public BlockDudeState() {
	}

	public BlockDudeState(int w, int h, int nBlocks) {
		this.agent = new BlockDudeAgent();
		this.map = new BlockDudeMap(w, h);
		this.exit = new BlockDudeCell(CLASS_EXIT, CLASS_EXIT);
		this.blocks = new ArrayList<BlockDudeCell>(nBlocks);
		for(int i = 0; i < nBlocks; i++){
			this.blocks.add(new BlockDudeCell(CLASS_BLOCK, CLASS_BLOCK+i));
		}
	}

	public BlockDudeState(BlockDudeAgent agent, BlockDudeMap map, BlockDudeCell exit, BlockDudeCell...blocks) {
		this.agent = agent;
		this.map = map;
		this.exit = exit;
		this.blocks = Arrays.asList(blocks);
	}

	public BlockDudeState(BlockDudeAgent agent, BlockDudeMap map, BlockDudeCell exit, List<BlockDudeCell> blocks) {
		this.agent = agent;
		this.map = map;
		this.exit = exit;
		this.blocks = blocks;
	}

	@Override
	public MutableOOState addObject(ObjectInstance o) {

		if(!(o instanceof BlockDudeCell) || !o.className().equals(CLASS_BLOCK)){
			throw new RuntimeException("Can only add block objects to state.");
		}
		//copy on write
		this.blocks = new ArrayList<BlockDudeCell>(blocks);
		blocks.add((BlockDudeCell) o);

		return this;
	}

	@Override
	public MutableOOState removeObject(String oname) {

		int ind = this.blockForName(oname);
		if(ind == -1){
			throw new RuntimeException("Can only remove block objects from state.");
		}
		//copy on write
		this.blocks = new ArrayList<BlockDudeCell>(blocks);
		this.blocks.remove(ind);

		return this;
	}

	@Override
	public MutableOOState renameObject(String objectName, String newName) {

		int ind = this.blockForName(objectName);
		if(ind == -1){
			throw new RuntimeException("Can only rename block objects.");
		}
		BlockDudeCell oldBlock = this.blocks.get(ind);
		//copy on write
		this.blocks = new ArrayList<BlockDudeCell>(blocks);
		this.blocks.remove(ind);
		this.blocks.add((BlockDudeCell) oldBlock.copyWithName(newName));

		return this;
	}

	@Override
	public int numObjects() {
		return this.blocks.size() + 3;
	}

	@Override
	public ObjectInstance object(String oname) {
		if(oname.equals(CLASS_AGENT)){
			return agent;
		}
		else if(oname.equals(CLASS_MAP)){
			return map;
		}
		else if(oname.equals(exit.name())){
			return exit;
		}
		int ind = this.blockForName(oname);
		if (ind != -1){
			return this.blocks.get(ind);
		}


		return null;
	}

	@Override
	public List<ObjectInstance> objects() {
		ArrayList<ObjectInstance> obs = new ArrayList<ObjectInstance>(this.blocks);
		obs.add(agent);
		obs.add(map);
		obs.add(exit);
		return obs;
	}

	@Override
	public List<ObjectInstance> objectsOfClass(String oclass) {
		if(oclass.equals(CLASS_AGENT)){
			return Arrays.<ObjectInstance>asList(agent);
		}
		else if(oclass.equals(CLASS_MAP)){
			return Arrays.<ObjectInstance>asList(map);
		}
		else if(oclass.equals(CLASS_EXIT)){
			return Arrays.<ObjectInstance>asList(exit);
		}
		else if(oclass.equals(CLASS_BLOCK)){
			return new ArrayList<ObjectInstance>(blocks);
		}
		throw new RuntimeException("No object class " + oclass);
	}

	@Override
	public MutableState set(Object variableKey, Object value) {

		OOVariableKey key = OOStateUtilities.generateKey(variableKey);
		if(key.obName.equals(CLASS_AGENT)){
			this.agent = this.agent.copy();
			if(variableKey.equals(VAR_X)){
				agent.x = StateUtilities.stringOrNumber(value).intValue();
			}
			else if(variableKey.equals(VAR_Y)){
				agent.y = StateUtilities.stringOrNumber(value).intValue();
			}
			else if(variableKey.equals(VAR_DIR)) {
				agent.dir = StateUtilities.stringOrNumber(value).intValue();
			}
			else if(variableKey.equals(VAR_HOLD)){
				agent.holding = StateUtilities.stringOrBoolean(value);
			}
		}
		else if(key.obName.equals(CLASS_MAP)){
			this.map = map.copy();
			this.map.map = (int[][])value;
		}
		else if(key.obName.equals(exit.name())){
			Integer iv = StateUtilities.stringOrNumber(value).intValue();
			if(variableKey.equals(VAR_X)){
				exit.x = iv;
			}
			else if(variableKey.equals(VAR_Y)){
				exit.y = iv;
			}
		}
		else{
			int ind = blockForName(key.obName);
			if(ind != -1){
				BlockDudeCell block = this.blocks.get(ind).copy();
				this.blocks = new ArrayList<BlockDudeCell>(blocks);
				this.blocks.set(ind, block);
				Integer iv = StateUtilities.stringOrNumber(value).intValue();
				if(variableKey.equals(VAR_X)){
					block.x = iv;
				}
				else if(variableKey.equals(VAR_Y)){
					block.y = iv;
				}
			}
		}


		return this;
	}

	@Override
	public List<Object> variableKeys() {
		return OOStateUtilities.flatStateKeys(this);
	}

	@Override
	public Object get(Object variableKey) {
		return OOStateUtilities.get(this, variableKey);
	}

	@Override
	public State copy() {
		return new BlockDudeState(agent, map, exit, blocks);
	}

	public BlockDudeCell block(int i){
		return this.blocks.get(i);
	}

	protected int blockForName(String ob){
		int i = 0;
		for(BlockDudeCell b : this.blocks){
			if(b.name().equals(ob)){
				return i;
			}
			i++;
		}
		return -1;
	}

	@Override
	public String toString() {
		return OOStateUtilities.ooStateToString(this);
	}

	public void copyBlocks(){
		this.blocks = new ArrayList<BlockDudeCell>(blocks);
	}
}
