/**
 * RenderedBox.java
 *
 * Created on 26. 11. 2016, 18:07:28 by burgetr
 */
package org.fit.layout.rendering;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.Iterator;
import java.util.List;

import org.fit.layout.impl.DefaultBox;
import org.fit.layout.model.Box;
import org.fit.layout.model.Rectangular;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import cz.vutbr.web.css.CSSProperty;

/**
 * 
 * @author burgetr
 */
public abstract class RenderedBox extends DefaultBox
{
    /** Overlapping threshold - the corners are considered to overlap if the boxes
     *  share more than OVERLAP pixels */
    private static final int OVERLAP = 2;
    
    /** Which percentage of the box area must be inside of another box in order
     * to consider it as a child box (from 0 to 1) */
    private static final double AREAP = 0.9;
    
    /** The transformation to be applied when rendering */
    private AffineTransform transform;

    /** Efficient background color */
    protected Color efficientBackground = null;
    
    /** Potential nearest parent node in the box tree */
    private RenderedBox nearestParent = null;

    
    public RenderedBox(org.fit.cssbox.layout.Box box, AffineTransform transform)
    {
        this.transform = transform;
        setSourceNodeId(System.identityHashCode(box.getNode()));
        setFontFamily(box.getVisualContext().getFont().getName());
        setFontSize(box.getVisualContext().getFont().getSize2D());
        setFontStyle(box.getVisualContext().getFont().isItalic() ? 1.0f : 0.0f);
        setFontWeight(box.getVisualContext().getFont().isBold() ? 1.0f : 0.0f);
        setColor(box.getVisualContext().getColor());
        setUnderline(box.getVisualContext().getTextDecoration().contains(CSSProperty.TextDecoration.UNDERLINE) ? 1.0f : 0.0f);
        setLineThrough(box.getVisualContext().getTextDecoration().contains(CSSProperty.TextDecoration.LINE_THROUGH) ? 1.0f : 0.0f);
        loadDOMAttributes(box.getNode());
    }

    public AffineTransform getTransform()
    {
        return transform;
    }

    public void setTransform(AffineTransform transform)
    {
        this.transform = transform;
    }

    public Color getEfficientBackground()
    {
        return efficientBackground;
    }

    public void setEfficientBackground(Color efficientBackground)
    {
        this.efficientBackground = efficientBackground;
    }

    public RenderedBox getNearestParent()
    {
        return nearestParent;
    }

    public void setNearestParent(RenderedBox nearestParent)
    {
        this.nearestParent = nearestParent;
    }

    /** 
     * Expands the box node in order to fully enclose another box 
     */
    public void expandToEnclose(Box child)
    {
        getBounds().expandToEnclose(child.getBounds());
    }
    
    /** 
     * Checks if another node is located inside the visual bounds of this box.
     * @param childNode the node to check
     * @return <code>true</code> if the child node is completely inside this node, <code>false</code> otherwise 
     */
    public boolean visuallyEncloses(RenderedBox childNode)
    {
        int cx1 = childNode.getVisualBounds().getX1();
        int cy1 = childNode.getVisualBounds().getY1();
        int cx2 = childNode.getVisualBounds().getX2();
        int cy2 = childNode.getVisualBounds().getY2();
        int px1 = getVisualBounds().getX1();
        int py1 = getVisualBounds().getY1();
        int px2 = getVisualBounds().getX2();
        int py2 = getVisualBounds().getY2();
        
        /*if (this.toString().contains("pBody") && childNode.toString().contains("mediawiki"))
            System.out.println(childNode + " inside of " + this);
        if (childNode.toString().contains("www-lupa-cz") && this.toString().contains("[page]"))
            System.out.println(childNode + " inside of " + this);*/
        /*if (this.getOrder() == 70 && childNode.getOrder() == 74)
            System.out.println("jo!");*/
        
        
        //check how many corners of the child are inside enough (with some overlap)
        int ccnt = 0;
        if (cx1 >= px1 + OVERLAP && cx1 <= px2 - OVERLAP &&
            cy1 >= py1 + OVERLAP && cy1 <= py2 - OVERLAP) ccnt++; //top left
        if (cx2 >= px1 + OVERLAP && cx2 <= px2 - OVERLAP &&
            cy1 >= py1 + OVERLAP && cy1 <= py2 - OVERLAP) ccnt++; //top right
        if (cx1 >= px1 + OVERLAP && cx1 <= px2 - OVERLAP &&
            cy2 >= py1 + OVERLAP && cy2 <= py2 - OVERLAP) ccnt++; //bottom left
        if (cx2 >= px1 + OVERLAP && cx2 <= px2 - OVERLAP &&
            cy2 >= py1 + OVERLAP && cy2 <= py2 - OVERLAP) ccnt++; //bottom right
        //check how many corners of the child are inside the parent exactly
        int xcnt = 0;
        if (cx1 >= px1 && cx1 <= px2 &&
            cy1 >= py1 && cy1 <= py2) xcnt++; //top left
        if (cx2 >= px1 && cx2 <= px2 &&
            cy1 >= py1 && cy1 <= py2) xcnt++; //top right
        if (cx1 >= px1 && cx1 <= px2 &&
            cy2 >= py1 && cy2 <= py2) xcnt++; //bottom left
        if (cx2 >= px1 && cx2 <= px2 &&
            cy2 >= py1 && cy2 <= py2) xcnt++; //bottom right
        //and reverse direction - how many corners of the parent are inside of the child
        int rxcnt = 0;
        if (px1 >= cx1 && px1 <= cx2 &&
            py1 >= cy1 && py1 <= cy2) rxcnt++; //top left
        if (px2 >= cx1 && px2 <= cx2 &&
            py1 >= cy1 && py1 <= cy2) rxcnt++; //top right
        if (px1 >= cx1 && px1 <= cx2 &&
            py2 >= cy1 && py2 <= cy2) rxcnt++; //bottom left
        if (px2 >= cx1 && px2 <= cx2 &&
            py2 >= cy1 && py2 <= cy2) rxcnt++; //bottom right
        //shared areas
        int shared = getVisualBounds().intersection(childNode.getVisualBounds()).getArea();
        double sharedperc = (double) shared / childNode.getBounds().getArea();
        
        //no overlap
        if (xcnt == 0)
            return false;
        //fully overlapping or over a corner - the order decides
        else if ((cx1 == px1 && cy1 == py1 && cx2 == px2 && cy2 == py2) //full overlap
                 || (ccnt == 1 && xcnt <= 1)) //over a corner
            return this.getId() < childNode.getId() && sharedperc >= AREAP;
        //fully inside
        else if (xcnt == 4)
            return true;
        //partly inside (at least two corners)
        else if (xcnt >= 2)
        {
            if (rxcnt == 4) //reverse relation - the child contains the parent
                return false;
            else //child partly inside the parent
                return this.getId() < childNode.getId() && sharedperc >= AREAP;
        }
        //not inside
        else
            return false;
    }
    
    /** 
     * Checks if another node is fully located inside the content bounds of this box.
     * @param childNode the node to check
     * @return <code>true</code> if the child node is completely inside this node, <code>false</code> otherwise 
     */
    public boolean contentEncloses(RenderedBox childNode)
    {
        //System.out.println(childNode + " => " + childNode.getVisualBounds());
        int cx1 = childNode.getContentBounds().getX1();
        int cy1 = childNode.getContentBounds().getY1();
        int cx2 = childNode.getContentBounds().getX2();
        int cy2 = childNode.getContentBounds().getY2();
        int px1 = getContentBounds().getX1();
        int py1 = getContentBounds().getY1();
        int px2 = getContentBounds().getX2();
        int py2 = getContentBounds().getY2();
        
        //check how many corners of the child are inside the parent exactly
        int xcnt = 0;
        if (cx1 >= px1 && cx1 <= px2 &&
            cy1 >= py1 && cy1 <= py2) xcnt++; //top left
        if (cx2 >= px1 && cx2 <= px2 &&
            cy1 >= py1 && cy1 <= py2) xcnt++; //top right
        if (cx1 >= px1 && cx1 <= px2 &&
            cy2 >= py1 && cy2 <= py2) xcnt++; //bottom left
        if (cx2 >= px1 && cx2 <= px2 &&
            cy2 >= py1 && cy2 <= py2) xcnt++; //bottom right
        
        if ((cx1 == px1 && cy1 == py1 && cx2 == px2 && cy2 == py2)) //exact overlap
           return this.getId() < childNode.getId();
        else
            return xcnt == 4;
    }
    
    /**
     * Recomputes the total bounds of the whole subtree. The bounds of each box will
     * correspond to its visual bounds. If the child boxes exceed the parent box,
     * the parent box bounds will be expanded accordingly.
     */
    public void recomputeBounds()
    {
        setBounds(new Rectangular(getVisualBounds()));
        for (int i = 0; i < getChildCount(); i++)
        {
            RenderedBox child = (RenderedBox) getChildBox(i);
            child.recomputeBounds();
            expandToEnclose(child);
        }
    }

    /**
     * Re-computes the visual bounds of the whole subtree.
     */
    public void recomputeVisualBounds()
    {
        for (int i = 0; i < getChildCount(); i++)
            ((RenderedBox) getChildBox(i)).recomputeVisualBounds();
        setVisualBounds(computeVisualBounds());
    }

    /**
     * Takes a list of nodes and selects the nodes that are located directly inside 
     * of this node's box. The {@code nearestParent} of the selected boxes is set to this box.
     * @param list the list of nodes to test
     * @param full when set to true, all the nodes within the box content bounds are considered.
     *          Otherwise, only the boxes within the visual bounds are considered.
     */
    public void markNodesInside(List<RenderedBox> list, boolean full)
    {
        for (Iterator<RenderedBox> it = list.iterator(); it.hasNext();)
        {
            RenderedBox node = it.next();
            if (full)
            {
                if (node != this 
                    && this.contentEncloses(node)
                    && (node.getNearestParent() == null || !this.contentEncloses(node.nearestParent))) 
                {
                    node.nearestParent = this;
                }
            }
            else
            {
                if (node != this 
                        && this.visuallyEncloses(node)
                        && (node.getNearestParent() == null || !this.visuallyEncloses(node.nearestParent))) 
                {
                    node.nearestParent = this;
                }
            }
        }
    }
    
    /**
     * Takes a list of nodes and selects the nodes whose parent box is identical to this node's box. 
     * The {@code nearestParent} of the selected boxes is set to this box node.
     * @param list the list of nodes to test
     */
    public void markChildNodes(List<RenderedBox> list)
    {
        final org.fit.cssbox.layout.Box thisBox = this.getBox(); 
        for (Iterator<RenderedBox> it = list.iterator(); it.hasNext();)
        {
            RenderedBox node = it.next();
            if (node != this && node.getBox().getParent() == thisBox)
                node.nearestParent = this;
        }        
    }
    
    /**
     * Goes through the parent's children, takes all the nodes that are inside of this node
     * and makes them the children of this node. Then, recursively calls the children to take
     * their nodes.
     */
    public void takeChildren(List<RenderedBox> list)
    {
        for (Iterator<RenderedBox> it = list.iterator(); it.hasNext();)
        {
            RenderedBox node = it.next();
            if (node.nearestParent.equals(this))    
            {
                add(node);
                it.remove();
            }
        }
        //let the children take their children
        for (int i = 0; i < getChildCount(); i++)
            ((RenderedBox) getChildBox(i)).takeChildren(list);
    }
    
    //==========================================================================
    
    @Override
    public boolean isVisible()
    {
        return !getVisualBounds().isEmpty();
    }
    
    @Override
    public String toString()
    {
        return getId() + ": " + super.toString();
    }

    //==========================================================================
    
    abstract public org.fit.cssbox.layout.Box getBox();
    
    abstract public boolean isVisuallySeparated();
    
    abstract protected Rectangular computeVisualBounds();
   
    //==========================================================================
    
    /**
     * Transforms a rectangle according to the current box transformation.
     * @param rect the source rectangle to be transformed
     * @return The transformed rectangle. When no transformation is assigned,
     * the source rectangle is returned.
     */
    protected Rectangular transform(Rectangular rect)
    {
        if (transform != null)
        {
            Rectangle src = new Rectangle(rect.getX1(), rect.getY1(), rect.getWidth(), rect.getHeight());
            Shape dest = transform.createTransformedShape(src);
            Rectangle destr;
            if (dest instanceof Rectangle)
                destr = (Rectangle) dest;
            else
                destr = dest.getBounds();
            return new Rectangular(destr);
        }
        else
            return rect;
    }
    
    private void loadDOMAttributes(Node node)
    {
        NamedNodeMap map = null;
        if (node.getNodeType() == Node.ELEMENT_NODE)
        {
            map = node.getAttributes();
        }
        else if (node.getNodeType() == Node.TEXT_NODE) //text nodes -- try parent //TODO how to propagate from ancestors correctly?
        {
            final Node pnode = node.getParentNode();
            if (pnode != null && pnode.getNodeType() == Node.ELEMENT_NODE)
            {
                map = pnode.getAttributes();
            }
        }
        
        if (map != null) //store the attributes found
        {
            for (int i = 0; i < map.getLength(); i++)
            {
                final Node attr = map.item(i);
                setAttribute(attr.getNodeName(), attr.getNodeValue());
            }
        }
        //eventually add the href value (which may be inherited from top)
        if (getAttribute("href") == null)
        {
            String href = getAncestorAttribute(node, "a", "href");
            if (href != null)
                setAttribute("href", href);
        }
    }
    
    private String getAncestorAttribute(Node node, String elementName, String attrName)
    {
        Node cur = node;
        //find the parent with the given name
        while (cur.getNodeType() != Node.ELEMENT_NODE || !elementName.equals(cur.getNodeName()))
        {
            cur = cur.getParentNode();
            if (cur == null)
                return null;
        }
        //read the attribute
        final Element el = (Element) cur;
        if (el.hasAttribute(attrName))
            return el.getAttribute(attrName);
        else
            return null;
    }

}
