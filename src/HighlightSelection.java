import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

public class HighlightSelection extends JFrame {
    private String dateStr;
    private Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private File logfile;

    private void guiCreation(HighlightSelection hs){
        // Getting current date (year)
        SimpleDateFormat sdate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateStr = sdate.format(new Date()).substring(0, 4);

        // Creating main window layout
        JPanel mainL = new JPanel(new BorderLayout());

        // Creating center panel
        JPanel centerPanel = new JPanel(new GridLayout(3, 1));
        centerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Upload a logfile"),
            BorderFactory.createEmptyBorder(5,5,5,5)
        ));

        // Adding the file upload section
        JFileChooser jfc = new JFileChooser();
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        // Setting default fille chooser directory
        File workingDirectory = new File(System.getProperty("user.dir"));
        jfc.setCurrentDirectory(workingDirectory);

        JButton openBtn = new JButton(createImageIcon("reference/folder.png"));
        openBtn.setPreferredSize(new Dimension(40, 40));
    
        JLabel openBtnLabel = new JLabel("Open an existing logfile... ");
        openBtnLabel.setLabelFor(openBtn);
        openBtnLabel.setFont(font);

        // FlowLayout to keep the label and button in-line
        JPanel openBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        openBtnPanel.add(openBtnLabel);
        openBtnPanel.add(openBtn);
        centerPanel.add(openBtnPanel);

        // TextFields and Labels for start and end time fields
        JTextField sTime = new JTextField(15);
        JTextField eTime = new JTextField(15);
        JLabel sTimeLabel = new JLabel("Start Time (hh:mm) ");
        JLabel eTimeLabel = new JLabel("End Time   (hh:mm) ");

        sTimeLabel.setLabelFor(sTime);
        sTimeLabel.setFont(font);
        eTimeLabel.setLabelFor(eTime);
        eTimeLabel.setFont(font);
        
        // Adding a panel with each field to the main layout
        JPanel sTimePanel = new JPanel(new FlowLayout());
        JPanel eTimePanel = new JPanel(new FlowLayout());
        sTimePanel.add(sTimeLabel);
        sTimePanel.add(sTime);
        eTimePanel.add(eTimeLabel);
        eTimePanel.add(eTime);
        centerPanel.add(sTimePanel);
        centerPanel.add(eTimePanel);

        //Listener for the file selection button
        openBtn.addActionListener(e -> {
            if (e.getSource() == openBtn){
                int returnVal = jfc.showOpenDialog(hs);

                // Stores the selected file under logfile
                if (returnVal == JFileChooser.APPROVE_OPTION){
                    logfile = jfc.getSelectedFile();
                    openBtnLabel.setText(logfile.getName());
                    openBtn.setIcon(createImageIcon("reference/file.png"));
                }
            }
        });

        // Creating a panel for submission button
        JPanel submitPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton submitButton = new JButton("Highlight");
        submitPanel.add(submitButton);

        // Runs the highlight if fields have been filled out correctly
        submitButton.addActionListener((ActionEvent e) -> {
                    if (!sTime.getText().isEmpty() && !eTime.getText().isEmpty() && logfile.exists()) {
                        String sText = sTime.getText().replaceAll("\\s+", "");
                        String eText = eTime.getText().replaceAll("\\s+", "");
                        if (!sText.matches("^\\d{2}:\\d{2}") || !eText.matches("^\\d{2}:\\d{2}")) {
                            errorReport("Start and End times should be of the form hh:mm", hs);
                            System.exit(0);
                        }
                        // Get text with fRead, pass it to highlightText
                        hs.dispose();
                        String logText = fRead(sText, eText, hs);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                highlightText(logText, hs);
                            }
                        });
                    } else if (sTime.getText().isEmpty() && eTime.getText().isEmpty() && logfile.exists())
                    {
                        hs.dispose();
                        String logText = readFull(hs);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                highlightText(logText, hs);
                            }
                        });
                    } else if (sTime.getText().isEmpty() && !eTime.getText().isEmpty() && logfile.exists()) {
                        String eText = eTime.getText().replaceAll("\\s+", "");
                        if (!eText.matches("^\\d{2}:\\d{2}")) {
                            errorReport("End time should be of the form hh:mm", hs);
                            System.exit(0);
                        }
                        hs.dispose();
                        String logText = readPart(1, eText, hs);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                highlightText(logText, hs);
                            }
                        });
                    } else if(!sTime.getText().isEmpty() && eTime.getText().isEmpty() && logfile.exists()) {
                        String sText = sTime.getText().replaceAll("\\s+", "");
                        if (!sText.matches("^\\d{2}:\\d{2}")) {
                            errorReport("Start time should be of the form hh:mm", hs);
                            System.exit(0);
                        }
                        hs.dispose();
                        String logText = readPart(0, sText, hs);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                highlightText(logText, hs);
                            }
                        });
                    }
                });

        // Adding everything to the main panel
        mainL.add(centerPanel, BorderLayout.CENTER);
        mainL.add(submitPanel, BorderLayout.PAGE_END);
        
        // Settings for the window
        hs.setSize(550, 450);
        hs.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        hs.setTitle("Log Multi-Highlight");
        hs.add(mainL);
        hs.setLocationRelativeTo(null);
        hs.setVisible(true);
    }

    /**
     * Reads the logfile when only one input field was filled
     * @param elementGiven 0 if startTime was given, 1 if endTime was given
     * @param inputField The text from the field that wasn't empty
     * @param hs HighlightSelection object
     * @return Text from the logfile depending on input given
     */
    private String readPart(int elementGiven, String inputField, HighlightSelection hs){
        String text = "";
        try{
            // Reading from the uploaded file
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(logfile));
            int readByte = bis.read();
            StringBuilder outputBuffer = new StringBuilder();

            while (readByte != 0xfffffff){
                outputBuffer.append((char) readByte);
                readByte = bis.read();
            }
            bis.close();
            if (elementGiven == 0){
                if (outputBuffer.toString().contains(inputField)) {
                    text = outputBuffer.toString().substring(outputBuffer.toString().indexOf(inputField + ":"));
                } else {
                    //TODO
                }
            } else if (elementGiven == 1){
                text = outputBuffer.toString().substring(0, outputBuffer.toString().lastIndexOf(inputField + ":") - 15);
            }

        } catch(IOException e){
            errorReport(e.getMessage(), hs);
        }
        return text;
    }

    /**
     * Reads the full logfile
     * @param hs HighlightSelection object
     * @return The full text from the given logfile
     */
    private String readFull(HighlightSelection hs){
        String text = "";
        try {
            // Reading from the uploaded file
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(logfile));
            int readByte = bis.read();
            StringBuilder outputBuffer = new StringBuilder();

            while (readByte != 0xffffffff) {
                outputBuffer.append((char) readByte);
                readByte = bis.read();
            }
            bis.close();
            text = outputBuffer.toString();
        } catch(IOException e){
            errorReport(e.getMessage(), hs);
        }
        return text;
    }

    /**
     * Reads the logfile between a given start and endTime
     * @param startTime The time to start the reading from
     * @param endTime The time to end the reading at
     * @return All the text between the given start and end times
     */
    private String fRead(String startTime, String endTime, HighlightSelection hs){
        String text = "";
        try {
            // Reading from the uploaded file
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(logfile));
            int readByte = bis.read();
            StringBuilder outputBuffer = new StringBuilder();

            while (readByte != 0xffffffff) {
                outputBuffer.append((char) readByte);
                readByte = bis.read();
            }
            bis.close();
            // Adding ':' to the end of the given start and end times so searching is more accurate
            String uSTime = startTime + ":";
            String uETime = endTime + ":";

            // If statements adjust start and endTimes depending on whether the ones given are found in the log or not
            if (outputBuffer.toString().contains(uSTime) && outputBuffer.toString().contains(uETime)) {
                text = outputBuffer.toString().substring(outputBuffer.toString().indexOf(uSTime), outputBuffer.toString().lastIndexOf(uETime) - 15);
            } else if (!outputBuffer.toString().contains(uSTime) && outputBuffer.toString().contains(uETime)) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:");
                Date uSDate = new Date();
                try {
                    uSDate = sdf.parse(startTime + ":");
                } catch (ParseException e) {
                    System.err.println(e.getMessage());
                }
                Calendar c = Calendar.getInstance();
                c.setTime(uSDate);
                while (Integer.parseInt(sdf.format(c.getTime()).substring(3, 5)) < 59) {
                    c.add(Calendar.MINUTE, 1);
                    if (outputBuffer.toString().contains(sdf.format(c.getTime()))) {
                        text = outputBuffer.toString().substring(outputBuffer.toString().indexOf(sdf.format(c.getTime())), outputBuffer.toString().lastIndexOf(uETime) - 15);
                        break;
                    }
                }
                if (Integer.parseInt(sdf.format(c.getTime()).substring(3, 5)) == 59) {
                    errorReport("No start times found within the hour specified", hs);
                }
            } else if (outputBuffer.toString().contains(uSTime) && !outputBuffer.toString().contains(uETime)) {
                if (outputBuffer.toString().lastIndexOf(uETime.substring(0, 3)) > 0) {
                    text = outputBuffer.toString().substring(outputBuffer.toString().indexOf(uSTime), outputBuffer.toString().lastIndexOf(" " + uETime.substring(0, 3)) - 15);
                } else {
                    errorReport("No end times found within the hour specified", hs);
                }
            } else if (!outputBuffer.toString().contains(uSTime) && !outputBuffer.toString().contains(uETime)) {
                if (outputBuffer.toString().lastIndexOf(uETime.substring(0, 3)) > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:");
                    Date uSDate = new Date();
                    try {
                        uSDate = sdf.parse(startTime + ":");
                    } catch (ParseException e) {
                        System.err.println(e.getMessage());
                    }
                    Calendar c = Calendar.getInstance();
                    c.setTime(uSDate);
                    while (Integer.parseInt(sdf.format(c.getTime()).substring(3, 5)) < 59) {
                        c.add(Calendar.MINUTE, 1);
                        if (outputBuffer.toString().contains(sdf.format(c.getTime()))) {
                            text = outputBuffer.toString().substring(outputBuffer.toString().indexOf(sdf.format(c.getTime())), outputBuffer.toString().lastIndexOf(" " + uETime.substring(0, 3)) - 15);
                            break;
                        }
                    }

                    if (Integer.parseInt(sdf.format(c.getTime()).substring(3, 5)) == 59) {
                        errorReport("No start times found within the hour specified", hs);
                    }
                } else {
                    errorReport("No end times found within the hour specified", hs);
                }

            }
        }catch (IOException e){
            // Sends error message as a pop-up
            errorReport(e.getMessage(), hs);
        }
        return text;
    }

    /**
     * Highlights the text based on a user selection
     * @param text The text from the log between a given start and end time
     * @param hs A HighlightSelection object
     */
    private void highlightText(String text, HighlightSelection hs){
        // Making sure fRead was successful
        if (text.isEmpty()){
            errorReport("Error: Text is null", hs);
        }

        HighlightSelection ht = new HighlightSelection();
        ht.setLayout(new BorderLayout());

        // Tags are array lists of objects used to keep track of each highlight
        List<Object> hl_tag_blue = new ArrayList<>();
        List<Object> hl_tag_purple = new ArrayList<>();
        List<Object> hl_tag_green = new ArrayList<>();

        // Displaying the text
        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setFont(font);
        ta.setMargin(new Insets(10,10,10,10));
        ta.setCursor(new Cursor(Cursor.TEXT_CURSOR));
        ta.setSelectionColor(new Color(255, 255, 0));
        ta.setText(text);

        JScrollPane taScroll = new JScrollPane(ta);
        taScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        Highlighter hl = ta.getHighlighter();

        // 3 Different colour highlights
        Highlighter.HighlightPainter paint_blue = new DefaultHighlighter.DefaultHighlightPainter(new Color(19, 247, 235, 150));
        Highlighter.HighlightPainter paint_purple = new DefaultHighlighter.DefaultHighlightPainter(new Color(228, 112, 255, 150));
        Highlighter.HighlightPainter paint_green = new DefaultHighlighter.DefaultHighlightPainter(new Color(50, 252, 80, 150));

        ht.add(taScroll, BorderLayout.CENTER);
        JPanel flow = new JPanel();
        flow.setLayout(new FlowLayout());

        // Initializing panel and textfield for the search bar
        JPanel search = new JPanel(new FlowLayout());
        JTextField searchField = new JTextField(30);

        // Search submit button
        JButton searchButton = new JButton("Search");
        search.add(searchField);
        search.add(searchButton);

        searchButton.addActionListener(e -> {
            // If user has entered text in the search key
            if (!searchField.getText().isEmpty()){
                String searchKey = searchField.getText();
                // Finding the index of the searchKey
                int indexI = ta.getText().toLowerCase().indexOf(searchKey.toLowerCase());
                int indexF = indexI + searchKey.length();

                // Focus textfield before selecting
                ta.requestFocusInWindow();

                // Selecting the searched text
                ta.select(indexI, indexF);
            }
        });

        // Buttons to select each colour, and a final one to clear all
        JButton btn_1 = new JButton("Key 1");
        btn_1.setBackground(new Color(19, 247, 235));
        JButton btn_2 = new JButton("Key 2");
        btn_2.setBackground(new Color(228, 112, 255));
        JButton btn_3 = new JButton("Key 3");
        btn_3.setBackground(new Color(50, 252, 80));
        JButton btn_clear = new JButton("Clear All");

        flow.add(btn_1);
        flow.add(btn_2);
        flow.add(btn_3);
        flow.add(btn_clear);


        // Creating action listeners for each button
        btn_1.addActionListener(e -> {
            // Delete previous highlights of this colour, if any exist
            if (!hl_tag_blue.isEmpty()) {
                for (Object hlb : hl_tag_blue) {
                    hl.removeHighlight(hlb);
                }
            }
            // Highlighting the text selected by user
            if (ta.getSelectedText() != null) {
                String pattern = ta.getSelectedText();
                int occurrence = text.indexOf(pattern);
                while (occurrence >= 0) {
                    try {
                        hl_tag_blue.add(hl.addHighlight(occurrence, occurrence + pattern.length(), paint_blue));
                        occurrence = text.indexOf(pattern, occurrence + pattern.length());
                    } catch (BadLocationException ex) {
                        errorReport(ex.getMessage(), ht);
                    }
                }
            }
        });

        // See comments for btn_1
        btn_2.addActionListener(e -> {
            if (e.getSource() == btn_2) {

                if (!hl_tag_purple.isEmpty()) {
                    for (Object hlp : hl_tag_purple) {
                        hl.removeHighlight(hlp);
                    }
                }
                if (ta.getSelectedText() != null) {
                    String pattern = ta.getSelectedText();
                    int occurrence = text.indexOf(pattern);
                    while (occurrence >= 0) {
                        try {
                            hl_tag_purple.add(hl.addHighlight(occurrence, occurrence + pattern.length(), paint_purple));
                            occurrence = text.indexOf(pattern, occurrence + pattern.length());
                        } catch (BadLocationException ex) {
                            errorReport(ex.getMessage(), ht);
                        }
                    }
                }
            }
        });

        // See comments for btn_1
        btn_3.addActionListener(e -> {
            if (e.getSource() == btn_3){
                if (!hl_tag_green.isEmpty()){
                    for (Object hlg : hl_tag_green){
                        hl.removeHighlight(hlg);
                    }
                }
                if (ta.getSelectedText() != null){
                    String pattern = ta.getSelectedText();
                    int occurrence = text.indexOf(pattern);
                    while (occurrence >= 0) {
                        try{
                            hl_tag_green.add(hl.addHighlight(occurrence, occurrence + pattern.length(), paint_green));
                            occurrence = text.indexOf(pattern, occurrence + pattern.length());
                        } catch (BadLocationException ex){
                            errorReport(ex.getMessage(), ht);
                        }
                    }
                }
            }
        });
        btn_clear.addActionListener(e -> {
            if (e.getSource() == btn_clear){
                hl.removeAllHighlights();
            }
        });
        ht.add(search, BorderLayout.PAGE_START);
        ht.add(flow, BorderLayout.PAGE_END);
        ht.setSize(950, 700);
        ht.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        ht.setTitle("Multi-Highlight");
        ht.setLocationRelativeTo(null);
        ht.setVisible(true);
    }

    /**
     * Creates an imageIcon to add to a button
     * @param path Path of the iamge to be used
     * @return Image icon usin ga URL created from the path
     */
    private ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = HighlightSelection.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    /**
     * Creates a pop-up with an errorMessage for the user, before closing the app.
     * @param message Message to be displayed
     */
    private void errorReport(String message, HighlightSelection hs){
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
        hs.dispose();
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                HighlightSelection hs = new HighlightSelection();
                hs.guiCreation(hs);
            }
        });
    }
}
