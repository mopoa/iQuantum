package org.iquantum.policies.qtasks;
import org.iquantum.backends.quantum.qubittopologies.QubitTopology;
import org.iquantum.tasks.QTask;
import org.jgrapht.*;
import org.jgrapht.alg.isomorphism.VF2SubgraphIsomorphismInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.List;
import java.util.Comparator;

public class SubgraphIsomorphism {

    public static boolean isSubgraph( QTask Qtask, List<QubitTopology.Node> QNode) {
        //  گراف اول (کوچکتر)
        Graph<String, DefaultEdge> graph1 = new SimpleGraph<>(DefaultEdge.class);
        List<QubitTopology.Node> Qbits =Qtask.getQubitTopology().getQubits();
        for (int i=0; i < Qbits.size();i++){
            graph1.addVertex(Integer.toString(Qbits.get(i).getQubitIndex()));
            for (int j=0; j < Qbits.get(i).getNeighbors().size(); j++){
                graph1.addEdge(Integer.toString(Qbits.get(i).getQubitIndex()), Integer.toString(Qbits.get(i).getNeighbors().get(j).getQubitIndex()));
            }
        }

        //  گراف دوم (بزرگتر)
        Graph<String, DefaultEdge> graph2 = new SimpleGraph<>(DefaultEdge.class);
        for (int i=0; i < QNode.size();i++){
            graph1.addVertex(Integer.toString(QNode.get(i).getQubitIndex()));
            if (!QNode.get(i).getNeighbors().isEmpty()){
                for (int j = 0; j < QNode.get(i).getNeighbors().size(); j++){
                    graph1.addEdge(Integer.toString(QNode.get(i).getQubitIndex()), Integer.toString(QNode.get(i).getNeighbors().get(j).getQubitIndex()));
                }
            }
        }

        Comparator<String> vertexComparator = (v1, v2) -> 0;
        Comparator<DefaultEdge> edgeComparator = (e1, e2) -> 0;

        VF2SubgraphIsomorphismInspector<String, DefaultEdge> inspector =
                new VF2SubgraphIsomorphismInspector<>(
                        graph2,
                        graph1,
                        vertexComparator,
                        edgeComparator
                );

        return inspector.isomorphismExists();
    }
}
