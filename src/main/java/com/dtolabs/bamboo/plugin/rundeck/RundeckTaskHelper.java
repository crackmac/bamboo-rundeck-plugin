/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.dtolabs.bamboo.plugin.rundeck;

import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.build.logger.BuildLogger;

import org.rundeck.api.OptionsBuilder;


public class RundeckTaskHelper 
{

    protected static String getVariableKeyValue(String key, Map variableDefinitions) throws TaskException {

       // get a list of variable keys for rundeck info in order to connect properly to the rundeck server
       VariableDefinitionContext variableDefinitionContext =  (VariableDefinitionContext)variableDefinitions.get((Object)key);

       if (null == variableDefinitionContext) throw new TaskException("unable to get key variable: " + key);

       String returnValue = (String)variableDefinitionContext.getValue();

       if (null == returnValue) throw new TaskException("key variable:" + key + " does not have any value");

       return returnValue;
    }


    protected static Properties getVariableKeyValuesByRegex(String keyRegex, Map variableDefinitions, BuildLogger buildLogger) throws TaskException {
       
       Set variableKeySet = variableDefinitions.keySet();
       Object[] keys = variableKeySet.toArray();


       OptionsBuilder options = new OptionsBuilder();

       for (int i=0; i<keys.length; i++) {

          String planVarKey = (String)keys[i];
          buildLogger.addBuildLogEntry("checking key: " + planVarKey + " to match regex: " + keyRegex);

          if (planVarKey.matches(keyRegex)) {
             buildLogger.addBuildLogEntry("key: " + planVarKey + " key matches jobarg pattern");
             String jobArgKey=planVarKey.replaceAll(keyRegex, "$1");
             buildLogger.addBuildLogEntry("jobArgKey: " + jobArgKey + " from: " + planVarKey);
             String keyValue = getVariableKeyValue(planVarKey, variableDefinitions);
             buildLogger.addBuildLogEntry("keyValue: " + keyValue) ;
             options.addOption(jobArgKey, keyValue);
          } else {
             buildLogger.addBuildLogEntry("key: " + planVarKey + " NO key match jobarg pattern:");
          }
       }

       return options.toProperties();

    }


   public static Properties convertArgsToProperties(String args) throws TaskException {
      Properties argProperties = new Properties();


      String[] subStrings = args.split("[\\s]+");
      boolean isParam=false;
      boolean isValue=false;

      if (subStrings.length == 0) {
         return argProperties;
      }
      if (subStrings.length == 1) {
         if (null == subStrings[0] || "".equals(subStrings[0])) {
            return argProperties;
         }
      }

      for (int i=0; i<subStrings.length;) {
         System.out.println("substring: " + subStrings[i]);
         String paramString = subStrings[i];

         String param = null;
         String value = null;
         if (paramString.startsWith("-")) {
            param = paramString.substring(1);
         } else {
             System.out.println("throw arg format param error + " + param);
             throw new TaskException("jobArgs format error, param: " + param + " does not begin with -");
         }

         i++;
         if (i == subStrings.length) {
             throw new TaskException("jobArgs format param error, param: " + param + " does not have a value");
         }
         value = subStrings[i];

         //System.out.println("param: " + param);
         //System.out.println("value: " + value);

         argProperties.setProperty(param, value);

         i++;
      }

      return argProperties;
   }

   public static String convertFileToArgs(String fileName) throws TaskException {
      Properties properties = new Properties();
      try {
         properties.load(new FileInputStream(fileName));
      } catch  (FileNotFoundException e) {
         throw new TaskException("file: "+ fileName + " not found");
      } catch  (IOException e) {
         throw new TaskException("while loading file: "+ fileName + " caught IOException");
      }

      Set propertyNamesSet = properties.stringPropertyNames();
      String propertyNames[] = (String [])propertyNamesSet.toArray(new String[0]);


      StringBuffer sb = new StringBuffer();
      for (int i=0; i<propertyNames.length; i++) {
          String propertyName = propertyNames[i];
          if (propertyName.startsWith(Constants.FILE_JOB_ARGS_PROPERTY_PREFIX)) {
             if (sb.length() != 0) {
                sb.append(" ");
             }
             sb.append(properties.getProperty(propertyName));
          }
      }
      return sb.toString();
   }

}
