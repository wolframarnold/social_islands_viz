package com.socialislands.viz;

import com.mongodb.*;

import java.awt.Color;
import java.io.*;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.Query;
import org.gephi.filters.api.Range;
import org.gephi.filters.plugin.graph.DegreeRangeBuilder.DegreeRangeFilter;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.exporter.spi.GraphExporter;
import org.gephi.io.exporter.preview.PNGExporter;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2Builder;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.types.EdgeColor;
import org.gephi.ranking.api.*;
import org.gephi.ranking.plugin.transformer.*;
import org.gephi.partition.api.*;
import org.gephi.partition.plugin.NodeColorTransformer;
import org.gephi.statistics.plugin.*;

import org.openide.util.Lookup;

import org.gephi.io.exporter.spi.CharacterExporter;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import org.gephi.filters.plugin.graph.KCoreBuilder;

/**
 *
 * @author weidongyang
 */
public class VizApp extends App {

    private ArrayList<BasicDBObject> labels_for_mongo = new ArrayList<BasicDBObject>();
    private DBCollection fb_graph_collection;
    
    private float pngScaling = (float)1.0;

    @Override
    protected void generateResult() {
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        PreviewModel model = Lookup.getDefault().lookup(PreviewController.class).getModel();
        FilterController filterController = Lookup.getDefault().lookup(FilterController.class);
        RankingController rankingController = Lookup.getDefault().lookup(RankingController.class);
        int totalNodes = undirectedGraph.getNodeCount();
        System.out.println("inside genGraph, Nodes: " + totalNodes);
        System.out.println("Edges: " + undirectedGraph.getEdgeCount());

        // get degree distribution
        Degree degree = new Degree();
        degree.execute(graphModel, attributeModel);
        System.out.println("average degree " + degree.getAverageDegree());
        AttributeColumn degreeColumn = attributeModel.getNodeTable().getColumn("Degree");

        int[] degreeArray;
        degreeArray = new int[undirectedGraph.getNodeCount()];
        int i1 = 0;
        for (Node n : graphModel.getGraph().getNodes()) {
            int val = Integer.valueOf(n.getNodeData().getAttributes().getValue(Ranking.DEGREE_RANKING).toString());
            degreeArray[i1++] = val;
//            System.out.print(val  + " ");
        }
        reportStatistics(degreeArray);

        //Filter      
        //This is for preventing users with too few friends not being able to get any graph back.
        //The larger the graph, the higher we filter the graph, so the image is not too cluttered.
        int degreeFilterRange = 0;
        if (totalNodes > 1000) {
            degreeFilterRange = 6;
        } else if (totalNodes > 800) {
            degreeFilterRange = 5;
        } else if (totalNodes > 600) {
            degreeFilterRange = 4;
        } else if (totalNodes > 400) {
            degreeFilterRange = 3;
        } else if (totalNodes > 200) {
            degreeFilterRange = 2;
        } else if (totalNodes > 100) {
            degreeFilterRange = 1;
        } else if (totalNodes > 50) {
            degreeFilterRange = 0;
        }

        System.out.println("total node:" + totalNodes + " filter by degree of:" + degreeFilterRange);
        System.out.println("Filter by degree...");
        DegreeRangeFilter degreeFilter = new DegreeRangeFilter();
        degreeFilter.init(undirectedGraph);
        degreeFilter.setRange(new Range(degreeFilterRange, Integer.MAX_VALUE));     //Remove nodes with degree < 30
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
        for (int i = 0; i < 250 && fa2Layout.canAlgo(); i++) {
            fa2Layout.goAlgo();
            cnt++;
        }
        System.out.println("Layout done, num itr is: " + cnt + "    ...");
        fa2Layout.endAlgo();


        //Get Centrality
        GraphDistance distance = new GraphDistance();
        distance.setDirected(false);
        distance.execute(graphModel, attributeModel);


        //Rank size by centrality
        System.out.println("Rank size by centrality...");
        AttributeColumn centralityColumn = attributeModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS);



        double maxVal = 0;
        double secondMax = 0;
        double thirdMax = 0;
        double extremVal = 0;
        Node nMax = null;
        Node nSecond = null;

        int numNode = graphModel.getGraph().getNodeCount();
        //only does it for graph with more than 20 nodes, reduce the size of the largest node - ego, to make the whole graph more balanced.
        if (numNode > 20) {
            for (Node n : graphModel.getGraph().getNodes()) {
                double val = Double.valueOf(n.getNodeData().getAttributes().getValue(GraphDistance.BETWEENNESS).toString());
                //            System.out.print(val  + " ");
                if (val > maxVal) {
                    thirdMax = secondMax;
                    secondMax = maxVal;
                    nSecond = nMax;
                    maxVal = val;
                    nMax = n;

                } else if (val > secondMax) {
                    thirdMax = secondMax;
                    secondMax = val;
                    nSecond = n;
                } else if (val > thirdMax) {
                    thirdMax = val;
                }
            }
            System.out.println(numNode + "nodes, max: " + maxVal + " second: " + secondMax + " third: " + thirdMax);

            nMax.getNodeData().getAttributes().setValue(GraphDistance.BETWEENNESS, thirdMax * 1.5);
            nSecond.getNodeData().getAttributes().setValue(GraphDistance.BETWEENNESS, thirdMax * 1.2);
        }
//        int valCnt = 0;
//        for (Node n : graphModel.getGraph().getNodes()){
//            double val = Double.valueOf(n.getNodeData().getAttributes().getValue(GraphDistance.BETWEENNESS).toString());
////            System.out.print(val  + " ");
//            if (val > maxVal){
//                n.getNodeData().getAttributes().setValue(GraphDistance.BETWEENNESS, maxVal);
//                valCnt ++;
//            }
//        }

//        System.out.println("numExtrem " + valCnt +" val " + extremVal);



        Ranking centralityRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, centralityColumn.getId());

        AbstractSizeTransformer sizeTransformer = (AbstractSizeTransformer) rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_SIZE);
        sizeTransformer.setMinSize(6);
        sizeTransformer.setMaxSize(30);
//        rankingController.setInterpolator(Interpolator.newBezierInterpolator(new Float(0.01), new Float(1.0), new Float(0.0), new Float(1.0)));
        rankingController.transform(centralityRanking, sizeTransformer);

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
//        modularity.setRandom(false);
        modularity.execute(graphModel, attributeModel);

        //Partition with 'modularity_class', just created by Modularity algorithm
        AttributeColumn modColumn = attributeModel.getNodeTable().getColumn(Modularity.MODULARITY_CLASS);
        Partition p2 = partitionController.buildPartition(modColumn, undirectedGraph);
        System.out.println(p2.getPartsCount() + " partitions found");

        // Improve colors -- prefer some neon colors that shine on black background
        NodeColorTransformer nodeColorTransformer2 = new NodeColorTransformer();
        Map<Object, Color> color_map = nodeColorTransformer2.getMap();
        setColorsAndSetLabelsForMongo(p2, color_map);

        partitionController.transform(p2, nodeColorTransformer2);
    }

    private void setColorsAndSetLabelsForMongo(Partition p2, Map<Object, Color> color_map) throws MongoException {
        // build up linear array for colors to store in MongoDB
        for (Part p : p2.getParts()) {
            int i = (Integer) p.getValue();
            Color color = Palette.colors[i];
            color_map.put(i, color);

            BasicDBObject color_hash = new BasicDBObject();
            color_hash.put("r", color.getRed());
            color_hash.put("g", color.getGreen());
            color_hash.put("b", color.getBlue());
            BasicDBObject label_for_mongo = new BasicDBObject();
            label_for_mongo.put("name", "Label me #" + (i + 1));
            label_for_mongo.put("group_index", i);
            label_for_mongo.put("color", color_hash);
            this.labels_for_mongo.add(label_for_mongo);
        }

        //        nodeColorTransformer2.randomizeColors(p2);
        //        printMap(nodeColorTransformer2.getMap());
    }

    // TODO: These stats should be written to a stats table instead of the FB Profile table
    private void reportStatistics(int[] arr) {
        DescriptiveStatistics stat = new DescriptiveStatistics();
        int numItem = arr.length;

        for (int i1 = 0; i1 < numItem; i1++) {
            stat.addValue(arr[i1]);
        }

        double mean = stat.getMean();
        double std = stat.getStandardDeviation();
        double median = stat.getPercentile(50);

        System.out.println("total nodes:" + numItem + ", average degreeb: " + mean + ", stdb: " + std + "median: " + median);

        BasicDBObject query = new BasicDBObject("_id", this.facebook_profile_id);
        BasicDBObject updateCmd = new BasicDBObject("$set", new BasicDBObject("degree_average", mean));
        this.fb_profiles_collection.update(query, updateCmd);
        updateCmd = new BasicDBObject("$set", new BasicDBObject("degree_stddev", std));
        this.fb_profiles_collection.update(query, updateCmd);
        updateCmd = new BasicDBObject("$set", new BasicDBObject("degree_median", median));
        this.fb_profiles_collection.update(query, updateCmd);
        updateCmd = new BasicDBObject("$set", new BasicDBObject("histogram_num_connections", arr));
        this.fb_profiles_collection.update(query, updateCmd);
    }

    @Override
    protected void exportToMongo() {
        //Export only visible graph -- to string buffer, then to Mongo as "graph" attribute on Facebook profile record.

        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        GraphExporter exporter = (GraphExporter) ec.getExporter("gexf");     //Get GEXF exporter
        exporter.setExportVisible(true);  //Only exports the visible (filtered) graph
        exporter.setWorkspace(workspace);
        StringWriter stringWriter = new StringWriter();
        ec.exportWriter(stringWriter, (CharacterExporter) exporter);
        String result = stringWriter.toString();
        System.out.println("Graph output string size: " + result.length());

        // Note: There is a 16MB per record limit in Mongo. With some profiles
        // we've exeeded this (e.g. David Sifry)

        BasicDBObject fields = new BasicDBObject();
        fields.put("gexf", result);
        // TODO: WARNING -- THIS WILL WIPE OUT EXISTING LABELS!!!
        fields.put("labels", labels_for_mongo);

        this.fb_graph_collection = db.getCollection("facebook_graphs");

        BasicDBObject query = new BasicDBObject("facebook_profile_id", this.facebook_profile_id);
        DBObject fb_graph = fb_graph_collection.findOne(query);

        if (fb_graph == null) {
            fields.put("facebook_profile_id", this.facebook_profile_id);
            fb_graph_collection.insert(fields);
        } else {
            BasicDBObject updateCmd = new BasicDBObject("$set", fields);
            fb_graph_collection.update(query, updateCmd);
        }

        //================testing purpose, dump gexf to a file=============
//        try {
//            BufferedWriter fos = new BufferedWriter(new FileWriter("graph.gexf"));
//            fos.write(result);
//            fos.close();
//        } catch (IOException ioe) {
//            System.out.println("IOException : " + ioe);
//        }

    }

    private void validateGraphOrientation() {
        float xmin = 100, xmax = -100, ymin = 100, ymax = -100;
        for (Node n : graphModel.getGraph().getNodes()) {
            float x = n.getNodeData().x();
            float y = n.getNodeData().y();
            if (x < xmin) {
                xmin = x;
            }
            if (x > xmax) {
                xmax = x;
            }
            if (y < ymin) {
                ymin = y;
            }
            if (y > ymax) {
                ymax = y;
            }
        }
        float xrange = xmax - xmin;
        float yrange = ymax - ymin;
        System.out.println("xmin:" + xmin + " xmax:" + xmax + " xrange:" + xrange);
        System.out.println("ymin:" + ymin + " ymax:" + ymax + " yrange:" + yrange);
        if (yrange > xrange) { //yrange should be smaller than xrange, switch coordinates
            for (Node n : graphModel.getGraph().getNodes()) {
                float x = n.getNodeData().x();
                float y = n.getNodeData().y();
                n.getNodeData().setY(x);
                n.getNodeData().setX(y);
            }
            System.out.println("===================X, Y flipped=====================");
        }

        float scale_range = yrange>xrange ? yrange:xrange;
        pngScaling = (float) 900.0/scale_range;
    }

    protected void exportToPNG() {
        //creating png file
        //1st flip vertically, to match the visual with sigma.js
        for (Node n : graphModel.getGraph().getNodes()) {
            float y = -n.getNodeData().y()*pngScaling;
            float x = n.getNodeData().x()*pngScaling;
            
            n.getNodeData().setX(x);
            n.getNodeData().setY(y);
        }

        //2nd rescale the size to match sigma.js 
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        RankingController rankingController = Lookup.getDefault().lookup(RankingController.class);
        AttributeColumn centralityColumn = attributeModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS);
        Ranking centralityRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, centralityColumn.getId());

        AbstractSizeTransformer sizeTransformer = (AbstractSizeTransformer) rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_SIZE);
        sizeTransformer.setMinSize(new Float(2));
        sizeTransformer.setMaxSize(new Float(8));
//        sizeTransformer.setMinSize(new Float(4));
//        sizeTransformer.setMaxSize(new Float(15));
        rankingController.transform(centralityRanking, sizeTransformer);

        PreviewModel model = Lookup.getDefault().lookup(PreviewController.class).getModel();
        model.getProperties().putValue(PreviewProperty.BACKGROUND_COLOR, Color.BLACK);
        model.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.FALSE);
        model.getProperties().putValue(PreviewProperty.NODE_BORDER_WIDTH, new Float(0.0f));
        model.getProperties().putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(EdgeColor.Mode.SOURCE));
        model.getProperties().putValue(PreviewProperty.EDGE_THICKNESS, new Float(0.3f));
        model.getProperties().putValue(PreviewProperty.EDGE_OPACITY, new Float(80.0f));

        model.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT, model.getProperties().getFontValue(PreviewProperty.NODE_LABEL_FONT).deriveFont(8));

        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        PNGExporter pngExporter = (PNGExporter) ec.getExporter("png");
//        pngExporter.setExportVisible(true);  //Only exports the visible (filtered) graph
        pngExporter.setWorkspace(workspace);
        pngExporter.setWidth(800);
        pngExporter.setHeight(600);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ec.exportStream(baos, pngExporter);

        BasicDBObject fields = new BasicDBObject();
        fields.put("png", baos.toByteArray());

        BasicDBObject query = new BasicDBObject("facebook_profile_id", this.facebook_profile_id);
        BasicDBObject updateCmd = new BasicDBObject("$set", fields);
        this.fb_graph_collection.update(query, updateCmd);

//        byte[] png = baos.toByteArray();
//        try {
//            FileOutputStream fos = new FileOutputStream("graph.png");
//            fos.write(png);
//            fos.close();
//        } catch (FileNotFoundException ex) {
//            System.out.println("FileNotFoundException : " + ex);
//        } catch (IOException ioe) {
//            System.out.println("IOException : " + ioe);
//        }

        //done exporting to png file

    }
    // make an HTTP Post with the facebook profile id to the frontend
    // which notifies it that the graph is ready

    private void notifyFrontend() {

        if (this.postbackUrl == null || this.postbackUrl == "") {
            return;
        }

        HttpClient httpclient = new DefaultHttpClient();

        HttpPost post = new HttpPost(this.postbackUrl);

        List<NameValuePair> data = new ArrayList<NameValuePair>(1);
        data.add(new BasicNameValuePair("facebook_profile_id", this.facebook_profile_id.toString()));

        try {
            post.setEntity(new UrlEncodedFormEntity(data));
            HttpResponse response = httpclient.execute(post);
        } catch (ClientProtocolException e) {
            System.out.println("ERROR--ClientProtocolException: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("ERROR--IOException: " + e.getMessage());
        }

    }

    /**
     *
     */
    @Override
    public void run() {

        //Init a project - and therefore a workspace
        System.out.println("VizApp Run started...");
        try {
            mongoDB2Graph();
//            add2Graph("4f63c6e23f033175fe000004");
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        System.out.println("From formed graph, Nodes: " + undirectedGraph.getNodeCount() + " Edges: " + undirectedGraph.getEdgeCount());

        System.out.println("Start gen graph...");
        generateResult();

        System.out.println("Start export graph...");
        validateGraphOrientation();
        exportToMongo();
        exportToPNG();
        System.out.println("Pinging frontend");
        notifyFrontend();
        System.out.println("Done...");
    }

    /**
     *
     * @param s
     * @throws UnknownHostException
     */
    public VizApp(final String s, final String url) throws UnknownHostException {
        super(s, url);
    }
}
