package src.pas.pacman.routing;

// SYSTEM IMPORTS
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;

// JAVA PROJECT IMPORTS
import edu.bu.pas.pacman.game.Action;
import edu.bu.pas.pacman.game.Game.GameView;
import edu.bu.pas.pacman.game.Tile;
import edu.bu.pas.pacman.graph.Path;
import edu.bu.pas.pacman.routing.BoardRouter;
import edu.bu.pas.pacman.routing.BoardRouter.ExtraParams;
import edu.bu.pas.pacman.utils.Coordinate;

public class ThriftyBoardRouter extends BoardRouter
{

    public static class BoardExtraParams extends ExtraParams
    {
        // No extra parameters needed for basic BFS
    }

    public ThriftyBoardRouter(int myUnitId,
                              int pacmanId,
                              int ghostChaseRadius)
    {
        super(myUnitId, pacmanId, ghostChaseRadius);
    }

    @Override
    public Collection<Coordinate> getOutgoingNeighbors(
        final Coordinate src,
        final GameView game,
        final ExtraParams params)
    {
        List<Coordinate> neighbors = new ArrayList<>();

        for (Action action : Action.values())
        {
            Coordinate next = src.getNeighbor(action);

            if (!game.isInBounds(next))
            {
                continue;
            }

            Tile tile = game.getTile(next);

            if (tile.getState() == Tile.State.WALL)
            {
                continue;
            }

            neighbors.add(next);
        }

        return neighbors;
    }

    @Override
    public Path<Coordinate> graphSearch(
        final Coordinate src,
        final Coordinate tgt,
        final GameView game)
    {
        if (src.equals(tgt))
        {
            return new Path<>(src);
        }

        Queue<Path<Coordinate>> frontier = new LinkedList<>();
        Set<Coordinate> visited = new HashSet<>();

        frontier.add(new Path<>(src));
        visited.add(src);

        while (!frontier.isEmpty())
        {
            Path<Coordinate> currentPath = frontier.poll();
            Coordinate current = currentPath.getDestination();

            if (current.equals(tgt))
            {
                return currentPath;
            }

            for (Coordinate neighbor :
                    getOutgoingNeighbors(current, game, null))
            {
                if (!visited.contains(neighbor))
                {
                    visited.add(neighbor);

                    Path<Coordinate> newPath =
                        new Path<>(neighbor, 1.0f, currentPath);

                    frontier.add(newPath);
                }
            }
        }

        return null;
    }
}
