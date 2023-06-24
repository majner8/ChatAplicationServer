package Main;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import CommonPart.SQL.MainSQL;
import CommonPart.SQL.MainSQL.Query;
import CommonPart.SQL.MainSQL.Query.TypeOFQuery;
import CommonPart.ThreadManagement.ThreadPoolingManagement;

public class ThreadManagementServer extends ThreadPoolingManagement {

	public ThreadManagementServer() {
		
		//create MessageTaskMaker
		for(int i=0;i<3;i++) {
			new MessageTaskMaker();
		}
		//create databasePoolThred
		for(int i=0;i<5;i++) {
			try {
				new DatabasePoolManagement();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static LinkedBlockingQueue<Runnable> QueueOfUnprocessMessages=new LinkedBlockingQueue<Runnable>();
	private static LinkedBlockingQueue<ProcessSQLTaskObject> UnFinishSQLTask=new LinkedBlockingQueue<ProcessSQLTaskObject>();
	private ScheduledExecutorService ScheduledAndWaitService=Executors.newSingleThreadScheduledExecutor();

	private void AddUnfinishSQLTask(ProcessSQLTaskObject task) {
		
		this.UnFinishSQLTask.add(task);
		MessageTaskMaker.InteruptThread(false);
	}
	
	@Override
	public void ProcessReceivedMessage(Runnable command) {
		// TODO Auto-generated method stub
		this.QueueOfUnprocessMessages.add(command);
	}

	@Override
	public void ProcesSQLTask(Query[] SQLQueryTask, ProcessSQLTask callAfter) {
		// TODO Auto-generated method stub			
		DatabasePoolManagement.QueueOfUnprocessMessages.add(new DatabasePoolManagement.DatabaseQuery(SQLQueryTask,callAfter));
		
	}

	@Override
	public void schedule(Runnable command, long delay, TimeUnit timeUnit) {
		// TODO Auto-generated method stub
		ScheduledAndWaitService.schedule(command, delay, timeUnit);

	}

	@Override
	public void Execute(Runnable Command) {
		// TODO Auto-generated method stub
		this.ProcessReceivedMessage(Command);
	}

	@Override
	protected void ShutDownService() {
		// TODO Auto-generated method stub
		
	}

	private class DatabasePoolManagement extends Thread{
		public static LinkedBlockingQueue<DatabaseQuery> QueueOfUnprocessMessages=new LinkedBlockingQueue<DatabaseQuery>();
		private static List<Thread> ListOfThread=Collections.synchronizedList(new ArrayList<Thread>());

		private Connection con;
		
		public DatabasePoolManagement() throws SQLException {
			this.con=MainSQL.mainSQL.CreateDatabaseConnection();
			this.ListOfThread.add(this);
			super.start();
		}

		public synchronized static void InteruptThread() {
				MessageTaskMaker.ListOfThread.forEach((t)->{
					t.interrupt();
				});
			
			
		}
		@Override
		public void run() {
			while(!Main.isServerStopped()) {
				DatabaseQuery query = null;
				boolean MakeInterup=false;

				try {
					query=this.QueueOfUnprocessMessages.take();
					Statement stm=con.createStatement();
					ResultSet rs=this.makeTasks(query.getQuery(),stm);
					AddUnfinishSQLTask(new ProcessSQLTaskObject(query.getCallAfter(),stm,rs,null));
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {

					AddUnfinishSQLTask(new ProcessSQLTaskObject(query.getCallAfter(),null,null,e));
				}
				
			}
		}
		
		
		public synchronized ResultSet makeTasks(Query[] task,Statement stm)throws SQLException {
			ResultSet returns=null;

			try {
				//start transaction
				this.con.setAutoCommit(false);
				for(Query x:task) {

					stm.execute("use "+x.getDatabaseTyp().toString()+";");

					if(x.getTypeQuery()!=TypeOFQuery.SelectQuery) {

						stm.execute(x.getSQLQuery());
						continue;
					}
					else {
						//resultSetStatement last statement.
						returns=stm.executeQuery(x.getSQLQuery());
						break;
					}
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				this.con.rollback();
				throw e;
			}
			this.con.commit();
			return returns;
		}

		public static class DatabaseQuery{
		private Query[] query;
		private ProcessSQLTask callAfter;
			public DatabaseQuery(Query[] query, ProcessSQLTask callAfter) {
				this.query = query;
				this.callAfter = callAfter;
			}
			public Query[] getQuery() {
				return query;
			}
			public ProcessSQLTask getCallAfter() {
				return callAfter;
			}
			
			
		}

	}
	
	private class MessageTaskMaker extends Thread{
		private static List<Thread> ListOfThread=Collections.synchronizedList(new ArrayList<Thread>());
		public MessageTaskMaker() {
			this.ListOfThread.add(this);
			super.start();
			
		}
		@Override
		public void run() {
			this.MakeTask();
		}

		public  static void InteruptThread(boolean all) {
			if (all) {
				MessageTaskMaker.ListOfThread.forEach((t) -> {
					t.interrupt();
				});
				return;
			}
			ListOfThread.get(0).interrupt();
			


			
		}
		private void MakeTask() {
			while(!Main.isServerStopped()) {
				if(super.interrupted()) {
				}
				ProcessSQLTaskObject x=UnFinishSQLTask.poll();
				if(x!=null) {
					//process unfinishSQL task
					x.makeTask();
					continue;
				}
				try {

					Runnable task=QueueOfUnprocessMessages.take();
					task.run();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
				}
	
			}
		}
		
	}

	
}
