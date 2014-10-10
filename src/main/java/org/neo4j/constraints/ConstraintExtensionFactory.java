package org.neo4j.constraints;

import org.neo4j.constraints.Constraint.Result;
import org.neo4j.graphdb.ConstraintViolationException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.event.LabelEntry;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.kernel.lifecycle.Lifecycle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author mh
 * @since 10.10.14
 */
public class ConstraintExtensionFactory extends KernelExtensionFactory<ConstraintExtensionFactory.Dependencies> {
    public ConstraintExtensionFactory() {
        super("constraint");
    }

    public interface Dependencies {
        GraphDatabaseService getGraphDatabase();
    }

    @Override
    public Lifecycle newKernelExtension(Dependencies deps) throws Throwable {
        return new ConstraintExtension(deps.getGraphDatabase());
    }

    private static class ConstraintExtension implements Lifecycle, TransactionEventHandler<Object> {
        private final GraphDatabaseService graphDatabase;
        private final ConstraintPersister persister;
        private Constraint.Constraints constraints;

        public ConstraintExtension(GraphDatabaseService graphDatabase) {
            this.graphDatabase = graphDatabase;
            this.persister = new ConstraintPersister(graphDatabase);
        }

        @Override
        public void init() throws Throwable {
        }

        @Override
        public void start() throws Throwable {
            graphDatabase.registerTransactionEventHandler(this);
        }

        @Override
        public void stop() throws Throwable {
            graphDatabase.unregisterTransactionEventHandler(this);
        }

        @Override
        public void shutdown() throws Throwable { }

        @Override
        public Object beforeCommit(TransactionData transactionData) throws Exception {
            if (isGraphPropertyChange(transactionData)) return null;
            this.constraints = persister.restore();
            if (constraints.isEmpty()) return null;

            Set<Node> nodesToCheck = collectNodesToCheck(transactionData);

            Map<Node, Map<Constraint, Result>> errors = checkConstraints(nodesToCheck);
            if (!errors.isEmpty()) {
                throw new ConstraintViolationException("Nodes violated cardinality constraints:\n"+errors);
            }
            return null;
        }

        private boolean isGraphPropertyChange(TransactionData transactionData) {
            return
               (isEmpty(transactionData.deletedNodes()) &&
                isEmpty(transactionData.deletedNodes()) &&
                isEmpty(transactionData.assignedLabels()) &&
                isEmpty(transactionData.removedLabels()) &&
                isEmpty(transactionData.deletedRelationships()) &&
                isEmpty(transactionData.createdRelationships()));
        }

        private boolean isEmpty(Iterable<?> iterable) {
            return !iterable.iterator().hasNext();
        }

        private Map<Node, Map<Constraint, Result>> checkConstraints(Set<Node> nodesToCheck) {
            Map<Node,Map<Constraint,Result>> errors = new HashMap<>();
            for (Node node : nodesToCheck) {
                Map<Constraint, Result> result = constraints.check(node);
                if (result == null) continue;
                errors.put(node,result);
            }
            return errors;
        }

        private Set<Node> collectNodesToCheck(TransactionData transactionData) {
            Set<Node> nodesToCheck = new HashSet<>();
            for (Node node : transactionData.createdNodes()) if (constraints.matches(node)) nodesToCheck.add(node);
            for (Node node : transactionData.deletedNodes()) if (constraints.matches(node)) nodesToCheck.add(node);
            for (LabelEntry entry : transactionData.assignedLabels()) if (constraints.matches(entry.label()) && constraints.matches(entry.node())) nodesToCheck.add(entry.node());
            for (LabelEntry entry : transactionData.removedLabels()) if (constraints.matches(entry.label()) && constraints.matches(entry.node())) nodesToCheck.add(entry.node());
            for (Relationship rel : transactionData.createdRelationships()) {
                if (constraints.matches(rel.getStartNode())) nodesToCheck.add(rel.getStartNode());
                if (constraints.matches(rel.getEndNode())) nodesToCheck.add(rel.getEndNode());
            }
            for (Relationship rel : transactionData.deletedRelationships()) {
                if (constraints.matches(rel.getStartNode())) nodesToCheck.add(rel.getStartNode());
                if (constraints.matches(rel.getEndNode())) nodesToCheck.add(rel.getEndNode());
            }
            return nodesToCheck;
        }

        @Override
        public void afterCommit(TransactionData transactionData, Object o) {

        }

        @Override
        public void afterRollback(TransactionData transactionData, Object o) {

        }
    }
}
