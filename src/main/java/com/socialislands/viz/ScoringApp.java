package com.socialislands.viz;

import com.mongodb.*;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Arrays;

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
        
//        MeanClusteringCoefficientTable mCCTable= new MeanClusteringCoefficientTable();
        double xi= 20;
        System.out.println(xi + " interp to " + MeanClusteringCoefficientTable.interp(xi));
        System.out.println(xi + " interp to " + LowerClusteringCoefficientTable.interp(xi));
        System.out.println(xi + " interp to " + UpperClusteringCoefficientTable.interp(xi));
        
        
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
