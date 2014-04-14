begin
  dbms_metadata.set_transform_param(dbms_metadata.session_transform, 'SQLTERMINATOR', false );
  for cur_rec in ( SELECT object_name, object_type from user_objects where object_type='VIEW' )
  loop
    begin
      for cur_view in
      (select trim( replace( replace( dbms_metadata.get_ddl( 'VIEW', cur_rec.object_name ), 'CREATE OR REPLACE FORCE VIEW', 'CREATE MATERIALIZED VIEW' ), 'CREATE OR REPLACE VIEW', 'CREATE MATERIALIZED VIEW' ) ) "MATERIALIZED_VIEW",
        trim( dbms_metadata.get_ddl( 'VIEW', cur_rec.object_name ) ) "VIEW"
      from dual )
      loop
        begin
          begin
            execute immediate 'drop materialized view ' || cur_rec.object_name;
          exception
          when others then
            if sqlcode != -12003 then
              raise;
            end if;
          end;
          begin
            execute immediate 'drop view ' || cur_rec.object_name;
          exception
          when others then
            if sqlcode != -942 then
              raise;
            end if;
          end;
          -- create the view as a materialized view.
          begin
            --dbms_output.put_line( 'Attempting to create materialized view named ' || cur_rec.object_name );
            execute immediate cur_view."MATERIALIZED_VIEW";
          exception
          when others then
            begin
              --dbms_output.put_line( 'Failed to create materialized view, recreating original view ' || cur_rec.object_name );
              --dbms_output.put_line( 'Error was: ' || sqlerrm( sqlcode ) );
              --dbms_output.put_line( cur_view."MATERIALIZED_VIEW" );
              execute immediate cur_view."VIEW";
            exception
            when others then
              --dbms_output.put_line( 'ERROR: Could not recreate view named ' || cur_rec.object_name || '.' );
              --dbms_output.put_line( 'SQL was:' || cur_view."VIEW" );
              raise;
            end;
          end;
        end;
      end loop;
    end;
  end loop;
end;