package org.apache.geode.redis;

import org.apache.geode.redis.internal.ZSet;
import org.apache.geode.redis.internal.ZSetRangeException;
import org.apache.geode.test.junit.categories.RedisTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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

        List<String> members = zset.getMembersInRange(1, 3);
        assertEquals(3, members.size());
        assertEquals("e", members.get(0));
        assertEquals("l", members.get(1));
        assertEquals("p", members.get(2));
    }

    @Test
    public void testZSetGetMembersInRange_ThrowsOutOfBoundsError() {
        ZSet zset = new ZSet();
        zset.insert(1.0, "a");
        zset.insert(5.0, "e");
        zset.insert(12.0, "l");
        zset.insert(16.0, "p");

        Exception thrown = null;
        try {
            zset.getMembersInRange(1, 4);
        } catch(Exception e) {
            thrown = e;
        }

        assertTrue(thrown instanceof ZSetRangeException);
    }

    @Test
    public void testZSetGetMembersInRangeScore() {
        ZSet zset = new ZSet();
        zset.insert(1.0, "a");
        zset.insert(5.0, "e");
        zset.insert(12.0, "l");
        zset.insert(16.0, "p");

        List<String> members = zset.getMembersInRangeByScore(5.0, 16.0);
        assertEquals(3, members.size());
        assertEquals("e", members.get(0));
        assertEquals("l", members.get(1));
        assertEquals("p", members.get(2));

        members = zset.getMembersInRangeByScore(1.0, 12.0);
        assertEquals(3, members.size());
        assertEquals("a", members.get(0));
        assertEquals("e", members.get(1));
        assertEquals("l", members.get(2));
    }

    @Test
    public void testZSetGetMembersInRangeScoreTie() {
        ZSet zset = new ZSet();
        zset.insert(1.0, "a");
        zset.insert(5.0, "e");
        zset.insert(5.0, "f");
        zset.insert(12.0, "l");
        zset.insert(16.0, "p");

        List<String> members = zset.getMembersInRangeByScore(5.0, 16.0);
        assertEquals(4, members.size());
        assertEquals("e", members.get(0));
        assertEquals("f", members.get(1));
        assertEquals("l", members.get(2));
        assertEquals("p", members.get(3));
    }

    @Test
    public void testZSetGetMembersInRangeScoreUpdate() {
        ZSet zset = new ZSet();
        zset.insert(1.0, "a");
        zset.insert(5.0, "e");
        zset.insert(5.0, "l");
        zset.insert(16.0, "p");

        List<String> members = zset.getMembersInRangeByScore(5.0, 16.0);
        assertEquals(3, members.size());
        assertEquals("e", members.get(0));
        assertEquals("l", members.get(1));
        assertEquals("p", members.get(2));

        zset.insert(13.0, "e");

        members = zset.getMembersInRangeByScore(5.0, 16.0);
        assertEquals(3, members.size());
        assertEquals("l", members.get(0));
        assertEquals("e", members.get(1));
        assertEquals("p", members.get(2));
    }

    @Test
    public void testZSetGetMembersInRangeScoreOutOfBounds() {
        ZSet zset = new ZSet();
        zset.insert(1.0, "a");
        zset.insert(5.0, "e");
        zset.insert(12.0, "l");
        zset.insert(16.0, "p");

        List<String> members = zset.getMembersInRangeByScore(0.0, 20.0);
        assertEquals(4, members.size());
        assertEquals("a", members.get(0));
        assertEquals("e", members.get(1));
        assertEquals("l", members.get(2));
        assertEquals("p", members.get(3));
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

    private void insertN(int n, ZSet zset) {
        double startScore = (double) zset.getSize() + 1.0;
        for (int i = 0; i < n; i++) {
            double score = startScore + (double)i;
            zset.insert(score, Double.toHexString(score));
        }
    }
}
