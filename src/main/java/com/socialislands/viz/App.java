package com.socialislands.viz;

import com.mongodb.*;
import org.bson.types.ObjectId;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.project.api.*;

import org.openide.util.Lookup;

/**
 *
 * @author weidongyang
 */

public abstract class App implements Runnable
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
    protected Map friendHash;
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
        
        
        Iterator itr = friends.iterator(); 
        BasicDBObject friend = new BasicDBObject();
        friendHash = new HashMap();
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
        
        if(edges.size()>0)
            System.out.println(edges.toArray()[0].getClass().getName());
        
        itr = edges.iterator(); 
        BasicDBObject edge;
        while(itr.hasNext()) {

            edge = (BasicDBObject) itr.next(); 
            long node1 = Long.valueOf(edge.get("uid1").toString());
            long node2 = Long.valueOf(edge.get("uid2").toString());
            if (node2 > node1){ // skip symmetrical edges
                //2012-05-06 FB sometimes return a person to the edge list, but not to the node list. It's taken cared of here.
                Object obj1 = friendHash.get(node1);
                Object obj2 = friendHash.get(node2);
                if((obj1!=null)&&(obj2!=null)){
                    int idx1 = (Integer) obj1;
                    int idx2 = (Integer) obj2;
                    Edge e1 = graphModel.factory().newEdge(nodes[idx1], nodes[idx2]);
                    undirectedGraph.addEdge(e1);
                }else{
                    if(obj1==null)
                        System.out.println("node1 not in the list: "+node1+"!!!!");
                    if(obj2==null)
                        System.out.println("node2 not in the list: "+node2+"!!!!");
                }
            }
            //System.out.println(edge);

        }
            
        for (int i1 = 0; i1< friends.size(); i1++){
            Edge e2 = graphModel.factory().newEdge(nodes[i1], nego);
            undirectedGraph.addEdge(e2);
        }
       
        
        System.out.println(edges.size());
    }
    
    // Abstract methods -- required to be implemented by subclasses
    protected abstract void generateResult();

    protected  abstract void exportToMongo();
    protected  abstract void exportToFile();
    
    public abstract void run();
    
    /**
     * 
     * @param s
     * @throws UnknownHostException
     */
    public App(final String s) throws UnknownHostException {
        this.user_id = s;
        
        String mongo_url = (new YamlConfig("mongo.yml")).propertiesForCurrentEnv().getProperty("uri");
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
            this.userName = this.fb_profile.get("name").toString();
            this.userUid = Long.valueOf(this.fb_profile.get("uid").toString());
        }
        catch(java.util.NoSuchElementException e) {
            System.out.println("Could not find record in facebook_profiles or users collection with user_id: "+this.user_id);
        }
    }
}
