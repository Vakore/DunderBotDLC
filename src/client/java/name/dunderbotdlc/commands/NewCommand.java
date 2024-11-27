/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package name.dunderbotdlc.commands;

import baritone.api.IBaritone;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
//import baritone.api.command.exception.CommandInvalidStateException;
import baritone.api.pathing.calc.IPathingControlManager;
import baritone.api.process.IBaritoneProcess;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class NewCommand extends Command {

    public NewCommand(IBaritone baritone) {
        super(baritone, "newcommand");
    }

    @Override
    public void execute(String label, IArgConsumer args) {
        args.requireMax(0);
        IPathingControlManager pathingControlManager = baritone.getPathingControlManager();
        IBaritoneProcess process = pathingControlManager.mostRecentInControl().orElse(null);
        if (process == null) {
            //throw new CommandInvalidStateException("No process in control");
        } else {
            IPathingBehavior pathingBehavior = baritone.getPathingBehavior();

            double ticksRemainingInSegment = pathingBehavior.ticksRemainingInSegment().orElse(Double.NaN);
            double ticksRemainingInGoal = pathingBehavior.estimatedTicksToGoal().orElse(Double.NaN);

            logDirect(String.format(
                    "Next segment: %.1fs (%.0f ticks)\n" +
                            "Goal: %.1fs (%.0f ticks)",
                    ticksRemainingInSegment / 20, // we just assume tps is 20, it isn't worth the effort that is needed to calculate it exactly
                    ticksRemainingInSegment,
                    ticksRemainingInGoal / 20,
                    ticksRemainingInGoal
            ));
            logDirect("This is a NewCommand from DunderLC");
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "View the current ETA";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "The ETA command provides information about the estimated time until the next segment.",
                "and the goal",
                "",
                "Amogus, jump sprinting WIP",
                "",
                "Usage:",
                "> newcommand - View ETA, if present"
        );
    }
}