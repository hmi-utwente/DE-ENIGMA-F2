package flipper;

import static nl.utwente.hmi.middleware.helpers.JsonNodeBuilders.object;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.utwente.hmi.middleware.Middleware;
import nl.utwente.hmi.middleware.MiddlewareListener;
import nl.utwente.hmi.middleware.helpers.JsonNodeBuilders.ObjectNodeBuilder;
import nl.utwente.hmi.middleware.loader.GenericMiddlewareLoader;


public class FlipperMiddleware implements MiddlewareListener {

	protected BlockingQueue<JsonNode> queue = null;
	private static Logger logger = LoggerFactory.getLogger(FlipperMiddleware.class.getName());
	protected Middleware middleware;
	protected ObjectMapper mapper;

	public FlipperMiddleware(String middlewareProps) {
		this.queue = new LinkedBlockingQueue<JsonNode>();
		Properties ps = new Properties();
        InputStream mwProps = FlipperMiddleware.class.getClassLoader().getResourceAsStream(middlewareProps);
		mapper = new ObjectMapper();
        
		try {
			ps.load(mwProps);
		} catch (IOException ex) {
            logger.warn("Could not load flipper middleware props file {}", mwProps);
            ex.printStackTrace();
        }
		
		GenericMiddlewareLoader.setGlobalPropertiesFile("P4/config/defaultmiddleware.properties");
		
        GenericMiddlewareLoader gml = new GenericMiddlewareLoader(ps.getProperty("middleware"), ps);
        middleware = gml.load();
        middleware.addListener(this);
	}
	
	public boolean init() {
		return true;
	}
	
	// { "content": "$data" }
	public void send(String data) {
        ObjectNodeBuilder on = object();
        try {
			on.with("content", URLEncoder.encode(data, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return;
		}
        middleware.sendData(on.end());
	}
	
	public void sendJSONString(String json) {
		//parse json string and create JsonObject
		try {
			JsonNode jn = mapper.readTree(json);
			logger.debug("Transformed to json object: {}", jn.toString());
			middleware.sendData(jn);
		} catch (JsonProcessingException e) {
			logger.warn("Error while parsing JSON string \"{}\": {}", json, e.getMessage());
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean hasMessage() {
		return isConnected() && !queue.isEmpty();
	}
	
	/**
	 * only call this when hasMessage() returned true,
	 * as it is otherwise blocking until it receives a message.
	 */
	public String getMessage() {
		try {
			JsonNode msg = queue.take();
			logger.debug("Processing message from middleware: {}", msg);
			return msg.toString();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return "{}";
	}

	@Override
	public void receiveData(JsonNode jn) {
		queue.clear();
		queue.add(jn);
	}
	
	public boolean isConnected() {
		return middleware != null;
	}
	
	public static void Log(String s) {
        logger.debug("\n===\n{}\n===", s);
	}

}