import java.io.IOException;

import javax.media.Buffer;
import javax.media.CannotRealizeException;
import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.DataSink;
import javax.media.Format;
import javax.media.IncompatibleSourceException;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSinkException;
import javax.media.NoPlayerException;
import javax.media.NoProcessorException;
import javax.media.Player;
import javax.media.Processor;
import javax.media.control.FormatControl;
import javax.media.control.FrameGrabbingControl;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.util.BufferToImage;

import jmapps.util.JMFUtils;

public class Test {
	
	
	protected Object doInBackground() throws Exception {
	    CaptureDeviceInfo videoDevice = configPanel.getVideoDevice();
	    Format videoFormat = configPanel.getVideoFormat();
	    CaptureDeviceInfo audioDevice = configPanel.getAudioDevice();
	    Format audioFormat = configPanel.getAudioFormat();
	    try {
	        player = createPlayer(videoDevice, videoFormat, audioDevice, audioFormat);
	    } catch (Exception e) {
	        //发生异常构建player失败，再次尝试
	        if (player == null) {
	            player = createPlayer(videoDevice, videoFormat, audioDevice, audioFormat);
	        }
	    }
	    return WORKE_FINISH;
	}
	/**
	 * 利用给定的设备和格式构建一个Player
	 * 
	 * @param videoDevice
	 * @param videoFormat
	 * @param audioDevice
	 * @param audioFormat
	 * @return
	 * @throws IncompatibleSourceException
	 * @throws IOException
	 * @throws NoPlayerException
	 * @throws CannotRealizeException
	 */
	private Player createPlayer(CaptureDeviceInfo videoDevice,
			Format videoFormat, CaptureDeviceInfo audioDevice,
			Format audioFormat) throws IncompatibleSourceException,
			IOException, NoPlayerException, CannotRealizeException {
		DataSource mergeDataSource = null;
		DataSource videoDataSource = null;
		DataSource audioDataSource = null;
		if (videoDevice != null) {
			videoDataSource = JMFUtils.initializeCaptureDataSource(null,
					videoDevice.getName(), videoFormat);
			videoDataSource.connect();
		}
		if (audioDevice != null) {
			audioDataSource = JMFUtils.initializeCaptureDataSource(null,
					audioDevice.getName(), audioFormat);
		}
		if (videoDataSource == null && audioDataSource != null) {
			mergeDataSource = audioDataSource;
		} else if (videoDataSource != null && audioDataSource == null) {
			mergeDataSource = videoDataSource;
		} else if (videoDataSource != null && audioDataSource != null) {
			mergeDataSource = Manager.createMergingDataSource(new DataSource[] {
					videoDataSource, audioDataSource });
		}
		return Manager.createRealizedPlayer(mergeDataSource);
	}
}