package com.socialislands.viz;

import com.mongodb.*;
import org.bson.types.ObjectId;

import java.net.UnknownHostException;
import java.util.*;

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
    protected ObjectId facebook_profile_id;
    protected DBObject fb_profile;
    protected DBObject fb_graph;
    protected DBCollection fb_profiles_collection;
    protected String fb_name;
    protected HashMap<Long,Node> nodes;
    protected Long fb_uid;
    protected DB db;
    protected String postbackUrl;
    
    protected void mongoDB2Graph()  throws Exception {
        pc = Lookup.getDefault().lookup(ProjectController.class);
        
        pc.newProject();
        workspace = pc.getCurrentWorkspace();

        //Get a graph model - it exists because we have a workspace
        graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
        undirectedGraph = graphModel.getUndirectedGraph();
        nodes = new HashMap<Long,Node>();
        
        BasicDBList friend_ids = (BasicDBList) this.fb_profile.get("facebook_profile_uids");
        //set up a HashSet to hold friend ids, later each friend's friend will be checked against this list.
        Set<Long> friend_ids_set = new HashSet<Long>();
        
        Iterator f_itr = friend_ids.iterator();
        while (f_itr.hasNext()){
            long f_uid = ((Number)f_itr.next()).longValue();
            friend_ids_set.add(f_uid);
        }
        System.out.println("number of unique friends: " + friend_ids_set.size());
        
        
        BasicDBObject query = new BasicDBObject();
        query.put("uid", new BasicDBObject("$in", friend_ids));
        DBCursor cursor = this.fb_profiles_collection.find(query);
        
        addNode(fb_uid,fb_name);  // Ego node
        
        while(cursor.hasNext()) {
            DBObject friend_profile = cursor.next();
            Long friend_uid = ((Number) friend_profile.get("uid")).longValue();
            addNode(friend_uid, (String)friend_profile.get("name"));

            // Add edge from ego node to friend
            Edge edge_ego = graphModel.factory().newEdge(nodes.get(fb_uid), nodes.get(friend_uid));
            undirectedGraph.addEdge(edge_ego);
            
            // Add nodes and edges for all friends of friends
            BasicDBList friends_friend_ids = (BasicDBList) friend_profile.get("facebook_profile_uids");
            Iterator ff_itr = friends_friend_ids.iterator();
            while(ff_itr.hasNext()) {
                Long ff_uid = ((Number)ff_itr.next()).longValue();
                if(friend_ids_set.contains(ff_uid)){
                    addNode(ff_uid, null);
                    // We wind up adding the edge twice, once from each direction, but that's OK
                    Edge edge_ff = graphModel.factory().newEdge(nodes.get(friend_uid), nodes.get(ff_uid));
                    undirectedGraph.addEdge(edge_ff);
                }
            }
        }
    }

    protected void addNode(Long uid, String name) {
        // Add Node
        Node node = nodes.get(uid);
        if (node == null) {
            node = graphModel.factory().newNode(); //Long.toString(friend_uid));
            nodes.put(uid, node);
            undirectedGraph.addNode(node);
        }
        if (name != null) {
            node.getNodeData().setLabel(name);
        }
    }
    
    // Abstract methods -- required to be implemented by subclasses
    protected abstract void generateResult();

    protected  abstract void exportToMongo();
    
    public abstract void run();
    
    /**
     * 
     * @param s
     * @throws UnknownHostException
     */
    public App(final String s, final String url) throws UnknownHostException {
        this.facebook_profile_id = new ObjectId(s);
        this.postbackUrl = url;
        
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
        this.fb_profiles_collection    = db.getCollection("facebook_profiles");
        
        BasicDBObject query = new BasicDBObject("_id", this.facebook_profile_id);
        
        this.fb_profile = this.fb_profiles_collection.findOne(query);
        if (this.fb_profile == null) {
            System.out.println("Could not find record in facebook_profiles or users collection with facebook_profile_id: "+this.facebook_profile_id);
            return;
        }
        
        this.fb_name = (String)  this.fb_profile.get("name");

//        this.fb_uid  = (Long)this.fb_profile.get("uid");
        this.fb_uid  = ((Number)this.fb_profile.get("uid")).longValue();
    }
}
