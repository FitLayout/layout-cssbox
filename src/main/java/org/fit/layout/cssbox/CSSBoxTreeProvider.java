/**
 * CSSBoxTreeProvider.java
 *
 * Created on 27. 1. 2015, 15:14:55 by burgetr
 */
package org.fit.layout.cssbox;

import java.awt.Dimension;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.fit.layout.impl.BaseBoxTreeProvider;
import org.fit.layout.model.Page;
import org.xml.sax.SAXException;

/**
 * A box tree provider implementation based on CSSBox 
 * 
 * @author burgetr
 */
public class CSSBoxTreeProvider extends BaseBoxTreeProvider
{
    private URL url;
    private int width;
    private int height;
    
    private final String[] paramNames = { "url", "width", "height" };
    private final ValueType[] paramTypes = { ValueType.STRING, ValueType.INTEGER, ValueType.INTEGER };

    public CSSBoxTreeProvider()
    {
        url = null;
        width = 1200;
        height = 800;
    }
    
    public CSSBoxTreeProvider(URL url, int width, int height)
    {
        this.url = url;
        this.width = width;
        this.height = height;
    }

    @Override
    public String getId()
    {
        return "FitLayout.CSSBox";
    }

    @Override
    public String getName()
    {
        return "CSSBox HTML and PDF renderer";
    }

    @Override
    public String getDescription()
    {
        return "Uses the CSSBox rendering engine for obtaining the box tree.";
    }

    @Override
    public String[] getParamNames()
    {
        return paramNames;
    }

    @Override
    public ValueType[] getParamTypes()
    {
        return paramTypes;
    }

    public URL getUrl()
    {
        return url;
    }

    public void setUrl(URL url)
    {
        this.url = url;
    }
    
    public void setUrl(String url)
    {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Malformed URL: " + url);
        }
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

    @Override
    public Page getPage()
    {
        CSSBoxTreeBuilder build = new CSSBoxTreeBuilder(new Dimension(width, height));
        try {
            build.parse(url);
            return build.getPage();
        } catch (IOException | SAXException e) {
            e.printStackTrace();
            return null;
        }
    }

}
