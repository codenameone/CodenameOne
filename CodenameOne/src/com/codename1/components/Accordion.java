/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.components;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;

/**
 * The accordion ui pattern is a vertically stacked list of items. 
 * Each item can be opened/closed to reveal more content 
 * 
 * @author Chen
 */
public class Accordion extends Container {

    private Image closedIcon;

    private Image openedIcon;

    private boolean autoClose = true;

    /**
     * Empty Constructor
     */ 
    public Accordion() {
        super.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        closedIcon = FontImage.createMaterial(FontImage.MATERIAL_KEYBOARD_ARROW_RIGHT, UIManager.getInstance().getComponentStyle("Label"));
        openedIcon = FontImage.createMaterial(FontImage.MATERIAL_KEYBOARD_ARROW_DOWN, UIManager.getInstance().getComponentStyle("Label"));
    }

    /**
     * Add an item to the Accordion Container
     * 
     * @param header the item title
     * @param body the item Component to hide/show
     */ 
    public void addContent(String header, Component body) {
        addContent(new Label(header), body);
    }

    /**
     * Add an item to the Accordion Container
     * 
     * @param header the item title Component
     * @param body the item Component to hide/show
     */ 
    public void addContent(Component header, Component body) {        
        add(new AccordionContent(header, body));
    }

    /**
     * Sets the closed icon
     * @param closedIcon closed icon
     */ 
    public void setClosedIcon(Image closedIcon) {
        this.closedIcon = closedIcon;
    }

    /**
     * Sets the opened icon
     * @param openedIcon opened icon
     */ 
    public void setOpenedIcon(Image openedIcon) {
        this.openedIcon = openedIcon;
    }

    /**
     * Sets the auto close flag, if this flag is true clicking on an item to open 
     * an item will automatically close the previous opened item.
     * 
     * @param autoClose determines if more then 1 item can be opened on screen
     */ 
    public void setAutoClose(boolean autoClose) {
        this.autoClose = autoClose;
    }
    
    

    class AccordionContent extends Container {
        
        private boolean closed = true;
        
        private Button arrow = new Button();
        
        private Component body;
            
        public AccordionContent(final Component header, final Component body) {
            setLayout(new BorderLayout());
            this.body = body;
            header.setSelectedStyle(header.getUnselectedStyle());
            header.setPressedStyle(header.getUnselectedStyle());
            Container top = new Container(new BorderLayout());
            top.add(BorderLayout.CENTER, header);
            arrow.setUIID("Label");
            arrow.setIcon(closedIcon);
            arrow.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    
                    //toggle the current
                    openClose(!isClosed());
                    if(autoClose){
                        for (int i = 0; i < Accordion.this.getComponentCount(); i++) {
                            AccordionContent c = (AccordionContent)Accordion.this.getComponentAt(i);
                            if(c != AccordionContent.this && !c.isClosed()){
                                c.openClose(true);
                            }
                        }
                    }
                    Accordion.this.animateLayout(250);
                }
            });
            top.add(BorderLayout.EAST, arrow);
            top.setLeadComponent(arrow);
            add(BorderLayout.NORTH, top);
            body.setHidden(true);
            add(BorderLayout.CENTER, body);
        }

        public boolean isClosed() {
            return closed;
        }
        
        public void openClose(boolean close) {
            closed = close;
            if (closed) {
                arrow.setIcon(closedIcon);
            } else {
                arrow.setIcon(openedIcon);
            }
            body.setHidden(closed);
        }


    }

}
