package org.neo4j.constraints;

import org.neo4j.graphdb.*;
import org.neo4j.helpers.Strings;

import java.util.*;

/**
 * @author mh
 * @since 10.10.14
 */
public class Constraint {
    public static final int MANY_VALUE = -1;
    public static final int ONE_VALUE = 1;
    public static final int SIZE = 8;
    public static final String NO_TYPE = "";
    private final String from;
    private final String to;
    private final String typeName;
    private final Direction direction;
    private int minFrom, maxFrom, minTo, maxTo;
    private final DynamicRelationshipType type;
    private final Label toLabel, fromLabel;

    public Constraint(String from, String to, String typeName, Direction direction, int minFrom, int maxFrom, int minTo, int maxTo) {
        this.from = Strings.isBlank(from) ? NO_TYPE : from;
        this.fromLabel = Strings.isBlank(from) ? null : DynamicLabel.label(from);
        this.to = Strings.isBlank(to) ? NO_TYPE : to;
        this.toLabel = Strings.isBlank(to) ? null : DynamicLabel.label(to);
        this.typeName = typeName == null ? NO_TYPE : typeName;
        this.type = typeName == null ? null : DynamicRelationshipType.withName(typeName);
        this.direction = direction == null ? Direction.BOTH : direction;
        this.minFrom = minFrom;
        this.maxFrom = maxFrom;
        this.minTo = minTo;
        this.maxTo = maxTo;
    }

    public static int parseValue(String str, int defaultValue) {
        if (str == null) return defaultValue;
        if ("one".equalsIgnoreCase(str)) return ONE_VALUE;
        if ("many".equalsIgnoreCase(str)) return MANY_VALUE;
        return Integer.parseInt(str);
    }

    public enum Result {
        OK, TO_FEW, TO_MANY;

        public static Result check(int degree, int minFrom, int maxFrom) {
            if (minFrom != MANY_VALUE && minFrom > degree) return TO_FEW;
            if (maxFrom != MANY_VALUE && maxFrom < degree) return TO_MANY;
            return OK;
        }
    }

    public Result check(Node node) {
        if (fromLabel == null || node.hasLabel(fromLabel)) {
            int degree = toLabel == null ? getDegree(node, direction) : getDegreeWithOtherLabel(node, direction, toLabel);
            Result result = Result.check(degree, minTo, maxTo);
            if (result != Result.OK) return result;
        }
        if (toLabel == null || node.hasLabel(toLabel)) {
            int degree = fromLabel == null ? getDegree(node, direction.reverse()) : getDegreeWithOtherLabel(node, direction.reverse(), fromLabel);
            return Result.check(degree, minFrom, maxFrom);
        }
        return Result.OK;
    }

    private int getDegree(Node node, Direction direction) {
        return type == null ? node.getDegree(direction) : node.getDegree(type, direction);
    }

    private int getDegreeWithOtherLabel(Node node, Direction direction, Label toLabel) {
        int degree = 0;
        Iterable<Relationship> relationships = type == null ? node.getRelationships(direction) : node.getRelationships(type, direction);
        for (Relationship relationship : relationships) {
            if (relationship.getOtherNode(node).hasLabel(toLabel)) degree++;
        }
        return degree;
    }

    public int intoArray(String[] data, int offset) {
        data[offset] = from;
        data[offset+1] = to;
        data[offset+2] = typeName;
        data[offset+3] = direction.name();
        data[offset+4] = String.valueOf(minFrom);
        data[offset+5] = String.valueOf(maxFrom);
        data[offset+6] = String.valueOf(minTo);
        data[offset+7] = String.valueOf(maxTo);
        return offset+SIZE;
    }

    public static Constraint from(String[] data,int offset) {
        if (data.length - offset < SIZE) throw new IllegalArgumentException("Invalid data size, need 7 elements");
        int minFrom = parseValue(data[offset + 4], MANY_VALUE);
        int minTo = parseValue(data[offset + 6], MANY_VALUE);
        return new Constraint(
                data[offset], data[offset+1], data[offset+2], Direction.valueOf(data[offset+3]),
                minFrom, parseValue(data[offset+5],minFrom),
                minTo, parseValue(data[offset+7],minTo));
    }

    @Override
    public String toString() {
        String left = direction == Direction.INCOMING ? "<" : "";
        String right = direction == Direction.OUTGOING ? ">" : "";
        return "(:"+from+")"+left+"-[:"+ typeName +"]-"+right+"(:"+to+") OF "+minFrom+".."+maxFrom+":"+minTo+".."+maxTo;
    }

    static class Constraints {
        private final Set<String> labels;
        private final Set<String> types;
        private List<Constraint> constraints = new ArrayList<>();

        public Constraints(List<Constraint> constraints) {
            this.constraints = new ArrayList<>(constraints);
            this.labels = new HashSet<String>();
            for (Constraint constraint : constraints) {
                labels.add(constraint.from);
                labels.add(constraint.to);
            }
            this.types = new HashSet<String>();
            for (Constraint constraint : constraints) {
                types.add(constraint.typeName);
            }
        }

        public boolean matches(Node node) {
            Iterator<Label> labels = node.getLabels().iterator();
            if (!labels.hasNext()) return this.labels.contains(NO_TYPE);
            while (labels.hasNext()) {
                Label next = labels.next();
                if (this.labels.contains(next.name())) return true;
            }
            return false;
        }

        public boolean matches(Label label) {
            if (label == null) return this.labels.contains(NO_TYPE);
            return this.labels.contains(label.name());
        }

        public boolean matches(Relationship relationship) {
            return this.types.contains(relationship.getType().name());
        }

        public String[] toArray() {
            int size = constraints.size();
            String[] result = new String[size * SIZE];
            int offset = 0;
            for (Constraint constraint : constraints) {
                offset = constraint.intoArray(result, offset);
            }
            return result;
        }

        public static Constraints from(String[] data) {
            if (data == null || data.length == 0) return new Constraints(Collections.<Constraint>emptyList());
            if (data.length % SIZE != 0) throw new IllegalArgumentException("Invalid data size, need multiple of 7 elements");
            List<Constraint> result = new ArrayList<>(data.length / SIZE);
            for (int offset=0;offset < data.length;offset+=SIZE) {
                result.add(Constraint.from(data, offset));
            }
            return new Constraints(result);
        }

        public boolean isEmpty() {
            return constraints.isEmpty();
        }

        public Map<Constraint, Result> check(Node node) {
            Map<Constraint,Result> errors = null;
            for (Constraint constraint : constraints) {
                if (!constraint.matches(node)) continue;
                Result result = constraint.check(node);
                if (result == Result.OK) continue;
                if (errors==null) errors = new HashMap<>();
                errors.put(constraint,result);
            }
            return errors;
        }

        public void add(Constraint constraint) {
            this.constraints.add(constraint);
        }
    }

    public boolean matches(Node node) {
        return fromLabel == null || toLabel == null || node.hasLabel(fromLabel) || node.hasLabel(toLabel);
    }
}
