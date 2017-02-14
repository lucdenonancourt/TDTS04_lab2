package proxy;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebProxy implements Runnable{

	private Socket connectionSocket;

	//This is the list of inappropriate Web content
	private static ArrayList<String> inappropriate = new ArrayList<String>(Arrays.asList(
			"spongebob",
			"britney spears",
			"britney%20spears",
			"paris hilton",
			"paris%20hilton",
			"norrköping"
			));


	/**
	 * The constructor of the class will give us the socket that we will need to use to get 
	 * the information
	 * 
	 * @param serverSocket Socket on which the server will send information
	 */
	public WebProxy(Socket serverSocket){
		connectionSocket = serverSocket;
	}

	@Override
	public void run() {

		try {
			//First we setup the stream between the browser and the proxy to take care of inappropriate URL
			InputStream streamFromBrowser = connectionSocket.getInputStream();
			OutputStream streamToBrowser = connectionSocket.getOutputStream();

			//Then we get the header from the connection socket
			String header = getHeader(streamFromBrowser);
			
			System.out.println(header);
			
			//We now try to see if there is an inappropriate word in the header
			for(String word : inappropriate){
				if((header.toLowerCase()).contains(word)){
					System.out.println("Inappropiate word find in the header (URL)");
					//We redirect the connection to the LIU page
					streamToBrowser.write("HTTP/1.1 301 Moved Permanently\r\nLocation: http://www.ida.liu.se/~TDTS04/labs/2011/ass2/error1.html\r\n\r\n".getBytes());
					streamToBrowser.flush();
					streamFromBrowser.close();
					streamToBrowser.close();
					connectionSocket.close();
					return;
				}
			}

			//We create a socket on the port 80 to access the website (we need the name of the host)
			String hostName = findHost(header);
			Socket remoteSocket = new Socket(hostName, 80);
			
			//We first setup the stream between the proxy and the website
			InputStream streamFromHost = remoteSocket.getInputStream();
			PrintWriter streamToHost = new PrintWriter(remoteSocket.getOutputStream(),true);
			
			//We then send the header from the browser to the host
			streamToHost.write(header);
			streamToHost.flush();
			
			//We get all the data bytes from the remote host
			int bytesRead;
			byte[] data = new byte[1024];
			ArrayList<byte[]> dataList = new ArrayList<byte[]>();
			String dataListString = "";
			Boolean containText = false;
			Boolean headerFound = false;
			while((bytesRead = streamFromHost.read(data)) != -1){
				//We check here if its text or not (if it is not, we just forward it)
				while(!headerFound){
					if(byteToString(data).contains("\r\n\r\n")){
						String serverHeader = byteToString(data);
						serverHeader = serverHeader.substring(0, serverHeader.indexOf("\r\n\r\n"));
						headerFound = true;
						if(serverHeader.contains(": text/")){
							containText = true;
						}
					}
				}
				if(containText){
					dataList.add(Arrays.copyOfRange(data, 0, bytesRead));
					dataListString += (byteToString(Arrays.copyOfRange(data, 0, bytesRead)));
				}
				else{
					streamToBrowser.write(data, 0, bytesRead);
                    streamToBrowser.flush();
				}
			}
			dataListString = dataListString.toLowerCase();
			//We then check for any inappropriate word
			for(String word : inappropriate){
				if(dataListString.contains(word)){
					System.out.println("Inappropiate word find in the content");
					//We redirect the connection to the LIU page
					streamToBrowser.write("HTTP/1.1 301 Moved Permanently\r\nLocation: http://www.ida.liu.se/~TDTS04/labs/2011/ass2/error2.html\r\n\r\n".getBytes());
					streamToBrowser.flush();
					streamFromBrowser.close();
					streamToBrowser.close();
					connectionSocket.close();
					return;
				}
			}
			
			//We forward if no problem 
			for(byte[] bytes : dataList){
				streamToBrowser.write(bytes);
	            streamToBrowser.flush();
			}
			
			//Done. Close streams and socket.
			streamFromHost.close();
			streamToBrowser.close();
			streamToHost.close();
			streamFromBrowser.close();
			remoteSocket.close();
			connectionSocket.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * This methode will tell us if the header contain a inappropriate word
	 * @param stream The stream (from the browser)
	 * @return return the header of the request
	 */
	public String getHeader(InputStream stream){
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		String header = "";
		String line;
		//We go through the reader line by line and return the header
		try {
			while(((line = reader.readLine()) != null) && (!line.equals(""))){
				 if(!line.contains("Connection")) {
					 	if(line.contains(".com/ ")){
					 		header += "GET / HTTP/1.1\r\n";
					 	}else{
						 header += line + "\r\n";
					 	}
				 }
			}
			header += "Connection: close\r\n";
			header += "\r\n";
		} catch (IOException e) {
			e.printStackTrace();
		}
		return header;

	}

	public String findHost(String header){
		//The host is the string between "Host:" and the \r\n 
		Pattern p = Pattern.compile("Host:\\s(.*?)\r", Pattern.MULTILINE);
		Matcher matcher = p.matcher(header);
		matcher.find();
		return matcher.group(1);
	}

	/**
	 * This is a simple method to turn bytes into string (used to verify the content)
	 * @param byteArray The bytes array that we need to transform into a string
	 * @return the string
	 */
	private static String byteToString(byte[] byteArray) {
		try {
			return new String(byteArray, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
