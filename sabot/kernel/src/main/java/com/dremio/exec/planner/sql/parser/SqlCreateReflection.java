/*
 * Copyright (C) 2017-2018 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.exec.planner.sql.parser;

import java.util.ArrayList;
import java.util.List;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSpecialOperator;
import org.apache.calcite.sql.parser.SqlParserPos;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class SqlCreateReflection extends SqlSystemCall {

  public static final SqlSpecialOperator OPERATOR = new SqlSpecialOperator("ADD_LAYOUT", SqlKind.OTHER_DDL) {
    @Override
    public SqlCall createCall(SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
      Preconditions.checkArgument(operands.length == 10, "SqlCreateReflection.createCall() has to get 10 operands!");
      return new SqlCreateReflection(
          pos,
          (SqlIdentifier) operands[0],
          operands[1],
          (SqlNodeList) operands[2],
          (SqlNodeList) operands[3],
          (SqlNodeList) operands[4],
          (SqlNodeList) operands[5],
          (SqlNodeList) operands[6],
          (SqlNodeList) operands[7],
          ((SqlLiteral) operands[8]).symbolValue(PartitionDistributionStrategy.class),
          (SqlIdentifier) operands[9]
          );
    }
  };

  private final SqlIdentifier tblName;
  private final SqlNode isRaw;
  private final SqlNodeList displayList;
  private final SqlNodeList dimensionList;
  private final SqlNodeList measureList;
  private final SqlNodeList distributionList;
  private final SqlNodeList partitionList;
  private final SqlNodeList sortList;
  private final PartitionDistributionStrategy partitionDistributionStrategy;
  private final SqlIdentifier name;

  private SqlCreateReflection(SqlParserPos pos, SqlIdentifier tblName, SqlNode isRaw, SqlNodeList displayList,
                              SqlNodeList dimensionList, SqlNodeList measureList, SqlNodeList distributionList, SqlNodeList partitionList,
                              SqlNodeList sortList, PartitionDistributionStrategy partitionDistributionStrategy, SqlIdentifier name) {
    super(pos);
    this.tblName = tblName;
    this.isRaw = isRaw;
    this.displayList = Objects.firstNonNull(displayList, SqlNodeList.EMPTY);
    this.dimensionList = Objects.firstNonNull(dimensionList, SqlNodeList.EMPTY);
    this.measureList = Objects.firstNonNull(measureList, SqlNodeList.EMPTY);
    this.distributionList = distributionList;
    this.partitionList = partitionList;
    this.sortList = sortList;
    this.partitionDistributionStrategy = partitionDistributionStrategy;
    this.name = name;
  }

  @Override
  public SqlOperator getOperator() {
    return OPERATOR;
  }

  @Override
  public List<SqlNode> getOperandList() {
    List<SqlNode> operands = new ArrayList<>();

    operands.addAll(ImmutableList.of(tblName, isRaw, displayList, dimensionList, measureList, distributionList, partitionList,
        sortList, SqlLiteral.createSymbol(partitionDistributionStrategy, SqlParserPos.ZERO)));

    operands.add(name);
    return operands;
  }

  public SqlIdentifier getTblName() {
    return tblName;
  }

  public boolean isRaw(){
    return ((SqlLiteral) this.isRaw).booleanValue();
  }

  public List<String> getDisplayList() {
    return toStrings(displayList);
  }

  public SqlIdentifier getName() {
    return name;
  }

  public List<NameAndGranularity> getDimensionList() {
    return toNameAndGranularity(dimensionList);
  }

  public List<String> getMeasureList() {
    return toStrings(measureList);
  }

  public List<String> getDistributionList() {
    return toStrings(distributionList);
  }

  public List<String> getPartitionList() {
    return toStrings(partitionList);
  }

  public List<String> getSortList() {
    return toStrings(sortList);
  }

  public PartitionDistributionStrategy getPartitionDistributionStrategy() {
    return partitionDistributionStrategy;
  }

  private List<NameAndGranularity> toNameAndGranularity(SqlNodeList list){
    if(list == null){
      return ImmutableList.of();
    }
    List<NameAndGranularity> columnNames = Lists.newArrayList();
    for(SqlNode node : list.getList()) {
      IdentifierWithGranularity ident = (IdentifierWithGranularity) node;
      NameAndGranularity value = new NameAndGranularity(ident.getSimple(), ident.getByDay() ? Granularity.BY_DAY : Granularity.NORMAL);
      columnNames.add(value);

    }
    return columnNames;
  }

  private List<String> toStrings(SqlNodeList list){
    if(list == null){
      return ImmutableList.of();
    }
    List<String> columnNames = Lists.newArrayList();
    for(SqlNode node : list.getList()) {
      columnNames.add(node.toString());
    }
    return columnNames;
  }

  public static SqlCreateReflection createAggregation(SqlParserPos pos, SqlIdentifier tblName, SqlNodeList dimensionList,
                                                      SqlNodeList measureList, SqlNodeList distributionList,
                                                      SqlNodeList partitionList, SqlNodeList sortList,
                                                      PartitionDistributionStrategy partitionDistributionStrategy, SqlIdentifier name) {
    return new SqlCreateReflection(pos, tblName, SqlLiteral.createBoolean(false, SqlParserPos.ZERO), null, dimensionList,
        measureList, distributionList, partitionList, sortList, partitionDistributionStrategy, name);
  }

  public static SqlCreateReflection createRaw(SqlParserPos pos, SqlIdentifier tblName, SqlNodeList displayList,
                                              SqlNodeList distributionList, SqlNodeList partitionList,
                                              SqlNodeList sortList,
                                              PartitionDistributionStrategy partitionDistributionStrategy,
                                              SqlIdentifier name) {
    return new SqlCreateReflection(pos, tblName, SqlLiteral.createBoolean(true, SqlParserPos.ZERO), displayList, null, null,
        distributionList, partitionList, sortList, partitionDistributionStrategy, name);
  }

  public static class NameAndGranularity {
    private final String name;
    private final Granularity granularity;

    public NameAndGranularity(String name, Granularity granularity) {
      super();
      this.name = name;
      this.granularity = granularity;
    }
    public String getName() {
      return name;
    }
    public Granularity getGranularity() {
      return granularity;
    }


  }

  public static enum Granularity{
    NORMAL, BY_DAY
  }
}
