/*
 * Copyright 2019-2024 The Polypheny Project
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
 *
 * This file incorporates code covered by the following terms:
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.polypheny.db.algebra.metadata;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Set;
import org.polypheny.db.algebra.AlgCollation;
import org.polypheny.db.algebra.AlgDistribution;
import org.polypheny.db.algebra.AlgNode;
import org.polypheny.db.algebra.constant.ExplainLevel;
import org.polypheny.db.algebra.core.relational.RelScan;
import org.polypheny.db.plan.AlgOptCost;
import org.polypheny.db.plan.AlgOptPredicateList;
import org.polypheny.db.rex.RexNode;
import org.polypheny.db.rex.RexTableIndexRef;
import org.polypheny.db.rex.RexTableIndexRef.AlgTableRef;
import org.polypheny.db.util.BuiltInMethod;
import org.polypheny.db.util.ImmutableBitSet;


/**
 * Contains the interfaces for several common forms of metadata.
 */
public abstract class BuiltInMetadata {

    /**
     * Metadata about the selectivity of a predicate.
     */
    public interface Selectivity extends Metadata {

        MetadataDef<Selectivity> DEF = MetadataDef.of( Selectivity.class, Selectivity.Handler.class, BuiltInMethod.SELECTIVITY.method );

        /**
         * Estimates the percentage of an expression's output rows which satisfy a given predicate. Returns null to indicate that no reliable estimate can be produced.
         *
         * @param predicate predicate whose selectivity is to be estimated against rel's output
         * @return estimated selectivity (between 0.0 and 1.0), or null if no reliable estimate can be determined
         */
        Double getSelectivity( RexNode predicate );

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<Selectivity> {

            Double getSelectivity( AlgNode r, AlgMetadataQuery mq, RexNode predicate );

        }

    }


    /**
     * Metadata about which combinations of columns are unique identifiers.
     */
    public interface UniqueKeys extends Metadata {

        MetadataDef<UniqueKeys> DEF = MetadataDef.of( UniqueKeys.class, UniqueKeys.Handler.class, BuiltInMethod.UNIQUE_KEYS.method );

        /**
         * Determines the set of unique minimal keys for this expression. A key is represented as an {@link ImmutableBitSet}, where each bit position represents a 0-based output column ordinal.
         * <p>
         * Nulls can be ignored if the algebra expression has filtered out null values.
         *
         * @param ignoreNulls if true, ignore null values when determining whether the keys are unique
         * @return set of keys, or null if this information cannot be determined (whereas empty set indicates definitely no keys at all)
         */
        Set<ImmutableBitSet> getUniqueKeys( boolean ignoreNulls );

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<UniqueKeys> {

            Set<ImmutableBitSet> getUniqueKeys( AlgNode r, AlgMetadataQuery mq, boolean ignoreNulls );

        }

    }


    /**
     * Metadata about whether a set of columns uniquely identifies a row.
     */
    public interface ColumnUniqueness extends Metadata {

        MetadataDef<ColumnUniqueness> DEF = MetadataDef.of( ColumnUniqueness.class, ColumnUniqueness.Handler.class, BuiltInMethod.COLUMN_UNIQUENESS.method );

        /**
         * Determines whether a specified set of columns from a specified algebra expression are unique.
         * <p>
         * For example, if the algebra expression is a {@code Scan} to T(A, B, C, D) whose key is (A, B), then:
         * <ul>
         * <li>{@code areColumnsUnique([0, 1])} yields true,
         * <li>{@code areColumnsUnique([0])} yields false,
         * <li>{@code areColumnsUnique([0, 2])} yields false.
         * </ul>
         *
         * Nulls can be ignored if the algebra expression has filtered out null values.
         *
         * @param columns column mask representing the subset of columns for which uniqueness will be determined
         * @param ignoreNulls if true, ignore null values when determining column uniqueness
         * @return whether the columns are unique, or null if not enough information is available to make that determination
         */
        Boolean areColumnsUnique( ImmutableBitSet columns, boolean ignoreNulls );

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<ColumnUniqueness> {

            Boolean areColumnsUnique( AlgNode r, AlgMetadataQuery mq, ImmutableBitSet columns, boolean ignoreNulls );

        }

    }


    /**
     * Metadata about which columns are sorted.
     */
    public interface Collation extends Metadata {

        MetadataDef<Collation> DEF = MetadataDef.of( Collation.class, Collation.Handler.class, BuiltInMethod.COLLATIONS.method );

        /**
         * Determines which columns are sorted.
         */
        ImmutableList<AlgCollation> collations();

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<Collation> {

            ImmutableList<AlgCollation> collations( AlgNode r, AlgMetadataQuery mq );

        }

    }


    /**
     * Metadata about how an algebra expression is distributed.
     * <p>
     * If you are an operator consuming an algebra expression, which subset of the rows are you seeing? You might be seeing all of them (BROADCAST or SINGLETON), only those whose key column values have a particular hash
     * code (HASH) or only those whose column values have particular values or ranges of values (RANGE).
     * <p>
     * When an algebra expression is partitioned, it is often partitioned among nodes, but it may be partitioned among threads running on the same node.
     */
    public interface Distribution extends Metadata {

        MetadataDef<Distribution> DEF = MetadataDef.of( Distribution.class, Distribution.Handler.class, BuiltInMethod.DISTRIBUTION.method );

        /**
         * Determines how the rows are distributed.
         */
        AlgDistribution distribution();

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<Distribution> {

            AlgDistribution distribution( AlgNode r, AlgMetadataQuery mq );

        }

    }


    /**
     * Metadata about the node types in an algebra expression.
     * <p>
     * For each algebra expression, it returns a multimap from the class to the nodes instantiating that class. Each node will appear in the multimap only once.
     */
    public interface NodeTypes extends Metadata {

        MetadataDef<NodeTypes> DEF = MetadataDef.of( NodeTypes.class, NodeTypes.Handler.class, BuiltInMethod.NODE_TYPES.method );

        /**
         * Returns a multimap from the class to the nodes instantiating that class. The default implementation for a node classifies it as a {@link AlgNode}.
         */
        Multimap<Class<? extends AlgNode>, AlgNode> getNodeTypes();

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<NodeTypes> {

            Multimap<Class<? extends AlgNode>, AlgNode> getNodeTypes( AlgNode r, AlgMetadataQuery mq );

        }

    }


    /**
     * Metadata about the number of tuples returned by an expression.
     */
    public interface TupleCount extends Metadata {

        MetadataDef<TupleCount> DEF = MetadataDef.of( TupleCount.class, TupleCount.Handler.class, BuiltInMethod.TUPLE_COUNT.method );

        /**
         * Estimates the number of rows which will be returned by an algebra expression. The default implementation for this query asks the alg itself
         * via {@link AlgNode#estimateTupleCount}, but metadata providers can override this with their own cost models.
         *
         * @return estimated row count, or null if no reliable estimate can be determined
         */
        Double getTupleCount();

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<TupleCount> {

            Double getTupleCount( AlgNode r, AlgMetadataQuery mq );

        }

    }


    /**
     * Metadata about the maximum number of rows returned by an algebra expression.
     */
    public interface MaxRowCount extends Metadata {

        MetadataDef<MaxRowCount> DEF = MetadataDef.of( MaxRowCount.class, MaxRowCount.Handler.class, BuiltInMethod.MAX_ROW_COUNT.method );

        /**
         * Estimates the max number of rows which will be returned by a algebra expression.
         * <p>
         * The default implementation for this query returns {@link Double#POSITIVE_INFINITY}, but metadata providers can override this with their own cost models.
         *
         * @return upper bound on the number of rows returned
         */
        Double getMaxRowCount();

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<MaxRowCount> {

            Double getMaxRowCount( AlgNode r, AlgMetadataQuery mq );

        }

    }


    /**
     * Metadata about the minimum number of rows returned by a algebra expression.
     */
    public interface MinRowCount extends Metadata {

        MetadataDef<MinRowCount> DEF = MetadataDef.of( MinRowCount.class, MinRowCount.Handler.class, BuiltInMethod.MIN_ROW_COUNT.method );

        /**
         * Estimates the minimum number of rows which will be returned by a algebra expression.
         * <p>
         * The default implementation for this query returns 0, but metadata providers can override this with their own cost models.
         *
         * @return lower bound on the number of rows returned
         */
        Double getMinRowCount();

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<MinRowCount> {

            Double getMinRowCount( AlgNode r, AlgMetadataQuery mq );

        }

    }


    /**
     * Metadata about the number of distinct rows returned by a set of columns in a algebra expression.
     */
    public interface DistinctRowCount extends Metadata {

        MetadataDef<DistinctRowCount> DEF = MetadataDef.of( DistinctRowCount.class, DistinctRowCount.Handler.class, BuiltInMethod.DISTINCT_ROW_COUNT.method );

        /**
         * Estimates the number of rows which would be produced by a GROUP BY on the set of columns indicated by groupKey, where the input to the GROUP BY has been pre-filtered by predicate. This quantity (leaving out predicate) is
         * often referred to as cardinality (as in gender being a "low-cardinality column").
         *
         * @param groupKey column mask representing group by columns
         * @param predicate pre-filtered predicates
         * @return distinct row count for groupKey, filtered by predicate, or null if no reliable estimate can be determined
         */
        Double getDistinctRowCount( ImmutableBitSet groupKey, RexNode predicate );

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<DistinctRowCount> {

            Double getDistinctRowCount( AlgNode r, AlgMetadataQuery mq, ImmutableBitSet groupKey, RexNode predicate );

        }

    }


    /**
     * Metadata about the proportion of original rows that remain in a algebra expression.
     */
    public interface PercentageOriginalRows extends Metadata {

        MetadataDef<PercentageOriginalRows> DEF = MetadataDef.of( PercentageOriginalRows.class, PercentageOriginalRows.Handler.class, BuiltInMethod.PERCENTAGE_ORIGINAL_ROWS.method );

        /**
         * Estimates the percentage of the number of rows actually produced by a algebra expression out of the number of rows it would produce if all single-table filter conditions were removed.
         *
         * @return estimated percentage (between 0.0 and 1.0), or null if no reliable estimate can be determined
         */
        Double getPercentageOriginalRows();

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<PercentageOriginalRows> {

            Double getPercentageOriginalRows( AlgNode r, AlgMetadataQuery mq );

        }

    }


    /**
     * Metadata about the number of distinct values in the original source of a column or set of columns.
     */
    public interface PopulationSize extends Metadata {

        MetadataDef<PopulationSize> DEF = MetadataDef.of( PopulationSize.class, PopulationSize.Handler.class, BuiltInMethod.POPULATION_SIZE.method );

        /**
         * Estimates the distinct row count in the original source for the given {@code groupKey}, ignoring any filtering being applied by the expression.
         * Typically, "original source" means base table, but for derived columns, the estimate may come from a non-leaf alg such as a LogicalProject.
         *
         * @param groupKey column mask representing the subset of columns for which the row count will be determined
         * @return distinct row count for the given groupKey, or null if no reliable estimate can be determined
         */
        Double getPopulationSize( ImmutableBitSet groupKey );

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<PopulationSize> {

            Double getPopulationSize( AlgNode r, AlgMetadataQuery mq, ImmutableBitSet groupKey );

        }

    }


    /**
     * Metadata about the size of rows and columns.
     */
    public interface Size extends Metadata {

        MetadataDef<Size> DEF = MetadataDef.of( Size.class, Size.Handler.class, BuiltInMethod.AVERAGE_ROW_SIZE.method, BuiltInMethod.AVERAGE_COLUMN_SIZES.method );

        /**
         * Determines the average size (in bytes) of a row from this algebra expression.
         *
         * @return average size of a row, in bytes, or null if not known
         */
        Double averageRowSize();

        /**
         * Determines the average size (in bytes) of a value of a column in this algebra expression.
         * <p>
         * Null values are included (presumably they occupy close to 0 bytes).
         * <p>
         * It is left to the caller to decide whether the size is the compressed size, the uncompressed size, or memory allocation when the value is wrapped in an object in the Java heap.
         * The uncompressed size is probably a good compromise.
         *
         * @return an immutable list containing, for each column, the average size of a column value, in bytes. Each value or the entire list may be null if the metadata is not available
         */
        List<Double> averageColumnSizes();

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<Size> {

            Double averageRowSize( AlgNode r, AlgMetadataQuery mq );

            List<Double> averageColumnSizes( AlgNode r, AlgMetadataQuery mq );

        }

    }


    /**
     * Metadata about the origins of columns.
     */
    public interface ColumnOrigin extends Metadata {

        MetadataDef<ColumnOrigin> DEF = MetadataDef.of( ColumnOrigin.class, ColumnOrigin.Handler.class, BuiltInMethod.COLUMN_ORIGIN.method );

        /**
         * For a given output column of an expression, determines all columns of underlying tables which contribute to result values. An output column may have more than one origin due to expressions such as Union and
         * LogicalProject. The optimizer may use this information for catalog access (e.g. index availability).
         *
         * @param outputColumn 0-based ordinal for output column of interest
         * @return set of origin columns, or null if this information cannot be determined (whereas empty set indicates definitely no origin columns at all)
         */
        Set<AlgColumnOrigin> getColumnOrigins( int outputColumn );

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<ColumnOrigin> {

            Set<AlgColumnOrigin> getColumnOrigins( AlgNode r, AlgMetadataQuery mq, int outputColumn );

        }

    }


    /**
     * Metadata about the origins of expressions.
     */
    public interface ExpressionLineage extends Metadata {

        MetadataDef<ExpressionLineage> DEF = MetadataDef.of( ExpressionLineage.class, ExpressionLineage.Handler.class, BuiltInMethod.EXPRESSION_LINEAGE.method );

        /**
         * Given the input expression applied on the given {@link AlgNode}, this provider returns the expression with its lineage resolved.
         * <p>
         * In particular, the result will be a set of nodes which might contain references to columns in Scan operators ({@link RexTableIndexRef}). An expression can have more than one lineage expression due to
         * Union operators. However, we do not check column equality in Filter predicates. Each Scan operator below the node is identified uniquely by its qualified name and its entity number.
         * <p>
         * For example, if the expression is {@code $0 + 2} and {@code $0} originated from column {@code $3} in the {@code 0} occurrence of table {@code A} in the plan, result will be: {@code A.#0.$3 + 2}.
         * Occurrences are generated in no particular order, but it is guaranteed that if two expressions referred to the same table, the qualified name + occurrence will be the same.
         *
         * @param expression expression whose lineage we want to resolve
         * @return set of expressions with lineage resolved, or null if this information cannot be determined (e.g. origin of an expression is an aggregation in an {@link org.polypheny.db.algebra.core.Aggregate} operator)
         */
        Set<RexNode> getExpressionLineage( RexNode expression );

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<ExpressionLineage> {

            Set<RexNode> getExpressionLineage( AlgNode r, AlgMetadataQuery mq, RexNode expression );

        }

    }


    /**
     * Metadata to obtain references to tables used by a given expression.
     */
    public interface TableReferences extends Metadata {

        MetadataDef<TableReferences> DEF = MetadataDef.of( TableReferences.class, TableReferences.Handler.class, BuiltInMethod.TABLE_REFERENCES.method );

        /**
         * This provider returns the tables used by a given plan.
         * <p>
         * In particular, the result will be a set of unique table references ({@link AlgTableRef}) corresponding to each Scan operator in the plan. These table references are
         * composed by the table qualified name and an entity number.
         * <p>
         * Importantly, the table identifiers returned by this metadata provider will be consistent with the unique identifiers used by the {@link ExpressionLineage} provider,
         * meaning that it is guaranteed that same table will use same unique identifiers in both.
         *
         * @return set of unique table identifiers, or null if this information cannot be determined
         */
        Set<AlgTableRef> getTableReferences();

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<TableReferences> {

            Set<AlgTableRef> getTableReferences( AlgNode r, AlgMetadataQuery mq );

        }

    }


    /**
     * Metadata about the cost of evaluating an algebra expression, including all of its inputs.
     */
    public interface CumulativeCost extends Metadata {

        MetadataDef<CumulativeCost> DEF = MetadataDef.of( CumulativeCost.class, CumulativeCost.Handler.class, BuiltInMethod.CUMULATIVE_COST.method );

        /**
         * Estimates the cost of executing an algebra expression, including the cost of its inputs. The default implementation for this query adds {@link NonCumulativeCost#getNonCumulativeCost}
         * to the cumulative cost of each input, but metadata providers can override this with their own cost models, e.g. to take into account interactions between expressions.
         *
         * @return estimated cost, or null if no reliable estimate can be determined
         */
        AlgOptCost getCumulativeCost();

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<CumulativeCost> {

            AlgOptCost getCumulativeCost( AlgNode r, AlgMetadataQuery mq );

        }

    }


    /**
     * Metadata about the cost of evaluating an algebra expression, not including its inputs.
     */
    public interface NonCumulativeCost extends Metadata {

        MetadataDef<NonCumulativeCost> DEF = MetadataDef.of( NonCumulativeCost.class, NonCumulativeCost.Handler.class, BuiltInMethod.NON_CUMULATIVE_COST.method );

        /**
         * Estimates the cost of executing an algebra expression, not counting the cost of its inputs. (However, the non-cumulative cost is still usually dependent on the row counts of the inputs.)
         * The default implementation for this query asks the alg itself via {@link AlgNode#computeSelfCost}, but metadata providers can override this with their own cost models.
         *
         * @return estimated cost, or null if no reliable estimate can be determined
         */
        AlgOptCost getNonCumulativeCost();

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<NonCumulativeCost> {

            AlgOptCost getNonCumulativeCost( AlgNode r, AlgMetadataQuery mq );

        }

    }


    /**
     * Metadata about whether an algebra expression should appear in a plan.
     */
    public interface ExplainVisibility extends Metadata {

        MetadataDef<ExplainVisibility> DEF = MetadataDef.of( ExplainVisibility.class, ExplainVisibility.Handler.class, BuiltInMethod.EXPLAIN_VISIBILITY.method );

        /**
         * Determines whether an algebra expression should be visible in EXPLAIN PLAN output at a particular level of detail.
         *
         * @param explainLevel level of detail
         * @return true for visible, false for invisible
         */
        Boolean isVisibleInExplain( ExplainLevel explainLevel );

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<ExplainVisibility> {

            Boolean isVisibleInExplain( AlgNode r, AlgMetadataQuery mq, ExplainLevel explainLevel );

        }

    }


    /**
     * Metadata about the predicates that hold in the rows emitted from an algebra expression.
     */
    public interface Predicates extends Metadata {

        MetadataDef<Predicates> DEF = MetadataDef.of( Predicates.class, Predicates.Handler.class, BuiltInMethod.PREDICATES.method );

        /**
         * Derives the predicates that hold on rows emitted from an algebra expression.
         *
         * @return Predicate list
         */
        AlgOptPredicateList getPredicates();

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<Predicates> {

            AlgOptPredicateList getPredicates( AlgNode r, AlgMetadataQuery mq );

        }

    }


    /**
     * Metadata about the predicates that hold in the rows emitted from an algebra expression.
     * <p>
     * The difference with respect to {@link Predicates} provider is that this provider tries to extract ALL predicates even if they are not applied on the output expressions of the algebra expression; we rely
     * on {@link RexTableIndexRef} to reference origin columns in {@link RelScan} for the result predicates.
     */
    public interface AllPredicates extends Metadata {

        MetadataDef<AllPredicates> DEF = MetadataDef.of( AllPredicates.class, AllPredicates.Handler.class, BuiltInMethod.ALL_PREDICATES.method );

        /**
         * Derives the predicates that hold on rows emitted from an algebra expression.
         *
         * @return predicate list, or null if the provider cannot infer the lineage for any of the expressions contained in any of the predicates
         */
        AlgOptPredicateList getAllPredicates();

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<AllPredicates> {

            AlgOptPredicateList getAllPredicates( AlgNode r, AlgMetadataQuery mq );

        }

    }


    /**
     * Metadata about the degree of parallelism of an algebra expression, and how its operators are assigned to processes with independent resource pools.
     */
    public interface Parallelism extends Metadata {

        MetadataDef<Parallelism> DEF = MetadataDef.of( Parallelism.class, Parallelism.Handler.class, BuiltInMethod.IS_PHASE_TRANSITION.method, BuiltInMethod.SPLIT_COUNT.method );

        /**
         * Returns whether each physical operator implementing this algebra expression belongs to a different process than its inputs.
         * <p>
         * A collection of operators processing all of the splits of a particular stage in the query pipeline is called a "phase". A phase starts with a leaf node such as a {@link RelScan},
         * or with a phase-change node such as an {@link org.polypheny.db.algebra.core.Exchange}. Hadoop's shuffle operator (a form of sort-exchange) causes data to be sent across the network.
         */
        Boolean isPhaseTransition();

        /**
         * Returns the number of distinct splits of the data.
         * <p>
         * Note that splits must be distinct. For broadcast, where each copy is the same, returns 1.
         * <p>
         * Thus the split count is the <em>proportion</em> of the data seen by each operator instance.
         */
        Integer splitCount();

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<Parallelism> {

            Boolean isPhaseTransition( AlgNode r, AlgMetadataQuery mq );

            Integer splitCount( AlgNode r, AlgMetadataQuery mq );

        }

    }


    /**
     * Metadata about the memory use of an operator.
     */
    public interface Memory extends Metadata {

        MetadataDef<Memory> DEF = MetadataDef.of(
                Memory.class,
                Memory.Handler.class, BuiltInMethod.MEMORY.method,
                BuiltInMethod.CUMULATIVE_MEMORY_WITHIN_PHASE.method,
                BuiltInMethod.CUMULATIVE_MEMORY_WITHIN_PHASE_SPLIT.method );

        /**
         * Returns the expected amount of memory, in bytes, required by a physical operator implementing this algebra expression, across all splits.
         * <p>
         * How much memory is used depends very much on the algorithm; for example, an implementation of {@link org.polypheny.db.algebra.core.Aggregate} that loads all data into a hash table requires approximately
         * {@code rowCount * averageRowSize} bytes, whereas an implementation that assumes that the input is sorted requires only {@code averageRowSize} bytes to maintain a single accumulator for each aggregate function.
         */
        Double memory();

        /**
         * Returns the cumulative amount of memory, in bytes, required by the physical operator implementing this algebra expression, and all other operators within the same phase, across all splits.
         *
         * @see Parallelism#splitCount()
         */
        Double cumulativeMemoryWithinPhase();

        /**
         * Returns the expected cumulative amount of memory, in bytes, required by the physical operator implementing this algebra expression, and all operators within the same phase, within each split.
         * <p>
         * Basic formula:
         *
         * <blockquote>
         * cumulativeMemoryWithinPhaseSplit = cumulativeMemoryWithinPhase / Parallelism.splitCount
         * </blockquote>
         */
        Double cumulativeMemoryWithinPhaseSplit();

        /**
         * Handler API.
         */
        interface Handler extends MetadataHandler<Memory> {

            Double memory( AlgNode r, AlgMetadataQuery mq );

            Double cumulativeMemoryWithinPhase( AlgNode r, AlgMetadataQuery mq );

            Double cumulativeMemoryWithinPhaseSplit( AlgNode r, AlgMetadataQuery mq );

        }

    }


}

