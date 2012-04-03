package com.socialislands.viz;

import com.mongodb.*;
import org.bson.types.ObjectId;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.gephi.data.attributes.api.*;
import org.gephi.filters.api.*;
import org.gephi.filters.plugin.graph.DegreeRangeBuilder.*;
import org.gephi.graph.api.*;
import org.gephi.io.exporter.api.*;
import org.gephi.layout.plugin.*;
import org.gephi.layout.plugin.force.*;
import org.gephi.layout.plugin.force.yifanHu.*;
import org.gephi.layout.plugin.forceAtlas.*;
import org.gephi.preview.api.*;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.*;
import org.gephi.ranking.api.*;
import org.gephi.ranking.plugin.transformer.*;
import org.gephi.partition.api.*;
import org.gephi.partition.plugin.NodeColorTransformer;
import org.gephi.statistics.plugin.*;

import org.openide.util.Lookup;

import net.greghaines.jesque.*;
import net.greghaines.jesque.worker.*;
import net.greghaines.jesque.client.*;
import static net.greghaines.jesque.utils.JesqueUtils.*;


/**
 *
 * @author weidongyang
 */
class JavaFriend{
    long uid;
    String name;
    public JavaFriend(long _uid, String _name){
        uid = _uid;
        name = _name;
    }
}
 

class JavaEdge{
    int idx1;
    int idx2;
    public JavaEdge(int _idx1, int _idx2){
        idx1 = _idx1;
        idx2 = _idx2;
    }
    
}

public class App implements Runnable
{
    private static ProjectController pc;
    private static Workspace workspace;
    private static GraphModel graphModel;
    private static UndirectedGraph undirectedGraph;
    
    private void mongoDB2Graph()  throws Exception {
        pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        workspace = pc.getCurrentWorkspace();

        //Get a graph model - it exists because we have a workspace
        graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
        undirectedGraph = graphModel.getUndirectedGraph();
        
        Mongo m = new Mongo();
        DB db = m.getDB("trust_exchange_development");
        
        DBCollection fb_profiles = db.getCollection("facebook_profiles");
        
        BasicDBObject query = new BasicDBObject();
        query.put("user_id", new ObjectId(this.user_id));
        
        DBCursor cursor = fb_profiles.find(query);
        
        DBObject fb_profile = cursor.next();
        
        BasicDBList friends = (BasicDBList) fb_profile.get("friends");
        
        JavaFriend[] javaFriends = new JavaFriend[friends.size()];
        Node[] nodes = new Node[friends.size()];
        Hashtable<Long, Integer> friendHash = new Hashtable<Long, Integer>();
        Iterator itr = friends.iterator(); 
        BasicDBObject friend = new BasicDBObject();
        int idx = 0;
        while(itr.hasNext()) {
            friend = (BasicDBObject) itr.next(); 
            long uid = Long.valueOf(friend.get("uid").toString());
            String name = friend.get("name").toString();
            friendHash.put(new Long(uid), new Integer(idx));
            javaFriends[idx]=new JavaFriend(uid, name);
            String suid = ""+uid;
            Node n0 = graphModel.factory().newNode(suid);
            n0.getNodeData().setLabel(name);
            undirectedGraph.addNode(n0);
            nodes[idx]=n0;
            idx++;
            //            System.out.println(friend);

        }
        Node nego = graphModel.factory().newNode("0");
        nego.getNodeData().setLabel("Ego");
        undirectedGraph.addNode(nego);
        
        System.out.println(friend);
        System.out.println(friend.get("uid"));
        System.out.println(friends.size());
        
        BasicDBList edges = (BasicDBList) fb_profile.get("edges");
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
            Edge e2 = graphModel.factory().newEdge(nego, nodes[i1]);
            undirectedGraph.addEdge(e2);
        }
        
        long int1 = Long.valueOf(edge.get("uid1").toString());
        long int2 = Long.valueOf(edge.get("uid2").toString());
        
        System.err.println(int1);
        System.err.println(int2);
        
        System.out.println(edges.size());
        
        
//        for(BasicDBObject f : friends){
//            
//        }
        
//        System.out.println(fb_profile.get("friends"));
//        System.out.println(fb_profile.get("friends").getClass().getName());
//        edges = fb_profile.get("edges");
//        System.out.println(edges);
//        
//        Set<String> colls = db.getCollectionNames();
//        for (String s : colls){
//            System.out.println(s);
//        }
        // TODO code application logic here
        
    }
    
    private void genNExportGraph() {
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        PreviewModel model = Lookup.getDefault().lookup(PreviewController.class).getModel();
        FilterController filterController = Lookup.getDefault().lookup(FilterController.class);
        RankingController rankingController = Lookup.getDefault().lookup(RankingController.class); 
        
        System.out.println("inside genNExportGraph, Nodes: " + undirectedGraph.getNodeCount());
        System.out.println("Edges: " + undirectedGraph.getEdgeCount());

        //Filter      
        DegreeRangeFilter degreeFilter = new DegreeRangeFilter();
        degreeFilter.init(undirectedGraph);
        degreeFilter.setRange(new Range(6, Integer.MAX_VALUE));     //Remove nodes with degree < 30
        Query query = filterController.createQuery(degreeFilter);
        GraphView view = filterController.filter(query);
        graphModel.setVisibleView(view);    //Set the filter result as the visible view

                //See visible graph stats
        UndirectedGraph graphVisible = graphModel.getUndirectedGraphVisible();
        System.out.println("After Filtering, Nodes: " + graphVisible.getNodeCount());
        System.out.println("Edges: " + graphVisible.getEdgeCount());

        
        //Layout for 1 minute
                //Layout for 1 minute
        AutoLayout autoLayout = new AutoLayout(10, TimeUnit.SECONDS);
        autoLayout.setGraphModel(graphModel);
        YifanHuLayout firstLayout = new YifanHuLayout(null, new StepDisplacement(1f));
        ForceAtlasLayout secondLayout = new ForceAtlasLayout(null);
        AutoLayout.DynamicProperty adjustBySizeProperty = AutoLayout.createDynamicProperty("forceAtlas.adjustSizes.name", Boolean.TRUE, 0.1f);//True after 10% of layout time
        AutoLayout.DynamicProperty repulsionProperty = AutoLayout.createDynamicProperty("forceAtlas.repulsionStrength.name", new Double(500.), 0f);//500 for the complete period
        autoLayout.addLayout(firstLayout, 0.1f);
        autoLayout.addLayout(secondLayout, 0.9f, new AutoLayout.DynamicProperty[]{adjustBySizeProperty, repulsionProperty});
        autoLayout.execute();

                //Get Centrality
        GraphDistance distance = new GraphDistance();
        distance.setDirected(false);
        distance.execute(graphModel, attributeModel);

                //Rank color by Degree
        Ranking degreeRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, Ranking.DEGREE_RANKING);
        AbstractColorTransformer colorTransformer = (AbstractColorTransformer) rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_COLOR);
        colorTransformer.setColors(new Color[]{new Color(0xFEF0D9), new Color(0xB30000)});
        rankingController.transform(degreeRanking,colorTransformer);

                //Rank size by centrality
        AttributeColumn centralityColumn = attributeModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS);
        Ranking centralityRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, centralityColumn.getId());
        AbstractSizeTransformer sizeTransformer = (AbstractSizeTransformer) rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_SIZE);
        sizeTransformer.setMinSize(6);
        sizeTransformer.setMaxSize(20);
        rankingController.transform(centralityRanking,sizeTransformer);

                //Preview
        model.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
        model.getProperties().putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(Color.GRAY));
        model.getProperties().putValue(PreviewProperty.EDGE_THICKNESS, new Float(0.1f));
        model.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT, model.getProperties().getFontValue(PreviewProperty.NODE_LABEL_FONT).deriveFont(8));

        PartitionController partitionController = Lookup.getDefault().lookup(PartitionController.class);
        //Partition p = partitionController.buildPartition(attributeModel.getNodeTable().getColumn("source"), graph);
        //NodeColorTransformer nodeColorTransformer = new NodeColorTransformer();
        //nodeColorTransformer.randomizeColors(p);
        //partitionController.transform(p, nodeColorTransformer);
        
                //Run modularity algorithm - community detection
        Modularity modularity = new Modularity();
        modularity.execute(graphModel, attributeModel);

                //Partition with 'modularity_class', just created by Modularity algorithm
        AttributeColumn modColumn = attributeModel.getNodeTable().getColumn(Modularity.MODULARITY_CLASS);
        Partition p2 = partitionController.buildPartition(modColumn, undirectedGraph);
        System.out.println(p2.getPartsCount() + " partitions found");
        NodeColorTransformer nodeColorTransformer2 = new NodeColorTransformer();
        nodeColorTransformer2.randomizeColors(p2);
        partitionController.transform(p2, nodeColorTransformer2);
       
        //Export
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        try {
            ec.exportFile(new File("fb_simple_wolf.svg"));
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
    }
    
    public void run() {

        //Init a project - and therefore a workspace
        try {
            mongoDB2Graph();
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        
        System.out.println("From formed graph, Nodes: "+undirectedGraph.getNodeCount()+" Edges: "+undirectedGraph.getEdgeCount());

        genNExportGraph();
    }
    
    private final String user_id;
    
    public App(final String s) {
            this.user_id = s;
    }
    
    public static void main(String[] args) throws Exception {
        final Config config = new ConfigBuilder().build();

        final Worker worker = new WorkerImpl(config,
                Arrays.asList("viz"), 
                map(entry("com.socialislands.viz.VizWorker", App.class)));

        final Thread t = new Thread(worker);
        t.start();
        //Thread.yield();
        // loop til some exit condition
//        worker.end(false);
        t.join();
    }

}
