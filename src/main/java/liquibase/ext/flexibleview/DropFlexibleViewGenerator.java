package liquibase.ext.flexibleview;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import liquibase.database.Database;
import liquibase.database.core.OracleDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.exception.ValidationErrors;
import liquibase.ext.ora.dropmaterializedview.DropMaterializedViewGenerator;
import liquibase.ext.ora.dropmaterializedview.DropMaterializedViewStatement;
import liquibase.logging.LogService;
import liquibase.logging.Logger;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.sqlgenerator.core.DropViewGenerator;
import liquibase.statement.core.DropViewStatement;

public class DropFlexibleViewGenerator extends AbstractSqlGenerator<DropFlexibleViewStatement> {
	
	private static final Logger log = LogService.getLog(DropFlexibleViewGenerator.class);
	
	@Override
	public boolean supports( DropFlexibleViewStatement statement, Database database ) {
		return database instanceof OracleDatabase;
	}

	public ValidationErrors validate( DropFlexibleViewStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain ) {
		ValidationErrors validationErrors = new ValidationErrors();
		validationErrors.checkRequiredField( "viewName", statement.getViewName() );
		return validationErrors;
	}

	public Sql[] generateSql( DropFlexibleViewStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain ) {
		JdbcConnection connection = (JdbcConnection) database.getConnection();
		// check if the view already exists, then formulate a plan from there!
		
		FlexibleView existingView = null;
		
		ResultSet rs = null;
		try {
			rs = connection.getMetaData().getTables( database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName(), statement.getViewName().toUpperCase(), FlexibleView.getSQLValues() );
			if ( rs.next() ) {
				existingView = FlexibleView.deriveByValue( rs.getString( "TABLE_TYPE" ) );
			}
		} catch ( DatabaseException e ) {
			throw new UnexpectedLiquibaseException( "Failed on database view investigation.", e );
		} catch ( SQLException e ) {
			throw new UnexpectedLiquibaseException( "Failed on database view investigation.", e );
		} finally {
			try { if ( rs != null ) rs.close(); } catch ( Throwable t ) { }
		}
		
		List<Sql> sequel = new ArrayList<Sql>();
		
		if ( FlexibleView.MATERIALIZED == existingView ) {
			DropMaterializedViewStatement dropMViewStmt = new DropMaterializedViewStatement( statement.getViewName() );
			dropMViewStmt.setSchemaName( database.getLiquibaseSchemaName() );
			DropMaterializedViewGenerator dropMViewGen = new DropMaterializedViewGenerator();
			sequel.addAll( Arrays.asList( dropMViewGen.generateSql( dropMViewStmt, database, null ) ) );
		} else if ( FlexibleView.VIEW == existingView ) {
			DropViewStatement dropViewStmt = new DropViewStatement( database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName(), statement.getViewName() );
			DropViewGenerator dropViewGen = new DropViewGenerator();
			sequel.addAll( Arrays.asList( dropViewGen.generateSql( dropViewStmt, database, null ) ) );
		} else {
			log.warning( String.format( "The [materialized] view named %s was not found when it was attempted to be dropped from the database.", statement.getViewName() ) );
		}

		log.info( sequel.stream().map( new java.util.function.Function<Object, String>() {
			public String apply( Object o ) {
				return String.valueOf( o );
			} }  ).collect( Collectors.joining( "\n" ) ) );
		
		return sequel.toArray( new Sql[0] );
	}
}
