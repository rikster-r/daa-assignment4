import java.util.*;

public class DAGShortestPath {
    private Metrics metrics;

    public DAGShortestPath() {
        this.metrics = new Metrics();
    }

    public static class ShortestPathResult {
        private final int[] distances;
        private final int[] predecessors;
        private final Integer source;
        private final int criticalPathLength;
        private final List<Integer> criticalPath;
        private final Metrics metrics;

        public ShortestPathResult(int[] distances, int[] predecessors, Integer source,
                                  int criticalPathLength, List<Integer> criticalPath, Metrics metrics) {
            this.distances = distances;
            this.predecessors = predecessors;
            this.source = source;
            this.criticalPathLength = criticalPathLength;
            this.criticalPath = criticalPath;
            this.metrics = metrics;
        }

        public int[] getDistances() { return distances; }
        public int[] getPredecessors() { return predecessors; }
        public Integer getSource() { return source; }
        public int getCriticalPathLength() { return criticalPathLength; }
        public List<Integer> getCriticalPath() { return criticalPath; }
        public Metrics getMetrics() { return metrics; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Shortest Path Results:\n");
            if (source != null) {
                sb.append("Source: ").append(source).append("\n");
                for (int i = 0; i < distances.length; i++) {
                    if (distances[i] == Integer.MAX_VALUE) {
                        sb.append(String.format("  Distance to %d: INFINITY\n", i));
                    } else {
                        sb.append(String.format("  Distance to %d: %d\n", i, distances[i]));
                    }
                }
            }
            sb.append("\nCritical Path Analysis:\n");
            sb.append("  Critical Path Length: ").append(criticalPathLength).append("\n");
            sb.append("  Critical Path: ").append(criticalPath).append("\n");
            sb.append(metrics.getSummary());
            return sb.toString();
        }
    }

    public ShortestPathResult findShortestPaths(Graph dag, Integer source) {
        metrics.reset();
        metrics.startTiming();

        int n = dag.getN();
        List<Edge> edges = dag.getEdges();

        // Step 1: Perform topological sort
        List<Integer> topoOrder = topologicalSort(dag);

        // Step 2: Initialize distances and predecessors
        int[] dist = new int[n];
        int[] pred = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(pred, -1);

        if (source != null) {
            dist[source] = 0;
        }

        // Step 3: Process vertices in topological order - Shortest Path
        for (int u : topoOrder) {
            if (source == null || dist[u] != Integer.MAX_VALUE) {
                for (Edge edge : edges) {
                    if (edge.getU() == u) {
                        int v = edge.getV();
                        int weight = edge.getW();

                        metrics.incrementRelaxations();
                        if (dist[u] != Integer.MAX_VALUE && dist[u] + weight < dist[v]) {
                            dist[v] = dist[u] + weight;
                            pred[v] = u;
                        }
                    }
                }
            }
        }

        // Step 4: Find critical path (longest path) using negative weights
        int criticalPathLength = findCriticalPath(dag, topoOrder);
        List<Integer> criticalPath = reconstructCriticalPath(dag, topoOrder, criticalPathLength);

        metrics.stopTiming();
        return new ShortestPathResult(dist, pred, source, criticalPathLength, criticalPath, metrics);
    }

    private List<Integer> topologicalSort(Graph dag) {
        int n = dag.getN();
        List<List<Integer>> adj = dag.getAdjacencyList();
        int[] inDegree = new int[n];

        // Calculate in-degrees
        for (Edge edge : dag.getEdges()) {
            inDegree[edge.getV()]++;
        }

        // Initialize queue with nodes having zero in-degree
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            if (inDegree[i] == 0) {
                queue.offer(i);
            }
        }

        List<Integer> topoOrder = new ArrayList<>();
        while (!queue.isEmpty()) {
            int u = queue.poll();
            topoOrder.add(u);

            for (int v : adj.get(u)) {
                inDegree[v]--;
                if (inDegree[v] == 0) {
                    queue.offer(v);
                }
            }
        }

        return topoOrder;
    }

    private int findCriticalPath(Graph dag, List<Integer> topoOrder) {
        int n = dag.getN();
        List<Edge> edges = dag.getEdges();

        // For critical path, we find the longest path by using negative weights
        // and then taking the absolute value, OR we can modify the relaxation condition
        int[] longest = new int[n];
        Arrays.fill(longest, Integer.MIN_VALUE);

        // Initialize all nodes with 0 if we consider node durations
        // For edge-weighted approach, we'll find the maximum path
        for (int i = 0; i < n; i++) {
            longest[i] = 0; // Assuming each node has unit duration
        }

        // Process in topological order for longest path
        for (int u : topoOrder) {
            for (Edge edge : edges) {
                if (edge.getU() == u) {
                    int v = edge.getV();
                    int weight = edge.getW(); // Using edge weights for critical path

                    metrics.incrementRelaxations();
                    if (longest[u] + weight > longest[v]) {
                        longest[v] = longest[u] + weight;
                    }
                }
            }
        }

        // Find the maximum distance (critical path length)
        int maxDist = 0;
        for (int distance : longest) {
            if (distance > maxDist) {
                maxDist = distance;
            }
        }

        return maxDist;
    }

    private List<Integer> reconstructCriticalPath(Graph dag, List<Integer> topoOrder, int criticalPathLength) {
        int n = dag.getN();
        List<Edge> edges = dag.getEdges();

        // Backtrack to find the critical path
        int[] longest = new int[n];
        int[] pred = new int[n];
        Arrays.fill(longest, 0);
        Arrays.fill(pred, -1);

        // Forward pass to compute longest paths and predecessors
        for (int u : topoOrder) {
            for (Edge edge : edges) {
                if (edge.getU() == u) {
                    int v = edge.getV();
                    int weight = edge.getW();

                    if (longest[u] + weight > longest[v]) {
                        longest[v] = longest[u] + weight;
                        pred[v] = u;
                    }
                }
            }
        }

        // Find the node with maximum distance (end of critical path)
        int endNode = -1;
        int maxDist = -1;
        for (int i = 0; i < n; i++) {
            if (longest[i] > maxDist) {
                maxDist = longest[i];
                endNode = i;
            }
        }

        // Reconstruct the path backwards
        List<Integer> criticalPath = new ArrayList<>();
        int current = endNode;
        while (current != -1) {
            criticalPath.add(current);
            current = pred[current];
        }
        Collections.reverse(criticalPath);

        return criticalPath;
    }

    public List<Integer> reconstructPath(ShortestPathResult result, int target) {
        if (result.getSource() == null) {
            throw new IllegalStateException("No source specified for path reconstruction");
        }

        if (result.getDistances()[target] == Integer.MAX_VALUE) {
            return Collections.emptyList(); // No path exists
        }

        List<Integer> path = new ArrayList<>();
        int current = target;

        while (current != -1) {
            path.add(current);
            current = result.getPredecessors()[current];
        }

        Collections.reverse(path);
        return path;
    }
}