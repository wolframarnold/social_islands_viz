/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.socialislands.viz;

import java.util.Arrays;
import net.greghaines.jesque.Config;
import net.greghaines.jesque.worker.Worker;
import net.greghaines.jesque.worker.WorkerImpl;
import static net.greghaines.jesque.utils.JesqueUtils.*;
import redis.clients.jedis.Jedis;


/**
 *
 * @author wolfram
 */
public class JesqueScoring extends JesqueWorker {

    public static void main(String[] args) throws Exception {
        final Config config = configureRedisConnection();
        
        // Because we currently don't stop workers properly by calling end()
        // when the program exists or is aborted somehow, we need to clean
        // the queues prior to start up.
        // TODO: This will wipe out any worker registration done by the viz app
        // and vice versa -- we should figure out how to clean up properly.
        Jedis jedis = new Jedis(config.getHost(), config.getPort(), config.getTimeout());
        jedis.flushDB();
        
        final Worker worker = new WorkerImpl(config,
                Arrays.asList("scoring"), 
                map(entry("com.socialislands.viz.ScoringWorker", ScoringApp.class)));
        
        final Thread t = new Thread(worker);
        System.out.println("Starting Scoring Worker thread");
        t.start();
        t.join();
    }    
}
