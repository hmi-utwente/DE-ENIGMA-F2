package starters;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

import flipper.FlipperMiddleware;
import nl.utwente.hmi.middleware.Middleware;
import nl.utwente.hmi.middleware.MiddlewareListener;
import nl.utwente.hmi.middleware.helpers.JSONHelper;
import nl.utwente.hmi.middleware.loader.GenericMiddlewareLoader;
import nl.utwente.hmi.middleware.worker.AbstractWorker;


public class VideoLogger extends AbstractWorker implements MiddlewareListener {
	private static Logger logger = LoggerFactory.getLogger(VideoLogger.class.getName());

	private Middleware middleware;

	private enum Camera {GENIUS,LOGITECH};
	
	//possible alternative: https://github.com/bytedeco/javacv/blob/master/samples/WebcamAndMicrophoneCapture.java
	
	private static final String VIDEO_DIR = "C:\\videos\\";

	private Process logitechVideoProcess;
	private Process geniusVideoProcess;

	private JSONHelper jh;

	private ObjectMapper om;

	private PrintWriter logitechOSWriter;
	private PrintWriter geniusOSWriter;
	
	public VideoLogger(){
		super();
	}
	
	public void init() {
		jh = new JSONHelper();
		om = new ObjectMapper();

		//start the worker thread
		new Thread(this).start();
		
		Properties ps = new Properties();
        InputStream mwProps = VideoLogger.class.getClassLoader().getResourceAsStream("P4/config/videoLogger.properties");
        
		try {
			ps.load(mwProps);
		} catch (IOException ex) {
            logger.warn("Could not load flipper middleware props file {}", mwProps);
            ex.printStackTrace();
        }
		
        GenericMiddlewareLoader gml = new GenericMiddlewareLoader(ps.getProperty("middleware"), ps);
        middleware = gml.load();
        middleware.addListener(this);
		

        //make sure we shut down the ffmpeg instance as this program shuts down
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
            	closeFFMPEG();
            }
        });
        
        /*
		//for testing a simple recording
		ObjectNode starter1 = om.createObjectNode();
		starter1.put("status", "session_start");
		starter1.put("ts", 123L);
		
		this.receiveData(starter1);
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//stop it again
        ObjectNode stopper1 = om.createObjectNode();
		stopper1.put("status", "end_session");
		stopper1.put("ts", 345L);
		
		this.receiveData(stopper1);
		*/
		
		
		/*
		//start a new one
        ObjectNode starter2 = om.createObjectNode();
		ObjectNode action3 = om.createObjectNode();
		action3.put("id", "testid2");
		action3.put("action", "START");
		
		starter2.set("command", action3);
		
		this.receiveData(starter2);

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//and another
        ObjectNode starter3 = om.createObjectNode();
		ObjectNode action4 = om.createObjectNode();
		action4.put("id", "testid3");
		action4.put("action", "START");
		
		starter3.set("command", action4);
		
		this.receiveData(starter3);
		
		*/
	}
	
	@Override
	public void receiveData(JsonNode jn) {
		addDataToQueue(jn);
	}


	/**
	 * close any previous running video recording
	 */
	private void closeFFMPEG(){
		stopLogitech();
		stopGenius();
	}
	
	private void stopLogitech() {

		if(logitechVideoProcess != null && logitechVideoProcess.isAlive()){
			logger.info("Stopping current logitech video recording");
			logitechOSWriter.println("q");
			logitechOSWriter.flush();
			
			//give it a moment to gracefully shut down
			try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			if(logitechVideoProcess != null && logitechVideoProcess.isAlive()){
				//finally destroy
				logitechVideoProcess.destroy();
				
				//and wait for it to actually close
				try {
					logitechVideoProcess.waitFor();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	private void stopGenius() {

		if(geniusVideoProcess != null && geniusVideoProcess.isAlive()){
			logger.info("Stopping current genius video recording");
			geniusOSWriter.println("q");
			geniusOSWriter.flush();
			
			//give it a moment to gracefully shut down
			try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			if(geniusVideoProcess != null && geniusVideoProcess.isAlive()){
				//finally destroy
				geniusVideoProcess.destroy();
				
				//and wait for it to actually close
				try {
					geniusVideoProcess.waitFor();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public void processData(JsonNode jn) {
		logger.debug("Got new command: {}", jn.toString());
		if(jn != null && jn.isObject() && jn.get("status") != null && jn.get("ts") != null){

			
			//should we start recording now?
			long timeStamp = jn.get("ts").asLong();
			
			
			//start a new process
			if("session_start".equals(jn.get("status").asText())){
				closeFFMPEG();
				try {
					startLogitech(timeStamp);
					startGenius(timeStamp);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if("end_session".equals(jn.get("status").asText())){
				closeFFMPEG();
			}
		}
	}
	
	private void startLogitech(long timeStamp) throws IOException {
		String[] logitechParams = makeFFMPEGParams(Camera.LOGITECH, "logitech_cam_"+timeStamp);
		logger.info("Starting logitech video recording for timestamp {}: {}", timeStamp, logitechParams);
		ProcessBuilder logitechPB = new ProcessBuilder(logitechParams);
		logitechPB.redirectError(new File("logitecherror.txt"));
		//pb.redirectInput(new File("input.txt"));
		logitechPB.redirectOutput(new File("logitechoutput.txt"));
		
		logitechVideoProcess = logitechPB.start();
		logitechOSWriter = new PrintWriter(logitechVideoProcess.getOutputStream());
	}
	
	private void startGenius(long timeStamp) throws IOException {
		String[] geniusParams = makeFFMPEGParams(Camera.GENIUS, "genius_cam_"+timeStamp);
		logger.info("Starting genius video recording for timestamp {}: {}", timeStamp, geniusParams);
		ProcessBuilder geniusPB = new ProcessBuilder(geniusParams);
		geniusPB.redirectError(new File("geniuserror.txt"));
		//pb.redirectInput(new File("input.txt"));
		geniusPB.redirectOutput(new File("geniusoutput.txt"));
		
		geniusVideoProcess = geniusPB.start();
		geniusOSWriter = new PrintWriter(geniusVideoProcess.getOutputStream());
		
	}
	
	public static void main(String[] args) throws InterruptedException {
		String help = "Expecting commandline arguments in the form of \"-<argname> <arg>\".\nAccepting the following argnames: middlewareprops";

		String mwPropFile = "P4/config/defaultmiddleware.properties";
		
	    if(args.length % 2 != 0){
	    	System.err.println(help);
	    	System.exit(1);
	    }
	    
	    for(int i = 0; i < args.length; i = i + 2){
	    	if(args[i].equals("-middlewareprops")){
	    		mwPropFile = args[i+1];
	    	} else {
	        	System.err.println("Unknown commandline argument: \""+args[i]+" "+args[i+1]+"\".\n"+help);
	        	System.exit(1);
	    	}
	    }

		GenericMiddlewareLoader.setGlobalPropertiesFile(mwPropFile);
	    
		try {
			VideoLogger videoLogger = new VideoLogger();
			videoLogger.init();
		} catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		
	}

	/**
	 * Constructs a param string array for FFMPEG with all the correct settings (for windows only, using the Genius webcam)
	 * Assumes you have added the FFMPEG executable to your PATH
	 * 
	 * based on following commandline: 
	 * ffmpeg -f dshow -video_size 1280x720 -rtbufsize 10240k -framerate 30 -vcodec mjpeg -i video="USB_Camera":audio="Microphone (USB2.0 MIC)" -vcodec libx264 -filter:v fps=30 -crf 26 -preset veryfast [filename].mp4
	 * where [filename] is "id_yyyy-MM-dd_HH:mm:ss.SSS"
	 * @param id a prefix id for the filename
	 * @return a string array containing all params
	 */
	private String[] makeFFMPEGParams(Camera cam, String id){
		// ffmpeg -f dshow -video_size 1280x720 -rtbufsize 10240k -framerate 30 -vcodec mjpeg -i video="USB_Camera":audio="Microphone (USB2.0 MIC)" -vcodec libx264 -filter:v fps=30 -crf 26 -preset veryfast D:\videos\out720.mp4
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
		long curTime = System.currentTimeMillis();
	    String strDate = sdfDate.format(new Date(curTime));
	    
	    //ffmpeg -list_devices true -f dshow -i dummy

	    /*return new String[] {"cmd", "/c",
				"ffmpeg -f dshow -video_size 1280x720 -rtbufsize 10240k -framerate 30 -vcodec mjpeg -i video=\"USB_Camera\":audio=\"Microphone (USB2.0 MIC)\" -vcodec libx264 -filter:v fps=30 -crf 26 -preset veryfast " + VIDEO_DIR + id + "_" + strDate + "_" + curTime + ".mp4", 
				};*/

	    if(cam == Camera.GENIUS) {
		    return new String[] {
					"ffmpeg", "-f", 			
					"dshow", 					//use directshow to select webcam and mic
					"-video_size", "1920x1080", 	//select a supported resolution from webcam
					//"-rtbufsize", "10240k", 	//select a buffer size (this should be enough for about 5 seconds)
					"-framerate", "30",			//select a supported framerate from the webcam
					"-vcodec", "mjpeg", 		//select the video encoding we want to receive from the webcam
					"-i", "video=USB_Camera:audio=Microphone (USB2.0 MIC)", //Genius F100 wide angle (lower quality)
					"-vcodec", "libx264", 		//select the output encoding
					"-pix_fmt","yuvj422p",
					"-f", "matroska",			//select mkv container
					"-filter:v", "fps=30", 		//set the output framerate
					"-crf", "26", 				//set the quality fo the output video stream (26 seems ok, and not too large filesize)
					"-preset", "veryfast", 		//set the tradeoff between CPU power and filesize (veryfast seems to take around 10% CPU, filesize seems around 1GB/hour)
					VIDEO_DIR + id + "_" + strDate + "_" + curTime + ".mkv" 
					};
	    } else {
		    return new String[] {
					"ffmpeg", "-f", 			
					"dshow", 					//use directshow to select webcam and mic
					"-video_size", "1920x1080", 	//select a supported resolution from webcam
					//"-rtbufsize", "10240k", 	//select a buffer size (this should be enough for about 5 seconds)
					"-framerate", "30",			//select a supported framerate from the webcam
					"-vcodec", "mjpeg", 		//select the video encoding we want to receive from the webcam
					"-i", "video=HD Pro Webcam C920:audio=Microphone (HD Pro Webcam C920)", //logitech C920 narrow angle (better quality)
					"-vcodec", "libx264", 		//select the output encoding
					"-pix_fmt","yuvj422p",
					"-f", "matroska",			//select mkv container
					"-filter:v", "fps=30", 		//set the output framerate
					"-crf", "26", 				//set the quality fo the output video stream (26 seems ok, and not too large filesize)
					"-preset", "veryfast", 		//set the tradeoff between CPU power and filesize (veryfast seems to take around 10% CPU, filesize seems around 1GB/hour)
					VIDEO_DIR + id + "_" + strDate + "_" + curTime + ".mkv" 
					};
	    }
/*
	    return new String[] {
				"ffmpeg","-list_devices","true","-f","dshow","-i","dummy", 	
				};
				*/
	}
	
}
