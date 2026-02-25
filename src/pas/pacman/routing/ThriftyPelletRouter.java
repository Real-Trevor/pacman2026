package src.pas.pacman.routing;


import java.util.ArrayList;
// SYSTEM IMPORTS
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

// JAVA PROJECT IMPORTS
import edu.bu.pas.pacman.game.Action;
import edu.bu.pas.pacman.game.Game.GameView;
import edu.bu.pas.pacman.game.Tile;
import edu.bu.pas.pacman.graph.Path;
import edu.bu.pas.pacman.graph.PelletGraph.PelletVertex;
import edu.bu.pas.pacman.routing.PelletRouter;
import edu.bu.pas.pacman.routing.PelletRouter.ExtraParams;
import edu.bu.pas.pacman.utils.Coordinate;
import edu.bu.pas.pacman.utils.Pair;


public class ThriftyPelletRouter
    extends PelletRouter
{

    // If you want to encode other information you think is useful for planning the order
    // of pellets ot eat besides Coordinates and data available in GameView
    // you can do so here.
    public static class PelletExtraParams
        extends ExtraParams
    {

    }

    // feel free to add other fields here!

    public ThriftyPelletRouter(int myUnitId,
                               int pacmanId,
                               int ghostChaseRadius)
    {
        super(myUnitId, pacmanId, ghostChaseRadius);

        // if you add fields don't forget to initialize them here!
    }

    @Override
    public Collection<PelletVertex> getOutgoingNeighbors(final PelletVertex src,
                                                         final GameView game,
                                                         final ExtraParams params)
    {
        List<PelletVertex> neighbors = new ArrayList<>();

        for (Coordinate pelletCoordinate : src.getRemainingPelletCoordinates())
            {
                PelletVertex neighbor = src.removePellet(pelletCoordinate);
                neighbors.add(neighbor);
            }
        return neighbors;
    }

    @Override
    public float getEdgeWeight(final PelletVertex src,
                               final PelletVertex dst,
                               final ExtraParams params)
    {
        // TODO: implement me!
        return 1f;
    }

    @Override
    public float getHeuristic(final PelletVertex src,
                              final GameView game,
                              final ExtraParams params)
    {
        Set<Coordinate> pellets = src.getRemainingPelletCoordinates();
        Coordinate pacman = src.getPacmanCoordinate();

        Set<Coordinate> allNodes = new HashSet<>(pellets);
        allNodes.add(pacman);

        HashMap<Coordinate, Coordinate> tree = new HashMap<>();

        for (Coordinate coord1 : allNodes) {
            for (Coordinate coord2 : allNodes) {
                if (!coord1.equals(coord2)) {
                    tree.put(coord1, coord2);
                }
            }
        }
        

        return 0f;
    }

    @Override
    public Path<PelletVertex> graphSearch(final GameView game) 
    {
        // TODO: implement me!
        return null;
    }

}

