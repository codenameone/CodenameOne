package com.codename1.impl;

import com.codename1.components.FileTree;
import com.codename1.components.FileTreeModel;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Storage;
import com.codename1.ui.Button;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.ImageIO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Hashtable;

class OpenGalleryFileTree extends FileTree {

    private final ActionListener response;
    private final Dialog d;

    public OpenGalleryFileTree(FileTreeModel model, ActionListener response, Dialog d) {
        super(model);
        this.response = response;
        this.d = d;
    }

    @Override
    protected Button createNodeComponent(final Object node, int depth) {
        if (node == null || !getModel().isLeaf(node)) {
            return super.createNodeComponent(node, depth);
        }
        Hashtable t = (Hashtable) Storage.getInstance().readObject("thumbnails");
        if (t == null) {
            t = new Hashtable();
        }
        final Hashtable thumbs = t;
        final Button b = super.createNodeComponent(node, depth);
        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent evt) {
                response.actionPerformed(new ActionEvent(node, ActionEvent.Type.Other));
                d.dispose();
            }
        });
        final ImageIO imageio = ImageIO.getImageIO();
        if (imageio != null) {
            Display.getInstance().scheduleBackgroundTask(
                    new CreateNodeComponentRunnable(thumbs, node, imageio, b));
        }
        return b;
    }

    private static class CreateNodeComponentRunnable implements Runnable {

        private final Hashtable thumbs;
        private final Object node;
        private final ImageIO imageio;
        private final Button b;

        public CreateNodeComponentRunnable(Hashtable thumbs, Object node, ImageIO imageio, Button b) {
            this.thumbs = thumbs;
            this.node = node;
            this.imageio = imageio;
            this.b = b;
        }

        @Override
        public void run() {
            byte[] data = (byte[]) thumbs.get(node);
            if (data == null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    imageio.save(FileSystemStorage.getInstance().openInputStream((String) node),
                            out,
                            ImageIO.FORMAT_JPEG,
                            b.getIcon().getWidth(), b.getIcon().getHeight(), 1);
                    data = out.toByteArray();
                    thumbs.put(node, data);
                    Storage.getInstance().writeObject("thumbnails", thumbs);
                } catch (IOException ex) {
                    Log.e(ex);
                }
            }
            if (data != null) {
                Image im = Image.createImage(data, 0, data.length);
                b.setIcon(im);
            }
        }
    }
}
