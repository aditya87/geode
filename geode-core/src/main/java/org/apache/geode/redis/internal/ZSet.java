package org.apache.geode.redis.internal;

import org.apache.geode.DataSerializable;
import org.apache.geode.DataSerializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private int position(Optional<String> maybeMember, Double score) {
        int l = 0;
        int r = sortedMemberList.size() - 1;
        String member = maybeMember.orElse(null);

        while (l <= r) {
            int mid = (l + r) / 2;
            String midMember = sortedMemberList.get(mid);
            double midScore = getScore(midMember);

            /*
             * Compare score in middle of search range to input score, and adjust next search range
             * accordingly. In case a member is passed in and the middle score is equal to the
             * input score, check middle member's lexicographical order w.r.t input member.
             */
            if (score < midScore
                || (score == midScore && member == null)
                || (score == midScore && member.compareTo(midMember) < 0))
            {
                r = mid - 1;
            } else {
                l = mid + 1;
            }
        }

        return l;
    }

    // Used for range calls, when member is not explicitly given
    private int position(Double score) {
        return position(Optional.empty(), score);
    }

    public void insert(Double score, String member) {
        boolean alreadyExists = memberMap.containsKey(member);
        if (alreadyExists) remove(member);
        memberMap.put(member, score);
        sortedMemberList.add(position(Optional.of(member), score), member);
    }

    public void remove(String member) {
        double score = getScore(member);
        int position = position(Optional.of(member), score);
        sortedMemberList.remove(position - 1);
        memberMap.remove(member);
    }

    public Double getScore(String member) {
        return memberMap.get(member);
    }

    public int getSize() {
        return memberMap.size();
    }

    public List<String> getMembersInRange(int lowerBound, int upperBound) throws ZSetRangeException {
        if (lowerBound < 0 || upperBound >= sortedMemberList.size()) {
            throw new ZSetRangeException();
        }

        return sortedMemberList.subList(lowerBound, upperBound + 1);
    }

    public List<String> getMembersInRangeByScore(Double lowerScore, Double upperScore) {
        int lowerBound = position(lowerScore);
        int upperBound = min(position(upperScore) + 1, sortedMemberList.size());

        return sortedMemberList.subList(lowerBound, upperBound);
    }

    private int min(int a, int b) {
        if (a <= b) return a;
        return b;
    }
}
