package org.example;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.UUID;

public class HyperLogLogExample {

    private JedisPool jedisPool;

    public HyperLogLogExample(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public void impressView(String video, String userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.pfadd(video, userId);
        }
    }

    public long getViewCount(String... videos) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.pfcount(videos);
        }
    }


    public static void main(String[] args) {
        HyperLogLogExample hyperLogLogExample = new HyperLogLogExample(new JedisPool("localhost", 6379));
//        hyperLogLogExample.testInsert();
        System.out.println(hyperLogLogExample.getViewCount("video3", "video2"));
        System.out.println(hyperLogLogExample.getViewCount("video1", "video2"));
        System.out.println(hyperLogLogExample.getViewCount("video1", "video3"));
    }

    private void testInsert() {
        for (int i = 0; i < 10000; i++) {
            String uuid = UUID.randomUUID().toString();
            impressView("video2", uuid);
            impressView("video3", uuid);
        }
    }

}
