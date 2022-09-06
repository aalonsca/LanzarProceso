
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;

import com.clarify.cbo.Application;

import amdocs.bpm.BusinessProcess;
import amdocs.bpm.FocusKind;
import amdocs.bpm.ProcInstManager;
import amdocs.bpm.ProcInstManagerFactory;
import amdocs.bpm.ProcInstTraversal;
import amdocs.bpm.ProcessStatus;
import amdocs.bpm.RootCreateInfo;
import amdocs.bpm.Step;
import amdocs.bpm.ejb.ProcMgrSession;
import amdocs.bpm.ejb.ProcMgrSessionHome;
import amdocs.epi.session.EpiSessionContext;
import amdocs.epi.session.EpiSessionId;
import amdocs.epi.util.EpiCollections;
import amdocs.epi.util.ListSet;
import amdocs.epi.util.params.StringHolder;
import amdocs.ofc.process.BaseProcInst;
import amdocs.ofc.process.BaseStepInst;
import amdocs.ofc.process.Milestone;
import amdocs.ofc.process.MilestoneStep;
import amdocs.ofc.process.ProcessCreateInfo;
import amdocs.oms.apcore.ApItem;
import amdocs.oms.bpaproxy.OmsBpaCase;
import amdocs.oms.cih.mapping.CihOrderActionFilter;
import amdocs.oms.infra.IlSession;  // ofc.jar
import amdocs.oms.infra.domains.ActionTypeTP;
import amdocs.oms.infra.domains.ActionTypeTP.ActionType;
import amdocs.oms.infra.domains.BooleanValTP;
import amdocs.oms.infra.domains.BooleanValTP.BooleanVal;
import amdocs.oms.infra.domains.ConfigurationTP;
import amdocs.oms.infra.domains.LanguageCodeTP;
import amdocs.oms.infra.domains.OrderActionStatusTP;
import amdocs.oms.infra.domains.OrderActionStatusTP.OrderActionStatus;
import amdocs.oms.infra.domains.LanguageCodeTP.LanguageCode;
import amdocs.oms.infra.domains.MilestoneStatusTP;
import amdocs.oms.infra.domains.OrderActionParentRelationTP;
import amdocs.oms.infra.domains.OrderActionParentRelationTP.OrderActionParentRelation;
import amdocs.oms.infra.domains.OrderStatusTP;
import amdocs.oms.infra.domains.OrderUnitTypeTP;
import amdocs.oms.infra.domains.OrderStatusTP.OrderStatus;
import amdocs.oms.infra.domains.OrderUnitTypeTP.OrderUnitType;
import amdocs.oms.infra.domains.RecontactPeriodTP;
import amdocs.oms.infra.domains.RecontactPeriodTP.RecontactPeriod;
import amdocs.oms.infra.domains.dynamic.DynOrderModeTP;
import amdocs.oms.infra.domains.dynamic.DynOrderModeTP.DynOrderMode;
import amdocs.oms.infra.exceptions.OmsCreateRequestFailedException;
import amdocs.oms.infra.exceptions.OmsDataNotFoundException;
import amdocs.oms.infra.exceptions.OmsInvalidImplementationException;
import amdocs.oms.infra.exceptions.OmsInvalidUsageException;
import amdocs.oms.ocs.InitialProcessService;

import amdocs.oms.oem.OmOrder;  // core_domain.jar
import amdocs.oms.oem.OmOrderAction;
import amdocs.oms.oem.OmOrderHolder;
import amdocs.oms.osmancust.OmOrderAddition;
import amdocs.oms.pc.PcProcessDefinition;
//import amdocs.oms.pc.PcProcessDefinitionAug;
import amdocs.oms.rootset.AlRootSet;
import amdocs.oms.rootset.RootSetBase;
//import sun.org.mozilla.javascript.internal.Callable;

import com.amdocs.cih.services.oms.lib.StartOrderInput;
import com.amdocs.cih.services.oms.rvt.domain.OrderActionStatusRVT;
import com.amdocs.cih.services.oms.rvt.domain.OrderActionTypeRVT;
import com.amdocs.cih.services.oms.rvt.domain.OrderActionUserActionRVT;
import com.amdocs.cih.services.oms.rvt.domain.OrderModeRVT;
import com.amdocs.cih.services.oms.rvt.domain.OrderStatusRVT;
import com.amdocs.cih.services.oms.rvt.domain.OrderUserActionRVT;
import com.amdocs.cih.services.oms.rvt.referencetable.SalesChannelRVT;
import com.amdocs.cih.services.order.lib.Order;
import com.amdocs.cih.services.order.lib.OrderID;
import com.amdocs.cih.services.orderaction.lib.OrderAction;
import com.amdocs.cih.services.orderaction.lib.OrderActionData;
import com.amdocs.cih.services.orderaction.lib.OrderActionDetails;
import com.amdocs.cih.services.orderaction.lib.OrderActionID;
import com.amdocs.cih.services.order.lib.OrderDetails;
import com.amdocs.cih.services.order.lib.OrderHeader;
import com.amdocs.cih.services.party.lib.OrganizationID;
import com.amdocs.cih.services.party.lib.PersonHeader;
import com.amdocs.cih.services.party.lib.PersonID;
import com.amdocs.cih.services.subscription.lib.SubscriptionGroupID;
import com.amdocs.cih.common.datatypes.DynamicAttribute;
import com.amdocs.cih.common.datatypes.OrderActionUserAction;
import com.amdocs.cih.common.datatypes.OrderUserAction;
import com.clarify.cbo.Session;
import com.clarify.cbo.SqlExec;


/**
 * Lanza un proceso de APM y lo asocia al objeto de OMS correspondiente
 * @author NEORIS
 * @version: 1.0
 */
public class LanzaProceso {
	
	static String sNombreFich = "proceso.properties";
	static String sRutaIni = System.getProperty("DIR_EJEC", ".");
	
	private String strIDContract;
	private String strProcessName;
	private String strVersion;
	private String strObjidLanzado;

	// Variables del fichero properties
	private static String strURL_WLS = null;
	private static String strUser_WLS = null;
	private static String strPassword_WLS = null;
	private static String strDS_WLS_OMS = null;
	private static String strDS_WLS_PC = null;
	
	private static String strDebug = null;
	private static String strDBName = null;
	private static String strUser_DB_OMS = null;
	private static String strPassword_DB_OMS = null;
	private static String strUser_DB_PC = null;
	private static String strPassword_DB_PC = null;
	
	private static String strJdbc_DB = null;
	private static String strJdbc_Port = null;
	
	private static String ORACLE_DRIVER_CLFY = null;
	
	private final static String initialContextFactory = "weblogic.jndi.WLInitialContextFactory";
	
	private Object objref = null;
	private EpiSessionId tPooledId = null;
	private ProcMgrSession procSess = null;
	private ProcMgrSessionHome procMgrLocal = null;
	private IlSession session = null;

	private String strQueryProcessDef = "SELECT P.CID, P.PCVERSION_ID, P.PROCESS_MAP_ACTION, P.LINE_OF_BUSINESS, P.SALES_CHANNEL  FROM TBPROCESS P WHERE P.PROCESS_MAP_NAME = '%1'";
	private String strQueryOmsOrder = "SELECT T.CTDB_LAST_UPDATOR, T.ORDER_MODE, T.GROUP_ID, T.ROOT_CUSTOMER_ID, T.STATUS, T.OPPORTUNITY_ID, T.CURRENT_SALES_CHANNEL, T.DEALER_CODE, T.ADDRESS_ID, T.EXT_REF_NUM" // 10
									 + ", SERVICE_REQ_DATE, CREATION_DATE, APPLICATION_DATE, PROP_EXPIRY_DATE, CUST_ORDER_REF, CONTACT_ID, CUSTOMER_ID, DEPOSIT_ID, ORDER_UNIT_ID" // 20
									 + ", ORDER_UNIT_TYPE, PRIORITY, PROP_EXPIRY_DATE, RECONTACT_IN_MONTH, RECONTACT_IN_YEAR, RECONTACT_PERIOD, SOURCE_ORDER" // 30
									 + " FROM TBORDER T"
									 + " WHERE T.REFERENCE_NUMBER = '%1'";
	
	private String strQuerOmsOrderAction = "SELECT T.ORDER_UNIT_ID, T.PARENT_ORDER_UNIT, T.ORDER_ID, T.STATUS, T.ACTION_TYPE, T.DUE_DATE, T.AP_ID, T.PARENT_RELATION, T.MAIN_IND, T.AP_VERSION_ID" // 10"
									+ ", CONFIGURATION, LANGUAGE, PARENT_ASSOCIATED_ID, SERVICE_REQ_DATE, PRIORITY, APPLICATION_DATE" // 16
									+ ", ORG_OWNER_ID, CONTACT_ID, CUSTOMER_ID, REFERENCE_NUMBER, CREATOR_ID, APPLICATION_REF_ID"  //22
									+ ", ORDER_UNIT_TYPE, APPLICATION_NAME, EXT_REF_NUM, CREATION_DATE, SALES_CHANNEL, REASON_ID"	//28
									+ ", GROUP_ID, DYNAMIC_ATTRS, CUST_ORDER_REF, APPLICATION_REF_ID, EARLY_DATE, EXT_REF_REMARK"	//34
									+ ", ITEM_PARTITION_KEY, PRIVS_KEY, QSEQUENCE_NUM, QUANTITY, REASON_FREE_TEXT"	//39
									+ ", CUST_WILL_RCNT_IND, RECONTACT_PERIOD, RECONTACT_IN_MONTH, RECONTACT_IN_YEAR, REPLACED_OFFER_AP_ID"	//44
									+ ", REQUEST_LINE_ID"	//45
									+ " FROM TBORDER_ACTION T"
									+ " WHERE T.ORDER_ID = '%1'"
									+ " ORDER BY T.ORDER_UNIT_ID";
	
	private String strQueryBPM = "SELECT ID, BYTECODE, NAME, VERSION, OBJID, DEFINITION, STATUS, DEFINITION_VERSION, FOCUS_KIND, FOCUS_TYPE " // 10
								 + " FROM TABLE_BPM_PROCESS "
								 + " WHERE NAME = '%1'"
								 + " AND VERSION = '%2'";
			
	
	protected String salesChannel = null;
	protected static Application clfyAppOms = null;
	protected static Session clfySessionOms = null;
	protected static Application clfyAppPC = null;
	protected static Session clfySessionPC = null;
	protected static Connection oraConexionOMS = null;
	protected static Connection oraConexionPC = null;
	protected static SqlExec clfySqlExecOms = null;
	protected static SqlExec clfySqlExecPC = null;
	
	protected OmOrder order = null;
	protected Order inputOrder = null;
	protected StartOrderInput startOrderInput = null;
	protected PcProcessDefinition processDef = null;
	
	protected static boolean DebugMode = false;
	

	/**
	 * Funcion de entrada
	 * @param args
	 *   0 -> id de la orden
	 *   1 -> name del proceso
	 *   2 -> version del proceso
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		//solo es necesario para permitir lanzar procesos desde linea de comandos
		if (args.length == 0) {
			throw new Exception("Error. Son necesarios parametros de entrada: TBORDER.REFERENCE_ID BPM_PROCESS.NAME BPM_PROCESS.VERSION");
		}
		
		LanzaProceso proceso = new LanzaProceso();

		proceso.setStrIDContract(args[0]);
		proceso.setStrProcessName(args[1]);
		proceso.setStrVersion(args[2]);
		
		//leemos la configuracion
		proceso.getConfiguration();
		
		// Lanzamos el proceso
		if (proceso.execProcess() < 0) {
			throw new Exception("Error. No se ha podido generar el proceso");
		};

	}

	/**
	 * Constructor
	 */
	public LanzaProceso () {
	
	}

	/**
	 * Abre las conexiones con bbdd
	 * @param intTipo
	 * 	0 -> conexion clfySession para OMS, solo para consultas
	 * 10 -> conexion clfySession para PC, solo para consultas
	 * 11 -> conexion clfySession para OMS y para PC, solo consultas
	 *  1 -> conexion Oracle, para ejecucion procedimientos almacenados
	 *  2 -> conexion clfySession para OMS, solo para consultas y conexion Oracle, para procedimientos almacenados
	 * 20 -> conexion clfySession para PC, solo para consultas y conexion Oracle, para procedimientos almacenados
	 * 21 -> conexion clfySession para OMS y para PC, solo consultas y conexion Oracle, para procedimientos almacenados
	 * @throws SQLException
	 */
	
	/**
	 * @deprecated
	 * 
	 */
	private static void openDBConnection(Integer intTipo) throws SQLException {

		if (getDebugMode()) {
			System.out.println("------------------------------------------------------------------------------------------");
			System.out.println("Entering openDBConnection with value : " + intTipo);
		}

		
		if (intTipo == 0 || intTipo == 2 || intTipo == 11 || intTipo == 21) {
			// Definimos las variables de conexion
			clfyAppOms = new Application();
			clfySessionOms = clfyAppOms.getGlobalSession();
	
			try {
				clfyAppOms.setAlternateDBInfo(1, strJdbc_DB, strDBName, strJdbc_Port, strUser_DB_OMS, strJdbc_DB, strDS_WLS_OMS);

				
			} catch (Exception e) {
	
				try {
					strDBName += ".world";
					clfyAppOms.setAlternateDBInfo(1, strJdbc_DB, strDBName, strJdbc_Port, strUser_DB_OMS, strJdbc_DB, strDS_WLS_OMS);
					
				} catch (Exception e1) {
					if (getDebugMode()) {
						System.out.println("------------------------------------------------------------------------------------------");
						System.out.println("ERROR Opening OMS DB connection FAILED: " + e.toString());
					}
					throw new SQLException("Error al abrir la conexion OMS con la bbdd " + strDBName + ":" + strJdbc_Port);
				}
						
			}
	
			clfySessionOms.predefinedLogin();
			if (getDebugMode()) {
				System.out.println("------------------------------------------------------------------------------------------");
				System.out.println("Clfy OMS connection opened");
			}
			
			clfySqlExecOms = (SqlExec) clfySessionOms.createObj("Clarify.SqlExec");
		}

		if (intTipo == 10 || intTipo == 11 || intTipo == 20 || intTipo == 21) {
			// Definimos las variables de conexion
			clfyAppPC = new Application();
			clfySessionPC = clfyAppPC.getGlobalSession();
	
			try {
				clfyAppPC.setAlternateDBInfo(1, strJdbc_DB, strDBName, strJdbc_Port, strUser_DB_OMS, strJdbc_DB, strDS_WLS_PC);

			} catch (Exception e) {
	
				try {
					strDBName += ".world";
					clfyAppPC.setAlternateDBInfo(1, strJdbc_DB, strDBName, strJdbc_Port, strUser_DB_OMS, strJdbc_DB, strDS_WLS_PC);
					
				} catch (Exception e1) {
					if (getDebugMode()) {
						System.out.println("------------------------------------------------------------------------------------------");
						System.out.println("ERROR Opening PC DB connection FAILED: " + e.toString());
					}
					throw new SQLException("Error al abrir la conexion PC con la bbdd " + strDBName + ":" + strJdbc_Port);
				}
						
			}
	
			clfySessionPC.predefinedLogin();
			if (getDebugMode()) {
				System.out.println("------------------------------------------------------------------------------------------");
				System.out.println("Clfy PC connection opened");
			}
			
			clfySqlExecPC = (SqlExec) clfySessionPC.createObj("Clarify.SqlExec");
		}
		
		
		if (intTipo == 2 || intTipo == 20 || intTipo == 21) {
			//abrimos una conexion de Oracle para permitir lanzar procesos 
			try {
				DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
				ORACLE_DRIVER_CLFY = "jdbc:oracle:thin:@" + strJdbc_DB + ":" + strJdbc_Port + ":" + strDBName;
				oraConexionOMS = DriverManager.getConnection(ORACLE_DRIVER_CLFY, strUser_DB_OMS, strPassword_DB_OMS);

				if (getDebugMode()) {
					System.out.println("------------------------------------------------------------------------------------------");
					System.out.println("ORACLE OMS connection opened");
				}

			} catch (SQLException sqle) {
				
				if (getDebugMode()) {
					System.out.println("------------------------------------------------------------------------------------------");
					System.out.println("ERROR Opening Oracle OMS DB connection FAILED: " + sqle.toString());
				}
				
				throw new SQLException("Error al abrir la conexion Oracle con la bbdd " + strDBName + ":" + strJdbc_Port);
			}
		}
	}
	

	/**
	 * Abre las conexiones con bbdd
	 * @param strServicio
	 *   OMS  -> conexion con el usuario / password para OMS
	 *   PC   -> conexion con el usuario / password para PC
	 *   BOTH -> ambas conexiones
	 * @throws SQLException
	 */
	private static void openDBConnection(String strServicio) throws SQLException {

		if (getDebugMode()) {
			System.out.println("------------------------------------------------------------------------------------------");
			System.out.println("Entering openDBConnection with value : " + strServicio);
		}

		
		if (!"".equals(strServicio) && ("OMS".equals(strServicio) || "BOTH".equals(strServicio))) {

			//abrimos una conexion de Oracle para permitir lanzar procesos 
			try {
				DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
				ORACLE_DRIVER_CLFY = "jdbc:oracle:thin:@" + strJdbc_DB + ":" + strJdbc_Port + ":" + strDBName;
				oraConexionOMS = DriverManager.getConnection(ORACLE_DRIVER_CLFY, strUser_DB_OMS, strPassword_DB_OMS);

				if (getDebugMode()) {
					System.out.println("------------------------------------------------------------------------------------------");
					System.out.println("ORACLE OMS connection opened");
				}

			} catch (SQLException sqle) {
				
				if (getDebugMode()) {
					System.out.println("------------------------------------------------------------------------------------------");
					System.out.println("ERROR Opening Oracle OMS DB connection FAILED: " + sqle.toString());
				}
				
				throw new SQLException("Error al abrir la conexion Oracle con la bbdd " + strDBName + ":" + strJdbc_Port);
			}
		}

		if (!"".equals(strServicio) && ("PC".equals(strServicio) || "BOTH".equals(strServicio))) {

			//abrimos una conexion de Oracle para permitir lanzar procesos 
			try {
				DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
				ORACLE_DRIVER_CLFY = "jdbc:oracle:thin:@" + strJdbc_DB + ":" + strJdbc_Port + ":" + strDBName;
				oraConexionPC = DriverManager.getConnection(ORACLE_DRIVER_CLFY, strUser_DB_PC, strPassword_DB_PC);

				if (getDebugMode()) {
					System.out.println("------------------------------------------------------------------------------------------");
					System.out.println("ORACLE PC connection opened");
				}

			} catch (SQLException sqle) {
				
				if (getDebugMode()) {
					System.out.println("------------------------------------------------------------------------------------------");
					System.out.println("ERROR Opening Oracle OMS DB connection FAILED: " + sqle.toString());
				}
				
				throw new SQLException("Error al abrir la conexion Oracle con la bbdd " + strDBName + ":" + strJdbc_Port);
			}
		}
		
		
	}	
	
	/**
	 * Cierra las conexiones abiertas
	 * @throws SQLException
	 */
	private static void closeDBConnection() throws SQLException {


		if (getDebugMode()) {
			System.out.println("------------------------------------------------------------------------------------------");
			System.out.println("Entering closeDBConnection");
		}

		//Si esta abierta la sesion de clfy, la cerramos
		if (clfySessionOms != null) {
			clfySessionOms.release();
		}
		
		if (clfySessionPC != null) {
			clfySessionPC.release();
		}

		//Si esta abierta la sesion de Oracle, la cerramos
		if (oraConexionOMS != null) { 
			try {
				oraConexionOMS.close();
			} catch (SQLException sqle) {
				if (getDebugMode()) {
					System.out.println("------------------------------------------------------------------------------------------");
					System.out.println("ERROR Closing Oracle OMS DB connection FAILED: " + sqle.toString());
				}
					
				throw new SQLException("Error al cerrar la conexion con la bbdd " + strDBName + ":" + strJdbc_Port);
			}
		}

		if (oraConexionPC != null) { 
			try {
				oraConexionPC.close();
			} catch (SQLException sqle) {
				if (getDebugMode()) {
					System.out.println("------------------------------------------------------------------------------------------");
					System.out.println("ERROR Closing Oracle PC DB connection FAILED: " + sqle.toString());
				}
					
				throw new SQLException("Error al cerrar la conexion con la bbdd " + strDBName + ":" + strJdbc_Port);
			}
		}
		
		if (getDebugMode()) {
			System.out.println("Connections closed");
			System.out.println("------------------------------------------------------------------------------------------");

		}
		
		
	}
		

	/**
	 * Recoge la configuracion de un fichero .properties
	 * @throws IOException
	 */
	private void getConfiguration() 
			throws IOException, Exception {
		
		try {
			//Obtenemos los valores del entorno de un fichero .properties
			Properties properties = new Properties();
			
			//Abrimos el fichero
			File fProp = new File(sRutaIni, sNombreFich);
			
			if (!fProp.canRead()) {
				throw new IOException("No existe el fichero " + fProp.getAbsolutePath());
			}
			
			// Cargamos el .properties y lo cerramos
			FileInputStream fiProp = new FileInputStream(fProp);
			properties.load(fiProp);
			fiProp.close();
			
			strURL_WLS = properties.getProperty("WLS_URL");
			strUser_WLS = properties.getProperty("WLS_USER");
			strPassword_WLS = properties.getProperty("WLS_PASS");
			strDS_WLS_OMS = properties.getProperty("WLS_DS_OMS");
			strDS_WLS_PC =  properties.getProperty("WLS_DS_PC");
			strDebug = properties.getProperty("DEBUG");
	
			strDBName = properties.getProperty("DB");
			strUser_DB_OMS = properties.getProperty("DB_USER_OMS");;
			strPassword_DB_OMS = properties.getProperty("DB_PASS_OMS");
			strUser_DB_PC = properties.getProperty("DB_USER_PC");;
			strPassword_DB_PC = properties.getProperty("DB_PASS_PC");
			strJdbc_DB = properties.getProperty("DB_JDBC");
			strJdbc_Port = properties.getProperty("DB_PORT");

			
			if ((!"".equals(strDebug)) && "1".equals(strDebug)) {
				
				setDebugMode(true);
				
				System.out.println("URL:" + strURL_WLS);
				System.out.println("USER:" + strUser_WLS);
				//System.out.println("PASS:" + strPassword_WLS); // No mostramos la password en el log
				System.out.println("DATASOURCE OMS:" + strDS_WLS_OMS);
				System.out.println("DATASOURCE PC:" + strDS_WLS_PC);
				System.out.println("----");
				System.out.println("DB:" + strDBName);			
				System.out.println("USER_OMS:" + strUser_DB_OMS);
				System.out.println("USER_PC:" + strUser_DB_PC);
				//System.out.println("PASS" + strPassword_DB); //No mostramos la password en el log
				System.out.println("JDBC:" + strJdbc_DB);
				System.out.println("PORT:" + strJdbc_Port);
			}
		}			
		catch(Exception e) {
			System.out.println("------------------------------------------------------------------------------------------");
			System.out.println("ERROR Opening .properties FAILED: " + e.toString());
			
			throw new Exception("Error al leer el fichero .properties");

		}

	}	
	

	/**
	 * Conecta el proceso con la instacia de WL ejecutandose
	 * @return 0 -> Conexion OK
	 *        -1 -> Fallo en la conexion
	 */
	private int prepareConnWL() {
		
		if (getDebugMode()) {
			System.out.println("------------------------------------------------------------------------------------------");
			System.out.println("Entering prepareConnWL");
		}

		
		// Generamos la conexions al WL
		try {
			// Generamos una configuracion de properties para la conexion
			Properties propertiesCon = new Properties();
			propertiesCon.put(InitialContext.INITIAL_CONTEXT_FACTORY, initialContextFactory);
			propertiesCon.put(InitialContext.PROVIDER_URL, strURL_WLS);
			if (!"".equals(strUser_WLS) && strUser_WLS != null) {
				propertiesCon.put(InitialContext.SECURITY_PRINCIPAL, strUser_WLS);
				propertiesCon.put(InitialContext.SECURITY_CREDENTIALS, strPassword_WLS == null ? "" : LanzaProceso.strPassword_WLS);
			}

			if (getDebugMode()) {
				System.out.println("Properties created: ");
				System.out.println(propertiesCon.toString());
			}
			
			
			InitialContext context = new InitialContext(propertiesCon);
			objref = context.lookup("/omsserver_weblogic/amdocs/bpm/ejb/ProcMgrSession");
			
			procMgrLocal = (ProcMgrSessionHome) PortableRemoteObject.narrow(objref, ProcMgrSessionHome.class);
			procSess = procMgrLocal.create();			
			tPooledId = procSess.createSession();

			if (getDebugMode()) {
				System.out.println("Session created: ");
				System.out.println(tPooledId.toString());
			}
			
			return 0;
			
		}catch(Exception e){
			
			if (getDebugMode()) {
				System.out.println("------------------------------------------------------------------------------------------");
				System.out.println("ERROR APM Session initialization FAILED: " + e.toString());
			}
			
			if (e.toString().contains("javax.naming.NameNotFoundException")) {
				
			}
			return -1;
		}
		
	}
	
	
	/**
	 * Lanzamiento proceso. Genera todos los objetos necesarios recuperandolos de BBDD
	 * @return 0 --> Lanzamiento OK
	 *        -1 --> Error en el lanzamiento
	 */
	private int execProcess() {

		/* PARA LAS PRUEBAS CON WL CAIDO
		boolean commit = true;
		*/
		
		if (getDebugMode()) {
			System.out.println("------------------------------------------------------------------------------------------");
			System.out.println("Entering execProcess ");
		}
		
		/* PARA LAS PRUEBAS CON WL CAIDO
		if (prepareConnWL() < 0) {

			if (getDebugMode()) {
				System.out.println("ERROR WL connection failed. Exiting...");
			}

			return -1;
		}
		*/
		 
		
		// Generamos los objetos necesarios para lanzar el proceso asociado a la orden
		try {
	          String caseId = null;
	          StringHolder caseIdH = new StringHolder(caseId);
	          	  
	          /* PARA LAS PRUEBAS CON WL CAIDO
	          session = (IlSession) EpiSessionContext.findSession(tPooledId);	          
	          session.startTransaction();  // Llamamos asi, aunque es static, para garantizar que se use la sesion a la que nos hemos conectado
	          */
			  if (getDebugMode()) {
				System.out.println("Transaction started: ");
				/* PARA LAS PRUEBAS CON WL CAIDO
				System.out.println(session.toString());
				*/
			  }

	          
      		// generamos las conexiones
      		LanzaProceso.openDBConnection("BOTH");
      		//if (("".equals(clfySessionOms.getSessionTag())) && ("".equals(clfySessionPC.getSessionTag()))) {
      		if (oraConexionPC == null || oraConexionOMS == null) {
      			// Si no hemos podido abrir las sesiones, salimos del procedimiento. 
				if (getDebugMode()) {
					System.out.println("ERROR checking DB connections. Exiting");
				}
    			return -1;
      		}
      		
			//Recuperamos la info del proceso a lanzar
      		//this.setProcessDef(getProcessDetails(clfySqlExecPC, getStrProcessName()));
      		this.setProcessDef(getProcessDetails(oraConexionPC, getStrProcessName()));  
      		if (this.getProcessDef() == null) {
				if (getDebugMode()) {
					System.out.println("------------------------------------------------------------------------------------------");
					System.out.println("ERROR cannot get process definition. Exiting");
				}

      			return -1;
      		}
			

      		//recuperamos los valores de la orden a partir del id
			try {
				
				 if (getDebugMode()) {
					System.out.println("Creating order objects... ");
				 }
				
	      		//generamos el objeto OmOrder
	      		//this.order = getOmOrderDetails(clfySqlExecOms, getStrIDContract());
	      		this.order = getOmOrderDetails(oraConexionOMS, getStrIDContract());
	      		this.inputOrder = getInputOrderDetails(this.order);
	      		
	      		//this.startOrderInput = getStartOrderInput(clfySqlExecOms, this.order.getsourceOrder());
	      		this.startOrderInput = getStartOrderInput(oraConexionOMS, this.order.getsourceOrder());

				 if (getDebugMode()) {
					System.out.println("Order objects created");
				 }

				String custID = this.order.getrootCustomerId();
						
				//Nos quedamos con la ultima OA (se obtienen ordenadas)
				ListSet oma = this.order.getOMORDER_ACTION_CHILDs();					
				OmOrderAction existingOA = (OmOrderAction) oma.get(oma.size());
						
				//Recorremos los distintos Order Action que se hayan recuperado
				for (int i = 0; i < startOrderInput.getOrderActionsData().length; i++){

					if (getDebugMode()) {
						System.out.println("Loop over order actions");
					}

					OrderActionData oaData = this.startOrderInput.getOrderActionsData()[i];
					OrderActionDetails oaDetails = oaData.getOrderActionInfo().getOrderActionDetails();
					String actionType = oaDetails.getActionType().getValueAsString();
					ActionTypeTP.ActionType oaType = (ActionTypeTP.ActionType)ActionTypeTP.def.findByCodeNoAutoCreate(actionType);
							
					String lineOfBusiness = null;
					if (oaDetails.getLineOfBusiness() != null)
						lineOfBusiness = oaDetails.getLineOfBusiness().getValueAsString();
							
					Date serviceRequiredDate = oaDetails.getServiceRequireDate();

					if (getDebugMode()) {
						System.out.println("Creating creationInfo object");
					}

					InitialProcessService.OrderActionCreationInfo creationInfo = new InitialProcessService.OrderActionCreationInfo(oaType, this.order.getcurrentSalesChannel(), lineOfBusiness, serviceRequiredDate);

					DynamicAttribute[] dynamicAttrs = oaData.getDynamicAttributes();
					if (dynamicAttrs != null) 
						creationInfo.dynamicAttributes = CihOrderActionFilter.createDynamicAttributesMap(dynamicAttrs);


					if (getDebugMode()) {
						System.out.println("Starting and executing process");
					}

					//Lanzamos el proceso
					//caseIdH = startProcess(clfySqlExecOms, creationInfo, custID, existingOA, oaData);
					caseIdH = startProcess(oraConexionOMS, creationInfo, custID, existingOA, oaData);
							
					// y recuperamos el caseid generado
					if (caseIdH.value != null) {
								
						caseId = caseIdH.value;
						this.setStrObjidLanzado(caseId);
								
						if (getDebugMode()) {
							System.out.println("Launched process id: " + caseId);
						}

						//Guardamos el objid recuperado en un fichero de texto
						BufferedWriter bw = null;
						try {
							File fichero = new File(sRutaIni + "/out", this.getStrIDContract() + "_" + this.getStrVersion());
							bw = new BufferedWriter(new FileWriter(fichero));
							bw.write(this.getStrObjidLanzado());

						} catch (IOException e) {
							if (getDebugMode()) {
								System.out.println("ERROR handling output file : " + e.toString());
							}
								
						} finally {
							try {
								bw.close();
							} catch (Exception e) {}
						}
					}
				}
			} catch (Exception e) {
	
				if (getDebugMode()) {
					System.out.println("ERROR generic exception creating session objects" );
					System.out.println(e.toString());
				}
				return -1;
			}
				
				
		} catch (Exception e) {
			if (getDebugMode()) {
				System.out.println("ERROR al generar los objetos de conexion al APM");
				System.out.println(e.toString());
			}
			return -1;
			
		} finally {
			
			try {
				// cerramos las conexiones
				LanzaProceso.closeDBConnection();
				
	      		
				/* PARA PRUEBAS CON WL CAIDO
				session.endTransaction(commit);
				clfyAppOms.release();
				clfyAppPC.release();
				session.close();
				*/
				
			}catch (Exception e1) {
				if (getDebugMode()) {
					System.out.println("ERROR closing connections");
					System.out.println(e1.toString());
				}
				return -1;
			}
		}
		
		return 0;
			
	}	

	/**
	 * Inicia los subprocesos asociados a un proceso
	 * @param processInstance
	 * @param traversal
	 * @param createInfo
	 * @return 0 -> OK
	 *        <0 -> Error
	 */
	private int initSubProcesses(BaseProcInst processInstance, ProcInstTraversal traversal, ProcessCreateInfo createInfo)
	{

		int iControl = 0;

		if (getDebugMode()) {
			System.out.println("------------------------------------------------------------------------------------------");
			System.out.println("Starting subprocesses");
		}
		

		List subProcesses = processInstance.getSubprocInsts(traversal);
		
		for (Iterator i = subProcesses.iterator(); i.hasNext();)
		{
			BaseProcInst subProcInst = (BaseProcInst)i.next();
			
			try {
				OmOrderAction oa = (OmOrderAction) createInfo.getProcessObject();
				RootSetBase rsb = (RootSetBase) subProcInst.getRootContext();
				AlRootSet mr = (AlRootSet) subProcInst.getRootProcInst().getRootContext();
			
				rsb.setmainRoot(mr);
				rsb.setorderAction(oa);
				if (rsb instanceof AlRootSet) 
					((AlRootSet) rsb).setCaseDoneVal("N");
			
			}catch (Exception e) {
				return -1;
			}
			
			// Llamamos a la misma funcion con el valor de los subprocesos
			iControl = initSubProcesses(subProcInst, traversal, createInfo);
			if (iControl != 0) 
				return -1;
			
		}
		return 0;
	}

	/**
	 * Lanza el proceso asociado a una order action
	 * @param sqlQuery
	 * @param creationInfo
	 * @param customerID
	 * @param existingOA
	 * @param orderActionData
	 * @return
	 * @throws OmsDataNotFoundException
	 * @throws OmsCreateRequestFailedException
	 * @throws OmsInvalidImplementationException
	 * @throws OmsInvalidUsageException
	 */
	
	/** @deprecated
	 */
	protected StringHolder startProcess(SqlExec sqlQuery, InitialProcessService.OrderActionCreationInfo creationInfo, String customerID, OmOrderAction existingOA, OrderActionData orderActionData)
		    throws Exception
	{

		String caseId = null;
		StringHolder caseIdH = new StringHolder(caseId);
		
		if (getDebugMode()) {
			System.out.println("------------------------------------------------------------------------------------------");
			System.out.println("Starting process");
		}
		
		
		try {

			// Generamos la consulta con los datos del proceso a ejecutar
			sqlQuery.execute(strQueryBPM.replace("%1", getStrProcessName()).replace("%2", getStrVersion()));
			
			if (sqlQuery.getRowCount() > 0) {
			
				if (getDebugMode()) {
					System.out.println("Creating objects to launch the process");
				}

				 //createCase(procName, existingOA, true, true, caseIdH)
				 ProcessCreateInfo createInfo = new ProcessCreateInfo();
				 createInfo.setBusinessProcessName(this.getStrProcessName());
				 createInfo.setVersion(getStrVersion());
				 createInfo.setIsInitialProcess();
				 createInfo.setUseLatest(true);
				 createInfo.setFirstInQuantity(false);
				 createInfo.setStart(true);

				if (getDebugMode()) {
					System.out.println("ProcessCreateInfo:" + createInfo.toString());
				}

				 
				 //OmsBpaCase cse = (OmsBpaCase)ActivityManager.getInstance().createProcessInstance(createInfo);
				 BusinessProcess bproc = null;
				 bproc.setId((String)sqlQuery.getValue(1, 1));
				 bproc.setName((String)sqlQuery.getValue(1, 3));				
				 bproc.setStatus(ProcessStatus.valueOf((Integer)sqlQuery.getValue(1, 7)));
				 bproc.setDefinitionVersion((Integer)sqlQuery.getValue(1, 8));
				 bproc.setFocusKind(FocusKind.valueOf((Integer)sqlQuery.getValue(1, 9)));
				 bproc.setFocusType((String)sqlQuery.getValue(1, 10));
				 //bproc.setDynamicInstantiation(true);
				 bproc.setSubclassingPolicyName("default");
				 
				if (getDebugMode()) {
					System.out.println("BusinessProcess:" + bproc.toString());
				}

				 
				 RootCreateInfo info = new RootCreateInfo();
				 info.setMakeBeginStepInstAvailable(false);
				 
				 ProcInstManager piMgr = ProcInstManagerFactory.get();
				 
				if (getDebugMode()) {
						System.out.println("ProcInstManager:" + piMgr.toString());
				}

				 
				 BaseProcInst procInst = null;				 
				 // Lanzamos el proceso
				 procInst = (BaseProcInst) piMgr.createRootProcInst(bproc, null, info);

				 if (procInst != null) {
					if (getDebugMode()) {
						System.out.println("BaseProcInst:" + procInst.toString());
					}
				 
				 
					//Asignamos el proceso a la OA
					existingOA.assignProcessInstanceId(procInst.getId(), true);
					createInfo.setProcessObject(existingOA);
					
					OmsBpaCase caseObj = (OmsBpaCase) procInst;
					if (getDebugMode()) {
						System.out.println("OmsBpaCase:" + caseObj.toString());
					}
					
					caseObj.setLastUpdatedBy(session.getLogicalServerName());
				 
					ProcInstTraversal traversal = piMgr.createProcInstTraversal(procInst);
					if (getDebugMode()) {
						System.out.println("ProcInstTraversal:" + traversal.toString());
					}
				 
					
					if (initSubProcesses(procInst, traversal, createInfo) != 0) {
						if (getDebugMode()) {
							System.out.println("ERROR initializing subprocess");
						}
						return caseIdH;	
					};


					BaseStepInst stepInst = (BaseStepInst) procInst.getBeginStepInst();
					stepInst.setReferenceText(stepInst.getId());
				 
					if (getDebugMode()) {
						System.out.println("BaseStepInst:" + stepInst.toString());
					}
					
					
					if (stepInst.getStep() instanceof MilestoneStep ) {
						Step st = stepInst.getStep();
					 
						Milestone m = null;
						m.setStepInstanceId(stepInst.getId());
						m.setProcessObject(existingOA);
						m.setMilestoneStatus(MilestoneStatusTP.IN);
						m.setMilestoneType(((MilestoneStep) st).getMilestoneType());
						m.setIndex(0);
						m.setConfiguration(existingOA.getConfiguration());
					 
						m.initiateAchievementDate(null);
						m.initiateReachingDate(null);
						m.initiateEarlyDate(null);
						m.setDueDate(null);
					 
						existingOA.addMilestone(m);
						if (getDebugMode()) {
							System.out.println("Milestone:" + m.toString());
						}
						
					}
				 
					try {
						 procInst.startRootProcInst(); 
						 existingOA.setIsProcessStarted(true);
						 
						 caseIdH.value = caseObj.getId();
						 
						 if (getDebugMode()) {
							System.out.println("CaseId generated: " + caseObj.getId());
						}

					}catch (Exception e) {
						 if (getDebugMode()) {
							 System.out.println("ERROR Exception: " + e.toString());
						 }

						 return caseIdH;
					 }
					 
				 }else{

					if (getDebugMode()) {
						System.out.println("ERROR ProcInst not created");
					}

					 
				 }
			}
		}catch(Exception e) {
			 if (getDebugMode()) {
				 System.out.println("ERROR Generic Exception: " + e.toString());
			 }

			 return caseIdH;
		}
		finally {
			sqlQuery.release();
		}
		
		return caseIdH;
	}	


	/** 
	 * Lanza el proceso asociado a una order action
	 * @param conn
	 * @param creationInfo
	 * @param customerID
	 * @param existingOA
	 * @param orderActionData
	 * @return
	 * @throws Exception
	 */
	
	protected StringHolder startProcess(Connection conn, InitialProcessService.OrderActionCreationInfo creationInfo, String customerID, OmOrderAction existingOA, OrderActionData orderActionData)
		    throws Exception
	{

		String caseId = null;
		StringHolder caseIdH = new StringHolder(caseId);

		CallableStatement sqlQuery = null;
		ResultSet result = null; 
		
		if (getDebugMode()) {
			System.out.println("------------------------------------------------------------------------------------------");
			System.out.println("Starting process");
		}
		
		
		try {

			if (conn == null) {
				LanzaProceso.openDBConnection("OMS"); // abrimos la sesion para PC
			}
			
			// Generamos la consulta con los datos del proceso a ejecutar
			sqlQuery = (CallableStatement) conn.prepareStatement(strQueryBPM.replace("%1", getStrProcessName().replace("%2", getStrVersion())));			
			result = sqlQuery.executeQuery();
			
			if (result.getFetchSize() > 0) {
			
				if (getDebugMode()) {
					System.out.println("Creating objects to launch the process");
				}

				 //createCase(procName, existingOA, true, true, caseIdH)
				 ProcessCreateInfo createInfo = new ProcessCreateInfo();
				 createInfo.setBusinessProcessName(this.getStrProcessName());
				 createInfo.setVersion(getStrVersion());
				 createInfo.setIsInitialProcess();
				 createInfo.setUseLatest(true);
				 createInfo.setFirstInQuantity(false);
				 createInfo.setStart(true);

				if (getDebugMode()) {
					System.out.println("ProcessCreateInfo:" + createInfo.toString());
				}

				 
				 //OmsBpaCase cse = (OmsBpaCase)ActivityManager.getInstance().createProcessInstance(createInfo);
				 BusinessProcess bproc = null;
				 bproc.setId(result.getString(1));
				 bproc.setName(result.getString(3));				
				 bproc.setStatus(ProcessStatus.valueOf(result.getInt(7)));
				 bproc.setDefinitionVersion(result.getInt(8));
				 bproc.setFocusKind(FocusKind.valueOf(result.getInt(9)));
				 bproc.setFocusType(result.getString(10));
				 //bproc.setDynamicInstantiation(true);
				 bproc.setSubclassingPolicyName("default");
				 
				if (getDebugMode()) {
					System.out.println("BusinessProcess:" + bproc.toString());
				}

				 
				 RootCreateInfo info = new RootCreateInfo();
				 info.setMakeBeginStepInstAvailable(false);
				 
				 ProcInstManager piMgr = ProcInstManagerFactory.get();
				 
				if (getDebugMode()) {
						System.out.println("ProcInstManager:" + piMgr.toString());
				}

				 
				 BaseProcInst procInst = null;				 
				 // Lanzamos el proceso
				 procInst = (BaseProcInst) piMgr.createRootProcInst(bproc, null, info);

				 if (procInst != null) {
					if (getDebugMode()) {
						System.out.println("BaseProcInst:" + procInst.toString());
					}
				 
				 
					//Asignamos el proceso a la OA
					existingOA.assignProcessInstanceId(procInst.getId(), true);
					createInfo.setProcessObject(existingOA);
					
					OmsBpaCase caseObj = (OmsBpaCase) procInst;
					if (getDebugMode()) {
						System.out.println("OmsBpaCase:" + caseObj.toString());
					}
					
					caseObj.setLastUpdatedBy(session.getLogicalServerName());
				 
					ProcInstTraversal traversal = piMgr.createProcInstTraversal(procInst);
					if (getDebugMode()) {
						System.out.println("ProcInstTraversal:" + traversal.toString());
					}
				 
					
					if (initSubProcesses(procInst, traversal, createInfo) != 0) {
						if (getDebugMode()) {
							System.out.println("ERROR initializing subprocess");
						}
						return caseIdH;	
					};


					BaseStepInst stepInst = (BaseStepInst) procInst.getBeginStepInst();
					stepInst.setReferenceText(stepInst.getId());
				 
					if (getDebugMode()) {
						System.out.println("BaseStepInst:" + stepInst.toString());
					}
					
					
					if (stepInst.getStep() instanceof MilestoneStep ) {
						Step st = stepInst.getStep();
					 
						Milestone m = null;
						m.setStepInstanceId(stepInst.getId());
						m.setProcessObject(existingOA);
						m.setMilestoneStatus(MilestoneStatusTP.IN);
						m.setMilestoneType(((MilestoneStep) st).getMilestoneType());
						m.setIndex(0);
						m.setConfiguration(existingOA.getConfiguration());
					 
						m.initiateAchievementDate(null);
						m.initiateReachingDate(null);
						m.initiateEarlyDate(null);
						m.setDueDate(null);
					 
						existingOA.addMilestone(m);
						if (getDebugMode()) {
							System.out.println("Milestone:" + m.toString());
						}
						
					}
				 
					try {
						 procInst.startRootProcInst(); 
						 existingOA.setIsProcessStarted(true);
						 
						 caseIdH.value = caseObj.getId();
						 
						 if (getDebugMode()) {
							System.out.println("CaseId generated: " + caseObj.getId());
						}

					}catch (Exception e) {
						 if (getDebugMode()) {
							 System.out.println("ERROR Exception: " + e.toString());
						 }

						 return caseIdH;
					 }
					 
				 }else{

					if (getDebugMode()) {
						System.out.println("ERROR ProcInst not created");
					}

					 
				 }
			}
		}catch(Exception e) {
			 if (getDebugMode()) {
				 System.out.println("ERROR Generic Exception: " + e.toString());
			 }

			 return caseIdH;
		}
		finally {
			try {
				//Liberamos la consulta
		  		result.close();
		  		sqlQuery.close();
			}catch(Exception e) {}
		}
		
		return caseIdH;
	}	
	
	/**
	 * Devuelve un objeto con el detalle del proceso a lanzar
	 * @param sqlQuery
	 * @param strProcessName
	 * @return PcProcessDefinition
	 */
	/**
	 * @deprecated
	 */
	private PcProcessDefinition getProcessDetails(SqlExec sqlQuery, String strProcessName) { 
		
		PcProcessDefinition pd = null;
		//PcProcessDefinitionAug pd = null;
		
		if (getDebugMode()) {
			System.out.println("------------------------------------------------------------------------------------------");
			System.out.println("Getting process definition details: " + strProcessName);
		}

		
		try {
		
			if (sqlQuery == null) {
				LanzaProceso.openDBConnection(10); // abrimos la sesion para PC
			}
			
			sqlQuery.execute(strQueryProcessDef.replace("%1", strProcessName));
	
			if (sqlQuery.getRowCount() == 1) {
		  		pd.setcId((String) sqlQuery.getValue(1, 1));
		  		pd.setlineOfBusiness((String) sqlQuery.getValue(1, 4));
		  		pd.setsalesChannel((String) sqlQuery.getValue(1, 5));
		  		pd.setprocessMapAction((ActionType)ActionTypeTP.def.findByCode((String) clfySqlExecPC.getValue(1, 3)));
		  		pd.setspecifiedVersionId((Integer) sqlQuery.getValue(1, 2));
				
			}
		}catch(Exception e){
			if (getDebugMode()) {
				System.out.println("------------------------------------------------------------------------------------------");
				System.out.println("ERROR getting process definition details: " + e.toString());
			}
			return null;
			
		}finally{			
			//Liberamos la consulta
			sqlQuery.release();
		}
		return pd;
	}

	/**
	 * Devuelve un objeto con el detalle del proceso a lanzar
	 * @param conn
	 * @param strProcessName
	 * @return
	 */
	private PcProcessDefinition getProcessDetails(Connection conn, String strProcessName) { 
		
		PcProcessDefinition pd = null;
		CallableStatement sqlQuery = null;
		
		ResultSet result = null; 
		if (getDebugMode()) {
			System.out.println("------------------------------------------------------------------------------------------");
			System.out.println("Getting process definition details: " + strProcessName);
		}

		
		try {

			if (conn == null) {
				LanzaProceso.openDBConnection("PC"); // abrimos la sesion para PC
			}
			
			sqlQuery = (CallableStatement) conn.prepareStatement(strQueryProcessDef.replace("%1", strProcessName));
			result = sqlQuery.executeQuery();
			
			if (result.getFetchSize() == 1) {
		  		pd.setcId(result.getString(1));
		  		pd.setlineOfBusiness(result.getString(4));
		  		pd.setsalesChannel(result.getString(5));
		  		pd.setprocessMapAction((ActionType)ActionTypeTP.def.findByCode(result.getString(3)));
		  		pd.setspecifiedVersionId(result.getInt(2));
		  		
			}
		}catch(Exception e){
			if (getDebugMode()) {
				System.out.println("------------------------------------------------------------------------------------------");
				System.out.println("ERROR getting process definition details: " + e.toString());
			}
			return null;
			
		}finally{		
			try {
				//Liberamos la consulta
		  		result.close();
		  		sqlQuery.close();
			}catch(Exception e) {}
			
		}
		return pd;
	}
	

	
	/**
	 * Devuelve un objeto con el detalle de la orden obtenido de bbdd
	 * @param sqlQuery
	 * @param orderId
	 * @return OmOrder
	 */
	/**
	 * @deprecated
	 */
	private OmOrder getOmOrderDetails(SqlExec sqlQuery, String orderId) {
		OmOrder om = null;
		
		if (getDebugMode()) {
			System.out.println("------------------------------------------------------------------------------------------");
			System.out.println("Getting OMOrder details: " + orderId);
		}
		
		try {

			if (sqlQuery == null) {
				LanzaProceso.openDBConnection(0); // abrimos la sesion para OMS
			}
			
	  		om = (OmOrder) OmOrder.create(getStrIDContract());
	  		om.setclfyOrderIdVal(getStrIDContract());

	  		sqlQuery.execute(strQueryOmsOrder.replace("%1", orderId));
	  		
	  		if (sqlQuery.getRowCount() == 1) {

	  			om.setorderMode((DynOrderMode) DynOrderModeTP.def.findByCode((String) sqlQuery.getValue(1, 2)));
	  			om.setgroupId((String)sqlQuery.getValue(1, 3));
	  			om.setrootCustomerId((String)sqlQuery.getValue(1, 4));
	  			om.setorderStatus((OrderStatus) OrderStatusTP.def.findByCode((String) sqlQuery.getValue(1, 5)));
	  			om.setopportunityId((String)sqlQuery.getValue(1, 6));
	  			om.setcurrentSalesChannel((String)sqlQuery.getValue(1, 7));
	  			om.setdealerCode((String)sqlQuery.getValue(1, 8));
	  			om.setaddressId((String)sqlQuery.getValue(1, 9));
	  			om.setisCreatedAnonymous(false);
	      		
	      		OmOrderHolder h = new OmOrderHolder();
	      		h.setValue(om);
	      		
	      		OmOrderAddition.createNewOrderAddition(orderId, h);
	      		om.setORDER_ADDITION(((OmOrder) h.getValue()).getORDER_ADDITION()); 
	      		
	      		om.setapplicationDate((Date) sqlQuery.getValue(1, 13));
	      		om.setcontactId((String)sqlQuery.getValue(1, 16));
	      		om.setcreationDate((Date) sqlQuery.getValue(1, 12));
	      		om.setcustomerId((String)sqlQuery.getValue(1, 17));
	      		om.setcustOrderReference((String)sqlQuery.getValue(1, 15));
	      		om.setdepositID((String)sqlQuery.getValue(1, 19));
	      		om.setDueDate((Date) sqlQuery.getValue(1, 14));
	      		om.setextReferenceNum((String)sqlQuery.getValue(1, 10));
	      		om.setisSaved(true);
	      		om.setorderUnitId((String)sqlQuery.getValue(1, 20));
	      		om.setorderUnitType((OrderUnitType) OrderUnitTypeTP.def.findByCode((String)sqlQuery.getValue(1, 21)));
	      		om.setpriority((Integer)sqlQuery.getValue(1, 22));
	      		om.setproposalExpiryDate((Date) sqlQuery.getValue(1, 23));
	      		om.setrecontact_in_month((Integer)sqlQuery.getValue(1, 24));
	      		om.setrecontact_in_year((Integer)sqlQuery.getValue(1, 25));
	      		om.setrecontactPeriod((RecontactPeriod) RecontactPeriodTP.def.findByCode((String) (sqlQuery.getValue(1, 26))));
	      		om.setreferenceNumber(orderId);
	      		om.setsalesChannel((String)sqlQuery.getValue(1, 7));
	      		om.setserviceRequiredDate((Date) sqlQuery.getValue(1, 11));
	      		om.setsourceOrder((String)sqlQuery.getValue(1, 27));

	  		}
	  		
		}catch (Exception e){
			if (getDebugMode()) {
				System.out.println("------------------------------------------------------------------------------------------");
				System.out.println("ERROR getting OMOrder details: " + e.toString());
			}
			
			return null;
			
	    }finally{
	    	
	    	sqlQuery.release();
	    	
	    }
		
		return om;
	}

	/**
	 *  Devuelve un objeto con el detalle de la orden obtenido de bbdd
	 * @param con
	 * @param orderId
	 * @return
	 */
	private OmOrder getOmOrderDetails(Connection conn, String orderId) {
		OmOrder om = null;
		CallableStatement sqlQuery = null;
		ResultSet result = null;
		
		if (getDebugMode()) {
			System.out.println("------------------------------------------------------------------------------------------");
			System.out.println("Getting OMOrder details: " + orderId);
		}
		
		try {

			if (conn == null) {
				LanzaProceso.openDBConnection("OMS"); // abrimos la sesion para OMS
			}
			
	  		om = (OmOrder) OmOrder.create(getStrIDContract());
	  		om.setclfyOrderIdVal(getStrIDContract());

			sqlQuery = (CallableStatement) conn.prepareStatement(strQueryOmsOrder.replace("%1", orderId));
			result = sqlQuery.executeQuery();
			
			if (result.getFetchSize() == 1) {

	  			om.setorderMode((DynOrderMode) DynOrderModeTP.def.findByCode(result.getString(2)));
	  			om.setgroupId(result.getString(3));
	  			om.setrootCustomerId(result.getString(4));
	  			om.setorderStatus((OrderStatus) OrderStatusTP.def.findByCode(result.getString(5)));
	  			om.setopportunityId(result.getString(6));
	  			om.setcurrentSalesChannel(result.getString(7));
	  			om.setdealerCode(result.getString(8));
	  			om.setaddressId(result.getString(9));
	  			om.setisCreatedAnonymous(false);
	      		
	      		OmOrderHolder h = new OmOrderHolder();
	      		h.setValue(om);
	      		
	      		OmOrderAddition.createNewOrderAddition(orderId, h);
	      		om.setORDER_ADDITION(((OmOrder) h.getValue()).getORDER_ADDITION()); 
	      		
	      		om.setapplicationDate( result.getDate(13));
	      		om.setcontactId(result.getString(16));
	      		om.setcreationDate(result.getDate(12));
	      		om.setcustomerId(result.getString(17));
	      		om.setcustOrderReference(result.getString(15));
	      		om.setdepositID(result.getString(19));
	      		om.setDueDate(result.getDate(14));
	      		om.setextReferenceNum(result.getString(10));
	      		om.setisSaved(true);
	      		om.setorderUnitId(result.getString(20));
	      		om.setorderUnitType((OrderUnitType) OrderUnitTypeTP.def.findByCode(result.getString(21)));
	      		om.setpriority(result.getInt(22));
	      		om.setproposalExpiryDate(result.getDate(23));
	      		om.setrecontact_in_month(result.getInt(24));
	      		om.setrecontact_in_year(result.getInt(25));
	      		om.setrecontactPeriod((RecontactPeriod) RecontactPeriodTP.def.findByCode(result.getString(26)));
	      		om.setreferenceNumber(orderId);
	      		om.setsalesChannel(result.getString(7));
	      		om.setserviceRequiredDate(result.getDate(11));
	      		om.setsourceOrder(result.getString(27));

	  		}
	  		
		}catch (Exception e){
			if (getDebugMode()) {
				System.out.println("------------------------------------------------------------------------------------------");
				System.out.println("ERROR getting OMOrder details: " + e.toString());
			}
			
			return null;
			
	    }finally{
	    	try {
		    	result.close();
		    	sqlQuery.close();
		    	
	    	}catch(Exception e) {}
	    	
	    }
		
		return om;
	}
	/**
	 * Recupera los datos de Order
	 * @param om  --> OMOrder desde la propiedad this.order
	 * @return Order
	 */
	private Order getInputOrderDetails(OmOrder om) {

		Order o = new Order();
		
		if (getDebugMode()) {
			System.out.println("------------------------------------------------------------------------------------------");
			System.out.println("Getting Order details: " + om.getId());
		}
		
		try {
			//definimos los objetos que necesitaremos para generar inputOrder
			OrderID oid = null;
			oid.setOrderID(om.getId());
			
			SalesChannelRVT sc = new SalesChannelRVT(om.getsalesChannel());
			
			OrderUserActionRVT ouaRVT = null;
			OrderUserAction[] oua = null;
			oua[0].setAction(ouaRVT);
			oua[0].setAllowed(true);
			oua[0].setRelinquishChannels(0, sc);
	
			OrderDetails od = null;			
			od.setServiceRequiredDate(om.getserviceRequiredDate());
			od.setCreationDate(om.getcreationDate());
			od.setApplicationDate(om.getapplicationDate());
			od.setExpiryDate(om.getDueDate());
			od.setOrderStatus(new OrderStatusRVT(om.getOrderStatusVal().getCode()));
			od.setOrderMode(new OrderModeRVT(om.getorderMode().getCode()));
			od.setSalesChannel(sc);
			od.setExternalOrderID(om.getextReferenceNum());
			od.setCustomerOrderID(om.getcustOrderReference());
			od.setCurrentSalesChannel(sc);

			PersonID pid = null;
			pid.setId(om.getcontactId());

			PersonHeader ph = null;
			ph.setPersonID(pid);
			ph.setId(pid.getId());
			
			// formamos el objeto Order
			o.setOrderDetails(od);
			o.setOrderID(oid);
			o.setAvailableUserActions(oua);
			o.setAnonymous(false);
			o.setAddressIDX9(om.getaddressId());
			o.setBiometricCheckValidatedSuccessfullyX9(true); //Esta accion del proceso pasara siempre como completada
			o.setExternalOrderIdX11(om.getextReferenceNum());
			o.setContact(ph);
			o.setAddressIDX9(om.getaddressId());
			o.setExternalOrderIdX9(om.getextReferenceNum());
			
		}catch(Exception e) {
			
			if (getDebugMode()) {
				System.out.println("------------------------------------------------------------------------------------------");
				System.out.println("ERROR getting Order details: " + e.toString());
			}
			
			return null;
		}
		
		return o;
	}
	
	/**
	 * Recupera los datosde StartOrderInput
	 * @param sqlQuery  --> consulta sobre OMS
	 * @param orderId   --> id de orden
	 * @return StartOrderInput
	 */
	/**
	 * @deprecated
	 */
	private StartOrderInput getStartOrderInput(SqlExec sqlQuery, String orderId) {
		
		StartOrderInput sti = new StartOrderInput();
		
		if (getDebugMode()) {
			System.out.println("------------------------------------------------------------------------------------------");
			System.out.println("Getting StartOrderInput details: " + orderId);
		}

		try{
			
			if (sqlQuery == null) {
				LanzaProceso.openDBConnection(0); // abrimos la sesion para OMS
			}

			sqlQuery.execute(strQuerOmsOrderAction.replace("%1", orderId));

			if (sqlQuery.getRowCount() == 1) {
				OrderActionData[] oad = null;
				OmOrderAction[] oma = null;
				OmOrderAction omoa = null;
			
				
				for (int i=0; i < sqlQuery.getRowCount(); i++) {

					DynamicAttribute[] dynAtr = null;
					String strDynamic = (String)sqlQuery.getValue(1, 5);
					
					if (!"".equals(strDynamic) && strDynamic != null) {
						
						String[] values = strDynamic.split("=");
						if (!"".equals(values[0]) && values[0] !=null) {
							dynAtr[0].setName(values[0]);
							dynAtr[0].setValue(values[1]);
						}
					}
					
					//formamos el objeto OrderAction
					OrderAction oaInfo = null;
					
					OrderActionID oaID = null;
					oaID.setOrderActionID((String)sqlQuery.getValue(1, 1));
					oaInfo.setOrderActionID(oaID);

					//OrderActionDetails
					OrderActionDetails oaDet = null;
					
					oaDet.setActionType(new OrderActionTypeRVT((String)sqlQuery.getValue(1, 5)));
					oaDet.setStatus(new OrderActionStatusRVT((String) sqlQuery.getValue(1, 4)));
					oaDet.setOriginator((String) sqlQuery.getValue(1, 21));
					oaDet.setCurrentOwner((String)sqlQuery.getValue(1, 21));
					oaDet.setApplicationDate((Date)sqlQuery.getValue(1, 16));
					oaDet.setServiceRequireDate((Date)sqlQuery.getValue(1, 14));
					oaDet.setDueDate((Date)sqlQuery.getValue(1, 6));
					oaDet.setSalesChannel(new SalesChannelRVT((String) sqlQuery.getValue(1, 27)));
					oaDet.setCancelProcess(false);
					oaDet.setExternalOrderActionID((String)sqlQuery.getValue(1, 25));
					oaDet.setCustomerOrderActionID((String)sqlQuery.getValue(1, 31));
					
					SubscriptionGroupID sgID = new SubscriptionGroupID();
					sgID.setID((Integer)sqlQuery.getValue(1, 23));							
					oaDet.setSubscriptionGroupID(sgID);
					oaDet.setAmendProcess(false);
					oaDet.setCancelProcess(false);	
					
					OrganizationID oID = null;
					oID.setId(null); // Pendiente de buscar el valor a usar
					oaDet.setOrganizationID(oID); 
					
					//OrderActionUserAction
					OrderActionUserAction[] ouAct = null;
					OrderActionUserActionRVT oauAct = null;
					ouAct[0].setAction(oauAct);
					
					//OrderHeader
					OrderHeader oh = null;

					oh.setOrderID(this.inputOrder.getOrderID());
					oh.setOrderMode(this.inputOrder.getOrderDetails().getOrderMode());
					oh.setOrderStatus(this.inputOrder.getOrderDetails().getOrderStatus());
					oh.setApplicationDate(this.inputOrder.getOrderDetails().getApplicationDate());
					oh.setServiceRequiredDate(this.inputOrder.getOrderDetails().getServiceRequiredDate());
					oh.setSalesChannel(this.inputOrder.getOrderDetails().getSalesChannel());
					oh.setExpiryDate(this.inputOrder.getOrderDetails().getExpiryDate());
					oh.setCustomerOrderID(this.inputOrder.getOrderDetails().getCustomerOrderID());
					oh.setExternalOrderID(this.inputOrder.getOrderDetails().getExternalOrderID());
					oh.setAvailableUserActions(this.inputOrder.getAvailableUserActions());
					oh.setSalesChannel(this.inputOrder.getOrderDetails().getCurrentSalesChannel());
					oh.setLocked(true);
					oh.setAnonymous(false);
					
					oh.setOrderRetrievalCriteria(dynAtr);
					oh.setCustomerAgreedToPayDeposit(false);

					// Rellenamos el objeto OrderAction
					oaInfo.setOrderActionID(oaID);
					oaInfo.setOrderActionDetails(oaDet);
					oaInfo.setAvailableUserActions(ouAct);
					oaInfo.setOrderHeader(oh);

					// OrderActionData
					oad[i].setOrderActionInfo(oaInfo);
					oad[i].setDynamicAttributes(dynAtr);
					
					// Generamos la info de OMOrderAction con la misma consulta
					omoa = null;
		      		
		      		omoa.setApId((String)sqlQuery.getValue(1, 7));
		      		
		      		ApItem api = null;					      		
		      		api.setAPId((String)sqlQuery.getValue(1, 7));
		      		api.setVersionIdVal((String)sqlQuery.getValue(1, 10));
		      		omoa.setAPITEM_REL(api);
		      		
		      		omoa.setapplicationDate((Date)sqlQuery.getValue(1, 16));
		      		omoa.setApplicationReferenceIdVal((String)sqlQuery.getValue(1, 32));
		      		omoa.setApVersionId((String)sqlQuery.getValue(1, 10));					      		
		      		omoa.setBpNameVal(this.getStrProcessName()); //Nombre del proceso a ejecutar!
		      		
		      		omoa.setconfigOA(ConfigurationTP.R);
		      		omoa.setcontactId((String)sqlQuery.getValue(1, 18));
		      		omoa.setcreationDate((Date)sqlQuery.getValue(1, 26));
		      		omoa.setcreatorId((String)sqlQuery.getValue(1, 21));
		      		omoa.setcustomerId((String)sqlQuery.getValue(1, 19));
		      		omoa.setcustOrderReference((String)sqlQuery.getValue(1, 31));
		      		omoa.setDueDate((Date)sqlQuery.getValue(1, 6));
		      		omoa.setearlyDate((Date)sqlQuery.getValue(1, 33));
		      		omoa.setextReferenceNum((String)sqlQuery.getValue(1, 25));
		      		omoa.setextReferenceRemark((String)sqlQuery.getValue(1, 34));
		      		omoa.setInitialProcessDefinition(this.getProcessDef());
		      		omoa.setisActive(BooleanValTP.TRUE);
		      		omoa.setisMain((BooleanVal) BooleanValTP.def.findByCode((String)sqlQuery.getValue(1, 9)));
		      		omoa.setIsProcessStarted(false);
		      		omoa.setLanguageVal((LanguageCode) LanguageCodeTP.def.findByCode((String)sqlQuery.getValue(1, 12)));
		      		omoa.setItemPartitionKeyVal((Integer) sqlQuery.getValue(1, 35));
		      		omoa.setorder(this.order);
		      		omoa.setorderActionStatus((OrderActionStatus) OrderActionStatusTP.def.findByCode((String)sqlQuery.getValue(1, 4)));
		      		omoa.setorderUnitId((String)sqlQuery.getValue(1, 1));
		      		omoa.setorderTypeName((ActionType) ActionTypeTP.def.findByCode((String)sqlQuery.getValue(1, 5)));
		      		omoa.setorderUnitType(this.order.getorderUnitType());
		      		omoa.setorganizationOwnerId((String)sqlQuery.getValue(1, 17));
		      		omoa.setparentOrderUnitId((String)sqlQuery.getValue(1, 2));
		      		omoa.setparentRelation((OrderActionParentRelation) OrderActionParentRelationTP.def.findByCode((String)sqlQuery.getValue(1, 8)));
		      		omoa.setpriority((Integer)sqlQuery.getValue(1, 15));
		      		omoa.setPrivsKeyVal((String)sqlQuery.getValue(1, 36));
		      		omoa.setQSequenceNumVal((Integer)sqlQuery.getValue(1, 37));
		      		omoa.setQuantityVal((Integer)sqlQuery.getValue(1, 38));
		      		omoa.setreasonFreeText((String)sqlQuery.getValue(1, 39));
		      		omoa.setreasonId((String)sqlQuery.getValue(1, 28));
		      		omoa.setcustomerWillReContactInd((1 == (Integer)sqlQuery.getValue(1, 40)));
		      		omoa.setrecontactPeriod((RecontactPeriod)RecontactPeriodTP.def.findByCode((String) sqlQuery.getValue(1, 41)));
		      		omoa.setrecontact_in_month((Integer)sqlQuery.getValue(1, 42));
		      		omoa.setrecontact_in_year((Integer)sqlQuery.getValue(1, 43));
		      		omoa.setreferenceNumber((String)sqlQuery.getValue(1, 20));
		      		omoa.setreplacedOfferApID((String)sqlQuery.getValue(1, 44));
		      		omoa.setrequestLineId((String)sqlQuery.getValue(1, 45));
		      		omoa.setSalesChannelVal((String)sqlQuery.getValue(1, 27));					      		
		      		omoa.setserviceRequiredDate(this.order.getserviceRequiredDate());
		      		
		      		//sumamos la OA al array
		      		oma[i] = omoa;
					
					
				}
				
	      		this.order.setOMORDER_ACTION_CHILDs(EpiCollections.singletonTypedListSet(OmOrderAction.class, oma));
				
				// StartOrderInput
	      		sti.setOrder(this.inputOrder);
	      		sti.setOrderActionsData(oad);
	      		sti.setConfirmationChecksApproved(true);
	      		sti.setMarkOrderAsSaved(true);
				
			}
			
		}catch (Exception e){
			if (getDebugMode()) {
				System.out.println("------------------------------------------------------------------------------------------");
				System.out.println("ERROR getting StartOrderInput details: " + e.toString());
			}
			
			return null;
			
			
			
		}finally {
			sqlQuery.release();
		}
		
		return sti;
	}
	


	/**
	 * Recupera los datosde StartOrderInput
	 * @param sqlQuery  --> consulta sobre OMS
	 * @param orderId   --> id de orden
	 * @return StartOrderInput
	 */
	private StartOrderInput getStartOrderInput(Connection conn, String orderId) {
		
		StartOrderInput sti = new StartOrderInput();

		CallableStatement sqlQuery = null;
		ResultSet result = null;
		
		if (getDebugMode()) {
			System.out.println("------------------------------------------------------------------------------------------");
			System.out.println("Getting StartOrderInput details: " + orderId);
		}

		try{
			
			if (conn == null) {
				LanzaProceso.openDBConnection("OMS"); // abrimos la sesion para OMS
			}

			sqlQuery = (CallableStatement) conn.prepareStatement(strQueryOmsOrder.replace("%1", orderId));
			result = sqlQuery.executeQuery();
			
			if (result.getFetchSize() > 0) {			

				OrderActionData[] oad = null;
				OmOrderAction[] oma = null;
				OmOrderAction omoa = null;
				int iConta = 0;
				
				while (result.next()) {

					DynamicAttribute[] dynAtr = null;
					String strDynamic = result.getString(5);
					
					if (!"".equals(strDynamic) && strDynamic != null) {
						
						String[] values = strDynamic.split("=");
						if (!"".equals(values[0]) && values[0] !=null) {
							dynAtr[0].setName(values[0]);
							dynAtr[0].setValue(values[1]);
						}
					}
					
					//formamos el objeto OrderAction
					OrderAction oaInfo = null;
					
					OrderActionID oaID = null;
					oaID.setOrderActionID(result.getString(1));
					oaInfo.setOrderActionID(oaID);

					//OrderActionDetails
					OrderActionDetails oaDet = null;
					
					oaDet.setActionType(new OrderActionTypeRVT((result.getString(5))));
					oaDet.setStatus(new OrderActionStatusRVT(result.getString(4)));
					oaDet.setOriginator(result.getString(21));
					oaDet.setCurrentOwner(result.getString( 21));
					oaDet.setApplicationDate(result.getDate(16));
					oaDet.setServiceRequireDate(result.getDate(14));
					oaDet.setDueDate(result.getDate(6));
					oaDet.setSalesChannel(new SalesChannelRVT(result.getString(27)));
					oaDet.setCancelProcess(false);
					oaDet.setExternalOrderActionID(result.getString(25));
					oaDet.setCustomerOrderActionID(result.getString(31));
					
					SubscriptionGroupID sgID = new SubscriptionGroupID();
					sgID.setID(result.getInt(23));							
					oaDet.setSubscriptionGroupID(sgID);
					oaDet.setAmendProcess(false);
					oaDet.setCancelProcess(false);	
					
					OrganizationID oID = null;
					oID.setId(null); // Pendiente de buscar el valor a usar
					oaDet.setOrganizationID(oID); 
					
					//OrderActionUserAction
					OrderActionUserAction[] ouAct = null;
					OrderActionUserActionRVT oauAct = null;
					ouAct[0].setAction(oauAct);
					
					//OrderHeader
					OrderHeader oh = null;

					oh.setOrderID(this.inputOrder.getOrderID());
					oh.setOrderMode(this.inputOrder.getOrderDetails().getOrderMode());
					oh.setOrderStatus(this.inputOrder.getOrderDetails().getOrderStatus());
					oh.setApplicationDate(this.inputOrder.getOrderDetails().getApplicationDate());
					oh.setServiceRequiredDate(this.inputOrder.getOrderDetails().getServiceRequiredDate());
					oh.setSalesChannel(this.inputOrder.getOrderDetails().getSalesChannel());
					oh.setExpiryDate(this.inputOrder.getOrderDetails().getExpiryDate());
					oh.setCustomerOrderID(this.inputOrder.getOrderDetails().getCustomerOrderID());
					oh.setExternalOrderID(this.inputOrder.getOrderDetails().getExternalOrderID());
					oh.setAvailableUserActions(this.inputOrder.getAvailableUserActions());
					oh.setSalesChannel(this.inputOrder.getOrderDetails().getCurrentSalesChannel());
					oh.setLocked(true);
					oh.setAnonymous(false);
					
					oh.setOrderRetrievalCriteria(dynAtr);
					oh.setCustomerAgreedToPayDeposit(false);

					// Rellenamos el objeto OrderAction
					oaInfo.setOrderActionID(oaID);
					oaInfo.setOrderActionDetails(oaDet);
					oaInfo.setAvailableUserActions(ouAct);
					oaInfo.setOrderHeader(oh);

					// OrderActionData
					oad[iConta].setOrderActionInfo(oaInfo);
					oad[iConta].setDynamicAttributes(dynAtr);
					
					// Generamos la info de OMOrderAction con la misma consulta
					omoa = null;
		      		
		      		omoa.setApId(result.getString(7));
		      		
		      		ApItem api = null;					      		
		      		api.setAPId(result.getString(7));
		      		api.setVersionIdVal(result.getString(10));
		      		omoa.setAPITEM_REL(api);
		      		
		      		omoa.setapplicationDate(result.getDate(16));
		      		omoa.setApplicationReferenceIdVal(result.getString(32));
		      		omoa.setApVersionId(result.getString(10));					      		
		      		omoa.setBpNameVal(this.getStrProcessName()); //Nombre del proceso a ejecutar!
		      		
		      		omoa.setconfigOA(ConfigurationTP.R);
		      		omoa.setcontactId(result.getString(18));
		      		omoa.setcreationDate(result.getDate( 26));
		      		omoa.setcreatorId(result.getString(21));
		      		omoa.setcustomerId(result.getString(19));
		      		omoa.setcustOrderReference(result.getString(31));
		      		omoa.setDueDate(result.getDate( 6));
		      		omoa.setearlyDate(result.getDate(33));
		      		omoa.setextReferenceNum(result.getString(25));
		      		omoa.setextReferenceRemark(result.getString(34));
		      		omoa.setInitialProcessDefinition(this.getProcessDef());
		      		omoa.setisActive(BooleanValTP.TRUE);
		      		omoa.setisMain((BooleanVal) BooleanValTP.def.findByCode(result.getString(9)));
		      		omoa.setIsProcessStarted(false);
		      		omoa.setLanguageVal((LanguageCode) LanguageCodeTP.def.findByCode(result.getString(12)));
		      		omoa.setItemPartitionKeyVal(result.getInt(35));
		      		omoa.setorder(this.order);
		      		omoa.setorderActionStatus((OrderActionStatus) OrderActionStatusTP.def.findByCode(result.getString(4)));
		      		omoa.setorderUnitId(result.getString(1));
		      		omoa.setorderTypeName((ActionType) ActionTypeTP.def.findByCode(result.getString(5)));
		      		omoa.setorderUnitType(this.order.getorderUnitType());
		      		omoa.setorganizationOwnerId(result.getString(17));
		      		omoa.setparentOrderUnitId(result.getString(2));
		      		omoa.setparentRelation((OrderActionParentRelation) OrderActionParentRelationTP.def.findByCode(result.getString(8)));
		      		omoa.setpriority(result.getInt(15));
		      		omoa.setPrivsKeyVal(result.getString(36));
		      		omoa.setQSequenceNumVal(result.getInt(37));
		      		omoa.setQuantityVal(result.getInt(38));
		      		omoa.setreasonFreeText(result.getString(39));
		      		omoa.setreasonId(result.getString(28));
		      		omoa.setcustomerWillReContactInd((1 == result.getInt(40)));
		      		omoa.setrecontactPeriod((RecontactPeriod)RecontactPeriodTP.def.findByCode(result.getString(41)));
		      		omoa.setrecontact_in_month(result.getInt(42));
		      		omoa.setrecontact_in_year(result.getInt(43));
		      		omoa.setreferenceNumber(result.getString(20));
		      		omoa.setreplacedOfferApID(result.getString(44));
		      		omoa.setrequestLineId(result.getString(45));
		      		omoa.setSalesChannelVal(result.getString(27));					      		
		      		omoa.setserviceRequiredDate(this.order.getserviceRequiredDate());
		      		
		      		//sumamos la OA al array
		      		oma[iConta] = omoa;
		      		iConta++;
					
				}
				
	      		this.order.setOMORDER_ACTION_CHILDs(EpiCollections.singletonTypedListSet(OmOrderAction.class, oma));
				
				// StartOrderInput
	      		sti.setOrder(this.inputOrder);
	      		sti.setOrderActionsData(oad);
	      		sti.setConfirmationChecksApproved(true);
	      		sti.setMarkOrderAsSaved(true);
				
			}
			
		}catch (Exception e){
			if (getDebugMode()) {
				System.out.println("------------------------------------------------------------------------------------------");
				System.out.println("ERROR getting StartOrderInput details: " + e.toString());
			}
			
			return null;
			
			
			
		}finally {
	    	try {
		    	result.close();
		    	sqlQuery.close();
		    	
	    	}catch(Exception e) {}
			
		}
		
		return sti;
	}

	// GETTERS Y SETTERS
	public String getStrIDContract() {
		return strIDContract;
	}
	public void setStrIDContract(String strObjidContract) {
		this.strIDContract = strObjidContract;
	}
	public String getStrProcessName() {
		return strProcessName;
	}
	public void setStrProcessName(String strProcessName) {
		this.strProcessName = strProcessName;
	}
	public String getStrVersion() {
		return strVersion;
	}
	public void setStrVersion(String strVersion) {
		this.strVersion = strVersion;
	}	
	public String getStrObjidLanzado() {
		return strObjidLanzado;
	}
	public void setStrObjidLanzado(String strObjidLanzado) {
		this.strObjidLanzado = strObjidLanzado;
	}

	protected OmOrder getOrder() {
		return order;
	}
	protected void setOrder(OmOrder order) {
		this.order = order;
	}
	protected String getSalesChannel() {
		return salesChannel;
	}
	protected void setSalesChannel(String salesChannel) {
		this.salesChannel = salesChannel;
	}

	protected static boolean getDebugMode() {
		return DebugMode;
	}

	protected static void setDebugMode(boolean debugMode) {
		DebugMode = debugMode;
	}

	protected PcProcessDefinition getProcessDef() {
		return processDef;
	}

	protected void setProcessDef(PcProcessDefinition processDef) {
		this.processDef = processDef;
	}

	
}
