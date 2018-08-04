package org.apache.geode.redis;

import org.apache.geode.internal.tcp.ByteBufferInputStream;
import org.apache.geode.redis.internal.Pair;
import org.apache.geode.redis.internal.ZSet;
import org.apache.geode.redis.internal.ZSetRangeException;
import org.apache.geode.test.junit.categories.RedisTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import static org.apache.geode.internal.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@Category({RedisTest.class})
public class ZSetTest {
    @Test
    public void testZSetInsert() {
        ZSet zset = new ZSet();
        zset.insert(1.0, "a");
        zset.insert(5.0, "e");
        zset.insert(12.0, "l");
        zset.insert(16.0, "p");

        assertEquals(4, zset.getSize());
        assertEquals(1.0, zset.getScore("a"), 0.0);
        assertEquals(5.0, zset.getScore("e"), 0.0);
        assertEquals(12.0, zset.getScore("l"), 0.0);
        assertEquals(16.0, zset.getScore("p"), 0.0);
    }

    @Test
    public void testZSetGetMembersInRange() throws ZSetRangeException {
        ZSet zset = new ZSet();
        zset.insert(1.0, "a");
        zset.insert(5.0, "e");
        zset.insert(12.0, "l");
        zset.insert(16.0, "p");

        List<Pair<String, Double>> members = zset.getMembersInRange(1, 3);
        assertEquals(3, members.size());
        assertEquals("e", members.get(0).fst);
        assertEquals("l", members.get(1).fst);
        assertEquals("p", members.get(2).fst);

        members = zset.getMembersInRange(1, 4);
        assertEquals(3, members.size());
        assertEquals("e", members.get(0).fst);
        assertEquals("l", members.get(1).fst);
        assertEquals("p", members.get(2).fst);

        members = zset.getMembersInRange(1, -1);
        assertEquals(3, members.size());
        assertEquals("e", members.get(0).fst);
        assertEquals("l", members.get(1).fst);
        assertEquals("p", members.get(2).fst);

        members = zset.getMembersInRange(1, 2);
        assertEquals(2, members.size());
        assertEquals("e", members.get(0).fst);
        assertEquals("l", members.get(1).fst);

        members = zset.getMembersInRange(1, -2);
        assertEquals(2, members.size());
        assertEquals("e", members.get(0).fst);
        assertEquals("l", members.get(1).fst);

        members = zset.getMembersInRange(-1, 5);
        assertEquals(1, members.size());
        assertEquals("p", members.get(0).fst);

        members = zset.getMembersInRange(1, 0);
        assertEquals(0, members.size());

        members = zset.getMembersInRange(1, -5);
        assertEquals(0, members.size());

        members = zset.getMembersInRange(5, 6);
        assertEquals(0, members.size());
    }

    @Test
    public void testZSetGetMembersInRevRange() throws ZSetRangeException {
        ZSet zset = new ZSet();
        zset.insert(1.0, "a");
        zset.insert(5.0, "e");
        zset.insert(12.0, "l");
        zset.insert(16.0, "p");

        List<Pair<String, Double>> members = zset.getMembersInRevRange(1, 3);
        assertEquals(3, members.size());
        assertEquals("l", members.get(0).fst);
        assertEquals("e", members.get(1).fst);
        assertEquals("a", members.get(2).fst);

        members = zset.getMembersInRevRange(1, 4);
        assertEquals(3, members.size());
        assertEquals("l", members.get(0).fst);
        assertEquals("e", members.get(1).fst);
        assertEquals("a", members.get(2).fst);

        members = zset.getMembersInRevRange(1, -1);
        assertEquals(3, members.size());
        assertEquals("l", members.get(0).fst);
        assertEquals("e", members.get(1).fst);
        assertEquals("a", members.get(2).fst);

        members = zset.getMembersInRevRange(1, 2);
        assertEquals(2, members.size());
        assertEquals("l", members.get(0).fst);
        assertEquals("e", members.get(1).fst);

        members = zset.getMembersInRevRange(1, -2);
        assertEquals(2, members.size());
        assertEquals("l", members.get(0).fst);
        assertEquals("e", members.get(1).fst);

        members = zset.getMembersInRevRange(-1, 5);
        assertEquals(1, members.size());
        assertEquals("a", members.get(0).fst);

        members = zset.getMembersInRevRange(1, 0);
        assertEquals(0, members.size());

        members = zset.getMembersInRevRange(1, -5);
        assertEquals(0, members.size());

        members = zset.getMembersInRevRange(5, 6);
        assertEquals(0, members.size());
    }

    @Test
    public void testZSetGetMembersInRangeScore() {
        ZSet zset = new ZSet();
        zset.insert(1.0, "a");
        zset.insert(5.0, "e");
        zset.insert(12.0, "l");
        zset.insert(16.0, "p");

        List<Pair<String, Double>> members = zset.getMembersInRangeByScore(5.0, 16.0,
                true, true);
        assertEquals(3, members.size());
        assertEquals("e", members.get(0).fst);
        assertEquals("l", members.get(1).fst);
        assertEquals("p", members.get(2).fst);

        members = zset.getMembersInRangeByScore(1.0, 12.0,
                true, true);
        assertEquals(3, members.size());
        assertEquals("a", members.get(0).fst);
        assertEquals("e", members.get(1).fst);
        assertEquals("l", members.get(2).fst);

        members = zset.getMembersInRangeByScore(-Double.MAX_VALUE, Double.MAX_VALUE,
                true, true);
        assertEquals(4, members.size());
        assertEquals("a", members.get(0).fst);
        assertEquals("e", members.get(1).fst);
        assertEquals("l", members.get(2).fst);
        assertEquals("p", members.get(3).fst);
    }

    @Test
    public void testZSetGetMembersInRangeScoreExclusive() {
        ZSet zset = new ZSet();
        zset.insert(1.0, "a");
        zset.insert(5.0, "e");
        zset.insert(12.0, "l");
        zset.insert(16.0, "p");

        List<Pair<String, Double>> members = zset.getMembersInRangeByScore(5.0, 16.0,
                false, true);
        assertEquals(2, members.size());
        assertEquals("l", members.get(0).fst);
        assertEquals("p", members.get(1).fst);

        members = zset.getMembersInRangeByScore(1.0, 12.0,
                true, false);
        assertEquals(2, members.size());
        assertEquals("a", members.get(0).fst);
        assertEquals("e", members.get(1).fst);
    }

    @Test
    public void testZSetGetMembersInRangeScoreTie() {
        ZSet zset = new ZSet();
        zset.insert(1.0, "a");
        zset.insert(5.0, "f");
        zset.insert(5.0, "e");
        zset.insert(12.0, "l");
        zset.insert(16.0, "p");

        List<Pair<String, Double>> members = zset.getMembersInRangeByScore(5.0, 16.0,
                true, true);
        assertEquals(4, members.size());
        assertEquals("e", members.get(0).fst);
        assertEquals("f", members.get(1).fst);
        assertEquals("l", members.get(2).fst);
        assertEquals("p", members.get(3).fst);

        zset = new ZSet();
        zset.insert(5.0, "e");
        zset.insert(5.0, "l");
        zset.insert(16.0, "p");
        members = zset.getMembersInRangeByScore(5.0, 16.0,
                true, true);
        assertEquals(3, members.size());
        assertEquals("e", members.get(0).fst);
        assertEquals("l", members.get(1).fst);
        assertEquals("p", members.get(2).fst);

        members = zset.getMembersInRangeByScore(5.0, 16.0,
                false, true);
        assertEquals(1, members.size());
        assertEquals("p", members.get(0).fst);

        zset = new ZSet();
        zset.insert(5.0, "e");
        zset.insert(16.0, "l");
        zset.insert(16.0, "p");
        members = zset.getMembersInRangeByScore(14.0, 16.0,
                true, true);
        assertEquals(2, members.size());
        assertEquals("l", members.get(0).fst);
        assertEquals("p", members.get(1).fst);

        members = zset.getMembersInRangeByScore(5.0, 16.0,
                true, false);
        assertEquals(1, members.size());
        assertEquals("e", members.get(0).fst);
    }

    @Test
    public void testZSetGetMembersInRangeScoreUpdate() {
        ZSet zset = new ZSet();
        zset.insert(1.0, "a");
        zset.insert(5.0, "e");
        zset.insert(5.0, "l");
        zset.insert(16.0, "p");

        List<Pair<String, Double>> members = zset.getMembersInRangeByScore(5.0, 16.0,
                true, true);
        assertEquals(3, members.size());
        assertEquals("e", members.get(0).fst);
        assertEquals("l", members.get(1).fst);
        assertEquals("p", members.get(2).fst);

        Double old = zset.insert(13.0, "e");

        members = zset.getMembersInRangeByScore(5.0, 16.0,
                true, true);
        assertEquals(3, members.size());
        assertEquals("l", members.get(0).fst);
        assertEquals("e", members.get(1).fst);
        assertEquals("p", members.get(2).fst);
        assertEquals(5.0, old, 0.0);
    }

    @Test
    public void testZSetGetMembersInRangeLex() {
        ZSet zset = new ZSet();
        zset.insert(0.0, "a");
        zset.insert(0.0, "e");
        zset.insert(0.0, "l");
        zset.insert(0.0, "p");

        List<Pair<String, Double>> members = zset.getMembersInRangeByLex("a", "j",
                true, true);
        assertEquals(2, members.size());
        assertEquals("a", members.get(0).fst);
        assertEquals("e", members.get(1).fst);

        members = zset.getMembersInRangeByLex("-", "+",
                true, true);
        assertEquals(4, members.size());
        assertEquals("a", members.get(0).fst);
        assertEquals("e", members.get(1).fst);
        assertEquals("l", members.get(2).fst);
        assertEquals("p", members.get(3).fst);
    }

    @Test
    public void testZSetGetMembersInRangeScoreOutOfBounds() {
        ZSet zset = new ZSet();
        zset.insert(1.0, "a");
        zset.insert(5.0, "e");
        zset.insert(12.0, "l");
        zset.insert(16.0, "p");

        List<Pair<String, Double>> members = zset.getMembersInRangeByScore(0.0, 20.0,
                true, true);
        assertEquals(4, members.size());
        assertEquals("a", members.get(0).fst);
        assertEquals("e", members.get(1).fst);
        assertEquals("l", members.get(2).fst);
        assertEquals("p", members.get(3).fst);
    }

    @Test
    public void testZSetSerialization() throws IOException {
        ZSet zset = new ZSet();
        zset.insert(1.0, "a");
        zset.insert(5.0, "e");
        zset.insert(12.0, "l");
        zset.insert(16.0, "p");

        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(ba);
        zset.toData(out);
        out.flush();

        ByteBuffer bb = ByteBuffer.wrap(ba.toByteArray());
        ByteBufferInputStream bbis = new ByteBufferInputStream(bb);
        ZSet zset2 = new ZSet();
        zset2.fromData(bbis);
        List<Pair<String, Double>> members = zset2.getMembersInRangeByScore(0.0, 20.0,
                true, true);
        assertEquals(4, members.size());
        assertEquals("a", members.get(0).fst);
        assertEquals("e", members.get(1).fst);
        assertEquals("l", members.get(2).fst);
        assertEquals("p", members.get(3).fst);
    }

    @Test
    public void testZSetComplexity() {
        ZSet zset = new ZSet();
        insertN(1000, zset);

        long start1 = System.nanoTime();
        insertN(1, zset);
        long end1 = System.nanoTime();
        long t1 = end1 - start1;

        insertN(10000, zset);

        long start2 = System.nanoTime();
        insertN(1, zset);
        long end2 = System.nanoTime();
        long t2 = end2 - start2;

        double overhead = (double)t2/(double)t1;

        // logarithmic
        assertTrue(overhead < (4.0/3.0));
    }

    @Test
    public void testZSetRank() {
        ZSet zset = new ZSet();
        zset.insert(1.0, "a");
        zset.insert(5.0, "e");
        zset.insert(12.0, "l");
        zset.insert(16.0, "p");

        assertEquals((Integer)0, zset.getRank("a"));
        assertEquals((Integer)2, zset.getRank("l"));
        assertEquals((Integer)3, zset.getRank("p"));
    }

    private void insertN(int n, ZSet zset) {
        double startScore = (double) zset.getSize() + 1.0;
        for (int i = 0; i < n; i++) {
            double score = startScore + (double)i;
            zset.insert(score, Double.toHexString(score));
        }
    }
}
