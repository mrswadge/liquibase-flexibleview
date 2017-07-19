package liquibase.ext.flexibleview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import liquibase.database.Database;
import liquibase.database.core.OracleDatabase;
import liquibase.exception.ValidationErrors;
import liquibase.ext.ora.dropmaterializedview.DropMaterializedViewOracle;
import liquibase.ext.ora.dropmaterializedview.DropMaterializedViewStatement;
import liquibase.ext.oracle.preconditions.OracleMaterializedViewExistsPrecondition;
import liquibase.logging.LogFactory;
import liquibase.logging.Logger;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.sqlgenerator.core.CreateViewGenerator;
import liquibase.statement.core.CreateViewStatement;

public class CreateFlexibleViewGenerator extends AbstractSqlGenerator<CreateFlexibleViewStatement> {
	
	private static final Logger log = LogFactory.getLogger();
	
	@Override
	public boolean supports( CreateFlexibleViewStatement statement, Database database ) {
		return database instanceof OracleDatabase;
	}

	public ValidationErrors validate( CreateFlexibleViewStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain ) {
		ValidationErrors validationErrors = new ValidationErrors();
		validationErrors.checkRequiredField( "viewName", statement.getViewName() );
		validationErrors.checkRequiredField( "selectQuery", statement.getSelectQuery() );
		return validationErrors;
	}

	/**
	 * 1. View creation to be set to run once only, no more runAlways="true".
	 * 2. If there is a materialized view with the same name as this view, we drop it.
	 * 3. We can then can create the standard view: 'create or replace force view as ... select 1 from dual' 
	 * 4. If the environment is set for materialized views, convert all views to materialized views at the end using the v99.99.99 script.
	 */
	public Sql[] generateSql( CreateFlexibleViewStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain ) {
		// check if the view already exists, then formulate a plan from there!
		String viewName = statement.getViewName().toUpperCase();
		List<Sql> sequel = new ArrayList<Sql>();

		OracleMaterializedViewExistsPrecondition matViewExistsCondition = new OracleMaterializedViewExistsPrecondition();
		matViewExistsCondition.setViewName( viewName );
		if ( matViewExistsCondition.check( database ) ) {
			DropMaterializedViewStatement dropMViewStmt = new DropMaterializedViewStatement( statement.getViewName() );
			dropMViewStmt.setSchemaName( database.getLiquibaseSchemaName() );
			DropMaterializedViewOracle dropMViewGen = new DropMaterializedViewOracle();
			sequel.addAll( Arrays.asList( dropMViewGen.generateSql( dropMViewStmt, database, null ) ) );
		}
		
		// if the view exists as a regular view already, we don't actually care.
		CreateViewStatement createViewStmt = new CreateViewStatement( database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName(), statement.getViewName(), statement.getSelectQuery(), true );

		CreateViewGenerator createViewGen = new CreateViewGenerator();
		sequel.addAll( Arrays.asList( createViewGen.generateSql( createViewStmt, database, null ) ) );

		return sequel.toArray( new Sql[0] );
	}
}
