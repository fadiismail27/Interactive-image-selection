package selector;

import static selector.SelectionModel.SelectionState.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import selector.SelectionModel.SelectionState;
import scissors.ScissorsSelectionModel;

/**
 * A graphical application for selecting and extracting regions of images.
 */
public class SelectorApp implements PropertyChangeListener {

    /**
     * Our application window.  Disposed when application exits.
     */
    private final JFrame frame;

    /**
     * Component for displaying the current image and selection tool.
     */
    private final ImagePanel imgPanel;

    /**
     * The current state of the selection tool.  Must always match the model used by `imgPanel`.
     */
    private SelectionModel model;

    /* Components whose state must be changed during the selection process. */
    private JMenuItem saveItem;
    private JMenuItem undoItem;
    private JButton cancelButton;
    private JButton undoButton;
    private JButton resetButton;
    private JButton finishButton;
    private final JLabel statusLabel;


    // New in A6
    /**
     * Progress bar to indicate the progress of a model that needs to do long calculations in a
     * PROCESSING state.
     */
    private JProgressBar processingProgress;


    /**
     * Construct a new application instance.  Initializes GUI components, so must be invoked on the
     * Swing Event Dispatch Thread.  Does not show the application window (call `start()` to do
     * that).
     */
    public SelectorApp() {
        // Initialize application window
        frame = new JFrame("Selector");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // New in A6: Add progress bar
        processingProgress = new JProgressBar();
        frame.add(processingProgress, BorderLayout.PAGE_START);


        // TODO 1A: Add `statusLabel` to the bottom of our window.  Stylistic alteration of the
        //  label (i.e., custom fonts and colors) is allowed.
        //  See the BorderLayout tutorial [1] for example code that you can adapt.
        //  [1]: https://docs.oracle.com/javase/tutorial/uiswing/layout/border.html

        // Add status bar 1A
        statusLabel = new JLabel("NO SELECTION");
        frame.add(statusLabel,BorderLayout.PAGE_END);


        // Add image component with scrollbars
        imgPanel = new ImagePanel();

        // TODO 1B: Replace the following line with code to put scroll bars around `imgPanel` while
        //  otherwise keeping it in the center of our window.  The scroll pane should also be given
        //  a moderately large preferred size (e.g., between 400 and 700 pixels wide and tall).
        //  The Swing Tutorial has lots of info on scrolling [1], but for this task you only need
        //  the basics from lecture.
        //  [1] https://docs.oracle.com/javase/tutorial/uiswing/components/scrollpane.html
        frame.add(imgPanel,BorderLayout.CENTER);  // Replace this line
        JScrollPane scrollPane = new JScrollPane(imgPanel);
        scrollPane.setPreferredSize(new Dimension(500,500));
        frame.add(scrollPane,BorderLayout.CENTER);

        // Add menu bar
        frame.setJMenuBar(makeMenuBar());

        // Add control buttons
        // TODO 3E: Call `makeControlPanel()`, then add the result to the window next to the image.
        JPanel panel = makeControlPanel();
        frame.add(panel,BorderLayout.EAST);


        // Controller: Set initial selection tool and update components to reflect its state
        setSelectionModel(new PointToPointSelectionModel(true));
    }

    /**
     * Create and populate a menu bar with our application's menus and items and attach listeners.
     * Should only be called from constructor, as it initializes menu item fields.
     */
    private JMenuBar makeMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Create and populate File menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        JMenuItem openItem = new JMenuItem("Open...");
        fileMenu.add(openItem);
        saveItem = new JMenuItem("Save...");
        fileMenu.add(saveItem);
        JMenuItem closeItem = new JMenuItem("Close");
        fileMenu.add(closeItem);
        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(exitItem);

        // Create and populate Edit menu
        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);
        undoItem = new JMenuItem("Undo");
        editMenu.add(undoItem);

        // TODO (embellishment): Assign keyboard shortcuts to menu items [1].  (1 point)
        //  [1] https://docs.oracle.com/javase/tutorial/uiswing/components/menu.html#mnemonic

        // Controller: Attach menu item listeners
        openItem.addActionListener(e -> openImage());
        closeItem.addActionListener(e -> imgPanel.setImage(null));
        saveItem.addActionListener(e -> saveSelection());
        exitItem.addActionListener(e -> frame.dispose());
        undoItem.addActionListener(e -> model.undo());

        return menuBar;
    }

    /**
     * Return a panel containing buttons for controlling image selection.  Should only be called
     * from constructor, as it initializes button fields.
     */
    private JPanel makeControlPanel() {



        JPanel panel = new JPanel();
        GridLayout grid = new GridLayout(4,1);
        panel.setLayout(grid);


        cancelButton = new JButton("Cancel");
        undoButton= new JButton("Undo");
        resetButton= new JButton("Reset");
        finishButton = new JButton("Finish");

        String option1 = "PointToPointSelectionModel";
        String option2 = "Intelligent scissors: gray";
        String option3 = "Intelligent scissors: color";

        String[] options = {option1,option2,option3};

        JComboBox<String> dropDown = new JComboBox<>(options);


        panel.add(cancelButton);
        panel.add(undoButton);
        panel.add(resetButton);
        panel.add(finishButton);
        panel.add(dropDown);

        cancelButton.addActionListener(e -> model.cancelProcessing());
        undoButton.addActionListener(e -> model.undo());

        resetButton.addActionListener(e -> model.reset());
        finishButton.addActionListener(e -> model.finishSelection());





        dropDown.addActionListener(e -> {

            JComboBox cb = (JComboBox) e.getSource();
            String selectedOption = (String) cb.getSelectedItem();

            if (selectedOption.equals(option1)){
                SelectionModel newModel = new PointToPointSelectionModel(model);
                setSelectionModel(newModel);
            } else if (selectedOption.equals(option2)) {
                SelectionModel newModel = new ScissorsSelectionModel("CrossGradMono",model);
                setSelectionModel(newModel);
            } else if (selectedOption.equals(option3)) {
                SelectionModel newModel = new ScissorsSelectionModel("ColorAware", model);
                setSelectionModel(newModel);
            }


        });



        // not printing the errors to the console log
        return panel;
    }

    /**
     * Start the application by showing its window.
     */
    public void start() {
        // Compute ideal window size
        frame.pack();

        frame.setVisible(true);
    }

    /**
     * React to property changes in an observed model.  Supported properties include:
     * * "state": Update components to reflect the new selection state.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("state".equals(evt.getPropertyName())) {
                reflectSelectionState(model.state());
        }

        // TODO A6.0b: Update the progress bar [1] as follows:
        //  * When the model transitions into the PROCESSING state, set the progress bar to
        //    "indeterminate" mode.  That way, the user sees that something is happening even before
        //    the model has an estimate of its progress.
        //  * When the model transitions to any other state, ensure that the progress bar's value is
        //    0 and that it is not in "indeterminate" mode.  The progress bar should look inert if
        //    the model isn't currently processing.
        //  * Upon receiving a "progress" property change, set the progress bar's value to the new
        //    progress value (which will be an integer in [0..100]) and ensure it is not in
        //    "indeterminate" mode.  You need to use the event object to get the new value.
        //  [1] https://docs.oracle.com/javase/tutorial/uiswing/components/progress.html

        if(model.state().equals("PROCESSING")){ // TODOOO check if its right
            processingProgress.setIndeterminate(true);
        }
        else{
            processingProgress.setValue(0);
            processingProgress.setIndeterminate(false);
        }

        if ("progress".equals(evt.getPropertyName())) {
            processingProgress.setIndeterminate(false);
            int newProgress = (Integer) evt.getNewValue();
            processingProgress.setValue(newProgress);
        }
    }

    /**
     * Update components to reflect a selection state of `state`.  Disable buttons and menu items
     * whose actions are invalid in that state, and update the status bar.
     */
    private void reflectSelectionState(SelectionState state) {
        // Update status bar to show current state
        statusLabel.setText(state.toString());

        cancelButton.setEnabled(state == PROCESSING);
        undoButton.setEnabled(state != NO_SELECTION);
        resetButton.setEnabled(state != NO_SELECTION);
        finishButton.setEnabled(state == SELECTING);
        saveItem.setEnabled(state == SELECTED);


        // TODO 3F: Enable/disable components (both buttons and menu items) as follows:
        //  * Cancel is only allowed when the selection is processing
        //  * Undo and Reset are not allowed when there is no selection (pending or complete)
        //  * Finish is only allowed when selecting
        //  * Saving is only allowed when the selection is complete
        //  The JButton tutorial [1] shows an example of enabling buttons in an event handler.
        //  [1] https://docs.oracle.com/javase/tutorial/uiswing/components/button.html
    }

    /**
     * Return the model of the selection tool currently in use.
     */
    public SelectionModel getSelectionModel() {
        return model;
    }

    /**
     * Use `newModel` as the selection tool and update our view to reflect its state.  This
     * application will no longer respond to changes made to its previous selection model and will
     * instead respond to property changes from `newModel`.
     */
    public void setSelectionModel(SelectionModel newModel) {
        // Stop listening to old model
        if (model != null) {
            model.removePropertyChangeListener(this);
        }

        imgPanel.setSelectionModel(newModel);
        model = imgPanel.selection();
        model.addPropertyChangeListener("state", this);
        model.addPropertyChangeListener("progress", this);

        // Since the new model's initial state may be different from the old model's state, manually
        //  trigger an update to our state-dependent view.
        reflectSelectionState(model.state());
    }

    /**
     * Start displaying and selecting from `img` instead of any previous image.  Argument may be
     * null, in which case no image is displayed and the current selection is reset.
     */
    public void setImage(BufferedImage img) {
        imgPanel.setImage(img);
    }

    /**
     * Allow the user to choose a new image from an "open" dialog.  If they do, start displaying and
     * selecting from that image.  Show an error message dialog (and retain any previous image) if
     * the chosen image could not be opened.
     */
    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        // Start browsing in current directory
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        // Filter for file extensions supported by Java's ImageIO readers
        chooser.setFileFilter(new FileNameExtensionFilter("Image files",
                ImageIO.getReaderFileSuffixes()));



        // TODO 1C: Complete this method as specified by performing the following tasks:
        //  * Show an "open file" dialog using the above chooser [1].
        //  * If the user selects a file, read it into a BufferedImage [2], then set that as the
        //    current image (by calling `this.setImage()`).
        //  * If a problem occurs when reading the file (either an exception is thrown or null is
        //    returned), show an error dialog with a descriptive title and message [3].
        //  [1] https://docs.oracle.com/javase/tutorial/uiswing/components/filechooser.html
        //  [2] https://docs.oracle.com/javase/tutorial/2d/images/loadimage.html
        //  [3] https://docs.oracle.com/javase/tutorial/uiswing/components/dialog.html
        // TODO (embellishment): After a problem, re-show the open dialog.  By reusing the same
        //  chooser, the dialog will show the same directory as before the problem. (1 point)


        try {
            int returnVal = chooser.showOpenDialog(frame);
            File file = chooser.getSelectedFile();
            BufferedImage img = ImageIO.read(file);
            this.setImage(img);
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Unable to open image file, try again",
                    "Unsupported Type",
                    JOptionPane.ERROR_MESSAGE);
        }



    }

    /**
     * Save the selected region of the current image to a file selected from a "save" dialog.
     * Show an error message dialog if the image could not be saved.
     */
    private void saveSelection() {
        JFileChooser chooser = new JFileChooser();
        // Start browsing in current directory
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        // We always save in PNG format, so only show existing PNG files
        chooser.setFileFilter(new FileNameExtensionFilter("PNG images", "png"));

        // TODO 3G: Complete this method as specified by performing the following tasks:
        //  * Show a "save file" dialog using the above chooser [1].
        //  * If the user selects a file, write an image containing the selected pixels to the file.
        //  * If a problem occurs when opening or writing to the file, show an error dialog with the
        //    class of the exception as its title and the exception's message as its text [2].
        //  [1] https://docs.oracle.com/javase/tutorial/uiswing/components/filechooser.html
        //  [2] https://docs.oracle.com/javase/tutorial/uiswing/components/dialog.html
        // TODO (embellishment):
        //  * If the selected filename does not end in ".png", append that extension. (1 point) DONE!
        //  * Prompt with a yes/no/cancel dialog before overwriting a file. (1 point) DONE!
        //  * After an IOException, or after user selects "No" (instead of "Cancel") when prompted,
        //    re-show the save dialog.  By reusing the same chooser, the dialog will show the same
        //    directory as before the problem. (1 point)
        chooser.showSaveDialog(frame);
        File file = chooser.getSelectedFile();

        if (!file.getName().toLowerCase().endsWith(".png")) {
            file = new File(file.getParentFile(), file.getName() + ".png");
        }


        if (file.exists()) {
            int confirm = JOptionPane.showConfirmDialog(
                    frame,
                    "The file already exists. Do you want to overwrite it?",
                    "Overwrite File?",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm != JOptionPane.YES_OPTION) {
                if (confirm == JOptionPane.NO_OPTION) {
                    saveSelection();
                }
                return;
            }
        }


        try{
            OutputStream out = new FileOutputStream(file);
            model.saveSelection(out);

        } catch (Exception e){
            JOptionPane.showMessageDialog(
                    frame,
                    "Failed to save the image: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );

//            System.out.println(e.getMessage());
        }
    }

    /**
     * Run an instance of SelectorApp.  No program arguments are expected.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Set Swing theme to look the same (and less old) on all operating systems.
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignored) {
                /* If the Nimbus theme isn't available, just use the platform default. */
            }

            // Create and start the app
            SelectorApp app = new SelectorApp();
            app.start();
        });
    }
}
