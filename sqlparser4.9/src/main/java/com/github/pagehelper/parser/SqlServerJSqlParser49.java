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

package com.github.pagehelper.parser;

import com.github.pagehelper.page.PageMethod;
import com.github.pagehelper.PageException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

import java.util.*;

/**
 * 将sqlserver查询语句转换为分页语句<br>
 * 注意事项：<br>
 * <ol>
 * <li>请先保证你的SQL可以执行</li>
 * <li>sql中最好直接包含order by，可以自动从sql提取</li>
 * <li>如果没有order by，可以通过入参提供，但是需要自己保证正确</li>
 * <li>如果sql有order by，可以通过orderby参数覆盖sql中的order by</li>
 * <li>order by的列名不能使用别名</li>
 * <li>表和列使用别名的时候不要使用单引号(')</li>
 * </ol>
 * 该类设计为一个独立的工具类，依赖jsqlparser,可以独立使用
 *
 * @author liuzh
 */
public class SqlServerJSqlParser49 implements SqlServerSqlParser {
    //开始行号
    public static final String START_ROW = String.valueOf(Long.MIN_VALUE);
    //结束行号
    public static final String PAGE_SIZE = String.valueOf(Long.MAX_VALUE);
    //外层包装表
    protected static final String WRAP_TABLE = "WRAP_OUTER_TABLE";
    //表别名名字
    protected static final String PAGE_TABLE_NAME = "PAGE_TABLE_ALIAS";
    //protected
    public static final Alias PAGE_TABLE_ALIAS = new Alias(PAGE_TABLE_NAME);
    //行号
    protected static final String PAGE_ROW_NUMBER = "PAGE_ROW_NUMBER";
    //行号列
    protected static final Column PAGE_ROW_NUMBER_COLUMN = new Column(PAGE_ROW_NUMBER);
    //TOP 100 PERCENT
    protected static final Top TOP100_PERCENT;
    //别名前缀
    protected static final String PAGE_COLUMN_ALIAS_PREFIX = "ROW_ALIAS_";

    //静态方法处理
    static {
        TOP100_PERCENT = new Top();
        TOP100_PERCENT.setExpression(new LongValue(100));
        TOP100_PERCENT.setPercentage(true);
    }

    /**
     * 转换为分页语句
     *
     * @param sql
     * @param offset
     * @param limit
     * @return
     */
    public String convertToPageSql(String sql, Integer offset, Integer limit) {
        //解析SQL
        Statement stmt;
        try {
            stmt = SqlParserUtil.parse(sql);
        } catch (Throwable e) {
            throw new PageException("The SQL statement cannot be converted to a pagination query!", e);
        }
        if (!(stmt instanceof Select)) {
            throw new PageException("the pagination statement must be a select query!");
        }
        //获取分页查询的select
        Select pageSelect = getPageSelect((Select) stmt);
        String pageSql = pageSelect.toString();
        //缓存移到外面了，所以不替换参数
        if (offset != null) {
            pageSql = pageSql.replace(START_ROW, String.valueOf(offset));
        }
        if (limit != null) {
            pageSql = pageSql.replace(PAGE_SIZE, String.valueOf(limit));
        }
        return pageSql;
    }

    /**
     * 获取一个外层包装的TOP查询
     *
     * @param select
     * @return
     */
    protected Select getPageSelect(Select select) {
        SelectBody selectBody = select.getSelectBody();
        if (selectBody instanceof SetOperationList) {
            selectBody = wrapSetOperationList((SetOperationList) selectBody);
        }
        //这里的selectBody一定是PlainSelect
        if (((PlainSelect) selectBody).getTop() != null) {
            throw new PageException("The pagination statement already contains the top, and can no longer be used to query the pagination plugin!");
        }
        //获取查询列
        List<SelectItem> selectItems = getSelectItems((PlainSelect) selectBody);
        //对一层的SQL增加ROW_NUMBER()
        List<SelectItem> autoItems = new ArrayList<SelectItem>();
        SelectItem orderByColumn = addRowNumber((PlainSelect) selectBody, autoItems);
        //加入自动生成列
        ((PlainSelect) selectBody).addSelectItems(autoItems.toArray(new SelectItem[autoItems.size()]));
        //处理子语句中的order by
        processSelectBody(selectBody, 0);

        //中层子查询
        PlainSelect innerSelectBody = new PlainSelect();
        //PAGE_ROW_NUMBER
        innerSelectBody.addSelectItems(orderByColumn);
        innerSelectBody.addSelectItems(selectItems.toArray(new SelectItem[selectItems.size()]));
        //将原始查询作为内层子查询
        SubSelect fromInnerItem = new SubSelect();
        fromInnerItem.setSelectBody(selectBody);
        fromInnerItem.setAlias(PAGE_TABLE_ALIAS);
        innerSelectBody.setFromItem(fromInnerItem);

        //新建一个select
        Select newSelect = new Select();
        PlainSelect newSelectBody = new PlainSelect();
        //设置top
        Top top = new Top();
        top.setExpression(new LongValue(Long.MAX_VALUE));
        newSelectBody.setTop(top);
        //设置order by
        List<OrderByElement> orderByElements = new ArrayList<OrderByElement>();
        OrderByElement orderByElement = new OrderByElement();
        orderByElement.setExpression(PAGE_ROW_NUMBER_COLUMN);
        orderByElements.add(orderByElement);
        newSelectBody.setOrderByElements(orderByElements);
        //设置where
        GreaterThan greaterThan = new GreaterThan();
        greaterThan.setLeftExpression(PAGE_ROW_NUMBER_COLUMN);
        greaterThan.setRightExpression(new LongValue(Long.MIN_VALUE));
        newSelectBody.setWhere(greaterThan);
        //设置selectItems
        newSelectBody.setSelectItems(selectItems);
        //设置fromIterm
        SubSelect fromItem = new SubSelect();
        fromItem.setSelectBody(innerSelectBody); //中层子查询
        fromItem.setAlias(PAGE_TABLE_ALIAS);
        newSelectBody.setFromItem(fromItem);

        newSelect.setSelectBody(newSelectBody);
        if (isNotEmptyList(select.getWithItemsList())) {
            newSelect.setWithItemsList(select.getWithItemsList());
        }
        return newSelect;
    }

    /**
     * 包装SetOperationList
     *
     * @param setOperationList
     * @return
     */
    protected SelectBody wrapSetOperationList(SetOperationList setOperationList) {
        //获取最后一个plainSelect
        SelectBody setSelectBody = setOperationList.getSelects().get(setOperationList.getSelects().size() - 1);
        if (!(setSelectBody instanceof PlainSelect)) {
            throw new PageException("Unable to process the SQL, you can submit issues in GitHub for help.!");
        }
        PlainSelect plainSelect = (PlainSelect) setSelectBody;
        PlainSelect selectBody = new PlainSelect();
        List<SelectItem> selectItems = getSelectItems(plainSelect);
        selectBody.setSelectItems(selectItems);

        //设置fromIterm
        SubSelect fromItem = new SubSelect();
        fromItem.setSelectBody(setOperationList);
        fromItem.setAlias(new Alias(WRAP_TABLE));
        selectBody.setFromItem(fromItem);
        //order by
        if (isNotEmptyList(setOperationList.getOrderByElements())) {
            selectBody.setOrderByElements(setOperationList.getOrderByElements());
            setOperationList.setOrderByElements(null);
        }
        return selectBody;
    }

    /**
     * 获取查询列
     *
     * @param plainSelect
     * @return
     */
    protected List<SelectItem> getSelectItems(PlainSelect plainSelect) {
        //设置selectItems
        List<SelectItem> selectItems = new ArrayList<SelectItem>();
        for (SelectItem selectItem : plainSelect.getSelectItems()) {
            //别名需要特殊处理
            if (selectItem instanceof SelectExpressionItem) {
                SelectExpressionItem selectExpressionItem = (SelectExpressionItem) selectItem;
                if (selectExpressionItem.getAlias() != null) {
                    //直接使用别名
                    Column column = new Column(selectExpressionItem.getAlias().getName());
                    SelectExpressionItem expressionItem = new SelectExpressionItem(column);
                    selectItems.add(expressionItem);
                } else if (selectExpressionItem.getExpression() instanceof Column) {
                    Column column = (Column) selectExpressionItem.getExpression();
                    SelectExpressionItem item = null;
                    if (column.getTable() != null) {
                        Column newColumn = new Column(column.getColumnName());
                        item = new SelectExpressionItem(newColumn);
                        selectItems.add(item);
                    } else {
                        selectItems.add(selectItem);
                    }
                } else {
                    selectItems.add(selectItem);
                }
            } else if (selectItem instanceof AllTableColumns) {
                selectItems.add(new AllColumns());
            } else {
                selectItems.add(selectItem);
            }
        }
        // SELECT *, 1 AS alias FROM TEST
        // 应该为
        // SELECT * FROM (SELECT *, 1 AS alias FROM TEST)
        // 不应该为
        // SELECT *, alias FROM (SELECT *, 1 AS alias FROM TEST)
        for (SelectItem selectItem : selectItems) {
            if (selectItem instanceof AllColumns) {
                return Collections.singletonList(selectItem);
            }
        }
        return selectItems;
    }

    /**
     * 获取 ROW_NUMBER() 列
     *
     * @param plainSelect 原查询
     * @param autoItems   自动生成的查询列
     * @return ROW_NUMBER() 列
     */
    protected SelectItem addRowNumber(PlainSelect plainSelect, List<SelectItem> autoItems) {
        //增加ROW_NUMBER()
        StringBuilder orderByBuilder = new StringBuilder();
        orderByBuilder.append("ROW_NUMBER() OVER (");
        if (isNotEmptyList(plainSelect.getOrderByElements())) {
            orderByBuilder.append(PlainSelect.orderByToString(
                    getOrderByElements(plainSelect, autoItems)).substring(1));
            //清空排序列表
            plainSelect.setOrderByElements(null);
        } else {
            orderByBuilder.append("ORDER BY RAND()");
        }
        orderByBuilder.append(") ");
        orderByBuilder.append(PAGE_ROW_NUMBER);
        return new SelectExpressionItem(new Column(orderByBuilder.toString()));
    }

    /**
     * 处理selectBody去除Order by
     *
     * @param selectBody
     */
    protected void processSelectBody(SelectBody selectBody, int level) {
        if (selectBody != null) {
            if (selectBody instanceof PlainSelect) {
                processPlainSelect((PlainSelect) selectBody, level + 1);
            } else if (selectBody instanceof WithItem) {
                WithItem withItem = (WithItem) selectBody;
                if (withItem.getSubSelect() != null && !keepSubSelectOrderBy()) {
                    processSelectBody(withItem.getSubSelect().getSelectBody(), level + 1);
                }
            } else {
                SetOperationList operationList = (SetOperationList) selectBody;
                if (operationList.getSelects() != null && operationList.getSelects().size() > 0) {
                    List<SelectBody> plainSelects = operationList.getSelects();
                    for (SelectBody plainSelect : plainSelects) {
                        processSelectBody(plainSelect, level + 1);
                    }
                }
                if (!orderByHashParameters(operationList.getOrderByElements())) {
                    operationList.setOrderByElements(null);
                }
            }
        }
    }

    /**
     * 处理PlainSelect类型的selectBody
     *
     * @param plainSelect
     */
    protected void processPlainSelect(PlainSelect plainSelect, int level) {
        if (!orderByHashParameters(plainSelect.getOrderByElements())) {
            plainSelect.setOrderByElements(null);
        }
        if (plainSelect.getFromItem() != null) {
            processFromItem(plainSelect.getFromItem());
        }
        if (plainSelect.getJoins() != null && plainSelect.getJoins().size() > 0) {
            List<Join> joins = plainSelect.getJoins();
            for (Join join : joins) {
                if (join.getRightItem() != null) {
                    processFromItem(join.getRightItem());
                }
            }
        }
    }

    /**
     * 处理子查询
     *
     * @param fromItem
     */
    protected void processFromItem(FromItem fromItem) {
        if (fromItem instanceof SubJoin) {
            SubJoin subJoin = (SubJoin) fromItem;
            if (subJoin.getJoinList() != null && subJoin.getJoinList().size() > 0) {
                for (Join join : subJoin.getJoinList()) {
                    if (join.getRightItem() != null) {
                        processFromItem(join.getRightItem());
                    }
                }
            }
            if (subJoin.getLeft() != null) {
                processFromItem(subJoin.getLeft());
            }
        } else if (fromItem instanceof SubSelect) {
            SubSelect subSelect = (SubSelect) fromItem;
            if (subSelect.getSelectBody() != null && !keepSubSelectOrderBy()) {
                processSelectBody(subSelect.getSelectBody(), level + 1);
            }
        } else if (fromItem instanceof ValuesList) {

        } else if (fromItem instanceof LateralSubSelect) {
            LateralSubSelect lateralSubSelect = (LateralSubSelect) fromItem;
            if (lateralSubSelect.getSubSelect() != null) {
                SubSelect subSelect = lateralSubSelect.getSubSelect();
                if (subSelect.getSelectBody() != null && !keepSubSelectOrderBy()) {
                    processSelectBody(subSelect.getSelectBody(), level + 1);
                }
            }
        }
        //Table时不用处理
    }

    /**
     * 保留 order by
     */
    protected boolean keepOrderBy() {
        return PageMethod.getLocalPage() != null && PageMethod.getLocalPage().keepOrderBy();
    }

    /**
     * 保留子查询 order by
     */
    protected boolean keepSubSelectOrderBy() {
        return PageMethod.getLocalPage() != null && PageMethod.getLocalPage().keepSubSelectOrderBy();
    }

    /**
     * 判断Orderby是否包含参数，有参数的不能去
     *
     * @param orderByElements
     * @return
     */
    public boolean orderByHashParameters(List<OrderByElement> orderByElements) {
        if (orderByElements == null) {
            return false;
        }
        for (OrderByElement orderByElement : orderByElements) {
            if (orderByElement.toString().contains("?")) {
                return true;
            }
        }
        return false;
    }
}
