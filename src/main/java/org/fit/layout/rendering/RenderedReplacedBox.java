/**
 * RenderedReplacedBox.java
 *
 * Created on 26. 11. 2016, 19:28:50 by burgetr
 */
package org.fit.layout.rendering;

import java.awt.geom.AffineTransform;

import org.fit.cssbox.layout.Box;
import org.fit.cssbox.layout.ReplacedBox;
import org.fit.cssbox.layout.ReplacedContent;
import org.fit.cssbox.layout.ReplacedImage;
import org.fit.layout.cssbox.ContentImageImpl;
import org.fit.layout.model.ContentObject;
import org.fit.layout.model.Rectangular;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * 
 * @author burgetr
 */
public class RenderedReplacedBox extends RenderedBox
{
    private ReplacedBox box;

    public RenderedReplacedBox(ReplacedBox box, AffineTransform transform)
    {
        super((Box) box, transform);
        this.box = box;
        setType(Type.REPLACED_CONTENT);
        if (((Box) box).getNode().getNodeType() == Node.ELEMENT_NODE)
            setTagName(((Element) ((Box) box).getNode()).getTagName().toLowerCase());
        setContentObject(createContentObject());
        setContentBounds(computeContentBounds());
        setBounds(new Rectangular(getContentBounds())); //later, this will be recomputed using recomputeBounds()
    }

    public Box getBox()
    {
        return (Box) box;
    }

    public void setBox(ReplacedBox box)
    {
        this.box = box;
    }

    @Override
    public boolean isVisuallySeparated()
    {
        return isVisible();
    }
    
    /**
     * Computes node the content bounds.
     * The box is clipped by its clipping box.
     */
    private Rectangular computeContentBounds()
    {
        Box box = (Box) getBox();
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
        Box box = (Box) getBox();
        return transform(new Rectangular(box.getMinimalAbsoluteBounds().intersection(box.getClipBlock().getClippedContentBounds())));
    }

    private ContentObject createContentObject()
    {
        ReplacedContent content = box.getContentObj();
        if (content instanceof ReplacedImage)
            return new ContentImageImpl((ReplacedImage) content);
        else
            return null;
    }
    
}
