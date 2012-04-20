/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.socialislands.viz;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import net.greghaines.jesque.Config;
import net.greghaines.jesque.ConfigBuilder;
import net.greghaines.jesque.worker.Worker;
import net.greghaines.jesque.worker.WorkerImpl;
import static net.greghaines.jesque.utils.JesqueUtils.*;


/**
 *
 * @author wolfram
 */
public class JesqueViz {

    public static void main(String[] args) throws Exception {
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
        
        final Config config = jesque_config_builder.build();
        
        final Worker worker = new WorkerImpl(config,
                Arrays.asList("viz"), 
                map(entry("com.socialislands.viz.VizWorker", App.class)));
        
        final Thread t = new Thread(worker);
        t.start();
        t.join();
    }    
}
