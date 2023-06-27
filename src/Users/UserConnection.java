package Users;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import CommonPart.SQL.MainSQL;
import CommonPart.SQL.MainSQL.Query;
import CommonPart.SocketComunication.ComunicationPortHandling;
import CommonPart.SocketComunication.ComunicationPortHandling.ComunicationPortHandlingInterface;
import CommonPart.SocketComunication.SocketComunication;
import CommonPart.SocketComunication.SocketComunication.OneSocketMessage;
import CommonPart.SocketComunication.SocketComunication.SimpleResultSet;
import CommonPart.SocketComunication.SocketComunication.SocketComunicationEnum;
import CommonPart.SocketComunication.SocketComunication.SocketComunicationException;
import CommonPart.ThreadManagement.ThreadPoolingManagement;
import Main.Main;
import Main.ThreadManagementServer;
import SQL.SQLServer;

public class UserConnection {

	private final String userUUID;
	
	
	
	
	public UserConnection(String userUUID) {
		this.userUUID = userUUID;
		System.out.println("new UserConnection");
	}

	//when you are sending message, you have to verify if a user exist
		//and also if a user have approach to this chat
		private static Map<String,UserConnection>ConnectedUser=Collections.synchronizedMap(new HashMap<String,UserConnection>());
		
		/**Metod return reference to appropriate UserConnection
		 * Metod also increament number of reference to this User
		 * this metod it should be use only from ChatConnection et.c.
		 * if appropriate object do not exist metod create new one */
		public static UserConnection getUserConnection(String UserUUID) {
			UserConnection x;
			synchronized(ConnectedUser) {
				x=ConnectedUser.get(UserUUID);
				if(x==null) {
					x=new UserConnection(UserUUID);
					ConnectedUser.put(UserUUID, x);
				}
				x.AmountOfReference.incrementAndGet();
				
			}
			return x;
		}
		
		private synchronized void referenceRemoved() {
			if(this.AmountOfReference.decrementAndGet()<=0) {
				synchronized(this.ConnectedUser) {
					if(this.AmountOfReference.get()<=0) {
						this.ConnectedUser.remove(this.userUUID, this);
						System.out.println("I am removing reference");
					}
				}
			}
		}
		public void AddUserDeviceConnection(ComunicationPortHandling listenPort,String UserUUID) {
			
			UserDeviceConnection x=new UserDeviceConnection(listenPort);
			this.SetOfConnectedDevice.add(x);
			
						
		}
		
		public AtomicInteger AmountOfReference=new AtomicInteger(0);//appropriate amount of reference
		//to this user
		//if a AmountOfLink is zero, and also none of User device is connected
		//reference to this object can be removed from memory

		
		/**Metod put message to queue, after that will be send to appropriate recipient, and also message would be send
		 * to rest of connected device */
		private void OutPutMessage(SocketComunication message) {
			ThreadPoolingManagement.thread.Execute(()->{
				//decide if UUID is User-User/Or groupChat
				if(!this.isGroupChat(message.getUUIDRecipient())) {
					//user-toUserChat
					this.getUserConnection(message.getUUIDRecipient().replace(userUUID, "")).InputMessage(message, null);;
					
				}
				else {
					GroupChat.ProcessMessage(message, true);
				}
			});
		}
		
		
	
		/** Metod manage send message to all conected device, if user do not have connected device, nothing happen
		 * @param UUIDofSender- if you do not wan to send message back to sender
		 * in the other hand, put null*/
		public void InputMessage(SocketComunication message,String UUIDofSender) {
			
			ThreadPoolingManagement.thread.Execute(()->{
				synchronized(this.SetOfConnectedDevice) {
					this.SetOfConnectedDevice.forEach((S)->{
						
						if(UUIDofSender!=null&&UUIDofSender.equals(S.deviceUUID)) {
							//skip same Sender
						}
						else {
							S.comu.writeMessage(message.toString(), false);
						}
					});
				}
			});
		
		}
				
		private Set<UserDeviceConnection> SetOfConnectedDevice=Collections.synchronizedSet(new HashSet<UserDeviceConnection>());

		
		// have to be done later, in update which bring group chat
		private boolean isGroupChat(String RecipientUUID) {
			return false;
		}
		
		
		
		
		
		
 		public class UserDeviceConnection implements ComunicationPortHandling.ComunicationPortHandlingInterface
 		{
 			private final String deviceUUID;
 			private ComunicationPortHandling comu;
			protected UserDeviceConnection(ComunicationPortHandling comu) {
				this.comu=comu;
				deviceUUID=this.comu.getUUIDDevice();
				this.comu.ChangeComunicationInterface(this);
				// TODO Auto-generated constructor stub
			}
			
			private SynchronizationProcess syn;
			/**Metod chech if a object is init and eventually create new one
			 *@param init true-if you want to init object, false to removed
			 *@return SynchronizationProcess object, or null if a param is false */
			private synchronized SynchronizationProcess initSynchronization(boolean init) {
				if(!init) {
					this.syn=null;
					return null;
				}
				if(this.syn==null) {
					this.syn=new SynchronizationProcess();
				}
				return this.syn;
				
			}

			private class SynchronizationProcess{
			
				private LocalDateTime startSyn;
				
				private void ProcessSynchronizationMessage(SocketComunication message) {
					if(message.getMessage(0).getOneValue()!=null) {
						//starting synchronization
						if(message.getMessage(0).getOneValue().trim().equals("start")) {
						//message do not contain simpleResultSet
						this.StartSynchronization(message);
						}
						
						else {
							//have to valide, if Onevalue is LocalDateTime			
							this.LoadMessageFromChat(message);
						}
					}
					
				}

				//when a Database process task, it set this metod as true, 
				private  void StartSynchronization(SocketComunication mes) {
					
					
					//chech if a mes contain only one value
					//if it is true it mean that client ask server to send all UUID table 
					//where he is join
							if(startSyn!=null) {
								return;
							}

							//start writting new message into file
							comu.StartStopFileWriting(true);
							
							//set start time of synchronization
							startSyn=LocalDateTime.now();
							//metod load all user table and send them to device
							Query []x=MainSQL.getQuery(SQLServer.databaseTaskType.SelectTableWhereUserHaveInteract, new SimpleResultSet(new ArrayList<String>(),null), comu.getUserUUID());
							
							ThreadPoolingManagement.thread.ProcesSQLTask(x, (Statement,ResultSet, SQLException)->{
								try {

									if(SQLException!=null) {
										SQLException.printStackTrace();
										
									}
									SimpleResultSet sim=new SimpleResultSet(ResultSet);
									int chatUUIDIndex=sim.getColumnName(false).indexOf("chatUUID");
									MainSQL.ClosedStatement(Statement, ResultSet);

									List<String> value;
									for(int i=0;(value=sim.getRowValue(false, i))!=null;i++) {
										//stored used chatUUID to Set, for last using
										this.nameOfUsedTable.add(value.get(chatUUIDIndex));
									}									
									//send used table name to user as back
									SocketComunication retu=SocketComunication.createNewSocketComunication(null,null);
									OneSocketMessage ms=new OneSocketMessage(SocketComunication.SocketComunicationEnum.StartSynchronization, sim);
									retu.addOneSocketMessage(ms);
									comu.writeMessage(retu.toString(), true);
								} catch (SQLException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
									Main.stopServer(null);
								}
							});
							return;	
				}	
				
				//true if userName was send
				private boolean UserNameSend=false;
				
				private void LoadMessageFromChat(SocketComunication message) {
				
					String tim=message.getMessage(0).getOneValue();
					if(tim.equals("null")) {
						tim=Timestamp.from(Instant.ofEpochSecond(1)).toString();
					}

					
					String time=tim;
					SimpleResultSet res=new SimpleResultSet(new ArrayList<String>(),new ArrayList<String>());
					res.addNewColumn("TimeOfMessage", true);
					res.addNewColumn(MainSQL.tableName.toString(), true);
					
					synchronized(this.nameOfUsedTable) {
						this.nameOfUsedTable.forEach((S)->{
							ArrayList<String> x=new ArrayList<String>();
							x.add(time);
							x.add(S);
							//metod add all used table and time to query
							res.addValue(x, true);
						});
					}
					Query []newMessageValue=MainSQL.getQuery(SQLServer.databaseTaskType.Synchronization, res, "");
					
					ThreadManagementServer.thread.ProcesSQLTask(newMessageValue, (Statement,ResultSet,SQLException)->{
						if(SQLException!=null) {
							SQLException.printStackTrace();
							MainSQL.ClosedStatement(Statement, ResultSet);
							return;
						}
						
						try {
							
							SimpleResultSet x=new SimpleResultSet(ResultSet);
							MainSQL.ClosedStatement(Statement, ResultSet);
							if(x.isEmpty(false)) {
								//synchronization finish
								this.EndSynchronization();
							}
							OneSocketMessage mes=new OneSocketMessage(SocketComunication.SocketComunicationEnum.Synchronization,x);		
							
							SocketComunication m=SocketComunication.createNewSocketComunication(null, null);
							m.addOneSocketMessage(mes);
							if(!UserNameSend) {

								UserNameSend=true;

								SimpleResultSet UserQuickMessage=new SimpleResultSet(new ArrayList<String>(),null);
								UserQuickMessage.addNewColumn("userUUID", false);
								UserQuickMessage.addNewColumn("chatName", false);
								UserQuickMessage.addNewColumn(MainSQL.tableName, false);

								//used for noticifation
								AtomicInteger amountOfRunQuery=new AtomicInteger(this.nameOfUsedTable.size());
								
								//name of usedTable
								synchronized(this.nameOfUsedTable) {
									this.nameOfUsedTable.forEach((S)->{
										Query[] query=MainSQL.getQuery(SQLServer.databaseTaskType.SelectFromAdministrationTable, new SimpleResultSet(new ArrayList<String>(),null), S);
										ThreadManagementServer.thread.ProcesSQLTask(query, (Stm,Rest,SQL)->{
											if(SQL!=null) {
												SQL.printStackTrace();
												return;
											}
											ArrayList<String>value=new ArrayList<String>();
											try {

												while (Rest.next()){
													value=new ArrayList<String>();
												value.add(Rest.getString("userUUID"));
												value.add(Rest.getString("chatName"));
												value.add(S);
												UserQuickMessage.addValue(value, false);
												}
											} catch (SQLException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}
											finally {
												MainSQL.ClosedStatement(Stm, ResultSet);
												if(amountOfRunQuery.decrementAndGet()==0) {
													//Send message to server
													OneSocketMessage oneMes=new OneSocketMessage(SocketComunicationEnum.Synchronization,UserQuickMessage);
													m.addOneSocketMessage(oneMes);
													comu.writeMessage(m.toString(), true);
												}
											}
											
										});
									});
								
								}
							

							}
							else {
							
							comu.writeMessage(m.toString(), true);
							
							}
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					});
					
				}
				//set, where is stored all used table, during syn.
				private Set<String>nameOfUsedTable=Collections.synchronizedSet(new HashSet<String>());
				
				private void EndSynchronization( ) {
				
				
					
					initSynchronization(false);
				}
			
			}

	
			

			@Override
			public void ProcessMessage(SocketComunication message) {
				// TODO Auto-generated method stub
				OneSocketMessage mes=message.getMessage(0);
				SocketComunicationEnum ems=mes.getTypeOfMessage();
				
				if(ems==SocketComunicationEnum.Synchronization) {
					
					this.initSynchronization(true).ProcessSynchronizationMessage(message);
					
					return;
				}
				if(ems==SocketComunicationEnum.SearchnewUser) {
					new SearchNewUser(message);
					return;
				}
				if(ems==SocketComunicationEnum.SendMessage) {					
				this.ProcessSendMessageToUser(message, message.getMessage(0).getOneValue().trim().equals("true")?true:false,null);
				}
			}
		
			
			/**Metod process searching task */
			private void SearchNewUser(SocketComunication message) {
				OneSocketMessage mes=message.getMessage(0);
				//searching new User on input
				SimpleResultSet value=new SimpleResultSet(new ArrayList<String>(),null);
				value.addNewColumn("UUIDUser", false);
				
				value.addNewColumn("name", false);
				value.addNewColumn("LastName", false);
				ArrayList<String>row=new ArrayList<String>();
				row.add(userUUID);
				row.add("%"+mes.getOneValue()+"%");
				row.add("%"+mes.getOneValue()+"%");
				value.addValue(row, false);
				Query [] x=MainSQL.getQuery(SQLServer.databaseTaskType.FindNewUser, value, "");

				ThreadManagementServer.thread.ProcesSQLTask(x, (Statement,ResultSet,SQLException)->{
					if(SQLException!=null) {
						SQLException.printStackTrace();
						return;
					}
					SimpleResultSet rs;
					try {
						rs=new SimpleResultSet(ResultSet);
						MainSQL.ClosedStatement(Statement, ResultSet);
				
						//now have to
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					}
					OneSocketMessage one=new OneSocketMessage(SocketComunicationEnum.SearchnewUser, rs);
					//send responce to server, with same UUID
					SocketComunication mess=SocketComunication.createNewSocketComunication(null, message.getUUIDTask());
					mess.addOneSocketMessage(one);
					this.comu.writeMessage(mess.toString(), true);
				});
				return;

			}
			
			/**Metod process sending message in chat User-User
			 * @param AdministrationTable- SimpleResultSet will be send to sender as second oneMessage
			 * If you put null, it would not be send*/
			private void ProcessSendMessageToUser(SocketComunication message, boolean createNewChat,SimpleResultSet AdministrationTable) 
			{

				//in future have to invent, how to device UUID, User-ToUser-and Chat				
				
				if(createNewChat) {

					
					
					SimpleResultSet createNewChatSim=new SimpleResultSet(new ArrayList<String>(),null);
					createNewChatSim.addNewColumn("userUUID", false);
					createNewChatSim.addNewColumn(MainSQL.tableName.toString(), false);

						ArrayList<String>value=new ArrayList<String>();
						value.add(message.getUUIDRecipient().replace(this.comu.getUserUUID(), ""));
						value.add(message.getUUIDRecipient());
						createNewChatSim.addValue(value, false);
						
						value=new ArrayList<String>();
						value.add(userUUID);
						value.add(message.getUUIDRecipient());
						createNewChatSim.addValue(value, false);
					

												
						//including value for creating chat
					Query[] createNewUserChat=MainSQL.getQuery(SQLServer.databaseTaskType.CreateUserChat, createNewChatSim, null);
					//userUUID
					//chatName
					ThreadManagementServer.thread.ProcesSQLTask(createNewUserChat, (stm,rs,Ex)->{
						if(Ex!=null) {
							Ex.printStackTrace();
							return;
						}
						//process notification QuickTable
						//have to be done after table is create
						
						MakeNotificationUserQuickTable.MakeNotificationUserToUserChat(message.getUUIDRecipient());
						SimpleResultSet simRs=null;
						try {
							
							//just change resultSet, to create chat with appropriate value on other side
							simRs=new SimpleResultSet(rs);
							message.getMessage(0).SetMessage(simRs, SocketComunication.SocketComunicationEnum.CreateNewChat);
						
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							return;
						}
						
						MainSQL.ClosedStatement(stm, rs);
						this.ProcessSendMessageToUser(message, false,simRs);
					});
					return;
				}
				
				

				
				
				Query [] insertQueryTask=MainSQL.getQuery(SQLServer.databaseTaskType.InsertMessageToChat,message.getMessage(1).getSimpleResultSet() ,null);

				
			
				
				ThreadManagementServer.thread.ProcesSQLTask(insertQueryTask, (Statement,ResultSet,SQLException)->{
					if(SQLException!=null) {
						SQLException.printStackTrace();
						return;
					}
					try {
						//retrive get autoIncrementUUID
						if(!ResultSet.next()) {
							(new SQLException("ResultSetCannot be null")).printStackTrace();
							return;
						}
						
						Long UUID=ResultSet.getLong("LastGenerateUUID");
						
						String time=ResultSet.getTimestamp("TimeOfMessage").toLocalDateTime().toString();
						MainSQL.ClosedStatement(Statement, ResultSet);

						{
							//send message back to sender
							SimpleResultSet sender=new SimpleResultSet(new ArrayList<String>(),null);
							sender.addNewColumn("LastGenerateUUID", false);
							sender.addNewColumn("TimeOfMessage", false);
							ArrayList<String>value=new ArrayList<String>();
							value.add(String.valueOf(UUID));
							value.add(time);
							sender.addValue(value, false);
							OneSocketMessage mes=new OneSocketMessage(SocketComunication.SocketComunicationEnum.SendMessage,sender);
							SocketComunication messages=SocketComunication.createNewSocketComunication(null, message.getUUIDTask());
							messages.addOneSocketMessage(mes);
							if(AdministrationTable!=null) {
								message.addOneSocketMessage(new OneSocketMessage(SocketComunication.SocketComunicationEnum.CreateNewChat,AdministrationTable));
							};
								this.comu.writeMessage(messages.toString(), false);
							
						}
						
						//add them into SimpleResultSet
						message.getMessage(1).getSimpleResultSet().ChangeValue("numberOFmessage", 0, String.valueOf(UUID), false);
						message.getMessage(1).getSimpleResultSet().ChangeValue("TimeOfMessage", 0,time, false);
						;
						//sendMesssage,
						
						
						//send message to appropriate chat
						OutPutMessage(message);
						//send message other connected device, including this
						InputMessage(message,deviceUUID);
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					} catch (SocketComunicationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
			}
			
			private static class MakeNotificationUserQuickTable{
				
				/**Metod process notification, have to be call only with chatUserToUser */
				public static void MakeNotificationUserToUserChat(String chatUUID) {
					// after(adding group chat), make control if UUID is UserToUser
					
					//UserToUserUUID- is just two user UUID
					int size=chatUUID.length();
					
					MakeNotificationUserQuickTable.SelectDefaultNameAndSavedMessageUserToUserChat(chatUUID, chatUUID.substring(0,size/2),chatUUID.substring(size/2,size));
					MakeNotificationUserQuickTable.SelectDefaultNameAndSavedMessageUserToUserChat(chatUUID, chatUUID.substring(size/2,size),chatUUID.substring(0,size/2));

				}
				private static void SelectDefaultNameAndSavedMessageUserToUserChat(String chatUUID,String UUIDUser,String TableName) {
					SimpleResultSet res=new SimpleResultSet(new ArrayList<String>(),null);
					res.addNewColumn("UUIDUser", false);
					ArrayList<String> x=new ArrayList<String>();
					//chat UUID is user1.UUID +user2.UUID, in order by alphabet
					//to get other user UUID,just remove this userUUID
					x.add(UUIDUser);
					res.addValue(x, false);
					
					Query [] SelectDefaultNameQuery=MainSQL.getQuery(SQLServer.databaseTaskType.SelectDefaultNameToUser, res, "");

					ThreadManagementServer.thread.ProcesSQLTask(SelectDefaultNameQuery, (stm,rs,EX)->{
						if(EX!=null) {
							EX.printStackTrace();
							return;
						}
						try {
							if(!rs.next()) {
								new SQLException("ResultSet cannot be null").printStackTrace();
							}
							MakeNotificationUserQuickTable.makeNotificationUserQuickTable(chatUUID, TableName, rs.getString(1));
							MainSQL.ClosedStatement(stm, rs);
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							MainSQL.ClosedStatement(stm, rs);

						}
					});
				}
				
				private static void makeNotificationUserQuickTable(String chatUUID,String userUUID,String TableName) {

					SimpleResultSet res=new SimpleResultSet(new ArrayList<String>(),null);
					res.addNewColumn("chatUUID", false);
					res.addNewColumn("UserTableName", false);
					ArrayList<String> value=new ArrayList<String>();
					value.add(chatUUID);
					value.add(TableName);
					res.addValue(value, false);
					Query[] InsertNewChatIntoQuickMessage=MainSQL.getQuery(SQLServer.databaseTaskType.NoticeNewChatToUserQUickTable, res, userUUID);


					ThreadManagementServer.thread.ProcesSQLTask(InsertNewChatIntoQuickMessage, (stm,rs,EX)->{
						if(EX!=null) {
							EX.printStackTrace();
						}
						MainSQL.ClosedStatement(stm, rs);
						
					
					});
				}
			}
			/**Metod make saved notification to userQuickTable, that new chat was created
			 * @param UserTableName- null if you are creating user-userChat, UserTableName- will diferent to user
			 * @throws RunnTimeException(when will be UserTableName-null and @param ListOfUserUUID will have more than 2 object)*/
			private void MakeNotificationUserQuickTable(String []ListOfUserUUID,String UserTableName,String chatUUID) {
				if(UserTableName==null&&ListOfUserUUID.length!=2) {
					throw new RuntimeException("cannot be UserTableName null, if you are creating group chat ");
				}
				if(UserTableName==null) {
					
					
					
				}
			}

			private class SearchNewUser {
				
				private final String messageUUIDtask;
				
				public SearchNewUser(SocketComunication message) {
					this.messageUUIDtask=message.getUUIDTask();
					this.StartSearching(message);
				}
				private  void StartSearching(SocketComunication message) {
					OneSocketMessage mes=message.getMessage(0);
					//searching new User on input
					SimpleResultSet value=new SimpleResultSet(new ArrayList<String>(),null);
					value.addNewColumn("UUIDUser", false);
					
					value.addNewColumn("name", false);
					value.addNewColumn("LastName", false);
					ArrayList<String>row=new ArrayList<String>();
					row.add(userUUID);
					row.add("%"+mes.getOneValue()+"%");
					row.add("%"+mes.getOneValue()+"%");
					value.addValue(row, false);
					Query [] x=MainSQL.getQuery(SQLServer.databaseTaskType.FindNewUser, value, "");

					ThreadManagementServer.thread.ProcesSQLTask(x, (Statement,ResultSet,SQLException)->{
						if(SQLException!=null) {
							SQLException.printStackTrace();
							return;
						}
						SimpleResultSet rs;
						try {
							rs=new SimpleResultSet(ResultSet);
							MainSQL.ClosedStatement(Statement, ResultSet);
							int UUIDUserIndex=rs.getColumnName(false).indexOf("UUIDUser");
							List<List<String>> values=rs.getValues(false);
							//if list is empty
							//have to send directly message
							if(values.isEmpty()) {
								this.SendResultSetToServer(rs, this.messageUUIDtask);
								return;
							}
							
							
							this.amountOfRunningSubQuery.set(values.size());
							values.forEach((list)->{
								//metod compare and eventually removed nonSamevalue
								ThreadManagementServer.thread.Execute(()->{
									this.CompareValueRowWithQuickTable(rs.getColumnName(false),list.get(UUIDUserIndex),list);
								});
							
							});
							
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							return;
						}
						
						
						
					});

				}

				//as key is userUUID
				//have to be same as limit, when a query which complete verify task is finish, than value is removed
				//when value is empty, process of searching user will be end
				private AtomicInteger amountOfRunningSubQuery=new AtomicInteger(20);
				private Map<String,List<String>> savedRow=Collections.synchronizedMap(new HashMap<String,List<String>>());
			
				
			/** @param rowValue- value have to be order by searNewUserColumn*/
				private void CompareValueRowWithQuickTable(List<String>responceColumnName,String findUserUUID,List<String>rowValue) {

					this.savedRow.put(findUserUUID, rowValue);
					Query qur[]=null;

					//as table name have to be userUUID
					//as chat name have to be chatUUID, chatUUID is thisUserUUID+otherUserUUID in alphabet order
					{SimpleResultSet res=new SimpleResultSet(new ArrayList<String>(),null);
					res.addNewColumn("chatUUID", false);
					ArrayList<String> value=new ArrayList<String>();
					//generate chatUUSerUUID
					value.add(SocketComunication.generateChatUUID(userUUID,findUserUUID));
					res.addValue(value, false);
					qur=MainSQL.getQuery(SQLServer.databaseTaskType.VerifyFindUser, res, userUUID);
				 }
					ThreadPoolingManagement.thread.ProcesSQLTask(qur,(Statement,Result,EX)->{
						if(EX!=null) {
							EX.printStackTrace();
							return;
						}
						try {
							if(!Result.next()) {
								new SQLException("Result.next cannot be false").printStackTrace();
								return;
							}
							if(!Result.getBoolean(1)) {
								//false have to removed from savedRow
								savedRow.remove(findUserUUID);
							}
							if(amountOfRunningSubQuery.decrementAndGet()==0) {
								//this thread is finish last have to complete
								SimpleResultSet rs=new SimpleResultSet(responceColumnName,null);
								synchronized(this.savedRow) {
									savedRow.forEach((Key,value)->{
										rs.addValue(value, false);
									});
								}
								this.SendResultSetToServer(rs, this.messageUUIDtask);
								
							}
							
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							return;
						}
					});
					
				}

				private void SendResultSetToServer(SimpleResultSet result,String messageUUID) {
					OneSocketMessage one=new OneSocketMessage(SocketComunicationEnum.SearchnewUser, result);
					//send responce to server, with same UUID
					SocketComunication mess=SocketComunication.createNewSocketComunication(null,messageUUID);
					mess.addOneSocketMessage(one);
					comu.writeMessage(mess.toString(), true);
				}
			}



			@Override
			public void ConnectionIsEnd(Exception e) {
				// TODO Auto-generated method stub
				SetOfConnectedDevice.remove(this);
				referenceRemoved();
			
			}
			
 		}


 		public static class DoNotSendMessageToSenderSocketComunicationMessage {
 			public String senderUUID;
 			public SocketComunication message;
 			public DoNotSendMessageToSenderSocketComunicationMessage(SocketComunication message,String senderUUID) {
 				this.message=message;
 				this.senderUUID=senderUUID;
 			}
 			
 		}
 		

		

		
		

		
}
