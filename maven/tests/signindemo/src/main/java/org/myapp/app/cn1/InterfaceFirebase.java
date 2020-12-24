/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.myapp.app.cn1;

/**
 *
 * @author shannah
 */
import com.codename1.system.NativeInterface;
public interface InterfaceFirebase extends NativeInterface {
   public void launchFirebase();
   public void logWithFirebase(String key, String info);
}
