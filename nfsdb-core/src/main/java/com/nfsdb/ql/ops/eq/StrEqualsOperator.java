/*******************************************************************************
 *  _  _ ___ ___     _ _
 * | \| | __/ __| __| | |__
 * | .` | _|\__ \/ _` | '_ \
 * |_|\_|_| |___/\__,_|_.__/
 *
 * Copyright (c) 2014-2015. The NFSdb project and its contributors.
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
 ******************************************************************************/

package com.nfsdb.ql.ops.eq;

import com.nfsdb.collections.ObjList;
import com.nfsdb.misc.Chars;
import com.nfsdb.ql.Record;
import com.nfsdb.ql.ops.AbstractBinaryOperator;
import com.nfsdb.ql.ops.Function;
import com.nfsdb.ql.ops.VirtualColumn;
import com.nfsdb.storage.ColumnType;

public class StrEqualsOperator extends AbstractBinaryOperator {

    public final static StrEqualsOperator FACTORY = new StrEqualsOperator();

    private StrEqualsOperator() {
        super(ColumnType.BOOLEAN);
    }

    @Override
    public boolean getBool(Record rec) {
        return Chars.equals(lhs.getFlyweightStr(rec), rhs.getFlyweightStr(rec));
    }

    @Override
    public Function newInstance(ObjList<VirtualColumn> args) {
        VirtualColumn vc = args.getQuick(1);
        if (vc.isConstant() && vc.getFlyweightStr(null) == null) {
            return new StrEqualsNullOperator();
        }
        return new StrEqualsOperator();
    }
}
