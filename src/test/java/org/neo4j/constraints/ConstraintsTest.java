package org.neo4j.constraints;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.test.TestGraphDatabaseFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConstraintsTest {

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

    enum Labels implements Label { Person, Company, Product, Manager };
    enum Types implements RelationshipType { WORKS_FOR, KNOWS, REPORTS_TO };

    @Before
    public void setUp() throws Exception {
        db = new TestGraphDatabaseFactory().newImpermanentDatabase();
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
        tx = db.beginTx();
        worksForConstraint = ConstraintParser.parse("(:Person)-[:WORKS_FOR]->(:Company) TO many:1");
        reportsToConstraint = ConstraintParser.parse("(:Person)-[:REPORTS_TO]->(:Manager) to 1..3:0..1");
    }

    @After
    public void tearDown() throws Exception {
        if (tx != null) {
            tx.failure();tx.close();
        }
        db.shutdown();
    }

    @Test
    public void testMatchesWorksFor() throws Exception {
        assertTrue("person1", worksForConstraint.matches(person1));
        assertTrue("manager", worksForConstraint.matches(manager));
        assertTrue("company", worksForConstraint.matches(company));
        assertFalse("product", worksForConstraint.matches(product));
    }

    @Test
    public void testMatchesReportsTo() throws Exception {
        assertTrue("person1", reportsToConstraint.matches(person1));
        assertTrue("manager", reportsToConstraint.matches(manager));
        assertFalse("company", reportsToConstraint.matches(company));
        assertFalse("product", reportsToConstraint.matches(product));
    }

    @Test
    public void testCheckWorksFor() throws Exception {
        assertEquals("check person1", Constraint.Result.OK, worksForConstraint.check(person1));
        assertEquals("check company", Constraint.Result.OK, worksForConstraint.check(company));
        assertEquals("check manager", Constraint.Result.OK, worksForConstraint.check(manager));
        assertEquals("check person2", Constraint.Result.TO_MANY, worksForConstraint.check(person2));
    }

    @Test
    public void testCheckReportsTo() throws Exception {
        assertEquals("check person1", Constraint.Result.OK, reportsToConstraint.check(person1));
        assertEquals("check person2", Constraint.Result.OK, reportsToConstraint.check(person2));
        assertEquals("check manager", Constraint.Result.OK, reportsToConstraint.check(manager));
        assertEquals("check company", Constraint.Result.OK, reportsToConstraint.check(company));
    }
}
