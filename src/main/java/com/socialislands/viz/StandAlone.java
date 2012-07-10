/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.socialislands.viz;

import com.mongodb.*;
import com.mongodb.BasicDBList;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bson.types.ObjectId;
import org.gephi.graph.api.Node;

/**
 *
 * @author wolfram
 */
public class StandAlone {

        
    
    public static void main(String[] args) throws Exception {

    //        VizApp vizapp = new VizApp("4f63c7633f033175fe000007"); //Weidong
    //        VizApp vizapp = new VizApp("4f567b699881b47c29000001");  //Wolf
    //          VizApp vizapp = new VizApp("4fa7180e5a8513000a000003");  //Janet      Has friend in edge list but not on node list
//              VizApp vizapp = new VizApp("4fdd6ddba074560007000005"); //Julia
              VizApp vizapp = new VizApp("4fdc1d8915b39f0007000005"); //Laura
              
              
    //          VizApp vizapp = new VizApp("4fa847368578760007000001");  
                vizapp.run();
    //            ScoringApp scoringapp = new ScoringApp("4f63c6e23f033175fe000004"); //Wolf
//            ScoringApp scoringapp = new ScoringApp("4f91c5d4a3f1ec0003000001");
//              scoringapp.runLocalTest();
    //            runBatchScoring();
//        testScoring();
    }

  
    private static void runBatchScoring() throws UnknownHostException{
            DBCollection users;
            DBObject user;
            
            String mongo_url = (new YamlConfig("mongo.yml")).propertiesForCurrentEnv().getProperty("uri");
            MongoURI mongoURI = new MongoURI(mongo_url);
            DB dbtmp = mongoURI.connectDB();
   
            users = dbtmp.getCollection("users");
        
            DBCursor cursor = users.find();
            while(cursor.hasNext()){
                user = cursor.next();
                String userId = user.get("_id").toString();
                String userName = user.get("name").toString();
                System.out.println(userId+" "+userName);
                if(!(userName.equals("Weidong Yang"))){
                    ScoringApp scoringapp = new ScoringApp(userId); //Weidong
                    scoringapp.run();
                }
            }
//            BasicDBObject query = new BasicDBObject();
//            query.put("user_id", new ObjectId(this.user_id));
//        
//            DBCursor cursor = fb_profiles.find(query);
//        
//        try {
//            this.fb_profile = cursor.next();
//            this.userName = this.fb_profile.get("name").toString();
//            this.userUid = Long.valueOf(this.fb_profile.get("uid").toString());
//        }
//        catch(java.util.NoSuchElementException e) {
//            System.out.println("Could not find record in facebook_profiles or users collection with user_id: "+this.user_id);
//        }
            
            //BasicDBList friends = (BasicDBList) this.fb_profile.get("friends");
            //BasicDBList friends = (BasicDBList) this.fb_profile.get("friends");
        }
}
