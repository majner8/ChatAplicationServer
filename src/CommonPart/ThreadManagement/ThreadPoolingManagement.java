package CommonPart.ThreadManagement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import CommonPart.SQL.MainSQL.Query;
import CommonPart.SocketComunication.SocketComunication.SimpleResultSet;

public abstract class ThreadPoolingManagement {

	public static ThreadPoolingManagement thread;
	public static boolean isShutDown;
	public ThreadPoolingManagement(){
		this.thread=this;
	}
	
	
	
	
//	private static ScheduledExecutorService ScheduledAndWaitService=Executors.newSingleThreadScheduledExecutor();
	
	
	//private static ExecutorService Service;

	public static void ShutDown() {thread.ShutDownService();}
	
	public abstract void ProcessReceivedMessage(Runnable command);
	
	public abstract void ProcesSQLTask(Query[] SQLQueryTask,ProcessSQLTask callAfter);
	
	public abstract void schedule(Runnable command, long delay,TimeUnit timeUnit);
	
	public abstract void Execute(Runnable Command);

	protected abstract void ShutDownService();
	public static interface ProcessSQLTask{
		public  void SQLTaskFinish(Statement stm,ResultSet rs,SQLException duringPRocess);
	}
	
	public static class ProcessSQLTaskObject {
		private ResultSet resultSet;
		private ProcessSQLTask callAfter;
		private SQLException ExceptionduringPRocess;
		private Statement stm;
		public ProcessSQLTaskObject(ProcessSQLTask callAfter,Statement stm,ResultSet resultSet, SQLException exceptionduringPRocess) {
			this.resultSet = resultSet;
			this.callAfter=callAfter;
			ExceptionduringPRocess = exceptionduringPRocess;
			this.stm=stm;
		}
		public ResultSet getResultSet() {
			return resultSet;
		}
		public SQLException getExceptionduringPRocess() {
			return ExceptionduringPRocess;
		}
		public void makeTask() {
			this.callAfter.SQLTaskFinish(stm,resultSet, ExceptionduringPRocess);
		}
		
		
	}

}
