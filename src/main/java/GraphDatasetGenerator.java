import java.util.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GraphDatasetGenerator {
    private Random random;

    public GraphDatasetGenerator() {
        this.random = new Random(42); // Fixed seed for reproducibility
    }

    public GraphDatasetGenerator(long seed) {
        this.random = new Random(seed);
    }

    public static class GraphMetadata {
        private final String name;
        private final int nodes;
        private final int edges;
        private final boolean isCyclic;
        private final int sccCount;
        private final String density;
        private final String description;

        public GraphMetadata(String name, int nodes, int edges, boolean isCyclic,
                             int sccCount, String density, String description) {
            this.name = name;
            this.nodes = nodes;
            this.edges = edges;
            this.isCyclic = isCyclic;
            this.sccCount = sccCount;
            this.density = density;
            this.description = description;
        }

        // Getters
        public String getName() { return name; }
        public int getNodes() { return nodes; }
        public int getEdges() { return edges; }
        public boolean isCyclic() { return isCyclic; }
        public int getSccCount() { return sccCount; }
        public String getDensity() { return density; }
        public String getDescription() { return description; }

        @Override
        public String toString() {
            return String.format(
                    "%-20s | Nodes: %-3d | Edges: %-3d | Cyclic: %-5s | SCCs: %-2d | Density: %-8s | %s",
                    name, nodes, edges, isCyclic, sccCount, density, description
            );
        }
    }

    public void generateAllDatasets() {
        List<GraphMetadata> allMetadata = new ArrayList<>();

        // Create data directory
        try {
            Files.createDirectories(Paths.get("data"));
        } catch (IOException e) {
            System.err.println("Failed to create data directory: " + e.getMessage());
            return;
        }

        // Small graphs (6-10 nodes)
        System.out.println("Generating small graphs (6-10 nodes)...");
        allMetadata.add(generateSmallDAG("small_dag_1", 8, 0.3));
        allMetadata.add(generateSmallCyclic("small_cyclic_1", 7, 0.4, 2));
        allMetadata.add(generateSmallMixed("small_mixed_1", 9, 0.35));

        // Medium graphs (10-20 nodes)
        System.out.println("Generating medium graphs (10-20 nodes)...");
        allMetadata.add(generateMediumDAG("medium_dag_1", 15, 0.25));
        allMetadata.add(generateMediumCyclic("medium_cyclic_1", 18, 0.3, 3));
        allMetadata.add(generateMediumMixed("medium_mixed_1", 12, 0.4));

        // Large graphs (20-50 nodes)
        System.out.println("Generating large graphs (20-50 nodes)...");
        allMetadata.add(generateLargeDAG("large_dag_1", 35, 0.2));
        allMetadata.add(generateLargeCyclic("large_cyclic_1", 45, 0.25, 5));
        allMetadata.add(generateLargeMixed("large_mixed_1", 28, 0.3));

        // Save metadata report
        saveMetadataReport(allMetadata);
        System.out.println("\nGenerated 9 datasets in /data/ directory");
    }

    // Small graph generators (6-10 nodes)
    private GraphMetadata generateSmallDAG(String name, int nodes, double density) {
        List<Edge> edges = generateDAG(nodes, density);
        saveGraph(name, nodes, edges);
        return new GraphMetadata(name, nodes, edges.size(), false, nodes,
                getDensityLevel(density), "Pure DAG, no cycles");
    }

    private GraphMetadata generateSmallCyclic(String name, int nodes, double density, int cycleCount) {
        List<Edge> edges = generateCyclicGraph(nodes, density, cycleCount);
        saveGraph(name, nodes, edges);
        return new GraphMetadata(name, nodes, edges.size(), true, cycleCount,
                getDensityLevel(density), "Cyclic with " + cycleCount + " SCCs");
    }

    private GraphMetadata generateSmallMixed(String name, int nodes, double density) {
        List<Edge> edges = generateMixedGraph(nodes, density);
        saveGraph(name, nodes, edges);
        // Count SCCs by analyzing the graph
        int sccCount = estimateSCCCount(edges, nodes);
        return new GraphMetadata(name, nodes, edges.size(), true, sccCount,
                getDensityLevel(density), "Mixed structure with cycles and DAG parts");
    }

    // Medium graph generators (10-20 nodes)
    private GraphMetadata generateMediumDAG(String name, int nodes, double density) {
        List<Edge> edges = generateLayeredDAG(nodes, density, 4);
        saveGraph(name, nodes, edges);
        return new GraphMetadata(name, nodes, edges.size(), false, nodes,
                getDensityLevel(density), "Layered DAG with 4 layers");
    }

    private GraphMetadata generateMediumCyclic(String name, int nodes, double density, int sccCount) {
        List<Edge> edges = generateMultipleSCCs(nodes, density, sccCount);
        saveGraph(name, nodes, edges);
        return new GraphMetadata(name, nodes, edges.size(), true, sccCount,
                getDensityLevel(density), "Multiple SCCs with inter-component edges");
    }

    private GraphMetadata generateMediumMixed(String name, int nodes, double density) {
        List<Edge> edges = generateComplexMixedGraph(nodes, density);
        saveGraph(name, nodes, edges);
        int sccCount = estimateSCCCount(edges, nodes);
        return new GraphMetadata(name, nodes, edges.size(), true, sccCount,
                getDensityLevel(density), "Complex mixed structure");
    }

    // Large graph generators (20-50 nodes)
    private GraphMetadata generateLargeDAG(String name, int nodes, double density) {
        List<Edge> edges = generateLargeScaleDAG(nodes, density);
        saveGraph(name, nodes, edges);
        return new GraphMetadata(name, nodes, edges.size(), false, nodes,
                getDensityLevel(density), "Large scale DAG for performance testing");
    }

    private GraphMetadata generateLargeCyclic(String name, int nodes, double density, int sccCount) {
        List<Edge> edges = generateLargeCyclicGraph(nodes, density, sccCount);
        saveGraph(name, nodes, edges);
        return new GraphMetadata(name, nodes, edges.size(), true, sccCount,
                getDensityLevel(density), "Large cyclic graph with multiple SCCs");
    }

    private GraphMetadata generateLargeMixed(String name, int nodes, double density) {
        List<Edge> edges = generateLargeMixedGraph(nodes, density);
        saveGraph(name, nodes, edges);
        int sccCount = estimateSCCCount(edges, nodes);
        return new GraphMetadata(name, nodes, edges.size(), true, sccCount,
                getDensityLevel(density), "Large mixed graph for comprehensive testing");
    }

    // Core graph generation methods
    private List<Edge> generateDAG(int nodes, double density) {
        List<Edge> edges = new ArrayList<>();
        int maxEdges = (int) (nodes * (nodes - 1) * density / 2);

        for (int u = 0; u < nodes; u++) {
            for (int v = u + 1; v < nodes; v++) {
                if (random.nextDouble() < density && edges.size() < maxEdges) {
                    edges.add(new Edge(u, v, random.nextInt(10) + 1));
                }
            }
        }
        return edges;
    }

    private List<Edge> generateLayeredDAG(int nodes, double density, int layers) {
        List<Edge> edges = new ArrayList<>();
        int[] layerAssignment = new int[nodes];

        // Assign nodes to layers
        for (int i = 0; i < nodes; i++) {
            layerAssignment[i] = random.nextInt(layers);
        }

        // Create edges only from lower layers to higher layers
        for (int u = 0; u < nodes; u++) {
            for (int v = 0; v < nodes; v++) {
                if (u != v && layerAssignment[u] < layerAssignment[v]) {
                    if (random.nextDouble() < density) {
                        edges.add(new Edge(u, v, random.nextInt(10) + 1));
                    }
                }
            }
        }
        return edges;
    }

    private List<Edge> generateCyclicGraph(int nodes, double density, int cycleCount) {
        List<Edge> edges = new ArrayList<>();

        // Create cycles
        for (int i = 0; i < cycleCount; i++) {
            int cycleSize = random.nextInt(3) + 2; // Cycle size between 2-4
            int startNode = random.nextInt(nodes - cycleSize);

            // Create cycle
            for (int j = 0; j < cycleSize; j++) {
                int u = startNode + j;
                int v = startNode + ((j + 1) % cycleSize);
                edges.add(new Edge(u, v, random.nextInt(10) + 1));
            }
        }

        // Add random edges to achieve desired density
        int currentEdges = edges.size();
        int targetEdges = (int) (nodes * nodes * density);

        while (edges.size() < targetEdges) {
            int u = random.nextInt(nodes);
            int v = random.nextInt(nodes);
            if (u != v && !edgeExists(edges, u, v)) {
                edges.add(new Edge(u, v, random.nextInt(10) + 1));
            }
        }

        return edges;
    }

    private List<Edge> generateMultipleSCCs(int nodes, double density, int sccCount) {
        List<Edge> edges = new ArrayList<>();
        int[] component = new int[nodes];

        // Assign nodes to components
        for (int i = 0; i < nodes; i++) {
            component[i] = random.nextInt(sccCount);
        }

        // Create strong connections within components
        for (int comp = 0; comp < sccCount; comp++) {
            List<Integer> compNodes = new ArrayList<>();
            for (int i = 0; i < nodes; i++) {
                if (component[i] == comp) compNodes.add(i);
            }

            if (compNodes.size() > 1) {
                // Create a cycle within the component
                Collections.shuffle(compNodes, random);
                for (int i = 0; i < compNodes.size(); i++) {
                    int u = compNodes.get(i);
                    int v = compNodes.get((i + 1) % compNodes.size());
                    edges.add(new Edge(u, v, random.nextInt(10) + 1));
                }
            }
        }

        // Add some cross-component edges
        for (int u = 0; u < nodes; u++) {
            for (int v = 0; v < nodes; v++) {
                if (component[u] != component[v] && random.nextDouble() < density * 0.3) {
                    edges.add(new Edge(u, v, random.nextInt(10) + 1));
                }
            }
        }

        return edges;
    }

    private List<Edge> generateMixedGraph(int nodes, double density) {
        List<Edge> edges = new ArrayList<>();

        // Start with a DAG base
        edges.addAll(generateDAG(nodes, density * 0.7));

        // Add some cycles
        int cycleCount = random.nextInt(3) + 1;
        for (int i = 0; i < cycleCount; i++) {
            int u = random.nextInt(nodes);
            int v = random.nextInt(nodes);
            if (u != v && !edgeExists(edges, u, v)) {
                edges.add(new Edge(u, v, random.nextInt(10) + 1));
                // Add reverse edge to create cycle
                if (random.nextBoolean() && !edgeExists(edges, v, u)) {
                    edges.add(new Edge(v, u, random.nextInt(10) + 1));
                }
            }
        }

        return edges;
    }

    private List<Edge> generateComplexMixedGraph(int nodes, double density) {
        // Combine DAG structures with multiple SCCs
        List<Edge> edges = generateMultipleSCCs(nodes / 2, density, 2);

        // Add DAG portion
        List<Edge> dagEdges = generateDAG(nodes / 2, density);
        // Offset the node indices for DAG portion
        for (Edge edge : dagEdges) {
            edges.add(new Edge(edge.getU() + nodes / 2, edge.getV() + nodes / 2, edge.getW()));
        }

        // Add some connecting edges between SCC and DAG parts
        for (int i = 0; i < nodes / 4; i++) {
            int u = random.nextInt(nodes / 2);
            int v = random.nextInt(nodes / 2) + nodes / 2;
            edges.add(new Edge(u, v, random.nextInt(10) + 1));
        }

        return edges;
    }

    private List<Edge> generateLargeScaleDAG(int nodes, double density) {
        return generateLayeredDAG(nodes, density, 8); // More layers for large DAG
    }

    private List<Edge> generateLargeCyclicGraph(int nodes, double density, int sccCount) {
        return generateMultipleSCCs(nodes, density, sccCount);
    }

    private List<Edge> generateLargeMixedGraph(int nodes, double density) {
        List<Edge> edges = new ArrayList<>();

        // Create multiple components with different characteristics
        int componentSize = nodes / 4;
        for (int comp = 0; comp < 4; comp++) {
            if (comp % 2 == 0) {
                // Even components are cyclic
                List<Edge> cyclicEdges = generateCyclicGraph(componentSize, density, 2);
                for (Edge edge : cyclicEdges) {
                    edges.add(new Edge(edge.getU() + comp * componentSize,
                            edge.getV() + comp * componentSize, edge.getW()));
                }
            } else {
                // Odd components are DAG
                List<Edge> dagEdges = generateDAG(componentSize, density);
                for (Edge edge : dagEdges) {
                    edges.add(new Edge(edge.getU() + comp * componentSize,
                            edge.getV() + comp * componentSize, edge.getW()));
                }
            }
        }

        // Add cross-component edges
        for (int i = 0; i < nodes * 2; i++) {
            int u = random.nextInt(nodes);
            int v = random.nextInt(nodes);
            if (u / componentSize != v / componentSize && random.nextDouble() < density * 0.2) {
                edges.add(new Edge(u, v, random.nextInt(10) + 1));
            }
        }

        return edges;
    }

    // Utility methods
    private boolean edgeExists(List<Edge> edges, int u, int v) {
        return edges.stream().anyMatch(e -> e.getU() == u && e.getV() == v);
    }

    private int estimateSCCCount(List<Edge> edges, int nodes) {
        // Simple estimation based on connectivity patterns
        Graph graph = new Graph(nodes, edges, true, null, "unweighted");
        Kosaraju analyzer = new Kosaraju();
        Kosaraju.SCCResult result = analyzer.kosarajuSCC(graph);
        return result.getComponents().size();
    }

    private String getDensityLevel(double density) {
        if (density < 0.2) return "Sparse";
        else if (density < 0.4) return "Medium";
        else return "Dense";
    }

    private void saveGraph(String name, int nodes, List<Edge> edges) {
        try (PrintWriter writer = new PrintWriter("data/" + name + ".json")) {
            writer.println("{");
            writer.println("  \"nodes\": " + nodes + ",");
            writer.println("  \"edges\": [");

            for (int i = 0; i < edges.size(); i++) {
                Edge edge = edges.get(i);
                writer.printf("    {\"u\": %d, \"v\": %d, \"w\": %d}",
                        edge.getU(), edge.getV(), edge.getW());
                if (i < edges.size() - 1) writer.println(",");
                else writer.println();
            }

            writer.println("  ]");
            writer.println("}");
        } catch (FileNotFoundException e) {
            System.err.println("Failed to save graph " + name + ": " + e.getMessage());
        }
    }

    private void saveMetadataReport(List<GraphMetadata> metadata) {
        try (PrintWriter writer = new PrintWriter("data/DATASET_REPORT.md")) {
            writer.println("# Graph Dataset Report");
            writer.println();
            writer.println("## Overview");
            writer.println("Generated " + metadata.size() + " test datasets for graph algorithm testing.");
            writer.println();
            writer.println("## Dataset Details");
            writer.println();
            writer.println("| Name | Nodes | Edges | Cyclic | SCCs | Density | Description |");
            writer.println("|------|-------|-------|--------|------|---------|-------------|");

            for (GraphMetadata meta : metadata) {
                writer.printf("| %s | %d | %d | %s | %d | %s | %s |\n",
                        meta.getName(), meta.getNodes(), meta.getEdges(),
                        meta.isCyclic(), meta.getSccCount(), meta.getDensity(),
                        meta.getDescription());
            }

            writer.println();
            writer.println("## Categories");
            writer.println("- **Small**: 6-10 nodes, simple structures");
            writer.println("- **Medium**: 10-20 nodes, mixed structures");
            writer.println("- **Large**: 20-50 nodes, performance testing");

        } catch (FileNotFoundException e) {
            System.err.println("Failed to save metadata report: " + e.getMessage());
        }
    }

    // Main method to generate all datasets
    public static void main(String[] args) {
        GraphDatasetGenerator generator = new GraphDatasetGenerator();
        generator.generateAllDatasets();
    }
}