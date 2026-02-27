
package src.pas.pacman.routing;

// SYSTEM IMPORTS
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

// JAVA PROJECT IMPORTS
import edu.bu.pas.pacman.game.Game.GameView;
import edu.bu.pas.pacman.graph.Path;
import edu.bu.pas.pacman.graph.PelletGraph.PelletVertex;
import edu.bu.pas.pacman.routing.BoardRouter;
import edu.bu.pas.pacman.routing.PelletRouter;
import edu.bu.pas.pacman.utils.Coordinate;
import edu.bu.pas.pacman.utils.Pair;

public class ThriftyPelletRouter extends PelletRouter {
  
  // Custom Params to hold cache, but we handle null cases defensively
  public static class PelletExtraParams extends ExtraParams {
    public GameView game;
    public Map<Pair<Coordinate, Coordinate>, Float> distanceCache;
    
    public PelletExtraParams(GameView game) {
      this.game = game;
      this.distanceCache = new HashMap<>();
    }
  }

  private BoardRouter boardRouter;

  public ThriftyPelletRouter(int myUnitId, int pacmanId, int ghostChaseRadius) {
    super(myUnitId, pacmanId, ghostChaseRadius);
    this.boardRouter = new src.pas.pacman.routing.ThriftyBoardRouter(myUnitId, pacmanId, ghostChaseRadius);
  }

  @Override
  public Collection<PelletVertex> getOutgoingNeighbors(
      final PelletVertex src, final GameView game, final ExtraParams params) {
    Set<Coordinate> pellets = src.getRemainingPelletCoordinates();
    List<PelletVertex> neighbors = new ArrayList<>(pellets.size());
    for (Coordinate pellet : pellets) {
      neighbors.add(src.removePellet(pellet));
    }
    return neighbors;
  }

  @Override
  public float getEdgeWeight(
      final PelletVertex src, final PelletVertex dst, final ExtraParams params) {
    
    // 1. Identify Target Pellet
    Set<Coordinate> srcPellets = src.getRemainingPelletCoordinates();
    Set<Coordinate> dstPellets = dst.getRemainingPelletCoordinates();

    Coordinate targetPellet = null;
    for (Coordinate p : srcPellets) {
      if (!dstPellets.contains(p)) {
        targetPellet = p;
        break;
      }
    }
    if (targetPellet == null) return 1f;

    Coordinate pacmanPos = src.getPacmanCoordinate();

    // 2. Check Cache if available
    Map<Pair<Coordinate, Coordinate>, Float> cache = null;
    GameView game = null;
    
    if (params instanceof PelletExtraParams) {
        PelletExtraParams pp = (PelletExtraParams) params;
        cache = pp.distanceCache;
        game = pp.game;
    }

    if (cache != null) {
        Pair<Coordinate, Coordinate> key = new Pair<>(pacmanPos, targetPellet);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        
        // Compute and Cache
        if (game != null) {
            Path<Coordinate> path = this.boardRouter.graphSearch(pacmanPos, targetPellet, game);
            float cost = (path != null) ? path.getTrueCost() : manhattanDistance(pacmanPos, targetPellet);
            cache.put(key, cost);
            return cost;
        }
    }
    
    // Fallback if no cache/game provided
    return manhattanDistance(pacmanPos, targetPellet);
  }

  private float manhattanDistance(Coordinate c1, Coordinate c2) {
    return Math.abs(c1.x() - c2.x()) + Math.abs(c1.y() - c2.y());
  }

  /**
   * Optimized MST Heuristic using Prim's Algorithm with Arrays.
   * O(N^2) complexity where N is number of pellets.
   * Much faster than PQ based approach for dense graphs.
   */
  private float computeFastMSTHeuristic(final PelletVertex src) {
    Set<Coordinate> pellets = src.getRemainingPelletCoordinates();
    int n = pellets.size();
    
    if (n == 0) return 0f;

    Coordinate pacman = src.getPacmanCoordinate();
    
    // 1. Distance to nearest pellet
    float minToPellet = Float.MAX_VALUE;
    Coordinate startNode = null;
    
    // Use an ArrayList for indexed access during MST
    List<Coordinate> nodes = new ArrayList<>(n);
    
    for (Coordinate p : pellets) {
        nodes.add(p);
        float d = manhattanDistance(pacman, p);
        if (d < minToPellet) {
            minToPellet = d;
            startNode = p;
        }
    }
    
    if (n == 1) return minToPellet;

    // 2. MST of the pellets
    // Array-based Prim's algorithm
    float[] minEdge = new float[n];
    boolean[] visited = new boolean[n];
    
    // Init minEdge
    for(int i=0; i<n; i++) minEdge[i] = Float.MAX_VALUE;
    
    // Start MST from the pellet closest to Pacman
    int startIdx = nodes.indexOf(startNode);
    minEdge[startIdx] = 0f;
    
    float mstWeight = 0f;
    
    for (int i = 0; i < n; i++) {
        int u = -1;
        float minVal = Float.MAX_VALUE;
        
        // Find min unvisited
        for (int v = 0; v < n; v++) {
            if (!visited[v] && minEdge[v] < minVal) {
                minVal = minEdge[v];
                u = v;
            }
        }
        
        if (u == -1) break;
        
        visited[u] = true;
        mstWeight += minVal;
        
        Coordinate uCoord = nodes.get(u);
        
        // Update neighbors
        for (int v = 0; v < n; v++) {
            if (!visited[v]) {
                float dist = manhattanDistance(uCoord, nodes.get(v));
                if (dist < minEdge[v]) {
                    minEdge[v] = dist;
                }
            }
        }
    }

    return minToPellet + mstWeight;
  }

  @Override
  public float getHeuristic(final PelletVertex src, final GameView game, final ExtraParams params) {
    // We ignore params cache for the heuristic to keep it strictly admissible via Manhattan
    // This also avoids the NPE issues in unit tests.
    return computeFastMSTHeuristic(src);
  }

  @Override
  public Path<PelletVertex> graphSearch(final GameView game) {
    PelletVertex start = new PelletVertex(game);
    PelletExtraParams params = new PelletExtraParams(game);

    // Order by F = G + H
    // Path object stores G internally. H is stored in estimatedPathCostToGoal.
    PriorityQueue<Path<PelletVertex>> openSet =
        new PriorityQueue<>(
            Comparator.comparingDouble(p -> p.getTrueCost() + p.getEstimatedPathCostToGoal()));

    Set<PelletVertex> visited = new HashSet<>();
    
    float startH = getHeuristic(start, game, params);
    
    // Create root path. 
    // Note: Path(dst, edgeCost, heuristic, parent) is standard.
    // For root, we can use 1-arg constructor and set H.
    Path<PelletVertex> root = new Path<>(start);
    root.setEstimatedPathCostToGoal(startH);
    
    openSet.add(root);

    while (!openSet.isEmpty()) {
      Path<PelletVertex> currentPath = openSet.poll();
      PelletVertex currentVertex = currentPath.getDestination();

      if (currentVertex.getRemainingPelletCoordinates().isEmpty()) {
        return currentPath;
      }

      if (visited.contains(currentVertex)) {
        continue;
      }
      visited.add(currentVertex);

      for (PelletVertex neighbor : getOutgoingNeighbors(currentVertex, game, params)) {
        if (!visited.contains(neighbor)) {
          float edgeCost = getEdgeWeight(currentVertex, neighbor, params);
          float heuristic = getHeuristic(neighbor, game, params);
          
          // Create new path node
          Path<PelletVertex> newPath = new Path<>(neighbor, edgeCost, heuristic, currentPath);
          openSet.add(newPath);
        }
      }
    }

    return null;
  }
}

