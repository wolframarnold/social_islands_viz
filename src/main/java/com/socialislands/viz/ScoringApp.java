package com.socialislands.viz;

import com.mongodb.*;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Iterator;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.filters.plugin.graph.KCoreBuilder;
import org.gephi.graph.api.*;
import org.gephi.project.api.ProjectController;
import org.gephi.statistics.plugin.ClusteringCoefficient;
import org.openide.util.Lookup;

/**
 *
 * @author weidongyang
 */

public class ScoringApp extends App
{
    private int numNodes;
    private int numEdges;
    private double graphDensity;
    double averageClusteringCoefficient;
    private int kCore;
    private int kCoreSize;
    
    @Override
    protected void mongoDB2Graph()  throws Exception {
        pc = Lookup.getDefault().lookup(ProjectController.class);
        
        pc.newProject();
        workspace = pc.getCurrentWorkspace();

        //Get a graph model - it exists because we have a workspace
        graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
        undirectedGraph = graphModel.getUndirectedGraph();
        
        BasicDBList friends = (BasicDBList) this.fb_profile.get("friends");
        
        nodes = new Node[friends.size()*2];
        friendHash = new Hashtable<Long, Integer>();
        
        Iterator itr = friends.iterator(); 
        BasicDBObject friend = new BasicDBObject();
        int idx = 0;
        while(itr.hasNext()) {
            friend = (BasicDBObject) itr.next(); 
            long uid = Long.valueOf(friend.get("uid").toString());
            String name = friend.get("name").toString();
            friendHash.put(new Long(uid), new Integer(idx));
            String suid = ""+uid;
            Node n0 = graphModel.factory().newNode(suid);
            n0.getNodeData().setLabel(name);
            undirectedGraph.addNode(n0);
            nodes[idx]=n0;
            idx++;
            //            System.out.println(friend);

        }
        
        friendHash.put(new Long(userUid), idx);
        
        System.out.println(friend);
        System.out.println(friend.get("uid"));
        System.out.println(friends.size());
        
        BasicDBList edges = (BasicDBList) this.fb_profile.get("edges");
        System.out.println(edges.toArray()[0].getClass().getName());
        
        itr = edges.iterator(); 
        BasicDBObject edge = new BasicDBObject();
        while(itr.hasNext()) {

            edge = (BasicDBObject) itr.next(); 
            long node1 = Long.valueOf(edge.get("uid1").toString());
            long node2 = Long.valueOf(edge.get("uid2").toString());
            if (node2 > node1){ // skip symmetrical edges
                int idx1 = friendHash.get(node1);
                int idx2 = friendHash.get(node2);
                Edge e1 = graphModel.factory().newEdge(nodes[idx1], nodes[idx2]);
                undirectedGraph.addEdge(e1);
            }
            //System.out.println(edge);

        }
        
        System.out.println(edges.size());
    }
    
    
    @Override
    protected void generateResult() {
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        numNodes = undirectedGraph.getNodeCount();
        numEdges = undirectedGraph.getEdgeCount();
        graphDensity = numEdges/(numNodes*(numNodes-1)*0.5);
        
        System.out.println("Total number of Nodes: " + numNodes);
        System.out.println("Total number of Edges: " + numEdges);
        System.out.println("Graph Density: "+ graphDensity);
        
        
        ClusteringCoefficient clusteringCoefficient = new ClusteringCoefficient();
        clusteringCoefficient.execute(graphModel, attributeModel);
        averageClusteringCoefficient = clusteringCoefficient.getAverageClusteringCoefficient();
        System.out.println("Average Clustering Coefficient: " + averageClusteringCoefficient);
        
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
        
        updateCmd = new BasicDBObject("$set", new BasicDBObject("averageClusteringCoefficient", averageClusteringCoefficient));
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
