/**
 * PageImpl.java
 *
 * Created on 22. 10. 2014, 14:25:28 by burgetr
 */
package org.fit.layout.cssbox;

import java.net.URL;

import org.fit.layout.model.Box;
import org.fit.layout.model.Page;

/**
 * 
 * @author burgetr
 */
public class PageImpl implements Page
{
    protected URL url;
    protected BoxNode root;
    protected int width;
    protected int height;
    

    public PageImpl(URL url)
    {
        this.url = url;
    }
    
    @Override
    public URL getSourceURL()
    {
        return url;
    }

    public Box getRoot()
    {
        return root;
    }

    public void setRoot(BoxNode root)
    {
        this.root = root;
    }

    public int getWidth()
    {
        return width;
    }

    public void setWidth(int width)
    {
        this.width = width;
    }

    public int getHeight()
    {
        return height;
    }

    public void setHeight(int height)
    {
        this.height = height;
    }

}
