/**
 * 
 */
package eu.boxwor.dhbw.examples.test.webservice;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import client.WebClient;
import eu.boxwork.dhbw.examples.webservice.Webserver;

/**
 * @author Patrick Jungk
 *
 */
public class ClientAppTest {

	public static final String ip = "localhost";
	public static final String port = "5000";
	
	WebClient app = new WebClient();
	Webserver server = new Webserver();
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		server.startServer(ip, port);
		
		String serverString = "http://"+ip+":"+port+"/";
		String baseURLRoot = serverString+WebClient.BASE;
				
		app.initialise(baseURLRoot);
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		server.stopServer();
	}

	/**
	 * Test method for {@link client.WebClient#getTextResponse()}.
	 */
	@Test
	public void testGetTextResponse() {
		assertEquals("hello!!!",app.getTextResponse());
	}

	/**
	 * Test method for {@link client.WebClient#getJsonResponseAsText(java.lang.String)}.
	 */
	@Test
	public void testGetJsonResponseAsText() {
		assertEquals("{\"listOfStates\":[\"Baden\",\"Württemberg\"],\"name\":\"A-NAME\"}",app.getJsonResponseAsText("A-NAME"));
	}

	/**
	 * Test method for {@link client.WebClient#getJsonResponseAsJsonObject(java.lang.String)}.
	 */
	@Test
	public void testGetJsonResponseAsJsonObject() {
		assertEquals("hello A-NAME",app.getJsonResponseAsJsonObject("A-NAME").toString());
	}

	/**
	 * Test method for {@link client.WebClient#getJsonResponseAsJsonObjectManuallyParsed(java.lang.String)}.
	 */
	@Test
	public void testGetJsonResponseAsJsonObjectManuallyParsed() {
		assertEquals("hello A-NAME",app.getJsonResponseAsJsonObjectManuallyParsed("A-NAME").toString());
	}

	/**
	 * Test method for {@link client.WebClient#getErrorCodeWrongDataType()}.
	 */
	@Test
	public void testGetErrorCodeWrongDataType() {
		assertEquals(406,app.getErrorCodeWrongDataType());
	}

}
