package Users;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import CommonPart.SQL.MainSQL;
import CommonPart.SQL.MainSQL.Query;
import CommonPart.SocketComunication.ComunicationPortHandling;
import CommonPart.SocketComunication.SocketComunication;import CommonPart.SocketComunication.SocketComunication.OneSocketMessage;
import CommonPart.SocketComunication.SocketComunication.SimpleResultSet;
import CommonPart.SocketComunication.SocketComunication.SocketComunicationException;
import CommonPart.ThreadManagement.ThreadPoolingManagement;
import CommonPart.ThreadManagement.ThreadPoolingManagement.ProcessSQLTask;
import Main.ThreadManagementServer;
import SQL.SQLServer;

public class ConnectionAutorize implements ComunicationPortHandling.ComunicationPortHandlingInterface{
	private final static  long timeout = (int)1*60000;	
	private final long StartTime;
	private final long MaxTimeout;
	private ComunicationPortHandling comunication;
	
	
	public ConnectionAutorize(BufferedReader read, BufferedWriter write, Socket socket) {
		this.comunication=new ComunicationPortHandling(false,this,read,write,socket);
		// TODO Auto-generated constructor stub
		this.StartTime=System.currentTimeMillis();
		this.MaxTimeout=this.StartTime+this.timeout;
	}
	private volatile int amountOfActiveProces=0;
	

	
		
	


	private boolean LogginRegisterSuccesful=false;//value represent state when
	//device has made loggin/registration, and now have to complete registration
	private void SendMessage(SocketComunication message) {
		
		this.comunication.writeMessage(message.toString(),false);
	}
	
	@Override
	public void ProcessMessage(SocketComunication message) {
		// TODO Auto-generated method stub
		
		OneSocketMessage mes;
		if((mes=message.getMessage(0))!=null) {
			if(mes.getTypeOfMessage()==SocketComunication.SocketComunicationEnum.Loggin) {
				this.Loggin(message);
			}
			if(mes.getTypeOfMessage()==SocketComunication.SocketComunicationEnum.Register) {
				this.Registration(message);
				
			}
			
			if(mes.getTypeOfMessage()==SocketComunication.SocketComunicationEnum.FinishRegistration&&LogginRegisterSuccesful) {
				this.FinishRegistration(message);
			}
			
			
		}
		
		

	}

	private void Registration(SocketComunication message) {
		String UserUUID=MainSQL.generateRandomAlphanumericString(15);
		try {
			//have to add user UUID to simpleResultSet
			message.getMessage(0).getSimpleResultSet().addNewColumn(MainSQL.NameOFUUIDColumn, false);
			message.getMessage(0).getSimpleResultSet().ChangeValue(MainSQL.NameOFUUIDColumn, 0,UserUUID , false);
		} catch (SocketComunicationException e1) {
			// TODO Auto-generated catch block
			SocketComunicationException x=new SocketComunicationException(String.format(null, null));
			e1.printStackTrace();
			
		}
		
		Query [] query;
		query=MainSQL.getQuery(SQLServer.databaseTaskType.Registration, message.getMessage(0).getSimpleResultSet(), "");
		ThreadPoolingManagement.thread.ProcesSQLTask(query, (Statement,ResultSet,SQLException)->{
			OneSocketMessage result;
			MainSQL.ClosedStatement(Statement, ResultSet);
			
			if(SQLException!=null) {
				//during a process happen some exception, have to handle it.
				if(SQLException.getErrorCode()!=1062) {
					// problem with SQLDatabase
					SQLException.printStackTrace();
					Main.Main.stopServer(null);
				}
				String Exce=SQLException.getMessage();
				if(Exce.contains(MainSQL.NameOfUUIDUserColumnWithTable)) {
					//Duplicate UUID user
					//this exception is very rare, so it is not neccesary make different solution for this exception
					//enough is just call this metod again
				this.ProcessMessage(message);
					return;
					
				}
				result=new OneSocketMessage(SocketComunication.SocketComunicationEnum.Register,"null");
				SocketComunication answer=SocketComunication.createNewSocketComunication(null, message.getUUIDTask());

				answer.addOneSocketMessage(result);

				SendMessage(answer);


			}
			else {
				this.comunication.setUserUUID(UserUUID);

			//metod sending result to client
				
				this.getDeviceUUID((DeviceUUID)->{
					this.comunication.setUUIDDevice(DeviceUUID);
					OneSocketMessage rs=new OneSocketMessage(SocketComunication.SocketComunicationEnum.Register,this.comunication.getUserUUID()+this.comunication.DeviceUUIDCharacter+DeviceUUID);
					
					SocketComunication answer=SocketComunication.createNewSocketComunication(null, message.getUUIDTask());

					answer.addOneSocketMessage(rs);

					SendMessage(answer);
					LogginRegisterSuccesful=true;

				}, this.comunication.getUserUUID(),SQLServer.databaseTaskType.SelectDeviceUUIDRegister);
			}
		});
		
	}
	
	private void Loggin(SocketComunication message) {
		Query [] query =null;
		query=MainSQL.getQuery(SQLServer.databaseTaskType.Loggin, message.getMessage(0).getSimpleResultSet(), "");
		ThreadPoolingManagement.thread.ProcesSQLTask(query, (Statement,ResultSet,SQLException)->{
			OneSocketMessage[] result = new OneSocketMessage[1];
			
			//handle Exception, in loggin mean automaticly return 
			if(SQLException!=null) {
				SQLException.printStackTrace();
				MainSQL.ClosedStatement(Statement, ResultSet);
				return;
			}
			//if simpleResultSet is null-it mean is empty, and loggin was not succesfull
			if(ResultSet==null) {
				MainSQL.ClosedStatement(Statement, ResultSet);
				result[0]=new OneSocketMessage(SocketComunication.SocketComunicationEnum.Loggin,"null");
				
			}
			else {

				SimpleResultSet simple;
				try {
					simple = new SimpleResultSet(ResultSet);
					//ResultSet was used, it can be closed and removed from memory
					MainSQL.ClosedStatement(Statement, ResultSet);
				
					
					//interface which will will be contining after another metod return deviceUUID
					
					MakeTaskAfter makeAfter=(DeviceUUID)->{
						//add deviceUUID to SimpleResultSet
						simple.addNewColumn("DeviceUUID", false);
						try {
							simple.ChangeValue("DeviceUUID", 0, DeviceUUID, false);
						} catch (SocketComunicationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							return;
						}
						if(simple.getValue("FinishRegistration", false, 0).equals("1")) {
							//userIsLoggin
							//move to anotherSection¨
							SocketComunication mes=SocketComunication.createNewSocketComunication(null,message.getUUIDTask());
							mes.addOneSocketMessage(result[0]);
							//this.comunication.writeMessage(mes.toString(), true);
							this.UserIsAutorized(mes);
							return;
						}
						else {
							
							//user is loggin, but registration is not complete
							this.LogginRegisterSuccesful=true;
						}
						SocketComunication mes=SocketComunication.createNewSocketComunication(null,message.getUUIDTask());
						mes.addOneSocketMessage(result[0]);
						SendMessage(mes);
						
					};
					
				result[0]=new OneSocketMessage(SocketComunication.SocketComunicationEnum.Loggin,simple);
				if(simple.getValue("UUIDUser", false, 0).equals("null")) {
					SocketComunication mes=SocketComunication.createNewSocketComunication(null,message.getUUIDTask());
					mes.addOneSocketMessage(result[0]);
					SendMessage(mes);
					
					
				}
				
				else {
				this.comunication.setUserUUID(simple.getValue("UUIDUser", false, 0));
				this.getDeviceUUID(makeAfter,this.comunication.getUserUUID(),SQLServer.databaseTaskType.SelectDeviceUUIDLoggin);
				return;
				}
				
				
				
				
				
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Main.Main.stopServer(null);
				}
			}
		});
	}
	
	private void FinishRegistration(SocketComunication message) {
		Query [] query =null;
		SimpleResultSet rs=message.getMessage(0).getSimpleResultSet();
		rs.addNewColumn("UUIDUser", false);
		
		try {
			rs.ChangeValue("UUIDUser", 0, this.comunication.getUserUUID(), false);
		} catch (SocketComunicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		query=MainSQL.getQuery(SQLServer.databaseTaskType.FinishRegistration, message.getMessage(0).getSimpleResultSet(), this.comunication.getUserUUID());
		ThreadPoolingManagement.thread.ProcesSQLTask(query, (Statement,ResultSet,SQLException)->{
			OneSocketMessage mes;
			MainSQL.ClosedStatement(Statement, ResultSet);

			if(SQLException!=null) {
				SQLException.printStackTrace();
				return;
			}
			
			mes=new OneSocketMessage(SocketComunication.SocketComunicationEnum.FinishRegistration, "true"); 
		
			SocketComunication answer=SocketComunication.createNewSocketComunication(null,message.getUUIDTask());
			answer.addOneSocketMessage(mes);
			//SendMessage(answer);
			this.UserIsAutorized(answer);
		});
		
		
	}
	
	
	/** Metod get device UUID, or insert new one to database, depends on InetAdress*/
	private void getDeviceUUID(MakeTaskAfter taskMakeAfter,String UserUUID,SQLServer.databaseTaskType databaseTask) {
		SimpleResultSet simRS=new SimpleResultSet(new ArrayList<String>(),null);
		simRS.addNewColumn("IpAdressDevice", false);
		{
			//metod init value and put them into resultSet
			ArrayList<String>rowValue=new ArrayList<String>();
			rowValue.add(this.comunication.getInetAdress().toString());
			simRS.addValue(rowValue, false);
		}
		Query[] firstTask = MainSQL.getQuery(databaseTask, simRS, UserUUID);// first query 
		ThreadManagementServer.thread.ProcesSQLTask(firstTask, (Statement,ResultSet, SQLException)->{
			//chech if a resultSet is not null, after that call another task
			if(SQLException!=null) {
				SQLException.printStackTrace();
				Main.Main.stopServer(null);
			}
			
			
			try {
				if(!ResultSet.next()) {
					String deviceUUID=this.comunication.generateUUID(5);
					// is null call insert task
					SimpleResultSet siRS=new SimpleResultSet(new ArrayList<String>(),null);
					siRS.addNewColumn("IpAdressDevice", false);
					siRS.addNewColumn("DeviceUUID", false);

					{
						ArrayList<String>value=new ArrayList<String>();
						value.add(this.comunication.getInetAdress().toString());
						value.add(deviceUUID);
						siRS.addValue(value, false);
					}
					Query []insertTask=MainSQL.getQuery(SQLServer.databaseTaskType.InsertDeviceUUID, siRS, UserUUID);
					//insert new UUID value
					ThreadManagementServer.thread.ProcesSQLTask(insertTask, (stm,rs,SQLExce)->{
						//chech exception
						if(SQLExce!=null) {
							//chech if exception is not duplicated
							if(SQLExce.getErrorCode()==1062) {
								if(SQLExce.getMessage().contains("IpAdressDevice")||SQLExce.getMessage().contains("DeviceUUID")) {
									//call this metod again
									this.getDeviceUUID(taskMakeAfter, UserUUID,databaseTask);
									MainSQL.ClosedStatement(stm, rs);
									return;
								}
								
							}
							SQLExce.printStackTrace();
							//stop server because error is not except and handle
							MainSQL.ClosedStatement(stm, rs);
							Main.Main.stopServer(null);
							return;
						}
						//everithing work properly
						//now you can set deviceUUID and run task after
						MainSQL.ClosedStatement(stm, rs);
						this.comunication.setUUIDDevice(deviceUUID);
						taskMakeAfter.run(this.comunication.getUUIDDevice());
						
					});
					
					MainSQL.ClosedStatement(Statement, ResultSet);
					return;
				}
				//IP adres contain decice UUID, have to get it
				this.comunication.setUUIDDevice(ResultSet.getString("DeviceUUID"));
				//continue with task
				MainSQL.ClosedStatement(Statement, ResultSet);

				taskMakeAfter.run(this.comunication.getUUIDDevice());
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}
	
	/** Metod end autorization
	 * @param Message-message which have to be send to user as notification 
	 * to message will be add information about user name*/
	private void UserIsAutorized(SocketComunication message) {
	
		//add UserUUID to SimpleResultSet
		
		SimpleResultSet x=new SimpleResultSet(new ArrayList<String>(),null);
		x.addNewColumn("UUIDUser", false);
		ArrayList<String> value=new ArrayList<String>();
		value.add(comunication.getUserUUID());
		x.addValue(value, false);
		Query []q=MainSQL.getQuery(SQLServer.databaseTaskType.GetUserConnectedName, x, "");
		
		ThreadManagementServer.thread.ProcesSQLTask(q, (Statement,ResultSet,Ex)->{
			if(Ex!=null) {
				Ex.printStackTrace();
				return;
			}
			try {
				if(!ResultSet.next()) {
					new SQLException("ResultSet.Next cannot be false").printStackTrace();
					return;
				}
				String name=ResultSet.getString("UserName");
				//add get result to message
			
				OneSocketMessage mes=new OneSocketMessage(SocketComunication.SocketComunicationEnum.UserName,name);
				message.addOneSocketMessage(mes);
				
				UserConnection con=UserConnection.getUserConnection(this.comunication.getUserUUID());
				con.AddUserDeviceConnection(this.comunication, this.comunication.getUserUUID());
				this.comunication.writeMessage(message.toString(), true);
				this.comunication.AllowFileWrite();

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			

		
		});
	}
	
	
	
	
	private static interface MakeTaskAfter{
		public void run(String DeviceUUID);
	}




	@Override
	public void ConnectionIsEnd(Exception e) {
		// TODO Auto-generated method stub
		
	}


	
}


	



	
	

