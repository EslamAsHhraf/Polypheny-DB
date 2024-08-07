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
 */

package org.polypheny.db.prepare;

import java.util.List;
import org.apache.calcite.linq4j.tree.BlockStatement;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.linq4j.tree.ParameterExpression;
import org.polypheny.db.rex.RexNode;

/**
 * Translator from Java AST to {@link RexNode}.
 */
interface ScalarTranslator {

    RexNode toRex( BlockStatement statement );

    List<RexNode> toRexList( BlockStatement statement );

    RexNode toRex( Expression expression );

    ScalarTranslator bind( List<ParameterExpression> parameterList, List<RexNode> values );

}
