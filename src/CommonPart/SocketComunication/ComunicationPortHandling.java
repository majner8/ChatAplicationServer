package CommonPart.SocketComunication;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;

import CommonPart.SocketComunication.SocketComunication.SocketComunicationException;
import CommonPart.ThreadManagement.ThreadPoolingManagement;
import Main.Main;

public final class ComunicationPortHandling {
	
    private static Random random = new Random();
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static final String EndMessageCharacter="\\|&\\&";
	public static String generateUUID(int lengt) {
	        StringBuilder builder = new StringBuilder();
	        for (int i = 0; i < lengt; i++) {
	            int index = random.nextInt(ALPHABET.length());
	            char randomChar = ALPHABET.charAt(index);
	            builder.append(randomChar);
	        }
	        return builder.toString();
	    
	}
	private ComunicationPortHandlingInterface comunication;
	public static final String AutorizationKey = "dasrrweijewieqwožš4č";
	
	public static final String DeviceUUIDCharacter="&sd";

	//this character is for notification, when a server finish autorization proces
	//client send this notification, then reader will be interupt and connection would be change
	//
	private static final int SoTimeoutOFSocket = 180 * 1000;



	



	private Socket socket;

	private ReadSocket ReaderHandler;
	private WriteMessage WriteHandler;

	
	private String UserUUID;
	private String UUIDDevice;

	private boolean UnConnected=false;//true when a connection is closed
	
	private boolean TemporaryClosed=false;
	
	


	public ComunicationPortHandling(boolean connectionAutorize,ComunicationPortHandling.ComunicationPortHandlingInterface comuInterface,BufferedReader read, BufferedWriter write, Socket socket) {
		this.comunication=comuInterface;
		this.socket=socket;
		this.WriteHandler=new WriteMessage(write);
		this.ReaderHandler=new ReadSocket(connectionAutorize,read);
		try {
			this.socket.setSoTimeout(this.SoTimeoutOFSocket);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			ProblemWithConnection(e);
			
		}
	}
	/**Metod start, or stop file writting, depends on parametr */
	public void StartStopFileWriting(boolean start) {
		this.WriteHandler.WriteIntoFile(start);
	}
	
	private void CloseTemporary() {
		System.out.println("I am closing");
		System.out.println(LocalDateTime.now());
		
		this.TemporaryClosed=true;
		try {
			this.socket.close();
			this.WriteHandler.close();
			this.ReaderHandler.read.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
		this.comunication.ConnectionIsEnd();
	}
	
	private void ProblemWithConnection(Exception e) {
		this.UnConnected=true;
		e.printStackTrace();
		this.WriteHandler.WriteIntoFile(true);
		try {
			this.socket.close();
			this.WriteHandler.writer.close();
			this.ReaderHandler.read.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		this.CloseTemporary();
	}

	/** metod change appropriate comunication interface
	 * metod manage this in synchronized block
	 */
	public void ChangeComunicationInterface(ComunicationPortHandlingInterface inte) {
		synchronized(this.comunication) {
			this.comunication=inte;
		}
	}
	
	/** metod return saved Interface
	 * 	metod manage this interface, in synchronized process, protection during
	 * change class
	 * @return
	 */
	private ComunicationPortHandlingInterface getComunicationInterface() {
		synchronized(this.comunication) {
			return this.comunication;
		}
	}

	/**
	 * Metod put String into bottom of queue
	 * 
	 * @param sendInPrior-true if you want to send message in prior, or during
	 *                         synchronization, message which is set as false,would
	 *                         not be send
	 * @throws ConnectionIsTemporaryClosed 
	 */
	public void writeMessage(String messageToWrite, boolean sendInPrior)  {
		if(this.TemporaryClosed) {
			return;
		}
		//handle later
		//task is not saved into file
		this.WriteHandler.SendMessage(messageToWrite, true);
		
		
	}
	
	public void AllowFileWrite() {
		this.WriteHandler.SetFileWriting(UserUUID, DeviceUUIDCharacter);
	}

	private final class ReadSocket extends Thread {

		private BufferedReader read;
		public ReadSocket(boolean connectionAutorized,BufferedReader read) {
			this.read=read;
			this.connectionAutorized=connectionAutorized;
			this.start();

		}
		private byte SocketTimeOutTryAmount=0;
		private static byte MaximumSocketTimeOutTryAmout=3;
		private boolean connectionAutorized = false;
		private byte SecurityTry=0;
		private static byte MaximumSecuryTry=3;
		private byte LengSecurityTry=1;
		private int maximumCharacterOfOneMessage=8000;
		@Override
		public void run() {
			while(!Main.isServerStopped()&&!TemporaryClosed&&!UnConnected) {
				StringBuilder sb = new StringBuilder();

				try {
					int c;
					
					while ((c = read.read()) != -1) {
						//System.out.print((char)c);
					    sb.append((char) c);
					    if(sb.length()>this.maximumCharacterOfOneMessage*this.LengSecurityTry) {
					    	this.LengSecurityTry++;
							continue;
					    }
					    if(this.LengSecurityTry>this.MaximumSecuryTry) {
					    	System.out.println("closeA");
							CloseTemporary();
							return;
					    }
					    
					    //chech if end by endMessageCharacter
					    if(sb.length()>=ComunicationPortHandling.EndMessageCharacter.length()) {
						    String lastMessage=sb.substring(sb.length()-ComunicationPortHandling.EndMessageCharacter.length());
						    if(lastMessage.equals(ComunicationPortHandling.EndMessageCharacter)) {
						    	//end
						    	break;
						    }
					    }
					}
					if(c==-1) {
				    	System.out.println("closeB");

						CloseTemporary();
						return;
					}
					this.LengSecurityTry=1;
					
				//	readMessage[0]=read.readLine();
					
					/*
					if(readMessage[0]==null) {
						this.SecurityTry++;
						if(this.SecurityTry>=this.MaximumSecuryTry*3) {
							CloseTemporary();
						}
						continue;
					}
					if(readMessage[0].equals("null")) {
						this.SecurityTry++;
						if(this.SecurityTry>=this.MaximumSecuryTry*3) {
							CloseTemporary();
						}
						continue;
					}
					*/
				}
				catch(SocketTimeoutException e) {
					SocketTimeOutTryAmount++;
					if(this.SocketTimeOutTryAmount<=this.MaximumSocketTimeOutTryAmout) {
						continue;
					}
					ProblemWithConnection(e);
					continue;
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					ProblemWithConnection(e);
					continue;
				}

				final String curentMessage = sb.toString().substring(0,sb.length()-ComunicationPortHandling.EndMessageCharacter.length());
				
				sb.setLength(0);
				if(!this.connectionAutorized) {
					if (!curentMessage.equals(AutorizationKey)) {
						this.SecurityTry++;
						if(this.SecurityTry>=this.MaximumSecuryTry) {
					    	System.out.println("closeC");

							CloseTemporary();
						}
						continue;
					}
					this.SecurityTry=0;
					this.connectionAutorized=true;
				}
			
				
				ThreadPoolingManagement.thread.ProcessReceivedMessage(() -> {
					try {
						SocketComunication message=new SocketComunication(curentMessage, getInetAdress());
						getComunicationInterface().ProcessMessage(message);
					} catch (SocketComunicationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
				
			}
		}

	}

	public final class WriteMessage extends Thread {
		private BufferedWriter writer;
		private static String newMessagePatern = "&&€|@";

		private File file;
		private FileWriter FileWriter;
		private BufferedReader FileReader;
		private static String path = "SQL\\DeviceConnection\\";

		private boolean WriteMessageIntoFile = false;
		private LinkedBlockingDeque<String> MessateToWrite = new LinkedBlockingDeque<String>();
		
		private boolean FileWritingAllow=false;
		public WriteMessage(BufferedWriter write) {
			this.writer=write;
			
			super.start();

		}

		private void close() {
			try {
				this.writer.close();
			} catch (IOException |NullPointerException e) {
				// TODO Auto-generated catch block
			}
			try {
				
				this.FileWriter.close();
			} catch (IOException |NullPointerException e) {
				// TODO Auto-generated catch block
			}
			try {
				this.FileReader.close();
			} catch (IOException |NullPointerException e) {
				// TODO Auto-generated catch block
			}
			this.MessateToWrite.clear();
		}
		
		/** metod allow fileWriting*/
		public synchronized void SetFileWriting(String UserUUID,String deviceUUID) {
			if(this.FileWritingAllow) {
				throw new RuntimeException("FileWriting was already allow");
			}
			this.FileWritingAllow=true;
			try {
				this.file = new File(this.path + UserUUID + UUIDDevice + ".txt");
				if (this.file.exists()) {
					this.file.delete();
				}
				
				this.file.createNewFile();
				this.FileWriter = new FileWriter(this.file, true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Main.stopServer(null);
			}
		}
		
		/**
		 * Metod put message to send
		 * 
		 * @param PrioritySend true-if you want to send in prior also, true if you want
		 *                     to send message user, even if saving message into
		 *                     temporary file is active if a connection is broken,
		 *                     message will be send to file even if PrioritySend is true
		 */
		public void SendMessage(String message, boolean PrioritySend) {
			if (PrioritySend) {
				if (!this.WriteMessageIntoFile || UnConnected) {
					this.MessateToWrite.addFirst(message);
				} else {
					try {
						this.SendMessageToDevice(message);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						ProblemWithConnection(e);
						this.MessateToWrite.addFirst(message);
					}
				}
			} else {
				this.MessateToWrite.addLast(message);
			}
		}

		private synchronized void SendMessageToDevice(String message) throws IOException {
			this.writer.write(message);
			this.writer.write(ComunicationPortHandling.EndMessageCharacter);
			
			this.writer.flush();;


		}

		private boolean ReSending = false;// when a value is true, it mean
		// that message from file have to be send

		/** Metod end or start writing into file */
		public synchronized void WriteIntoFile(boolean start) {
			this.ReSending = !start;
			this.WriteMessageIntoFile = start;
		}



		public synchronized void WriteMessageIntoFile(String message) {
			if(!this.FileWritingAllow) {
		    	System.out.println("closeDD");

				CloseTemporary();
				return;
			}
			try {
				this.writer.append("\n");
				this.writer.append(message);
				this.writer.append("\n");
				this.writer.append(this.newMessagePatern);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Main.stopServer(null);
			}

		}

		private int currentReadMessage = 0;

		private synchronized void ReadMessageFromFileAndSend() {
			StringBuilder readMessage = new StringBuilder();

			String message = null;
			try {
				while (!(message = this.FileReader.readLine()).trim().equals(this.newMessagePatern)
						&& message != null) {
					readMessage.append(message);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Main.stopServer(null);
			}
			if (message == null) {
				// reading finish
				this.ReSending = false;
				this.currentReadMessage = 0;
				return;
			}
			try {
				this.SendMessageToDevice(readMessage.toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				this.ReSending = false;
				this.WriteMessageIntoFile = true;

			}
			this.currentReadMessage = this.currentReadMessage + 1;

		}

		@Override
		public void run() {
			while (!Main.isServerStopped() && !TemporaryClosed) {
				// chech if reSending is not avaiable
				if (this.ReSending&&this.FileWritingAllow) {
					this.ReadMessageFromFileAndSend();
					try {
						this.sleep(1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
					}
					continue;
				}
				String message = null;
				try {
					message = this.MessateToWrite.takeFirst();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
				}
				if (message == null) {
					continue;
				}
				if ((this.WriteMessageIntoFile || UnConnected)&&this.FileWritingAllow) {
					this.WriteMessageIntoFile(message);
				} 
				else {
					try {
						this.SendMessageToDevice(message);
					} catch (IOException e) {
						// put unsend message back
						this.MessateToWrite.addFirst(message);
						// TODO Auto-generated catch block
						ProblemWithConnection(e);
					}

				}
				try {
					this.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
				}

			}
			if(this.file!=null) {
			this.file.delete();
			}
		}

	}

	
	
	
	
	
	



	
	///////
	
	public static interface ComunicationPortHandlingInterface{
		public  void ConnectionIsEnd();

		public  void ProcessMessage(SocketComunication message);
	
	}
	
	public String getTemporaryTableUUID() {
		return this.getUserUUID() + this.getUUIDDevice();
	}

	public InetAddress getInetAdress() {
		return this.socket.getInetAddress();
	}

	public String getUserUUID() {
		return UserUUID;
	}

	public void setUserUUID(String userUUID) {
		UserUUID = userUUID;
	}

	public String getUUIDDevice() {
		return UUIDDevice;
	}

	public void setUUIDDevice(String uUIDDevice) {
		UUIDDevice = uUIDDevice;
	}

	public static class ConnectionIsTemporaryClosed extends Exception{
		
	}
	
}
