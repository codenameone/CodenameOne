/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.util.regex;

/**
 *
 * @author shannah
 */
public interface MatchResult {
   public int end();
   public int end(int group);
   public String group();
   public String group(int group);
   public int groupCount();
   public int start();
   public int start(int group);
}
