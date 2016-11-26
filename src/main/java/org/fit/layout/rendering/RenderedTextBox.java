/**
 * RenderedTextBox.java
 *
 * Created on 26. 11. 2016, 19:26:04 by burgetr
 */
package org.fit.layout.rendering;

import java.awt.geom.AffineTransform;

import org.fit.cssbox.layout.TextBox;
import org.fit.layout.model.Rectangular;

/**
 * 
 * @author burgetr
 */
public class RenderedTextBox extends RenderedBox
{
    private TextBox box;

    public RenderedTextBox(TextBox text, AffineTransform transform)
    {
        super(text, transform);
        this.box = text;
        setType(Type.TEXT_CONTENT);
        setText(box.getText());
        setContentBounds(computeContentBounds());
        setBounds(new Rectangular(getContentBounds())); //later, this will be recomputed using recomputeBounds()
    }

    public TextBox getBox()
    {
        return box;
    }

    public void setBox(TextBox box)
    {
        this.box = box;
    }

    @Override
    public boolean isVisuallySeparated()
    {
        return isVisible() && !box.isEmpty();
    }
    
    /**
     * Computes node the content bounds.
     * The box is clipped by its clipping box.
     */
    private Rectangular computeContentBounds()
    {
        Rectangular ret = null;
        
        ret = new Rectangular(box.getAbsoluteBounds());

        //clip with the clipping bounds
        if (box.getClipBlock() != null)
        {
            Rectangular clip = new Rectangular(box.getClipBlock().getClippedContentBounds());
            ret = ret.intersection(clip);
        }
        
        return transform(ret);
    }

    @Override
    protected Rectangular computeVisualBounds()
    {
        return transform(new Rectangular(box.getAbsoluteBounds().intersection(box.getClipBlock().getClippedContentBounds())));
    }
    
}
