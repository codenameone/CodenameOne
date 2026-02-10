---
title: Map Layout Update
slug: map-layout-update
url: /blog/map-layout-update/
original_url: https://www.codenameone.com/blog/map-layout-update.html
aliases:
- /blog/map-layout-update.html
date: '2018-03-07'
author: Shai Almog
---

![Header Image](/blog/map-layout-update/build-real-world-full-stack-mobile-apps-in-java.jpg)

__ |  The information in this blog post is slighly out of date. Check out the [newer blog post](/blog/map-component-positioning-revisited.html) that covers positioning components on the map.   
---|---  
  
A while back I [introduced a MapLayout class](/blog/tip-map-layout-manager.html) as a tip and discussed the usage of this class. Since that introduction we ran into some scale issues as the layout misbehaved when a lot of elements were added to it. The crux of the issue is in the native map API which runs on the OS native thread and the Codename One API which needs immediate responses for layout.

These issues became very apparent in the [Uber app](https://codenameone.teachable.com/p/build-real-world-full-stack-mobile-apps-in-java) clone code. As a solution we updated the layout to use an approach that’s asynchronous and fetches data in batches. This made the layout far more responsive.

While we were there we also needed a way to align components to the position in the map e.g. a marker needs a center/bottom position while a car would be centered etc. So we added support for alignment as well which you can specify in the new API using:
    
    
    MapLayout.setHorizontalAlignment(myCmp, HALIGN.LEFT);
    MapLayout.setVerticalAlignment(myCmp, VALIGN.BOTTOM);

This tool is still a bit of a cludge, ideally as we work with it in the future we’ll abstract it as a nice API into the map cn1lib. For now this is the revised version of the class:
    
    
    public class MapLayout extends Layout implements MapListener {
            private static final String COORD_KEY = "$coord";
            private static final String POINT_KEY = "$point";
            private static final String HORIZONTAL_ALIGNMENT = "$align";
            private static final String VERTICAL_ALIGNMENT = "$valign";
            private final MapContainer map;
            private final Container actual;
            private boolean inUpdate;
            private Runnable nextUpdate;
            private int updateCounter;
    
            public static enum HALIGN {
                LEFT {
                    int convert(int x, int width) { return x; }
                },
                CENTER {
                    int convert(int x, int width) { return x - width / 2; }
                },
                RIGHT { int convert(int x, int width) { return x - width; }
                };
    
                abstract int convert(int x, int width);
            }
    
            public static enum VALIGN {
                TOP {
                    int convert(int y, int height) { return y; }
                },
                MIDDLE {
                    int convert(int y, int height) { return y + height / 2; }
                },
                BOTTOM {
                    int convert(int y, int height) { return y + height; }
                };
    
                abstract int convert(int y, int height);
            }
    
            public MapLayout(MapContainer map, Container actual) {
                this.map = map;
                this.actual = actual;
                map.addMapListener(this);
            }
    
            @Override
            public void addLayoutComponent(Object value, Component comp, Container c) {
                comp.putClientProperty(COORD_KEY, (Coord) value);
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
    
            public static void setHorizontalAlignment(Component cmp, HALIGN a) {
                cmp.putClientProperty(HORIZONTAL_ALIGNMENT, a);
            }
    
            public static void setVerticalAlignment(Component cmp, VALIGN a) {
                cmp.putClientProperty(VERTICAL_ALIGNMENT, a);
            }
    
            @Override
            public void layoutContainer(Container parent) {
                int parentX = 0;
                int parentY = 0;
                for (Component current : parent) {
                    Coord crd = (Coord) current.getClientProperty(COORD_KEY);
                    Point p = (Point) current.getClientProperty(POINT_KEY);
                    if (p == null) {
                        p = map.getScreenCoordinate(crd);
                        current.putClientProperty(POINT_KEY, p);
                    }
                    HALIGN h = (HALIGN)current.getClientProperty(HORIZONTAL_ALIGNMENT);
                    if(h == null) {
                        h = HALIGN.LEFT;
                    }
                    VALIGN v = (VALIGN)current.getClientProperty(VERTICAL_ALIGNMENT);
                    if(v == null) {
                        v = VALIGN.TOP;
                    }
                    current.setSize(current.getPreferredSize());
                    current.setX(h.convert(p.getX() - parentX, current.getWidth()));
                    current.setY(v.convert(p.getY() - parentY, current.getHeight()));
                }
            }
    
            @Override
            public Dimension getPreferredSize(Container parent) {
                return new Dimension(100, 100);
            }
    
            @Override
            public void mapPositionUpdated(Component source, int zoom, Coord center) {
                Runnable r = new Runnable() {
                    public void run() {
                        inUpdate = true;
                        try {
                            List<Coord> coords = new ArrayList<>();
                            List<Component> cmps = new ArrayList<>();
                            int len = actual.getComponentCount();
                            for (Component current : actual) {
                                Coord crd = (Coord) current.getClientProperty(COORD_KEY);
                                coords.add(crd);
                                cmps.add(current);
                            }
                            int startingUpdateCounter = ++updateCounter;
                            List<Point> points = map.getScreenCoordinates(coords);
                            if (startingUpdateCounter != updateCounter || len != points.size()) {
                                // Another update must have run while we were waiting for the bounding box.
                                // in which case, that update would be more recent than this one.
                                return;
                            }
                            for (int i=0; i<len; i++) {
                                Component current = cmps.get(i);
                                Point p = points.get(i);
                                current.putClientProperty(POINT_KEY, p);
                            }
                            actual.setShouldCalcPreferredSize(true);
                            actual.revalidate();
                            if (nextUpdate != null) {
                                Runnable nex = nextUpdate;
                                nextUpdate = null;
                                callSerially(nex);
                            }
                        } finally {
                            inUpdate = false;
                        }
    
                    }
    
                };
                if (inUpdate) {
                    nextUpdate = r;
                } else {
                    nextUpdate = null;
                    callSerially(r);
                }
            }
    }

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
