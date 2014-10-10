package org.neo4j.constraints;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConstraintParserTest {

    @Test
    public void testParseParts() throws Exception {
        assertTrue("label", "(:Person)".matches(ConstraintParser.LABEL));
        assertTrue("label space", "( : Person )".matches(ConstraintParser.LABEL));
        assertTrue("type", "[:KNOWS]".matches(ConstraintParser.TYPE));
        assertTrue("type space", "[ : KNOWS ]".matches(ConstraintParser.TYPE));
        assertTrue("pattern", "(:Person)-[:KNOWS]-(:Person)".matches(ConstraintParser.PATTERN));
        assertTrue("pattern both", "(:Person)<-[:KNOWS]->(:Person)".matches(ConstraintParser.PATTERN));
        assertTrue("pattern in", "(:Person)<-[:KNOWS]-(:Person)".matches(ConstraintParser.PATTERN));
        assertTrue("pattern out", "(:Person)<-[:KNOWS]->(:Person)".matches(ConstraintParser.PATTERN));
        assertTrue("pattern space", "( : Person ) < - [ : KNOWS ] - > ( : Person )".matches(ConstraintParser.PATTERN));
        assertTrue("card 0", "0".matches(ConstraintParser.COUNT));
        assertTrue("card 1", "1".matches(ConstraintParser.COUNT));
        assertTrue("card 10", "10".matches(ConstraintParser.COUNT));
        assertTrue("card one", "one".matches(ConstraintParser.COUNT));
        assertTrue("card many", "many".matches(ConstraintParser.COUNT));
        assertTrue("card 1..1", "1..1".matches(ConstraintParser.MIN_MAX));
        assertTrue("card 1..many", "1..many".matches(ConstraintParser.MIN_MAX));
        assertTrue("card 1:", "1".matches(ConstraintParser.MIN_MAX));
        assertTrue("card many:", "many".matches(ConstraintParser.MIN_MAX));
        assertTrue("card many:many", "many:many".matches(ConstraintParser.CARD));
        assertTrue("card 1:many", "1:many".matches(ConstraintParser.CARD));
        assertTrue("card 1:1", "1:1".matches(ConstraintParser.CARD));
        assertTrue("card 1..many:1..1", "1..many:1..1".matches(ConstraintParser.CARD));
    }

    @Test
    public void testParse() throws Exception {
        assertConstraint("(:Person)-[:KNOWS]-(:Person) OF -1..-1:-1..-1", "(:Person)-[:KNOWS]-(:Person) TO many:many");
        assertConstraint("(:Company)<-[:WORKS_AT]-(:Person) OF 1..1:-1..-1", "(:Company)<-[:WORKS_AT]-(:Person) TO 1:many");
        assertConstraint("(:Employee)-[:REPORTS_TO]->(:Manager) OF 1..3:1..1", "(:Employee)-[:REPORTS_TO]->(:Manager) TO 1..3:1..1");
    }

    private void assertConstraint(String expected, String pattern) {
        assertEquals(expected, ConstraintParser.parse(pattern).toString());
    }
}
