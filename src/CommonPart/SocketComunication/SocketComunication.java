package CommonPart.SocketComunication;

import java.net.InetAddress;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;



public class SocketComunication {
	
	private final static String RegexEscapePatern="(?<!\\\\)\\";
	// SocketMessage look like as follow
	//UUID recipient separator
	//UUID task separator separator
	//FirstSocketMessage separator 
	//mainSQL Enum separator
	//first SimpleResultSet separator
	//second ResultSet separator- pokud second separator neni, musi byt hodnota null
	//enum ....
	
	private String UUIDTask;
	private String UUIDRecipient;

	
	//have to be init
	private TypeOfUUIDRecipient typeOfUUID;
	private String UUIDSender;

	
	private ArrayList<OneSocketMessage> messages=new ArrayList<OneSocketMessage>();
	private InetAddress IPAdress;

	private final static String RowCharacter="|r";
	private final static String ColumnCharacter="|c";
	private final static String Separator="|s";
	// Character represent OneSocketValue which contain only one simple value.
	private final static String NoSimpleResultSetSeparator="|n";
	
	
	
	
	
	private static String getRegexPatern(String str) {
		return  SocketComunication.RegexEscapePatern+str;
	}

	
	// SocketMessage look like as follow
	//UUID recipient separator
	//UUID task separator separator
	//mainSQL Enum separator
	//first SimpleResultSet separator
	//second ResultSet separator- pokud second separator neni, musi byt hodnota null
	//enum ....
	public SocketComunication(String Message,InetAddress adressOfSender) throws SocketComunicationException {try {
		String [] field=Message.split(getRegexPatern(SocketComunication.Separator));
			
		
			this.UUIDRecipient=field[0];
			this.UUIDTask=field[1];
			this.messages=OneSocketMessage.ConvertMessageToOneSocketMessages(field, 2);
			
		}
		catch(IndexOutOfBoundsException e) {
			new SocketComunicationException("Problem with Convertin SocketMessage, type MainException");
		}
	}
	
	public static SocketComunication createNewSocketComunication(String UUIDRecipient,String UUIDTask) {
		if(UUIDTask==null) {
			UUIDTask=SocketComunication.generateMessageUUID();
		}
		return new SocketComunication(UUIDTask,UUIDRecipient);
	}
	private SocketComunication(String UUIDTask,String UUIDRecipient) {
		this.UUIDTask=UUIDTask;
		this.UUIDRecipient=UUIDRecipient;
	}
	/**@return null if SocketComunication does not contain OneSocketMessage with specified index */
	public synchronized OneSocketMessage getMessage(int index) {
		if(index>=this.messages.size()) 
		{ return null;}
		return this.messages.get(index);
	}
	public void addOneSocketMessage(OneSocketMessage messageToADD) {
		this.messages.add(messageToADD);
	}
	
	public static class OneSocketMessage{
		// SocketMessage look like as follow
		//UUID recipient separator
		//UUID task separator separator
		//mainSQL Enum separator
		//first SimpleResultSet separator
		//second ResultSet separator- pokud second separator neni, musi byt hodnota null
		//enum ....
		private static final int NumberOfFieldGroup=3;//represent how many part have each
		// OneSocketMessage
		private SocketComunicationEnum MainEnum;
		private String oneValue;
		private SimpleResultSet simpleResultSet;
		/**Metod convert string message to OneSocketMessage
		 * Message have to be devided into group, by Separator
		 * @param devidedMessage -received devidedMessage  
		 * @param beginIndex -number of first OneSocketMessage value in devidedMessage parametr 
		 * @throws SocketComunicationException */
		public static ArrayList<OneSocketMessage> ConvertMessageToOneSocketMessages(String [] devidedMessage,int beginIndex) throws SocketComunicationException {
			//simple control size of field
			if((devidedMessage.length-2)%3!=0) {
				
				//From total length is deduced 2, because of patern, first-is UUID recipient
				//second is UUID task, than is starting OneSocketMessage
				throw new SocketComunicationException("Problem with converting mainSQL A",devidedMessage);
			}
			
			ArrayList<OneSocketMessage> returns=new ArrayList<OneSocketMessage>();
			
			//total part of oneSocketMessage is equal NumberOfFieldGroup
			for(int i=beginIndex;i<devidedMessage.length;i=NumberOfFieldGroup+i) {
				returns.add(new OneSocketMessage(devidedMessage,i));
			}
			return returns;
		}
		private OneSocketMessage(String [] DevideMessage,int BeginIndex) throws SocketComunicationException {
			try {
				this.MainEnum=SocketComunicationEnum.valueOf(DevideMessage[BeginIndex].trim());
				BeginIndex++;
				
					//firstSimpleResultSet/OneValue
					
					
					if(!DevideMessage[BeginIndex].contains(SocketComunication.NoSimpleResultSetSeparator)){
						this.oneValue=null;
						this.simpleResultSet=new SimpleResultSet(DevideMessage[BeginIndex],DevideMessage[BeginIndex+1].equals("null")?null:DevideMessage[BeginIndex+1]);
	
					}
					else {
						//one socketMessage contain simple value
						if(!DevideMessage[BeginIndex+1].equals("null")) {
							//throw exception
							throw new SocketComunicationException("Problem with ConvertingValue, Exception A",DevideMessage);

						}
						this.simpleResultSet=null;
						this.oneValue=DevideMessage[BeginIndex].replaceAll(SocketComunication.getRegexPatern(SocketComunication.NoSimpleResultSetSeparator), "");
					}
					
			}
			catch(IllegalArgumentException e) {
				throw new SocketComunicationException("Problem with ConvertingValue, Exception C",DevideMessage);
	
			}
			catch(IndexOutOfBoundsException e) {
				throw new SocketComunicationException("Problem with ConvertingValue, Exception D",DevideMessage);
	
			}
		}
		
		public SimpleResultSet getSimpleResultSet() {
			return this.simpleResultSet;
		}
		
		public void SetMessage(SimpleResultSet rs,SocketComunicationEnum mainEnum) {
			this.simpleResultSet=rs;
			this.oneValue=null;
			this.MainEnum=mainEnum;
		}
		@Override
		public String toString() {
			StringBuilder bd=new StringBuilder();
			bd.append(this.MainEnum);
			bd.append(SocketComunication.Separator);

			if(oneValue!=null) {
				bd.append(SocketComunication.NoSimpleResultSetSeparator);
				bd.append(this.oneValue);
				bd.append(SocketComunication.Separator);
				bd.append("null");
				return bd.toString();
			}

			bd.append(this.simpleResultSet);
			return bd.toString();
		}
		public SocketComunicationEnum getTypeOfMessage() {
			return this.MainEnum;
		}
		public String getOneValue() {
			return this.oneValue;
		}
		public OneSocketMessage(SocketComunicationEnum MainEnum,String oneValue) {
			this.MainEnum=MainEnum;
			this.oneValue=oneValue;
			this.simpleResultSet=null;
		}
		public OneSocketMessage(SocketComunicationEnum MainEnum,SimpleResultSet rs) {
			this.MainEnum=MainEnum;
			this.oneValue=null;
			this.simpleResultSet=rs;
		}
	}

	
public static class SimpleResultSet{
		
		private SimpleResultSetValue MainResultSet,ExtendResultSet;
		
		
		public  SimpleResultSet (ResultSet rs) throws SQLException {
			this.ExtendResultSet=null;
			
			
	        ResultSetMetaData rsmd=rs.getMetaData();
			ArrayList<String> columnName=new ArrayList<String>();
			for(int i=1;i<=rsmd.getColumnCount();i++) {
	        	columnName.add(rsmd.getColumnName(i));
	        }
	        this.MainResultSet=new SimpleResultSetValue(columnName);
	        while(rs.next()) {
	        	// now columnName is used as arraylist for rowValue
	        	columnName=new ArrayList<String>();
	        	//add each row value into list
	        	for(int i=1;i<=rsmd.getColumnCount();i++) {
	        		if(rs.getString(i)==null) {
			        	columnName.add("null");

	        			continue;
	        		}

		        	columnName.add(rs.getString(i));

		        }
	        	this.MainResultSet.addRow(columnName);
	        }
	        
		}
		public SimpleResultSet(List<String> list,List<String>ColumnNameExtend) {
			this.MainResultSet=new SimpleResultSetValue(list);
			if(ColumnNameExtend!=null) {
				this.ExtendResultSet=new SimpleResultSetValue(ColumnNameExtend);
			}
		}

		public  SimpleResultSet (String MainResultSet,String ExtendResultSet) {
			this.MainResultSet=new SimpleResultSetValue(MainResultSet);
			if(ExtendResultSet!=null) {
				this.ExtendResultSet=new SimpleResultSetValue(ExtendResultSet);
			}
		}
		/**Metod add new row to simpleResultSet,
		 * value have to be in same order as column name*/
		public void addValue(List<String> list,boolean isExtend) {
			SimpleResultSetValue SimpleValue;
			if(isExtend) {
				SimpleValue=this.ExtendResultSet;
			}
			else {
				SimpleValue=this.MainResultSet;
			}
			SimpleValue.addRow(list);
		}
		
		@Override
		public String toString() {
			StringBuilder br=new StringBuilder();
			br.append(this.MainResultSet.toString());
			br.append(SocketComunication.Separator);
			if(this.ExtendResultSet!=null) {
				br.append(this.ExtendResultSet.toString());
			}
			else {
				br.append("null");
			}
			return br.toString();
		}

		/**@return null if a index is outOfBounds */
		public String getValue(String columnName,boolean isExtend,int index) {
			SimpleResultSetValue x;
			if(isExtend) {
				x=this.ExtendResultSet;
			}
			else {
				x=this.MainResultSet;
			}
			return x.getValue(index, columnName);
		}
		/**Metod return appropriate row, or null if a index is outOf bounds */
		public List<String> getRowValue(boolean isExtend, int index){
			if(isExtend) {
				return this.ExtendResultSet.getRowValue(index);
			}
			return this.MainResultSet.getRowValue(index);
		};
		
		public int lenght(boolean extend) {
			if(extend) {
				return this.ExtendResultSet.lenght();
			}
			return this.MainResultSet.lenght();
		}
		/**Metod add new column to SimpleResultSet, if SimpleResultSet already contain value, nothing happen */
		public void addNewColumn(String nameOfColumn,boolean isExtend) {
		if(isExtend) {
			this.ExtendResultSet.addNewColumn(nameOfColumn);
		}
		else {
			this.MainResultSet.addNewColumn(nameOfColumn);
		}
		}
		
		public void ChangeValue(String columnName,int indexOfRow,String value,boolean isExtend) throws SocketComunicationException {
			if(isExtend) {
				this.ExtendResultSet.ChangeValue(columnName, indexOfRow, value);
			}else {
				this.MainResultSet.ChangeValue(columnName, indexOfRow, value);
			}
		}
		
		/**Metod return all values */
		public List<List<String>> getValues(boolean exten){
			if(exten) {
				return this.ExtendResultSet.value;
			}
			else {
				return this.MainResultSet.value;

			}
		}
		
		public  List<String> getColumnName(boolean extend){
			if(extend) {
				return this.ExtendResultSet.getColumName();
			}
			return this.MainResultSet.getColumName();
		}
		
		public boolean isEmpty(boolean extend) {
			if(extend) {
				return this.ExtendResultSet.value.isEmpty();
			}
			return this.MainResultSet.value.isEmpty();
		}
		//class which represent and save all value
		private class SimpleResultSetValue{
			//first arrayListRepresent column, other row
			private List<List<String>> value=Collections.synchronizedList(new ArrayList<List<String>>());
			private List<String> columnName=Collections.synchronizedList(new ArrayList<String>());
			
			
			public SimpleResultSetValue(String message) {
				String [] value=message.split(SocketComunication.getRegexPatern(SocketComunication.RowCharacter));
				// first value in list is list of columnValue
				if(value[0].trim().startsWith((SocketComunication.ColumnCharacter), 0)) {
					value[0]=value[0].trim().substring(2,value[0].length());
				}
				
				this.columnName=new ArrayList<>(Arrays.asList(value[0].split(SocketComunication.getRegexPatern(SocketComunication.ColumnCharacter))));
				for(int i=1;i<value.length;i++) {
					if(value[i].trim().startsWith((SocketComunication.ColumnCharacter), 0)) {
						value[i]=value[i].trim().substring(2,value[i].length());
					}
					ArrayList<String>x=new ArrayList<>(Arrays.asList(value[i].split(SocketComunication.getRegexPatern(SocketComunication.ColumnCharacter))));
					this.addRow(x);
				}
			}
			
			public void addNewColumn(String columnName) {
				synchronized(this) {
					if(!this.value.contains(columnName)) {
						this.columnName.add(columnName);
						this.value.forEach((row)->{
							row.add(null);
						});
						
					}
				}
			}
			private SimpleResultSetValue(List<String>columnName) {
				this.columnName=Collections.synchronizedList(columnName);
			}
			private List<String> getColumName(){
				return this.columnName;
			}
			
			/**Metod add new value, value have to be in same order as columnName */
			public void addRow(List<String> list) {
				synchronized(this) {
					
				this.value.add(Collections.synchronizedList(list));
				}
			}
			public void ChangeValue(String columnName,int indexOfRow,String value) throws SocketComunicationException {
				synchronized(this) {
					int index=this.columnName.indexOf(columnName);
					if(index==-1) {
						throw new SocketComunicationException("Exception in change value, column name"+columnName+" is not exist");
					}
					try {
					this.value.get(indexOfRow).set(index, value);
					}
					catch(IndexOutOfBoundsException e) {
						throw new SocketComunicationException("Exception in change value, index"+indexOfRow+"is not exist");

					}
				}
			}
			/**Metod return appropriate row, or null if a index is outOf bounds */
			private List<String>getRowValue(int index){
				if (this.value.size()<=index) {
					return null;
				}
				return this.value.get(index);
			}
			
			/**Metod return appropriate String value
			 * @param rowNumber- number of row
			 * @param Type of needed value */
			public String getValue(int rowNumber, String columnName) {
				synchronized(this) {

					if (this.value.size()<=rowNumber) {
						return null;
					}
					
					
				return this.value.get(rowNumber).get(this.columnName.indexOf(columnName));
	
				
				}
			}
			public int lenght() {
				synchronized(this) {
					return this.value.size();
				}
			}
			
			
			@Override
			public String toString() {
				StringBuilder br=new StringBuilder();
				synchronized(this) {
				
					this.columnName.forEach((p)->{
						br.append(ColumnCharacter);
						br.append(p);

					});
					br.append(SocketComunication.RowCharacter);
					this.value.forEach((p)->{
						p.forEach((v)->{
							br.append(ColumnCharacter);

							br.append(v);
						});
						br.append(SocketComunication.RowCharacter);
					});	
					return br.toString();
				}
			}
			}
	
		
		
	}
	
	public static enum SocketComunicationEnum{
		Synchronization,StartSynchronization,SearchnewUser,
		Loggin,Register,FinishRegistration,SendMessage,UserName,
		CreateNewChat;
		
		public String getSQLPath() {
			return null;
		}
	}
	
	public static enum TypeOfUUIDRecipient{
		GroupChat,UserTOUser;
	}
	
	public static final class SocketComunicationException extends Exception{
		
		public SocketComunicationException(String message) {
			super(message);
		}
		public SocketComunicationException(String message,String [] sendMessage) {
			super(message);
		}
	}

	public TypeOfUUIDRecipient getTypeUUID() {
		return this.typeOfUUID;
	}
	public String getUUIDTask() {
		return UUIDTask;
	}


	public String getUUIDSender() {
		return this.UUIDSender;
	}
	public String getUUIDRecipient() {
		return UUIDRecipient;
	}


	public InetAddress getIPAdress() {
		return IPAdress;
	}
	
	

	// SocketMessage look like as follow
	//UUID recipient separator
	//UUID task separator separator
	//mainSQL Enum separator
	//first SimpleResultSet separator
	//second ResultSet separator- pokud second separator neni, musi byt hodnota null
	//enum ....
	@Override
	public String toString() {
		StringBuilder bd=new StringBuilder();
		bd.append(this.UUIDRecipient);
		bd.append(this.Separator);
		bd.append(this.UUIDTask);
		bd.append(this.Separator);
		OneSocketMessage mes;
		for(int i=0;(mes=this.getMessage(i))!=null;i++) {
			if(i>0) {
				bd.append(this.Separator);
			}
			bd.append(mes.toString());
			
		}
		return bd.toString();
	}
	
	public String generateMessageUUID_SetUUIDTask() {
		this.UUIDTask=this.generateMessageUUID();
		return this.UUIDTask;
	}

	 private static final String ALPHANUMERIC_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	 private static final AtomicInteger COUNTER = new AtomicInteger(0);

	 /**Metod return new UUID of chat, by join added string
	  *Order of String, depends on their alphabet
	  */
	 public static String generateChatUUID(String UUIDOwner,String UUIDNewUser) {
		 int com=UUIDOwner.compareTo(UUIDNewUser);
		 if(com>0) {
			 return UUIDNewUser+UUIDOwner;
		 }
		 else if(com<0) {
			 return UUIDOwner+UUIDNewUser;
		 }
		 return null;
	 }
	 
	 
	//have to change generation, includeUserUUID;
	public static String generateMessageUUID() {
        // Get the current timestamp in milliseconds
        long timestamp = System.currentTimeMillis();

        // Increment the counter and add it to the timestamp
        int count = COUNTER.getAndIncrement();
        if (count >= 1000) {
            // Reset the counter if it reaches 100
            COUNTER.set(0);
        }
        timestamp += count;

        // Convert the timestamp to base62
        String base62Timestamp = toBase62(timestamp);


        // Append the random character to the base62 timestamp
        return base62Timestamp;	}
	
		private static String toBase62(long value) {
	        if (value == 0) {
	            return "0";
	        }

	        StringBuilder sb = new StringBuilder();
	        while (value > 0) {
	            sb.append(ALPHANUMERIC_CHARACTERS.charAt((int) (value % 62)));
	            value /= 62;
	        }

	        return sb.reverse().toString();
	    }
	


}
