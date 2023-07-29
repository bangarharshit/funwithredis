package org.example;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Inspired from https://codedamn.com/news/backend/distributed-locks-with-redis
 */
public class DistributedLockExample {
    private List<Jedis> jedis;
    private Function<Integer, Integer> task;
    private int consumerNumber;
    private static SetParams SET_PARAMS = SetParams.setParams().nx().ex(1000);
    private static String RELEASE_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    private static String LOCK_KEY = "lock";

    public DistributedLockExample(List<Jedis> jedis, Function<Integer, Integer> task, int consumerNumber) {
        this.jedis = jedis;
        this.task = task;
        this.consumerNumber = consumerNumber;
    }

    public boolean acquireLock() {
        int acquired = 0;
        for (Jedis jedi : jedis) {
            acquired += "OK".equals(jedi.set(LOCK_KEY, String.valueOf(consumerNumber), SET_PARAMS))?1:0;
        }
        if (acquired>jedis.size()/2) {
            return true;
        }
        releaseLock();
        return false;
    }

    public void releaseLock() {
        for (Jedis jedi : jedis) {
            jedi.eval(RELEASE_SCRIPT, Collections.singletonList(LOCK_KEY), Collections.singletonList(String.valueOf(consumerNumber)));
        }
    }

    private void acquireAndDoWork() {
        System.out.println("acquiring lock " +  consumerNumber);
        while (!acquireLock()) {}
        System.out.println("Lock acquired by consumer " +  consumerNumber);
        task.apply(1);
        System.out.println("releasing lock " +  consumerNumber);
        releaseLock();
        System.out.println("released lock " +  consumerNumber);
    }

    public static void main(String[] args) {
        List<JedisPool> jedisPools = Arrays.asList(
                new JedisPool("localhost", 6379),
                new JedisPool("localhost", 6381),
                new JedisPool("localhost", 6382),
                new JedisPool("localhost", 6383),
                new JedisPool("localhost", 6384));


        Function<Integer, Integer> function = integer -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return integer;
        };
        for (int i = 0; i < 5; i++) {
            int finalI = i;
            new Thread(() -> {
                DistributedLockExample simpleLockExample = new DistributedLockExample(jedisPools.stream().map(JedisPool::getResource).collect(Collectors.toList()), function, finalI);
                simpleLockExample.acquireAndDoWork();
            }).start();
        }
    }
}
