/*
 * Copyright (c) 2010, 2011 Itiner.pl. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Itiner designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Itiner in the LICENSE.txt file that accompanied this code.
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
 */
package com.codename1.maps;

import com.codename1.maps.Tile;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkEvent;
import com.codename1.io.services.ImageDownloadService;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.geom.Dimension;

import com.codename1.maps.BoundingBox;
import com.codename1.ui.events.ActionListener;
import com.codename1.util.StringUtil;

/**
 * This Tile brings the tile image from a given http url.
 * 
 * @author Roman Kamyk <roman.kamyk@itiner.pl>
 */
public class ProxyHttpTile extends Tile {

    private Tile _tile;
    private String _url;

    /**
     * Creates an Http Tile
     * 
     * @param tileSize the tile size
     * @param bbox the tile bounding box
     * @param url the url to bring the image from
     */
    public ProxyHttpTile(Dimension tileSize, BoundingBox bbox, final String url) {
        super(tileSize, bbox, null);
        _url = url;
        String cacheId = url.substring(url.indexOf(":")+1);
        cacheId = StringUtil.replaceAll(cacheId, "\\", "_");
        cacheId = StringUtil.replaceAll(cacheId, "/", "_");
        cacheId = StringUtil.replaceAll(cacheId, ".", "_");
        cacheId = StringUtil.replaceAll(cacheId, "?", "_");
        cacheId = StringUtil.replaceAll(cacheId, "&", "_");
        
        ImageDownloadService.createImageToStorage(url, new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                NetworkEvent ne = (NetworkEvent) evt;

                _tile = new Tile(ProxyHttpTile.this.dimension(),
                        ProxyHttpTile.this.getBoundingBox(),
                        (Image) ne.getMetaData());
                ProxyHttpTile.this.fireReady();
            }
        }, cacheId, true);


    }

    /**
     * @inheritDoc
     */
    public boolean paint(Graphics g) {
        if(_tile == null){
            return false;
        }
        return _tile.paint(g);
    }
}
