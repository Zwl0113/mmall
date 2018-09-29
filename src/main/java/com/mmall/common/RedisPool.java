package com.mmall.common;

import com.mmall.util.PropertiesUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisPool {
    private static JedisPool jedisPool;//jedis连接池
    private static Integer maxTotal = Integer.parseInt(PropertiesUtil.getProperty("redis.max.total","20"));//最大连接数
    private static Integer minIdle = Integer.parseInt(PropertiesUtil.getProperty("redis.max.idle","20"));//最小空闲连接数
    private static Integer maxIdle = Integer.parseInt(PropertiesUtil.getProperty("redis.min.idle","20"));//最大空闲连接数
    private static Boolean testOnBorrow  = Boolean.parseBoolean(PropertiesUtil.getProperty("redis.test.borrow","true"));//在获取一个jedis实例时，是否需要验证操作
    private static Boolean testOnReturn = Boolean.parseBoolean(PropertiesUtil.getProperty("redis.test.return","true"));//在返回一个jedis实例时，是否需要验证操作
    private static String host = PropertiesUtil.getProperty("redis.hosts");
    private static Integer port = Integer.parseInt(PropertiesUtil.getProperty("redis.port"));

    private static void initPool(){
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setBlockWhenExhausted(true);
        config.setTestOnBorrow(testOnBorrow);
        config.setTestOnReturn(testOnReturn);

        jedisPool = new JedisPool(config,host,port,2*1000);
    }

    static{
        initPool();
    }

    public static Jedis getJedis(){
        return jedisPool.getResource();
    }

    public static void returnBrokenJedis(Jedis jedis){
        jedisPool.returnBrokenResource(jedis);
    }

    public static void returnResource(Jedis jedis){
        jedisPool.returnResource(jedis);
    }

    public static void main(String[] args) {
        Jedis jedis = null;
        try {
            jedis = RedisPool.getJedis();
        } catch (Exception e) {
            e.printStackTrace();
        }
        jedis.set("test","huiren");
        RedisPool.returnResource(jedis);
        System.out.println("program is end");
    }
}
