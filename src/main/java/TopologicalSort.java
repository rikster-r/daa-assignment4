import java.util.*;


public class TopologicalSort {
    private final Metrics metrics;

    public TopologicalSort() {
        this.metrics = new Metrics();
    }


    public static class TopoSortResult {
        private final List<Integer> order;
        private final boolean isDAG;
        private final Metrics metrics;

        public TopoSortResult(List<Integer> order, boolean isDAG, Metrics metrics) {
            this.order = order;
            this.isDAG = isDAG;
            this.metrics = metrics;
        }

        public List<Integer> getOrder() { return order; }
        public boolean isDAG() { return isDAG; }
        public Metrics getMetrics() { return metrics; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Topological Sort Result:\n");
            sb.append("  Is DAG: ").append(isDAG).append("\n");
            sb.append("  Order: ").append(order).append("\n");
            sb.append(metrics.getSummary());
            return sb.toString();
        }
    }


    public TopoSortResult kahnSort(Graph graph) {
        metrics.reset();
        metrics.startTiming();

        int n = graph.getN();
        List<List<Integer>> adj = graph.getAdjacencyList();
        int[] indegree = new int[n];

        for (Edge e : graph.getEdges()) {
            indegree[e.getV()]++;
            metrics.incrementEdgesExplored();
        }

        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            if (indegree[i] == 0) {
                queue.offer(i);
                metrics.incrementStackPushes();
            }
        }

        List<Integer> topoOrder = new ArrayList<>();
        while (!queue.isEmpty()) {
            int u = queue.poll();
            metrics.incrementStackPops();
            topoOrder.add(u);

            for (int v : adj.get(u)) {
                indegree[v]--;
                metrics.incrementEdgesExplored();
                if (indegree[v] == 0) {
                    queue.offer(v);
                    metrics.incrementStackPushes();
                }
            }
        }

        metrics.stopTiming();

        boolean isDAG = topoOrder.size() == n;
        if (!isDAG) {
            System.err.println("[Warning] Graph contains a cycle — Topological order is incomplete!");
        }

        return new TopoSortResult(topoOrder, isDAG, metrics);
    }


    public TopoSortResult dfsSort(Graph graph) {
        metrics.reset();
        metrics.startTiming();

        int n = graph.getN();
        List<List<Integer>> adj = graph.getAdjacencyList();
        boolean[] visited = new boolean[n];
        boolean[] inStack = new boolean[n];
        Deque<Integer> stack = new ArrayDeque<>();

        boolean[] hasCycle = {false};

        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                dfs(i, adj, visited, inStack, stack, hasCycle);
            }
        }

        metrics.stopTiming();

        List<Integer> topoOrder = new ArrayList<>(stack);
        Collections.reverse(topoOrder);

        boolean isDAG = !hasCycle[0];
        if (!isDAG) {
            System.err.println("[Warning] Cycle detected during DFS — graph is not a DAG!");
        }

        return new TopoSortResult(topoOrder, isDAG, metrics);
    }

    private void dfs(int u, List<List<Integer>> adj, boolean[] visited,
                     boolean[] inStack, Deque<Integer> stack, boolean[] hasCycle) {
        visited[u] = true;
        inStack[u] = true;
        metrics.incrementDFSVisits();

        for (int v : adj.get(u)) {
            metrics.incrementEdgesExplored();
            if (!visited[v]) {
                dfs(v, adj, visited, inStack, stack, hasCycle);
            } else if (inStack[v]) {
                hasCycle[0] = true;
            }
        }

        inStack[u] = false;
        stack.push(u);
        metrics.incrementStackPushes();
    }


    public void printResult(TopoSortResult result) {
        System.out.println(result);
    }
}