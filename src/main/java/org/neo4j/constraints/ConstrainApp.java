package org.neo4j.constraints;

import org.neo4j.kernel.impl.core.GraphProperties;
import org.neo4j.kernel.impl.core.NodeManager;
import org.neo4j.shell.*;
import org.neo4j.shell.kernel.apps.TransactionProvidingApp;

/**
 * @author mh
 * @since 10.10.14
 */
public class ConstrainApp extends TransactionProvidingApp {


    @Override
    protected Continuation exec(AppCommandParser appCommandParser, Session session, Output output) throws Exception {
        String line = appCommandParser.getLineWithoutApp();
        Constraint constraint = ConstraintParser.parse(line);
        storeConstraint(constraint);
        return Continuation.INPUT_COMPLETE;
    }

    private void storeConstraint(Constraint constraint) {
        GraphProperties props = getServer().getDb().getDependencyResolver().resolveDependency(NodeManager.class).getGraphProperties();
        ConstraintPersister constraintPersister = new ConstraintPersister(props);
        Constraint.Constraints constraints = constraintPersister.restore();
        constraints.add(constraint);
        constraintPersister.persist(constraints);
    }

    @Override
    public String getName() {
        return "constrain";
    }
}
