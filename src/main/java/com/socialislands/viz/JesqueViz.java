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
public class JesqueViz extends JesqueWorker {

    public static void main(String[] args) throws Exception {
        final Config config = configureRedisConnection();
        
        // Because we currently don't stop workers properly by calling end()
        // when the program exists or is aborted somehow, we need to clean
        // the queues prior to start up.
        // TODO: This will wipe out any worker registration done for this queue.
        clearWorkerFromRedis("viz", config);   

        final Worker worker = new WorkerImpl(config,
                Arrays.asList("viz"), 
                map(entry("com.socialislands.viz.VizWorker", VizApp.class)));
        
        final Thread t = new Thread(worker);
        t.start();
        t.join();
    }    
}
