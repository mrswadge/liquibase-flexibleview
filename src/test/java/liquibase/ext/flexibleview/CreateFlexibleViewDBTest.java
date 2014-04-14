package liquibase.ext.flexibleview;

import org.dbunit.Assertion;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.Before;
import org.junit.Test;

import liquibase.Contexts;
import liquibase.ext.flexibleview.testcore.BaseTestCase;

/**
 * Copied from the Oracle Create Materialized View addon, although I'm not entirely sure of the aim
 * of this test.
 * @author sbs
 *
 */
public class CreateFlexibleViewDBTest extends BaseTestCase {

	private IDataSet loadedDataSet;
	private final String TABLE_NAME = "mytabledb";

	@Before
	public void setUp() throws Exception {
		changeLogFile = "liquibase/ext/flexibleview/changelog.test.xml";
		connectToDB();
		cleanDB();
	}

	protected IDatabaseConnection getConnection() throws Exception {
		return new DatabaseConnection( connection );
	}

	protected IDataSet getDataSet() throws Exception {
		loadedDataSet = new FlatXmlDataSet( this.getClass().getClassLoader().getResourceAsStream( "liquibase/ext/flexibleview/input.xml" ) );
		return loadedDataSet;
	}

	@Test
	public void testCompare() throws Exception {
		QueryDataSet actualDataSet = new QueryDataSet( getConnection() );

		liquiBase.update( new Contexts() );
		actualDataSet.addTable( TABLE_NAME, "SELECT * from " + TABLE_NAME );
		loadedDataSet = getDataSet();

		Assertion.assertEquals( loadedDataSet, actualDataSet );
	}

}
