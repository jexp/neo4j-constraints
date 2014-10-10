package org.neo4j.constraints;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.impl.core.GraphProperties;
import org.neo4j.kernel.impl.core.NodeManager;

/**
 * @author mh
 * @since 10.10.14
 */
public class ConstraintPersister {
    private static final java.lang.String CONSTRAINT = "CONSTRAINT";

    private final GraphProperties properties;

    public ConstraintPersister(GraphProperties properties) {
        this.properties = properties;
    }

    public ConstraintPersister(GraphDatabaseService db) {
        this(((GraphDatabaseAPI)db).getDependencyResolver().resolveDependency(NodeManager.class).getGraphProperties());
    }

    public void persist(Constraint.Constraints constraints) {
        try (Transaction tx = getGraphDatabase().beginTx()) {
            this.properties.setProperty(CONSTRAINT, constraints.toArray());
            tx.success();
        }
    }

    private GraphDatabaseService getGraphDatabase() {
        return properties.getGraphDatabase();
    }

    public Constraint.Constraints restore() {
        try (Transaction tx = getGraphDatabase().beginTx()) {
            Object value = properties.getProperty(CONSTRAINT, null);
            if (!(value instanceof String[])) value=null; // GraphProperty has a bug in getProperty, if the prop-key is not known it returns false
            String[] data = (String[]) value;
            Constraint.Constraints constraints = Constraint.Constraints.from(data);
            tx.success();
            return constraints;
        }
    }
}
