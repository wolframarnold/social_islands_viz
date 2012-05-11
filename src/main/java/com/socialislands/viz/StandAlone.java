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
    //          VizApp vizapp = new VizApp("4fa7509700d7ed0007000001");  //Pieter Dubois  Has one friend
    //          VizApp vizapp = new VizApp("4fa847368578760007000001");  
    //            vizapp.run();
    //            ScoringApp scoringapp = new ScoringApp("4f63c6e23f033175fe000004"); //Wolf
    //        ScoringApp scoringapp = new ScoringApp("4fa8648a8578760007000002");
    //          scoringapp.run();
    //            runBatchScoring();
        testScoring();
    }

    private static void testScoring() throws UnknownHostException{
        Map tagsMap;
        Map tagsNameMap;
    
        DBCollection users;
        DBObject user;

        String mongo_url = (new YamlConfig("mongo.yml")).propertiesForCurrentEnv().getProperty("uri");
        MongoURI mongoURI = new MongoURI(mongo_url);
        DB dbtmp = mongoURI.connectDB();

        users = dbtmp.getCollection("users");
        tagsMap = new HashMap<Long, Integer>();
        tagsNameMap = new HashMap<Long, String>();
        
        

        DBCollection facebookProfiles;
        facebookProfiles = dbtmp.getCollection("facebook_profiles");

        BasicDBObject query = new BasicDBObject();
        query.put("name", "Weidong Yang");
        user = facebookProfiles.findOne(query);

        String email = user.get("email").toString();
        System.out.println(email);

        BasicDBList photos = (BasicDBList) user.get("photos"); 
        Iterator itr = photos.iterator(); 
        BasicDBObject photo = new BasicDBObject();
        int idx = 0;
        while(itr.hasNext()) {
            photo = (BasicDBObject) itr.next(); 
            System.out.println(idx);
            System.out.println("photo: " + photo.toString());
            //System.out.println(photo.get("tags"));  //likes, tags, comments
            BasicDBObject tagsObj = (BasicDBObject) photo.get("tags");
            if(tagsObj!=null){
                BasicDBList tags = (BasicDBList) tagsObj.get("data");
                System.out.println("tags: " + tags);
                Iterator tagsItr = tags.iterator();
                while(tagsItr.hasNext()){
                    BasicDBObject tag = (BasicDBObject) tagsItr.next();
                    String stringId = (String) tag.get("id");
                    if(stringId!=null){
                        System.out.println("tagged by: "+stringId);
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
        
        int numPhoto = idx;
        System.out.println(tagsMap);
        System.out.println(tagsNameMap);
        System.out.println(tagsMap.values());
    
        int sum = 0;
        itr = tagsMap.entrySet().iterator();
        while(itr.hasNext()){
            Map.Entry pairs = (Map.Entry)itr.next();
            sum += (Integer) pairs.getValue();
        }
        sum -= numPhoto;
        System.out.println("Total tags: "+ sum);
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
