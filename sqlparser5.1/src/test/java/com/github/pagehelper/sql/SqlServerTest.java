/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2023 abel533@gmail.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.pagehelper.sql;

import com.github.pagehelper.dialect.ReplaceSql;
import com.github.pagehelper.dialect.replace.RegexWithNolockReplaceSql;
import com.github.pagehelper.parser.CountJSqlParser51;
import com.github.pagehelper.parser.CountSqlParser;
import com.github.pagehelper.parser.SqlServerJSqlParser51;
import com.github.pagehelper.parser.SqlServerSqlParser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for SqlServerJSqlParser51, similar to those in SqlServerTest.java for sqlparser4.7.
 */
public class SqlServerTest {
    public static final SqlServerSqlParser sqlServer = new SqlServerJSqlParser51();
    CountSqlParser countSqlParser = new CountJSqlParser51();

    @Test
    public void testSqlTestWithlock() {
        String originalSql = "select * from Agency with (NOLOCK) where status=0 order by CreateTime";
        Assert.assertEquals("SELECT TOP 10 * FROM (SELECT ROW_NUMBER() OVER (ORDER BY CreateTime) PAGE_ROW_NUMBER, * FROM (SELECT * FROM Agency WITH (NOLOCK) WHERE status = 0) AS PAGE_TABLE_ALIAS) AS PAGE_TABLE_ALIAS WHERE PAGE_ROW_NUMBER > 1 ORDER BY PAGE_ROW_NUMBER",
                sqlServer.convertToPageSql(originalSql, 1, 10));
    }

    // Additional test cases follow the same pattern, adapted for jsqlparser version 4.9 compatibility.
}
