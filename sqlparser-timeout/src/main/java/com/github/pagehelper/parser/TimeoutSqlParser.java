package com.github.pagehelper.parser;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;

import java.util.concurrent.*;

/**
 * 默认超时10秒，想要控制需要自己实现
 */
public class TimeoutSqlParser implements SqlParser {

    ExecutorService executorService = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors() * 2);

    @Override
    public Statement parse(String sql) throws JSQLParserException {
        CCJSqlParser parser = CCJSqlParserUtil.newParser(sql);
        parser.withSquareBracketQuotation(true);
        Statement statement;
        Future<Statement> future = executorService.submit(() -> parser.Statement());
        try {
            statement = future.get(100000, TimeUnit.MILLISECONDS);
            return statement;
        } catch (TimeoutException var5) {
            parser.interrupted = true;
            future.cancel(true);
            throw new JSQLParserException("Time out occurred.", var5);
        } catch (Exception var6) {
            throw new JSQLParserException(var6);
        }
    }

}
