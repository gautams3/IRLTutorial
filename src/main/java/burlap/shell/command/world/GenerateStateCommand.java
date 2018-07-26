package burlap.shell.command.world;

import burlap.mdp.stochasticgames.world.World;
import burlap.shell.BurlapShell;
import burlap.shell.SGWorldShell;
import burlap.shell.command.ShellCommand;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.PrintStream;
import java.util.Scanner;

/**
 * A {@link burlap.shell.command.ShellCommand} for generating a new state in a {@link World}
 * according to the {@link World}'s assigned {@link burlap.mdp.auxiliary.StateGenerator}.
 * Use the -h option for help information.
 * @author James MacGlashan.
 */
public class GenerateStateCommand implements ShellCommand {

	protected OptionParser parser = new OptionParser("vh*");

	@Override
	public String commandName() {
		return "gs";
	}

	@Override
	public int call(BurlapShell shell, String argString, Scanner is, PrintStream os) {

		OptionSet oset = this.parser.parse(argString.split(" "));

		if(oset.has("h")) {
			os.println("[-v]\nCauses the world to generate a new initial state.\n\n" +
					"-v: print the new state after generating it.");
			return 0;
		}

		World w = ((SGWorldShell)shell).getWorld();
		w.generateNewCurrentState();

		if(oset.has("v")){
			os.println(w.getCurrentWorldState().toString());
		}

		return 1;
	}
}
