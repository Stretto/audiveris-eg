//----------------------------------------------------------------------------//
//                                                                            //
//                          S h e e t P a i n t e r                           //
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.glyph.facets.Glyph;

import org.audiveris.omr.grid.StaffInfo;

import org.audiveris.omr.score.entity.Measure;
import org.audiveris.omr.score.entity.Page;
import org.audiveris.omr.score.entity.ScoreSystem;
import org.audiveris.omr.score.entity.SystemPart;
import org.audiveris.omr.score.visitor.AbstractScoreVisitor;

import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;

import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.symbol.Alignment;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.Symbols;
import org.audiveris.omr.ui.util.UIUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ConcurrentModificationException;

/**
 * Class {@code SheetPainter} defines for every node in Page hierarchy
 * the rendering of related sections (with preset colors) in the dedicated
 * <b>Sheet</b> display.
 *
 * <p>Nota: It has been extended to deal with rendering of initial sheet
 * elements.
 *
 * @author Hervé Bitteur
 */
public class SheetPainter
    extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
        SheetPainter.class);

    //~ Instance fields --------------------------------------------------------

    /** Graphic context */
    private final Graphics2D g;

    /** Saved stroke for restoration at the end of the painting */
    private final Stroke oldStroke;

    /** Are we drawing editable boundaries? */
    private final boolean editableBoundaries;

    /** Music font properly scaled */
    private MusicFont musicFont;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // SheetPainter //
    //--------------//
    /**
     * Creates a new SheetPainter object.
     *
     * @param g                  Graphic context
     * @param editableBoundaries flag to draw editable boundaries
     */
    public SheetPainter (Graphics g,
                         boolean  editableBoundaries)
    {
        this.g = (Graphics2D) g;
        this.editableBoundaries = editableBoundaries;

        oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // visit Measure //
    //---------------//
    @Override
    public boolean visit (Measure measure)
    {
        try {
            // Render the measure ending barline
            if (measure.getBarline() != null) {
                measure.getBarline()
                       .renderLine(g);
            }
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warn(
                getClass().getSimpleName() + " Error visiting " + measure,
                ex);
        }

        // Nothing lower than measure
        return false;
    }

    //------------//
    // visit Page //
    //------------//
    @Override
    public boolean visit (Page page)
    {
        try {
            Sheet sheet = page.getSheet();

            // Use specific color
            g.setColor(Colors.ENTITY_MINOR);

            if (!page.getSystems()
                     .isEmpty()) {
                // Small protection about changing data...
                if (sheet.getScale() == null) {
                    return false;
                }

                // Determine proper font
                musicFont = MusicFont.getFont(sheet.getInterline());
                // Normal (full) rendering of the score
                page.acceptChildren(this);
            } else {
                // Render what we have got so far
                sheet.getStaffManager()
                     .render(g);
            }
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warn(
                getClass().getSimpleName() + " Error visiting " + page,
                ex);
        } finally {
            g.setStroke(oldStroke);
        }

        return false;
    }

    //------------------//
    // visit SystemPart //
    //------------------//
    @Override
    public boolean visit (SystemPart part)
    {
        try {
            // Render the part starting barline, if any
            if (part.getStartingBarline() != null) {
                part.getStartingBarline()
                    .renderLine(g);
            }
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warn(
                getClass().getSimpleName() + " Error visiting " + part,
                ex);
        }

        return true;
    }

    //--------------//
    // visit System //
    //--------------//
    @Override
    public boolean visit (ScoreSystem system)
    {
        try {
            if (system == null) {
                return false;
            }

            return visit(system.getInfo());
        } catch (ConcurrentModificationException ignored) {
            return false;
        } catch (Exception ex) {
            logger.warn(
                getClass().getSimpleName() + " Error visiting " + system,
                ex);

            return false;
        }
    }

    //------------------//
    // visit SystemInfo //
    //------------------//
    public boolean visit (SystemInfo systemInfo)
    {
        try {
            Rectangle bounds = systemInfo.getBounds();

            if (bounds == null) {
                return false;
            }

            // Check that this system is visible
            if (bounds.intersects(g.getClipBounds())) {
                g.setColor(Colors.ENTITY_MINOR);

                // System boundary
                systemInfo.getBoundary()
                          .render(g, editableBoundaries);

                // Staff lines
                for (StaffInfo staff : systemInfo.getStaves()) {
                    staff.renderAttachments(g);
                }

                //                // Stems
                //                for (Glyph glyph : systemInfo.getGlyphs()) {
                //                    if (glyph.isStem()) {
                //                        glyph.renderLine(g);
                //                    }
                //                }

                // Virtual glyphs
                paintVirtualGlyphs(systemInfo);

                return true;
            }
        } catch (ConcurrentModificationException ignored) {
            return false;
        } catch (Exception ex) {
            logger.warn(
                getClass().getSimpleName() + " Error visiting " +
                systemInfo.idString(),
                ex);
        }

        return false;
    }

    //--------------------//
    // paintVirtualGlyphs //
    //--------------------//
    /**
     * Paint the virtual glyphs on the sheet view
     *
     * @param systemInfo the containing system
     */
    private void paintVirtualGlyphs (SystemInfo systemInfo)
    {
        g.setColor(Colors.ENTITY_VIRTUAL);

        for (Glyph glyph : systemInfo.getGlyphs()) {
            if (glyph.isVirtual()) {
                ShapeSymbol symbol = Symbols.getSymbol(glyph.getShape());

                if (symbol == null) {
                    systemInfo.getScoreSystem()
                              .addError(
                        glyph,
                        "No symbol for " + glyph.idString());
                } else {
                    symbol.paintSymbol(
                        g,
                        musicFont,
                        glyph.getAreaCenter(),
                        Alignment.AREA_CENTER);
                }
            }
        }
    }
}
