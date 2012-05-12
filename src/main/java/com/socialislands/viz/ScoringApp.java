package com.socialislands.viz;

import com.mongodb.*;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Arrays;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

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

class PhotoActionStat{
    public int numPhotoTagged;
    public int numActions;
    public String actionName;
    public Map actionMap;
    public Map actionNameMap;
    void photoStat(){
        numPhotoTagged =0;
        numActions = 0;
        actionMap = new HashMap<Long, Integer>();
        actionNameMap = new HashMap<Long, Integer>();
    }
    public void show(){
        System.out.println("Action: "+ actionName);
        System.out.println("numPhotoTagged: " + numPhotoTagged + " total "+ actionName +": " + numActions + " independant actors: " +actionMap.size());
        
    }
}

public class ScoringApp extends App
{
    private int numNodes;
    private int numEdges;
    private double graphDensity;
    double averageClusteringCoefficient;
    private int kCore;
    private int kCoreSize;
    private double clusteringCoefficientMean;
    private double clusteringCoefficientLower;
    private double clusteringCoefficientUpper;
    private double kCoreMean;
    private double kCoreLower;
    private double kCoreUpper;
    
    
    int numPhotoTagged=0;
    int numPhotoLike=0;
    int numPhotoComment=0;
    int numFriendsInYourPhoto=0;
    int numLikesInYourPhoto = 0;
//    DBObject user;

    
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
        friendHash = new HashMap<Long, Integer>();
        
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
        if(edges.size()>0)
            System.out.println(edges.toArray()[0].getClass().getName());
        
        itr = edges.iterator(); 
        BasicDBObject edge = new BasicDBObject();
        while(itr.hasNext()) {

            edge = (BasicDBObject) itr.next(); 
            long node1 = Long.valueOf(edge.get("uid1").toString());
            long node2 = Long.valueOf(edge.get("uid2").toString());
            
            if (node2 > node1){ // skip symmetrical edges
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
        
        clusteringCoefficientMean=MeanClusteringCoefficientTable.interp(numNodes);
        clusteringCoefficientLower=LowerClusteringCoefficientTable.interp(numNodes);
        clusteringCoefficientUpper=UpperClusteringCoefficientTable.interp(numNodes);
        kCoreMean=MeanKCoreTable.interp(numNodes);
        kCoreLower=LowerKCoreTable.interp(numNodes);
        kCoreUpper=UpperKCoreTable.interp(numNodes);
        
        
        System.out.println("======Average Clustering Coefficient: " + averageClusteringCoefficient);
        System.out.println("lower CC " + clusteringCoefficientLower);
        System.out.println("mean CC " + clusteringCoefficientMean);
        System.out.println("upper CC " + clusteringCoefficientUpper);
        
        System.out.println("======kCore: "+ kCore + " subgraph size: "+kCoreSize);
        System.out.println("lower KC " + kCoreLower);
        System.out.println("mean KC " + kCoreMean);
        System.out.println("upper KC " + kCoreUpper);
      
    }
    
    @Override
    protected void exportToMongo() {
        BasicDBObject mongo_query = new BasicDBObject("_id", this.fb_profile.get("_id"));
        BasicDBObject updateCmd = new BasicDBObject("$set", new BasicDBObject("k_core", kCore));
	this.fb_profiles.update(mongo_query, updateCmd);
        
        updateCmd = new BasicDBObject("$set", new BasicDBObject("k_core_size", kCoreSize));
	this.fb_profiles.update(mongo_query, updateCmd);
        
        updateCmd = new BasicDBObject("$set", new BasicDBObject("graph_density", graphDensity));
	this.fb_profiles.update(mongo_query, updateCmd);
        
        updateCmd = new BasicDBObject("$set", new BasicDBObject("degree", numNodes));
	this.fb_profiles.update(mongo_query, updateCmd);
        
        updateCmd = new BasicDBObject("$set", new BasicDBObject("average_clustering_coefficient", averageClusteringCoefficient));
	this.fb_profiles.update(mongo_query, updateCmd);
        
        updateCmd = new BasicDBObject("$set", new BasicDBObject("clustering_coefficient_mean", clusteringCoefficientMean));
	this.fb_profiles.update(mongo_query, updateCmd);
        
        updateCmd = new BasicDBObject("$set", new BasicDBObject("clustering_coefficient_lower", clusteringCoefficientLower));
	this.fb_profiles.update(mongo_query, updateCmd);
        
        updateCmd = new BasicDBObject("$set", new BasicDBObject("clustering_coefficient_upper", clusteringCoefficientUpper));
	this.fb_profiles.update(mongo_query, updateCmd);
        
        updateCmd = new BasicDBObject("$set", new BasicDBObject("k_core_mean", kCoreMean));
	this.fb_profiles.update(mongo_query, updateCmd);
        
        updateCmd = new BasicDBObject("$set", new BasicDBObject("k_core_lower", kCoreLower));
	this.fb_profiles.update(mongo_query, updateCmd);
        
        updateCmd = new BasicDBObject("$set", new BasicDBObject("k_core_upper", kCoreUpper));
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
    private void testScoring() throws UnknownHostException{
        getPhotosStat();

    }
      
    private void getPhotosStat(){
        System.out.println("Tagged photos stats for: "+ userName);
        PhotoActionStat tagStat = getPhotosActionStat("tags");
        tagStat.show();
        
        PhotoActionStat likeStat = getPhotosActionStat("likes");
        likeStat.show();
        
        PhotoActionStat commentStat = getPhotosActionStat("comments");
        commentStat.show();
        
    }
    
    public PhotoActionStat getPhotosActionStat(String actionName){
        Map tagsMap = new HashMap<Long, Integer>();
        Map tagsNameMap = new HashMap<Long, String>();
 
        BasicDBList photos = (BasicDBList) fb_profile.get("photos"); 
        Iterator itr = photos.iterator(); 
        BasicDBObject photo = new BasicDBObject();
        int idx = 0;
        while(itr.hasNext()) {
            photo = (BasicDBObject) itr.next(); 
//            System.out.println(idx);
//            System.out.println("photo: " + photo.toString());
//            //retrieving tags
            BasicDBObject tagsObj = (BasicDBObject) photo.get(actionName);
            if(tagsObj!=null){
                BasicDBList tags = (BasicDBList) tagsObj.get("data");
//                System.out.println("tags: " + tags);
                Iterator tagsItr = tags.iterator();
                while(tagsItr.hasNext()){
                    BasicDBObject tag = (BasicDBObject) tagsItr.next();
                    String stringId;
                    if(actionName.equals("comments")){
                        BasicDBObject tagName = (BasicDBObject) tag.get("from");
                        stringId = (String) tagName.get("id");
                    }
                    else{
                        stringId = (String) tag.get("id");
                    }
                    if(stringId!=null){
//                        System.out.println("tagged by: "+stringId);
                        long id = Long.valueOf(stringId);
                        if(tagsMap.containsKey(id)){
                            Integer val =(Integer) tagsMap.get(id);
                            tagsMap.put(id, val+1);
                        }else{
                            tagsMap.put(id, 1);
                            tagsNameMap.put(id, (String) tag.get("name"));
                        }
                    }
                }
            }
            idx++;
        }
        
        int sum = 0;
        itr = tagsMap.entrySet().iterator();
        while(itr.hasNext()){
            Map.Entry pairs = (Map.Entry)itr.next();
            sum += (Integer) pairs.getValue();
        }
        if(actionName.equals("tags"))
            sum -= numPhotoTagged;
          
        PhotoActionStat photoActionStat = new PhotoActionStat();
        photoActionStat.numPhotoTagged = idx;
        photoActionStat.numActions = sum;
        photoActionStat.actionMap = tagsMap;
        photoActionStat.actionNameMap = tagsNameMap;
        photoActionStat.actionName = actionName;
        
        return photoActionStat;
    }
    
    public void runLocalTest() {
        try{
            testScoring();
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        
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

class MyTable{
    
    public MyTable(){   
    }
    
    public static double[] interpLinear(double[] x, double[] y, double[] xi) throws IllegalArgumentException {
        //added by Weidong Yang, to deal with lower and upper boundary limitations.
        for (int i = 0; i < xi.length; i++) {
            if (xi[i] > x[x.length - 1]) {
                 xi[i] = x[x.length -1];
            }else if(xi[i] < x[0]) {
                xi[i]=x[0];
            }
        }
           
        
        if (x.length != y.length) {
            throw new IllegalArgumentException("X and Y must be the same length");
        }
        if (x.length == 1) {
            throw new IllegalArgumentException("X must contain more than one value");
        }
        double[] dx = new double[x.length - 1];
        double[] dy = new double[x.length - 1];
        double[] slope = new double[x.length - 1];
        double[] intercept = new double[x.length - 1];

        // Calculate the line equation (i.e. slope and intercept) between each point
        for (int i = 0; i < x.length - 1; i++) {
            dx[i] = x[i + 1] - x[i];
            if (dx[i] == 0) {
                throw new IllegalArgumentException("X must be montotonic. A duplicate " + "x-value was found");
            }
            if (dx[i] < 0) {
                throw new IllegalArgumentException("X must be sorted");
            }
            dy[i] = y[i + 1] - y[i];
            slope[i] = dy[i] / dx[i];
            intercept[i] = y[i] - x[i] * slope[i];
        }

        // Perform the interpolation here
        double[] yi = new double[xi.length];
        for (int i = 0; i < xi.length; i++) {
            if ((xi[i] > x[x.length - 1]) || (xi[i] < x[0])) {
                yi[i] = Double.NaN;
            }
            else {
                int loc = Arrays.binarySearch(x, xi[i]);
                if (loc < -1) {
                    loc = -loc - 2;
                    yi[i] = slope[loc] * xi[i] + intercept[loc];
                }
                else {
                    yi[i] = y[loc];
                }
            }
        }

        return yi;
    }

}

class MeanClusteringCoefficientTable extends MyTable{

    static double X[]={117,185,245,282,317,346,374,395,430,461,476,496,512,528,547,568,572,574,576};
    static double Y[]={94,117,142,162,181,195,206,211,219,229,239,256,273,292,319,346,356,378,401};
    static double degree[]={1.950672,6.239871,17.408297,32.774119,59.628388,97.907746,158.035512,226.312174,411.746543,699.594894,904.152721,1272.824941,1673.363082,2199.944324,3044.473716,4359.788851,4668.429805,4830.849653,4998.920267};
    static double CC[]={0.51763945,0.38971174,0.28624589,0.223633,0.17688568,0.14881623,0.12992372,0.12214835,0.11066397,0.0978148,0.08645754,0.07009387,0.05682732,0.04494837,0.0322099,0.02308154,0.02040155,0.01555032,0.01170727};
    static double logDegree[]={0.6681739,1.8309596,2.8569469,3.4896391,4.0881318,4.5840257,5.0628198,5.4219153,6.020408,6.5505014,6.8069983,7.1489941,7.4225907,7.6961873,8.0210833,8.3801789,8.4485781,8.4827776,8.5169772};
    static double logCC[]={-0.6584763,-0.9423479,-1.2509041,-1.497749,-1.7322516,-1.9050431,-2.0408078,-2.102519,-2.2012569,-2.3246794,-2.4481018,-2.65792,-2.8677382,-3.1022408,-3.4354814,-3.7687221,-3.8921445,-4.1636739,-4.4475455};

    public static double interp(double x){
        double[] xlist = new double[1];
        xlist[0] = Math.log(x);
        double[] val = interpLinear(logDegree, logCC, xlist);
        return Math.exp(val[0]);
    }
    public MeanClusteringCoefficientTable(){
    }
}

class LowerClusteringCoefficientTable extends MyTable{

    static double X[]={210,251,305,348,387,440,463,480,502,515};
    static double Y[]={306,287,288,297,308,330,346,366,394,421};
    static double degree[]={9.568288,19.289206,48.566515,101.314065,197.377281,488.532436,723.934575,968.160077,1410.349476,1761.445522};
    static double CC[]={0.03781565,0.047809563,0.047223112,0.042258403,0.036893616,0.02812079,0.023081541,0.018032728,0.012763703,0.009146441};
    static double logDegree[]={2.258454,2.959546,3.882934,4.618225,5.285117,6.191406,6.584701,6.875397,7.251593,7.47389};
    static double logCC[]={-3.275032,-3.04053,-3.052872,-3.163952,-3.299717,-3.571246,-3.768722,-4.015567,-4.36115,-4.69439};

    public static double interp(double x){
        double[] xlist = new double[1];
        xlist[0] = Math.log(x);
        double[] val = interpLinear(logDegree, logCC, xlist);
        return Math.exp(val[0]);
    }
    public LowerClusteringCoefficientTable(){
    }
}

class UpperClusteringCoefficientTable extends MyTable{

    
    static double X[]={118,157,186,201,229,254,278,308,341,382,416,448,474,492,510,526,548,562,572,574,576};
    static double Y[]={41,41,42,51,62,76,93,109,125,139,146,156,168,184,204,224,244,268,277,298,326};
    static double degree[]={1.984315,3.865788,6.347489,8.203462,13.241427,20.304551,30.607344,51.122958,89.884609,181.203024,324.086651,560.149763,873.753855,1188.675469,1617.102301,2125.979153,3096.98123,3934.661642,4668.429805,4830.849653,4998.920267};
    static double CC[]={0.99567207,0.99567207,0.98345876,0.88006477,0.76833882,0.64641349,0.52406788,0.43015486,0.35307107,0.29704331,0.27245739,0.24082241,0.20767053,0.17045595,0.13317073,0.10404121,0.08128343,0.06044467,0.05408994,0.04174005,0.02954392};
    static double logDegree[]={0.6852737,1.3521655,1.8480594,2.1045562,2.5833503,3.010845,3.42124,3.9342337,4.4985267,5.1996181,5.7810109,6.3282042,6.7727987,7.0805949,7.3883911,7.6619878,8.0381831,8.2775802,8.4485781,8.4827776,8.5169772};
    static double logCC[]={-0.004337327,-0.004337327,-0.016679572,-0.127759777,-0.263524472,-0.436315902,-0.646134067,-0.843609987,-1.041085907,-1.213877337,-1.300273052,-1.423695502,-1.571802442,-1.769278362,-2.016123262,-2.262968162,-2.509813062,-2.806026942,-2.917107147,-3.176294292,-3.521877152};

    public static double interp(double x){
        double[] xlist = new double[1];
        xlist[0] = Math.log(x);
        double[] val = interpLinear(logDegree, logCC, xlist);
        return Math.exp(val[0]);
    }
    public UpperClusteringCoefficientTable(){
    }
}


class MeanKCoreTable extends MyTable{

    static double X[]={734,758,781,805,829,854,880,913,946,979,1009,1037,1064,1086,1116,1140,1168,1188};
    static double Y[]={400,384,362,338,313,294,276,257,238,218,197,178,159,146,135,129,121,115};
    static double degree[]={1.98395,2.989028,4.427031,6.669784,10.048725,15.400215,24.008181,42.180155,74.106634,130.198505,217.322657,350.566772,555.929676,809.442118,1351.091644,2035.560505,3283.596305,4620.430388};
    static double CC[]={0.5176533,0.7086363,1.0913196,1.7479484,2.8551522,4.1455844,5.9022587,8.5698801,12.4431762,18.4251817,27.8237932,40.3992071,58.6582829,75.7079856,93.9519899,105.6939532,123.6637335,139.1190212};
    static double logDegree[]={0.6850899,1.0949482,1.4877291,1.8975874,2.3074457,2.7343815,3.1783947,3.7419499,4.3055051,4.8690602,5.3813831,5.8595512,6.3206418,6.6963453,7.2086682,7.6185265,8.0966945,8.4382431};
    static double logCC[]={-0.65844965,-0.34441291,0.08738762,0.55844273,1.04912514,1.42204377,1.77533511,2.14825374,2.52117237,2.9137183,3.32589153,3.69881016,4.07172879,4.32688364,4.54278391,4.66054768,4.81756606,4.93532983};

    public static double interp(double x){
        double[] xlist = new double[1];
        xlist[0] = Math.log(x);
        double[] val = interpLinear(logDegree, logCC, xlist);
        return Math.exp(val[0]);
    }
    public MeanKCoreTable(){
    }
}


class LowerKCoreTable extends MyTable{

    static double X[]={806,808,868,874,899,902,918,923,937,957,982,1008,1035,1064,1092,1120,1138,1162,1183};
    static double Y[]={424,366,367,333,331,311,310,297,292,277,264,248,228,213,199,192,192,193,194};
    static double degree[]={6.784664,7.020397,19.559586,21.670028,33.210492,34.95628,45.940037,50.03507,63.548804,89.42111,137.042697,213.642855,338.795382,555.929676,896.779352,1446.6096,1967.210114,2963.807252,4242.279352};
    static double CC[]={0.3231933,1.008918,0.9893087,1.9281851,2.0053805,2.9694589,3.028317,3.9085321,4.3115539,5.7875428,7.4697585,10.2256514,15.1415911,20.3250636,26.752741,30.6927972,30.6927972,30.096254,29.5113052};
    static double logDegree[]={1.914665,1.94882,2.973466,3.07593,3.502866,3.554098,3.827337,3.912724,4.151808,4.493357,4.920293,5.364306,5.825396,6.320642,6.79881,7.276978,7.584372,7.99423,8.352856};
    static double logCC[]={-1.129504764,0.008878429,-0.010748867,0.656579212,0.695833805,1.088379733,1.10800703,1.363161883,1.461298366,1.755707812,2.010862666,2.324899409,2.717445337,3.011854784,3.286636934,3.424028009,3.424028009,3.404400712,3.384773416};

    public static double interp(double x){
        double[] xlist = new double[1];
        xlist[0] = Math.log(x);
        double[] val = interpLinear(logDegree, logCC, xlist);
        return Math.exp(val[0]);
    }
    public LowerKCoreTable(){
    }
}


class UpperKCoreTable extends MyTable{

    static double X[]={734,758,777,795,823,848,876,907,938,972,1004,1029,1058,1086,1122,1150,1169,1184};
    static double Y[]={366,367,328,304,280,259,244,225,206,186,164,145,127,112,99,90,83,79};
    static double degree[]={1.98395,2.989028,4.134719,5.622706,9.070081,13.90039,22.422949,38.072235,64.643374,115.528614,199.536265,305.80014,501.787751,809.442118,1496.871812,2414.628668,3340.153241,4315.348723};
    static double CC[]={1.008918,0.9893087,2.1270067,3.4067911,5.4566002,8.2399901,11.060814,16.059928,23.3184725,34.5287319,53.1752099,77.2086071,109.9254355,147.5565841,190.4455977,227.2412538,260.708603,282.0015326};
    static double logDegree[]={0.6850899,1.0949482,1.4194194,1.7268131,2.2049812,2.6319169,3.1100849,3.6394853,4.1688856,4.7495182,5.295996,5.7229318,6.2181772,6.6963453,7.3111328,7.7893008,8.113772,8.3699334};
    static double logCC[]={0.008878429,-0.010748867,0.754715694,1.225770808,1.696825923,2.108999148,2.403408594,2.776327227,3.149245859,3.541791787,3.973592309,4.346510941,4.699802277,4.994211724,5.249366577,5.426012245,5.56340332,5.641912506};

    public static double interp(double x){
        double[] xlist = new double[1];
        xlist[0] = Math.log(x);
        double[] val = interpLinear(logDegree, logCC, xlist);
        return Math.exp(val[0]);
    }
    public UpperKCoreTable(){
    }
}
