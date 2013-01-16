

import java.awt.BorderLayout;   
import java.awt.Color;   
import java.awt.Component;   
import java.awt.Dimension;   
import java.awt.FileDialog;   
import java.awt.Graphics;   
import java.awt.Graphics2D;   
import java.awt.Image;   
import java.awt.event.ActionEvent;   
import java.awt.event.ComponentEvent;   
import java.awt.event.ComponentListener;   
import java.awt.event.WindowAdapter;   
import java.awt.event.WindowEvent;   
import java.awt.event.WindowListener;   
import java.awt.image.BufferedImage;   
import java.awt.image.ImageObserver;   
import java.io.FileOutputStream;   
import java.io.IOException;   
import java.util.Vector;   
   
import javax.media.Buffer;   
import javax.media.CannotRealizeException;   
import javax.media.CaptureDeviceInfo;   
import javax.media.CaptureDeviceManager;   
import javax.media.Format;   
import javax.media.Manager;   
import javax.media.MediaLocator;   
import javax.media.NoPlayerException;   
import javax.media.Player;   
import javax.media.control.FormatControl;   
import javax.media.control.FrameGrabbingControl;   
import javax.media.format.VideoFormat;   
import javax.media.util.BufferToImage;   
import javax.swing.AbstractAction;   
import javax.swing.JFrame;   
import javax.swing.JLabel;   
import javax.swing.JOptionPane;   
import javax.swing.JPanel;   
import javax.swing.JToolBar;   
import javax.swing.UIManager;   
import javax.swing.WindowConstants;   
import javax.swing.border.EtchedBorder;   
   
import com.sun.image.codec.jpeg.JPEGCodec;   
import com.sun.image.codec.jpeg.JPEGEncodeParam;   
import com.sun.image.codec.jpeg.JPEGImageEncoder;   
import com.sun.media.protocol.vfw.VFWCapture;   
    
public class JWebCam extends JFrame   
    implements WindowListener, ComponentListener   
{   
    
    /**  
     *   
     */   
    private static final long serialVersionUID = 1L;   
    protected final static int MIN_WIDTH  = 100;  // 320;    
    protected final static int MIN_HEIGHT = 100;  // 240;    
    
    protected static int shotCounter = 1;   
    
    protected JLabel statusBar = null;   
    protected JPanel visualContainer = null;   
    protected Component visualComponent = null;   
    protected JToolBar toolbar = null;   
    protected MyToolBarAction formatButton    = null;   
    protected MyToolBarAction captureButton   = null;   
    
    protected Player player = null;   
    protected CaptureDeviceInfo webCamDeviceInfo = null;   
    protected MediaLocator ml = null;   
    protected Dimension imageSize = null;   
    protected FormatControl formatControl = null;   
    
    protected VideoFormat currentFormat = null;   
    protected Format[] videoFormats = null;   
    protected MyVideoFormat[] myFormatList = null;   
    protected MyCaptureDeviceInfo[] myCaptureDevices = null;   
    
    protected boolean initialised = false;   
    
    /* --------------------------------------------------------------  
     * Constructor  
     * -------------------------------------------------------------- */   
    
    public JWebCam ( String frameTitle )   
    {   
        super ( frameTitle );   
        try   
        {   
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");   
        }   
        catch ( Exception cnfe )   
        {   
            System.out.println ("Note : Cannot load look and feel settings");   
        }   
    
        setSize ( 320, 260 ); // default size...    
    
        addWindowListener ( this );   
        addComponentListener ( this );   
    
        getContentPane().setLayout ( new BorderLayout() );   
    
        visualContainer = new JPanel();   
        visualContainer.setLayout ( new BorderLayout() );   
    
        getContentPane().add ( visualContainer, BorderLayout.CENTER );   
    
        statusBar = new JLabel ("")   
        {   
            /**  
             *   
             */   
            private static final long serialVersionUID = 1L;   
   
            // Nasty bug workaround    
            // The minimum JLabel size was determined by the text in the status bar    
            // So the layoutmanager wouldn't shrink the window for the video image    
            public Dimension getPreferredSize (  )   
            {   
                // get the JLabel to "allow" a minimum of 10 pixels width    
                // not to work out the minimum size from the text length    
                return ( new Dimension ( 10, super.getPreferredSize().height ) );   
            }   
        };   
    
        statusBar.setBorder ( new EtchedBorder() );   
        getContentPane().add ( statusBar, BorderLayout.SOUTH );   
    }   
    
    /* --------------------------------------------------------------  
     * Initialise  
     *  
     * @returns true if web cam is detected  
     * -------------------------------------------------------------- */   
    
    public boolean initialise ( ) throws Exception   
    {   
        MyCaptureDeviceInfo[] cams = autoDetect();   
    
        if ( cams.length > 0 )   
        {   
            if ( cams.length == 1 )   
            {   
                 System.out.println ("Note : 1 web cam detected");   
                 return ( initialise ( cams[0].capDevInfo ) );   
            }   
            else   
            {   
                System.out.println ("Note : " + cams.length + " web cams detected");   
                Object selected = JOptionPane.showInputDialog (this,   
                                                               "Select Video format",   
                                                               "Capture format selection",   
                                                               JOptionPane.INFORMATION_MESSAGE,   
                                                               null,        //  Icon icon,    
                                                               cams, // videoFormats,    
                                                               cams[0] );   
                if ( selected != null )   
                {   
                    return ( initialise ( ((MyCaptureDeviceInfo)selected).capDevInfo ) );   
                }   
                else   
                {   
                    return ( initialise ( null ) );   
                }   
            }   
        }   
        else   
        {   
            return ( initialise ( null ) );   
        }   
    }   
    
    /* -------------------------------------------------------------------  
     * Initialise  
     *  
     * @params _deviceInfo, specific web cam device if not autodetected  
     * @returns true if web cam is detected  
     * ------------------------------------------------------------------- */   
    
    public boolean initialise ( CaptureDeviceInfo _deviceInfo )   
        throws Exception   
    {   
        statusBar.setText ( "Initialising...");   
        webCamDeviceInfo = _deviceInfo;   
    
        if ( webCamDeviceInfo != null )   
        {   
            statusBar.setText ( "Connecting to : " + webCamDeviceInfo.getName() );   
    
            try   
            {   
                setUpToolBar();   
                getContentPane().add ( toolbar, BorderLayout.NORTH );   
    
                ml = webCamDeviceInfo.getLocator();   
                if ( ml != null )   
                {   
                    player = Manager.createRealizedPlayer ( ml );   
                    if ( player != null )   
                    {   
                        player.start();   
                        formatControl = (FormatControl)player.getControl ( "javax.media.control.FormatControl" );   
                        videoFormats = webCamDeviceInfo.getFormats();   
    
                        myFormatList = new MyVideoFormat[videoFormats.length];   
                        for ( int i=0; i<videoFormats.length; i++ )   
                        {   
                            myFormatList[i] = new MyVideoFormat ( (VideoFormat)videoFormats[i] );   
                        }   
    
                        Format currFormat = formatControl.getFormat();   
    
                        visualComponent = player.getVisualComponent();   
                        if ( visualComponent != null )   
                        {   
                            visualContainer.add ( visualComponent, BorderLayout.CENTER );   
    
                            if ( currFormat instanceof VideoFormat )   
                            {   
                                 currentFormat = (VideoFormat)currFormat;   
                                 imageSize = currentFormat.getSize();   
                                 System.out.println(currentFormat);
                                 visualContainer.setPreferredSize ( imageSize );   
                                 setSize ( imageSize.width, imageSize.height + statusBar.getHeight() + toolbar.getHeight() );   
                            }   
                            else   
                            {   
                                System.err.println ("Error : Cannot get current video format");   
                            }   
    
                            invalidate();   
                            pack();   
                            return ( true );   
                        }   
                        else   
                        {   
                            System.err.println ("Error : Could not get visual component");   
                            return ( false );   
                        }   
                    }   
                    else   
                    {   
                        System.err.println ("Error : Cannot create player");   
                        statusBar.setText ( "Cannot create player" );   
                        return ( false );   
                    }   
                }   
                else   
                {   
                    System.err.println ("Error : No MediaLocator for " + webCamDeviceInfo.getName() );   
                    statusBar.setText ( "No Media Locator for : " + webCamDeviceInfo.getName() );   
                    return ( false );   
                }   
            }   
            catch ( IOException ioEx )   
            {   
                System.err.println ("Error connecting to [" + webCamDeviceInfo.getName() + "] : " + ioEx.getMessage() );   
    
                statusBar.setText ( "Connecting to : " + webCamDeviceInfo.getName() );   
    
                return ( false );   
            }   
            catch ( NoPlayerException npex )   
            {   
                statusBar.setText ("Cannot create player");   
                return ( false );   
            }   
            catch ( CannotRealizeException nre )   
            {   
                statusBar.setText ( "Cannot realize player");   
                return ( false );   
            }   
        }   
        else   
        {   
            return ( false );   
        }   
    }   
    
    
    /* -------------------------------------------------------------------  
     * Dynamically create menu items  
     *  
     * @returns the device info object if found, null otherwise  
     * ------------------------------------------------------------------- */   
    
    public void setFormat ( VideoFormat selectedFormat )   
    {   
        if ( formatControl != null )   
        {   
            // player.stop();       // not needed and big performance hit    
    
            currentFormat = selectedFormat;   
    
            if ( visualComponent != null )   
            {   
                 visualContainer.remove ( visualComponent );   
            }   
    
            imageSize = currentFormat.getSize();   
            visualContainer.setPreferredSize ( imageSize );   
    
            statusBar.setText ( "Format : " + currentFormat );   
    
            System.out.println ("Format : " + currentFormat );   
    
    
            formatControl.setFormat ( currentFormat );   
    
            // player.start();  // not needed and big performance hit    
    
            visualComponent = player.getVisualComponent();   
            if ( visualComponent != null )   
            {   
                visualContainer.add ( visualComponent, BorderLayout.CENTER );   
            }   
    
            invalidate();       // let the layout manager work out the sizes    
            pack();   
        }   
        else   
        {   
            System.out.println ("Visual component not an instance of FormatControl");   
            statusBar.setText ( "Visual component cannot change format" );   
        }   
    }   
    
    
    
    public VideoFormat getFormat ( )   
    {   
        return ( currentFormat );   
    }   
    
    
    
    protected void setUpToolBar ( )   
    {   
        toolbar = new JToolBar();   
    
    
        // Note : due to cosmetic glitches when undocking and docking the toolbar,    
        //        I've set this to false.    
        toolbar.setFloatable(false);   
    
        // Note : If you supply the 16 x 16 bitmaps then you can replace    
        // the commented line in the MyToolBarAction constructor    
    
        formatButton    = new MyToolBarAction ( "Resolution", "BtnFormat.jpg" );   
        captureButton   = new MyToolBarAction ( "Capture",    "BtnCapture.jpg" );   
    
        toolbar.add ( formatButton );   
        toolbar.add ( captureButton );   
    
        getContentPane().add ( toolbar, BorderLayout.NORTH );   
    }   
    
    
    protected void toolbarHandler ( MyToolBarAction actionBtn )   
    {   
        if ( actionBtn == formatButton )   
        {   
            Object selected = JOptionPane.showInputDialog (this,   
                                                           "Select Video format",   
                                                           "Capture format selection",   
                                                           JOptionPane.INFORMATION_MESSAGE,   
                                                           null,        //  Icon icon,    
                                                           myFormatList, // videoFormats,    
                                                           currentFormat );   
            if ( selected != null )   
            {   
                setFormat ( ((MyVideoFormat)selected).format );   
            }   
        }   
        else if ( actionBtn == captureButton )   
        {   
            Image photo = grabFrameImage ( );   
            if ( photo != null )   
            {   
                MySnapshot snapshot = new MySnapshot ( photo, new Dimension ( imageSize ) );   
            }   
            else   
            {   
                System.err.println ("Error : Could not grab frame");   
            }   
        }   
    }   
    
    
    /* -------------------------------------------------------------------  
     * autoDetects the first web camera in the system  
     * searches for video for windows ( vfw ) capture devices  
     *  
     * @returns the device info object if found, null otherwise  
     * ------------------------------------------------------------------- */   
    
    public MyCaptureDeviceInfo[ ] autoDetect (  )   
    {   
        Vector list = CaptureDeviceManager.getDeviceList ( null );   
    
        CaptureDeviceInfo devInfo = null;   
        String name;   
        Vector capDevices = new Vector();   
    
        if ( list != null )   
        {   
    
            for ( int i=0; i<list.size(); i++ )   
            {   
                devInfo = (CaptureDeviceInfo)list.elementAt ( i );   
                name = devInfo.getName();   
    
                if ( name.startsWith ("vfw:") )   
                {   
                    System.out.println ("DeviceManager List : " + name );   
                    capDevices.addElement ( new MyCaptureDeviceInfo ( devInfo ) );   
                }   
            }   
        }   
        else   
        {   
            for ( int i = 0; i<list.size(); i++ )   
            {   
                try   
                {   
                    name = VFWCapture.capGetDriverDescriptionName ( i );   
                    if (name != null && name.length() > 1)   
                    {   
                        devInfo = com.sun.media.protocol.vfw.VFWSourceStream.autoDetect ( i );   
                        if ( devInfo != null )   
                        {   
                            System.out.println ("VFW Autodetect List : " + name );   
                            capDevices.addElement ( new MyCaptureDeviceInfo ( devInfo ) );   
                        }   
                    }   
                }   
                catch ( Exception ioEx )   
                {   
    
                    System.err.println ("Error connecting to [" + webCamDeviceInfo.getName() + "] : " + ioEx.getMessage() );   
    
                    // ignore errors detecting device    
                    statusBar.setText ( "AutoDetect failed : " + ioEx.getMessage() );   
                }   
            }   
        }   
    
        MyCaptureDeviceInfo[] detected = new MyCaptureDeviceInfo[ capDevices.size() ];   
        for ( int i=0; i<capDevices.size(); i++ )   
        {   
            detected[i] = (MyCaptureDeviceInfo)capDevices.elementAt ( i );   
        }   
    
        return ( detected );   
    }   
    
    
    /* -------------------------------------------------------------------  
     * deviceInfo  
     *  
     * @note outputs text information  
     * ------------------------------------------------------------------- */   
    
    public void deviceInfo ( )   
    {   
        if ( webCamDeviceInfo != null )   
        {   
            Format[] formats = webCamDeviceInfo.getFormats();   
    
            if ( ( formats != null ) && ( formats.length > 0 ) )   
            {   
            }   
    
            for ( int i=0; i<formats.length; i++ )   
            {   
                Format aFormat = formats[i];   
                if ( aFormat instanceof VideoFormat )   
                {   
                    Dimension dim = ((VideoFormat)aFormat).getSize();   
                    // System.out.println ("Video Format " + i + " : " + formats[i].getEncoding() + ", " + dim.width + " x " + dim.height );    
                }   
            }   
        }   
        else   
        {   
            System.out.println ("Error : No web cam detected");   
        }   
    }   
    
    /* -------------------------------------------------------------------  
     * grabs a frame's buffer from the web cam / device  
     *  
     * @returns A frames buffer  
     * ------------------------------------------------------------------- */   
    
    public Buffer grabFrameBuffer ( )   
    {   
        if ( player != null )   
        {   
            FrameGrabbingControl fgc = (FrameGrabbingControl)player.getControl ( "javax.media.control.FrameGrabbingControl" );   
            if ( fgc != null )   
            {   
                return ( fgc.grabFrame() );   
            }   
            else   
            {   
                System.err.println ("Error : FrameGrabbingControl is null");   
                return ( null );   
            }   
        }   
        else   
        {   
            System.err.println ("Error : Player is null");   
            return ( null );   
        }   
    }   
    
    
    /* -------------------------------------------------------------------  
     * grabs a frame's buffer, as an image, from the web cam / device  
     *  
     * @returns A frames buffer as an image  
     * ------------------------------------------------------------------- */   
    
    public Image grabFrameImage ( )   
    {   
        Buffer buffer = grabFrameBuffer();   
        if ( buffer != null )   
        {   
            // Convert it to an image    
            BufferToImage btoi = new BufferToImage ( (VideoFormat)buffer.getFormat() );   
            if ( btoi != null )   
            {   
                Image image = btoi.createImage ( buffer );   
                if ( image != null )   
                {   
                    return ( image );   
                }   
                else   
                {   
                    System.err.println ("Error : BufferToImage cannot convert buffer");   
                    return ( null );   
                }   
            }   
            else   
            {   
                System.err.println ("Error : cannot create BufferToImage instance");   
                return ( null );   
            }   
        }   
        else   
        {   
            System.out.println ("Error : Buffer grabbed is null");   
            return ( null );   
        }   
    }   
    
    
    /* -------------------------------------------------------------------  
     * Closes and cleans up the player  
     *  
     * ------------------------------------------------------------------- */   
    
    public void playerClose ( )   
    {   
        if ( player != null )   
        {   
            player.close();   
            player.deallocate();   
            player = null;   
        }   
    }   
    
    public void windowClosing ( WindowEvent e )   
    {   
        playerClose();   
        System.exit ( 1 );   
    }   
    
    public void componentResized ( ComponentEvent e )   
    {   
        Dimension dim = getSize();   
        boolean mustResize = false;   
    
        if ( dim.width == MIN_WIDTH )   
        {   
             dim.width = MIN_WIDTH;   
             mustResize = true;   
        }   
    
        if ( dim.height ==  MIN_HEIGHT )   
        {   
            dim.height = MIN_HEIGHT;   
            mustResize = true;   
        }   
    
        if ( mustResize )   
             setSize ( dim );   
    }   
    
    public void windowActivated ( WindowEvent e )   {   }   
    
    public void windowClosed ( WindowEvent e )      {   }   
    
    public void windowDeactivated ( WindowEvent e ) {   }   
    
    public void windowDeiconified ( WindowEvent e ) {   }   
    
    public void windowIconified ( WindowEvent e )   {   }   
    
    public void windowOpened ( WindowEvent e )      {   }   
    
    public void componentHidden(ComponentEvent e)   {   }   
    
    public void componentMoved(ComponentEvent e)    {   }   
    
    public void componentShown(ComponentEvent e)    {   }   
    
    
    protected void finalize ( ) throws Throwable   
    {   
        playerClose();   
        super.finalize();   
    }   
    
    class MyToolBarAction extends AbstractAction   
    {   
        public MyToolBarAction ( String name, String imagefile )   
        {   
            // Note : Use version this if you supply your own toolbar icons    
            // super ( name, new ImageIcon ( imagefile ) );    
    
            super ( name );   
        }   
    
        public void actionPerformed ( ActionEvent event )   
        {   
            toolbarHandler ( this );   
        }   
    };   
    
    
    class MyVideoFormat   
    {   
        public VideoFormat format;   
    
        public MyVideoFormat ( VideoFormat format )   
        {   
            this.format = format;   
        }   
    
        public String toString ( )   
        {   
            Dimension dim = format.getSize();   
            return ( format.getEncoding() + " [ " + dim.width + " x " + dim.height + " ]" );   
        }   
    };   
    
    
    class MyCaptureDeviceInfo   
    {   
        public CaptureDeviceInfo capDevInfo;   
    
        public MyCaptureDeviceInfo ( CaptureDeviceInfo devInfo )   
        {   
            capDevInfo = devInfo;   
        }   
    
        public String toString ( )   
        {   
            return ( capDevInfo.getName() );   
        }   
    };   
    
    
    class MySnapshot extends JFrame implements ImageObserver   
    {   
        protected Image photo = null;   
        protected int shotNumber;   
    
        public MySnapshot ( Image grabbedFrame, Dimension imageSize )   
        {   
            super ( );   
    
            shotNumber = shotCounter++;   
            setTitle ( "Photo" + shotNumber );   
    
            photo = grabbedFrame;   
    
            setDefaultCloseOperation ( WindowConstants.DISPOSE_ON_CLOSE );   
    
            int imageHeight = photo.getWidth  ( this );   
            int imageWidth  = photo.getHeight ( this );   
    
            setSize ( imageSize.width, imageSize.height );   
    
            final FileDialog saveDialog = new FileDialog ( this, "Save JPEG", FileDialog.SAVE );   
            final JFrame thisCopy = this;   
            saveDialog.setFile ( "Photo" + shotNumber );   
    
            addWindowListener ( new WindowAdapter()   
                {   
                    public void windowClosing ( WindowEvent e )   
                    {   
                        saveDialog.show();   
    
                        String filename = saveDialog.getFile();   
    
                        if ( filename != null )   
                        {   
                            if ( saveJPEG ( filename ) )   
                            {   
                                JOptionPane.showMessageDialog ( thisCopy, "Saved " + filename );   
                                setVisible ( false );   
                                dispose();   
                            }   
                            else   
                            {   
                                JOptionPane.showMessageDialog ( thisCopy, "Error saving " + filename );   
                            }   
                        }   
                        else   
                        {   
                            setVisible ( false );   
                            dispose();   
                        }   
                    }   
                }   
            );   
    
            setVisible ( true );   
        }   
    
    
        public void paint ( Graphics g )   
        {   
            super.paint ( g );   
    
            g.drawImage ( photo, 0, 0, getWidth(), getHeight(), Color.black, this );   
        }   
    
    
    
        /* -------------------------------------------------------------------  
         * Saves an image as a JPEG  
         *  
         * @params the image to save  
         * @params the filename to save the image as  
         * ------------------------------------------------------------------- */   
    
        public boolean saveJPEG ( String filename )   
        {   
            boolean saved = false;   
            BufferedImage bi = new BufferedImage ( photo.getWidth(null),   
                                                   photo.getHeight(null),   
                                                   BufferedImage.TYPE_INT_RGB );   
            Graphics2D g2 = bi.createGraphics();   
            g2.drawImage ( photo, null, null );   
            FileOutputStream out = null;   
    
            try   
            {   
                out = new FileOutputStream ( filename );   
                JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder ( out );   
                JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam ( bi );   
                param.setQuality ( 1.0f, false );   // 100% high quality setting, no compression    
                encoder.setJPEGEncodeParam ( param );   
                encoder.encode ( bi );   
                out.close();   
                saved = true;   
            }   
            catch ( Exception ex )   
            {   
                System.out.println ("Error saving JPEG : " + ex.getMessage() );   
            }   
    
            return ( saved );   
        }   
    
    }   // of MySnapshot    
    
    
    
    public static void main (String[] args )   
    {   
        try   
        {   
            JWebCam myWebCam = new JWebCam ( "Web Cam Capture" );   
            myWebCam.setVisible ( true );   
    
            if ( !myWebCam.initialise() )   
            {   
                System.out.println ("Web Cam not detected / initialised");   
                System.exit(0);
            }   
        }   
        catch ( Exception ex )   
        {   
            ex.printStackTrace();   
        }   
    }   
}  