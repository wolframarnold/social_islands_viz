/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.socialislands.viz;

/**
 *
 * @author wolfram
 */
public class StandAlone {

    // run this from the command line like this:
    // sh target/bin/stand_alone "score" "4003453098098"
    // - the first argument is "socre" or "viz" for the type of job
    // - the second is the user_id from MongoDB

    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.out.println("Incorrect number of arguments!");
            System.out.println("Usage: stand_alone <job_type> <facebook_profile_id> <postback_url>");
            System.out.println("where: <job_type> is one of: viz, score");
            System.out.println("and:   <facebook_profile_id>  is the MongoDB ID of the document in the facebook_profiles collection");
            System.out.println("and:   <postback_url>  is the URL to notify upon completion of the computation");
        } else {
            String url = "";
            if (args.length == 3) url = args[2];
            if ("viz".equals(args[0])) {
                System.out.println("Running Viz app for profile: " + args[1]+ ", postback URL: " + url);
                VizApp app = new VizApp(args[1], url);
                app.run();
            } else if ("score".equals(args[0])) {
                System.out.println("Running Scoring app for profile: " + args[1]+ ", postback URL: " + url);
                ScoringApp app = new ScoringApp(args[1], url);
                app.run();
            } else {
                System.out.println(args[0]+" is an unknown job type. Use one of: viz, score");
            }
        }
            
    }
}
