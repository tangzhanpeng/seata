/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.server.console.impl.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Resource;
import javax.sql.DataSource;

import io.seata.common.exception.StoreException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.IOUtil;
import io.seata.common.util.PageUtil;
import io.seata.common.util.StringUtils;
import io.seata.core.console.param.GlobalSessionParam;
import io.seata.core.console.result.PageResult;
import io.seata.core.console.vo.BranchSessionVO;
import io.seata.core.console.vo.GlobalSessionVO;
import io.seata.core.store.db.DataSourceProvider;
import io.seata.core.store.db.sql.log.LogStoreSqlsFactory;
import io.seata.server.console.service.BranchSessionService;
import io.seata.server.console.service.GlobalSessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Global Session DataBase ServiceImpl
 *
 * @author: zhongxiang.wang
 * @author: lvekee 734843455@qq.com
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("#{'db'.equals('${sessionMode}')}")
public class GlobalSessionDBServiceImpl implements GlobalSessionService {

    @Value("#{environment.getProperty('seata.store.db.global-table')}")
    private String globalTable;
    @Value("#{environment.getProperty('seata.store.db.db-type')}")
    private String dbType;
    @Value("#{environment.getProperty('seata.store.db.datasource')}")
    private String dbDataSource;

    @Resource(type = BranchSessionService.class)
    private BranchSessionService branchSessionService;

    @Override
    public PageResult<GlobalSessionVO> query(GlobalSessionParam param) {
        PageUtil.checkParam(param.getPageNum(), param.getPageSize());

        List<Object> sqlParamList = new ArrayList<>();
        String whereCondition = getWhereConditionByParam(param, sqlParamList);

        String sourceSql = LogStoreSqlsFactory.getLogStoreSqls(dbType).getAllGlobalSessionSql(globalTable, whereCondition);
        String querySessionSql = PageUtil.pageSql(sourceSql, dbType, param.getPageNum(), param.getPageSize());
        String sessionCountSql = PageUtil.countSql(sourceSql, dbType);

        List<GlobalSessionVO> list = new ArrayList<>();
        int count = 0;

        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, dbDataSource).provide();

        ResultSet rs = null;
        ResultSet countRs = null;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(querySessionSql);
             PreparedStatement countPs = conn.prepareStatement(sessionCountSql)) {
            PageUtil.setObject(ps, sqlParamList);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(GlobalSessionVO.convert(rs));
            }

            PageUtil.setObject(countPs, sqlParamList);
            countRs = countPs.executeQuery();
            if (countRs.next()) {
                count = countRs.getInt(1);
            }
            if (param.isWithBranch()) {
                for (GlobalSessionVO globalSessionVO : list) {
                    PageResult<BranchSessionVO> pageResp = branchSessionService.queryByXid(globalSessionVO.getXid());
                    globalSessionVO.setBranchSessionVOs(new HashSet<>(pageResp.getData()));
                }
            }
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(rs, countRs);
        }
        return PageResult.success(list, count, param.getPageNum(), param.getPageSize());
    }

    private String getWhereConditionByParam(GlobalSessionParam param, List<Object> sqlParamList) {
        StringBuilder whereConditionBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(param.getXid())) {
            whereConditionBuilder.append(" and xid = ? ");
            sqlParamList.add(param.getXid());
        }
        if (StringUtils.isNotBlank(param.getApplicationId())) {
            whereConditionBuilder.append(" and application_id = ? ");
            sqlParamList.add(param.getApplicationId());
        }
        if (param.getStatus() != null) {
            whereConditionBuilder.append(" and status = ? ");
            sqlParamList.add(param.getStatus());
        }
        if (StringUtils.isNotBlank(param.getTransactionName())) {
            whereConditionBuilder.append(" and transaction_name = ? ");
            sqlParamList.add(param.getTransactionName());
        }
        if (param.getTimeStart() != null) {
            whereConditionBuilder.append(" and gmt_create >= ? ");
            sqlParamList.add(param.getTimeStart());
        }
        if (param.getTimeEnd() != null) {
            whereConditionBuilder.append(" and gmt_create <= ? ");
            sqlParamList.add(param.getTimeEnd());
        }
        String whereCondition = whereConditionBuilder.toString();
        return whereCondition.replaceFirst("and", "where");
    }

}
