import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.DataSink;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSinkException;
import javax.media.NoPlayerException;
import javax.media.NoProcessorException;
import javax.media.NotConfiguredError;
import javax.media.NotRealizedError;
import javax.media.Player;
import javax.media.PrefetchCompleteEvent;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.Time;
import javax.media.control.StreamWriterControl;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;

import jmapps.util.StateHelper;

public class MyVideoPlayer2 implements ControllerListener {
	public static void main(String[] args) throws Exception {
		MyVideoPlayer2 sp = new MyVideoPlayer2();
		sp.play();
//		sp.saveVideo();
	}

	private Frame f;
	// private Player videoplayer;
	// private Player audioplayer;
	private Player dualPlayer;
	private Panel panel;
	private Component visual;
	private Component control = null;
	private MediaLocator mediaLocator;
	private MediaLocator audioLocator;
	private DataSource ds = null;
	private StateHelper sh = null;

	public void play() {
		f = new Frame("MyPlayer");
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				if (dualPlayer != null) {
					dualPlayer.close();
				}
				System.exit(0);
			}
		});
		f.setSize(500, 400);

		f.setVisible(true);
		String filename = null;
		URL url = null;
		try {
			// 准备一个要播放的视频文件的URL
			// url = new URL("file:/d:/视频演示/1.mpg");

			mediaLocator = new MediaLocator("vfw://0");// 此类描述媒体目录的地址????
			audioLocator = new MediaLocator("javasound://44100");
			// saveVideo();
			// FileDialog fd = new FileDialog(f, "Select File",
			// FileDialog.LOAD);
			// fd.show();
			// filename = fd.getDirectory() + fd.getFile();
		} catch (Exception e) {
			System.out.println(e.toString());
		}

		try {
			// 通过调用Manager的createPlayer方法来创建一个Player的对象
			// 这个对象是媒体播放的核心控制对象

			// videoplayer = Manager.createPlayer(mediaLocator);
			// audioplayer = Manager.createPlayer(audioLocator);

			DataSource[] dataSources = new DataSource[2];
			dataSources[0] = Manager.createDataSource(mediaLocator);
			dataSources[1] = Manager.createDataSource(audioLocator);
			ds = Manager.createMergingDataSource(dataSources);
			ds = Manager.createCloneableDataSource(ds);
			dualPlayer = Manager.createPlayer(ds);

		} catch (Exception e1) {
			e1.printStackTrace();
		}

		// 对player对象注册监听器，能噶偶在相关事件发生的时候执行相关的动作
		// videoplayer.addControllerListener(this);

		// 让player对象进行相关的资源分配
		// videoplayer.realize();
		// audioplayer.realize();
		dualPlayer.realize();
		dualPlayer.addControllerListener(this);
		// try {
		// saveVideo(ds);
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
	}

	private void saveVideo() throws Exception {
		System.out.println("ppfmjfdsdf");
		Processor processor = null;
		DataSink dataSink = null;
		try {// player用cloneabledatasource数据源，processor用cloneddatasource的数据源
			processor = Manager.createProcessor(ds);
			sh = new StateHelper(processor);
		} catch (IOException ez5) {
			ez5.printStackTrace();
			System.exit(-1);
		} catch (NoProcessorException ez6) {
			ez6.printStackTrace();
			System.exit(-1);
		}
		// Configure the processor,让processor进入configured状态
		if (!sh.configure(10000)) {
			System.out.println("configure wrong!");
			System.exit(-1);
		}
		System.out.println("222");
		// 设置视频输出格式
		processor.setContentDescriptor(new FileTypeDescriptor(
				FileTypeDescriptor.QUICKTIME));
		// realize the processor，让processor进入realized状态
		if (!sh.realize(10000)) {
			System.out.println("realize wrong!");
			System.exit(-1);
		}
		// get the output of the processor，并且启动processor
		DataSource outsource = processor.getDataOutput();
		//File videofile = new File("test.mpeg");
		// 创建新文件
//		try {
//			videofile.createNewFile();
//		} catch (IOException e2) {
//			// TODO Auto-generated catch block
//			e2.printStackTrace();
//		}
		MediaLocator dest = new MediaLocator("file://e:/media.mov");

		processor.start();

		/*
		 * try { Thread.sleep(1000); } catch (InterruptedException e) { // TODO
		 * Auto-generated catch block e.printStackTrace(); }
		 */
		try {
			dataSink = Manager.createDataSink(outsource, dest);
			dataSink.open();
		} catch (NoDataSinkException ez1) {
			ez1.printStackTrace();
			System.exit(-1);
		} catch (IOException ez2) {
			ez2.printStackTrace();
			System.exit(-1);
		} catch (SecurityException ez3) {
			ez3.printStackTrace();
			System.exit(-1);
		}
		try {
			dataSink.start();
		} catch (IOException ez4) {
			ez4.printStackTrace();
			System.exit(-1);
		}
		if (dataSink != null) {
			
			System.out.println("xxx!");

		}
		System.out.println("end");

	}

	private int videoWidth = 0;
	private int videoHeight = 0;
	private int controlHeight = 30;
	private int insetWidth = 10;
	private int insetHeight = 30;

	// 监听player的相关事件
	public void controllerUpdate(ControllerEvent ce) {
		if (ce instanceof RealizeCompleteEvent) {
			// player实例化完成后进行player播放前预处理
			dualPlayer.prefetch();
		} else if (ce instanceof PrefetchCompleteEvent) {
			if (visual != null)
				return;

			// 取得player中的播放视频的组件，并得到视频窗口的大小
			// 然后把视频窗口的组件添加到Frame窗口中，
			if ((visual = dualPlayer.getVisualComponent()) != null) {
				Dimension size = visual.getPreferredSize();
				videoWidth = size.width;
				videoHeight = size.height;
				f.add(visual);
			} else {
				videoWidth = 320;
			}

			// 取得player中的视频播放控制条组件，并把该组件添加到Frame窗口中
			if ((control = dualPlayer.getControlPanelComponent()) != null) {
				controlHeight = control.getPreferredSize().height;
				f.add(control, BorderLayout.SOUTH);
			}

			// 设定Frame窗口的大小，使得满足视频文件的默认大小
			f.setSize(videoWidth + insetWidth, videoHeight + controlHeight
					+ insetHeight);
			f.validate();

			// 启动视频播放组件开始播放
			dualPlayer.start();

		} else if (ce instanceof EndOfMediaEvent) {
			// 当播放视频完成后，把时间进度条恢复到开始，并再次重新开始播放
			dualPlayer.setMediaTime(new Time(0));
			// videoplayer.start();
			// audioplayer.start();
			dualPlayer.start();
		}
	}

}
