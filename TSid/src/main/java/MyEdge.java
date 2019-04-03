import org.jgrapht.graph.DefaultEdge;

class MyEdge {
    private DefaultEdge edge;
    private double a;
    private double c;
    private double weight;

    MyEdge(DefaultEdge edge, double a, double c, double weight){
        this.edge = edge;
        this.a = a;
        this.c = c;
        this.weight = weight;
    }

    boolean isOverflowed(int packageSize) {
        return a*packageSize > c;
    }

    DefaultEdge getEdge() {
        return edge;
    }

    double getA() {
        return a;
    }

    double getC() {
        return c;
    }

    void addA(int val) {
        a += val;
    }

    double getWeight() {
        return weight;
    }

}
