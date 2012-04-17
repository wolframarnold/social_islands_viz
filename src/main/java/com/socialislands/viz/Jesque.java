/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.socialislands.viz;

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
public class Jesque {

    public static void main(String[] args) throws Exception {

        // DO NOT comment this out!!!
        // To run the app standalone, use the StandAlone class
        final Config config = new ConfigBuilder().build();

        final Worker worker = new WorkerImpl(config,
                Arrays.asList("viz"), 
                map(entry("com.socialislands.viz.VizWorker", App.class)));
        
        final Thread t = new Thread(worker);
        t.start();
        t.join();
    }    
}
