package liquibase.ext.flexibleview;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import liquibase.database.Database;
import liquibase.database.core.OracleDatabase;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.exception.ValidationErrors;
import liquibase.logging.LogFactory;
import liquibase.logging.Logger;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.sqlgenerator.core.RawSqlGenerator;
import liquibase.statement.core.RawSqlStatement;

public class ConvertViewsIntoMaterializedViewsGenerator extends AbstractSqlGenerator<ConvertViewsIntoMaterializedViewsStatement> {
	
	private static final Logger log = LogFactory.getLogger();
	
	@Override
	public boolean supports( ConvertViewsIntoMaterializedViewsStatement statement, Database database ) {
		return database instanceof OracleDatabase;
	}

	public ValidationErrors validate( ConvertViewsIntoMaterializedViewsStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain ) {
		return new ValidationErrors();
	}

	public Sql[] generateSql( ConvertViewsIntoMaterializedViewsStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain ) {
		String sql; 

		List<Sql> sqlList = new ArrayList<Sql>();
		RawSqlGenerator rawSqlGen = new RawSqlGenerator();
		
		sql = readSqlFile( "liquibase/ext/flexibleview/createViewDependencyGraphTempTable.sql" );
		sqlList.addAll( Arrays.asList( rawSqlGen.generateSql( new RawSqlStatement( sql, "" ), database, null ) ) );

		sql = readSqlFile( "liquibase/ext/flexibleview/convertViewsToMaterializedViews.sql" );
		sqlList.addAll( Arrays.asList( rawSqlGen.generateSql( new RawSqlStatement( sql, "" ), database, null ) ) );
		
		sql = readSqlFile( "liquibase/ext/flexibleview/dropViewDependencyGraphTempTable.sql" );
		sqlList.addAll( Arrays.asList( rawSqlGen.generateSql( new RawSqlStatement( sql, "" ), database, null ) ) );

		return sqlList.toArray( new Sql[0] );
	}
	
	private String readSqlFile( String location ) {
		ClassLoader loader = ConvertViewsIntoMaterializedViewsGenerator.class.getClassLoader();
		InputStream in = loader.getResourceAsStream( location );
		StringWriter sw = new StringWriter();
		Scanner s = new Scanner( in ).useDelimiter( "\\A" );
		String sql;
		if ( s.hasNext() ) {
			sql = s.next();
		} else {
			throw new UnexpectedLiquibaseException( "Could not locate the view conversion SQL." );
		}
		return sql;
	}
}
