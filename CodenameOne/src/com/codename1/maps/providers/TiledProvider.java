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
package com.codename1.maps.providers;

import com.codename1.ui.geom.Dimension;
import com.codename1.maps.BoundingBox;
import com.codename1.maps.Coord;
import com.codename1.ui.geom.Point;
import com.codename1.maps.Projection;
import com.codename1.maps.ProxyHttpTile;
import com.codename1.maps.Tile;

/**
 * This is a tiled map provider
 * 
 * @author Roman Kamyk <roman.kamyk@itiner.pl>
 */
public abstract class TiledProvider extends MapProvider {

    protected final String _url;
    // Very ugly cacheing
    private Point _tileNo;
    protected int _zoomLevel;

    /**
     * Creates a new Tiled provider
     * 
     * @param url the url of the provider
     * @param projection the Projection system of the Provider
     * @param tileSize the tiles size(usually 256x256)
     */
    public TiledProvider(String url, Projection projection, Dimension tileSize) {
        super(projection, tileSize);
        _url = url;
    }

    /**
     * build a url request for a tile
     * 
     * @param zoomLevel the zoom level
     * @param xTile x position of the tile
     * @param yTile y position of the tile
     * @return the image url of the tile
     */
    protected String url(int zoomLevel, int xTile, int yTile) {
        StringBuffer sb = new StringBuffer(_url);
        sb.append("/");
        sb.append(zoomLevel);
        sb.append("/");
        sb.append(xTile);
        sb.append("/");
        sb.append(yTile);
        sb.append(".png");
        return sb.toString();
    }

    private int tileNo(double pos, double pos0, double scale) {
        return (int) ((pos - pos0) / scale);
    }

    private double tileCoord(int tileNo, double pos0, double scale) {
        return tileNo * scale + pos0;
    }

    /**
     * Scale to the zoom level
     * @param zoomLevel to scale to
     * 
     * @return a scaled coordinate.
     */
    public Coord scale(int zoomLevel) {
        int divider = (1 << zoomLevel);
        double longitude = (1.0 * projection().extent().longitudeDifference()) / divider / tileSize().getWidth();
        double latitude = (1.0 * projection().extent().latitudeDifference()) / divider / tileSize().getHeight();
        return new Coord(latitude, longitude, false);
    }

    private Point tileNo(Coord position, Coord boundary, Coord scale) {
        return new Point(tileNo(position.getLongitude(), boundary.getLongitude(), scale.getLongitude()), tileNo(position.getLatitude(), boundary.getLatitude(), scale.getLatitude()));
    }

    private Coord tileCoord(Point tileNo, Coord boundary, Coord scale) {
        return new Coord(tileCoord(tileNo.getY(), boundary.getLatitude(), scale.getLatitude()),
                tileCoord(tileNo.getX(), boundary.getLongitude(), scale.getLongitude()), true);
    }

    /**
     * @inheritDoc
     */
    public BoundingBox bboxFor(Coord position, int zoomLevel) {
        _zoomLevel = zoomLevel;
        Coord scale = scale(zoomLevel);

        Dimension tileSize = tileSize();
        double x = scale.getLongitude() * tileSize.getWidth();
        double y = scale.getLatitude() * tileSize.getHeight();
        Coord tileScale = new Coord(y, x, false);

        _tileNo = tileNo(position, projection().extent().getSouthWest(), tileScale);
        Coord start = tileCoord(_tileNo, projection().extent().getSouthWest(), tileScale);
        Coord end = start.translate(tileScale.getLatitude(), tileScale.getLongitude());
        return new BoundingBox(start, end);
    }

    /**
     * @inheritDoc
     */
    public Tile tileFor(BoundingBox bbox) {
        String url = url(_zoomLevel, _tileNo.getX(), (1 << _zoomLevel) - _tileNo.getY() - 1);
        return new ProxyHttpTile(tileSize(), bbox, url);
    }
}
