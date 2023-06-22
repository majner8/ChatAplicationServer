package CommonPart.SQL;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import CommonPart.SocketComunication.SocketComunication.SimpleResultSet;
import CommonPart.ThreadManagement.ThreadPoolingManagement;

public abstract class MainSQL {
	
	public static MainSQL mainSQL;
	public static int MaximumAllowLetterInOneMessage=240;
	public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
public static final DateTimeFormatter DateTimeformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
public static LoadSQLFile LoadSQLFile;
	public MainSQL(Enum [] ListOfQuery ) throws FileLoadException {
		if(ListOfQuery==null) {
			return;
		}
		this.LoadSQLFile=new LoadSQLFile();
		//make loadFileProcess
		//this.LoadSQLFile(new Enum[] {SQLServer.databaseTaskType.TemporaryTable		});
		this.LoadSQLFile.LoadSQLFile(ListOfQuery, ListOfPaternSQLTask);	
		//this.LoadSQLFile(new Enum[]{SQLServer.databaseTaskType.Synchronization});
		this.mainSQL=this;		
		this.LoadSQLFile=null;
	}
	public static class LoadSQLFile{
		private void LoadSQLFile(Enum [] listOFQuery,Map<String, String[]> ListOfPaternSQLTask) throws FileLoadException {
			for (Enum x:listOFQuery) {
				String URL=x.toString();
				ListOfPaternSQLTask.put(x.name(), this.ReadText(URL).toString().split(DatabaseCharacter.EndQuery.toString()));
			}
		}
		
		public String ReadText(String URL) throws FileLoadException {
			StringBuilder text = new StringBuilder();
			try (BufferedReader rd = new BufferedReader(new FileReader(new File(URL)))) {
				String line;
				while ((line = rd.readLine()) != null) {
					text.append(line).append("\n");
				}
			} catch (IOException e) {
				// Rather than just printing the stack trace, it's better to pass the exception to the FuctionException so
				// that you can get the details of what went wrong when the FuctionException is caught.
				e.printStackTrace();
				throw new FileLoadException();
			}
			return text.toString();
		}
		
	}
	
	public final static String NameOfUUIDUserColumnWithTable="registerusers.UUIDUser";
	public static final String NameOFUUIDColumn="UUIDUser";
	public final static String tableName="tablename";
	private static Map<String, String[]> ListOfPaternSQLTask=Collections.synchronizedMap(new HashMap<String,String[]>());//have to by synchronized wrap
private static final String ALPHANUMERIC_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
public static String generateRandomAlphanumericString(int length) {
Random random = new Random();
StringBuilder sb = new StringBuilder(length);
for (int i = 0; i < length; i++) {
int index = random.nextInt(ALPHANUMERIC_CHARACTERS.length());
char randomChar = ALPHANUMERIC_CHARACTERS.charAt(index);
sb.append(randomChar);
}
return "U" + sb.toString();
}
	
	public abstract Connection CreateDatabaseConnection() throws SQLException;
	public static Query[] getQuery(Enum DatabaseTaskType,SimpleResultSet simpleResultSet,String tableName) {
		String[] queryes=MainSQL.ListOfPaternSQLTask.get(DatabaseTaskType.name());
		if(queryes==null) {
			throw new PaternRunnTimeException("Enum type for databaseTask do not exist "+DatabaseTaskType.toString());
		}
		
		Query query[]=new Query[queryes.length];
		//metod ask if a query is insert(without resultSet) or select
		int i=0;
		for(String x:queryes) {
			x=x.trim();
			Query.TypeOFQuery typeOfQuery;
			if(x.startsWith(Query.TypeOFQuery.InsertQuery.toString())) {
				typeOfQuery=Query.TypeOFQuery.InsertQuery;
				
			}
			else if (x.startsWith(Query.TypeOFQuery.SelectQuery.toString())) {
				typeOfQuery=Query.TypeOFQuery.SelectQuery;
			}
			else if (x.startsWith(Query.TypeOFQuery.WithoutResultSet.toString())) {
				typeOfQuery=Query.TypeOFQuery.WithoutResultSet;
			}
			else {
				
			throw new PaternRunnTimeException("Loaded query is not have valid type "+x
					+"\n TaskQuery:"+DatabaseTaskType.toString());
			}
			x=x.substring(typeOfQuery.toString().length());
			x=x.trim();
			{
				if(tableName==null) {
					//table name have to be inside SimpleResultSet
					tableName=simpleResultSet.getValue(MainSQL.tableName.toString(), false,0);
					
					if(tableName==null) {
						throw new PaternRunnTimeException("SimpleResultSetDoNotContain Table name");
					}
				}
			}
			//find typeOfDatabase
			String []TypDatabase=x.split(DatabaseCharacter.BeginQuery.toString(), 2);
			//first value is enum of Type if not, exception will be thrown
			TypeOfDatabase type=null;
			try {
				type=TypeOfDatabase.valueOf(TypDatabase[0].trim());
			}
			catch(IllegalArgumentException e) {
				throw new PaternRunnTimeException("Enum was not find"
						+TypDatabase[0]		+"\n TaskQuery:"+DatabaseTaskType.toString());
			}
			
			Query qur=new Query(typeOfQuery,type,TypDatabase[1]);
			try {
				// this method process each query
				MainSQL.ProcessEachQuery(qur, simpleResultSet);
			}
			catch(PaternRunnTimeException e) {
				e.ADDreason("problem with query"+DatabaseTaskType.toString());
				throw e;
			}
			qur.setSQLQuery(qur.getSQLQuery().replaceAll(DatabaseCharacter.TableName.toString(), tableName));
			query[i]=qur;
			i++;
		}
		return query;
	}
	private static String makeQuotesAroundValue(String value) {
		return String.format("\"%s\"", value);
	}
	private static void ProcessEachQuery(Query task,SimpleResultSet rs) {
		String query = null;
		if(task.getTypeQuery()==Query.TypeOFQuery.InsertQuery) {
			 query=MainSQL.InsertQuery(task.getSQLQuery(), rs);
		}
		else if(task.getTypeQuery()==Query.TypeOFQuery.SelectQuery||task.getTypeQuery()==Query.TypeOFQuery.WithoutResultSet) {
			query=MainSQL.SelectNoResultSetQuery(task.getSQLQuery(), rs);
		}
		if(query==null) {
			throw new NullPointerException();
		}
		task.setSQLQuery(query);
	}
	
	
	
	
	private static String InsertQuery(String rawTask,SimpleResultSet rs) {
		String valueCharacter;
		//ArrayListWith index of value in SimpleResultSet appropriate to query
		ArrayList<Integer>NameOfValue=new ArrayList<Integer>();
		{
			String [] field=rawTask.split("values");
			//first parametr, second is rest
			//get index of Each value, appropriate to simpleResultSet
			List<String>columnName=rs.getColumnName(false);
			
			field[0]=field[0]+"values";
			String parametr=field[0].split("\\(")[1];
			parametr=parametr.split("\\)")[0];
			parametr=parametr.trim();
			for(String value:parametr.split(",")) {
				value=value.trim();
				//get name of value
				NameOfValue.add(columnName.indexOf(value));
				
			}
			
			//get value
			String[] secondPart=field[1].split(";");
	
			valueCharacter=secondPart[0];
			secondPart[0]="&&";
			field[1]=String.join("", secondPart);
			rawTask=String.join("", field);
		}
		
		final AtomicReference<List<String>> rowValue = new AtomicReference<>();
		StringBuilder vl=new StringBuilder();
		
		for(int i=0;;i++) {
			List<String> RVL = rs.getRowValue(false, i);
			
			 if(RVL == null) {
			 break;
			 }
			
			 rowValue.set(RVL);
			
			String[] replacement= {valueCharacter};
			
			NameOfValue.forEach((index)->{
				replacement[0]=replacement[0].replaceFirst(DatabaseCharacter.Value.toString(), makeQuotesAroundValue(rowValue.get().get(index)))
			;
			});
			if(i!=0) {
				//add devided character between each value
				vl.append(",");
			}
			vl.append(replacement[0]);

		}
		vl.append(";");
		rawTask=rawTask.replace("&&",vl.toString() );
		return rawTask;
			}
	
	private static String SelectNoResultSetQuery(String rawTask,SimpleResultSet rs) {
		//separate part multipleValue part, it is part where we do not known how many row will be have
		//each part have to be query by Charater
		String[] field=rawTask.split(DatabaseCharacter.MultipleValue.toString());
		
		if(field.length!=3&&field.length!=1) {
			throw new PaternRunnTimeException(String.format("Problem with multipleValue",null));
		}
		String MultipleValue = null;
		if(field.length==3) {
		MultipleValue=field[1];
		//process rest part of query without MultipleValue, multipleValuePart is
				//is replaced as character ^&
				field[1]="^&";
				rawTask=String.join("", field);
				
		}
		
		rawTask=MainSQL.ProcessRawTaskWithoutMultipleValue(rawTask, rs,false,0);		
		if(field.length==3) {
		//process multipleValue
		MultipleValue=MainSQL.ProcessMultipleValueRawTask(MultipleValue, rs);
		rawTask=rawTask.replace("^&", MultipleValue);}
		
		return rawTask;
	}
	private static String ProcessRawTaskWithoutMultipleValue(String rawTask,SimpleResultSet rs,boolean MultipleValues,int index) {
		//devide into field, each field represent one Value
		String []field=rawTask.split(DatabaseCharacter.Value.toString());
	
		for(int i=0;i<field.length-1;i++)
		{
			//separate Comparison Operator
			String oneValue=field[i];
			oneValue=oneValue.trim();
			if(oneValue.endsWith("=")||oneValue.endsWith("<")||oneValue.endsWith(">")) {
				oneValue=oneValue.substring(0,oneValue.length()-1);
				if(oneValue.endsWith("<")||oneValue.endsWith(">")||oneValue.endsWith("!")) {
					oneValue=oneValue.substring(0,oneValue.length()-1);
				}
			}
			else if(oneValue.endsWith("like")) {
				oneValue=oneValue.substring(0,oneValue.length()-4);
			}
			else {
				throw new PaternRunnTimeException(String.format("Problem with multipleValue, value %s was not recognie",oneValue));
			}
			
			//separate appropriate value name
			String []valueName=new StringBuilder(oneValue.trim()).reverse().toString().split("\\s",2);
			String appropriateValueName=new StringBuilder(valueName[0]).reverse().toString();
			String appropriateValue=rs.getValue(appropriateValueName, MultipleValues,index);
			if(appropriateValue==null) {
				throw new PaternRunnTimeException(String.format("Appropriate value for %s was not find",appropriateValueName));
			}
			rawTask=rawTask.replaceFirst(DatabaseCharacter.Value.toString(), makeQuotesAroundValue(appropriateValue));
		}
		return rawTask;
	}
	private static String ProcessMultipleValueRawTask(String rawTask,SimpleResultSet rs) {
		//separate part which have to be add into gap between each statement
		String MultipleValueSeparator;
		String []field=rawTask.split(DatabaseCharacter.MultipleValueSeparator.toString());


		if(field.length!=3&&field.length!=1) {
			throw new PaternRunnTimeException(String.format("Problem with multipleValue, multipleSeparator",null));
		}
		MultipleValueSeparator=field[1];
		field[1]="";
		rawTask=String.join("", field);
		StringBuilder MultipleValuePatern=new StringBuilder();
		for(int i=0;i<rs.lenght(true);i++) {
			MultipleValuePatern.append(
			MainSQL.ProcessRawTaskWithoutMultipleValue(rawTask, rs, true,i).
			replaceAll(MainSQL.DatabaseCharacter.TableName.toString(), rs.getValue(MainSQL.tableName.toString(), true,i)));
			MultipleValuePatern.append("\n");
			if(i<rs.lenght(true)-1) {
				MultipleValuePatern.append(MultipleValueSeparator);
				MultipleValuePatern.append("\n");
			}
		}
		return MultipleValuePatern.toString();
	}
	
	//class save query code and also instead if a insert or other type
	public static class Query{
		public TypeOFQuery TypeQuery;
		public String SQLQuery;
		public TypeOfDatabase databaseTyp;
		public Query(TypeOFQuery TypeQuery,TypeOfDatabase databaseTyp,String SQLQuery) {
			this.databaseTyp=databaseTyp;
			this.TypeQuery=TypeQuery;
			this.SQLQuery=SQLQuery;
		}
		/**Metod convert list of Query task to one collection
		 *metod respect order of each query and collection */
		public static Query[] ConvertListOFqueryToOne(List<Query[]> listOfQuery) {
			synchronized(listOfQuery) {
				AtomicInteger MaximumSize=new AtomicInteger(0);
				listOfQuery.forEach((Collection)->{
					MaximumSize.addAndGet(Collection.length);
					
				});
				Query[] x=new Query[MaximumSize.get()];
				MaximumSize.set(-1);
				listOfQuery.forEach((XX)->{
					for(Query Z:XX) {
						x[MaximumSize.incrementAndGet()]=Z;
					}
				});
				return x;
			}
		}
		
		private void setTypeQuery(TypeOFQuery typeQuery) {
			TypeQuery = typeQuery;
		}
		public void setSQLQuery(String sQLQuery) {
			SQLQuery = sQLQuery;
		}
		private void setDatabaseTyp(TypeOfDatabase databaseTyp) {
			this.databaseTyp = databaseTyp;
		}
		public TypeOFQuery getTypeQuery() {
			return TypeQuery;
		}
		public String getSQLQuery() {
			return SQLQuery;
		}
		public TypeOfDatabase getDatabaseTyp() {
			return databaseTyp;
		}
		public boolean isResultSet() {
			if(this.TypeQuery==TypeOFQuery.SelectQuery) {
				return true;
			}
			return false;
		}
		public static enum TypeOFQuery{
			
			InsertQuery("I"),SelectQuery("S"),WithoutResultSet("W");
			private String type;
			TypeOFQuery(String type){
				this.type=type;
			}
			public String toString() {
				return this.type;
			}
		}	
		}
	
	
	
	public static class PaternRunnTimeException extends RuntimeException{
		private String messageToAdd;
		public void ADDreason(String reason) {
			this.messageToAdd="\n"+reason;
		}
		public PaternRunnTimeException() {
			
		}
		public PaternRunnTimeException(String name) {
			super(name);
			
		}
		public String getMessage() {
			return super.getMessage()+this.messageToAdd;
		}
	}
	
	public static enum DatabaseCharacter{
		
		TableName("XXX"),Value("ZZZ"),
		BeginQuery("BBB"),
		EndQuery("CCC"),MultipleValue("%"),
		EndInsertValueQuerz("III"),
		MultipleValueSeparator("-F"),MultipleYYYvalue("YYY");
		private String type;
		DatabaseCharacter(String type){
			this.type=type;
		}
		public String toString() {
			return this.type;
		}
	}
	
	
	public static enum TypeOfDatabase{
		user("user"),UserQuickChat("UserQuickChat"),SQLite("SQLite"),Chat("Chat");
		private String nameOfDatabase;
		TypeOfDatabase(String type){
			this.nameOfDatabase=type;
		}
		public String toString() {
			return this.nameOfDatabase;
		}
	}
	public static class FileLoadException extends Exception{
		
		public FileLoadException() {
			
		}
		public FileLoadException(String message) {
			super(message);
		}
	}
	
	public static void ClosedStatement(Statement stm,ResultSet rs) {
		ThreadPoolingManagement.thread.Execute(()->{
			try {
				if(rs!=null) {
				rs.close();}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
			}
			try {
				if(stm!=null) {
				stm.close();}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
			}
			
		});
		
	}
	
	
}
