/*****************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one                *
 * or more contributor license agreements.  See the NOTICE file              *
 * distributed with this work for additional information                     *
 * regarding copyright ownership.  The ASF licenses this file                *
 * to you under the Apache License, Version 2.0 (the                         *
 * "License"); you may not use this file except in compliance                *
 * with the License.  You may obtain a copy of the License at                *
 *                                                                           *
 *     http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                           *
 * Unless required by applicable law or agreed to in writing,                *
 * software distributed under the License is distributed on an               *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY                    *
 * KIND, either express or implied.  See the License for the                 *
 * specific language governing permissions and limitations                   *
 * under the License.                                                        *
 *                                                                           *
 *                                                                           *
 * This file is part of the BeanShell Java Scripting distribution.           *
 * Documentation and updates may be found at http://www.beanshell.org/       *
 * Patrick Niemeyer (pat@pat.net)                                            *
 * Author of Learning Java, O'Reilly & Associates                            *
 *                                                                           *
 *****************************************************************************/
package bsh.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

// For string related utils
import bsh.BshClassManager;
import bsh.ClassPathException;
import bsh.StringUtil;
import bsh.classpath.BshClassPath;
import bsh.classpath.ClassManagerImpl;
import bsh.classpath.ClassPathListener;

/**
    A simple class browser for the BeanShell desktop.
*/
public class ClassBrowser extends JSplitPane
    implements ListSelectionListener, ClassPathListener
{
    BshClassPath classPath;
    BshClassManager classManager;

    // GUI
    JFrame frame;
    JInternalFrame iframe;
    JList classlist, conslist, mlist, fieldlist;
    PackageTree ptree;
    JTextArea methodLine;
    JTree tree;
    // For JList models
    String [] packagesList;
    String [] classesList;
    Constructor [] consList;
    Method [] methodList;
    Field [] fieldList;

    String selectedPackage;
    Class selectedClass;

    private static final Color LIGHT_BLUE = new Color(245,245,255);

    public ClassBrowser() {
        this( BshClassManager.createClassManager( null/*interpreter*/ ) );
    }

    public ClassBrowser( BshClassManager classManager ) {
        super( VERTICAL_SPLIT, true );
        this.classManager = classManager;

        setBorder(null);
        javax.swing.plaf.SplitPaneUI ui = getUI();
        if(ui instanceof javax.swing.plaf.basic.BasicSplitPaneUI) {
            ((javax.swing.plaf.basic.BasicSplitPaneUI)ui).getDivider()
                .setBorder(null);
        }
    }

    String [] toSortedStrings ( Collection<String> c ) {
        String [] sa = c.toArray(new String[c.size()]);
        Arrays.sort(sa);
        return sa;
    }

    void setClist( String packagename ) {
        this.selectedPackage = packagename;

        Set set = classPath.getClassesForPackage( packagename );
        if ( set == null )
            set = new HashSet();

        // remove inner classes and shorten class names
        List list = new ArrayList();
        Iterator it = set.iterator();
        while (it.hasNext()) {
            String cname = (String)it.next();
            if ( cname.indexOf("$") == -1 )
                list.add( BshClassPath.splitClassname( cname )[1] );
        }

        classesList = toSortedStrings(list);
        classlist.setListData( classesList );
        //setMlist( (String)classlist.getModel().getElementAt(0) );
    }

    String [] parseConstructors( Constructor [] constructors ) {
        String [] sa = new String [ constructors.length ] ;
        for(int i=0; i< sa.length; i++) {
            Constructor con = constructors[i];
            sa[i] = StringUtil.methodString(
                con.getName(), con.getParameterTypes() );
        }
        //return bubbleSort(sa);
        return sa;
    }

    String [] parseMethods( Method [] methods ) {
        String [] sa = new String [ methods.length ] ;
        for(int i=0; i< sa.length; i++)
            sa[i] = StringUtil.methodString(
                methods[i].getName(), methods[i].getParameterTypes() );
        //return bubbleSort(sa);
        return sa;
    }

    String [] parseFields( Field[] fields ) {
        String [] sa = new String [ fields.length ] ;
        for(int i=0; i< sa.length; i++) {
            Field f = fields[i];
            sa[i] = f.getName();
        }
        return sa;
    }

    Constructor [] getPublicConstructors( Constructor [] constructors ) {
        Vector v = new Vector();
        for(int i=0; i< constructors.length; i++)
            if ( Modifier.isPublic(constructors[i].getModifiers()) )
                v.addElement( constructors[i] );

        Constructor [] ca = new Constructor [ v.size() ];
        v.copyInto( ca );
        return ca;
    }

    Method [] getPublicMethods( Method [] methods ) {
        Vector v = new Vector();
        for(int i=0; i< methods.length; i++)
            if ( Modifier.isPublic(methods[i].getModifiers()) )
                v.addElement( methods[i] );

        Method [] ma = new Method [ v.size() ];
        v.copyInto( ma );
        return ma;
    }

    Field[] getPublicFields( Field [] fields ) {
        Vector v = new Vector();
        for(int i=0; i< fields.length; i++)
            if ( Modifier.isPublic(fields[i].getModifiers()) )
                v.addElement( fields[i] );

        Field [] fa = new Field [ v.size() ];
        v.copyInto( fa );
        return fa;
    }

    void setConslist( Class clas ) {
        if ( clas == null ) {
            conslist.setListData( new Object [] { } );
            return;
        }

        consList = getPublicConstructors( clas.getDeclaredConstructors() );
        conslist.setListData( parseConstructors(consList) );
    }

    void setMlist( String classname )
    {
        if ( classname == null )
        {
            mlist.setListData( new Object [] { } );
            setConslist( null );
            setClassTree( null );
            return;
        }

        try {
            if ( selectedPackage.equals("<unpackaged>") )
                selectedClass = classManager.classForName( classname );
            else
                selectedClass = classManager.classForName(
                    selectedPackage + "." + classname );
        } catch ( Exception e ) {
            System.err.println(e);
            return;
        }
        if ( selectedClass == null ) {
            // not found?
            System.err.println("class not found: "+classname);
            return;
        }
        methodList = getPublicMethods( selectedClass.getDeclaredMethods() );
        mlist.setListData( parseMethods(methodList) );

        setClassTree( selectedClass );
        setConslist( selectedClass );
        setFieldList( selectedClass );
    }

    void setFieldList( Class clas ) {
        if ( clas == null ) {
            fieldlist.setListData( new Object [] { } );
            return;
        }

        fieldList = getPublicFields(clas.getDeclaredFields());
        fieldlist.setListData( parseFields(fieldList) );
    }

    void setMethodLine( Object method ) {
        methodLine.setText( method==null ? "" : method.toString() );
    }

    void setClassTree( Class clas ) {
        if ( clas == null ) {
            tree.setModel( null );
            return;
        }

        MutableTreeNode bottom = null, top = null;
        DefaultMutableTreeNode up;
        do {
            up= new DefaultMutableTreeNode( clas.toString() );
            if ( top != null )
                up.add( top );
            else
                bottom = up;
            top = up;
        } while ( (clas = clas.getSuperclass()) != null );
        tree.setModel( new DefaultTreeModel(top) );

        if ( null != bottom ) {
            TreeNode tn = bottom.getParent();
            if ( tn != null ) {
                TreePath tp =  new TreePath (
                    ((DefaultTreeModel)tree.getModel()).getPathToRoot( tn ) );
                tree.expandPath( tp );
            }
        }
    }

    JPanel labeledPane( JComponent comp, String label ) {
        JPanel jp = new JPanel( new BorderLayout() );
        jp.add( "Center", comp );
        jp.add( "North", new JLabel(label, SwingConstants.CENTER) );
        return jp;
    }

    public void init() throws ClassPathException
    {
        // Currently we have to cast because BshClassPath is not known by
        // the core.
        classPath = ((ClassManagerImpl)classManager).getClassPath();

    // maybe add MappingFeedbackListener here... or let desktop if it has
    /*
        classPath.insureInitialized( null
            // get feedback on mapping...
            new ConsoleInterface() {
                public Reader getIn() { return null; }
                public PrintStream getOut() { return System.out; }
                public PrintStream getErr() { return System.err; }
                public void println( String s ) { System.out.println(s); }
                public void print( String s ) { System.out.print(s); }
                public void print( String s, Color color ) { print( s ); }
                public void error( String s ) { print( s ); }
            }
        );
    */

        classPath.addListener( this );

        Set pset = classPath.getPackagesSet();

        ptree = new PackageTree( pset );
        ptree.addTreeSelectionListener( new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                TreePath tp = e.getPath();
                Object [] oa = tp.getPath();
                StringBuilder selectedPackage = new StringBuilder();
                for(int i=1; i<oa.length; i++) {
                    selectedPackage.append( oa[i].toString() );
                    if ( i+1 < oa.length )
                        selectedPackage.append(".");
                }
                setClist( selectedPackage.toString() );
            }
        } );

        classlist=new JList();
        classlist.setBackground(LIGHT_BLUE);
        classlist.addListSelectionListener(this);

        conslist = new JList();
        conslist.addListSelectionListener(this);

        mlist = new JList();
        mlist.setBackground(LIGHT_BLUE);
        mlist.addListSelectionListener(this);

        fieldlist = new JList();
        fieldlist.addListSelectionListener(this);

        JSplitPane methodConsPane = splitPane(
            JSplitPane.VERTICAL_SPLIT, true,
            labeledPane(new JScrollPane(conslist), "Constructors"),
            labeledPane(new JScrollPane(mlist), "Methods")
            );

        JSplitPane rightPane = splitPane(JSplitPane.VERTICAL_SPLIT, true,
            methodConsPane,
            labeledPane(new JScrollPane(fieldlist), "Fields")
            );

        JSplitPane sp = splitPane(
            JSplitPane.HORIZONTAL_SPLIT, true,
            labeledPane(new JScrollPane(classlist), "Classes"),
            rightPane );
        sp = splitPane(
            JSplitPane.HORIZONTAL_SPLIT, true,
                labeledPane(new JScrollPane(ptree), "Packages"), sp);

        JPanel bottompanel = new JPanel( new BorderLayout() );
        methodLine = new JTextArea(1,60);
        methodLine.setBackground(LIGHT_BLUE);
        methodLine.setEditable(false);
        methodLine.setLineWrap(true);
        methodLine.setWrapStyleWord(true);
        methodLine.setFont( new Font("Monospaced", Font.BOLD, 14) );
        methodLine.setMargin( new Insets(5,5,5,5) );
        methodLine.setBorder( new MatteBorder(1,0,1,0,
            LIGHT_BLUE.darker().darker()) );
        bottompanel.add("North", methodLine);
        JPanel p = new JPanel( new BorderLayout() );

        tree = new JTree();
        tree.addTreeSelectionListener( new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                driveToClass( e.getPath().getLastPathComponent().toString() );
            }
        } );

        tree.setBorder( BorderFactory.createRaisedBevelBorder() );
        setClassTree(null);
        p.add( "Center", tree );
        bottompanel.add("Center", p );

        // give it a preferred height
        bottompanel.setPreferredSize(new java.awt.Dimension(150,150));

        setTopComponent( sp );
        setBottomComponent( bottompanel );
    }

    private JSplitPane splitPane(
        int orientation,
        boolean redraw,
        JComponent c1,
        JComponent c2
    ) {
        JSplitPane sp = new JSplitPane(orientation, redraw, c1, c2);
        sp.setBorder(null);
        javax.swing.plaf.SplitPaneUI ui = sp.getUI();
        if(ui instanceof javax.swing.plaf.basic.BasicSplitPaneUI) {
            ((javax.swing.plaf.basic.BasicSplitPaneUI)ui).getDivider()
                .setBorder(null);
        }
        return sp;
    }

    public static void main( String [] args )
        throws Exception
    {
        ClassBrowser cb = new ClassBrowser();
        cb.init();

        JFrame f=new JFrame("BeanShell Class Browser v1.0");
        f.getContentPane().add( "Center", cb );
        cb.setFrame( f );
        f.pack();
        f.setVisible(true);
    }

    public void setFrame( JFrame frame ) {
        this.frame = frame;
    }
    public void setFrame( JInternalFrame frame ) {
        this.iframe = frame;
    }

    public void valueChanged(ListSelectionEvent e)
    {
        if ( e.getSource() == classlist )
        {
            String classname = (String)classlist.getSelectedValue();
            setMlist( classname );

            // hack
            // show the class source in the "method" line...
            String methodLineString;
            if ( classname == null )
                methodLineString = "Package: "+selectedPackage;
            else
            {
                String fullClassName =
                    selectedPackage.equals("<unpackaged>") ?
                        classname : selectedPackage+"."+classname;
                methodLineString =
                    fullClassName
                    +" (from "+ classPath.getClassSource( fullClassName ) +")";
            }

            setMethodLine( methodLineString );
        }
        else
        if ( e.getSource() == mlist )
        {
            int i = mlist.getSelectedIndex();
            if ( i == -1 )
                setMethodLine( null );
            else
                setMethodLine( methodList[i] );
        }
        else
        if ( e.getSource() == conslist )
        {
            int i = conslist.getSelectedIndex();
            if ( i == -1 )
                setMethodLine( null );
            else
                setMethodLine( consList[i] );
        }
        else
        if ( e.getSource() == fieldlist )
        {
            int i = fieldlist.getSelectedIndex();
            if ( i == -1 )
                setMethodLine( null );
            else
                setMethodLine( fieldList[i] );
        }
    }

    // fully qualified classname
    public void driveToClass( String classname ) {
        String [] sa = BshClassPath.splitClassname( classname );
        String packn = sa[0];
        String classn = sa[1];

        // Do we have the package?
        if ( classPath.getClassesForPackage(packn).size()==0 )
            return;

        ptree.setSelectedPackage( packn );

        for(int i=0; i< classesList.length; i++) {
            if ( classesList[i].equals(classn) ) {
                classlist.setSelectedIndex(i);
                classlist.ensureIndexIsVisible(i);
                break;
            }
        }
    }

    public void toFront() {
        if ( frame != null )
            frame.toFront();
        else
        if ( iframe != null )
            iframe.toFront();
    }

    class PackageTree extends JTree
    {
        TreeNode root;
        DefaultTreeModel treeModel;
        Map nodeForPackage = new HashMap();

        PackageTree( Collection packages ) {
            setPackages( packages );

            setRootVisible(false);
            setShowsRootHandles(true);
            setExpandsSelectedPaths(true);

            // open top level paths
            /*
            Enumeration e1=root.children();
            while( e1.hasMoreElements() ) {
                TreePath tp = new TreePath(
                    treeModel.getPathToRoot( (TreeNode)e1.nextElement() ) );
                expandPath( tp );
            }
            */
        }

        public void setPackages( Collection packages ) {
            treeModel = makeTreeModel(packages);
            setModel( treeModel );
        }

        DefaultTreeModel makeTreeModel( Collection packages )
        {
            Map packageTree = new HashMap();

            Iterator it=packages.iterator();
            while( it.hasNext() ) {
                String pack = (String)(it.next());
                String [] sa = pack.split( "\\." );
                Map level=packageTree;
                for (int i=0; i< sa.length; i++ ) {
                    String name = sa[i];
                    Map map=(Map)level.get( name );

                    if ( map == null ) {
                        map=new HashMap();
                        level.put( name, map );
                    }
                    level = map;
                }
            }

            root = makeNode( packageTree, "root" );
            mapNodes(root);
            return new DefaultTreeModel( root );
        }


        MutableTreeNode makeNode( Map map, String nodeName )
        {
            DefaultMutableTreeNode root =
                new DefaultMutableTreeNode( nodeName );
            Iterator it=map.keySet().iterator();
            while(it.hasNext() ) {
                String name = (String)it.next();
                Map val = (Map)map.get(name);
                if ( val.size() == 0 ) {
                    DefaultMutableTreeNode leaf =
                        new DefaultMutableTreeNode( name );
                    root.add( leaf );
                } else {
                    MutableTreeNode node = makeNode( val, name );
                    root.add( node );
                }
            }
            return root;
        }

        /**
            Map out the location of the nodes by package name.
            Seems like we should be able to do this while we build above...
            I'm tired... just going to do this.
        */
        void mapNodes( TreeNode node ) {
            addNodeMap( node );

            Enumeration e = node.children();
            while(e.hasMoreElements()) {
                TreeNode tn = (TreeNode)e.nextElement();
                mapNodes( tn );
            }
        }

        /**
            map a single node up to the root
        */
        void addNodeMap( TreeNode node ) {

            StringBuilder sb = new StringBuilder();
            TreeNode tn = node;
            while( tn != root ) {
                sb.insert(0, tn.toString() );
                if ( tn.getParent() != root )
                    sb.insert(0, "." );
                tn = tn.getParent();
            }
            String pack = sb.toString();

            nodeForPackage.put( pack, node );
        }

        void setSelectedPackage( String pack ) {
            DefaultMutableTreeNode node =
                (DefaultMutableTreeNode)nodeForPackage.get(pack);
            if ( node == null )
                return;

            TreePath tp = new TreePath(treeModel.getPathToRoot( node ));
            setSelectionPath( tp );
            setClist( pack );

            scrollPathToVisible( tp );
        }

    }

    public void classPathChanged() {
        Set pset = classPath.getPackagesSet();
        ptree.setPackages( pset );
        setClist(null);
    }

}
