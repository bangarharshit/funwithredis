package org.example;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.function.Function;

public class SimpleLockExample {
    private Jedis jedis;
    private Function<Integer, Integer> task;
    private int consumerNumber;
    private static SetParams setParams = SetParams.setParams().nx().ex(1000);

    public SimpleLockExample(Jedis jedis, Function<Integer, Integer> task, int consumerNumber) {
        this.jedis = jedis;
        this.task = task;
        this.consumerNumber = consumerNumber;
    }

    public boolean acquireLock() {
        return "OK".equals(jedis.set("lock", "1", setParams));
    }

    public void releaseLock() {
        jedis.del("lock");
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
        JedisPool jedisPool = new JedisPool("localhost", 6379);
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
                SimpleLockExample simpleLockExample = new SimpleLockExample(jedisPool.getResource(),function, finalI);
                simpleLockExample.acquireAndDoWork();
            }).start();
        }
    }
}
