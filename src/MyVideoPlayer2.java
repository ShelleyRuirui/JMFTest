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
			// ׼��һ��Ҫ���ŵ���Ƶ�ļ���URL
			// url = new URL("file:/d:/��Ƶ��ʾ/1.mpg");

			mediaLocator = new MediaLocator("vfw://0");// ��������ý��Ŀ¼�ĵ�ַ????
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
			// ͨ������Manager��createPlayer����������һ��Player�Ķ���
			// ���������ý�岥�ŵĺ��Ŀ��ƶ���

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

		// ��player����ע����������ܸ�ż������¼�������ʱ��ִ����صĶ���
		// videoplayer.addControllerListener(this);

		// ��player���������ص���Դ����
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
		try {// player��cloneabledatasource����Դ��processor��cloneddatasource������Դ
			processor = Manager.createProcessor(ds);
			sh = new StateHelper(processor);
		} catch (IOException ez5) {
			ez5.printStackTrace();
			System.exit(-1);
		} catch (NoProcessorException ez6) {
			ez6.printStackTrace();
			System.exit(-1);
		}
		// Configure the processor,��processor����configured״̬
		if (!sh.configure(10000)) {
			System.out.println("configure wrong!");
			System.exit(-1);
		}
		System.out.println("222");
		// ������Ƶ�����ʽ
		processor.setContentDescriptor(new FileTypeDescriptor(
				FileTypeDescriptor.QUICKTIME));
		// realize the processor����processor����realized״̬
		if (!sh.realize(10000)) {
			System.out.println("realize wrong!");
			System.exit(-1);
		}
		// get the output of the processor����������processor
		DataSource outsource = processor.getDataOutput();
		//File videofile = new File("test.mpeg");
		// �������ļ�
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

	// ����player������¼�
	public void controllerUpdate(ControllerEvent ce) {
		if (ce instanceof RealizeCompleteEvent) {
			// playerʵ������ɺ����player����ǰԤ����
			dualPlayer.prefetch();
		} else if (ce instanceof PrefetchCompleteEvent) {
			if (visual != null)
				return;

			// ȡ��player�еĲ�����Ƶ����������õ���Ƶ���ڵĴ�С
			// Ȼ�����Ƶ���ڵ������ӵ�Frame�����У�
			if ((visual = dualPlayer.getVisualComponent()) != null) {
				Dimension size = visual.getPreferredSize();
				videoWidth = size.width;
				videoHeight = size.height;
				f.add(visual);
			} else {
				videoWidth = 320;
			}

			// ȡ��player�е���Ƶ���ſ�������������Ѹ������ӵ�Frame������
			if ((control = dualPlayer.getControlPanelComponent()) != null) {
				controlHeight = control.getPreferredSize().height;
				f.add(control, BorderLayout.SOUTH);
			}

			// �趨Frame���ڵĴ�С��ʹ��������Ƶ�ļ���Ĭ�ϴ�С
			f.setSize(videoWidth + insetWidth, videoHeight + controlHeight
					+ insetHeight);
			f.validate();

			// ������Ƶ���������ʼ����
			dualPlayer.start();

		} else if (ce instanceof EndOfMediaEvent) {
			// ��������Ƶ��ɺ󣬰�ʱ��������ָ�����ʼ�����ٴ����¿�ʼ����
			dualPlayer.setMediaTime(new Time(0));
			// videoplayer.start();
			// audioplayer.start();
			dualPlayer.start();
		}
	}

}
