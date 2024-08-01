package name.dunderbotdlc.commands;

import java.util.ArrayList;

public class jumpSprintState {
    
    public SimInstance state;
    public boolean open;
    public boolean shouldJump;
    public double score;
    public ArrayList<jumpSprintState> children;
    public jumpSprintState(SimInstance myState, boolean open, boolean shouldJump, double myScore) {
        this.state = myState;
        this.open = open;
        this.shouldJump = shouldJump;
        this.score = myScore;
        this.children = new ArrayList<jumpSprintState>();
    }

    public void addChild() {
        
    }
}
