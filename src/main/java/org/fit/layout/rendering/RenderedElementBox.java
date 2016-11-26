/**
 * RenderedElementBox.java
 *
 * Created on 26. 11. 2016, 19:21:08 by burgetr
 */
package org.fit.layout.rendering;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import org.fit.cssbox.layout.Box;
import org.fit.cssbox.layout.ElementBox;
import org.fit.cssbox.layout.ListItemBox;
import org.fit.cssbox.layout.Viewport;
import org.fit.layout.model.Border;
import org.fit.layout.model.Rectangular;
import org.fit.layout.model.Border.Side;
import org.fit.layout.model.Border.Style;

import cz.vutbr.web.css.CSSProperty;
import cz.vutbr.web.css.NodeData;
import cz.vutbr.web.css.TermColor;
import cz.vutbr.web.css.CSSProperty.BorderStyle;

/**
 * 
 * @author burgetr
 */
public class RenderedElementBox extends RenderedBox
{
    private ElementBox elem;

    public RenderedElementBox(ElementBox elem, AffineTransform transform)
    {
        super(elem, transform);
        this.elem = elem;
        //general attributes
        setType(Type.ELEMENT);
        setDisplayType(computeDisplayType());
        setTagName(elem.getElement().getTagName().toLowerCase());
        //visual attributes
        setBackgroundColor(elem.getBgcolor());
        for (Side side : Side.values())
            setBorderStyle(side, computeBorderStyle(side));
        //bounds
        setContentBounds(computeContentBounds());
        setBounds(new Rectangular(getContentBounds())); //later, this will be recomputed using recomputeBounds()
    }

    public ElementBox getElement()
    {
        return elem;
    }

    public void setElement(ElementBox elem)
    {
        this.elem = elem;
    }

    @Override
    public Box getBox()
    {
        return elem;
    }

    @Override
    public boolean isVisuallySeparated()
    {
        //invisible boxes are not separated
        if (!isVisible()) 
            return false;
        //viewport is visually separated
        else if (elem instanceof Viewport)
            return true;
        //list item boxes with a bullet
        else if (elem instanceof ListItemBox)
        {
            return ((ListItemBox) elem).hasVisibleBullet();
        }
        //other element boxes
        else 
        {
            //check if separated by border -- at least one border needed
            if (getBorderCount() >= 1)
                return true;
            //check the background
            else if (isBackgroundSeparated())
                return true;
            return false;
        }
    }

    //====================================================================================
    
    /**
     * Computes node the content bounds. They correspond to the background bounds
     * however, when a border is present, it is included in the contents. Moreover,
     * the box is clipped by its clipping box.
     */
    private Rectangular computeContentBounds()
    {
        Rectangular ret = null;
        
        if (elem instanceof Viewport)
        {
            ret = new Rectangular(((Viewport) elem).getClippedBounds());
        }
        else
        {
            //at least one border - take the border bounds
            //TODO: when only one border is present, we shouldn't take the whole border box? 
            if (elem.getBorder().top > 0 || elem.getBorder().left > 0 ||
                elem.getBorder().bottom > 0 || elem.getBorder().right > 0)
            {
                ret = new Rectangular(elem.getAbsoluteBorderBounds());
            }
            //no border
            else
            {
                ret = new Rectangular(elem.getAbsoluteBackgroundBounds());
            }
        }

        //clip with the clipping bounds
        if (elem.getClipBlock() != null)
        {
            Rectangular clip = new Rectangular(elem.getClipBlock().getClippedContentBounds());
            ret = ret.intersection(clip);
        }
        
        return transform(ret);
    }
    
    @Override
    protected Rectangular computeVisualBounds()
    {
        Rectangular ret = null;
        
        if (elem instanceof Viewport)
        {
            ret = new Rectangular(((Viewport) elem).getClippedBounds());
            ret = transform(ret);
        }
        else
        {
            //one border only -- the box represents the border only
            if (getBorderCount() == 1 && !isBackgroundSeparated())
            {
                Rectangular b = new Rectangular(elem.getAbsoluteBorderBounds().intersection(elem.getClipBlock().getClippedContentBounds())); //clipped absolute bounds
                if (hasTopBorder())
                    ret = new Rectangular(b.getX1(), b.getY1(), b.getX2(), b.getY1() + elem.getBorder().top - 1);
                else if (hasBottomBorder())
                    ret = new Rectangular(b.getX1(), b.getY2() - elem.getBorder().bottom + 1, b.getX2(), b.getY2());
                else if (hasLeftBorder())
                    ret = new Rectangular(b.getX1(), b.getY1(), b.getX1() + elem.getBorder().left - 1, b.getY2());
                else if (hasRightBorder())
                    ret = new Rectangular(b.getX2() - elem.getBorder().right + 1, b.getY1(), b.getX2(), b.getY2());
            }
            //at least two borders or a border and background - take the border bounds
            else if (getBorderCount() >= 2 || (getBorderCount() == 1 && isBackgroundSeparated()))
            {
                ret = new Rectangular(elem.getAbsoluteBorderBounds().intersection(elem.getClipBlock().getClippedContentBounds()));
            }
            
            //consider the background if different from the parent
            if (isBackgroundSeparated())
            {
                Rectangular bg = new Rectangular(elem.getAbsoluteBackgroundBounds().intersection(elem.getClipBlock().getClippedContentBounds()));
                if (ret == null)
                    ret = bg;
                else
                    ret.expandToEnclose(bg);
            }
            //no visual separators, consider the contents
            else
            {
                Rectangular cont = getContentVisualBounds();
                if (ret == null)
                    ret = cont;
                else
                    ret.expandToEnclose(cont);
            }
        }
        
        return transform(ret);
    }

    private Rectangular getContentVisualBounds()
    {
        Rectangular ret = null;
        for (int i = 0; i < getChildCount(); i++)
        {
            RenderedBox subnode = (RenderedBox) getChildBox(i); 
            Rectangular sb = subnode.getVisualBounds();
            if (subnode.isVisible() && sb.getWidth() > 0 && sb.getHeight() > 0)
            {
                if (ret == null)
                    ret = new Rectangular(sb);
                else
                    ret.expandToEnclose(sb);
            }
        }
        //if nothing has been found return an empty rectangle at the top left corner
        if (ret == null)
        {
            Rectangle b = elem.getAbsoluteBounds().intersection(elem.getClipBlock().getClippedContentBounds());
            return new Rectangular(b.x, b.y);
        }
        else
            return ret;
    }
    
    private Border computeBorderStyle(Side side)
    {
        final NodeData style = elem.getStyle();
        TermColor tclr = style.getValue(TermColor.class, "border-"+side+"-color");
        CSSProperty.BorderStyle bst = style.getProperty("border-"+side+"-style");
        if (bst == null)
            bst = BorderStyle.NONE;
        
        Color clr = null;
        if (tclr != null)
            clr = tclr.getValue();
        if (clr == null)
        {
            clr = elem.getVisualContext().getColor();
            if (clr == null)
                clr = Color.BLACK;
        }

        int rwidth = 0;
        switch (side)
        {
            case BOTTOM:
                rwidth = elem.getBorder().bottom;
                break;
            case LEFT:
                rwidth = elem.getBorder().left;
                break;
            case RIGHT:
                rwidth = elem.getBorder().right;
                break;
            case TOP:
                rwidth = elem.getBorder().top;
                break;
        }
        
        Border.Style rstyle;
        switch (bst)
        {
            case NONE:
            case HIDDEN:
                rstyle = Style.NONE;
                break;
            case DASHED:
                rstyle = Style.DASHED;
                break;
            case DOTTED:
                rstyle = Style.DOTTED;
                break;
            case DOUBLE:
                rstyle = Style.DOUBLE;
                break;
            default:
                rstyle = Style.SOLID;
                break;
        }
        
        return new Border(rwidth, rstyle, clr);
    }

    public DisplayType computeDisplayType()
    {
        CSSProperty.Display display = elem.getDisplay();
        if (display == null)
            return DisplayType.BLOCK; //e.g. the viewport has no display value
        switch (display)
        {
            case BLOCK:
                return DisplayType.BLOCK;
            case INLINE:
                return DisplayType.INLINE;
            case INLINE_BLOCK:
                return DisplayType.INLINE_BLOCK;
            case INLINE_TABLE:
                return DisplayType.INLINE_TABLE;
            case LIST_ITEM:
                return DisplayType.LIST_ITEM;
            case NONE:
                return DisplayType.NONE;
            case RUN_IN:
                return DisplayType.RUN_IN;
            case TABLE:
                return DisplayType.TABLE;
            case TABLE_CAPTION:
                return DisplayType.TABLE_CAPTION;
            case TABLE_CELL:
                return DisplayType.TABLE_CELL;
            case TABLE_COLUMN:
                return DisplayType.TABLE_COLUMN;
            case TABLE_COLUMN_GROUP:
                return DisplayType.TABLE_COLUMN_GROUP;
            case TABLE_FOOTER_GROUP:
                return DisplayType.TABLE_FOOTER_GROUP;
            case TABLE_HEADER_GROUP:
                return DisplayType.TABLE_HEADER_GROUP;
            case TABLE_ROW:
                return DisplayType.TABLE_ROW;
            case TABLE_ROW_GROUP:
                return DisplayType.TABLE_ROW_GROUP;
            default:
                return DisplayType.BLOCK; //this should not happen
        }
    }

}
