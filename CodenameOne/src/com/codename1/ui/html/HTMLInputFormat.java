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
package com.codename1.ui.html;

import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.plaf.UIManager;
import java.util.Enumeration;
import java.util.Vector;

/**
 * This class implements HTML's input format restrictions.
 * These restrictions can be provided in the FORMAT attribute of the INPUT tag and are relevant for textfields.
 * However as the FORMAT tag was deprectaed it is more standard to supply them in the '-wap-input-format' of the input field CSS or Style.
 *
 * @author Ofir Leitner
 */
class HTMLInputFormat {

    /**
     * The allowed literals in an input format defintion
     */
    private static char[] literals = {'a','A','n','N','x','X','m','M'};

    /**
     * The matching allowed character set for each literal
     */
    private static int[] literalConstraints =
                    {FormatConstraint.TYPE_LOWERCASE|FormatConstraint.TYPE_SYMBOL,
                     FormatConstraint.TYPE_UPPERCASE|FormatConstraint.TYPE_SYMBOL,
                     FormatConstraint.TYPE_NUMERIC|FormatConstraint.TYPE_SYMBOL,
                     FormatConstraint.TYPE_NUMERIC,
                     FormatConstraint.TYPE_LOWERCASE|FormatConstraint.TYPE_NUMERIC|FormatConstraint.TYPE_SYMBOL,
                     FormatConstraint.TYPE_UPPERCASE|FormatConstraint.TYPE_NUMERIC|FormatConstraint.TYPE_SYMBOL,
                     FormatConstraint.TYPE_LOWERCASE|FormatConstraint.TYPE_ANY,
                     FormatConstraint.TYPE_UPPERCASE|FormatConstraint.TYPE_ANY
    };

    private int minLength;
    private int maxLength;
    private Vector formatConstraints = new Vector();


    /**
     * This static method is used to create an HTMLInputFormat
     *
     * @param formatString The string representing the format defintion (As taken from the HTML/CSS)
     * @return An HTMLInputFormat object containing all the constraints or null if there are none or if the string is invalid
     */
    static HTMLInputFormat getInputFormat(String formatString) {
        if (formatString==null) {
            return null;
        }
        try {
            HTMLInputFormat format=new HTMLInputFormat(formatString);
            if (format.formatConstraints.size()==0) {
                return null;
            }
            return format;
        } catch (Exception e) {
            System.out.println(e.getMessage()+" at input format string "+formatString);
            return null;
        }
    }

    /**
     * A private constructor, to obtain an HTMLInputFormat object use HTMLInputFormat.getInputFormat
     * 
     * @param formatString The string representing the format defintion (As taken from the HTML/CSS)
     */
    private HTMLInputFormat(String formatString) {
        String count="";
        for(int i=0;i<formatString.length();i++) {
            char c=formatString.charAt(i);
            if (c=='*') {
                if (!count.equals("")) {
                    throw new IllegalArgumentException("Malformed format string. The wildcard * can't appear after any other count indicator.");
                } else {
                    count+=c;
                }
            } else if ((c>='0') && (c<='9')) {
                if (count.equals("*")) {
                    throw new IllegalArgumentException("Malformed format string. Count indicators cannot appear after the wildcard *");
                } else {
                    count+=c;
                }
            } else {
                int constraint=-1;
                for(int j=0;j<literals.length;j++) {
                    if (c==literals[j]) {
                        constraint=literalConstraints[j];
                        break;
                    }
                }
                if (constraint==-1) {
                    throw new IllegalArgumentException("Malformed format string. Unrecognized literal "+c);
                }
                FormatConstraint fc=new FormatConstraint(constraint, count);
                formatConstraints.addElement(fc);
                if (maxLength!=Integer.MAX_VALUE) {
                    if (fc.count==FormatConstraint.COUNT_EXACTLY_ONE) {
                        maxLength++;
                    } else if (fc.count==FormatConstraint.COUNT_NO_LIMIT) {
                        maxLength=Integer.MAX_VALUE;
                    } else {
                        maxLength+=fc.count;
                    }
                }
                if (fc.count==FormatConstraint.COUNT_EXACTLY_ONE) {
                    minLength++;
                }

                count="";

            }
        }
    }

    /**
     * Applies the constrains represented by this object to the given TextArea.
     * After invoking this method the returned TextArea should be used as restrictions are made sometimes on a new object.
     * In case this is a TextField, this method will also set the input modes as needed.
     *  
     * @param ta The TextArea to apply the constraints on.
     * @return An instance of TextArea (Either the given one or a new one) with the constraints.
     */
    TextArea applyConstraints(TextArea ta) {
        int widestConstraint=0;
        for (Enumeration e=formatConstraints.elements();e.hasMoreElements();) {
            FormatConstraint constraint=(FormatConstraint)e.nextElement();
            for(int i=1;i<=16;i*=2) {
                if ((constraint.type & i)!=0) {
                    widestConstraint|=i;
                }
            }
        }

        if (maxLength!=Integer.MAX_VALUE) {
            ta.setMaxSize(maxLength);
        }

        if (widestConstraint==FormatConstraint.TYPE_NUMERIC) { 
            ta.setConstraint(ta.getConstraint()|TextArea.NUMERIC);
        }
        if (ta instanceof TextField) {
            TextField tf= (TextField)ta;
            if (((widestConstraint & FormatConstraint.TYPE_SYMBOL)==0) && ((widestConstraint & FormatConstraint.TYPE_ANY)==0)) { // No symbols allowed
                tf=new TextField(ta.getText()) {
                    protected void showSymbolDialog() { // Block symbols dialog
                    }
                };
                tf.setConstraint(ta.getConstraint());
                ta=tf;
            }

            if ((widestConstraint & FormatConstraint.TYPE_ANY)!=0) {
                if ((widestConstraint & FormatConstraint.TYPE_UPPERCASE)!=0) {
                    tf.setInputMode("ABC");
                } else {
                    tf.setInputMode("abc");
                }
            } else {
                if ((widestConstraint & FormatConstraint.TYPE_LOWERCASE)==0) {
                    excludeInputMode(tf, "abc");
                    excludeInputMode(tf, "Abc");
                }
                if ((widestConstraint & FormatConstraint.TYPE_UPPERCASE)==0) {
                    excludeInputMode(tf, "ABC");
                    excludeInputMode(tf, "Abc");
                }
                if ((widestConstraint & FormatConstraint.TYPE_NUMERIC)==0) {
                    excludeInputMode(tf, "123");
                }
            }
            
        }
        return ta;
        
    }

    /**
     * Excludes the given input mode from the given TextField
     * 
     * @param tf The TextField to work on
     * @param modeToExclude The mode to exclude
     */
    private void excludeInputMode(TextField tf,String modeToExclude) {
        String[] curModes=tf.getInputModeOrder();
        String[] newModes=new String[curModes.length-1];
        int j=0;
        for(int i=0;i<curModes.length;i++) {
            if (!curModes[i].equals(modeToExclude)) {
                if (j<newModes.length) {
                    newModes[j]=curModes[i];
                    j++;
                } else {
                    return; //Mode was not there in the first place
                }
            }
        }
        tf.setInputModeOrder(newModes);
    }

    /**
     * Verifies that the given String conforms to the constraints represented by this object.
     * 
     * @param str The string to verify
     * @return true if the string is valid, false otherwise.
     */
    boolean verifyString(String str) {
        if ((str.length()>maxLength) || (str.length()<minLength)) {
            return false;
        }
        int i=0;
        Enumeration e=formatConstraints.elements();
        if (!str.equals("")) {
            char c=str.charAt(i);
            for (;e.hasMoreElements();) {
                FormatConstraint constraint=(FormatConstraint)e.nextElement();
                if (constraint.count==FormatConstraint.COUNT_EXACTLY_ONE) {
                    if (!verifyChar(c, constraint.type)) {
                        return false;
                    }
                    i++;
                    if (i<str.length()) {
                        c=str.charAt(i);
                    } else {
                        break;
                    }
                } else {
                    int charNum=0;
                    while ((i<str.length()) && (charNum<constraint.count)) {
                        if (!verifyChar(c, constraint.type)) {
                            // Note that if a char doesn't apply to the current constraint it might conform to the next one (since we are in an
                            // "up to X chars" count, so perhaps from the current constraint segment there are less chars, this is why we don't
                            // fail, but break out, and let the char be compared against the next constraint
                            break;
                        }
                        i++;
                        charNum++;
                        if (i<str.length()) {
                            c=str.charAt(i);
                        } else {
                            break;
                        }
                    }

                    if (i>=str.length()) {
                        break;
                    }
                }
            }

            if (i<str.length()) { // Chars left that are not covered by any constraint
                return false;
            }
        }

        // All chars covered, but more perhaps more constraints are still available
        // They will be checked to see if anyone forces to have one more char and if so fail the verification
        while(e.hasMoreElements()) {
            FormatConstraint constraint=(FormatConstraint)e.nextElement();
            if (constraint.count==FormatConstraint.COUNT_EXACTLY_ONE) {
                return false;
            }
        }

        return true;
    }

    /**
     * Verifies the given character. THis method is used by verifyString on each char
     * 
     * @param c The char to verify
     * @param constraint The constraint to verify againts
     * @return true if the char conforms to the given constraint, false otherwise
     */
    private boolean verifyChar(char c,int constraint) {
        if (((constraint & FormatConstraint.TYPE_ANY)!=0) ||
           (((constraint & FormatConstraint.TYPE_NUMERIC)!=0) && (c>='0') && (c<='9')) ||
           (((constraint & FormatConstraint.TYPE_UPPERCASE)!=0) && (c>='A') && (c<='Z')) ||
           (((constraint & FormatConstraint.TYPE_LOWERCASE)!=0) && (c>='a') && (c<='z'))) {
            return true;
        }

        if ((constraint & FormatConstraint.TYPE_SYMBOL)!=0) {
            char[] symbols=TextField.getSymbolTable();
            for(int i=0;i<symbols.length;i++) {
                if (symbols[i]==c) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * A printout of a user-friendly string describing the format
     * 
     * @return a printout of a user-friendly string describing the format
     */
    public String toString() {
        String str="";
        String followedBy="";
        int lastType=-1;
        String lastString="";
        int singlesCount=0;
        for (Enumeration e=formatConstraints.elements();e.hasMoreElements();) {
            FormatConstraint constraint=(FormatConstraint)e.nextElement();
            if (constraint.count==FormatConstraint.COUNT_EXACTLY_ONE) {
                if (lastType!=-1) {
                    if (lastType!=constraint.type) {
                        str+=followedBy+singlesCount+lastString;
                        followedBy=" followed by ";
                        singlesCount=1;
                        lastType=constraint.type;
                        lastString=constraint.toString();
                    } else {
                        singlesCount++;
                    }
                } else { //lastType==-1
                    lastType=constraint.type;
                    lastString=constraint.toString();
                    singlesCount=1;
                }
            } else {
                if (lastType!=-1) {
                    str+=followedBy+singlesCount+lastString;
                    followedBy=" followed by ";
                    lastType=-1;
                    singlesCount=0;
                    lastString="";
                }
                str+=followedBy+constraint.toString();
                followedBy=" followed by ";

            }


        }

        if (lastType!=-1) {
           str+=followedBy+singlesCount+lastString;
        }

        return str;
    }

    // Inner classes:

    /**
     * This class reprensents a single constraint on an input field
     * The HTMLInputFormat breaks down the format string into 1 or more FormatConstraint objects
     *
     * Each constraint is represented by a type and a count.
     * The type indicated which typs of characters are allows and can be one or more of the TYPE_* constants ORed together.
     * The count is either a number that indicates we allow up to this count or one of the COUNT_* constants (See below)
     *
     * @author Ofir Leitner
     */
    class FormatConstraint {

        /**
         * All lowercase english letters are allowed
         */
        static final int TYPE_LOWERCASE = 1;

        /**
         * All uppercase english letters are allowed
         */
        static final int TYPE_UPPERCASE = 2;

        /**
         * All numbers are allowed
         */
        static final int TYPE_NUMERIC = 4;

        /**
         * Symbols according to the TextField symbols table are allowed
         */
        static final int TYPE_SYMBOL = 8;

        /**
         * All characters are allowed
         */
        static final int TYPE_ANY = 16;

        /**
         * A constant representing that there must be one and one only of this type
         */
        static final int COUNT_EXACTLY_ONE=Integer.MIN_VALUE;

        /**
         * A constant representing that there can be 0 or more of this type
         */
        static final int COUNT_NO_LIMIT=Integer.MAX_VALUE;

        int type;
        int count;

        /**
         * The constructor which converts the count string into an integer or one of the COUNT_* constants above
         *
         * @param type The constraint type (one or more of the TYPE_* constants ORed together)
         * @param countStr A string representing the constraint count
         */
        FormatConstraint(int type,String countStr) {
            if (countStr.equals("*")) {
                count=COUNT_NO_LIMIT;
            } else if (countStr.equals("")) {
                count=COUNT_EXACTLY_ONE;
            } else {
                try {
                    count=Integer.parseInt(countStr);
                } catch (NumberFormatException nfe) {
                    System.out.println("Invalid FormatConstraint count "+countStr);
                }
            }
            this.type=type;
        }

        /**
         * A printout of a user-friendly string describing this constraint
         *
         * @return a printout of a user-friendly string describing this constraint
         */
        public String toString() {
            String str="";
            if (count==COUNT_EXACTLY_ONE) {
                //str+="";
            } else if (count==COUNT_NO_LIMIT) {
                str+=UIManager.getInstance().localize("html.format.anynumber", "any number of");
            } else {
                str+=UIManager.getInstance().localize("html.format.upto", "up to")+" "+count;
            }
            str+=" ";
            String orString=" "+UIManager.getInstance().localize("html.format.or", "or")+" ";
            String or="";
            if ((type & TYPE_ANY)!=0) {
                str+="any";
            } else {
                if ((type & TYPE_LOWERCASE)!=0) {
                    str+=UIManager.getInstance().localize("html.format.lowercase", "lowercase");
                    or=orString;
                }
                if ((type & TYPE_UPPERCASE)!=0) {
                    str+=or+UIManager.getInstance().localize("html.format.uppercase", "uppercase");
                    or=orString;
                }
                if ((type & TYPE_NUMERIC)!=0) {
                    str+=or+UIManager.getInstance().localize("html.format.numeric", "numeric");
                    or=orString;
                }
                if ((type & TYPE_SYMBOL)!=0) {
                    str+=or+UIManager.getInstance().localize("html.format.symbol", "symbol");
                }
            }
            str+=" ";

            if ((count!=COUNT_EXACTLY_ONE) && (count!=1)) {
                str+=UIManager.getInstance().localize("html.format.chars", "characters");
            } else {
                str+=UIManager.getInstance().localize("html.format.char", "character");
            }

            return str;
        }

    }


}

