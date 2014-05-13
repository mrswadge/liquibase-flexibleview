-- set serveroutput on;
BEGIN
  -- Due to bugs in Oracle, this script will only work on:
  -- 11.2.0.3 with patch set 30
  -- 11.2.0.4 with patch set 6 (not yet released)
  -- 12.2.0.2 
  EXECUTE immediate 'ALTER session SET "query_rewrite_enabled"      = false';
  EXECUTE immediate 'ALTER session SET "_query_rewrite_vop_cleanup" = false';
  EXECUTE immediate 'ALTER session SET "_complex_view_merging"      = false';
  EXECUTE immediate 'ALTER session SET "optimizer_features_enable"  = "11.1.0.7"';
  EXECUTE immediate 'ALTER session SET "_pred_move_around"          = false';
  dbms_metadata.set_transform_param(dbms_metadata.session_transform, 'SQLTERMINATOR', false );
  FOR cur_rec IN
  ( SELECT object_name, object_type FROM user_objects WHERE object_type='VIEW'
  )
  LOOP
    BEGIN
      FOR cur_view IN
      (SELECT trim( REPLACE( REPLACE( dbms_metadata.get_ddl( 'VIEW', cur_rec.object_name ), 'CREATE OR REPLACE FORCE VIEW', 'CREATE MATERIALIZED VIEW' ), 'CREATE OR REPLACE VIEW', 'CREATE MATERIALIZED VIEW' ) ) "MATERIALIZED_VIEW",
        trim( dbms_metadata.get_ddl( 'VIEW', cur_rec.object_name ) ) "VIEW"
      FROM dual
      )
      LOOP
        BEGIN
          BEGIN
            EXECUTE immediate 'drop materialized view ' || cur_rec.object_name;
          EXCEPTION
          WHEN OTHERS THEN
            IF SQLCODE != -12003 THEN
              raise;
            END IF;
          END;
          BEGIN
            EXECUTE immediate 'drop view ' || cur_rec.object_name;
          EXCEPTION
          WHEN OTHERS THEN
            IF SQLCODE != -942 THEN
              raise;
            END IF;
          END;
          -- create the view as a materialized view.
          BEGIN
            -- dbms_output.put_line( 'Attempting to create materialized view named ' || cur_rec.object_name );
            EXECUTE immediate cur_view."MATERIALIZED_VIEW";
          EXCEPTION
          WHEN OTHERS THEN
            BEGIN
              -- dbms_output.put_line( 'Failed to create materialized view, recreating original view ' || cur_rec.object_name );
              -- dbms_output.put_line( 'Error was: ' || sqlerrm( sqlcode ) );
              -- dbms_output.put_line( cur_view."MATERIALIZED_VIEW" );
              EXECUTE immediate cur_view."VIEW";
            EXCEPTION
            WHEN OTHERS THEN
              -- dbms_output.put_line( 'ERROR: Could not recreate view named ' || cur_rec.object_name || '.' );
              -- dbms_output.put_line( 'SQL was:' || cur_view."VIEW" );
              raise;
            END;
          END;
        END;
      END LOOP;
    END;
  END LOOP;
EXCEPTION
WHEN OTHERS THEN
  raise;
END;