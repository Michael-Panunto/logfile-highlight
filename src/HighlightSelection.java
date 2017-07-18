import javax.swing.*;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HighlightSelection extends JFrame {
    private String dateStr;
    private Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    private File logfile;

    /**
     * Creates the user interface for the program
     */
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
        submitButton.addActionListener(e -> {
            if (!sTime.getText().isEmpty() && !eTime.getText().isEmpty() && logfile.exists()){
                String sText = sTime.getText().replaceAll("\\s+", "");
                String eText = eTime.getText().replaceAll("\\s+", "");
                if (!sText.matches("^\\d{2}:\\d{2}") || !eText.matches("^\\d{2}:\\d{2}")){
                    errorReport("Start and End times should be of the form hh:mm", hs);
                    System.exit(0);
                }
                fRead(sText, eText, hs);
            }
            errorReport("Please make sure all input fields have been filled out.", hs);
            System.exit(0);
        });

        // Adding everything to the main panel
        mainL.add(centerPanel, BorderLayout.CENTER);
        mainL.add(submitPanel, BorderLayout.PAGE_END);

        // Settings for the window
        hs.setSize(625, 450);
        hs.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        hs.setTitle("Log Multi-Highlight");
        hs.add(mainL);
        hs.setLocationRelativeTo(null);
        hs.setVisible(true);
    }

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
            if (outputBuffer.toString().contains(uSTime) && outputBuffer.toString().contains(uETime)){
                text = outputBuffer.toString().substring(outputBuffer.toString().indexOf(uSTime), outputBuffer.toString().indexOf(uETime) + 1);
            }

            else if (!outputBuffer.toString().contains(uSTime) && outputBuffer.toString().contains(uETime)){
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:");
                Date uSDate = new Date();
               try {
                   uSDate = sdf.parse(startTime + ":");
               }catch (ParseException e){
                   System.err.println(e.getMessage());
               }
               Calendar c = Calendar.getInstance();
               c.setTime(uSDate);
               while (Integer.parseInt(sdf.format(c.getTime()).substring(3,5)) < 59){
                   c.add(Calendar.MINUTE, 1);
                   if (outputBuffer.toString().contains(sdf.format(c.getTime()))){
                       text = outputBuffer.toString().substring(outputBuffer.toString().indexOf(sdf.format(c.getTime())), outputBuffer.toString().indexOf(uETime) + 1);
                       break;
                   }
               }
               if (Integer.parseInt(sdf.format(c.getTime()).substring(3,5)) == 59){
                   errorReport("No start times found within the hour specified", hs);
               }
            }

            else if (outputBuffer.toString().contains(uSTime) && !outputBuffer.toString().contains(uETime)){
                if (outputBuffer.toString().lastIndexOf(uETime.substring(0,3)) > 0) {
                    text = outputBuffer.toString().substring(outputBuffer.toString().indexOf(uSTime), outputBuffer.toString().lastIndexOf(" " + uETime.substring(0,3)) + 1);
                }
                else {
                    errorReport("No end times found within the hour specified", hs);
                }
            }

            else if (!outputBuffer.toString().contains(uSTime) && !outputBuffer.toString().contains(uETime)){
                if (outputBuffer.toString().lastIndexOf(uETime.substring(0,3)) > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:");
                    Date uSDate = new Date();
                    try {
                        uSDate = sdf.parse(startTime + ":");
                    }catch (ParseException e){
                        System.err.println(e.getMessage());
                    }
                    Calendar c = Calendar.getInstance();
                    c.setTime(uSDate);
                    while (Integer.parseInt(sdf.format(c.getTime()).substring(3,5)) < 59){
                        c.add(Calendar.MINUTE, 1);
                        if (outputBuffer.toString().contains(sdf.format(c.getTime()))){
                            text = outputBuffer.toString().substring(outputBuffer.toString().indexOf(sdf.format(c.getTime())), outputBuffer.toString().lastIndexOf(uETime.substring(0,3)) + 1);
                            break;
                        }
                    }
                    if (Integer.parseInt(sdf.format(c.getTime()).substring(3,5)) == 59){
                        errorReport("No start times found within the hour specified", hs);
                    }
                }
                else {
                    errorReport("No end times found within the hour specified", hs);
                }

            }

        } catch (IOException e){
            // Sends error message as a popup
            errorReport(e.getMessage(), hs);
        }
        return text;
    }

    private void highlightText(String text){

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
            System.err.println("Coudln't find file: " + path);
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
