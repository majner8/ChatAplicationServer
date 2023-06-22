package SQL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Set;

import CommonPart.SQL.MainSQL;
import CommonPart.SocketComunication.SocketComunication.SimpleResultSet;
import CommonPart.ThreadManagement.ThreadPoolingManagement.ProcessSQLTask;

public class SQLServer extends MainSQL {

	public SQLServer() throws FileLoadException {
		super(databaseTaskType.values());
		// TODO Auto-generated constructor stub
	}

	public static final String path="SQL\\";
	
	public static enum databaseTaskType{// insert
		Loggin("Loggin.sql"),Registration("Registration.sql"), FinishRegistration("FinishRegistration.sql"),
		SendMessage("Loggin.sql"),SelectDeviceUUIDRegister("SelectDeviceUUIDRegister.sql"),SelectDeviceUUIDLoggin("SelectDeviceUUIDLoggin.sql"),InsertDeviceUUID("InsertDeviceUUID.sql"),
		SelectTableWhereUserHaveInteract("SelectTablesWhereUserHaveInteract.sql"),FindTableWithNewValue("FindTableWithNewValue.sql"),
		Synchronization("Synchronization.sql"),FindNewUser("FindNewUser.sql")
		,CreateUserChat("CreateUserChat.sql"),InsertMessageToChat("InsertMessageToChat.sql")
		,NoticeNewChatToUserQUickTable("NoticeNewMessageIntoUserQuickChat.sql"),SelectDefaultNameToUser("SelectDefaultNameToNewUser.sql")
		,SelectFromAdministrationTable("SelectFromAdministrationTable.sql"),
		GetUserConnectedName("GetUserConnectedName.sql"),
		VerifyFindUser("VerifyFindUser.sql");	
		
		
	
		private String URLQuery;
		databaseTaskType(String UURL){
			this.URLQuery=UURL;
		}
		@Override
		public String toString() {
			return SQLServer.path+this.URLQuery;
		}
	}

	static {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	@Override
	public Connection CreateDatabaseConnection() throws SQLException {
		// TODO Auto-generated method stub
	
		return DriverManager.getConnection("jdbc:mysql://localhost:3307", "root", "root");


	}
}
