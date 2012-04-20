/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.socialislands.viz;

import java.net.URI;
import java.net.URISyntaxException;
import net.greghaines.jesque.Config;
import net.greghaines.jesque.ConfigBuilder;

/**
 *
 * @author weidongyang
 */
public class JesqueWorker {
    
    protected static Config configureRedisConnection() {
        final ConfigBuilder jesque_config_builder = new ConfigBuilder();
        
        String redis_url = System.getenv("REDISTOGO_URL");
        if (redis_url != null) {
            // use Heroku settings
            try {
                URI redisURI = new URI(redis_url);
                jesque_config_builder.withHost(redisURI.getHost());
                jesque_config_builder.withPort(redisURI.getPort());
                jesque_config_builder.withPassword(redisURI.getUserInfo().split(":",2)[1]);
            }
            catch (URISyntaxException e) {
                System.out.println("ERROR! Parsing of Redis URL failed: " + e.getMessage());
            }
        }
        
        return jesque_config_builder.build();
    }      
}
