import org.javatuples.Pair;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.ext.*;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;

import static java.lang.System.exit;

public class Test {
    private static Random generator = new Random(System.currentTimeMillis());
    private ArrayList<Pair<Integer, Integer>> edgesEnds = new ArrayList<>();
    private double maxDelay = 0;

    public static void main(String [] args) {
        Test t = new Test();
        if (args.length != 0)
            try {
                t.simulation(args);
            } catch (IOException e) {
                System.out.println("Incorrect file name");
            } catch (NumberFormatException e) {
                System.out.println("Wrong parameter in file");
            }
        else
            t.launch();
    }

    private void launch() {
        ArrayList<MyEdge> myWeightedEdges = new ArrayList<>();
        SimpleGraph<Integer, DefaultEdge> g = new SimpleGraph<>(DefaultEdge.class);

        for (int i = 0; i < 20; i++)
            g.addVertex(i);

        for (int j = 0; j < 19; j++) {
            DefaultEdge t = g.addEdge(j, j + 1);
            myWeightedEdges.add(new MyEdge(t, 1, 1, 0.95));
        }

        System.out.println("Wynik bez cyklu= "+ monteCarlo(myWeightedEdges, g));
        exportGraph(g, "acykliczny");

        DefaultEdge t = g.addEdge(19, 0);
        myWeightedEdges.add(new MyEdge(t,1, 1, 0.95));
        System.out.println("Wynik z cyklem= "+ monteCarlo(myWeightedEdges, g));
        exportGraph(g, "jednocyklowy");

        myWeightedEdges.add(new MyEdge
                (g.addEdge(1, 10), 1, 1, 0.8));
        myWeightedEdges.add(new MyEdge
                (g.addEdge(5, 15), 1, 1, 0.7));
        System.out.println("Wyniki z kilkoma cyklami= " + monteCarlo(myWeightedEdges, g));
        exportGraph(g, "wielocykliczny");

        for (int i = 0; i < 4; i++) {
            int a = generator.nextInt(20);
            int b = generator.nextInt(20);
            if (!g.containsEdge(a, b) && a != b)
                myWeightedEdges.add(new MyEdge(g.addEdge(a, b), 1, 1, 0.4));
            else
                i--;
        }
        System.out.println("Wyniki kolejne = " + monteCarlo(myWeightedEdges, g));
        exportGraph(g, "losowekrawedzie");
    }

    private double monteCarlo(ArrayList<MyEdge> myWeightedEdges,
                              SimpleGraph<Integer, DefaultEdge> g) {
        int good = 0;
        final int repetitions = 10000;
        SimpleGraph<Integer, DefaultEdge> tempGraph =
                (SimpleGraph<Integer, DefaultEdge>) g.clone();
        ArrayList<MyEdge> toRemove = new ArrayList<>();

        for (int i = 0; i < repetitions; i++) {
            for (MyEdge e: myWeightedEdges)
                if (e.getWeight() < generator.nextDouble())
                    toRemove.add(e);

            if (!toRemove.isEmpty())
                for (MyEdge e: toRemove)
                    tempGraph.removeEdge(e.getEdge());

            ConnectivityInspector con = new ConnectivityInspector<>(tempGraph);
            if (con.isGraphConnected())
                good++;

            tempGraph = (SimpleGraph<Integer, DefaultEdge>) g.clone();
            toRemove.clear();
        }
        return ((double) good)/repetitions;
    }

    private void simulation(String [] args) throws IOException {
        String filename = "";
        int attempts = 0, size = 0, edgesNum = 0, lineCounter = 0, packageSize = 0;
        double rel = 0, tmax = 0;
        int [][] intensityMatrix = new int[0][];
        int [][] capacityMatrix = new int[0][];

        try {
            filename = args[0];
            attempts = Integer.parseInt(args[1]);
            rel = Double.parseDouble(args[2]);
            tmax = Double.parseDouble(args[3]);
        } catch (Exception e) {
            showInstruction();
            exit(0);
        }
        File file = new File("E:\\git\\TSid\\src\\main\\resources\\"+filename);
        BufferedReader br = new BufferedReader(new FileReader(file.getAbsolutePath()));
        String line;
        if ((line = br.readLine()) != null) {
            size = Integer.parseInt(line);
            intensityMatrix = new int[size][size];
            capacityMatrix = new int[size][size];
        }
        if ((line = br.readLine()) != null)
            packageSize = Integer.parseInt(line);
        //odczytywanie natezenia:
        for (int i = 0; i < size; i++) {
            if ((line = br.readLine()) != null) {
                String [] splited = line.split(" ;");
                for (int j = 0; j < size; j++) {
                    intensityMatrix[i][j] = Integer.parseInt(splited[j]);
                }
            }
        }
        //odczytywanie pojemnosci maksymalnej:
        for (int i = 0; i < size; i++) {
            if ((line = br.readLine()) != null) {
                String [] splited = line.split(" ;");
                for (int j = 0; j < size; j++) {
                    capacityMatrix[i][j] = Integer.parseInt(splited[j]);
                }
            }
        }

        //odczytywanie krawedzi:
        while ((line = br.readLine()) != null) {
            String [] edgeLine = line.split(";");
            int a = Integer.parseInt(edgeLine[0]);
            int b = Integer.parseInt(edgeLine[1]);
            edgesEnds.add(new Pair<>(a, b));
        }
        double failures = simulationGo(attempts, intensityMatrix, capacityMatrix, size, tmax, packageSize, rel);
        System.out.println("Failures: " + failures);
        System.out.println("Reliability:  " + (attempts - failures) / attempts);
    }

    private void init(final int [][] capacity,
                      SimpleGraph<Integer, DefaultEdge> graph,
                      ArrayList<MyEdge> edges,
                      final double rel,
                      final int size) {
        for (int i = 0; i < size; i++)
            graph.addVertex(i);

        for (Pair<Integer, Integer> p: edgesEnds) {
            int a = p.getValue0();
            int b = p.getValue1();
            edges.add(new MyEdge(graph.addEdge(a, b), 0, capacity[a][b], rel));
        }
    }

    private double simulationGo(final int repetitions,
                      final int [][] intensity,
                      final int [][] capacity,
                      final int size,
                      final double tmax,
                      final int packageSize,
                      final double rel) {
        double failures = 0;
        SimpleGraph<Integer, DefaultEdge> testGraph;
        ArrayList<MyEdge> testEdges = new ArrayList<>();
        ArrayList<MyEdge> toRemove = new ArrayList<>();

        for (int iterator = 0; iterator < repetitions; iterator++) {
            toRemove.clear();
            testEdges.clear();
            testGraph = new SimpleGraph<>(DefaultEdge.class);
            init(capacity, testGraph, testEdges, rel, size);
            if (iterator == 0) {
                exportGraph(testGraph, "propozycja1");
            }
            //usuwanie ewentualnych krawedzi:
            for (MyEdge e: testEdges)
                if (e.getWeight() < generator.nextDouble())
                    toRemove.add(e);

            for (MyEdge e: toRemove) {
                testGraph.removeEdge(e.getEdge());
                testEdges.remove(e);
            }

            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    GraphPath<Integer, DefaultEdge> path =
                            DijkstraShortestPath.findPathBetween(testGraph, i, j);
                    if (path != null)
                        for (DefaultEdge fromPath: path.getEdgeList())
                            for (MyEdge edge: testEdges)
                                if (edge.getEdge().equals(fromPath)) {
                                    edge.addA(intensity[i][j]);
                                    break;
                                }
                }
            }

            ConnectivityInspector con = new ConnectivityInspector(testGraph);
            if (con.isGraphConnected()) {
                if (isOverflowed(testEdges, packageSize))
                    failures++;
                else
                    if (isDelayCorrect(testEdges, intensity, size, packageSize, tmax))
                        failures++;
            } else {
                failures++;
            }
        }
        return failures;
    }

    private boolean isOverflowed(final ArrayList<MyEdge> edges, final int packageSize) {
        for (MyEdge e: edges) {
            if (e.isOverflowed(packageSize))
                return true;
        }
        return false;
    }

    private boolean isDelayCorrect(final ArrayList<MyEdge> edges,
                      final int [][] intensityM,
                      final int mSize,
                      final int packageSize,
                      final double tmax) {
            double G = 0;
            double SUM_e = 0;
            for (int i = 0; i < mSize; i++) {
                for (int j = 0; j < mSize; j++) {
                    G += intensityM[i][j];
                }
            }
            for (MyEdge e: edges) {
                SUM_e += (e.getA()/((e.getC()/packageSize) - e.getA()));
            }
            return (1/G)*SUM_e > tmax;
        }

    private void showInstruction() {
        System.out.println("Some of parameters are incorrect." +
                "\n Example of program call:" +
                "\n\t<program> filename attempts rel tmax" +
                "\nfilename - name of file with graph topology and intensity matrix" +
                "\nattempts - number of attemptions in simulation" +
                "\nrel - reliability of graph edge" +
                "\ntmax - max delay");
    }

    private void exportGraph(SimpleGraph<Integer, DefaultEdge> g, String filename) {
        File file = new File("src/main/resources/" + filename + ".txt");
        try {
            GraphExporter<Integer, DefaultEdge> exporter =
                    new CSVExporter<>(CSVFormat.EDGE_LIST, '\t');
            PrintWriter writer = new PrintWriter(file);
            exporter.exportGraph(g, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
