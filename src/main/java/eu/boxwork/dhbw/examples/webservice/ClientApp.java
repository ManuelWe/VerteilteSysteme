package eu.boxwork.dhbw.examples.webservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;


public class ClientApp {

	WebResource service = null;	
	// we set the base URL manually
	public static final String BASE = "example";
	
	public void initialise(String baseURL)
	{
		service = Client.create().resource(
				baseURL );
	}
	
	public String getTextResponse()
	{
		// first we request the text interface
		Builder b = service.path("hello").accept(MediaType.TEXT_PLAIN);		
		String  response = b.get(String.class);		
		return response;		
	}
	
	public String getJsonResponseAsText(String name)
	{
		// now we request a json but print it as String
		Builder b = service.path("hello/"+name).accept(MediaType.APPLICATION_JSON);
		String response = b.get(String.class);		
		return response;		
	}
	
	public HelloToSend getJsonResponseAsJsonObject(String name)
	{
		// now we request a json but print it as String
		Builder b = service.path("hello/"+name).accept(MediaType.APPLICATION_JSON);
		HelloToSend responseObject = b.get(HelloToSend.class);			
		return responseObject;		
	}
	
	public HelloToSend getJsonResponseAsJsonObjectManuallyParsed(String name)
	{
		// now we request a json but print it as String
		Builder b = service.path("hello/"+name).accept(MediaType.APPLICATION_JSON);
		String response = b.get(String.class);
		System.out.println("JSON RESPONSE as Text: "+response);
		HelloToSend parsed = parseJSONManually(response);			
		return parsed;		
	}
	
	public int getErrorCodeWrongDataType()
	{
		ClientResponse responseError = service.path("hello").
				accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		// the request is not supported due to invalid content type in header, error should be 406
		return responseError.getStatus();
	}
	
	public void fireRequests()
	{
		System.out.println("Text RESPONSE: "+getTextResponse());
	
		System.out.println("JSON RESPONSE as Text: "+getJsonResponseAsText("A-NAME"));
		
		// now we request a json and parse it implicitely as an object
		System.out.println("JSON RESPONSE as Object: "+getJsonResponseAsJsonObject("A-NAME"));
		
		// now we request again, but parse it manually to an object
		
		System.out.println("JSON RESPONSE as parsed object: "+getJsonResponseAsJsonObjectManuallyParsed("A-NAME"));
		
		// last but not least, we create an error
		try {
			// the request is not supported due to invalid content type in header, error should be 406
			int responseError =  getErrorCodeWrongDataType();
			if (getErrorCodeWrongDataType() != 200)
					System.out.println("ERROR code is: "+responseError);
			else
				System.err.println("no ERROR as exspected.");
		} catch (Exception e) {
			System.err.println(e.getLocalizedMessage());
		}
	}
	
	/**
	 * method to parse a json manually
	 * @param response String to parse
	 * @return {@link HelloToSend} parsed object
	 * */
	private HelloToSend parseJSONManually(String response) {
		HelloToSend ret = new HelloToSend();
		try {
			JsonParser parser = new JsonFactory().createJsonParser(response);
		
			while(parser.nextToken() != JsonToken.END_OBJECT)
			{
				if ("name".equals(parser.getCurrentName()))
				{
					String myName = "";
					parser.nextToken();
					myName = parser.getText();
					ret.setName(myName);
				}
				if ("listOfStates".equals(parser.getCurrentName()))
				{
					parser.nextToken();
					List<String> elements = new ArrayList<>();
					while (parser.nextToken() != JsonToken.END_ARRAY)
					{
						String stringElement = parser.getText();
						elements.add(stringElement);
					}
					ret.setListOfStates(elements.toArray(new String[elements.size()]));
				}
			}
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return ret;
	}

	/**
	 * starts the client to connect to the webservice, IP and port has to be set as parameter
	 * like java -jar serviceclientexample.jar localhost 8080
	 * @param args: 0 = IP, 1 = port
	 * */
	public static void main(String[] args) {
		String ip = "127.0.0.1";
		String port = "8080";
		String serverString = "http://"+ip+":"+port+"/";
		String baseURLRoot = serverString+BASE;
		
		ClientApp app = new ClientApp();
		
		app.initialise(baseURLRoot);
		app.fireRequests();
	}
}
