/*
 * Copyright 2019-2020 The Polypheny Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.polypheny.db.webui;


import java.util.Arrays;
import java.util.HashMap;
import org.junit.Assert;
import org.junit.Test;
import org.polypheny.db.catalog.Catalog.SchemaType;
import org.polypheny.db.catalog.Catalog.TableType;
import org.polypheny.db.catalog.entity.CatalogColumn;
import org.polypheny.db.catalog.entity.CatalogDatabase;
import org.polypheny.db.catalog.entity.CatalogDefaultValue;
import org.polypheny.db.catalog.entity.CatalogKey;
import org.polypheny.db.catalog.entity.CatalogSchema;
import org.polypheny.db.catalog.entity.CatalogTable;
import org.polypheny.db.catalog.entity.CatalogUser;
import org.polypheny.db.catalog.entity.combined.CatalogCombinedTable;
import org.polypheny.db.type.PolyType;


public class SchemaToJsonMapperTest {


    private static final String mockJson = "{\"tableName\":\"stores\",\"columns\":[{\"columnName\":\"sid\",\"type\":\"INTEGER\",\"nullable\":false},{\"columnName\":\"name\",\"type\":\"VARCHAR\",\"length\":50,\"nullable\":false},{\"columnName\":\"location\",\"type\":\"VARCHAR\",\"length\":30,\"nullable\":true,\"defaultValue\":\"Basel\"}],\"primaryKeyColumnNames\":[\"sid\",\"name\"]}";


    @Test
    public void exportTest() {
        CatalogCombinedTable catalogCombinedTable = new CatalogCombinedTable(
                new CatalogTable( 4, "stores", 1, "public", 1, "APP", 1, "hans", TableType.TABLE, "", 23L ),
                Arrays.asList(
                        new CatalogColumn( 5, "sid", 4, "stores", 1, "public", 1, "APP", 1, PolyType.INTEGER, null, null, false, null, null ),
                        new CatalogColumn( 6, "name", 4, "stores", 1, "public", 1, "APP", 2, PolyType.VARCHAR, 50, null, false, null, null ),
                        new CatalogColumn( 7, "location", 4, "stores", 1, "public", 1, "APP", 3, PolyType.VARCHAR, 30, null, true, null, new CatalogDefaultValue( 7, PolyType.VARCHAR, "Basel", null ) )
                ),
                new CatalogSchema( 1, "public", 1, "APP", 1, "hans", SchemaType.RELATIONAL ),
                new CatalogDatabase( 1, "APP", 1, "hans", 1L, "public" ),
                new CatalogUser( 1, "hans", "secrete" ),
                new HashMap<>(),
                new HashMap<>(),
                Arrays.asList(
                        new CatalogKey( 23L, 4, "stores", 1, "public", 1, "APP", Arrays.asList( 5L, 6L ), Arrays.asList( "sid", "name" ) ),
                        new CatalogKey( 24L, 4, "stores", 1, "public", 1, "APP", Arrays.asList( 6L ), Arrays.asList( "name" ) )
                )
        );
        String json = SchemaToJsonMapper.exportTableDefinitionAsJson( catalogCombinedTable, true, true );
        Assert.assertEquals( json, mockJson );
    }


    @Test
    public void getStatementTest() {
        String statement = SchemaToJsonMapper.getCreateTableStatementFromJson( mockJson, true, true, "foo", null, "hsqldb1" );
        final String expected1 = "CREATE TABLE \"foo\".\"stores\" (\"sid\" INTEGER NOT NULL, \"name\" VARCHAR(50) NOT NULL, \"location\" VARCHAR(30) DEFAULT 'Basel', PRIMARY KEY(\"sid\", \"name\")) ON STORE \"hsqldb1\"";
        Assert.assertEquals( statement, expected1 );

        statement = SchemaToJsonMapper.getCreateTableStatementFromJson( mockJson, false, false, "foo", null, null );
        final String expected2 = "CREATE TABLE \"foo\".\"stores\" (\"sid\" INTEGER NOT NULL, \"name\" VARCHAR(50) NOT NULL, \"location\" VARCHAR(30))";
        Assert.assertEquals( statement, expected2 );

        statement = SchemaToJsonMapper.getCreateTableStatementFromJson( mockJson, false, false, "foo", "bar", null );
        final String expected3 = "CREATE TABLE \"foo\".\"bar\" (\"sid\" INTEGER NOT NULL, \"name\" VARCHAR(50) NOT NULL, \"location\" VARCHAR(30))";
        Assert.assertEquals( statement, expected3 );
    }


    @Test
    public void getTableNameTest() {
        String name = SchemaToJsonMapper.getTableNameFromJson( mockJson );
        final String expected = "stores";
        Assert.assertEquals( name, expected );
    }

}