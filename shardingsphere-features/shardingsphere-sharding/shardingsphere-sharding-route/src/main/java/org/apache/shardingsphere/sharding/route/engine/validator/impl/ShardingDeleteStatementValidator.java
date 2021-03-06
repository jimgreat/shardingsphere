/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.sharding.route.engine.validator.impl;

import org.apache.shardingsphere.infra.exception.ShardingSphereException;
import org.apache.shardingsphere.sharding.route.engine.validator.ShardingStatementValidator;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.apache.shardingsphere.sharding.rule.TableRule;
import org.apache.shardingsphere.sql.parser.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.sql.parser.binder.type.TableAvailable;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.AndPredicate;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.DeleteStatement;

import java.util.List;
import java.util.Optional;

/**
 * Sharding delete statement validator.
 */
public final class ShardingDeleteStatementValidator implements ShardingStatementValidator<DeleteStatement> {

    @Override
    public void validate(final ShardingRule shardingRule, final SQLStatementContext<DeleteStatement> sqlStatementContext, final List<Object> parameters) {
        if (1 != ((TableAvailable) sqlStatementContext).getAllTables().size()) {
            throw new ShardingSphereException("Cannot support Multiple-Table for '%s'.", sqlStatementContext.getSqlStatement());
        }
        boolean hasShardingColumn = false;
        DeleteStatement sqlStatement = sqlStatementContext.getSqlStatement();
        String tableName = sqlStatement.getTables().iterator().next().getTableName().getIdentifier().getValue();
        if (sqlStatement.getWhere().isPresent()) {
            for (AndPredicate each : sqlStatement.getWhere().get().getAndPredicates()) {
                hasShardingColumn = each.getPredicates().stream().anyMatch(predicate -> {
                    String columnName = predicate.getColumn().getIdentifier().getValue();
                    return shardingRule.isShardingColumn(columnName, tableName);
                });
                if (hasShardingColumn) {
                    break;
                }
            }
        }

        Optional<TableRule> tableRuleOptional = shardingRule.findTableRule(tableName);
        if (tableRuleOptional.isPresent()) {
            if (tableRuleOptional.get().isForceShardingColumn() && !hasShardingColumn) {
                throw new ShardingSphereException("Must have sharding column, logic table: [%s]", tableName);
            }
        }
    }
}
