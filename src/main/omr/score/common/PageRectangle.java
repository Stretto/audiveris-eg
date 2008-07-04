//----------------------------------------------------------------------------//
//                                                                            //
//                         P a g e R e c t a n g l e                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.common;


/**
 * Class <code>PageRectangle</code> is a simple Rectangle that is meant to
 * represent a rectangle in a page, with its origin at the top left corner with
 * its components (width and height) specified in units.
 *
 * <p> This specialization is used to take benefit of compiler checks, to
 * prevent the use of rectangles with incorrect meaning or units. </p>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class PageRectangle
    extends SimpleRectangle
{
    //~ Constructors -----------------------------------------------------------

    //---------------//
    // PageRectangle //
    //---------------//
    /**
     * Creates an instance of <code>PageRectangle</code> with all items set to
     * zero.
     */
    public PageRectangle ()
    {
    }

    //---------------//
    // PageRectangle //
    //---------------//
    /**
     * Constructs a <code>PageRectangle</code> and initializes it with the
     * specified data.
     *
     * @param x the specified x
     * @param y the specified y
     * @param width the specified width
     * @param height the specified height
     */
    public PageRectangle (int x,
                          int y,
                          int width,
                          int height)
    {
        super(x, y, width, height);
    }
}
