package src.pas.pacman.agents;

// SYSTEM IMPORTS
import edu.bu.pas.pacman.agents.SearchAgent;
import edu.bu.pas.pacman.game.Action;
import edu.bu.pas.pacman.game.Game.GameView;
import edu.bu.pas.pacman.graph.Path;
import edu.bu.pas.pacman.graph.PelletGraph.PelletVertex;
import edu.bu.pas.pacman.routing.BoardRouter;
import edu.bu.pas.pacman.routing.PelletRouter;
import edu.bu.pas.pacman.utils.Coordinate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Stack;

// JAVA PROJECT IMPORTS
import src.pas.pacman.routing.ThriftyBoardRouter;
import src.pas.pacman.routing.ThriftyPelletRouter;

public class PacmanAgent extends SearchAgent {

    private final Random random;
    private BoardRouter boardRouter;
    private PelletRouter pelletRouter;

    public PacmanAgent(int myUnitId, int pacmanId, int ghostChaseRadius) {
        super(myUnitId, pacmanId, ghostChaseRadius);
        this.random = new Random();
        this.boardRouter = new ThriftyBoardRouter(myUnitId, pacmanId, ghostChaseRadius);
        this.pelletRouter = new ThriftyPelletRouter(myUnitId, pacmanId, ghostChaseRadius);
    }

    public final Random getRandom() { return this.random; }
    public final BoardRouter getBoardRouter() { return this.boardRouter; }
    public final PelletRouter getPelletRouter() { return this.pelletRouter; }

    @Override
    public void makePlan(final GameView game) {
        Coordinate src = game.getEntity(game.getPacmanId()).getCurrentCoordinate();
        Coordinate target = this.getTargetCoordinate();

        // CASE 1: Specific target set (Unit Tests, specific movement)
        if (target != null) {
            Path<Coordinate> path = this.getBoardRouter().graphSearch(src, target, game);
            if (path != null) {
                Stack<Coordinate> plan = pathToStack(path);
                this.setPlanToGetToTarget(plan);
            } else {
                this.setPlanToGetToTarget(new Stack<>());
            }
            return;
        }

        // CASE 2: No target set (Play Game: Eat all pellets efficiently)
        // Use PelletRouter to find the best order to eat pellets
        Path<PelletVertex> tour = this.getPelletRouter().graphSearch(game);
        
        if (tour != null) {
            // Unroll the high-level tour (Goal -> Start)
            List<PelletVertex> tourStates = new ArrayList<>();
            Path<PelletVertex> p = tour;
            while (p != null) {
                tourStates.add(p.getDestination());
                p = p.getParentPath();
            }
            Collections.reverse(tourStates); // Now Start -> Goal

            Stack<Coordinate> fullPlan = new Stack<>();
            
            // Build movement plan backwards (Last pellet -> ... -> First pellet)
            for (int i = tourStates.size() - 1; i > 0; i--) {
                Coordinate from = tourStates.get(i - 1).getPacmanCoordinate();
                Coordinate to = tourStates.get(i).getPacmanCoordinate();
                
                // Route between pellets
                Path<Coordinate> leg = this.getBoardRouter().graphSearch(from, to, game);
                if (leg != null) {
                    List<Coordinate> legCoords = new ArrayList<>();
                    Path<Coordinate> legP = leg;
                    // Add coords, excluding the 'from' node to avoid duplicates
                    while (legP != null && legP.getParentPath() != null) {
                        legCoords.add(legP.getDestination());
                        legP = legP.getParentPath();
                    }
                    // LegCoords is (Dest -> ... -> NextStep). 
                    // Stack needs NextStep at top.
                    for (Coordinate c : legCoords) {
                        fullPlan.push(c);
                    }
                }
            }
            this.setPlanToGetToTarget(fullPlan);
        }
    }

    // Helper to convert Path<Coordinate> to Stack<Coordinate>
    private Stack<Coordinate> pathToStack(Path<Coordinate> path) {
        List<Coordinate> coords = new ArrayList<>();
        Path<Coordinate> p = path;
        while (p != null && p.getParentPath() != null) { // Exclude source
            coords.add(p.getDestination());
            p = p.getParentPath();
        }
        // Coords: Dest -> ... -> FirstStep
        // Stack should pop FirstStep first.
        Stack<Coordinate> plan = new Stack<>();
        for (Coordinate c : coords) {
            plan.push(c);
        }
        return plan;
    }

    @Override
    public Action makeMove(final GameView game) {
        Stack<Coordinate> plan = this.getPlanToGetToTarget();

        if (plan == null || plan.isEmpty()) {
            this.makePlan(game);
            plan = this.getPlanToGetToTarget();
        }

        if (plan == null || plan.isEmpty()) {
            return Action.UP;
        }

        Coordinate current = game.getEntity(game.getPacmanId()).getCurrentCoordinate();
        Coordinate next = plan.peek(); // Peek first

        // Check if we are already there (edge case)
        if (current.equals(next)) {
            plan.pop();
            if (plan.isEmpty()) return Action.UP;
            next = plan.peek();
        }

        next = plan.pop();

        try {
            return Action.inferFromCoordinates(current, next);
        } catch (Exception e) {
            return Action.UP;
        }
    }

    @Override
    public void afterGameEnds(final GameView game) {}
}

