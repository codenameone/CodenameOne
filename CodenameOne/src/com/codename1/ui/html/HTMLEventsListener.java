package com.codename1.ui.html;

import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.List;
import com.codename1.ui.RadioButton;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.events.SelectionListener;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class serves as a aggregator of all the event listeners for all the active components within the HTML page.
 * It is in itself an ActionListener and a FocusListener, and it creates SelectionListeners and DataChangedListeners if needed.
 * Upon events it both updates the DOM if needed, and dispatches event methods on HTMLCallback
 *
 * @author Ofir Leitner
 */
class HTMLEventsListener implements ActionListener,FocusListener {

    Hashtable comps = new Hashtable();
    Hashtable listeners = new Hashtable();
    HTMLComponent htmlC;

    public HTMLEventsListener(HTMLComponent htmlC) {
        this.htmlC=htmlC;
    }

    /**
     * Registeres the specified component/element duo to listen to all available events
     *
     * @param cmp The actual component
     * @param element The element representing the component
     */
    void registerComponent(final Component cmp,final HTMLElement element) {
        comps.put(cmp, element);
        cmp.addFocusListener(this);
        if (cmp instanceof Button) { // catches Button, CheckBox, RadioButton
            ((Button)cmp).addActionListener(this);
        } else if (cmp instanceof List) { // catches ComboBox
            final List list = (List)cmp;
            list.addActionListener(this);
            SelectionListener sl = new SelectionListener() { // We create a listener and not listen ourself since the listener's method does not pass the event origin, so we need to make one listener per component
                public void selectionChanged(int oldSelected, int newSelected) {
                    if (htmlC.getHTMLCallback()!=null) {
                        htmlC.getHTMLCallback().selectionChanged(oldSelected, newSelected, htmlC, list, element);
                    }
                }
            };
            list.addSelectionListener(sl);
            listeners.put(cmp, sl);

        } else if (cmp instanceof TextArea) {
            ((TextArea)cmp).addActionListener(this);
            if (cmp instanceof TextField) {
                final TextField tf = (TextField)cmp;
                DataChangedListener dcl = new DataChangedListener() { // We create a listener and not listen ourself since the listener's method does not pass the event origin, so we need to make one listener per component
                    public void dataChanged(int type, int index) {
                        element.setAttributeById(HTMLElement.ATTR_VALUE, tf.getText());
                        if (htmlC.getHTMLCallback()!=null) {
                            htmlC.getHTMLCallback().dataChanged(type, index, htmlC, tf, element);
                        }
                    }
                };
                tf.addDataChangeListener(dcl);
                listeners.put(cmp, dcl);
            }
        }
    }

    /**
     * Deregisters all the listeners, happens before a new page is loaded
     */
    void deregisterAll() {
        for(Enumeration e=comps.keys();e.hasMoreElements();) {
            Component cmp = (Component)e.nextElement();
            cmp.removeFocusListener(this);
            if (cmp instanceof Button) { // catches Button, CheckBox, RadioButton
                ((Button)cmp).removeActionListener(this);
            } else if (cmp instanceof List) { // catches ComboBox
                ((List)cmp).removeSelectionListener((SelectionListener)listeners.get(cmp));
            } else if (cmp instanceof TextArea) {
                ((TextArea)cmp).removeActionListener(this);
                if (cmp instanceof TextField) {
                    ((TextField)cmp).removeDataChangeListener((DataChangedListener)listeners.get(cmp));
                }
            }
        }
        comps=new Hashtable();
        listeners=new Hashtable();
    }

    private void toggleChecked(HTMLElement element,boolean checkedX) {
        if (checkedX) {
            element.setAttributeById(HTMLElement.ATTR_CHECKED, "checked");
        } else {
            element.removeAttributeById(HTMLElement.ATTR_CHECKED);
        }

    }

    /**
     * {{@inheritDoc}}
     */
    public void actionPerformed(ActionEvent evt) {
        Object src = evt.getSource();
        HTMLElement element=(HTMLElement)comps.get(evt.getSource());
        if (src instanceof CheckBox) {
            toggleChecked(element,((CheckBox)src).isSelected());
        } else if (src instanceof RadioButton) {
            String curDomState = element.getAttributeById(HTMLElement.ATTR_CHECKED);
            if ((curDomState==null) || (!curDomState.equals("checked"))) {
                String name=element.getAttributeById(HTMLElement.ATTR_NAME);
                if (name!=null) { // If this is named radiobutton, we need to set the status of the others accordingly
                    for(Enumeration e=comps.keys();e.hasMoreElements();) {
                        Component cmp = (Component)e.nextElement();
                        if (cmp instanceof RadioButton) {
                            HTMLElement rbElem = (HTMLElement)comps.get(cmp);
                            String rbName=rbElem.getAttributeById(HTMLElement.ATTR_NAME);
                            if ((rbName!=null) && (rbName.equals(name))) {
                                rbElem.removeAttributeById(HTMLElement.ATTR_CHECKED);
                            }
                        }
                    }
                }
            }
            toggleChecked(element, ((RadioButton)src).isSelected());
            //element.setAttributeById(HTMLElement.ATTR_CHECKED, ((RadioButton)src).isSelected()?"checked":null);
        } else if (src instanceof TextArea) {
            String text=((TextArea)src).getText();
            if (element.getNumChildren()==0) {
                HTMLElement textElem=new HTMLElement(text, true);
                element.addChild(textElem);
            } else {
                HTMLElement textElem=(HTMLElement)element.getChildAt(0);
                if (textElem.isTextElement()) { // If the HTML is malformed we may have a different element - and we ignore
                    textElem.setText(text);
                } else {
                    System.out.println("Malformed HTML - Found a non-text element under TEXTAREA tag - ignoring");
                }
            }
        } else if (src instanceof List) { //combobox
            String item = ((List)src).getSelectedItem().toString();
            Vector v=element.getDescendantsByTagId(HTMLElement.TAG_OPTION); // This is activated on the SELECT tag - we take descendants and not only children due to OPTGROUP
            for(Enumeration e=v.elements();e.hasMoreElements();) {
                HTMLElement option=(HTMLElement)e.nextElement();
                if (option.getNumChildren()==1) { //we expect only a text element, if not the HTML is malformed and we ignore
                    HTMLElement textElem=(HTMLElement)option.getChildAt(0);
                    if (textElem.isTextElement()) { // If the HTML is malformed we may have a different element - and we ignore
                        if (textElem.getText().equalsIgnoreCase(item)) {
                            option.setAttributeById(HTMLElement.ATTR_SELECTED, "selected");
                        } else {
                            option.removeAttributeById(HTMLElement.ATTR_SELECTED);
                        }
                    } else {
                        System.out.println("Malformed HTML - Found a non-text element under OPTION tag - ignoring");
                    }
                } else {
                    System.out.println("Malformed HTML - Found illegal tags as children of the OPTION tag - ignoring");
                }
            }
        }
        if (htmlC.getHTMLCallback()!=null) {
            htmlC.getHTMLCallback().actionPerformed(evt, htmlC, element);
        }
    }

    /**
     * {{@inheritDoc}}
     */
    public void focusGained(Component cmp) {
        if (htmlC.getHTMLCallback()!=null) {
            htmlC.getHTMLCallback().focusGained(cmp, htmlC, (HTMLElement)comps.get(cmp));
        }
    }

    /**
     * {{@inheritDoc}}
     */
    public void focusLost(Component cmp) {
        if (htmlC.getHTMLCallback()!=null) {
            htmlC.getHTMLCallback().focusLost(cmp, htmlC, (HTMLElement)comps.get(cmp));
        }
    }

}