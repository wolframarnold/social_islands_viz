/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.socialislands.viz;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ho.yaml.Yaml;

/**
 *
 * @author wolfram
 */
public class YamlConfig {

    private String config_file;
    
    // Pass in the config file, e.g. "mongo.yml" which will be looked for in the config folder
    public YamlConfig(final String cfg_file)  {
        this.config_file = cfg_file;
    }
    
    // retrieve a System Property of Environment Variable (in that order of precedence)
    // setting it to default value if not found
    static String getSystemPropertyOrEnvironmentVariable(String var, String def) {
        String env = System.getProperty(var);
        if (env==null) env = System.getenv(var);
        if (env==null) env = def;
        return env;
    }
    
    static String getSystemPropertyOrEnvironmentVariable(String var) {
        return getSystemPropertyOrEnvironmentVariable(var,null);
    }
    

    static String getAppEnv() {
        return getSystemPropertyOrEnvironmentVariable("APP_ENV", "development");
    }
    
    public Properties propertiesForCurrentEnv() {
        try {
            Properties props = new Properties();

            HashMap yamlConfig = (HashMap) Yaml.load(new File("config/"+this.config_file));

            HashMap propertiesFromYamlForEnvironment = (HashMap) yamlConfig.get(YamlConfig.getAppEnv());
            
            Iterator it = propertiesFromYamlForEnvironment.entrySet().iterator();
            // matches string expansion like {{ MONGOHQ_URL }} with MONGOHQ_URL in capture group
            Pattern expansionPattern = Pattern.compile("\\{\\{ *(\\w+) *\\}\\}");
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                String key = (String)pair.getKey();
                String val = (String)pair.getValue();
                Matcher m = expansionPattern.matcher(val);
                if (m.matches()) {
                    // got a string expansion to look up
                    val = YamlConfig.getSystemPropertyOrEnvironmentVariable(m.group(1));
                }
                System.out.println(config_file + ": settings for environment: " + key + " = " + val);
                props.setProperty(key,val);
            }            
            return props;

        } catch (FileNotFoundException e) {
            System.out.println("ERROR:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("ERROR:" + e.getMessage());
        }
        return null;
    }
    
}
