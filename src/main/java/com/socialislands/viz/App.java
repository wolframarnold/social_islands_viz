package com.socialislands.viz;

import com.mongodb.*;
import org.bson.types.ObjectId;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.Math;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.Query;
import org.gephi.filters.api.Range;
import org.gephi.filters.plugin.graph.DegreeRangeBuilder.DegreeRangeFilter;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.exporter.spi.GraphExporter;
import org.gephi.io.generator.plugin.RandomGraph;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ContainerFactory;
import org.gephi.io.importer.api.EdgeDefault;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2Builder;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.*;
import org.gephi.ranking.api.*;
import org.gephi.ranking.plugin.transformer.*;
import org.gephi.partition.api.*;
import org.gephi.partition.plugin.NodeColorTransformer;
import org.gephi.statistics.plugin.*;

import org.openide.util.Lookup;

import org.gephi.io.exporter.spi.CharacterExporter;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.gephi.filters.plugin.graph.KCoreBuilder;

/**
 *
 * @author weidongyang
 */

public class App implements Runnable
{
   protected ProjectController pc;
    protected Workspace workspace;
    protected GraphModel graphModel;
    protected UndirectedGraph undirectedGraph;
    protected String user_id;
    protected DBObject fb_profile;         
    protected DBCollection fb_profiles;
    protected DBCollection users;
    protected DBObject user;
    protected String userName;
    protected long userUid;
    protected Hashtable<Long, Integer> friendHash;
    protected Node[] nodes;
    protected Mongo m;
    protected DB db;
    
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
        Node nego = graphModel.factory().newNode(""+userUid);
        nego.getNodeData().setLabel(userName);
        undirectedGraph.addNode(nego);
        nodes[idx]=nego;
        idx++;
        
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
            
        for (int i1 = 0; i1< friends.size(); i1++){
            Edge e2 = graphModel.factory().newEdge(nodes[i1], nego);
            undirectedGraph.addEdge(e2);
        }
       
        
        System.out.println(edges.size());
    }
    
    protected void generateResult() {
    }

    protected void exportToMongo() {
    }
    
    public void run() {
    }
    
    /**
     * 
     * @param s
     * @throws UnknownHostException
     */
    public App(final String s) throws UnknownHostException {
        this.user_id = s;
        
        String mongo_url = System.getenv("MONGOHQ_URL");
        if (mongo_url == null) {
            mongo_url = System.getProperty("MONGOHQ_URL");
        }
        if (mongo_url == null) {
            System.out.println("ERROR! Could not find Mongo connection parameters. Did you set MONGOHQ_URL as environmen variable or Java system parameter?");
            return;
        }
        MongoURI mongoURI = new MongoURI(mongo_url);
        this.db = mongoURI.connectDB();
        
        // Only authenticate if username or password provided
        if (!"".equals(mongoURI.getUsername()) || mongoURI.getPassword().length > 0) {
            Boolean success = this.db.authenticate(mongoURI.getUsername(), mongoURI.getPassword());  

            if (!success) {
                System.out.println("MongoDB Authentication failed");
                return;
            }
        }
        this.fb_profiles = db.getCollection("facebook_profiles");
        
        BasicDBObject query = new BasicDBObject();
        query.put("user_id", new ObjectId(this.user_id));
        
        DBCursor cursor = fb_profiles.find(query);
        
        try {
            this.fb_profile = cursor.next();

            this.users = db.getCollection("users");
            BasicDBObject queryb = new BasicDBObject();
            queryb.put("_id", new ObjectId(this.user_id));

            cursor = users.find(queryb);
            this.user = cursor.next();
            this.userName = user.get("name").toString();

            this.userUid = Long.valueOf(user.get("uid").toString());        
            run();        
        }
        catch(java.util.NoSuchElementException e) {
            System.out.println("Could not find record in facebook_profiles or users collection with user_id: "+this.user_id);
        }
    }
}
