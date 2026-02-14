---
title: Photo Cropping Wizard
slug: photo-cropping-wizard
url: /blog/photo-cropping-wizard/
original_url: https://www.codenameone.com/blog/photo-cropping-wizard.html
aliases:
- /blog/photo-cropping-wizard.html
date: '2019-05-28'
author: Shai Almog
---

![Header Image](/blog/photo-cropping-wizard/uidesign.jpg)

A month ago [Francesco asked](https://github.com/codenameone/CodenameOne/issues/2778) how to implement the common image cropping UI we see in many applications where the user can pan and zoom to decide the cropping area. We implemented that feature by effectively enhancing the `ImageViewer` class with two new methods:
    
    
    public Image getCroppedImage(int backgroundColor);
    public Image getCroppedImage(int width, int height, int backgroundColor);

But that’s just the first step, implementing the whole UI is still not trivial. We’d like to add a more general purpose component that does that but doing this with an approach that’s sufficiently generic is hard…​

[Francesco wrote some code](https://stackoverflow.com/questions/56013760/generalize-code-to-capture-and-crop-a-photo) that used the API but it was still non-trivial. So I took that code as a starting point and implemented something that’s slightly more generic, it still isn’t perfect but it should be a good starting point for a more general purpose component.

Since this uses the `ImageViewer` you can use pinch to zoom and drag to position the image the way you want:
    
    
    private void cropImage(Image img, int destWidth, int destHeight,
            OnComplete<Image> s) {
        Form previous = getCurrentForm();
        Form cropForm = new Form("Crop your avatar", new LayeredLayout());
    
        Label moveAndZoom = new Label("Move and zoom the photo to crop it");
        moveAndZoom.getUnselectedStyle().setFgColor(0xffffff); __**(1)**
        moveAndZoom.getUnselectedStyle().setAlignment(CENTER);
        moveAndZoom.setCellRenderer(true);
        cropForm.setGlassPane((Graphics g, Rectangle rect) -> {
            g.setColor(0x0000ff);
            g.setAlpha(150); __**(2)**
            Container cropCp = cropForm.getContentPane();
            int posY = cropForm.getContentPane().getAbsoluteY();
    
            GeneralPath p = new GeneralPath(); __**(3)**
            p.setRect(new Rectangle(0, posY, cropCp.getWidth(), cropCp.getHeight()), null);
            if(isPortrait()) {
                p.arc(0, posY + cropCp.getHeight() / 2 - cropCp.getWidth() / 2,
                    cropCp.getWidth() - 1, cropCp.getWidth() - 1, 0, Math.PI*2);
            } else {
                p.arc(cropCp.getWidth() / 2 - cropCp.getHeight() / 2, posY,
                    cropCp.getHeight() - 1, cropCp.getHeight() - 1, 0, Math.PI*2);
            }
            g.fillShape(p);
            g.setAlpha(255);
            g.setColor(0xffffff);
            moveAndZoom.setX(0);
            moveAndZoom.setY(posY);
            moveAndZoom.setWidth(cropCp.getWidth());
            moveAndZoom.setHeight(moveAndZoom.getPreferredH());
            moveAndZoom.paint(g);
        });
    
        final ImageViewer viewer = new ImageViewer();
        viewer.setImage(img);
    
        cropForm.add(viewer);
        cropForm.getToolbar().addMaterialCommandToRightBar("", FontImage.MATERIAL_CROP, e -> {
            previous.showBack();
            s.completed(viewer.getCroppedImage(0). __**(4)**
                    fill(destWidth, destHeight));
        });
        cropForm.getToolbar().addMaterialCommandToLeftBar("", FontImage.MATERIAL_CANCEL, e -> previous.showBack());
        cropForm.show();
    }

__**1** | This label is drawn manually on top of the glass pane overlay  
---|---  
__**2** | The glass pane highlights invisible portions although the cropped image in our case is still square, this is pretty common behavior as a UI will sometimes show the image rounded  
__**3** | This shape represents the blue tinted overlay on top  
  
The UI results in an image like this:

![The Crop UI on the Simulator](/blog/photo-cropping-wizard/crop-ui.png)

Figure 1. The Crop UI on the Simulator

To get to that point we need the UI that invokes this code which looks like this:
    
    
    Form hi = new Form("Cropped Image", BoxLayout.y());
    hi.getToolbar().addMaterialCommandToRightBar("", FontImage.MATERIAL_CAMERA, e -> {
        Capture.capturePhoto(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (evt == null || evt.getSource() == null) {
                    // the user cancelled the image capturing
                    return;
                }
                String file = (String) evt.getSource();
                try {
                    Image img = Image.createImage(file);
                    cropImage(img, 256, 256, i -> {
                        hi.removeAll();
                        hi.add(new ScaleImageLabel(i));
                        hi.revalidate();
                    });
                } catch (IOException ex) {
                    Log.p("Error loading captured image from camera", Log.ERROR);
                    Log.e(ex);
                }
            }
        });
    });
    
    hi.show();
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — May 29, 2019 at 5:08 pm ([permalink](https://www.codenameone.com/blog/photo-cropping-wizard.html#comment-23584))

> Thank you Shai 🙂
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
