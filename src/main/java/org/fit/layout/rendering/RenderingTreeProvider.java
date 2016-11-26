/**
 * CSSBoxTreeProvider.java
 *
 * Created on 27. 1. 2015, 15:14:55 by burgetr
 */
package org.fit.layout.rendering;

import java.awt.Dimension;
import java.io.IOException;
import java.net.URL;

import org.fit.cssbox.layout.Viewport;
import org.fit.layout.impl.BaseBoxTreeProvider;
import org.fit.layout.model.Page;
import org.xml.sax.SAXException;

/**
 * A box tree provider implementation based on CSSBox 
 * 
 * @author burgetr
 */
public class RenderingTreeProvider extends BaseBoxTreeProvider
{
    private String urlstring;
    private int width;
    private int height;
    private boolean useVisualBounds;
    private boolean preserveAux;
    private boolean replaceImagesWithAlt; //not published as a parameter now
    
    private final String[] paramNames = { "url", "width", "height", "useVisualBounds", "preserveAux" };
    private final ValueType[] paramTypes = { ValueType.STRING, ValueType.INTEGER, ValueType.INTEGER, ValueType.BOOLEAN, ValueType.BOOLEAN };

    private RenderingTreeBuilder builder;
    
    public RenderingTreeProvider()
    {
        urlstring = null;
        width = 1200;
        height = 800;
        useVisualBounds = true;
        preserveAux = false;
    }
    
    public RenderingTreeProvider(URL url, int width, int height, boolean useVisualBounds, boolean preserveAux)
    {
        this.urlstring = url.toString();
        this.width = width;
        this.height = height;
        this.useVisualBounds = useVisualBounds;
        this.preserveAux = preserveAux;
    }

    @Override
    public String getId()
    {
        return "FitLayout.Rendering";
    }

    @Override
    public String getName()
    {
        return "CSSBox visual rendering HTML and PDF renderer";
    }

    @Override
    public String getDescription()
    {
        return "Uses the visual renderer mechanism of the CSSBox rendering engine for obtaining the box tree.";
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

    public String getUrl()
    {
        return urlstring;
    }

    public void setUrl(String url)
    {
        urlstring = new String(url);
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
    
    public boolean getUseVisualBounds()
    {
        return useVisualBounds;
    }

    public void setUseVisualBounds(boolean useVisualBounds)
    {
        this.useVisualBounds = useVisualBounds;
    }

    public boolean getPreserveAux()
    {
        return preserveAux;
    }

    public void setPreserveAux(boolean preserveAux)
    {
        this.preserveAux = preserveAux;
    }

    public boolean getReplaceImagesWithAlt()
    {
        return replaceImagesWithAlt;
    }

    public void setReplaceImagesWithAlt(boolean replaceImagesWithAlt)
    {
        this.replaceImagesWithAlt = replaceImagesWithAlt;
    }

    @Override
    public Object[] getParamRange(String name)
    {
        Object[] ret = new Object[2];
        switch (name)
        {
            case "width":
            case "height":
                ret[0] = 0;
                ret[1] = 9999;
                return ret;
            case "url":
                ret[0] = 0;
                ret[1] = 64;
                return ret;
            default:
                return super.getParamRange(name);
        }
    }

    @Override
    public Page getPage()
    {
        builder = new RenderingTreeBuilder(new Dimension(width, height), useVisualBounds, preserveAux, replaceImagesWithAlt);
        try {
            builder.parse(urlstring);
            return builder.getPage();
        } catch (IOException | SAXException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public Viewport getViewport()
    {
        if (builder != null)
            return builder.getViewport();
        else
            return null;
    }

}
