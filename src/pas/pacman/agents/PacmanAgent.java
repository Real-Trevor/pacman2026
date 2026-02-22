package src.pas.pacman.agents;


// SYSTEM IMPORTS
import edu.bu.pas.pacman.agents.Agent;
import edu.bu.pas.pacman.agents.SearchAgent;
import edu.bu.pas.pacman.game.Action;
import edu.bu.pas.pacman.game.Game.GameView;
import edu.bu.pas.pacman.graph.Path;
import edu.bu.pas.pacman.graph.PelletGraph.PelletVertex;
import edu.bu.pas.pacman.routing.BoardRouter;
import edu.bu.pas.pacman.routing.PelletRouter;
import edu.bu.pas.pacman.utils.Coordinate;
import edu.bu.pas.pacman.utils.Pair;

import java.util.Random;
import java.util.Set;
import java.util.Stack;


// JAVA PROJECT IMPORTS
import src.pas.pacman.routing.ThriftyBoardRouter;  // responsible for how to get somewhere
import src.pas.pacman.routing.ThriftyPelletRouter; // responsible for pellet order


public class PacmanAgent
    extends SearchAgent
{

    private final Random random;
    private BoardRouter  boardRouter;
    private PelletRouter pelletRouter;

    public PacmanAgent(int myUnitId,
                       int pacmanId,
                       int ghostChaseRadius)
    {
        super(myUnitId, pacmanId, ghostChaseRadius);
        this.random = new Random();

        this.boardRouter = new ThriftyBoardRouter(myUnitId, pacmanId, ghostChaseRadius);
        this.pelletRouter = new ThriftyPelletRouter(myUnitId, pacmanId, ghostChaseRadius);
    }

    public final Random getRandom() { return this.random; }
    public final BoardRouter getBoardRouter() { return this.boardRouter; }
    public final PelletRouter getPelletRouter() { return this.pelletRouter; }

    @Override
    public void makePlan(final GameView game)
    {
        // TODO: implement me! This method is responsible for calculating
        // the "plan" of Coordinates you should visit in order to get from a starting
        // location and another ending location. I recommend you use
        // this.getBoardRouter().graphSearch(...) to get a path and convert it into
        // a Stack of Coordinates (see the documentation for SearchAgent)
        // which your makeMove can do something with!
        // Get Pacman's current coordinate
    Coordinate src =
        game.getEntity(game.getPacmanId()).getCurrentCoordinate();

    // Find a pellet by scanning the board
    Coordinate target = null;

    for (int x = 0; x < game.getXBoardDimension(); x++) {
        for (int y = 0; y < game.getYBoardDimension(); y++) {

            Coordinate c = new Coordinate(x, y);

            if (!game.isInBounds(c)) {
                continue;
            }

            if (game.getTile(c).getState()
                    == edu.bu.pas.pacman.game.Tile.State.PELLET) {

                target = c;
                break;
            }
        }
        if (target != null) {
            break;
        }
    }

    if (target == null) {
        this.setPlanToGetToTarget(new Stack<>());
        return;
    }

    this.setTargetCoordinate(target);

    Path<Coordinate> path =
        this.getBoardRouter().graphSearch(src, target, game);

    Stack<Coordinate> plan = new Stack<>();

    while (path != null) {
        plan.push(path.getDestination());
        path = path.getParentPath();
    }

    // Remove current position (top will be src)
    if (!plan.isEmpty()) {
        plan.pop();
    }

    this.setPlanToGetToTarget(plan);
    }

    @Override
    public Action makeMove(final GameView game)
    {
        // This is currently configured to choose a random action
        // TODO: change me!
        {
    Stack<Coordinate> plan =
        this.getPlanToGetToTarget();

    if (plan == null || plan.isEmpty()) {
        this.makePlan(game);
        plan = this.getPlanToGetToTarget();
    }

    if (plan == null || plan.isEmpty()) {
        return Action.UP;
    }

    Coordinate current =
        game.getEntity(game.getPacmanId())
            .getCurrentCoordinate();

    Coordinate next = plan.pop();

    try {
        return Action.inferFromCoordinates(current, next);
    } catch (Exception e) {
        return Action.UP;
        }
   }
        //return Action.values()[this.getRandom().nextInt(Action.values().length)];
    }

    @Override
    public void afterGameEnds(final GameView game)
    {
        // if you want to log stuff after a game ends implement me!
    }
}
