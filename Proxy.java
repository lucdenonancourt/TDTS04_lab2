package proxy;

import java.io.IOException;
import java.net.*;

public class Proxy {

	/**
	 * The main function will consist of a infinite loop that will wait for a connection
	 * and procede to forward it to the distant server or not depending on the content of the webpage
	 */
	public static void main(String[] args) {

		//Default port number
		int portnumber = 2000;
		if(args.length >= 1){
			portnumber = Integer.parseInt(args[0]);
		}
		try {
			//We create a socket on the port to listen from the browser
			ServerSocket socket = new ServerSocket(portnumber);
		while(true){
				Socket connectionSocket = socket.accept();
				//Here we use our own java object (which is a Thread)
				WebProxy web = new WebProxy(connectionSocket);
				new Thread(web).start();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
