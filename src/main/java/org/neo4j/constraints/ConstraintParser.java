package org.neo4j.constraints;

import org.neo4j.graphdb.Direction;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author mh
 * @since 10.10.14
 */
public class ConstraintParser {
    public static final String LABEL = "\\(\\s*:(\\s*[^\\)]+?)\\s*\\)";
    public static final String TYPE = "\\[\\s*:\\s*([^\\]]+?)\\s*\\]";
    public static final String PATTERN = LABEL+"\\s*(<)?\\s*-\\s*"+TYPE+"\\s*-\\s*(>)?\\s*"+LABEL;
    public static final String COUNT = "(one|many|\\d+)";
    public static final String MIN_MAX = COUNT + "\\s*(?:\\.\\." + COUNT + ")?";
    public static final String CARD = MIN_MAX + "\\s*:\\s*" + MIN_MAX;
    public final static Pattern pattern = Pattern.compile(PATTERN + "\\s+(?i:TO)\\s+" + CARD);

    public static Constraint parse(String str) {
        Matcher matcher = pattern.matcher(str);
        if (matcher.matches()) {
            String labelFrom = matcher.group(1);
            String arrowLeft = matcher.group(2);
            String type = matcher.group(3);
            String arrowRight = matcher.group(4);
            String labelTo = matcher.group(5);
            return Constraint.from(new String[]{labelFrom, labelTo, type, toDirection(arrowLeft, arrowRight),
                    matcher.group(6), matcher.group(7), matcher.group(8), matcher.group(9)}, 0);
        }
        throw new RuntimeException("Could not parse cardinality pattern "+str);
    }

    private static String toDirection(String arrowLeft, String arrowRight) {
        boolean isLeft = "<".equals(arrowLeft);
        boolean isRight = ">".equals(arrowRight);
        if (isLeft && isRight || !isLeft && !isRight) return Direction.BOTH.name();
        if (isLeft) return Direction.INCOMING.name();
        return Direction.OUTGOING.name();
    }
}
