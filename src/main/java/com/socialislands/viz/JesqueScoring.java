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


/**
 *
 * @author wolfram
 */
public class JesqueScoring extends JesqueWorker {

    public static void main(String[] args) throws Exception {
        final Config config = configureRedisConnection();
        
        final Worker worker = new WorkerImpl(config,
                Arrays.asList("scoring"), 
                map(entry("com.socialislands.viz.ScoringWorker", ScoringApp.class)));
        
        final Thread t = new Thread(worker);
        t.start();
        t.join();
    }    
}
