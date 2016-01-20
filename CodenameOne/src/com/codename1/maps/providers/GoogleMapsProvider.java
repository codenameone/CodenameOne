/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */

package com.codename1.maps.providers;

import com.codename1.maps.BoundingBox;
import com.codename1.maps.Coord;
import com.codename1.maps.Mercator;
import com.codename1.maps.Projection;
import com.codename1.maps.ProxyHttpTile;
import com.codename1.maps.Tile;
import com.codename1.ui.geom.Dimension;

/**
 * This is a GoogleMaps Provider https://developers.google.com/maps/documentation/staticmaps/
 * 
 * @author Chen
 */
public class GoogleMapsProvider extends TiledProvider{

    /**
     * @return the tileSize
     */
    public static int getTileSize() {
        return tileSize;
    }

    /**
     * @param aTileSize the tileSize to set
     */
    public static void setTileSize(int aTileSize) {
        tileSize = aTileSize;
    }
    
    private String apiKey;
    
    private int type;
    
    private String language;
    private boolean sensor;
    
    private static int tileSize = 256;
    
    /**
     * This is a regular road map
     */
    public static final int REGULAR = 0;
    
    /**
     * This is a satellite map
     */
    public static final int SATELLITE = 1;
    
    /**
     * This is a satellite + road map
     */
    public static final int HYBRID = 2;
    
    /**
     * Google map provider Constructor
     * @param apiKey google maps api key 
     * https://developers.google.com/maps/documentation/staticmaps/#api_key
     */
     public GoogleMapsProvider(String apiKey) {
        super("http://maps.googleapis.com/maps/api/staticmap?", new Mercator(), new Dimension(tileSize, tileSize));
        this.apiKey = apiKey;
    }
    
    
    /**
     * Sets the map type
     */ 
     public void setMapType(int type){
        this.type = type;
    }
     
    
    /**
     * {@inheritDoc}
     */
    public int maxZoomLevel() {
        return 18;
    }

    /**
     * {@inheritDoc}
     */
    public String attribution() {
        return "Google";
    }
    
    /**
     * {@inheritDoc}
     */
    public Tile tileFor(BoundingBox bbox) {
        StringBuilder sb = new StringBuilder(_url);
        Coord ne = bbox.getNorthEast();
        Coord c = projection().toWGS84(new Coord(ne.getLatitude() - bbox.latitudeDifference()/2, 
                ne.getLongitude() - bbox.longitudeDifference()/2, true));
        
        sb.append("center=");
        sb.append(c.getLatitude());
        sb.append(",");
        sb.append(c.getLongitude());
        sb.append("&format=png");
        sb.append("&zoom="+_zoomLevel);
        sb.append("&size=");
        sb.append(tileSize);
        sb.append("x");
        sb.append(tileSize);
        sb.append("&sensor=");
        sb.append(sensor);
        if(language != null) {
            sb.append("&language=");
            sb.append(language);
        }
        
        if(type == SATELLITE){
            sb.append("&maptype=satellite");        
        }else if(type == HYBRID){
            sb.append("&maptype=hybrid");                
        }
        
        sb.append("&key="+apiKey);
        
        return new ProxyHttpTile(tileSize(), bbox, sb.toString());
    }

    /**
     * Defines the language to use for display of labels on map tiles. 
     * Note that this parameter is only supported for some country tiles; 
     * if the specific language requested is not supported for the tile set, 
     * then the default language for that tileset will be used.
     * @return the language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Defines the language to use for display of labels on map tiles. 
     * Note that this parameter is only supported for some country tiles; 
     * if the specific language requested is not supported for the tile set, 
     * then the default language for that tileset will be used.
     * @param language the language to set
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Specifies whether the application requesting the static map is using a sensor to determine the user's location. 
     * 
     * @return the sensor
     */
    public boolean isSensor() {
        return sensor;
    }

    /**
     * Specifies whether the application requesting the static map is using a sensor to determine the user's location. 
     * 
     * @param sensor the sensor to set
     */
    public void setSensor(boolean sensor) {
        this.sensor = sensor;
    }

}
