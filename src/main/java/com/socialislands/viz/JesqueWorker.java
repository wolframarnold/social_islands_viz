/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.socialislands.viz;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import net.greghaines.jesque.Config;
import net.greghaines.jesque.ConfigBuilder;
import net.greghaines.jesque.utils.ResqueConstants;
import redis.clients.jedis.Jedis;

/**
 *
 * @author weidongyang
 */
public class JesqueWorker {
    
    protected static Config configureRedisConnection() {
        final ConfigBuilder jesque_config_builder = new ConfigBuilder();
        
        String redis_url = (new YamlConfig("redis.yml")).propertiesForCurrentEnv().getProperty("uri");
        if (redis_url != null) {
            // use Heroku settings
            try {
                URI redisURI = new URI(redis_url);
                jesque_config_builder.withHost(redisURI.getHost());
                jesque_config_builder.withPort(redisURI.getPort());
                String user_info = redisURI.getUserInfo();
                if (user_info != null) {
                  jesque_config_builder.withPassword(user_info.split(":",2)[1]);
                }
            }
            catch (URISyntaxException e) {
                System.out.println("ERROR! Parsing of Redis URL failed: " + e.getMessage());
            }
        }
        
        return jesque_config_builder.build();
    }     
    
    protected static void clearWorkerFromRedis(final String queueName, final Config config) {
        Jedis jedis = new Jedis(config.getHost(), config.getPort(), config.getTimeout());
        if (config.getPassword() != null)
        {
                jedis.auth(config.getPassword());
        }
        jedis.select(config.getDatabase());

        
        String key = config.getNamespace() + ":" +  ResqueConstants.WORKERS;
        Set<String> workers = jedis.smembers(key);
        
        String prefix = config.getNamespace() + ":" +  ResqueConstants.WORKER;
        for(String worker : workers) {
            if (worker.matches(".*"+queueName+".*")) {
                jedis.srem(key, worker);
            }
        }
    }

}
