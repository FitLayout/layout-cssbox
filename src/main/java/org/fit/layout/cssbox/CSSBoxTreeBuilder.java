/**
 * CSSBoxTreeBuilder.java
 *
 * Created on 24. 10. 2014, 23:52:25 by burgetr
 */
package org.fit.layout.cssbox;

import java.awt.Color;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.fit.cssbox.css.CSSNorm;
import org.fit.cssbox.css.DOMAnalyzer;
import org.fit.cssbox.io.DOMSource;
import org.fit.cssbox.io.DefaultDOMSource;
import org.fit.cssbox.io.DefaultDocumentSource;
import org.fit.cssbox.io.DocumentSource;
import org.fit.cssbox.layout.Box;
import org.fit.cssbox.layout.BrowserCanvas;
import org.fit.cssbox.layout.ElementBox;
import org.fit.cssbox.layout.Viewport;
import org.fit.cssbox.pdf.PdfBrowserCanvas;
import org.fit.layout.impl.DefaultBox;
import org.fit.layout.model.Box.DisplayType;
import org.fit.layout.model.Page;
import org.fit.layout.model.Rectangular;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cz.vutbr.web.css.MediaSpec;

/**
 * This class implements building the box tree using the CSSBox rendering engine.
 * 
 * @author burgetr
 */
public class CSSBoxTreeBuilder
{
    private static Logger log = LoggerFactory.getLogger(CSSBoxTreeBuilder.class);

    protected URL pageUrl;
    protected String pageTitle;
    
    /** The resulting page */
    protected PageImpl page;
    
    /** Requested page dimensions */
    protected Dimension pageSize;
    
    /** Use real visual bounds instead of the element content bounds for building the box hierarchy */
    protected boolean useVisualBounds;
    
    /** a counter for assigning the box order */
    private int order_counter;
    
   
    public CSSBoxTreeBuilder(Dimension pageSize, boolean useVisualBounds)
    {
        this.pageSize = pageSize;
        this.useVisualBounds = useVisualBounds;
    }
    
    public void parse(URL url) throws IOException, SAXException
    {
        //render the page
        BrowserCanvas canvas = renderUrl(url, pageSize);
        PageImpl pg = page = new PageImpl(pageUrl);
        pg.setTitle(pageTitle);
        
        //construct the box tree
        ElementBox rootbox = canvas.getViewport();
        BoxNode root = buildTree(rootbox);
        
        //initialize the page
        pg.setRoot(root);
        pg.setWidth(rootbox.getWidth());
        pg.setHeight(rootbox.getHeight());
    }
    
    public void parseList(List<URL> list) throws IOException, SAXException
    {
        int twidth = 0;
        int theight = 0;

        DefaultBox main = new DefaultBox();
        
        for (URL url : list)
        {
            log.info("Parsing: {}", url);
            //render the page
            BrowserCanvas canvas = renderUrl(url, pageSize);
            ElementBox rootbox = canvas.getViewport();
            BoxNode root = buildTree(rootbox);
            
            //wrap the page with a new block box
            DefaultBox pageBox = new DefaultBox();
            pageBox.add(root);
            pageBox.setDisplayType(DisplayType.BLOCK);
            pageBox.setBounds(new Rectangular(root.getBounds()));
            pageBox.getBounds().setX1(0);
            pageBox.getBounds().setY1(theight);
            pageBox.setVisualBounds(new Rectangular(pageBox.getBounds()));
            
            //add to the root
            main.add(pageBox);
            twidth = Math.max(twidth, pageBox.getWidth());
            theight = theight + pageBox.getHeight();
        }
        main.setBounds(new Rectangular(0, 0, twidth, theight));
        main.setVisualBounds(new Rectangular(0, 0, twidth, theight));
        
        //initialize the page
        page = new PageImpl(list.get(0));
        page.setRoot(main);
        page.setWidth(twidth);
        page.setHeight(theight);
    }
    
    public void parse(String urlstring) throws MalformedURLException, IOException, SAXException
    {
        urlstring = urlstring.trim();
        if (urlstring.startsWith("http:") ||
            urlstring.startsWith("https:") ||
            urlstring.startsWith("ftp:") ||
            urlstring.startsWith("file:"))
        {
            parse(new URL(urlstring));
        }
        else if (urlstring.startsWith("list:"))
        {
            List<URL> list = loadList(urlstring.substring(5));
            parseList(list);
        }
        else
            throw new MalformedURLException("Unsupported protocol in " + urlstring);
    }
    
    public Page getPage()
    {
        return page;
    }
    
    //===================================================================
    
    protected BrowserCanvas renderUrl(URL url, Dimension pageSize) throws IOException, SAXException
    {
        DocumentSource src = new DefaultDocumentSource(url);
        pageUrl = src.getURL();
        InputStream is = src.getInputStream();
        String mime = src.getContentType();
        if (mime == null)
            mime = "text/html";
        int p = mime.indexOf(';');
        if (p != -1)
            mime = mime.substring(0, p).trim();
        log.info("File type: " + mime);
        
        if (mime.equals("application/pdf"))
        {
            PDDocument doc = loadPdf(is);
            BrowserCanvas canvas = new PdfBrowserCanvas(doc, null, pageSize, src.getURL());
            pageTitle = "";
            return canvas;
        }
        else
        {
            DOMSource parser = new DefaultDOMSource(src);
            Document doc = parser.parse();
            pageTitle = findPageTitle(doc);
            
            String encoding = parser.getCharset();
            
            MediaSpec media = new MediaSpec("screen");
            //updateCurrentMedia(media);
            
            DOMAnalyzer da = new DOMAnalyzer(doc, src.getURL());
            if (encoding == null)
                encoding = da.getCharacterEncoding();
            da.setDefaultEncoding(encoding);
            da.setMediaSpec(media);
            da.attributesToStyles();
            da.addStyleSheet(null, CSSNorm.stdStyleSheet(), DOMAnalyzer.Origin.AGENT);
            da.addStyleSheet(null, CSSNorm.userStyleSheet(), DOMAnalyzer.Origin.AGENT);
            da.addStyleSheet(null, CSSNorm.formsStyleSheet(), DOMAnalyzer.Origin.AGENT);
            da.getStyleSheets();
            
            BrowserCanvas contentCanvas = new BrowserCanvas(da.getRoot(), da, src.getURL());
            contentCanvas.getConfig().setLoadImages(false);
            contentCanvas.getConfig().setLoadBackgroundImages(false);
            contentCanvas.getConfig().setReplaceImagesWithAlt(true);
            contentCanvas.createLayout(pageSize);
            
            src.close();

            return contentCanvas;
        }
        
    }
    
    private PDDocument loadPdf(InputStream is) throws IOException
    {
        PDDocument document = null;
        document = PDDocument.load(is);
        return document;
    }
    
    private String findPageTitle(Document doc)
    {
        NodeList heads = doc.getElementsByTagName("head");
        if (heads.getLength() > 0)
        {
            Element head = (Element) heads.item(0);
            NodeList titles = head.getElementsByTagName("title");
            if (titles.getLength() > 0)
            {
                final String ret = titles.item(0).getTextContent();
                return (ret == null) ? null : ret.trim();
            }
        }
        return "";
    }
    
    //===================================================================
    
    protected BoxNode buildTree(ElementBox rootbox)
    {
        //create the working list of nodes
        log.trace("LIST");
        Vector<BoxNode> boxlist = new Vector<BoxNode>();
        order_counter = 1;
        createBoxList(rootbox, boxlist);
        
        //create the tree
        log.trace("A1");
        BoxNode root = createBoxTree(rootbox, boxlist, true); //create a nesting tree based on the content bounds
        log.trace("A2");
        Color bg = rootbox.getBgcolor();
        if (bg == null) bg = Color.WHITE;
        computeBackgrounds(root, bg); //compute the efficient background colors
        log.trace("A2.5");
        root.recomputeVisualBounds(); //compute the visual bounds for the whole tree
        log.trace("A3");
        root = createBoxTree(rootbox, boxlist, !useVisualBounds); //create the nesting tree based on the visual bounds or content bounds depending on the settings
        root.recomputeVisualBounds(); //compute the visual bounds for the whole tree
        root.recomputeBounds(); //compute the real bounds of each node
        log.trace("A4");
        return root;
    }
    
    /**
     * Recursively creates a list of all the visible boxes in a box subtree. The nodes are 
     * added to the end of a specified list. The previous content of the list 
     * remains unchanged. The 'viewport' box is ignored.
     * @param root the source root box
     * @param list the list that will be filled with the nodes
     */
    private void createBoxList(Box root, Vector<BoxNode> list)
    {
        if (root.isDisplayed())
        {
            if (!(root instanceof Viewport)
                && root.isVisible()
                && root.getWidth() > 0 && root.getHeight() > 0)
            {
                BoxNode newnode = new BoxNode(root, page);
                newnode.setOrder(order_counter++);
                list.add(newnode);
            }
            if (root instanceof ElementBox)
            {
                ElementBox elem = (ElementBox) root;
                for (int i = elem.getStartChild(); i < elem.getEndChild(); i++)
                    createBoxList(elem.getSubBox(i), list);
            }
        }
    }

    /**
     * Creates a tree of box nesting based on the content bounds of the boxes.
     * This tree is only used for determining the backgrounds.
     * 
     * @param boxlist the list of boxes to build the tree from
     * @param full when set to true, the tree is build according to the content bounds
     * of each box. Otherwise, only the visual bounds are used.
     */
    private BoxNode createBoxTree(ElementBox rootbox, Vector<BoxNode> boxlist, boolean full)
    {
        //a working copy of the box list
        Vector<BoxNode> list = new Vector<BoxNode>(boxlist);

        //an artificial root node
        BoxNode root = new BoxNode(rootbox, page);
        root.setOrder(0);
        //detach the nodes from any old trees
        for (BoxNode node : list)
            node.removeFromTree();
        
        //when working with visual bounds, remove the boxes that are not visually separated
        if (!full)
        {
            for (Iterator<BoxNode> it = list.iterator(); it.hasNext(); )
            {
                BoxNode node = it.next();
                if (!node.isVisuallySeparated() || !node.isVisible())
                    it.remove();
            }
        }
        
        //let each node choose it's children - find the roots and parents
        for (BoxNode node : list)
            node.markNodesInside(list, full);
        
        //choose the roots
        for (Iterator<BoxNode> it = list.iterator(); it.hasNext();)
        {
            BoxNode node = it.next();
            
            /*if (!full) //DEBUG
            {
               if (node.toString().contains("mediawiki") || node.toString().contains("globalWrapper"))
                    System.out.println(node + " => " + node.nearestParent);
            }*/
            
            if (node.isRootNode())
            {
                root.add(node);
                it.remove();
            }
        }
        
        //recursively choose the children
        for (int i = 0; i < root.getChildCount(); i++)
            root.getChildBox(i).takeChildren(list);
        
        return root;
    }
    
    /**
     * Computes efficient background color for all the nodes in the tree
     */
    private void computeBackgrounds(BoxNode root, Color currentbg)
    {
        Color newbg = root.getBackgroundColor();
        if (newbg == null)
            newbg = currentbg;
        root.setEfficientBackground(newbg);
        root.setBackgroundSeparated(!newbg.equals(currentbg));
        
        for (int i = 0; i < root.getChildCount(); i++)
            computeBackgrounds(root.getChildBox(i), newbg);
    }
    
    //===================================================================
    
    private List<URL> loadList(String filename)
    {
        List<URL> ret = new ArrayList<URL>();
        try
        {
            BufferedReader read = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = read.readLine()) != null)
            {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#"))
                {
                    ret.add(new URL(line));
                }
            }
            read.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return ret;
    }
    
}
