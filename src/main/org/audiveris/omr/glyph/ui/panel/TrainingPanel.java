//----------------------------------------------------------------------------//
//                                                                            //
//                         T r a i n i n g P a n e l                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
// Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//----------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.glyph.ui.panel;

import org.audiveris.omr.glyph.EvaluationEngine;
import org.audiveris.omr.glyph.GlyphNetwork;
import org.audiveris.omr.glyph.GlyphRepository;
import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.*;
import org.audiveris.omr.glyph.facets.Glyph;
import static org.audiveris.omr.glyph.ui.panel.GlyphTrainer.Task.Activity.*;

import org.audiveris.omr.ui.util.Panel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.SwingWorker;

/**
 * Class {@code TrainingPanel} is a panel dedicated to the training of
 * an evaluation engine.
 * It is used through its subclasses {@link NetworkPanel} and {@link
 * RegressionPanel} to train the neural network engine and the linear
 * engine respectively. It is a dedicated companion of class {@link
 * GlyphTrainer}.
 *
 * @author Hervé Bitteur
 */
class TrainingPanel
    implements EvaluationEngine.Monitor, Observer
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(TrainingPanel.class);

    //~ Instance fields --------------------------------------------------------

    /** The swing component */
    protected final Panel component;

    /** Current activity (selecting the population, or training the engine on
     * the selected population */
    protected final GlyphTrainer.Task task;

    /** User action to launch the training */
    protected TrainAction trainAction;

    /** The underlying engine to be trained */
    protected EvaluationEngine engine;

    /** User progress bar to visualize the training process */
    protected JProgressBar progressBar = new JProgressBar();

    /** Common JGoodies constraints for this class and its subclass if any */
    protected CellConstraints cst = new CellConstraints();

    /** Common JGoodies builder for this class and its subclass if any */
    protected PanelBuilder builder;

    /** Repository of known glyphs */
    private final GlyphRepository repository = GlyphRepository.getInstance();

    /**
     * Flag to indicate that the whole population of recorded glyphs (and not
     * just the core ones) is to be considered
     */
    private boolean useWhole = true;

    /** Display of cardinality of whole population */
    private JLabel wholeNumber = new JLabel();

    /** Display of cardinality of core population */
    private JLabel coreNumber = new JLabel();

    /** UI panel dealing with repository selection */
    private final SelectionPanel selectionPanel;

    /** The Neural Network engine */
    private GlyphNetwork network = GlyphNetwork.getInstance();

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // TrainingPanel //
    //---------------//
    /**
     * Creates a new TrainingPanel object.
     *
     * @param task           the current training task
     * @param standardWidth  standard width for fields & buttons
     * @param engine         the underlying engine to train
     * @param selectionPanel user panel for glyphs selection
     * @param totalRows      total number of display rows, interlines not
     *                       counted
     */
    public TrainingPanel (GlyphTrainer.Task task,
                          String            standardWidth,
                          EvaluationEngine  engine,
                          SelectionPanel    selectionPanel,
                          int               totalRows)
    {
        this.engine = engine;
        this.task = task;
        this.selectionPanel = selectionPanel;

        component = new Panel();
        component.setNoInsets();

        FormLayout layout = Panel.makeFormLayout(
            totalRows,
            4,
            "",
            standardWidth,
            standardWidth);

        builder = new PanelBuilder(layout, component);

        defineLayout();
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void epochEnded (int    epochIndex,
                            double mse)
    {
    }

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Give access to the encapsulated swing component
     *
     * @return the user panel
     */
    public JComponent getComponent ()
    {
        return component;
    }

    @Override
    public void glyphProcessed (final Glyph glyph)
    {
    }

    @Override
    public void trainingStarted (final int    epochIndex,
                                 final double mse)
    {
    }

    //--------//
    // update //
    //--------//
    /**
     * Method triggered by new task activity : the train action is enabled only
     * when no activity is going on.
     *
     * @param obs    the task object
     * @param unused not used
     */
    @Override
    public void update (Observable obs,
                        Object     unused)
    {
        switch (task.getActivity()) {
        case INACTIVE :
            trainAction.setEnabled(true);

            break;

        case SELECTING :
        case TRAINING :
            trainAction.setEnabled(false);

            break;
        }
    }

    //----------//
    // useWhole //
    //----------//
    /**
     * Tell whether the whole glyph base is to be used, or just the core base
     *
     * @return true if whole, false if core
     */
    public boolean useWhole ()
    {
        return useWhole;
    }

    //--------------//
    // defineLayout //
    //--------------//
    /**
     * Define the common part of the layout, each subclass being able to augment
     * this layout from its constructor
     */
    protected void defineLayout ()
    {
        // Buttons to select just the core glyphs, or the whole population
        CoreAction   coreAction = new CoreAction();
        JRadioButton coreButton = new JRadioButton(coreAction);
        WholeAction  wholeAction = new WholeAction();
        JRadioButton wholeButton = new JRadioButton(wholeAction);

        // Group the radio buttons.
        ButtonGroup group = new ButtonGroup();
        group.add(wholeButton);
        wholeButton.setToolTipText("Use the whole glyph base for any action");
        group.add(coreButton);
        coreButton.setToolTipText(
            "Use only the core glyph base for any action");
        wholeButton.setSelected(true);

        // Evaluator Title & Progress Bar
        int    r = 1; // ----------------------------
        String title = engine.getName() + " Training";
        builder.addSeparator(title, cst.xyw(1, r, 7));
        builder.add(progressBar, cst.xyw(9, r, 7));

        r += 2; // ----------------------------
        builder.add(wholeButton, cst.xy(3, r));
        builder.add(wholeNumber, cst.xy(5, r));

        r += 2; // ----------------------------
        builder.add(coreButton, cst.xy(3, r));
        builder.add(coreNumber, cst.xy(5, r));

        // Initialize with population cardinalities
        coreAction.actionPerformed(null);
        wholeAction.actionPerformed(null);
    }

    //-----------------//
    // checkPopulation //
    //-----------------//
    private void checkPopulation (List<Glyph> glyphs)
    {
        // Check that all trainable shapes are present in the training
        // population and that only legal shapes are present. If illegal
        // (non trainable) shapes are found, they are removed from the
        // population.
        boolean[] present = new boolean[LAST_PHYSICAL_SHAPE.ordinal() + 1];
        Arrays.fill(present, false);

        for (Iterator<Glyph> it = glyphs.iterator(); it.hasNext();) {
            Glyph glyph = it.next();
            Shape shape = glyph.getShape();

            try {
                Shape physicalShape = shape.getPhysicalShape();

                if (physicalShape.isTrainable()) {
                    present[physicalShape.ordinal()] = true;
                } else {
                    logger.warn("Removing non trainable shape:{}", physicalShape);
                    it.remove();
                }
            } catch (Exception ex) {
                logger.warn("Removing weird shape: " + shape, ex);
                it.remove();
            }
        }

        for (int i = 0; i < present.length; i++) {
            if (!present[i]) {
                logger.warn("Missing shape: {}", Shape.values()[i]);
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //------------//
    // DumpAction //
    //------------//
    protected class DumpAction
        extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public DumpAction ()
        {
            super("Dump");
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            engine.dump();
        }
    }

    //-------------//
    // TrainAction //
    //-------------//
    protected class TrainAction
        extends AbstractAction
    {
        //~ Instance fields ----------------------------------------------------

        // Specific training starting mode
        protected EvaluationEngine.StartingMode mode = EvaluationEngine.StartingMode.SCRATCH;
        protected boolean                       confirmationRequired = true;

        //~ Constructors -------------------------------------------------------

        public TrainAction (String title)
        {
            super(title);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Ask user confirmation
            if (confirmationRequired) {
                int answer = JOptionPane.showConfirmDialog(
                    component,
                    "Do you really want to retrain " + engine.getName()
                        + " from scratch?");

                if (answer != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            class Worker
                extends Thread
            {
                @Override
                public void run ()
                {
                    train();
                }
            }

            Worker worker = new Worker();
            worker.setPriority(Thread.MIN_PRIORITY);
            worker.start();
        }

        //-------//
        // train //
        //-------//
        public void train ()
        {
            task.setActivity(TRAINING);

            Collection<String> gNames = selectionPanel.getBase(useWhole);
            progressBar.setValue(0);
            progressBar.setMaximum(network.getListEpochs());

            List<Glyph> glyphs = new ArrayList<>();

            for (String gName : gNames) {
                Glyph glyph = repository.getGlyph(gName, selectionPanel);

                if (glyph != null) {
                    if (glyph.getShape() != null) {
                        glyphs.add(glyph);
                    } else {
                        logger.warn("Cannot infer shape from {}", gName);
                    }
                } else {
                    logger.warn("Cannot get glyph {}", gName);
                }
            }

            // Check that all trainable shapes (and only those ones) are
            // present in the training population
            checkPopulation(glyphs);

            engine.train(glyphs, TrainingPanel.this, mode);

            task.setActivity(INACTIVE);
        }
    }

    //------------//
    // CoreAction //
    //------------//
    private class CoreAction
        extends AbstractAction
    {
        //~ Instance fields ----------------------------------------------------

        final SwingWorker<Integer, Object> worker = new SwingWorker<Integer, Object>() {
            @Override
            public void done ()
            {
                try {
                    coreNumber.setText("" + get());
                } catch (Exception ex) {
                    logger.warn("Error while loading core base", ex);
                }
            }

            @Override
            protected Integer doInBackground ()
            {
                return selectionPanel.getBase(false)
                                     .size();
            }
        };


        //~ Constructors -------------------------------------------------------

        public CoreAction ()
        {
            super("Core");
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            useWhole = false;
            worker.execute();
        }
    }

    //-------------//
    // WholeAction //
    //-------------//
    private class WholeAction
        extends AbstractAction
    {
        //~ Instance fields ----------------------------------------------------

        final SwingWorker<Integer, Object> worker = new SwingWorker<Integer, Object>() {
            @Override
            public void done ()
            {
                try {
                    wholeNumber.setText("" + get());
                } catch (Exception ex) {
                    logger.warn("Error while loading whole base", ex);
                }
            }

            @Override
            protected Integer doInBackground ()
            {
                return selectionPanel.getBase(true)
                                     .size();
            }
        };


        //~ Constructors -------------------------------------------------------

        public WholeAction ()
        {
            super("Whole");
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            useWhole = true;
            worker.execute();
        }
    }
}
