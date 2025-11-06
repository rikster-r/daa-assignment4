import java.util.*;
import java.util.stream.Collectors;

public class Kosaraju {
    private Metrics metrics;

    public Kosaraju() {
        this.metrics = new Metrics();
    }

    public SCCResult kosarajuSCC(Graph graph) {
        metrics.reset();
        metrics.startTiming();

        int n = graph.getN();
        List<List<Integer>> adj = graph.getAdjacencyList();
        List<List<Integer>> reverseAdj = graph.getReverseAdjacencyList();

        // Step 1: First DFS (on original graph) to get finishing times
        boolean[] visited = new boolean[n];
        Stack<Integer> stack = new Stack<>();

        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                kosarajuDFS1(i, adj, visited, stack);
            }
        }

        // Step 2: Second DFS (on reversed graph) in order of finishing times
        Arrays.fill(visited, false);
        List<List<Integer>> components = new ArrayList<>();
        List<Integer> componentSizes = new ArrayList<>();

        while (!stack.isEmpty()) {
            int node = stack.pop();
            metrics.incrementStackPops();

            if (!visited[node]) {
                List<Integer> component = new ArrayList<>();
                kosarajuDFS2(node, reverseAdj, visited, component);
                components.add(component);
                componentSizes.add(component.size());
            }
        }

        // Build condensation graph
        Graph condensationGraph = buildCondensationGraph(graph, components);

        metrics.stopTiming();
        return new SCCResult(components, componentSizes, condensationGraph, metrics);
    }

    private void kosarajuDFS1(int node, List<List<Integer>> adj, boolean[] visited, Stack<Integer> stack) {
        metrics.incrementDFSVisits();
        visited[node] = true;

        for (int neighbor : adj.get(node)) {
            metrics.incrementEdgesExplored();
            if (!visited[neighbor]) {
                kosarajuDFS1(neighbor, adj, visited, stack);
            }
        }

        metrics.incrementStackPushes();
        stack.push(node);
    }

    private void kosarajuDFS2(int node, List<List<Integer>> adj, boolean[] visited, List<Integer> component) {
        metrics.incrementDFSVisits();
        visited[node] = true;
        component.add(node);

        for (int neighbor : adj.get(node)) {
            metrics.incrementEdgesExplored();
            if (!visited[neighbor]) {
                kosarajuDFS2(neighbor, adj, visited, component);
            }
        }
    }

    private Graph buildCondensationGraph(Graph originalGraph, List<List<Integer>> components) {
        int n = components.size();
        Map<Integer, Integer> nodeToComponent = new HashMap<>();

        // Map each node to its component ID
        for (int compId = 0; compId < components.size(); compId++) {
            for (int node : components.get(compId)) {
                nodeToComponent.put(node, compId);
            }
        }

        // Build edges between components
        Set<String> componentEdges = new HashSet<>();
        List<Edge> condensationEdges = new ArrayList<>();

        for (Edge originalEdge : originalGraph.getEdges()) {
            int uComp = nodeToComponent.get(originalEdge.getU());
            int vComp = nodeToComponent.get(originalEdge.getV());

            // Add edge only if it connects different components
            if (uComp != vComp) {
                String edgeKey = uComp + "->" + vComp;
                if (!componentEdges.contains(edgeKey)) {
                    componentEdges.add(edgeKey);
                    condensationEdges.add(new Edge(uComp, vComp, 1));
                }
            }
        }

        return new Graph(n, condensationEdges, true, null, "unweighted");
    }

    public static class SCCResult {
        private final List<List<Integer>> components;
        private final List<Integer> componentSizes;
        private final Graph condensationGraph;
        private final Metrics metrics;

        public SCCResult(List<List<Integer>> components, List<Integer> componentSizes,
                         Graph condensationGraph, Metrics metrics) {
            this.components = components;
            this.componentSizes = componentSizes;
            this.condensationGraph = condensationGraph;
            this.metrics = metrics;
        }

        public List<List<Integer>> getComponents() { return components; }
        public List<Integer> getComponentSizes() { return componentSizes; }
        public Graph getCondensationGraph() { return condensationGraph; }
        public Metrics getMetrics() { return metrics; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Strongly Connected Components (Kosaraju's Algorithm):\n");
            for (int i = 0; i < components.size(); i++) {
                sb.append(String.format("  Component %d (size: %d): %s\n",
                        i + 1, componentSizes.get(i), components.get(i)));
            }
            sb.append("\nCondensation Graph:\n");
            sb.append("  Nodes: ").append(condensationGraph.getN()).append("\n");
            sb.append("  Edges: ").append(condensationGraph.getEdges().size()).append("\n");
            sb.append(metrics.getSummary());
            return sb.toString();
        }
    }
}