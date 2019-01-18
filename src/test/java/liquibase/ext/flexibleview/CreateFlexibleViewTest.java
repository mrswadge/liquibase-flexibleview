package liquibase.ext.flexibleview;

import static org.junit.Assert.*;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Scope;
import liquibase.change.Change;
import liquibase.change.ChangeFactory;
import liquibase.change.ChangeMetaData;
import liquibase.changelog.ChangeLogHistoryService;
import liquibase.changelog.ChangeLogHistoryServiceFactory;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.core.OracleDatabase;
import liquibase.exception.RollbackFailedException;
import liquibase.ext.flexibleview.CreateFlexibleViewChange;
import liquibase.ext.flexibleview.FlexibleView;
import liquibase.ext.flexibleview.testcore.BaseTestCase;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.SqlStatement;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CreateFlexibleViewTest extends BaseTestCase {

	@Before
	public void setUp() throws Exception {
		changeLogFile = "liquibase/ext/flexibleview/changelog.test.xml";
		connectToDB();
		cleanDB();
	}

	//@Test
	public void getChangeMetaData() {
		CreateFlexibleViewChange view = new CreateFlexibleViewChange();

		ChangeFactory changeFactory = Scope.getCurrentScope().getSingleton(ChangeFactory.class);
		assertEquals( "createFlexibleView", changeFactory.getChangeMetaData( view ).getName() );
		assertEquals( "Create a new database view or materialized view depending on context.", changeFactory.getChangeMetaData( view ).getDescription() );
		assertEquals( ChangeMetaData.PRIORITY_DEFAULT, changeFactory.getChangeMetaData( view ).getPriority() );
	}

	//@Test
	public void getConfirmationMessage() {
		final String VIEW_NAME = "myview";
		CreateFlexibleViewChange view = new CreateFlexibleViewChange();
		view.setViewName( VIEW_NAME );

		assertEquals( String.format( "Flexible View %s created", VIEW_NAME ), String.format( "Flexible View %s created", view.getViewName() ) );
		assertEquals( String.format( "Flexible View %s created", VIEW_NAME ), view.getConfirmationMessage() );
	}

	//@Test
	public void generateStatement() {
		CreateFlexibleViewChange view = new CreateFlexibleViewChange();
		view.setViewName( "myview" );
		view.setSelectQuery( "select 1 from dual" );

		SqlStatement[] sqlStatements = view.generateStatements( new OracleDatabase() );
		for ( SqlStatement sqlStatement : sqlStatements ) {
			
		}
	}

	@Test
	public void parseAndGenerate() throws Exception {
		Database database = liquiBase.getDatabase();
		ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();

		ChangeLogParameters changeLogParameters = new ChangeLogParameters();

		DatabaseChangeLog changeLog = ChangeLogParserFactory.getInstance().getParser( changeLogFile, resourceAccessor ).parse( changeLogFile, changeLogParameters, resourceAccessor );
		liquiBase.checkLiquibaseTables( true, changeLog, new Contexts(), new LabelExpression() );
		changeLog.validate( database );

		List<ChangeSet> changeSets = changeLog.getChangeSets();

		List<String> expectedQuery = new ArrayList<String>();

		expectedQuery.add( String.format( "CREATE OR REPLACE VIEW %s.myview1 AS select 1 as One from dual", database.getLiquibaseSchemaName() ) );
		expectedQuery.add( String.format( "Drop materialized view %s.myview2", database.getLiquibaseSchemaName() ) );
		expectedQuery.add( String.format( "CREATE OR REPLACE VIEW %s.myview2 AS select * from mytable", database.getLiquibaseSchemaName() ) );
		expectedQuery.add( String.format( "Drop materialized view %s.myview2", database.getLiquibaseSchemaName() ) );
		expectedQuery.add( String.format( "CREATE OR REPLACE VIEW %s.myview2 AS select * from mytable where rownum = 1", database.getLiquibaseSchemaName() ) );
		expectedQuery.add( String.format( "Drop materialized view %s.myview4", database.getLiquibaseSchemaName() ) );
		expectedQuery.add( String.format( "CREATE OR REPLACE VIEW %s.myview4 AS select 1 as One from dual", database.getLiquibaseSchemaName() ) );
		//expectedQuery.add( String.format( "Drop materialized view %s.myview3", database.getLiquibaseSchemaName() ) );
		//expectedQuery.add( String.format( "CREATE OR REPLACE VIEW %s.myview3 AS select * from mytable", database.getLiquibaseSchemaName() ) );
		expectedQuery.add( String.format( "Drop materialized view %s.myview3", database.getLiquibaseSchemaName() ) );
		expectedQuery.add( String.format( "CREATE OR REPLACE VIEW %s.myview3 AS select * from mytable where one like '%%'", database.getLiquibaseSchemaName() ) );
		expectedQuery.add( String.format( "Drop materialized view %s.myview2", database.getLiquibaseSchemaName() ) );

		int i = 0;
		for ( ChangeSet changeSet : changeSets ) {
			for ( Change change : changeSet.getChanges() ) {
				Sql[] sql = SqlGeneratorFactory.getInstance().generateSql( change.generateStatements( database )[0], database );
				
				if ( change instanceof CreateFlexibleViewChange ) {
					for ( Sql s : sql ) {
						System.out.println( "--------------------------------------------------------------------------------------------------------" );
						System.out.println( "[" + i + "] ACTUAL: " + s.toSql() );
						System.out.println( "[" + i + "] EXPECT: " + expectedQuery.get( i ) );
						assertEquals( expectedQuery.get( i ), s.toSql() );
						i++;
					}
				}
			}
		}
	}

	//@Test
	public void test() throws Exception {
		liquiBase.update( new Contexts() );
		
		/**
		 * Check the database.
		 * We expect:
		 * 1. MYVIEW1 to have been dropped.
		 * 2. MYVIEW2 to exist as a materialized view.
		 * 3. MYVIEW3 to exist as a real-time view.
		 */
		
		ResultSet myview1 = null;
		ResultSet myview2 = null;
		ResultSet myview3 = null;
		ResultSet myview4 = null;
		
		try {
			myview1 = connection.getMetaData().getTables( null, null, "MYVIEW1", FlexibleView.getSQLValues() );
			assertFalse( myview1.next() );
			
			myview2 = connection.getMetaData().getTables( null, null, "MYVIEW2", FlexibleView.getSQLValues() );
			assertTrue( myview2.next() );
			assertEquals( FlexibleView.MATERIALIZED, FlexibleView.deriveByValue( myview2.getString( "TABLE_TYPE" ) ) );
			
			myview3 = connection.getMetaData().getTables( null, null, "MYVIEW3", FlexibleView.getSQLValues() );
			assertTrue( myview3.next() );
			assertEquals( FlexibleView.VIEW, FlexibleView.deriveByValue( myview3.getString( "TABLE_TYPE" ) ) );

			myview4 = connection.getMetaData().getTables( null, null, "MYVIEW4", FlexibleView.getSQLValues() );
			assertFalse( myview4.next() );
		} finally {
			closeSilently( myview1, myview2, myview3, myview4 );
		}
	}

}
