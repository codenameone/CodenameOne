/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */

package com.codename1.tools.resourcebuilder;

import com.codename1.ui.util.EditableResources;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Allows us to define an animated gif as an animation object
 *
 * @author Shai Almog
 */
public class AnimationImpl {
    private File file;
    private boolean loop = true;
    
    public void writeResource(DataOutputStream output) throws IOException {
        ImageReader iReader = ImageIO.getImageReadersBySuffix("gif").next();
        iReader.setInput(ImageIO.createImageInputStream(new FileInputStream(file)));
        int frames = iReader.getNumImages(true);
        BufferedImage previousImage = null;
        int w = 0, h = 0;
        List<Frame> imageList = new ArrayList<Frame>();
        List<Integer> colorPalette = new ArrayList<Integer>();
        int totalTime = 0;

        for(int frameIter = 0 ; frameIter < frames ; frameIter++) {
            boolean drawPreviousFrame = false;
            BufferedImage currentImage = iReader.read(frameIter);
            if(previousImage != null) {
                //previousImage = iReader.read(0);
                Graphics2D g2d = previousImage.createGraphics();
                drawPreviousFrame = !isRestoreToBackgroundColor(iReader, frameIter);
                if(!drawPreviousFrame) {
                    previousImage = new BufferedImage(previousImage.getWidth(), previousImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    g2d.dispose();
                    g2d = previousImage.createGraphics();
                }
                Point p = getPixelOffsets(iReader, frameIter);
                g2d.drawImage(currentImage, p.x, p.y, null);
                g2d.dispose();
            } else {
                w = currentImage.getWidth();
                h = currentImage.getHeight();
                previousImage = currentImage;
            }
            Frame f = new Frame();
            f.imageData = new int[w * h];
            previousImage.getRGB(0, 0, w, h, f.imageData, 0, w);

            // extract the time from gif
            f.time = getFrameTime(iReader, frameIter);
            f.drawPreviousFrame = drawPreviousFrame;
            totalTime += f.time;

            // make sure the palette has the colors from all the frames
            for(int currentColor : f.imageData) {
                if(!colorPalette.contains(currentColor) && colorPalette.size() < 255) {
                    colorPalette.add(currentColor);
                }
            }
            imageList.add(f);
        }

        // write the palette for the animation into the resource
        output.writeByte(colorPalette.size());
        for(int color : colorPalette) {
            output.writeInt(color);
        }

        // write the width/height of the animation
        output.writeShort(w);
        output.writeShort(h);

        // write the number of frames
        output.writeByte(imageList.size());

        output.writeInt(totalTime);

        output.writeBoolean(loop);

        for(int iter = 0 ; iter < imageList.size() ; iter++)  {
            Frame currentFrame = imageList.get(iter);

            // the first frame must be a keyframe and doesn't need a timestamp
            // and is always a keyframe so its a special case...
            if(iter == 0) {
                for(int value : currentFrame.imageData) {
                    output.writeByte(getPaletteIndex(colorPalette, value));
                }
            } else {
                byte[] data = currentFrame.compress(w, imageList.get(iter - 1).imageData, colorPalette);
                // the timestamp for the animation frame
                output.writeInt(currentFrame.time);

                if(data != null) {
                    // indicate that this is not a keyframe
                    output.writeBoolean(false);
                    output.writeBoolean(currentFrame.drawPreviousFrame);
                    output.write(data);
                } else {
                    // this is a keyframe
                    output.writeBoolean(true);
                    for(int value : currentFrame.imageData) {
                        output.writeByte(getPaletteIndex(colorPalette, value));
                    }
                }
            }
        }
    }

    private static int getPaletteIndex(List<Integer> colorPalette, int value) {
        int offset = colorPalette.indexOf(value);
        // this can happen if there are more than 256 colors in a gif which is possible with animated gifs
        if(offset < 0) {
            return 0;
        }

        return offset;
    }

    public static Point getPixelOffsets(ImageReader reader, int num) throws IOException {
        IIOMetadata meta = reader.getImageMetadata(num);
        Point point = new Point(-1,-1);
        Node root = meta.getAsTree("javax_imageio_1.0");
        //printNodeTree(root);
        for (Node c = root.getFirstChild(); c != null; c = c.getNextSibling()) {
            String name = c.getNodeName();
            if ("Dimension".equals(name)) {
                for (c = c.getFirstChild(); c != null; c = c.getNextSibling()) {
                    name = c.getNodeName();
                    if ("HorizontalPixelOffset".equals(name))
                        point.x = getValueAttribute(c);
                    else if ("VerticalPixelOffset".equals(name))
                        point.y = getValueAttribute(c);
                }
                return point;
            }
        }
        return point;
    }

    public static int getFrameTime(ImageReader reader, int num) throws IOException {
        IIOMetadata meta = reader.getImageMetadata(num);
        Node parent = meta.getAsTree("javax_imageio_gif_image_1.0");
        //printNodeTree(parent);
        NodeList root = parent.getChildNodes();
        for(int iter = 0 ; iter < root.getLength() ; iter++) {
            Node n = root.item(iter);
            if(n.getNodeName().equals("GraphicControlExtension")) {
                return Integer.parseInt(n.getAttributes().getNamedItem("delayTime").getNodeValue()) * 10;
            }
        }
        return 1000;
    }

    static boolean isRestoreToBackgroundColor(ImageReader reader, int num) throws IOException {
        IIOMetadata meta = reader.getImageMetadata(num);
        Node parent = meta.getAsTree("javax_imageio_gif_image_1.0");
        //printNodeTree(parent);
        NodeList root = parent.getChildNodes();
        for(int iter = 0 ; iter < root.getLength() ; iter++) {
            Node n = root.item(iter);
            if(n.getNodeName().equals("GraphicControlExtension")) {
                return n.getAttributes().getNamedItem("disposalMethod").getNodeValue().equalsIgnoreCase("restoreToBackgroundColor");
            }
        }
        return false;
    }

    private static void printNodeTree(Node n) {
        System.out.println(n.getNodeName() + " has attributes: " + getNodeAttrs(n));
        NodeList list = n.getChildNodes();
        if(list != null && list.getLength() > 0) {
            System.out.println("Begin children of : " + n.getNodeName());
            for(int iter = 0 ; iter < list.getLength() ; iter++) {
                printNodeTree(list.item(iter));
            }
            System.out.println("End children of : " + n.getNodeName());
        }
    }

    private static String getNodeAttrs(Node n) {
        NamedNodeMap map = n.getAttributes();
        int size = map.getLength();
        String response = "";
        for(int iter = 0 ; iter < size ; iter++) {
            response += map.item(iter).getNodeName() + " = " + map.item(iter).getNodeValue() + " ";
        }
        return response;
    }

    static int getValueAttribute(Node node) {
        try {
            return Integer.parseInt(node.getAttributes().getNamedItem("value").getNodeValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    private static class Frame {
        public int time;
        public int[] imageData;
        public boolean drawPreviousFrame;

        /**
         * Try to compress the frame data, if this fails then return null and a keyframe
         * would be inserted. Failure is defined by compression coming out as more than
         * the size of the original or close enough.
         */
        public byte[] compress(int width, int[] previousFrame, List<Integer> colorPalette) throws IOException {
            ByteArrayOutputStream outputArray = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(outputArray);

            // compress the image by writing the row offset (as a short) for a modified
            // row followed by its data. If there are too many modified rows compression
            // won't be good
            int rowCount = previousFrame.length / width;
            for(int row = 0 ; row < rowCount ; row++) {
                int offset = row * width;
                for(int iter = offset ; iter < offset + width ; iter++) {
                    if(previousFrame[iter] != imageData[iter]) {
                        // this row is different we need to write it into the stream
                        output.writeShort(row);
                        for(int rowIter = 0 ; rowIter < width ; rowIter++) {
                            output.writeByte(getPaletteIndex(colorPalette, imageData[offset + rowIter]));
                        }
                        break;
                    }
                }
            }

            // indicate EOF...
            output.writeShort(-1);
            output.close();
            byte[] bytes = outputArray.toByteArray();
            // if its at least 70% the size then its worth the effort, otherwise
            // just store the keyframe
            if(bytes.length < imageData.length / 10 * 7) {
                return bytes;
            }
            return null;
        }
    }
    
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
    
    public boolean getLoop() {
        return loop;
    }
    
    public void setLoop(boolean loop) {
        this.loop = loop;
    }

}
