package com.github.pagehelper.sql;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.feature.Feature;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Ignore
public class SqlParserTest {
    public static final String sql = "select * " +
            "  from (select distinct A.USERID, " +
            "                        A.USERCODE, " +
            "                        A.USERNAME, " +
            "                        A.USERPWD, " +
            "                        A.CREATEDATE, " +
            "                        A.UPDATEDATE, " +
            "                        A.USERSTATE, " +
            "                        A.MEMO, " +
            "                        A.USERPHONE, " +
            "                        A.USEREMAIL, " +
            "                        A.IDCARD, " +
            "                        D.DEPTNAME, " +
            "                        D.DEPTID " +
            "          from BASE_SYS_USER A " +
            "          left JOIN BASE_SYS_ROLE_USER_REL B " +
            "            ON A.USERID = B.USERID " +
            "          left JOIN BASE_SYS_ROLE C " +
            "            on C.ROLEID = B.ROLEID " +
            "          left join BASE_SYS_DEPT_USER_REL REL " +
            "            on REL.USERID = A.USERID " +
            "          left join BASE_SYS_DEPT D " +
            "            on D.DEPTID = REL.DEPTID " +
            "         where 1 = 1 " +
            "           and C.ROLEID = ? " +
            "           and D.DEPTID = ? " +
            "           and A.USERNAME LIKE '%heh%' " +
            "           and A.USERSTATE = ? " +
            "         ) A Order by A.createDate desc";

    @Test
    public void testParse() throws InterruptedException {
        long start = System.currentTimeMillis();
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 10000; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Statement statement = CCJSqlParserUtil.parse(sql, parser -> parser.withSquareBracketQuotation(true));
                        if (statement instanceof Select) {
                            Select select = (Select) statement;
                            print(select);
                        }
                    } catch (JSQLParserException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
        System.out.println("cost: " + (System.currentTimeMillis() - start));
    }

    @Test
    public void testParse2() throws InterruptedException {
        long start = System.currentTimeMillis();
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        for (int i = 0; i < 10000; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Statement statement = CCJSqlParserUtil.parse(sql, parser -> parser.withSquareBracketQuotation(true));
                        if (statement instanceof Select) {
                            Select select = (Select) statement;
                            print(select);
                        }
                    } catch (JSQLParserException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
        System.out.println("cost: " + (System.currentTimeMillis() - start));
    }


    @Test
    public void testParse3() throws InterruptedException {
        long start = System.currentTimeMillis();
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        ExecutorService executorService2 = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors());
        for (int i = 0; i < 10000; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        CCJSqlParser sqlParser = CCJSqlParserUtil.newParser(sql);
                        Future<Statement> future = executorService2.submit(() -> sqlParser.Statement());
                        Statement statement = future.get(sqlParser.getConfiguration().getAsInteger(Feature.timeOut), TimeUnit.MILLISECONDS);
                        if (statement instanceof Select) {
                            Select select = (Select) statement;
                            print(select);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
        executorService2.shutdown();
        System.out.println("cost: " + (System.currentTimeMillis() - start));
    }

    @Test
    public void testParse3_2() throws InterruptedException {
        long start = System.currentTimeMillis();
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        ExecutorService executorService2 = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors() * 2);
        for (int i = 0; i < 10000; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        CCJSqlParser sqlParser = CCJSqlParserUtil.newParser(sql);
                        Future<Statement> future = executorService2.submit(() -> sqlParser.Statement());
                        Statement statement = future.get(sqlParser.getConfiguration().getAsInteger(Feature.timeOut), TimeUnit.MILLISECONDS);
                        if (statement instanceof Select) {
                            Select select = (Select) statement;
                            print(select);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
        executorService2.shutdown();
        System.out.println("cost: " + (System.currentTimeMillis() - start));
    }

    @Test
    public void testParse4() throws InterruptedException {
        long start = System.currentTimeMillis();
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        ExecutorService executorService2 = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors());
        for (int i = 0; i < 10000; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        CCJSqlParser sqlParser = CCJSqlParserUtil.newParser(sql);
                        Future<Statement> future = executorService2.submit(() -> sqlParser.Statement());
                        Statement statement = future.get(sqlParser.getConfiguration().getAsInteger(Feature.timeOut), TimeUnit.MILLISECONDS);
                        if (statement instanceof Select) {
                            Select select = (Select) statement;
                            print(select);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
        executorService2.shutdown();
        System.out.println("cost: " + (System.currentTimeMillis() - start));
    }

    @Test
    public void testParse4_2() throws InterruptedException {
        long start = System.currentTimeMillis();
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        ExecutorService executorService2 = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors() * 2);
        for (int i = 0; i < 10000; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        CCJSqlParser sqlParser = CCJSqlParserUtil.newParser(sql);
                        Future<Statement> future = executorService2.submit(() -> sqlParser.Statement());
                        Statement statement = future.get(sqlParser.getConfiguration().getAsInteger(Feature.timeOut), TimeUnit.MILLISECONDS);
                        if (statement instanceof Select) {
                            Select select = (Select) statement;
                            print(select);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
        executorService2.shutdown();
        System.out.println("cost: " + (System.currentTimeMillis() - start));
    }

    void print(Select select) {

    }
}
