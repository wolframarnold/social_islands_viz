package com.socialislands.viz;

import com.mongodb.*;
import java.net.UnknownHostException;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.filters.plugin.graph.KCoreBuilder;

/**
 *
 * @author weidongyang
 */

public class ScoringApp extends App
{
    private int numNodes;
    private int numEdges;
    private double graphDensity;
    private int kCore;
    private int kCoreSize;
    @Override
    protected void generateResult() {
        numNodes = undirectedGraph.getNodeCount();
        numEdges = undirectedGraph.getEdgeCount();
        graphDensity = numEdges/(numNodes*(numNodes-1)*0.5);
        
        System.out.println("Total number of Nodes: " + numNodes);
        System.out.println("Total number of Edges: " + numEdges);
        System.out.println("Graph Density: "+ graphDensity);
        
        //calculating KCore
        GraphView tview = graphModel.newView();
        UndirectedGraph tgraph = graphModel.getUndirectedGraph(tview);
        KCoreBuilder.KCoreFilter kCoreFilter = new KCoreBuilder.KCoreFilter();
        int k = 1;
        
        while (tgraph.getNodeCount() > 0){
            kCoreSize = tgraph.getNodeCount();
            kCoreFilter.setK(k);
            kCoreFilter.filter(tgraph);
//            System.out.println("After KCore Filtering, K" + k +" Nodes: " + tgraph.getNodeCount());
//            System.out.println("Edges: " + tgraph.getEdgeCount());
            k++;
        }
        kCore = k-1;
        System.out.println("kCore: "+ kCore + " subgraph size: "+kCoreSize);
        
        
        
    }
    
    @Override
    protected void exportToMongo() {
        BasicDBObject mongo_query = new BasicDBObject("_id", this.fb_profile.get("_id"));
        BasicDBObject updateCmd = new BasicDBObject("$set", new BasicDBObject("kCore", kCore));
	this.fb_profiles.update(mongo_query, updateCmd);
        
        updateCmd = new BasicDBObject("$set", new BasicDBObject("kCoreSize", kCoreSize));
	this.fb_profiles.update(mongo_query, updateCmd);
        
        updateCmd = new BasicDBObject("$set", new BasicDBObject("graphDensity", graphDensity));
	this.fb_profiles.update(mongo_query, updateCmd);
        
        updateCmd = new BasicDBObject("$set", new BasicDBObject("degree", numNodes));
	this.fb_profiles.update(mongo_query, updateCmd);
    }
    
    /**
     * 
     */
    @Override
    public void run() {

        //Init a project - and therefore a workspace
        System.out.println("ScoringApp Run started...");
        try {
            mongoDB2Graph();
//            add2Graph("4f63c6e23f033175fe000004");
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        
        System.out.println("From formed graph, Nodes: "+undirectedGraph.getNodeCount()+" Edges: "+undirectedGraph.getEdgeCount());

        System.out.println("Start calculating scores...");
        generateResult();
        
        System.out.println("Start export scores to MongoDB...");
        exportToMongo();
        System.out.println("Done...");
    }
    
    /**
     * 
     * @param s
     * @throws UnknownHostException
     */
    public ScoringApp(final String s) throws UnknownHostException {
        super(s);
    }

    
}
