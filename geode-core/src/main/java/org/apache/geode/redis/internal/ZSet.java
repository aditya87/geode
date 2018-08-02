package org.apache.geode.redis.internal;

import org.apache.geode.DataSerializable;
import org.apache.geode.DataSerializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ZSet implements DataSerializable {
    private Map<String, Double> memberMap;
    private List<String> sortedMemberList;

    public ZSet() {
        this.memberMap = new HashMap<>();
        this.sortedMemberList = new ArrayList<>();
    }

    @Override
    public void toData(DataOutput out) throws IOException {
        DataSerializer.writeInteger(getSize(), out);
        for (String member : sortedMemberList) {
            DataSerializer.writeString(member, out);
            DataSerializer.writeDouble(getScore(member), out);
        }
    }

    @Override
    public void fromData(DataInput in) throws IOException {
        int size = DataSerializer.readInteger(in);
        for (int i = 0; i < size; i++) {
            String member = DataSerializer.readString(in);
            Double score = DataSerializer.readDouble(in);
            memberMap.put(member, score);
            sortedMemberList.add(member);
        }
    }

    // Used to insert/delete members
    private int getInsertPosition(String member, Double score) {
        int l = 0;
        int r = sortedMemberList.size() - 1;

        while (l <= r) {
            int mid = (l + r) / 2;
            String midMember = sortedMemberList.get(mid);
            double midScore = getScore(midMember);

            /*
             * Compare score in middle of search range to input score, and adjust next search range
             * accordingly. In case a member is passed in and the middle score is equal to the
             * input score, check middle member's lexicographical order w.r.t input member.
             */
            if (score < midScore || (score == midScore && member.compareTo(midMember) < 0)) {
                r = mid - 1;
            } else {
                l = mid + 1;
            }
        }

        return l;
    }

    // Used for range calls, when member is not explicitly given
    private int getPosition(Double score, boolean goLeftOnEquals) {
        int l = 0;
        int r = sortedMemberList.size() - 1;

        while (l <= r) {
            int mid = (l + r) / 2;
            String midMember = sortedMemberList.get(mid);
            double midScore = getScore(midMember);

            if (score < midScore || (score == midScore && goLeftOnEquals)) {
                r = mid - 1;
            } else if (score > midScore || (score == midScore && !goLeftOnEquals)) {
                l = mid + 1;
            }
        }

        return l;
    }

    public Double insert(Double score, String member) {
        boolean alreadyExists = memberMap.containsKey(member);
        Double oldValue = null;
        if (alreadyExists) {
            oldValue = getScore(member);
            remove(member);
        }

        memberMap.put(member, score);
        sortedMemberList.add(getInsertPosition(member, score), member);

        return oldValue;
    }

    public void remove(String member) {
        double score = getScore(member);
        int position = getInsertPosition(member, score);
        sortedMemberList.remove(position - 1);
        memberMap.remove(member);
    }

    public Double getScore(String member) {
        return memberMap.get(member);
    }

    public int getSize() {
        return memberMap.size();
    }

    public List<Pair<String, Double>> getMembersInRange(int lb, int ub) {
        int size = sortedMemberList.size();
        int lowerBound = correct(lb);
        int upperBound = Math.min(size - 1, correct(ub));

        if (lowerBound > size - 1 || lowerBound > upperBound) return Collections.EMPTY_LIST;

        return sortedMemberList.subList(lowerBound, upperBound + 1).stream()
                .map(member -> {
                    Pair<String, Double> pair = Pair.of(member, getScore(member));
                    return pair;
                })
                .collect(Collectors.toList());
    }

    public List<Pair<String, Double>> getMembersInRevRange(int lb, int ub) {
        int size = sortedMemberList.size();
        int lowerBound = correct(lb);
        int upperBound = Math.min(size - 1, correct(ub));

        if (lowerBound > size - 1 || lowerBound > upperBound) return Collections.EMPTY_LIST;

        List<Pair<String, Double>> result = new ArrayList<>();

        for (int i = size - 1 - lowerBound; i >= size - 1 - upperBound; i--) {
            String member = sortedMemberList.get(i);
            result.add(Pair.of(member, getScore(member)));
        }

        return result;
    }

    private int correct(int index) {
        return index >= 0 ? index : sortedMemberList.size() + index;
    }

    public List<Pair<String, Double>> getMembersInRangeByScore(Double lowerScore, Double upperScore,
       boolean leftInclusive, boolean rightInclusive) {
        int lowerBound = getPosition(lowerScore, leftInclusive);
        int upperBound = Math.min(getPosition(upperScore, !rightInclusive),
                sortedMemberList.size());

        if (lowerBound > upperBound) return Collections.EMPTY_LIST;

        return sortedMemberList.subList(lowerBound, upperBound).stream()
                .map(member -> {
                    Pair<String, Double> pair = Pair.of(member, getScore(member));
                    return pair;
                })
                .collect(Collectors.toList());
    }
}
