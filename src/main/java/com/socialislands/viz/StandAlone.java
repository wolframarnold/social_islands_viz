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

        if (args.length != 2) {
            System.out.println("Incorrect number of arguments!");
            System.out.println("Usage: stand_alone <job_type> <user_id>");
            System.out.println("where: <job_type> is one of: viz, score");
            System.out.println("and:   <user_id>  is the MongoDB user_id");
        } else {
            if ("viz".equals(args[0])) {
                VizApp app = new VizApp(args[1]);
            } else if ("score".equals(args[0])) {
                ScoringApp app = new ScoringApp(args[1]);
            } else {
                System.out.println(args[0]+" is an unknown job type. Use one of: viz, score");
            }
        }
            
    }
}
