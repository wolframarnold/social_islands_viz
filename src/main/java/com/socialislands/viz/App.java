package com.socialislands.viz;

import com.mongodb.*;
import org.bson.types.ObjectId;

import java.awt.Color;
import java.io.*;
import java.lang.Math;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

import net.greghaines.jesque.*;
import net.greghaines.jesque.worker.*;
import net.greghaines.jesque.client.*;
import static net.greghaines.jesque.utils.JesqueUtils.*;
import org.gephi.io.exporter.spi.CharacterExporter;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jfree.chart.*;
import org.jfree.data.statistics.*;
import org.jfree.chart.plot.PlotOrientation;

/**
 *
 * @author weidongyang
 */

public class App implements Runnable
{
    private ProjectController pc;
    private Workspace workspace;
    private GraphModel graphModel;
    private UndirectedGraph undirectedGraph;
    private String user_id;
    private DBObject fb_profile;         
    private DBCollection fb_profiles;
    private DBCollection users;
    private DBObject user;
    private String userName;
    private long userUid;
    Hashtable<Long, Integer> friendHash;
    Node[] nodes;
    Mongo m;
    DB db;
    
    private void mongoDB2Graph()  throws Exception {
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
    
    private void add2Graph(String user_id)  throws Exception {
        System.out.println("Adding data from Mongo for user: "+user_id);
        
        BasicDBObject query = new BasicDBObject();
        query.put("user_id", new ObjectId(user_id));
        
        DBCursor cursor = fb_profiles.find(query);
        
        DBObject fb_profile = cursor.next();
        
        BasicDBObject queryb = new BasicDBObject();
        queryb.put("_id", new ObjectId(user_id));
        
        cursor = users.find(queryb);
        DBObject user = cursor.next();
        
        String userName = user.get("name").toString();

        Long userUid = Long.valueOf(user.get("uid").toString());        
      
        
        BasicDBList friends = (BasicDBList) fb_profile.get("friends");
       
        Iterator itr = friends.iterator(); 
        BasicDBObject friend = new BasicDBObject();
        int idx = friendHash.size();
        while(itr.hasNext()) {
            friend = (BasicDBObject) itr.next(); 
            long uid = Long.valueOf(friend.get("uid").toString());
            String name = friend.get("name").toString();
            if(!friendHash.containsKey(uid)){
                friendHash.put(new Long(uid), new Integer(idx));
                String suid = ""+uid;
                Node n0 = graphModel.factory().newNode(suid);
                n0.getNodeData().setLabel(name);
                undirectedGraph.addNode(n0);
                nodes[idx]=n0;
                idx++;
            }
        }
        if(!friendHash.containsKey(userUid)){
            friendHash.put(new Long(userUid), idx);
            Node nego = graphModel.factory().newNode(""+userUid);
            nego.getNodeData().setLabel(userName);
            undirectedGraph.addNode(nego);
            nodes[idx]=nego;
            idx++;
        }
        
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
                System.out.println("node1: "+node1+" idx: "+idx1+ " node2: "+node2 + " idx: "+idx2);
                Edge e1 = graphModel.factory().newEdge(nodes[idx1], nodes[idx2]);
                undirectedGraph.addEdge(e1);
            }
            //System.out.println(edge);

        }

        
        int egoidx = friendHash.get(userUid);
        itr = friends.iterator(); 
        friend = new BasicDBObject();
        while(itr.hasNext()) {
            friend = (BasicDBObject) itr.next(); 
            long uid = Long.valueOf(friend.get("uid").toString());
            int nodeIdx = friendHash.get(uid);
            Edge e2 = graphModel.factory().newEdge(nodes[nodeIdx], nodes[egoidx]);
            undirectedGraph.addEdge(e2);
        }
        
        System.out.println(edges.size());
    }
    
    private void genGraph() {
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        PreviewModel model = Lookup.getDefault().lookup(PreviewController.class).getModel();
        FilterController filterController = Lookup.getDefault().lookup(FilterController.class);
        RankingController rankingController = Lookup.getDefault().lookup(RankingController.class); 
        
        System.out.println("inside genGraph, Nodes: " + undirectedGraph.getNodeCount());
        System.out.println("Edges: " + undirectedGraph.getEdgeCount());

        
//        get degree distribution
        Degree degree = new Degree();
        degree.execute(graphModel, attributeModel);
        System.out.println("average degree " +degree.getAverageDegree());
        AttributeColumn degreeColumn = attributeModel.getNodeTable().getColumn("Degree");

        int[] degreeArray;
        degreeArray = new int[undirectedGraph.getNodeCount()];
        int i1=0;
        for (Node n : graphModel.getGraph().getNodes()){
            int val = Integer.valueOf(n.getNodeData().getAttributes().getValue(Ranking.DEGREE_RANKING).toString());
            degreeArray[i1++] = val;
//            System.out.print(val  + " ");
        }
        calculateStatistics(degreeArray);
        
        System.out.println(" ");
        
        
        
        //Filter      
        System.out.println("Filter by degree...");
        DegreeRangeFilter degreeFilter = new DegreeRangeFilter();
        degreeFilter.init(undirectedGraph);
        degreeFilter.setRange(new Range(4, Integer.MAX_VALUE));     //Remove nodes with degree < 30
        Query query = filterController.createQuery(degreeFilter);
        GraphView view = filterController.filter(query);
        graphModel.setVisibleView(view);    //Set the filter result as the visible view

        //See visible graph stats
        UndirectedGraph graphVisible = graphModel.getUndirectedGraphVisible();
        System.out.println("After Filtering, Nodes: " + graphVisible.getNodeCount());
        System.out.println("Edges: " + graphVisible.getEdgeCount() + "    start layout...");

        
        //Layout for 1 minute
//        AutoLayout autoLayout = new AutoLayout(40, TimeUnit.SECONDS);
//        autoLayout.setGraphModel(graphModel);
//        YifanHuLayout firstLayout = new YifanHuLayout(null, new StepDisplacement(1f));
//        ForceAtlasLayout secondLayout = new ForceAtlasLayout(null);
//        AutoLayout.DynamicProperty adjustBySizeProperty = AutoLayout.createDynamicProperty("forceAtlas.adjustSizes.name", Boolean.TRUE, 0.1f);//True after 10% of layout time
//        AutoLayout.DynamicProperty repulsionProperty = AutoLayout.createDynamicProperty("forceAtlas.repulsionStrength.name", new Double(500.), 0f);//500 for the complete period
//        autoLayout.addLayout(firstLayout, 0.1f);
//        autoLayout.addLayout(secondLayout, 0.9f, new AutoLayout.DynamicProperty[]{adjustBySizeProperty, repulsionProperty});
//        autoLayout.execute();

        ForceAtlas2 fa2Layout = new ForceAtlas2(new ForceAtlas2Builder());
        fa2Layout.setGraphModel(graphModel);
        fa2Layout.resetPropertiesValues();
        fa2Layout.setEdgeWeightInfluence(1.0);
        fa2Layout.setGravity(1.0);
        fa2Layout.setScalingRatio(2.0);
        fa2Layout.setBarnesHutTheta(1.2);
        fa2Layout.setJitterTolerance(0.1);
        
        
        fa2Layout.initAlgo();
        int cnt = 0;
        for(int i=0; i<250 && fa2Layout.canAlgo(); i++){
            fa2Layout.goAlgo();
            cnt++;
        }
        System.out.println("Layout done, num itr is: "+ cnt + "    ...");
        fa2Layout.endAlgo();
        
        
        //Get Centrality
        GraphDistance distance = new GraphDistance();
        distance.setDirected(false);
        distance.execute(graphModel, attributeModel);

        
        //Rank size by centrality
        System.out.println("Rank size by centrality...");
        AttributeColumn centralityColumn = attributeModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS);      
        
        
        
        double maxVal = 3000;
        double extremVal = 0;
        int valCnt = 0;
        for (Node n : graphModel.getGraph().getNodes()){
            double val = Double.valueOf(n.getNodeData().getAttributes().getValue(GraphDistance.BETWEENNESS).toString());
//            System.out.print(val  + " ");
            if (val > maxVal){
                extremVal = val;
                n.getNodeData().getAttributes().setValue(GraphDistance.BETWEENNESS, maxVal);
                valCnt ++;
            }
        }
        
        System.out.println("numExtrem " + valCnt +" val " + extremVal);
       
        
        
        Ranking centralityRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, centralityColumn.getId());
   
        AbstractSizeTransformer sizeTransformer = (AbstractSizeTransformer) rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_SIZE);
        sizeTransformer.setMinSize(6);
        sizeTransformer.setMaxSize(30);
//        rankingController.setInterpolator(Interpolator.newBezierInterpolator(new Float(0.01), new Float(1.0), new Float(0.0), new Float(1.0)));
        rankingController.transform(centralityRanking,sizeTransformer);

        //Preview
        System.out.println("Partitioning...");
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
    }
    
    private void calculateStatistics(int[] arr){
        DescriptiveStatistics stat = new DescriptiveStatistics();
        int numItem = arr.length;
        double[] value = new double[numItem];
        
        double sum = 0, sumSQ = 0;
        for (int i1 = 0; i1< numItem; i1++){
            stat.addValue(arr[i1]);
            value[i1]=arr[i1];
            sum+=arr[i1];
            sumSQ += arr[i1]*arr[i1];
        }
        double mean = sum / numItem;
        double std = Math.sqrt(sumSQ/numItem  - mean*mean);
        System.out.println("total nodes:" + numItem + ", average degree: " + mean+", std: " + std);
        
        double meanb = stat.getMean();
        double stdb = stat.getStandardDeviation();
        double medianb = stat.getPercentile(50);
        System.out.println("total nodes:" + numItem + ", average degreeb: " + meanb+", stdb: " + stdb + "median: "+medianb);
        
        BasicDBObject query = new BasicDBObject("_id", this.fb_profile.get("_id"));
        BasicDBObject updateCmd = new BasicDBObject("$set", new BasicDBObject("averageDegree", meanb));
	this.fb_profiles.update(query, updateCmd);
        updateCmd = new BasicDBObject("$set", new BasicDBObject("stdDegree", stdb));
	this.fb_profiles.update(query, updateCmd);

        
         try{
            // Create file 
            FileWriter fstream = new FileWriter("edges.txt");
            BufferedWriter out = new BufferedWriter(fstream);
            for (int i1 = 0; i1< numItem; i1++){
                out.write(""+arr[i1]+"\n");
            }
            //Close the output stream
            out.close();
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
         
         
         HistogramDataset dataset = new HistogramDataset();
         dataset.setType(HistogramType.RELATIVE_FREQUENCY);
         dataset.addSeries("Histogram", value, 20);
         String plotTitle = "Histogram";
         String xaxis = "Number of Connections";
         String yaxis = "Frequency";
         
         PlotOrientation orientation = PlotOrientation.VERTICAL;
         boolean show = false;
         boolean toolTips = false;
         boolean urls = false;
         JFreeChart chart = ChartFactory.createHistogram(plotTitle, xaxis, yaxis, dataset, orientation, show, toolTips, urls);
         int width = 500;
         int height = 300;
         try{
            ChartUtilities.saveChartAsPNG(new File("hist.png"), chart, width, height);
//             StringWriter stringWriter = new StringWriter();
        
//             ChartUtilities.saveChartAsPNG(stringWriter, chart, width, height);
         }catch(IOException e){}
    }
    
    private void exportGraph() {
         //Export
//        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
//        try {
//            ec.exportFile(new File("fb_simple_wolf.svg"));
//        } catch (IOException ex) {
//            ex.printStackTrace();
//            return;
//        }
//        

        //Export only visible graph -- to string buffer, then to Mongo as "graph" attribute on Facebook profile record.
        
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        GraphExporter exporter = (GraphExporter) ec.getExporter("gexf");     //Get GEXF exporter
        exporter.setExportVisible(true);  //Only exports the visible (filtered) graph
        exporter.setWorkspace(workspace);
        StringWriter stringWriter = new StringWriter();
        ec.exportWriter(stringWriter, (CharacterExporter)exporter);
        String result = stringWriter.toString();
        
        BasicDBObject query = new BasicDBObject("_id", this.fb_profile.get("_id"));
        BasicDBObject updateCmd = new BasicDBObject("$set", new BasicDBObject("graph", result));
 
	this.fb_profiles.update(query, updateCmd);

//        GraphExporter exporter = (GraphExporter) ec.getExporter("gexf");     //Get GEXF exporter
//        try {
//            ec.exportFile(new File("../../Sites/raphv-gexf-js/graph.gexf"), exporter);
//        } catch (IOException ex) {
//            ex.printStackTrace();
//            return;
//        }
//        try {
//            ec.exportFile(new File("../social_islands/public/graph.svg"));
//        } catch (IOException ex) {
//            ex.printStackTrace();
//            return;
//        }
    }
    
    /**
     * 
     */
    public void run() {

        //Init a project - and therefore a workspace
        System.out.println("Run started...");
        try {
            mongoDB2Graph();
//            add2Graph("4f63c6e23f033175fe000004");
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        
        System.out.println("From formed graph, Nodes: "+undirectedGraph.getNodeCount()+" Edges: "+undirectedGraph.getEdgeCount());

        System.out.println("Start gen graph...");
        genGraph();
        
        System.out.println("Start export graph...");
        exportGraph();
        System.out.println("Done...");
    }
    
    /**
     * 
     * @param s
     * @throws UnknownHostException
     */
    public App(final String s) throws UnknownHostException {
        this.user_id = s;
        
        System.out.println("Fetching data from Mongo...");
        m = new Mongo();
        db = m.getDB("trust_exchange_development");
        
        this.fb_profiles = db.getCollection("facebook_profiles");
        
        BasicDBObject query = new BasicDBObject();
        query.put("user_id", new ObjectId(this.user_id));
        
        DBCursor cursor = fb_profiles.find(query);
        
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
    
    /**
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        App app = new App("4f63c7633f033175fe000007"); //Weidong
//        App app = new App("4f63c6e23f033175fe000004");  //Wolf
//                final Config config = new ConfigBuilder().build();
//
//        final Worker worker = new WorkerImpl(config,
//                Arrays.asList("viz"), 
//                map(entry("com.socialislands.viz.VizWorker", App.class)));
//
//        
//        final Thread t = new Thread(worker);
//        t.start();
        //Thread.yield();
        //worker.end(false);
//        t.join();
    }

}
