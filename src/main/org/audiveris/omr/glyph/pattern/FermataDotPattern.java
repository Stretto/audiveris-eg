//----------------------------------------------------------------------------//
//                                                                            //
//                     F e r m a t a D o t P a t t e r n                      //
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
package org.audiveris.omr.glyph.pattern;

import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Glyphs;
import org.audiveris.omr.glyph.Evaluation;
import org.audiveris.omr.glyph.GlyphNetwork;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.facets.Glyph;

import org.audiveris.omr.sheet.SystemInfo;

import org.audiveris.omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Class {@code FermataDotPattern} looks for FERMATA & FERMATA_BELOW
 * shaped-glyphs to make sure that the "associated" dot is not
 * assigned anything else (such as staccato).
 *
 * @author Hervé Bitteur
 */
public class FermataDotPattern
        extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            FermataDotPattern.class);

    /** Dot avatars */
    private static final List<Shape> dots = Arrays.asList(
            Shape.DOT_set,
            Shape.AUGMENTATION_DOT,
            Shape.STACCATO);

    //~ Constructors -----------------------------------------------------------
    //-------------------//
    // FermataDotPattern //
    //-------------------//
    /**
     * Creates a new FermataDotPattern object.
     *
     * @param system the system to process
     */
    public FermataDotPattern (SystemInfo system)
    {
        super("FermataDot", system);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // runPattern //
    //------------//
    @Override
    public int runPattern ()
    {
        int nb = 0;

        for (final Glyph fermata : system.getGlyphs()) {
            if ((fermata.getShape() != Shape.FERMATA)
                && (fermata.getShape() != Shape.FERMATA_BELOW)) {
                continue;
            }

            if (fermata.isVip() || logger.isDebugEnabled()) {
                logger.info(
                        "Checking fermata #{} {}",
                        fermata.getId(),
                        fermata.getEvaluation());
            }

            // Find related dot within glyph box
            final Rectangle box = fermata.getBounds();

            Set<Glyph> candidates = Glyphs.lookupGlyphs(
                    system.getGlyphs(),
                    new Predicate<Glyph>()
            {
                @Override
                public boolean check (Glyph glyph)
                {
                    return (glyph != fermata)
                           && glyph.getBounds()
                            .intersects(box)
                           && dots.contains(glyph.getShape());
                }
            });

            for (Glyph candidate : candidates) {
                if (fermata.isVip()
                    || candidate.isVip()
                    || logger.isDebugEnabled()) {
                    logger.info("Dot candidate #{}", candidate);
                }

                Glyph compound = system.buildTransientCompound(
                        Arrays.asList(fermata, candidate));
                Evaluation eval = GlyphNetwork.getInstance()
                        .vote(
                        compound,
                        system,
                        Grades.noMinGrade);

                if (eval != null) {
                    // Assign and insert into system & nest environments
                    compound = system.addGlyph(compound);
                    compound.setEvaluation(eval);

                    if (compound.isVip() || logger.isDebugEnabled()) {
                        logger.info(
                                "Compound #{} built as {}",
                                compound.getId(),
                                compound.getEvaluation());
                    }

                    nb++;

                    break;
                }
            }
        }

        return nb;
    }
}
