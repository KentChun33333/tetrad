///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015 by Peter Spirtes, Richard Scheines, Joseph   //
// Ramsey, and Clark Glymour.                                                //
//                                                                           //
// This program is free software; you can redistribute it and/or modify      //
// it under the terms of the GNU General Public License as published by      //
// the Free Software Foundation; either version 2 of the License, or         //
// (at your option) any later version.                                       //
//                                                                           //
// This program is distributed in the hope that it will be useful,           //
// but WITHOUT ANY WARRANTY; without even the implied warranty of            //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             //
// GNU General Public License for more details.                              //
//                                                                           //
// You should have received a copy of the GNU General Public License         //
// along with this program; if not, write to the Free Software               //
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA //
///////////////////////////////////////////////////////////////////////////////

package edu.cmu.tetrad.search;

import edu.cmu.tetrad.data.IKnowledge;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.util.ChoiceGenerator;
import edu.cmu.tetrad.util.TetradLogger;

import java.util.*;

/**
 * Implements Meek's complete orientation rule set for PC (Chris Meek (1995), "Causal inference and causal explanation
 * with background knowledge"), modified for Conservative PC to check noncolliders against recorded noncolliders before
 * orienting.
 * <p>
 * For now, the fourth rule is always performed.
 *
 * @author Joseph Ramsey
 */
public class MeekRulesRestricted implements ImpliedOrientation {

    private IKnowledge knowledge;

    /**
     * If knowledge is available.
     */
    boolean useRule4;


    /**
     * The logger to use.
     */
    private Set<Node> visited = new HashSet<>();

    private Queue<Node> rule1Queue = new LinkedList<>();
    private Queue<Node> rule2Queue = new LinkedList<>();
    private Queue<Node> rule3Queue = new LinkedList<>();
    private Queue<Node> rule4Queue = new LinkedList<>();

    // Restricted to these nodes.
    private Set<Node> nodes;

    //    private Set<Node> colliderNodes = null;
    private Map<Edge, Edge> changedEdges = new HashMap<Edge, Edge>();

    private Set<NodePair> impliedColliders = new HashSet<>();
    boolean bettera = false;
    boolean betterb;

    /**
     * Constructs the <code>MeekRules</code> with no logging.
     */
    public MeekRulesRestricted() {
        useRule4 = knowledge != null && !knowledge.isEmpty();
    }

    //======================== Public Methods ========================//

    public void orientImplied(Graph graph) {
        betterb = !bettera;
        this.nodes = new HashSet<>(graph.getNodes());
        visited.addAll(nodes);

        TetradLogger.getInstance().log("impliedOrientations", "Starting Orientation Step D.");
        orientUsingMeekRulesLocally(knowledge, graph);
        TetradLogger.getInstance().log("impliedOrientations", "Finishing Orientation Step D.");

        graph.removeTriplesNotInGraph();
    }

    public void orientImplied(Graph graph, Set<Node> nodes) {
        this.nodes = nodes;
        visited.addAll(nodes);

        TetradLogger.getInstance().log("impliedOrientations", "Starting Orientation Step D.");
        orientUsingMeekRulesLocally(knowledge, graph);
        TetradLogger.getInstance().log("impliedOrientations", "Finishing Orientation Step D.");

        graph.removeTriplesNotInGraph();


    }

    public void setKnowledge(IKnowledge knowledge) {
        this.knowledge = knowledge;
    }

    //============================== Private Methods ===================================//

    private void orientUsingMeekRulesLocally(IKnowledge knowledge, Graph graph) {
        for (Node node : graph.getNodes()) {
            if (!graph.getParents(node).isEmpty()) {
//                meekR1Locally(node, graph, knowledge);

                if (bettera) {
                    meekR1a(node, graph, knowledge);
                    meekR2a(node, graph, knowledge);
                    meekR3a(node, graph, knowledge);

                    if (useRule4) {
                        meekR4a(node, graph, knowledge);
                    }
                }

                if (betterb) {
                    meekR1b(node, graph, knowledge);
                    meekR2b(node, graph, knowledge);
                    meekR3b(node, graph, knowledge);

                    if (useRule4) {
                        meekR4b(node, graph, knowledge);
                    }
                }
            }
        }
    }

//    private List<Node> getColliderNodes(Graph graph) {
//        if (colliderNodes != null) {
//            List<Node> nodes = new ArrayList<Node>();
//
//            for (Node node : colliderNodes) {
//                nodes.add(node);
//            }
//
//            return nodes;
//        }
//
//        List<Node> colliderNodes = new ArrayList<Node>();
//
//        NODES:
//        for (Node y : graph.getNodes()) {
//            List<Node> adj = graph.getAdjacentNodes(y);
//
//            int numInto = 0;
//
//            for (Node x : adj) {
//                if (graph.isDirectedFromTo(x, y)) numInto++;
//                if (numInto == 2) {
//                    colliderNodes.add(y);
//                    continue NODES;
//                }
//            }
//        }
//
//        return colliderNodes;
//    }

    /**
     * Meek's rule R1: if b-->a, a---c, and a not adj to c, then a-->c
     */
    private void meekR1b(Node b, Graph graph, IKnowledge knowledge) {
        List<Node> adjacentNodes = graph.getAdjacentNodes(b);

        if (adjacentNodes.size() < 2) {
            return;
        }

        ChoiceGenerator cg = new ChoiceGenerator(adjacentNodes.size(), 2);
        int[] choice;

        while ((choice = cg.next()) != null) {
            List<Node> nodes = GraphUtils.asList(choice, adjacentNodes);
            Node a = nodes.get(0);
            Node c = nodes.get(1);

            if (!graph.isAdjacentTo(a, c) && graph.isDirectedFromTo(a, b) && graph.isUndirectedFromTo(b, c)) {
                if (!isUnshieldedNoncollider(a, b, c, graph)) {
                    continue;
                }

                if (isArrowpointAllowed(b, c, knowledge)) {
                    Edge after = direct(b, c, graph);
                    Node y = after.getNode2();

                    rule1Queue.add(y);
                    rule3Queue.add(y);
                    rule3Queue.add(y);

                    if (useRule4) {
                        rule4Queue.add(y);
                    }

                    TetradLogger.getInstance().log("impliedOrientations", SearchLogUtils.edgeOrientedMsg(
                            "Meek R1 triangle (" + b + "-->" + a + "---" + c + ")", graph.getEdge(a, c)));
                }
            }

            Node e = c;
            c = a;
            a = e;

            if (!graph.isAdjacentTo(a, c) && graph.isDirectedFromTo(a, b) && graph.isUndirectedFromTo(b, c)) {
                if (!isUnshieldedNoncollider(a, b, c, graph)) {
                    continue;
                }

                if (isArrowpointAllowed(b, c, knowledge)) {
                    Edge after = direct(b, c, graph);
                    Node y = after.getNode2();

                    rule1Queue.add(y);
                    rule3Queue.add(y);
                    rule3Queue.add(y);

                    if (useRule4) {
                        rule4Queue.add(y);
                    }

                    TetradLogger.getInstance().log("impliedOrientations", SearchLogUtils.edgeOrientedMsg(
                            "Meek R1 triangle (" + b + "-->" + a + "---" + c + ")", graph.getEdge(a, c)));
                }
            }
        }
    }

    /**
     * If a-->b-->c, a--c, then b-->c.
     */
    private void meekR2b(Node b, Graph graph, IKnowledge knowledge) {
        List<Node> adjacentNodes = graph.getAdjacentNodes(b);

        if (adjacentNodes.size() < 2) {
            return;
        }

        ChoiceGenerator cg = new ChoiceGenerator(adjacentNodes.size(), 2);
        int[] choice;

        while ((choice = cg.next()) != null) {
            List<Node> nodes = GraphUtils.asList(choice, adjacentNodes);
            Node a = nodes.get(0);
            Node c = nodes.get(1);

            if (graph.isDirectedFromTo(a, b) &&
                    graph.isDirectedFromTo(b, c) &&
                    graph.isUndirectedFromTo(a, c)) {
                if (isArrowpointAllowed(a, c, knowledge)) {
                    Edge after = direct(a, c, graph);
                    Node y = after.getNode2();

                    rule1Queue.add(y);
                    rule3Queue.add(y);
                    rule3Queue.add(y);

                    if (useRule4) {
                        rule4Queue.add(c);
                    }

                    TetradLogger.getInstance().log("impliedOrientations", SearchLogUtils.edgeOrientedMsg("Meek R2", graph.getEdge(b, c)));
                }
            }

            Node e = c;
            c = a;
            a = e;

            if (graph.isDirectedFromTo(a, b) &&
                    graph.isDirectedFromTo(b, c) &&
                    graph.isUndirectedFromTo(a, c)) {
                if (isArrowpointAllowed(a, c, knowledge)) {
                    Edge after = direct(a, c, graph);
                    Node y = after.getNode2();

                    rule1Queue.add(y);
                    rule3Queue.add(y);
                    rule3Queue.add(y);

                    if (useRule4) {
                        rule4Queue.add(c);
                    }

                    TetradLogger.getInstance().log("impliedOrientations", SearchLogUtils.edgeOrientedMsg("Meek R2", graph.getEdge(b, c)));
                }
            }
        }
    }

    /**
     * Meek's rule R3. If a--b, a--c, a--d, c-->b, d-->b, then orient a-->b.
     */
    private void meekR3b(Node c, Graph graph, IKnowledge knowledge) {
        List<Node> adjacentNodes = graph.getAdjacentNodes(c);

        if (adjacentNodes.size() < 3) {
            return;
        }

        for (Node a : adjacentNodes) {
            List<Node> otherAdjacents = new LinkedList<>(adjacentNodes);
            otherAdjacents.remove(c);

            ChoiceGenerator cg = new ChoiceGenerator(otherAdjacents.size(), 2);
            int[] choice;

            while ((choice = cg.next()) != null) {
                List<Node> nodes = GraphUtils.asList(choice, adjacentNodes);
                Node b = nodes.get(0);
                Node d = nodes.get(1);

                if (!(graph.isAdjacentTo(a, b) && graph.isAdjacentTo(a, d) && graph.isAdjacentTo(b, c) && graph.isAdjacentTo(d, c) && graph.isAdjacentTo(a, c))
                        && graph.isDirectedFromTo(b, c) && graph.isDirectedFromTo(d, c) &&
                        graph.isUndirectedFromTo(a, c)) {
                    if (isArrowpointAllowed(a, c, knowledge)) {
                        if (!isUnshieldedNoncollider(d, a, b, graph)) {
                            continue;
                        }

                        direct(a, c, graph);
                        Edge after = direct(a, c, graph);
                        Node y = after.getNode2();

                        rule1Queue.add(y);
                        rule3Queue.add(y);
                        rule3Queue.add(y);

                        if (useRule4) {
                            rule4Queue.add(y);
                        }

                        TetradLogger.getInstance().log("impliedOrientations", SearchLogUtils.edgeOrientedMsg("Meek R3", graph.getEdge(a, b)));
                    }
                }
            }
        }
    }

    private void meekR4b(Node a, Graph graph, IKnowledge knowledge) {
        if (!useRule4) {
            return;
        }

        if (false) return;

        List<Node> adjacentNodes = graph.getAdjacentNodes(a);

        if (adjacentNodes.size() < 3) {
            return;
        }

        for (Node c : adjacentNodes) {
            List<Node> otherAdjacents = new LinkedList<>(adjacentNodes);
            otherAdjacents.remove(c);

            ChoiceGenerator cg = new ChoiceGenerator(otherAdjacents.size(), 2);
            int[] combination;

            while ((combination = cg.next()) != null) {
                Node b = otherAdjacents.get(combination[0]);
                Node d = otherAdjacents.get(combination[1]);

                if (!(graph.isAdjacentTo(a, b) && graph.isAdjacentTo(a, d) && graph.isAdjacentTo(b, c) && graph.isAdjacentTo(d, c) && graph.isAdjacentTo(a, c))) {
                    if (graph.isDirectedFromTo(b, c) && graph.isDirectedFromTo(c, d) && graph.isUndirectedFromTo(a, d)) {
                        if (isArrowpointAllowed(a, c, knowledge)) {
                            if (!isUnshieldedNoncollider(b, a, d, graph)) {
                                continue;
                            }

                            if (isArrowpointAllowed(c, d, knowledge)) {
                                Edge after = direct(c, d, graph);
                                Node x = after.getNode1();
                                Node y = after.getNode2();

                                rule1Queue.add(y);
                                rule2Queue.add(y);
                                rule3Queue.add(y);

                                if (useRule4) {
                                    rule4Queue.add(x);
                                }

                                TetradLogger.getInstance().log("impliedOientations", SearchLogUtils.edgeOrientedMsg("Meek T1", graph.getEdge(a, c)));
                                continue;
                            }
                        }
                    }

                    Node e = d;
                    d = b;
                    b = e;

                    if (graph.isDirectedFromTo(b, c) && graph.isDirectedFromTo(c, d) && graph.isUndirectedFromTo(a, d)) {
                        if (isArrowpointAllowed(a, c, knowledge)) {
                            if (!isUnshieldedNoncollider(b, a, d, graph)) {
                                continue;
                            }

                            if (isArrowpointAllowed(c, d, knowledge)) {
                                Edge after = direct(c, d, graph);
                                Node y = after.getNode2();

                                rule1Queue.add(y);
                                rule2Queue.add(y);
                                rule3Queue.add(y);

                                if (useRule4) {
                                    rule4Queue.add(y);
                                }

                                TetradLogger.getInstance().log("impliedOientations", SearchLogUtils.edgeOrientedMsg("Meek T1", graph.getEdge(a, c)));
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * Meek's rule R1: if b-->a, a---c, and a not adj to c, then a-->c
     */
    private void meekR1a(Node a, Graph graph, IKnowledge knowledge) {
        List<Node> adjacentNodes = graph.getAdjacentNodes(a);

        if (adjacentNodes.size() < 2) {
            return;
        }

        ChoiceGenerator cg = new ChoiceGenerator(adjacentNodes.size(), 2);
        int[] combination;

        while ((combination = cg.next()) != null) {
            Node b = adjacentNodes.get(combination[0]);
            Node c = adjacentNodes.get(combination[1]);

            // Skip triples that are shielded.
            if (graph.isAdjacentTo(b, c)) {
                continue;
            }

            if (graph.isDirectedFromTo(b, a) && graph.isUndirectedFromTo(a, c)) {
                if (!isUnshieldedNoncollider(b, a, c, graph)) {
                    continue;
                }

                if (isArrowpointAllowed(a, c, knowledge) && !createsCycle(a, c, graph)) {
                    Edge before = graph.getEdge(a, c);
                    Edge after = direct(a, c, graph);
                    Node x = after.getNode1();
                    Node y = after.getNode2();

//                    rule2Queue.add(x);
//                    rule3Queue.add(x);

//                    rule1Queue.add(y);
                    rule2Queue.add(y);
                    rule3Queue.add(y);

                    if (useRule4) {
                        rule4Queue.add(x);
                    }

                    changedEdges.put(after, before);

                    TetradLogger.getInstance().log("impliedOrientations", SearchLogUtils.edgeOrientedMsg(
                            "Meek R1 triangle (" + b + "-->" + a + "---" + c + ")", graph.getEdge(a, c)));
                }
            } else if (graph.isDirectedFromTo(c, a) && graph.isUndirectedFromTo(a, b)) {
                if (!isUnshieldedNoncollider(b, a, c, graph)) {
                    continue;
                }

                if (isArrowpointAllowed(a, b, knowledge) && !createsCycle(a, b, graph)) {
                    Edge before = graph.getEdge(a, b);
                    Edge after = direct(a, b, graph);
                    Node x = after.getNode1();
                    Node y = after.getNode2();

//                    rule1Queue.add(y);
                    rule2Queue.add(y);
                    rule3Queue.add(y);

                    if (useRule4) {
                        rule4Queue.add(x);
                    }

                    changedEdges.put(after, before);

                    TetradLogger.getInstance().log("impliedOrientations", SearchLogUtils.edgeOrientedMsg(
                            "Meek R1 (" + c + "-->" + a + "---" + b + ")", graph.getEdge(a, b)));
                }
            }
        }
    }

    private boolean createsCycle(Node a, Node c, Graph graph) {

        return false;
    }

    /**
     * If b-->a-->c, b--c, then b-->c.
     */
    private void meekR2a(Node a, Graph graph, IKnowledge knowledge) {
        List<Node> adjacentNodes = graph.getAdjacentNodes(a);

        if (adjacentNodes.size() < 2) {
            return;
        }

        ChoiceGenerator cg = new ChoiceGenerator(adjacentNodes.size(), 2);
        int[] combination;

        while ((combination = cg.next()) != null) {
            Node b = adjacentNodes.get(combination[0]);
            Node c = adjacentNodes.get(combination[1]);

            if (graph.isDirectedFromTo(b, a) &&
                    graph.isDirectedFromTo(a, c) &&
                    graph.isUndirectedFromTo(b, c)) {
                if (isArrowpointAllowed(b, c, knowledge) && !createsCycle(b, c, graph)) {
                    Edge before = graph.getEdge(b, c);
                    Edge after = direct(b, c, graph);
                    Node x = after.getNode1();
                    Node y = after.getNode2();

                    rule1Queue.add(y);
//                    rule2Queue.add(y);
                    rule3Queue.add(y);

                    if (useRule4) {
                        rule4Queue.add(x);
                    }

                    changedEdges.put(after, before);
                    TetradLogger.getInstance().log("impliedOrientations", SearchLogUtils.edgeOrientedMsg("Meek R2", graph.getEdge(b, c)));
                }
            } else if (graph.isDirectedFromTo(c, a) &&
                    graph.isDirectedFromTo(a, b) &&
                    graph.isUndirectedFromTo(c, b)) {
                if (isArrowpointAllowed(c, b, knowledge) && !createsCycle(c, b, graph)) {
                    Edge before = graph.getEdge(c, b);
                    Edge after = direct(c, b, graph);
                    Node x = after.getNode1();
                    Node y = after.getNode2();

                    rule1Queue.add(y);
//                    rule2Queue.add(y);
                    rule3Queue.add(y);

                    if (useRule4) {
                        rule4Queue.add(x);
                    }

                    changedEdges.put(after, before);
                    TetradLogger.getInstance().log("impliedOrientations", SearchLogUtils.edgeOrientedMsg("Meek R2", graph.getEdge(c, b)));
                }
            }
        }
    }

    /**
     * Meek's rule R3. If a--b, a--c, a--d, c-->b, d-->b, then orient a-->b.
     */
    private void meekR3a(Node c, Graph graph, IKnowledge knowledge) {
        List<Node> adjacentNodes = graph.getAdjacentNodes(c);

        if (adjacentNodes.size() < 3) {
            return;
        }

        for (Node a : adjacentNodes) {
            List<Node> otherAdjacents = new LinkedList<Node>(adjacentNodes);
            otherAdjacents.remove(c);

            ChoiceGenerator cg = new ChoiceGenerator(otherAdjacents.size(), 2);
            int[] combination;

            while ((combination = cg.next()) != null) {
                Node b = otherAdjacents.get(combination[0]);
                Node d = otherAdjacents.get(combination[1]);

                if (!(graph.isAdjacentTo(a, b) && graph.isAdjacentTo(a, d) && graph.isAdjacentTo(b, c) && graph.isAdjacentTo(d, c) && graph.isAdjacentTo(a, c))
                        && graph.isDirectedFromTo(b, c) && graph.isDirectedFromTo(d, c) &&
                        graph.isUndirectedFromTo(a, c)) {
                    if (isArrowpointAllowed(a, c, knowledge)) {
                        if (!isUnshieldedNoncollider(d, a, b, graph)) {
                            continue;
                        }

                        Edge before = graph.getEdge(a, c);
                        Edge after = direct(a, c, graph);

                        Node x = after.getNode1();
                        Node y = after.getNode2();

                        rule1Queue.add(y);
                        rule2Queue.add(y);
//                        rule3Queue.add(x);

                        if (useRule4) {
                            rule4Queue.add(x);
                        }

                        changedEdges.put(after, before);

                        TetradLogger.getInstance().log("impliedOrientations", SearchLogUtils.edgeOrientedMsg("Meek R3", graph.getEdge(a, b)));
                    }
                }
            }
        }
    }

    private void meekR4a(Node a, Graph graph, IKnowledge knowledge) {
        if (!useRule4) {
            return;
        }

        List<Node> adjacentNodes = graph.getAdjacentNodes(a);

        if (adjacentNodes.size() < 3) {
            return;
        }

        for (Node c : adjacentNodes) {
            List<Node> otherAdjacents = new LinkedList<Node>(adjacentNodes);
            otherAdjacents.remove(c);

            ChoiceGenerator cg = new ChoiceGenerator(otherAdjacents.size(), 2);
            int[] combination;

            while ((combination = cg.next()) != null) {
                Node b = otherAdjacents.get(combination[0]);
                Node d = otherAdjacents.get(combination[1]);

                if (!(graph.isAdjacentTo(a, b) && graph.isAdjacentTo(a, d) && graph.isAdjacentTo(b, c) && graph.isAdjacentTo(d, c) && graph.isAdjacentTo(a, c))) {
                    if (graph.isDirectedFromTo(b, c) && graph.isDirectedFromTo(c, d) && graph.isUndirectedFromTo(a, d)) {
                        if (isArrowpointAllowed(a, c, knowledge)) {
                            if (!isUnshieldedNoncollider(b, a, d, graph)) {
                                continue;
                            }

                            Edge before = graph.getEdge(a, d);
                            Edge after = direct(a, d, graph);

                            Node x = after.getNode1();
                            Node y = after.getNode2();

                            rule1Queue.add(y);
                            rule2Queue.add(y);
                            rule3Queue.add(x);

                            if (useRule4) {
                                rule4Queue.add(x);
                            }

                            changedEdges.put(after, before);

                            TetradLogger.getInstance().log("impliedOrientations", SearchLogUtils.edgeOrientedMsg("Meek R3", graph.getEdge(a, b)));
                        }
                    }

                    Node e = d;
                    d = b;
                    b = e;

                    if (graph.isDirectedFromTo(b, c) && graph.isDirectedFromTo(c, d) && graph.isUndirectedFromTo(a, d)) {
                        if (isArrowpointAllowed(a, c, knowledge)) {
                            if (!isUnshieldedNoncollider(b, a, d, graph)) {
                                continue;
                            }

                            Edge before = graph.getEdge(a, d);
                            Edge after = direct(a, d, graph);

                            Node x = after.getNode1();
                            Node y = after.getNode2();

                            rule1Queue.add(y);
                            rule2Queue.add(y);
                            rule3Queue.add(x);

                            if (useRule4) {
                                rule4Queue.add(x);
                            }

                            changedEdges.put(after, before);

                            TetradLogger.getInstance().log("impliedOrientations", SearchLogUtils.edgeOrientedMsg("Meek R3", graph.getEdge(a, b)));
                        }
                    }
                }
            }
        }
    }

    private Edge direct(Node a, Node c, Graph graph) {
        Edge before = graph.getEdge(a, c);
        Edge after = Edges.directedEdge(a, c);

        visited.add(a);
        visited.add(c);

        graph.removeEdge(before);
        graph.addEdge(after);

        List<Node> parents = graph.getParents(c);

        if (parents.size() > 1 && !before.pointsTowards(c)) {
            impliedColliders.add(new NodePair(a, c));
        }

        return after;
    }

    private static boolean isUnshieldedNoncollider(Node a, Node b, Node c, Graph graph) {
        if (!graph.isAdjacentTo(a, b)) {
            return false;
        }

        if (!graph.isAdjacentTo(c, b)) {
            return false;
        }

        if (graph.isAdjacentTo(a, c)) {
            return false;
        }

        if (graph.isAmbiguousTriple(a, b, c)) {
            return false;
        }

        return !(graph.getEndpoint(a, b) == Endpoint.ARROW && graph.getEndpoint(c, b) == Endpoint.ARROW);

    }


    private static boolean isArrowpointAllowed(Node from, Node to,
                                               IKnowledge knowledge) {
        return knowledge == null || (!knowledge.isRequired(to.toString(), from.toString()) &&
                !knowledge.isForbidden(from.toString(), to.toString()));
    }

    public Set<Node> getVisited() {
        return visited;
    }

    public Set<Node> getNodes() {
        return nodes;
    }

    public Set<NodePair> getImpliedColliders() {
        return impliedColliders;
    }
}


