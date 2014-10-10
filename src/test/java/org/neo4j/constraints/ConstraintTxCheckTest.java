package org.neo4j.constraints;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.test.TestGraphDatabaseFactory;

public class ConstraintTxCheckTest {

    private Node person1;
    private Node company;
    private GraphDatabaseService db;
    private Relationship worksFor1;
    private Relationship worksFor2;
    private Relationship worksForM;
    private Transaction tx;
    private Constraint worksForConstraint;
    private Node product;
    private Node manager;
    private Node person2;
    private Constraint reportsToConstraint;
    private Node company2;
    private Relationship worksFor2_2;
    private ConstraintPersister persister;
    private Relationship reportsTo1;

    enum Labels implements Label { Person, Company, Product, Manager };
    enum Types implements RelationshipType { WORKS_FOR, KNOWS, REPORTS_TO };

    @Before
    public void setUp() throws Exception {
        db = new TestGraphDatabaseFactory().newImpermanentDatabase();
        installConstraints();
    }

    private void installConstraints() {
        persister = new ConstraintPersister(db);
        worksForConstraint = ConstraintParser.parse("(:Person)-[:WORKS_FOR]->(:Company) TO many:1");
        reportsToConstraint = ConstraintParser.parse("(:Person)-[:REPORTS_TO]->(:Manager) to 1..3:0..1");
        Constraint.Constraints constraints = Constraint.Constraints.from(null);
        constraints.add(worksForConstraint);
        constraints.add(reportsToConstraint);
        persister.persist(constraints);
    }

    @After
    public void tearDown() throws Exception {
        db.shutdown();
    }

    /*
            try (Transaction tx = db.beginTx()) {
            company = db.createNode(Labels.Company);
            company2 = db.createNode(Labels.Company);
            person1 = db.createNode(Labels.Person);
            worksFor1 = person1.createRelationshipTo(company, Types.WORKS_FOR);
            person2 = db.createNode(Labels.Person);
            worksFor2 = person2.createRelationshipTo(company, Types.WORKS_FOR);
            worksFor2_2 = person2.createRelationshipTo(company2, Types.WORKS_FOR);
            manager = db.createNode(Labels.Person, Labels.Manager);
            worksForM = manager.createRelationshipTo(company, Types.WORKS_FOR);

            person1.createRelationshipTo(manager, Types.REPORTS_TO);
            person2.createRelationshipTo(manager, Types.REPORTS_TO);

            product = db.createNode(Labels.Product);
            tx.success();
        }

     */
    @Test(expected = TransactionFailureException.class)
    public void testCreateLonePerson() throws Exception {
        try (Transaction tx = db.beginTx()) {
            db.createNode(Labels.Person);
            tx.success();
        }
    }

    @Test
    public void testCreateLoneCompany() throws Exception {
        try (Transaction tx = db.beginTx()) {
            db.createNode(Labels.Company);
            tx.success();
        }
    }

    @Test(expected = TransactionFailureException.class)
    public void testCreateLoneManager() throws Exception {
        try (Transaction tx = db.beginTx()) {
            db.createNode(Labels.Manager);
            tx.success();
        }
    }

    @Test(expected = TransactionFailureException.class)
    public void testAddManagerLabel() throws Exception {
        try (Transaction tx = db.beginTx()) {
            company = db.createNode(Labels.Company);
            person1 = db.createNode(Labels.Person);
            person1.createRelationshipTo(company, Types.WORKS_FOR);
            person1.addLabel(Labels.Manager);
            tx.success();
        }
    }

    @Test
    public void testCreatePerson() throws Exception {
        try (Transaction tx = db.beginTx()) {
            company = db.createNode(Labels.Company);
            person1 = db.createNode(Labels.Person);
            person1.createRelationshipTo(company, Types.WORKS_FOR);
            tx.success();
        }

    }
    @Test
    public void testCreatePersonWithManager() throws Exception {
        try (Transaction tx = db.beginTx()) {
            company = db.createNode(Labels.Company);
            person1 = db.createNode(Labels.Person);
            person1.createRelationshipTo(company, Types.WORKS_FOR);
            manager = db.createNode(Labels.Person,Labels.Manager);
            manager.createRelationshipTo(company, Types.WORKS_FOR);
            person1.createRelationshipTo(manager,Types.REPORTS_TO);
            tx.success();
        }
    }

    @Ignore("should fail but have to figure out semantics of removing label, also base constraint on relationship-type ??")
    @Test(expected = TransactionFailureException.class)
    public void testCreatePersonWithManagerAndRemoveLabel() throws Exception {
        testCreatePersonWithManager();
        try (Transaction tx = db.beginTx()) {
            manager.removeLabel(Labels.Manager);
            tx.success();
        }
    }

    @Test(expected = TransactionFailureException.class)
    public void testCreatePersonWithManagerAndRemoveRelationship() throws Exception {
        testCreatePersonWithManager();
        try (Transaction tx = db.beginTx()) {
            for (Relationship rel : manager.getRelationships(Types.REPORTS_TO)) {
                rel.delete();
            }
            tx.success();
        }
    }

    @Test
    public void testCreateCompany() throws Exception {
        try (Transaction tx = db.beginTx()) {
            company = db.createNode(Labels.Company);
            tx.success();
        }
    }
}
