/*
 * Copyright (c) 1998-2002 Carnegie Mellon University.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY CARNEGIE MELLON UNIVERSITY ``AS IS'' AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package rcm.awt;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import rcm.util.Win;

// Note: after creating a PopupDialog (like any other top-level window, it
// seems), the JDK 1.1 runtime won't exit by itself, even if the PopupDialog
// is properly disposed.  Need to force it to exit using System.exit().

public class PopupDialog extends Dialog {

    public static final int YES = 0;
    public static final int OK = 0;
    public static final int NO = 1;
    public static final int CANCEL = 2;

    Component parent;
    int answer = -1;
    String text;

    TextField textfield;
    Button okButton, noButton, cancelButton;

    public static String ask (Component comp, String topic, String question, String defaultAnswer) {
        PopupDialog d = new PopupDialog (comp, topic, true,
                                         question, defaultAnswer,
                                         "OK", null, "Cancel");
        d.show ();
        switch (d.getAnswer ()) {
            case OK:
                return d.getText ();
            default:
                return null;
        }
    }

    public static String ask (Component comp, String topic, String question) {
        return ask (comp, topic, question, "");
    }

    public static boolean okcancel (Component comp, String topic, String question) {
        PopupDialog d = new PopupDialog (comp, topic, true,
                                         question, null,
                                         "OK", null, "Cancel");
        d.show ();
        return (d.getAnswer () == OK);
    }

    public static boolean yesno (Component comp, String topic, String question) {
        PopupDialog d = new PopupDialog (comp, topic, true,
                                         question, null,
                                         "Yes", "No", null);
        d.show ();
        return (d.getAnswer () == YES);
    }

    public static int yesnocancel (Component comp, String topic, String question) {
        PopupDialog d = new PopupDialog (comp, topic, true,
                                         question, null,
                                         "Yes", "No", "Cancel");
        d.show ();
        return d.getAnswer ();
    }

    public static void warn (Component comp, String topic, String message) {
        PopupDialog d = new PopupDialog (comp, topic, true,
                                         message, null,
                                         "OK", null, null);
        d.show ();
    }

    public static String currentDirectory = "";

    public static String askFilename (Component comp, String topic,
                                  String defaultFilename, boolean loading) {
        try {
            FileDialog fd = new FileDialog (Win.findFrame(comp),
                                            topic,
                                            loading ? FileDialog.LOAD : FileDialog.SAVE);

            if (currentDirectory != null)
                fd.setDirectory (currentDirectory);
            if (defaultFilename != null)
                fd.setFile (defaultFilename);

            fd.show ();

            String dir = fd.getDirectory();
            String file = fd.getFile ();

            if (dir == null || file == null)
                return null;

            currentDirectory = dir;
            return dir + file;
        } catch (AWTError e) {
            return ask (comp, topic, "Filename:", defaultFilename);
        }
    }

    public static String askDirectory (Component comp, String topic,
                                  String defaultFilename, boolean loading) {
        try {
            FileDialog fd = new FileDialog (Win.findFrame(comp),
                                            topic,
                                            loading ? FileDialog.LOAD : FileDialog.SAVE);

            if (currentDirectory != null)
                fd.setDirectory (currentDirectory);
            if (defaultFilename != null)
                fd.setFile (defaultFilename);

            fd.show ();

            String dir = fd.getDirectory();

            if (dir != null)
                currentDirectory = dir;

            return dir;
        } catch (AWTError e) {
            return ask (comp, topic, "Directory:", defaultFilename);
        }
    }


    public PopupDialog (Component parent, String title, boolean modal) {
        super (Win.findFrameOrMakeFrame (parent), title, modal);
        this.parent = parent;
    }
    
    public PopupDialog (Component parent, String title, boolean modal,
                         String question, String initialEntry,
                         String okOrYes, String no, String cancel) {
        this (parent, title, modal);

        if (parent != null)
            setFont (parent.getFont ());

        Panel middle = new Panel ();
        add ("Center", BorderPanel.wrap (middle, 10, 10, 10, 5));
        middle.setLayout (new BorderLayout ());
        MultiLineLabel questionLabel = new MultiLineLabel (question, Label.LEFT);
        middle.add ("Center", questionLabel);
        if (initialEntry != null) {
            textfield = new TextField (Math.max (40, initialEntry.length()+1));
            middle.add ("South", textfield);
            textfield.setText (initialEntry);
            textfield.selectAll ();
            textfield.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent event) {
                    answer = OK;
                    close ();
                }
            });
        }

        Panel bottom = new Panel ();
        add ("South", bottom);

        if (okOrYes != null) {
            okButton = new Button (okOrYes);
            okButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent event) {
                    answer = OK;
                    close ();
                }
            });
            bottom.add (okButton);
        }

        if (no != null) {
            noButton = new Button (no);
            noButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent event) {
                    answer = NO;
                    close ();
                }
            });
            bottom.add (noButton);
        }

        if (cancel != null) {
            cancelButton = new Button (cancel);
            cancelButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent event) {
                    answer = CANCEL;
                    close ();
                }
            });
            bottom.add (cancelButton);
        }

        addWindowListener (new WindowAdapter () {
            public void windowClosing (WindowEvent event) {
                if (cancelButton != null) {
                    answer = CANCEL;
                    close ();
                }
                else if (noButton == null && cancelButton == null) {
                    answer = OK;
                    close ();
                }
            }
        });

//         if (System.getProperty ("java.vendor").startsWith ("Netscape")) {
//             // pack() doesn't work under Netscape!
//             Dimension d = questionLabel.preferredSize();
//             resize (Math.max (100, d.width), 100 + d.height);
//         }
//         else
        pack ();
    }

    public static void centerWindow (Window window, Component ref) {
        Dimension size = window.getSize();
        Dimension refSize = (ref != null) ? ref.getSize() : Toolkit.getDefaultToolkit().getScreenSize();
        Point origin = (ref != null) ? ref.getLocationOnScreen () : new Point (0, 0);
        
        if (refSize != null) {
            int x = Math.max (0, origin.x + (refSize.width - size.width) / 2);
            int y = Math.max (0, origin.y + (refSize.height - size.height) / 2);
            window.setLocation (x, y);
        }
    }
    
    public void show () {
        centerWindow (this, parent);
        super.show ();
        if (textfield != null)
            textfield.requestFocus ();
    }

    public int getAnswer () {
        return answer;
    }

    public void setAnswer (int answer) {
        this.answer = answer;
    }

    public String getText () {
        return text;
    }

    Vector listeners = new Vector ();

    public synchronized void addPopupListener (PopupListener listener) {
        listeners.addElement (listener);
    }

    public synchronized void removePopupListener (PopupListener listener) {
        listeners.removeElement (listener);
    }

    public synchronized void close () {
        text = (answer == OK && textfield != null)
                 ? textfield.getText () : null;

        dispose ();
        if (parent == null)
            ((Frame)getParent()).dispose ();
        else
            parent.requestFocus ();

        if (answer != -1) {
            PopupEvent e = new PopupEvent (answer, text);
            for (int i=0; i<listeners.size (); ++i) {
                PopupListener p = (PopupListener) (listeners.elementAt (i));
                switch (e.getID ()) {
                    case YES:
                        p.yes (e);
                        break;
                    case NO:
                        p.no (e);
                        break;
                    case CANCEL:
                        p.cancel (e);
                        break;
                }
            }
        }

        try {
            finalize ();
        } catch (Throwable t) {
            throw new RuntimeException (t.toString());
        }
    }


    /*
     * Testing
     *
     */
    public static void main (String[] args) {
        String name = ask (null, "Enter Name", "Enter your full name:");

        if (name != null) {
            switch (yesnocancel (null, "Confirm",
                                 "Hello, " + name + ".\nIs this your name?")) {
                case PopupDialog.YES:
                    if (okcancel (null, "Thanks",
                                  "Great!\nDo you want to play a game?")) {
                        warn (null, "Sorry", "Too bad, my mommy won't let me out of the house.");
                    }
                    break;

                case PopupDialog.NO:
                    warn (null, "D'oh", "Oops.  My bad.");
                    break;
            }
        }

        System.exit (0);
    }

}
