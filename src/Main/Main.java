package Main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import CommonPart.SQL.MainSQL.FileLoadException;
import SQL.SQLServer;
import Users.ConnectionAutorize;



public class Main {

	public static final int defaultPort = 3406;
	public static InetAddress IPAdres;
	private static boolean StopServer = false;

	public static boolean isServerStopped() {
		return Main.StopServer;
	}

	public static void stopServer(String message) {
		Main.StopServer = true;
		System.exit(0);
	}

	static {
		try {
			IPAdres = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// throw hlavni chyba a shodit server
		}
	}

	public Main() {
		try {
			new SQLServer();
		} catch (FileLoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			stopServer(null);
		}
		new ListenPort();
		new ThreadManagementServer();
	}
	private class ListenPort extends Thread {

		public ListenPort() {
			try {
				this.init();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// private SSLServerSocketFactory serverSocketFactory;
		private ServerSocket serverSocketFactory;
		// private SSLServerSocket serverSocket;

		public void init() throws IOException {
			// this.serverSocketFactory= (SSLServerSocketFactory)
			// SSLServerSocketFactory.getDefault();
			// this.serverSocket=(SSLServerSocket)
			// this.serverSocketFactory.createServerSocket(mainSQL.defaultPort);
			this.serverSocketFactory = new ServerSocket(Main.defaultPort);
			super.start();
		}

		public void run() {
			while (!Main.isServerStopped()) {
				try {
					System.out.println("I am listening port number " + Main.defaultPort);
					Socket socket = serverSocketFactory.accept();
					System.out.println("Accept Socket");
					BufferedReader rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
					
					new ConnectionAutorize(rd,wr,socket);
					// socket was accept
					// new AutorizateConnection(socket,rd,wr);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}	
	


public static void main(String[] args) {
new Main();
}

	
	
}

	
	
	
	

	




	
