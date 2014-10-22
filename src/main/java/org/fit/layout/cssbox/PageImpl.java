/**
 * PageImpl.java
 *
 * Created on 22. 10. 2014, 14:25:28 by burgetr
 */
package org.fit.layout.cssbox;

import java.net.URL;

import org.fit.layout.model.Page;

/**
 * 
 * @author burgetr
 */
public class PageImpl implements Page
{
    protected URL url;
    protected BoxNode root;

    public PageImpl(URL url, BoxNode root)
    {
        this.url = url;
        this.root = root;
    }
    
    @Override
    public URL getSourceURL()
    {
        return url;
    }

    @Override
    public int getWidth()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getHeight()
    {
        // TODO Auto-generated method stub
        return 0;
    }

}
