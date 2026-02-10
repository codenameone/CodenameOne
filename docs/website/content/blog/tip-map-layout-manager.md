---
title: 'TIP: Map Layout Manager'
slug: tip-map-layout-manager
url: /blog/tip-map-layout-manager/
original_url: https://www.codenameone.com/blog/tip-map-layout-manager.html
aliases:
- /blog/tip-map-layout-manager.html
date: '2017-10-23'
author: Shai Almog
---

![Header Image](/blog/tip-map-layout-manager/tip.jpg)

__ |  The information in this blog post is slighly out of date. Check out the [newer blog post](/blog/map-component-positioning-revisited.html) that covers positioning components on the map.   
---|---  
  
I’ve recently added a segment to the [online course](https://codenameone.teachable.com/) covering the native maps. This segment is a part of a larger trend towards the upcoming Uber demo which we will release before the end of the year. As part of that work I did some initial “half baked” work on a map layout manager.

The motivation was to position components on top of the map in an effective way, I hacked this a bit and mixed some logic from the map and the layout thus breaking some of the separation but it’s still a pretty cool demo…​

The main thing to learn here is just how easy it is to build your own layout manager, don’t shy away from doing that if you need more control. The case for maps is great because we want to position components based on longitude/latitude values and this works perfectly with the layout manager semantics.

This is the layout manager I created, you’ll notice the code is really simple:
    
    
    public class MapLayout extends Layout implements MapListener {
        private static final String COORD_KEY = "$coord";
        private MapContainer map;
        private Container actual;
        public MapLayout(MapContainer map, Container actual) {
            this.map = map;
            this.actual = actual;
            map.addMapListener(this);
        }
    
        @Override
        public void addLayoutComponent(Object value, Component comp, Container c) {
            comp.putClientProperty(COORD_KEY, (Coord)value);
        }
    
        @Override
        public boolean isConstraintTracking() {
            return true;
        }
    
        @Override
        public Object getComponentConstraint(Component comp) {
            return comp.getClientProperty(COORD_KEY);
        }
    
        @Override
        public boolean isOverlapSupported() {
            return true;
        }
    
        @Override
        public void layoutContainer(Container parent) {
            for(Component current : parent) {
                Coord crd = (Coord)current.getClientProperty(COORD_KEY);
                Point p = map.getScreenCoordinate(crd);
                current.setSize(current.getPreferredSize());
                current.setX(p.getX() - current.getWidth() / 2);
                current.setY(p.getY() - current.getHeight());
            }
        }
    
        @Override
        public Dimension getPreferredSize(Container parent) {
            return new Dimension(100, 100);
        }
    
        @Override
        public void mapPositionUpdated(Component source, int zoom, Coord center) {
            actual.setShouldCalcPreferredSize(true);
            actual.revalidate();
        }
    }

I can use it like this:
    
    
    Form mapDemo = new Form("Maps", new LayeredLayout());
    mapDemo.getToolbar().addMaterialCommandToSideMenu("Hi", FontImage.MATERIAL_3D_ROTATION, e -> {});
    if(BrowserComponent.isNativeBrowserSupported()) {
        MapContainer mc = new MapContainer(JS_API_KEY);
        mapDemo.add(mc);
        Container markers = new Container();
        markers.setLayout(new MapLayout(mc, markers));
        mapDemo.add(markers);
    
        Coord moscone = new Coord(37.7831, -122.401558);
        Button mosconeButton = new Button("");
        FontImage.setMaterialIcon(mosconeButton, FontImage.MATERIAL_PLACE);
        markers.add(moscone, mosconeButton);
    
        mc.zoom(moscone, 5);
    } else {
        // iOS Screenshot process...
        mapDemo.add(new Label("Loading, please wait...."));
    }
    mapDemo.show();

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
