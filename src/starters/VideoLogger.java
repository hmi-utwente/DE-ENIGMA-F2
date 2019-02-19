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

	//possible alternative: https://github.com/bytedeco/javacv/blob/master/samples/WebcamAndMicrophoneCapture.java
	
	private static final String VIDEO_DIR = "C:\\videos\\";
	
	private Process videoProcess;

	private JSONHelper jh;

	private ObjectMapper om;

	private PrintWriter osWriter;
	
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
		ObjectNode action1 = om.createObjectNode();
		action1.put("id", "testid1");
		action1.put("action", "START");
		
		starter1.set("command", action1);
		
		this.receiveData(starter1);
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//stop it again
        ObjectNode stopper1 = om.createObjectNode();
		ObjectNode action2 = om.createObjectNode();
		action2.put("action", "STOP");
		
		stopper1.set("command", action2);
		
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
		if(videoProcess != null && videoProcess.isAlive()){
			logger.info("Stopping current video recording");
			osWriter.println("q");
			osWriter.flush();
			
			//give it a moment to gracefully shut down
			try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			if(videoProcess != null && videoProcess.isAlive()){
				//finally destroy
				videoProcess.destroy();
				
				//and wait for it to actually close
				try {
					videoProcess.waitFor();
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
			boolean start = "session_start".equals(jn.get("status").asText());
			long timeStamp = jn.get("ts").asLong();
			
			//command action is either start or stop.. either way, we need to stop the current (potential) running video recording
			closeFFMPEG();
			
			//start a new process
			if(start){
				try {
					String[] params = makeFFMPEGParams("overview_cam_"+timeStamp);
					logger.info("Starting video recording for timestamp {}: {}", timeStamp, params);
					ProcessBuilder pb = new ProcessBuilder(params);
					pb.redirectError(new File("error.txt"));
					//pb.redirectInput(new File("input.txt"));
					pb.redirectOutput(new File("output.txt"));
					
					videoProcess = pb.start();
					osWriter = new PrintWriter(videoProcess.getOutputStream());
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				logger.info("Not starting video.. start: {}", start);
			}
		}
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
	private String[] makeFFMPEGParams(String id){
		// ffmpeg -f dshow -video_size 1280x720 -rtbufsize 10240k -framerate 30 -vcodec mjpeg -i video="USB_Camera":audio="Microphone (USB2.0 MIC)" -vcodec libx264 -filter:v fps=30 -crf 26 -preset veryfast D:\videos\out720.mp4
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
		long curTime = System.currentTimeMillis();
	    String strDate = sdfDate.format(new Date(curTime));
	    
	    //ffmpeg -list_devices true -f dshow -i dummy

	    /*return new String[] {"cmd", "/c",
				"ffmpeg -f dshow -video_size 1280x720 -rtbufsize 10240k -framerate 30 -vcodec mjpeg -i video=\"USB_Camera\":audio=\"Microphone (USB2.0 MIC)\" -vcodec libx264 -filter:v fps=30 -crf 26 -preset veryfast " + VIDEO_DIR + id + "_" + strDate + "_" + curTime + ".mp4", 
				};*/

	    
	    return new String[] {
				"ffmpeg", "-f", 			
				"dshow", 					//use directshow to select webcam and mic
				"-video_size", "1920x1080", 	//select a supported resolution from webcam
				//"-rtbufsize", "10240k", 	//select a buffer size (this should be enough for about 5 seconds)
				"-framerate", "30",			//select a supported framerate from the webcam
				"-vcodec", "mjpeg", 		//select the video encoding we want to receive from the webcam
				"-i", "video=USB_Camera:audio=Microphone (USB2.0 MIC)", //which sources should we use for video and audio
				"-vcodec", "libx264", 		//select the output encoding
				"-pix_fmt","yuvj422p",
				"-f", "matroska",			//select mkv container
				"-filter:v", "fps=30", 		//set the output framerate
				"-crf", "26", 				//set the quality fo the output video stream (26 seems ok, and not too large filesize)
				"-preset", "veryfast", 		//set the tradeoff between CPU power and filesize (veryfast seems to take around 10% CPU, filesize seems around 1GB/hour)
				VIDEO_DIR + id + "_" + strDate + "_" + curTime + ".mkv" 
				};
/*
	    return new String[] {
				"ffmpeg","-list_devices","true","-f","dshow","-i","dummy", 	
				};
				*/
	}
	
}
