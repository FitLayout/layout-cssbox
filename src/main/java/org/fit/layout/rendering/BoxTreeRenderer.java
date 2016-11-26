/**
 * BoxTreeRenderer.java
 *
 * Created on 26. 11. 2016, 18:09:39 by burgetr
 */
package org.fit.layout.rendering;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fit.cssbox.layout.ElementBox;
import org.fit.cssbox.layout.ReplacedBox;
import org.fit.cssbox.layout.TextBox;
import org.fit.cssbox.render.BoxRenderer;
import org.fit.cssbox.render.Transform;
import org.fit.layout.model.Page;

/**
 * 
 * @author burgetr
 */
public class BoxTreeRenderer implements BoxRenderer
{
    /** The resulting list of boxes */
    private List<RenderedBox> boxes;

    /** Applied transformations */
    private Map<ElementBox, AffineTransform> savedTransforms;

    /** Current transformation */
    private AffineTransform curTransform; 
    
    /** Order counter */
    private int next_order;
    
    /** The destination page */
    private Page page;
    
    
    public BoxTreeRenderer(Page page)
    {
        this.page = page;
        boxes = new ArrayList<>();
        savedTransforms = new HashMap<ElementBox, AffineTransform>();
        curTransform = null;
        next_order = 1;
    }
    
    public List<RenderedBox> getBoxes()
    {
        return boxes;
    }

    //======================================================================
    
    @Override
    public void startElementContents(ElementBox elem)
    {
        //setup transformations for the contents
        AffineTransform at = Transform.createTransform(elem);
        if (at != null)
        {
            savedTransforms.put(elem, curTransform);
            curTransform = at;
        }
    }

    @Override
    public void finishElementContents(ElementBox elem)
    {
        //restore the stransformations
        AffineTransform origAt = savedTransforms.get(elem);
        if (origAt != null)
            curTransform = origAt;
    }

    @Override
    public void renderElementBackground(ElementBox elem)
    {
        AffineTransform at = Transform.createTransform(elem);
        RenderedElementBox box = new RenderedElementBox(elem, (at != null) ? at : curTransform);
        box.setId(next_order++);
        box.setPage(page);
        boxes.add(box);
    }

    @Override
    public void renderTextContent(TextBox text)
    {
        RenderedTextBox box = new RenderedTextBox(text, curTransform);
        box.setId(next_order++);
        box.setPage(page);
        boxes.add(box);
    }

    @Override
    public void renderReplacedContent(ReplacedBox box)
    {
        RenderedReplacedBox newbox = new RenderedReplacedBox(box, curTransform);
        newbox.setId(next_order++);
        newbox.setPage(page);
        boxes.add(newbox);
    }

    @Override
    public void close()
    {
    }

}
